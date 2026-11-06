package com.example.mesh.transport

import android.util.Log
import com.example.mesh.MeshPacket
import com.example.mesh.PacketPriority
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
import java.util.concurrent.ConcurrentHashMap

enum class TransportChannel {
    BLUETOOTH_CLASSIC,
    BLUETOOTH_LE,
    WIFI_LAN,
    WIFI_DIRECT,
    OPTIONAL_LORA,
    VIRTUAL_TESTBENCH
}

data class TransportStatus(
    val channel: TransportChannel,
    val name: String,
    val isEnabled: Boolean,
    val connectionState: ConnectionState,
    val isPhysical: Boolean
)

class HybridMeshRouter(
    val btClassicTransport: BluetoothClassicTransport,
    val blePhoneTransport: BluetoothLETransport,
    val wifiLanTransport: WifiLanTransport,
    val wifiDirectTransport: WifiDirectTransport,
    val optionalLoRaTransport: OptionalLoRaTransport,
    val virtualTestbench: VirtualSimulationTransport
) : MeshTransport {

    override val transportName: String = "Hybrid Mesh Engine (Multi-Transport)"

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(replay = 50, extraBufferCapacity = 100)
    override val incomingPackets: Flow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Channel enable/disable flags (Default: All phone-to-phone wireless + virtual testbench active)
    private val enabledTransports = ConcurrentHashMap<TransportChannel, Boolean>().apply {
        put(TransportChannel.BLUETOOTH_CLASSIC, true)
        put(TransportChannel.BLUETOOTH_LE, true)
        put(TransportChannel.WIFI_LAN, true)
        put(TransportChannel.WIFI_DIRECT, true)
        put(TransportChannel.OPTIONAL_LORA, false) // Optional extension
        put(TransportChannel.VIRTUAL_TESTBENCH, true) // Active for instant in-app testability
    }

    private val _transportsStatus = MutableStateFlow<List<TransportStatus>>(emptyList())
    val transportsStatus: StateFlow<List<TransportStatus>> = _transportsStatus.asStateFlow()

    init {
        updateStatusList()
        monitorAllTransports()
    }

    private fun monitorAllTransports() {
        val all = listOf(
            TransportChannel.BLUETOOTH_CLASSIC to btClassicTransport,
            TransportChannel.BLUETOOTH_LE to blePhoneTransport,
            TransportChannel.WIFI_LAN to wifiLanTransport,
            TransportChannel.WIFI_DIRECT to wifiDirectTransport,
            TransportChannel.OPTIONAL_LORA to optionalLoRaTransport,
            TransportChannel.VIRTUAL_TESTBENCH to virtualTestbench
        )

        all.forEach { (channel, transport) ->
            scope.launch {
                transport.incomingPackets.collect { packet ->
                    if (isChannelEnabled(channel)) {
                        _incomingPackets.emit(packet)
                    }
                }
            }
            scope.launch {
                transport.connectionState.collect {
                    updateStatusList()
                }
            }
        }
    }

    fun isChannelEnabled(channel: TransportChannel): Boolean {
        return enabledTransports[channel] ?: false
    }

    fun setChannelEnabled(channel: TransportChannel, enabled: Boolean) {
        enabledTransports[channel] = enabled
        updateStatusList()
        scope.launch {
            val transport = getTransport(channel)
            if (enabled) {
                transport.start()
            } else {
                transport.stop()
            }
        }
    }

    private fun getTransport(channel: TransportChannel): MeshTransport {
        return when (channel) {
            TransportChannel.BLUETOOTH_CLASSIC -> btClassicTransport
            TransportChannel.BLUETOOTH_LE -> blePhoneTransport
            TransportChannel.WIFI_LAN -> wifiLanTransport
            TransportChannel.WIFI_DIRECT -> wifiDirectTransport
            TransportChannel.OPTIONAL_LORA -> optionalLoRaTransport
            TransportChannel.VIRTUAL_TESTBENCH -> virtualTestbench
        }
    }

    private fun updateStatusList() {
        _transportsStatus.value = listOf(
            TransportStatus(
                channel = TransportChannel.WIFI_LAN,
                name = wifiLanTransport.transportName,
                isEnabled = isChannelEnabled(TransportChannel.WIFI_LAN),
                connectionState = wifiLanTransport.connectionState.value,
                isPhysical = true
            ),
            TransportStatus(
                channel = TransportChannel.BLUETOOTH_CLASSIC,
                name = btClassicTransport.transportName,
                isEnabled = isChannelEnabled(TransportChannel.BLUETOOTH_CLASSIC),
                connectionState = btClassicTransport.connectionState.value,
                isPhysical = true
            ),
            TransportStatus(
                channel = TransportChannel.BLUETOOTH_LE,
                name = blePhoneTransport.transportName,
                isEnabled = isChannelEnabled(TransportChannel.BLUETOOTH_LE),
                connectionState = blePhoneTransport.connectionState.value,
                isPhysical = true
            ),
            TransportStatus(
                channel = TransportChannel.WIFI_DIRECT,
                name = wifiDirectTransport.transportName,
                isEnabled = isChannelEnabled(TransportChannel.WIFI_DIRECT),
                connectionState = wifiDirectTransport.connectionState.value,
                isPhysical = true
            ),
            TransportStatus(
                channel = TransportChannel.OPTIONAL_LORA,
                name = optionalLoRaTransport.transportName,
                isEnabled = isChannelEnabled(TransportChannel.OPTIONAL_LORA),
                connectionState = optionalLoRaTransport.connectionState.value,
                isPhysical = true
            ),
            TransportStatus(
                channel = TransportChannel.VIRTUAL_TESTBENCH,
                name = virtualTestbench.transportName,
                isEnabled = isChannelEnabled(TransportChannel.VIRTUAL_TESTBENCH),
                connectionState = virtualTestbench.connectionState.value,
                isPhysical = false
            )
        )
    }

    override suspend fun start() {
        _connectionState.value = ConnectionState.CONNECTED
        // Start all enabled transports
        enabledTransports.forEach { (channel, enabled) ->
            if (enabled) {
                scope.launch {
                    try { getTransport(channel).start() } catch (e: Exception) { Log.w("HybridMesh", "Start failed for $channel: ${e.message}") }
                }
            }
        }
    }

    override suspend fun stop() {
        enabledTransports.forEach { (channel, _) ->
            scope.launch {
                try { getTransport(channel).stop() } catch (_: Exception) {}
            }
        }
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun sendPacket(packet: MeshPacket): Boolean {
        var sentOnAny = false

        // DYNAMIC MULTI-TRANSPORT SELECTION RULE:
        // If CRITICAL SOS: Preempt and broadcast across ALL active enabled transports simultaneously!
        if (packet.priority == PacketPriority.CRITICAL_SOS) {
            enabledTransports.forEach { (channel, isEnabled) ->
                if (isEnabled) {
                    val t = getTransport(channel)
                    try {
                        val ok = t.sendPacket(packet)
                        if (ok) sentOnAny = true
                    } catch (_: Exception) {}
                }
            }
            return sentOnAny
        }

        // For Normal / High Priority packets:
        // Try local Wi-Fi LAN first (fastest, lowest battery)
        if (isChannelEnabled(TransportChannel.WIFI_LAN)) {
            val ok = wifiLanTransport.sendPacket(packet)
            if (ok) sentOnAny = true
        }

        // Try Bluetooth Classic (P2P connected sockets)
        if (isChannelEnabled(TransportChannel.BLUETOOTH_CLASSIC)) {
            val ok = btClassicTransport.sendPacket(packet)
            if (ok) sentOnAny = true
        }

        // Try Bluetooth LE Phone-to-Phone
        if (isChannelEnabled(TransportChannel.BLUETOOTH_LE)) {
            val ok = blePhoneTransport.sendPacket(packet)
            if (ok) sentOnAny = true
        }

        // Try Wi-Fi Direct
        if (isChannelEnabled(TransportChannel.WIFI_DIRECT)) {
            val ok = wifiDirectTransport.sendPacket(packet)
            if (ok) sentOnAny = true
        }

        // If external LoRa is enabled, transmit over radio waves
        if (isChannelEnabled(TransportChannel.OPTIONAL_LORA)) {
            val ok = optionalLoRaTransport.sendPacket(packet)
            if (ok) sentOnAny = true
        }

        // If virtual testbench is enabled, feed into testbench simulation
        if (isChannelEnabled(TransportChannel.VIRTUAL_TESTBENCH)) {
            val ok = virtualTestbench.sendPacket(packet)
            if (ok) sentOnAny = true
        }

        return sentOnAny
    }

    override fun getSignalMetrics(): SignalMetrics {
        return SignalMetrics(
            rssiDbm = -60,
            snrDb = 14.5f,
            packetLossPercent = 0
        )
    }
}
