package com.example.mesh

import android.util.Log
import com.example.data.local.EmergencyAlertEntity
import com.example.data.local.MeshDao
import com.example.data.local.MeshMessageEntity
import com.example.data.local.MeshNodeEntity
import com.example.data.local.PendingPacketEntity
import com.example.mesh.transport.ConnectionState
import com.example.mesh.transport.MeshTransport
import com.example.mesh.transport.VirtualSimulationTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

class MeshEngine(
    val localNodeId: String = "NODE-" + UUID.randomUUID().toString().substring(0, 4).uppercase(),
    private val meshDao: MeshDao,
    private var activeTransport: MeshTransport
) {
    companion object {
        private const val TAG = "MeshEngine"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val deduplicationCache = DeduplicationCache(maxEntries = 1000)

    private val _isMeshActive = MutableStateFlow(true)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    private val _activeTransportName = MutableStateFlow(activeTransport.transportName)
    val activeTransportName: StateFlow<String> = _activeTransportName.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    var localAlias: String = "Responder-1"
    var localBatteryPercent: Int = 85
    var localLatitude: Double? = 37.7749
    var localLongitude: Double? = -122.4194
    var localLocationSource: String = "GPS_EXACT"

    init {
        startTransportMonitoring()
        startHeartbeatLoop()
        startStoreAndForwardWorker()
    }

    fun switchTransport(newTransport: MeshTransport) {
        scope.launch {
            activeTransport.stop()
            activeTransport = newTransport
            _activeTransportName.value = newTransport.transportName
            newTransport.start()
            startTransportMonitoring()
        }
    }

    fun getActiveTransport(): MeshTransport = activeTransport

    private fun startTransportMonitoring() {
        scope.launch {
            activeTransport.connectionState.collect { state ->
                _connectionState.value = state
                if (state == ConnectionState.CONNECTED) {
                    processPendingQueue()
                }
            }
        }

        scope.launch {
            activeTransport.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }
    }

    private fun handleIncomingPacket(packet: MeshPacket) {
        // Step 1: Drop duplicate packets to prevent broadcast storm relay loops
        if (!deduplicationCache.markIfNew(packet.packetId)) {
            Log.d(TAG, "Dropped duplicate packet ${packet.packetId}")
            return
        }

        // Drop packets originating from self
        if (packet.senderId == localNodeId) {
            return
        }

        Log.i(TAG, "Processing packet ${packet.packetId} from ${packet.senderId}, type: ${packet.packetType}, hops: ${packet.hopsTraveled}")

        // Step 2: Route according to packet type
        when (packet.packetType) {
            PacketType.SOS_ALERT -> {
                handleIncomingSosAlert(packet)
            }
            PacketType.CHAT_MESSAGE -> {
                handleIncomingChatMessage(packet)
            }
            PacketType.NODE_HEARTBEAT -> {
                handleIncomingHeartbeat(packet)
            }
            PacketType.ACK_RECEIPT -> {
                handleIncomingAck(packet)
            }
            PacketType.LOCATION_TELEMETRY -> {
                handleIncomingLocationTelemetry(packet)
            }
        }

        // Step 3: Multi-Hop Relay Forwarding
        // If hopLimit has not been exceeded, relay the packet to neighbor nodes
        if (packet.hopsTraveled < packet.hopLimit) {
            forwardPacket(packet)
        }
    }

    private fun handleIncomingSosAlert(packet: MeshPacket) {
        scope.launch {
            val payloadStr = String(packet.payload, StandardCharsets.UTF_8)
            var emergencyType = "GENERAL"
            var severity = "CRITICAL"
            var battery = 50
            var lat: Double? = null
            var lng: Double? = null
            var locType = "UNKNOWN"

            try {
                val json = JSONObject(payloadStr)
                emergencyType = json.optString("type", "GENERAL")
                severity = json.optString("severity", "CRITICAL")
                battery = json.optInt("battery", 50)
                if (json.has("lat")) lat = json.optDouble("lat")
                if (json.has("lng")) lng = json.optDouble("lng")
                locType = json.optString("locType", "UNKNOWN")
            } catch (_: Exception) {
                emergencyType = "GENERAL"
            }

            val alertEntity = EmergencyAlertEntity(
                alertId = packet.packetId,
                originNodeId = packet.senderId,
                emergencyType = emergencyType,
                severity = severity,
                batteryPercent = battery,
                latitude = lat,
                longitude = lng,
                locationType = locType,
                acknowledgedNodes = localNodeId,
                isActive = true,
                timestamp = packet.timestamp
            )
            meshDao.insertAlert(alertEntity)

            // Also record in messages stream as high-priority alert
            val messageEntity = MeshMessageEntity(
                messageId = packet.packetId,
                senderNodeId = packet.senderId,
                recipientNodeId = packet.recipientId,
                channelName = "#all-mesh-broadcast",
                content = "[EMERGENCY SOS: $emergencyType ($severity)] Location: ${lat ?: "N/A"}, ${lng ?: "N/A"}",
                priority = 2,
                hopLimit = packet.hopLimit,
                hopsTraveled = packet.hopsTraveled,
                deliveryStatus = "RELAYED",
                transportType = activeTransport.transportName,
                timestamp = packet.timestamp
            )
            meshDao.insertMessage(messageEntity)

            // Send immediate ACK receipt back to sender
            sendAck(packet.senderId, packet.packetId)
        }
    }

    private fun handleIncomingChatMessage(packet: MeshPacket) {
        scope.launch {
            val content = String(packet.payload, StandardCharsets.UTF_8)
            val messageEntity = MeshMessageEntity(
                messageId = packet.packetId,
                senderNodeId = packet.senderId,
                recipientNodeId = packet.recipientId,
                channelName = packet.channel,
                content = content,
                priority = packet.priority.code.toInt(),
                hopLimit = packet.hopLimit,
                hopsTraveled = packet.hopsTraveled,
                deliveryStatus = if (packet.recipientId == localNodeId) "DELIVERED" else "RELAYED",
                transportType = activeTransport.transportName,
                timestamp = packet.timestamp
            )
            meshDao.insertMessage(messageEntity)

            if (packet.recipientId == localNodeId) {
                sendAck(packet.senderId, packet.packetId)
            }
        }
    }

    private fun handleIncomingHeartbeat(packet: MeshPacket) {
        scope.launch {
            val payloadStr = String(packet.payload, StandardCharsets.UTF_8)
            var alias = packet.senderId
            var battery = 80
            var hw = "ESP32 LoRa"
            var lat: Double? = null
            var lng: Double? = null
            var rssi = -75
            var snr = 8.0f

            try {
                val json = JSONObject(payloadStr)
                alias = json.optString("alias", packet.senderId)
                battery = json.optInt("battery", 80)
                hw = json.optString("hw", "ESP32 LoRa")
                if (json.has("lat")) lat = json.optDouble("lat")
                if (json.has("lng")) lng = json.optDouble("lng")
                rssi = json.optInt("rssi", -75)
                snr = json.optDouble("snr", 8.0).toFloat()
            } catch (_: Exception) {}

            val nodeEntity = MeshNodeEntity(
                nodeId = packet.senderId,
                alias = alias,
                hardwareModel = hw,
                batteryPercent = battery,
                hopCount = packet.hopsTraveled.coerceAtLeast(1),
                rssi = rssi,
                snr = snr,
                latitude = lat,
                longitude = lng,
                locationSource = if (lat != null) "GPS_EXACT" else "UNKNOWN",
                isDirectNeighbor = (packet.hopsTraveled <= 1),
                lastSeenTimestamp = System.currentTimeMillis()
            )
            meshDao.insertOrUpdateNode(nodeEntity)
        }
    }

    private fun handleIncomingAck(packet: MeshPacket) {
        scope.launch {
            val ackNote = String(packet.payload, StandardCharsets.UTF_8)
            meshDao.updateMessageStatus(packet.packetId, "ACKNOWLEDGED")
        }
    }

    private fun handleIncomingLocationTelemetry(packet: MeshPacket) {
        handleIncomingHeartbeat(packet)
    }

    private fun forwardPacket(packet: MeshPacket) {
        scope.launch {
            delay(100) // Small randomized backoff to avoid channel collision
            val forwarded = packet.copy(
                hopsTraveled = packet.hopsTraveled + 1
            )
            activeTransport.sendPacket(forwarded)
        }
    }

    suspend fun broadcastSos(
        emergencyType: String,
        severity: String = "CRITICAL",
        note: String = ""
    ): String {
        val alertId = UUID.randomUUID().toString().substring(0, 8)
        val sosJson = JSONObject().apply {
            put("type", emergencyType)
            put("severity", severity)
            put("battery", localBatteryPercent)
            if (localLatitude != null) put("lat", localLatitude)
            if (localLongitude != null) put("lng", localLongitude)
            put("locType", localLocationSource)
            put("note", note)
        }.toString()

        val packet = MeshPacket(
            packetId = alertId,
            packetType = PacketType.SOS_ALERT,
            priority = PacketPriority.CRITICAL_SOS,
            hopLimit = 7,
            hopsTraveled = 0,
            senderId = localNodeId,
            recipientId = "BROADCAST",
            channel = "#all-mesh-broadcast",
            payload = sosJson.toByteArray(StandardCharsets.UTF_8)
        )

        // Mark self seen in deduplication cache
        deduplicationCache.markIfNew(alertId)

        // Store locally in database
        val alertEntity = EmergencyAlertEntity(
            alertId = alertId,
            originNodeId = localNodeId,
            emergencyType = emergencyType,
            severity = severity,
            batteryPercent = localBatteryPercent,
            latitude = localLatitude,
            longitude = localLongitude,
            locationType = localLocationSource,
            acknowledgedNodes = "",
            isActive = true,
            timestamp = System.currentTimeMillis()
        )
        meshDao.insertAlert(alertEntity)

        val messageEntity = MeshMessageEntity(
            messageId = alertId,
            senderNodeId = localNodeId,
            recipientNodeId = "BROADCAST",
            channelName = "#all-mesh-broadcast",
            content = "[SOS ALERT - $emergencyType] $note | Bat: $localBatteryPercent%",
            priority = 2,
            hopLimit = 7,
            hopsTraveled = 0,
            deliveryStatus = "QUEUED",
            transportType = activeTransport.transportName,
            timestamp = System.currentTimeMillis()
        )
        meshDao.insertMessage(messageEntity)

        // Transmit immediately or queue if disconnected
        val sent = activeTransport.sendPacket(packet)
        meshDao.updateMessageStatus(alertId, if (sent) "TRANSMITTED" else "QUEUED")
        if (!sent) {
            enqueuePendingPacket(packet)
        }

        return alertId
    }

    suspend fun sendMessage(
        content: String,
        channelName: String = "#all-mesh-broadcast",
        recipientId: String = "BROADCAST",
        priority: PacketPriority = PacketPriority.NORMAL
    ): String {
        val messageId = UUID.randomUUID().toString().substring(0, 8)
        val packet = MeshPacket(
            packetId = messageId,
            packetType = PacketType.CHAT_MESSAGE,
            priority = priority,
            hopLimit = 7,
            hopsTraveled = 0,
            senderId = localNodeId,
            recipientId = recipientId,
            channel = channelName,
            payload = content.toByteArray(StandardCharsets.UTF_8)
        )

        deduplicationCache.markIfNew(messageId)

        val messageEntity = MeshMessageEntity(
            messageId = messageId,
            senderNodeId = localNodeId,
            recipientNodeId = recipientId,
            channelName = channelName,
            content = content,
            priority = priority.code.toInt(),
            hopLimit = 7,
            hopsTraveled = 0,
            deliveryStatus = "QUEUED",
            transportType = activeTransport.transportName,
            timestamp = System.currentTimeMillis()
        )
        meshDao.insertMessage(messageEntity)

        val sent = activeTransport.sendPacket(packet)
        meshDao.updateMessageStatus(messageId, if (sent) "TRANSMITTED" else "QUEUED")
        if (!sent) {
            enqueuePendingPacket(packet)
        }

        return messageId
    }

    private suspend fun sendAck(recipientId: String, targetPacketId: String) {
        val ackPacket = MeshPacket(
            packetType = PacketType.ACK_RECEIPT,
            priority = PacketPriority.HIGH,
            hopLimit = 7,
            hopsTraveled = 0,
            senderId = localNodeId,
            recipientId = recipientId,
            payload = "ACK:$targetPacketId".toByteArray(StandardCharsets.UTF_8)
        )
        activeTransport.sendPacket(ackPacket)
    }

    private suspend fun enqueuePendingPacket(packet: MeshPacket) {
        val hex = packet.toByteArray().joinToString("") { "%02X".format(it) }
        val pending = PendingPacketEntity(
            packetId = packet.packetId,
            rawPacketHex = hex,
            priority = packet.priority.code.toInt(),
            retryCount = 0,
            maxRetries = 5,
            createdAt = System.currentTimeMillis(),
            nextRetryAt = System.currentTimeMillis() + 3000
        )
        meshDao.enqueuePacket(pending)
    }

    private fun startHeartbeatLoop() {
        scope.launch {
            while (isActive) {
                delay(30_000) // Send heartbeat every 30 seconds
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    val heartbeatJson = JSONObject().apply {
                        put("alias", localAlias)
                        put("battery", localBatteryPercent)
                        if (localLatitude != null) put("lat", localLatitude)
                        if (localLongitude != null) put("lng", localLongitude)
                        put("hw", "Android SOS MESH")
                    }.toString()

                    val packet = MeshPacket(
                        packetType = PacketType.NODE_HEARTBEAT,
                        priority = PacketPriority.NORMAL,
                        hopLimit = 3,
                        hopsTraveled = 0,
                        senderId = localNodeId,
                        recipientId = "BROADCAST",
                        payload = heartbeatJson.toByteArray(StandardCharsets.UTF_8)
                    )
                    activeTransport.sendPacket(packet)
                }
            }
        }
    }

    private fun startStoreAndForwardWorker() {
        scope.launch {
            while (isActive) {
                delay(15_000)
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    processPendingQueue()
                }
            }
        }
    }

    private suspend fun processPendingQueue() {
        val pending = meshDao.getPendingPackets()
        for (item in pending) {
            try {
                val bytes = item.rawPacketHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val packet = MeshPacket.fromByteArray(bytes)
                if (packet != null) {
                    val sent = activeTransport.sendPacket(packet)
                    if (sent) {
                        meshDao.dequeuePacket(item.packetId)
                        meshDao.updateMessageStatus(item.packetId, "TRANSMITTED")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing queued packet", e)
            }
        }
    }
}
