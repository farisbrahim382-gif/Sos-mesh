package com.example.data.repository

import com.example.data.local.EmergencyAlertEntity
import com.example.data.local.MeshDao
import com.example.data.local.MeshMessageEntity
import com.example.data.local.MeshNodeEntity
import com.example.data.local.RadioConfigEntity
import com.example.mesh.MeshEngine
import com.example.mesh.PacketPriority
import com.example.mesh.transport.ConnectionState
import com.example.mesh.transport.DiscoveredBleDevice
import com.example.mesh.transport.HybridMeshRouter
import com.example.mesh.transport.MeshTransport
import com.example.mesh.transport.OptionalLoRaTransport
import com.example.mesh.transport.TransportChannel
import com.example.mesh.transport.TransportStatus
import com.example.mesh.transport.VirtualSimulationTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class MeshRepository(
    val meshEngine: MeshEngine,
    private val meshDao: MeshDao,
    val hybridRouter: HybridMeshRouter,
    val optionalLoRaTransport: OptionalLoRaTransport
) {
    val allNodes: Flow<List<MeshNodeEntity>> = meshDao.getAllNodes()
    val allMessages: Flow<List<MeshMessageEntity>> = meshDao.getAllMessages()
    val activeAlerts: Flow<List<EmergencyAlertEntity>> = meshDao.getActiveAlerts()
    val radioConfig: Flow<RadioConfigEntity?> = meshDao.getRadioConfig()

    val localNodeId: String = meshEngine.localNodeId
    val isMeshActive: StateFlow<Boolean> = meshEngine.isMeshActive
    val activeTransportName: StateFlow<String> = meshEngine.activeTransportName
    val connectionState: StateFlow<ConnectionState> = meshEngine.connectionState

    val transportsStatus: StateFlow<List<TransportStatus>> = hybridRouter.transportsStatus
    val discoveredBleDevices: StateFlow<List<DiscoveredBleDevice>> = optionalLoRaTransport.discoveredDevices
    val connectedBleDeviceName: StateFlow<String?> = optionalLoRaTransport.connectedDeviceName

    suspend fun broadcastSos(
        emergencyType: String,
        severity: String = "CRITICAL",
        note: String = ""
    ): String {
        return meshEngine.broadcastSos(emergencyType, severity, note)
    }

    suspend fun deactivateAlert(alertId: String) {
        meshDao.deactivateAlert(alertId)
    }

    suspend fun sendMessage(
        content: String,
        channelName: String = "#all-mesh-broadcast",
        recipientId: String = "BROADCAST",
        priority: PacketPriority = PacketPriority.NORMAL
    ): String {
        return meshEngine.sendMessage(content, channelName, recipientId, priority)
    }

    suspend fun saveRadioConfig(config: RadioConfigEntity) {
        meshDao.saveRadioConfig(config)
    }

    fun updateCoordinates(latitude: Double?, longitude: Double?, source: String) {
        meshEngine.localLatitude = latitude
        meshEngine.localLongitude = longitude
        meshEngine.localLocationSource = source
    }

    fun switchTransport(transport: MeshTransport) {
        meshEngine.switchTransport(transport)
    }

    fun setTransportChannelEnabled(channel: TransportChannel, enabled: Boolean) {
        hybridRouter.setChannelEnabled(channel, enabled)
    }

    fun startBleScan() {
        hybridRouter.setChannelEnabled(TransportChannel.OPTIONAL_LORA, true)
        optionalLoRaTransport.startScan()
    }

    fun stopBleScan() {
        optionalLoRaTransport.stopScan()
    }

    fun connectBleDevice(address: String) {
        hybridRouter.setChannelEnabled(TransportChannel.OPTIONAL_LORA, true)
        optionalLoRaTransport.connectByAddress(address)
    }

    fun injectVirtualHopTest() {
        hybridRouter.virtualTestbench.injectMultiHopTestPacket()
    }

    fun injectVirtualSos() {
        hybridRouter.virtualTestbench.injectVirtualSosBeacon()
    }

    fun toggleSimulatedPacketLoss(simulate: Boolean) {
        hybridRouter.virtualTestbench.simulatePacketLoss = simulate
    }
}
