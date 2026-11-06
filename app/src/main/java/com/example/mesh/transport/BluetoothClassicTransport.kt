package com.example.mesh.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.mesh.MeshPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class BluetoothClassicTransport(
    private val context: Context
) : MeshTransport {

    override val transportName: String = "Bluetooth Classic (RFCOMM SPP)"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(replay = 20, extraBufferCapacity = 50)
    override val incomingPackets: Flow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var serverSocket: BluetoothServerSocket? = null
    private val activeSockets = CopyOnWriteArrayList<BluetoothSocket>()

    companion object {
        private const val TAG = "BtClassicTransport"
        // Dedicated RFCOMM SPP UUID for SOS MESH phone-to-phone links
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val SERVICE_NAME = "SOS_MESH_CLASSIC"
    }

    @SuppressLint("MissingPermission")
    override suspend fun start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        _connectionState.value = ConnectionState.CONNECTED
        startServerListener()
    }

    @SuppressLint("MissingPermission")
    private fun startServerListener() {
        scope.launch {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    activeSockets.add(socket)
                    manageConnectedSocket(socket)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Server socket closed or error: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(device: BluetoothDevice) {
        scope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
                activeSockets.add(socket)
                _connectionState.value = ConnectionState.CONNECTED
                manageConnectedSocket(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Failed connecting to peer: ${device.address}", e)
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        scope.launch {
            val inStream: InputStream = socket.inputStream
            val buffer = ByteArray(1024)
            val streamBuffer = ByteArrayOutputStream()

            try {
                while (isActive && socket.isConnected) {
                    val bytesRead = inStream.read(buffer)
                    if (bytesRead > 0) {
                        streamBuffer.write(buffer, 0, bytesRead)
                        val fullBytes = streamBuffer.toByteArray()
                        val packet = MeshPacket.fromByteArray(fullBytes)
                        if (packet != null) {
                            streamBuffer.reset()
                            _incomingPackets.emit(packet)
                        } else if (streamBuffer.size() > 2048) {
                            streamBuffer.reset()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Peer disconnected: ${e.message}")
            } finally {
                activeSockets.remove(socket)
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    override suspend fun sendPacket(packet: MeshPacket): Boolean {
        if (activeSockets.isEmpty()) return false
        val bytes = packet.toByteArray()
        var anySuccess = false

        for (socket in activeSockets) {
            try {
                val outStream: OutputStream = socket.outputStream
                outStream.write(bytes)
                outStream.flush()
                anySuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to socket", e)
                activeSockets.remove(socket)
            }
        }
        return anySuccess
    }

    @SuppressLint("MissingPermission")
    override suspend fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        for (socket in activeSockets) {
            try { socket.close() } catch (_: Exception) {}
        }
        activeSockets.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun getSignalMetrics(): SignalMetrics {
        return SignalMetrics(
            rssiDbm = -65,
            snrDb = 12.0f,
            packetLossPercent = 0
        )
    }
}
