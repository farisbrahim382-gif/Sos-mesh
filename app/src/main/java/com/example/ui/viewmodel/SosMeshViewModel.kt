package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SosMeshApplication
import com.example.data.local.EmergencyAlertEntity
import com.example.data.local.MeshMessageEntity
import com.example.data.local.MeshNodeEntity
import com.example.data.local.RadioConfigEntity
import com.example.data.repository.MeshRepository
import com.example.mesh.PacketPriority
import com.example.mesh.transport.ConnectionState
import com.example.mesh.transport.DiscoveredBleDevice
import com.example.mesh.transport.TransportChannel
import com.example.mesh.transport.TransportStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class RepoData(
    val nodes: List<MeshNodeEntity>,
    val messages: List<MeshMessageEntity>,
    val alerts: List<EmergencyAlertEntity>,
    val config: RadioConfigEntity?,
    val connState: ConnectionState,
    val bleDevices: List<DiscoveredBleDevice>,
    val connectedBleName: String?,
    val transports: List<TransportStatus>
)

data class SosMeshUiState(
    val localNodeId: String = "",
    val activeTransport: String = "Hybrid Mesh Engine",
    val connectionState: ConnectionState = ConnectionState.CONNECTED,
    val batteryPercent: Int = 85,
    val latitude: Double? = 37.7749,
    val longitude: Double? = -122.4194,
    val locationSource: String = "GPS_EXACT",
    val nodes: List<MeshNodeEntity> = emptyList(),
    val messages: List<MeshMessageEntity> = emptyList(),
    val activeAlerts: List<EmergencyAlertEntity> = emptyList(),
    val radioConfig: RadioConfigEntity? = null,
    val selectedChannel: String = "#all-mesh-broadcast",
    val isSosCountdownActive: Boolean = false,
    val sosCountdownSeconds: Int = 3,
    val isSosActive: Boolean = false,
    val activeSosType: String = "MEDICAL",
    val simulatedLossEnabled: Boolean = false,
    val selectedLanguage: String = "en",
    val discoveredBleDevices: List<DiscoveredBleDevice> = emptyList(),
    val connectedBleDeviceName: String? = null,
    val transportsStatus: List<TransportStatus> = emptyList()
)

class SosMeshViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SosMeshApplication
    private val repository: MeshRepository = app.repository

    private val _uiState = MutableStateFlow(
        SosMeshUiState(
            localNodeId = repository.localNodeId,
            activeTransport = repository.meshEngine.getActiveTransport().transportName,
            batteryPercent = repository.meshEngine.localBatteryPercent,
            latitude = repository.meshEngine.localLatitude,
            longitude = repository.meshEngine.localLongitude,
            locationSource = repository.meshEngine.localLocationSource
        )
    )

    private val repoFlow = combine(
        combine(
            repository.allNodes,
            repository.allMessages,
            repository.activeAlerts
        ) { nodes, messages, alerts -> Triple(nodes, messages, alerts) },
        combine(
            repository.radioConfig,
            repository.connectionState,
            repository.discoveredBleDevices,
            repository.connectedBleDeviceName
        ) { config, connState, bleDevices, bleName ->
            Quad(config, connState, bleDevices, bleName)
        },
        repository.transportsStatus
    ) { (nodes, messages, alerts), (config, connState, bleDevices, bleName), transports ->
        RepoData(nodes, messages, alerts, config, connState, bleDevices, bleName, transports)
    }

    val uiState: StateFlow<SosMeshUiState> = combine(
        _uiState,
        repoFlow
    ) { current, repo ->
        current.copy(
            nodes = repo.nodes,
            messages = repo.messages,
            activeAlerts = repo.alerts,
            radioConfig = repo.config,
            connectionState = repo.connState,
            discoveredBleDevices = repo.bleDevices,
            connectedBleDeviceName = repo.connectedBleName,
            transportsStatus = repo.transports,
            isSosActive = repo.alerts.any { it.originNodeId == current.localNodeId && it.isActive }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _uiState.value
    )

    private var countdownJob: Job? = null

    fun selectChannel(channel: String) {
        _uiState.update { it.copy(selectedChannel = channel) }
    }

    fun setSelectedSosType(type: String) {
        _uiState.update { it.copy(activeSosType = type) }
    }

    fun startSosCountdown(type: String, severity: String = "CRITICAL") {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                isSosCountdownActive = true,
                sosCountdownSeconds = 3,
                activeSosType = type
            )
        }

        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(sosCountdownSeconds = i) }
                delay(1000)
            }
            _uiState.update { it.copy(isSosCountdownActive = false, sosCountdownSeconds = 0) }
            broadcastSosAlert(type, severity)
        }
    }

    fun cancelSosCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.update { it.copy(isSosCountdownActive = false, sosCountdownSeconds = 3) }
    }

    fun broadcastSosAlert(type: String, severity: String = "CRITICAL", note: String = "") {
        viewModelScope.launch {
            repository.broadcastSos(emergencyType = type, severity = severity, note = note)
            _uiState.update { it.copy(isSosActive = true) }
        }
    }

    fun deactivateSosAlert() {
        viewModelScope.launch {
            val alerts = uiState.value.activeAlerts
            val ownAlert = alerts.find { it.originNodeId == uiState.value.localNodeId && it.isActive }
            if (ownAlert != null) {
                repository.deactivateAlert(ownAlert.alertId)
            }
            _uiState.update { it.copy(isSosActive = false) }
        }
    }

    fun sendMessage(content: String, priority: PacketPriority = PacketPriority.NORMAL) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(
                content = content.trim(),
                channelName = _uiState.value.selectedChannel,
                recipientId = "BROADCAST",
                priority = priority
            )
        }
    }

    fun updateCoordinates(latitude: Double, longitude: Double) {
        repository.updateCoordinates(latitude, longitude, "MANUAL")
        _uiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                locationSource = "MANUAL"
            )
        }
    }

    fun switchRegionalBand(band: String) {
        val freqHz = when (band) {
            "US915" -> 915_000_000L
            "EU868" -> 868_000_000L
            "AS923" -> 923_000_000L
            "ISM433" -> 433_175_000L
            else -> 915_000_000L
        }

        val current = uiState.value.radioConfig
        val updated = current?.copy(
            regionalBand = band,
            frequencyHz = freqHz
        ) ?: RadioConfigEntity(
            id = 1,
            activeTransport = "HYBRID_PHONE_MESH",
            regionalBand = band,
            frequencyHz = freqHz,
            bandwidthKhz = 125,
            spreadingFactor = 9,
            txPowerDbm = 20,
            connectedDeviceName = "Standalone Phone-to-Phone Mesh",
            connectedDeviceAddress = "PHONE_MESH_ACTIVE",
            isConnected = true
        )

        viewModelScope.launch {
            repository.saveRadioConfig(updated)
        }
    }

    fun setTransportChannelEnabled(channel: TransportChannel, enabled: Boolean) {
        repository.setTransportChannelEnabled(channel, enabled)
    }

    fun startBleScan() {
        repository.startBleScan()
    }

    fun stopBleScan() {
        repository.stopBleScan()
    }

    fun connectBleDevice(address: String) {
        repository.connectBleDevice(address)
    }

    fun injectVirtualHopTest() {
        repository.injectVirtualHopTest()
    }

    fun injectVirtualSos() {
        repository.injectVirtualSos()
    }

    fun toggleSimulatedPacketLoss() {
        val newState = !_uiState.value.simulatedLossEnabled
        _uiState.update { it.copy(simulatedLossEnabled = newState) }
        repository.toggleSimulatedPacketLoss(newState)
    }

    fun toggleLanguage() {
        _uiState.update {
            val next = if (it.selectedLanguage == "en") "ar" else "en"
            it.copy(selectedLanguage = next)
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
