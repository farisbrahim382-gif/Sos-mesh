package com.example.mesh.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.mesh.MeshPacket
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

class BleLoraTransport(
    private val context: Context
) : MeshTransport {

    override val transportName: String = "ESP32 LoRa (SX1262/SX1276)"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(replay = 20, extraBufferCapacity = 50)
    override val incomingPackets: Flow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var currentMtu: Int = 23

    // Nordic UART Service (NUS) UUIDs commonly used by ESP32 LoRa serial bridges
    companion object {
        private const val TAG = "BleLoraTransport"
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_TX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // App -> ESP32
        val NUS_RX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // ESP32 -> App
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }

    private val receiveBuffer = ByteArrayOutputStream()
    private var lastRssi: Int = -75
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val rawName = device.name ?: "Unknown LoRa Device"
            val address = device.address ?: return
            val rssi = result.rssi

            val entry = DiscoveredBleDevice(name = rawName, address = address, rssi = rssi)
            _discoveredDevices.update { list ->
                if (list.none { it.address == address }) {
                    list + entry
                } else {
                    list.map { if (it.address == address) entry else it }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed with errorCode: $errorCode")
            _connectionState.value = ConnectionState.ERROR
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun start() {
        startScan()
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        try {
            _discoveredDevices.value = emptyList()
            _connectionState.value = ConnectionState.SCANNING
            isScanning = true
            scanner.startScan(scanCallback)

            // Auto-stop scanning after 15 seconds to preserve battery
            scope.launch {
                delay(15_000)
                if (isScanning) {
                    stopScan()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLE permissions for scanning", e)
            _connectionState.value = ConnectionState.ERROR
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: SecurityException) {}
        isScanning = false
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING
        _connectedDeviceName.value = device.name ?: device.address
        try {
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException connecting to GATT", e)
            _connectionState.value = ConnectionState.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun connectByAddress(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device != null) {
            connectToDevice(device)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to ESP32 GATT server. Requesting MTU 512...")
                _connectionState.value = ConnectionState.CONNECTING
                // Request higher MTU for unfragmented LoRa packet transfer
                gatt?.requestMtu(512)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from ESP32 GATT server.")
                _connectionState.value = ConnectionState.DISCONNECTED
                _connectedDeviceName.value = null
                txCharacteristic = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                currentMtu = mtu
                Log.i(TAG, "Negotiated MTU: $mtu")
            }
            gatt?.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(NUS_SERVICE_UUID)
                if (service != null) {
                    txCharacteristic = service.getCharacteristic(NUS_TX_CHAR_UUID)
                    val rxChar = service.getCharacteristic(NUS_RX_CHAR_UUID)
                    if (rxChar != null) {
                        gatt.setCharacteristicNotification(rxChar, true)
                        val descriptor = rxChar.getDescriptor(CCCD_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt.readRemoteRssi()
                } else {
                    Log.w(TAG, "NUS service not found on device; operating in generic BLE connected mode")
                    _connectionState.value = ConnectionState.CONNECTED
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val bytes = characteristic?.value ?: return
            handleIncomingBytes(bytes)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastRssi = rssi
            }
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        synchronized(receiveBuffer) {
            receiveBuffer.write(bytes)
            val bufferBytes = receiveBuffer.toByteArray()
            val packet = MeshPacket.fromByteArray(bufferBytes)
            if (packet != null) {
                receiveBuffer.reset()
                scope.launch {
                    _incomingPackets.emit(packet)
                }
            } else if (receiveBuffer.size() > 1024) {
                receiveBuffer.reset()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendPacket(packet: MeshPacket): Boolean {
        val tx = txCharacteristic ?: return false
        val gatt = bluetoothGatt ?: return false
        val rawBytes = packet.toByteArray()

        val maxChunk = (currentMtu - 3).coerceIn(20, 509)
        return try {
            var offset = 0
            while (offset < rawBytes.size) {
                val end = (offset + maxChunk).coerceAtMost(rawBytes.size)
                val chunk = rawBytes.copyOfRange(offset, end)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(tx, chunk, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    tx.value = chunk
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(tx)
                }
                offset = end
                if (offset < rawBytes.size) delay(20)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error transmitting packet via BLE NUS", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stop() {
        stopScan()
        try {
            bluetoothGatt?.close()
            bluetoothGatt = null
            txCharacteristic = null
            _connectedDeviceName.value = null
        } catch (_: SecurityException) {}
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun getSignalMetrics(): SignalMetrics {
        return SignalMetrics(
            rssiDbm = lastRssi,
            snrDb = 7.8f,
            packetLossPercent = 0
        )
    }
}
