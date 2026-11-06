package com.example.mesh.transport

import android.content.Context
import com.example.mesh.MeshPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class OptionalLoRaTransport(
    context: Context
) : MeshTransport {

    override val transportName: String = "Optional LoRa (ESP32 SX1262/SX1276)"

    val bleLoraTransport = BleLoraTransport(context)

    override val connectionState: StateFlow<ConnectionState> = bleLoraTransport.connectionState
    override val incomingPackets: Flow<MeshPacket> = bleLoraTransport.incomingPackets

    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = bleLoraTransport.discoveredDevices
    val connectedDeviceName: StateFlow<String?> = bleLoraTransport.connectedDeviceName

    override suspend fun start() {
        bleLoraTransport.start()
    }

    override suspend fun stop() {
        bleLoraTransport.stop()
    }

    override suspend fun sendPacket(packet: MeshPacket): Boolean {
        return bleLoraTransport.sendPacket(packet)
    }

    override fun getSignalMetrics(): SignalMetrics {
        return bleLoraTransport.getSignalMetrics()
    }

    fun startScan() {
        bleLoraTransport.startScan()
    }

    fun stopScan() {
        bleLoraTransport.stopScan()
    }

    fun connectByAddress(address: String) {
        bleLoraTransport.connectByAddress(address)
    }
}
