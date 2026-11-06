package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshDao {

    // --- Mesh Nodes ---
    @Query("SELECT * FROM mesh_nodes ORDER BY lastSeenTimestamp DESC")
    fun getAllNodes(): Flow<List<MeshNodeEntity>>

    @Query("SELECT * FROM mesh_nodes WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getNodeById(nodeId: String): MeshNodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNode(node: MeshNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<MeshNodeEntity>)

    @Query("DELETE FROM mesh_nodes WHERE nodeId = :nodeId")
    suspend fun deleteNode(nodeId: String)

    // --- Messages ---
    @Query("SELECT * FROM mesh_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MeshMessageEntity>>

    @Query("SELECT * FROM mesh_messages WHERE channelName = :channel ORDER BY timestamp ASC")
    fun getMessagesForChannel(channel: String): Flow<List<MeshMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MeshMessageEntity)

    @Query("UPDATE mesh_messages SET deliveryStatus = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    // --- Emergency Alerts ---
    @Query("SELECT * FROM emergency_alerts WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<EmergencyAlertEntity>>

    @Query("SELECT * FROM emergency_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<EmergencyAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: EmergencyAlertEntity)

    @Query("UPDATE emergency_alerts SET isActive = 0 WHERE alertId = :alertId")
    suspend fun deactivateAlert(alertId: String)

    @Query("UPDATE emergency_alerts SET acknowledgedNodes = :nodes WHERE alertId = :alertId")
    suspend fun updateAlertAcks(alertId: String, nodes: String)

    // --- Radio Config ---
    @Query("SELECT * FROM radio_config WHERE id = 1 LIMIT 1")
    fun getRadioConfig(): Flow<RadioConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRadioConfig(config: RadioConfigEntity)

    // --- Pending Packets (Store and Forward Queue) ---
    @Query("SELECT * FROM pending_packets ORDER BY priority DESC, createdAt ASC")
    suspend fun getPendingPackets(): List<PendingPacketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueuePacket(packet: PendingPacketEntity)

    @Query("DELETE FROM pending_packets WHERE packetId = :packetId")
    suspend fun dequeuePacket(packetId: String)

    @Query("DELETE FROM pending_packets WHERE retryCount >= maxRetries")
    suspend fun purgeExpiredPackets()
}
