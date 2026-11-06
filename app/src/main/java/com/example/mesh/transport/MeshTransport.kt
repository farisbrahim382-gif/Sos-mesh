package com.example.mesh.transport

import com.example.mesh.MeshPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class SignalMetrics(
    val rssiDbm: Int = -75,
    val snrDb: Float = 8.5f,
    val packetLossPercent: Int = 0
)

interface MeshTransport {
    val transportName: String
    val connectionState: StateFlow<ConnectionState>
    val incomingPackets: Flow<MeshPacket>

    suspend fun start()
    suspend fun stop()
    suspend fun sendPacket(packet: MeshPacket): Boolean
    fun getSignalMetrics(): SignalMetrics
}
