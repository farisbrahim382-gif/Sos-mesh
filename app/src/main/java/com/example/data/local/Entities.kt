package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_nodes")
data class MeshNodeEntity(
    @PrimaryKey val nodeId: String, // e.g. "NODE-A1B2"
    val alias: String,
    val hardwareModel: String,
    val batteryPercent: Int,
    val hopCount: Int, // 1 = Direct neighbor, 2+ = Multi-hop
    val rssi: Int,
    val snr: Float,
    val latitude: Double?,
    val longitude: Double?,
    val locationSource: String, // "GPS_EXACT", "MANUAL", "ESTIMATED", "UNKNOWN"
    val isDirectNeighbor: Boolean,
    val lastSeenTimestamp: Long
)

@Entity(tableName = "mesh_messages")
data class MeshMessageEntity(
    @PrimaryKey val messageId: String,
    val senderNodeId: String,
    val recipientNodeId: String, // "BROADCAST" or target nodeId
    val channelName: String, // e.g. "#all-mesh-broadcast", "#medical-response"
    val content: String,
    val priority: Int, // 0 = NORMAL, 1 = HIGH, 2 = SOS
    val hopLimit: Int,
    val hopsTraveled: Int,
    val deliveryStatus: String, // "QUEUED", "TRANSMITTED", "RELAYED", "ACKNOWLEDGED", "FAILED"
    val transportType: String, // "LORA_ESP32", "MESHTASTIC", "BLE_DIRECT", "VIRTUAL"
    val timestamp: Long
)

@Entity(tableName = "emergency_alerts")
data class EmergencyAlertEntity(
    @PrimaryKey val alertId: String,
    val originNodeId: String,
    val emergencyType: String, // "MEDICAL", "LOST", "FIRE", "ACCIDENT", "DISASTER", "SECURITY", "GENERAL"
    val severity: String, // "CRITICAL", "URGENT", "ADVISORY"
    val batteryPercent: Int,
    val latitude: Double?,
    val longitude: Double?,
    val locationType: String,
    val acknowledgedNodes: String, // Comma-separated node IDs that confirmed receipt
    val isActive: Boolean,
    val timestamp: Long
)

@Entity(tableName = "radio_config")
data class RadioConfigEntity(
    @PrimaryKey val id: Int = 1,
    val activeTransport: String, // "VIRTUAL", "ESP32_LORA", "MESHTASTIC", "BLE_DIRECT"
    val regionalBand: String, // "US915", "EU868", "AS923", "ISM433"
    val frequencyHz: Long,
    val bandwidthKhz: Int,
    val spreadingFactor: Int,
    val txPowerDbm: Int,
    val connectedDeviceName: String?,
    val connectedDeviceAddress: String?,
    val isConnected: Boolean
)

@Entity(tableName = "pending_packets")
data class PendingPacketEntity(
    @PrimaryKey val packetId: String,
    val rawPacketHex: String,
    val priority: Int,
    val retryCount: Int,
    val maxRetries: Int,
    val createdAt: Long,
    val nextRetryAt: Long
)
