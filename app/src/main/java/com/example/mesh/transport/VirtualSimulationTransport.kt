package com.example.mesh.transport

import com.example.mesh.MeshPacket
import com.example.mesh.PacketPriority
import com.example.mesh.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.Random

class VirtualSimulationTransport : MeshTransport {
    override val transportName: String = "Virtual 3-Node Testbench"

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(replay = 50, extraBufferCapacity = 100)
    override val incomingPackets: Flow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val random = Random()

    var simulatePacketLoss: Boolean = false

    override suspend fun start() {
        _connectionState.value = ConnectionState.CONNECTED
        seedInitialVirtualNodes()
    }

    override suspend fun stop() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun sendPacket(packet: MeshPacket): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false

        // Simulate LoRa airtime delay (200ms - 400ms depending on payload)
        delay(250)

        if (simulatePacketLoss && random.nextInt(100) < 25) {
            // Simulated 25% packet loss on the RF channel
            return false
        }

        // Simulate multi-hop relay propagation through Node-B (Relay) to Node-C (Target)
        scope.launch {
            delay(300) // Hop 1: Node A -> Node B (Relay)

            if (packet.hopsTraveled < packet.hopLimit) {
                // Node B forwards with incremented hop count
                val relayedPacket = packet.copy(
                    hopsTraveled = packet.hopsTraveled + 1
                )

                // If destination was BROADCAST or NODE-C, Node C receives it and can respond with ACK
                if (packet.recipientId == "BROADCAST" || packet.recipientId == "NODE-C") {
                    delay(300) // Hop 2: Node B -> Node C
                    if (packet.packetType == PacketType.SOS_ALERT || packet.priority == PacketPriority.CRITICAL_SOS) {
                        // Node C emits an acknowledgment back through the mesh
                        val ackPayload = "ACK:SOS_RECEIVED_BY_NODE-C".toByteArray(StandardCharsets.UTF_8)
                        val ackPacket = MeshPacket(
                            packetType = PacketType.ACK_RECEIPT,
                            priority = PacketPriority.HIGH,
                            hopLimit = 7,
                            hopsTraveled = 2,
                            senderId = "NODE-C",
                            recipientId = packet.senderId,
                            payload = ackPayload
                        )
                        _incomingPackets.emit(ackPacket)
                    }
                }
            }
        }

        return true
    }

    /**
     * Seeds virtual neighborhood nodes (Node B - Relay, Node C - SAR Team)
     */
    private fun seedInitialVirtualNodes() {
        scope.launch {
            // Heartbeat from Node-B (Direct Relay, 1 hop away)
            val nodeBHeartbeat = MeshPacket(
                packetType = PacketType.NODE_HEARTBEAT,
                priority = PacketPriority.NORMAL,
                hopLimit = 7,
                hopsTraveled = 1,
                senderId = "NODE-B",
                recipientId = "BROADCAST",
                payload = """{"alias":"Relay-Alpha","battery":88,"rssi":-68,"snr":9.2,"lat":37.7750,"lng":-122.4180,"hw":"ESP32-SX1262"}""".toByteArray(StandardCharsets.UTF_8)
            )
            _incomingPackets.emit(nodeBHeartbeat)

            delay(150)
            // Heartbeat from Node-C (2 hops away, SAR Response unit)
            val nodeCHeartbeat = MeshPacket(
                packetType = PacketType.NODE_HEARTBEAT,
                priority = PacketPriority.NORMAL,
                hopLimit = 7,
                hopsTraveled = 2,
                senderId = "NODE-C",
                recipientId = "BROADCAST",
                payload = """{"alias":"SAR-Rescue-Unit","battery":94,"rssi":-84,"snr":6.5,"lat":37.7820,"lng":-122.4120,"hw":"Meshtastic-TBeam"}""".toByteArray(StandardCharsets.UTF_8)
            )
            _incomingPackets.emit(nodeCHeartbeat)
        }
    }

    /**
     * Injects a synthetic multi-hop test packet from Node-C across Node-B to local node
     */
    fun injectMultiHopTestPacket(sender: String = "NODE-C", message: String = "Test packet from field unit via relay") {
        scope.launch {
            delay(200)
            val packet = MeshPacket(
                packetType = PacketType.CHAT_MESSAGE,
                priority = PacketPriority.NORMAL,
                hopLimit = 7,
                hopsTraveled = 2, // Traveled 2 hops
                senderId = sender,
                recipientId = "BROADCAST",
                payload = message.toByteArray(StandardCharsets.UTF_8)
            )
            _incomingPackets.emit(packet)
        }
    }

    /**
     * Injects a synthetic SOS emergency beacon from virtual node
     */
    fun injectVirtualSosBeacon() {
        scope.launch {
            val sosJson = """{"type":"MEDICAL","severity":"CRITICAL","battery":42,"lat":37.7785,"lng":-122.4162,"locType":"GPS_EXACT","note":"Injured hiker, leg fracture"}"""
            val packet = MeshPacket(
                packetType = PacketType.SOS_ALERT,
                priority = PacketPriority.CRITICAL_SOS,
                hopLimit = 7,
                hopsTraveled = 1,
                senderId = "NODE-B",
                recipientId = "BROADCAST",
                payload = sosJson.toByteArray(StandardCharsets.UTF_8)
            )
            _incomingPackets.emit(packet)
        }
    }

    override fun getSignalMetrics(): SignalMetrics {
        return SignalMetrics(
            rssiDbm = -72,
            snrDb = 9.4f,
            packetLossPercent = if (simulatePacketLoss) 25 else 0
        )
    }
}
