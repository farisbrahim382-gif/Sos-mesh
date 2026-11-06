package com.example.mesh.transport

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class WifiDirectTransport(
    private val context: Context
) : MeshTransport {

    override val transportName: String = "Wi-Fi Direct (P2P Mesh)"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(replay = 20, extraBufferCapacity = 50)
    override val incomingPackets: Flow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val _discoveredP2pPeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val discoveredP2pPeers: StateFlow<List<WifiP2pDevice>> = _discoveredP2pPeers.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val p2pManager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var p2pChannel: WifiP2pManager.Channel? = null

    private var serverSocket: ServerSocket? = null
    private val activeSockets = CopyOnWriteArrayList<Socket>()

    companion object {
        private const val TAG = "WifiDirectTransport"
        const val P2P_PORT = 8888
    }

    @SuppressLint("MissingPermission")
    override suspend fun start() {
        if (p2pManager == null) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        p2pChannel = p2pManager.initialize(context, context.mainLooper, null)
        _connectionState.value = ConnectionState.CONNECTED

        startP2pListener()
        discoverPeers()
    }

    private fun startP2pListener() {
        scope.launch {
            try {
                serverSocket = ServerSocket(P2P_PORT)
                while (true) {
                    val socket = serverSocket?.accept() ?: break
                    activeSockets.add(socket)
                    handleSocketInput(socket)
                }
            } catch (e: Exception) {
                Log.d(TAG, "P2P ServerSocket closed: ${e.message}")
            }
        }
    }

    private fun handleSocketInput(socket: Socket) {
        scope.launch {
            try {
                val input = socket.getInputStream()
                val buffer = ByteArray(2048)
                while (socket.isConnected) {
                    val read = input.read(buffer)
                    if (read > 0) {
                        val packet = MeshPacket.fromByteArray(buffer.copyOfRange(0, read))
                        if (packet != null) {
                            _incomingPackets.emit(packet)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                activeSockets.remove(socket)
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        val channel = p2pChannel ?: return
        p2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "P2P discovery started")
                requestPeers()
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "P2P discovery failed: $reason")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun requestPeers() {
        val channel = p2pChannel ?: return
        p2pManager?.requestPeers(channel) { peers: WifiP2pDeviceList? ->
            val list = peers?.deviceList?.toList() ?: emptyList()
            _discoveredP2pPeers.value = list
        }
    }

    fun connectToPeerHost(hostIp: String) {
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(hostIp, P2P_PORT), 3000)
                activeSockets.add(socket)
                handleSocketInput(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to P2P host $hostIp", e)
            }
        }
    }

    override suspend fun sendPacket(packet: MeshPacket): Boolean {
        if (activeSockets.isEmpty()) return false
        val bytes = packet.toByteArray()
        var anySent = false

        for (socket in activeSockets) {
            try {
                val output = socket.getOutputStream()
                output.write(bytes)
                output.flush()
                anySent = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending to P2P socket", e)
                activeSockets.remove(socket)
            }
        }
        return anySent
    }

    override suspend fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        for (s in activeSockets) {
            try { s.close() } catch (_: Exception) {}
        }
        activeSockets.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun getSignalMetrics(): SignalMetrics {
        return SignalMetrics(
            rssiDbm = -45,
            snrDb = 22.0f,
            packetLossPercent = 0
        )
    }
}
