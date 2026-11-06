package com.example.mesh.transport

import android.content.Context
import android.net.wifi.WifiManager
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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WifiLanTransport(
    private val context: Context
) : MeshTransport {

    override val transportName: String = "Local Wi-Fi LAN (UDP Broadcast)"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(replay = 20, extraBufferCapacity = 50)
    override val incomingPackets: Flow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    companion object {
        private const val TAG = "WifiLanTransport"
        const val BROADCAST_PORT = 4242
    }

    override suspend fun start() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("SosMeshWifiLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            socket = DatagramSocket(BROADCAST_PORT).apply {
                broadcast = true
                reuseAddress = true
            }

            _connectionState.value = ConnectionState.CONNECTED
            startReceiverLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Wi-Fi LAN transport: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    private fun startReceiverLoop() {
        scope.launch {
            val buffer = ByteArray(2048)
            while (isActive) {
                try {
                    val s = socket ?: break
                    val datagram = DatagramPacket(buffer, buffer.size)
                    s.receive(datagram)

                    val packetData = buffer.copyOfRange(0, datagram.length)
                    val meshPacket = MeshPacket.fromByteArray(packetData)
                    if (meshPacket != null) {
                        _incomingPackets.emit(meshPacket)
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                    Log.w(TAG, "Receive error: ${e.message}")
                }
            }
        }
    }

    override suspend fun sendPacket(packet: MeshPacket): Boolean {
        val s = socket ?: return false
        val bytes = packet.toByteArray()

        return try {
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val datagram = DatagramPacket(bytes, bytes.size, broadcastAddr, BROADCAST_PORT)
            s.send(datagram)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending UDP broadcast packet", e)
            false
        }
    }

    override suspend fun stop() {
        try {
            socket?.close()
            socket = null
            multicastLock?.release()
            multicastLock = null
        } catch (_: Exception) {}
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun getSignalMetrics(): SignalMetrics {
        return SignalMetrics(
            rssiDbm = -50,
            snrDb = 25.0f,
            packetLossPercent = 0
        )
    }
}
