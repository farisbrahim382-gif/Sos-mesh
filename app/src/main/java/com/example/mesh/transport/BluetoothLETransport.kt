package com.example.mesh.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
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
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluetoothLETransport(
    private val context: Context
) : MeshTransport {

    override val transportName: String = "Bluetooth LE Phone-to-Phone"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(replay = 20, extraBufferCapacity = 50)
    override val incomingPackets: Flow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private val connectedPeers = ConcurrentHashMap<String, BluetoothGatt>()

    companion object {
        private const val TAG = "BlePhoneTransport"
        val MESH_SERVICE_UUID: UUID = UUID.fromString("0000FEBB-0000-1000-8000-00805F9B34FB")
        val MESH_CHAR_UUID: UUID = UUID.fromString("0000FEBC-0000-1000-8000-00805F9B34FB")
    }

    @SuppressLint("MissingPermission")
    override suspend fun start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        _connectionState.value = ConnectionState.CONNECTED
        setupGattServer()
        startAdvertising()
        startScanningForPeers()
    }

    @SuppressLint("MissingPermission")
    private fun setupGattServer() {
        try {
            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
            val service = BluetoothGattService(MESH_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val char = BluetoothGattCharacteristic(
                MESH_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            service.addCharacteristic(char)
            gattServer?.addService(service)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up GATT server", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting advertising", e)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(TAG, "BLE Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "BLE Advertising failed with code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanningForPeers() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE scanner", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val address = device.address ?: return
            if (!connectedPeers.containsKey(address)) {
                device.connectGatt(context, false, peerGattCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (responseNeeded) {
                @SuppressLint("MissingPermission")
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
            if (value != null) {
                val packet = MeshPacket.fromByteArray(value)
                if (packet != null) {
                    scope.launch {
                        _incomingPackets.emit(packet)
                    }
                }
            }
        }
    }

    private val peerGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val address = gatt?.device?.address ?: return
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedPeers[address] = gatt
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedPeers.remove(address)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendPacket(packet: MeshPacket): Boolean {
        if (connectedPeers.isEmpty()) return false
        val bytes = packet.toByteArray()
        var success = false

        for ((_, gatt) in connectedPeers) {
            val service = gatt.getService(MESH_SERVICE_UUID)
            val char = service?.getCharacteristic(MESH_CHAR_UUID)
            if (char != null) {
                char.value = bytes
                val wrote = gatt.writeCharacteristic(char)
                if (wrote) success = true
            }
        }
        return success
    }

    @SuppressLint("MissingPermission")
    override suspend fun stop() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            gattServer?.close()
            for ((_, gatt) in connectedPeers) {
                gatt.close()
            }
            connectedPeers.clear()
        } catch (_: Exception) {}
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun getSignalMetrics(): SignalMetrics {
        return SignalMetrics(
            rssiDbm = -70,
            snrDb = 10.0f,
            packetLossPercent = 0
        )
    }
}
