package com.example

import android.app.Application
import com.example.data.local.RadioConfigEntity
import com.example.data.local.SosMeshDatabase
import com.example.data.repository.MeshRepository
import com.example.mesh.MeshEngine
import com.example.mesh.transport.BluetoothClassicTransport
import com.example.mesh.transport.BluetoothLETransport
import com.example.mesh.transport.HybridMeshRouter
import com.example.mesh.transport.OptionalLoRaTransport
import com.example.mesh.transport.VirtualSimulationTransport
import com.example.mesh.transport.WifiDirectTransport
import com.example.mesh.transport.WifiLanTransport
import com.example.telemetry.BatteryMonitor
import com.example.telemetry.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SosMeshApplication : Application() {

    lateinit var database: SosMeshDatabase
        private set

    lateinit var btClassicTransport: BluetoothClassicTransport
        private set

    lateinit var blePhoneTransport: BluetoothLETransport
        private set

    lateinit var wifiLanTransport: WifiLanTransport
        private set

    lateinit var wifiDirectTransport: WifiDirectTransport
        private set

    lateinit var optionalLoRaTransport: OptionalLoRaTransport
        private set

    lateinit var virtualTransport: VirtualSimulationTransport
        private set

    lateinit var hybridRouter: HybridMeshRouter
        private set

    lateinit var meshEngine: MeshEngine
        private set

    lateinit var repository: MeshRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = SosMeshDatabase.getInstance(this)
        val dao = database.meshDao()

        // 1. Phone-to-Phone Transports (Primary standalone technologies)
        btClassicTransport = BluetoothClassicTransport(this)
        blePhoneTransport = BluetoothLETransport(this)
        wifiLanTransport = WifiLanTransport(this)
        wifiDirectTransport = WifiDirectTransport(this)

        // 2. Hardware Extensions & Testbench
        optionalLoRaTransport = OptionalLoRaTransport(this)
        virtualTransport = VirtualSimulationTransport()

        // 3. Hybrid Unified Mesh Router
        hybridRouter = HybridMeshRouter(
            btClassicTransport = btClassicTransport,
            blePhoneTransport = blePhoneTransport,
            wifiLanTransport = wifiLanTransport,
            wifiDirectTransport = wifiDirectTransport,
            optionalLoRaTransport = optionalLoRaTransport,
            virtualTestbench = virtualTransport
        )

        // 4. Decentralized Mesh Engine
        meshEngine = MeshEngine(
            meshDao = dao,
            activeTransport = hybridRouter
        )

        // Read initial device telemetry
        val battery = BatteryMonitor.getBatteryLevel(this)
        val location = LocationHelper.getLastKnownGpsLocation(this)
        meshEngine.localBatteryPercent = battery
        meshEngine.localLatitude = location.latitude
        meshEngine.localLongitude = location.longitude
        meshEngine.localLocationSource = location.source

        // 5. Repository Layer
        repository = MeshRepository(
            meshEngine = meshEngine,
            meshDao = dao,
            hybridRouter = hybridRouter,
            optionalLoRaTransport = optionalLoRaTransport
        )

        // Seed initial radio configuration and start hybrid engine
        CoroutineScope(Dispatchers.IO).launch {
            hybridRouter.start()
            val initialConfig = RadioConfigEntity(
                id = 1,
                activeTransport = "HYBRID_PHONE_MESH",
                regionalBand = "US915",
                frequencyHz = 915_000_000L,
                bandwidthKhz = 125,
                spreadingFactor = 9,
                txPowerDbm = 20,
                connectedDeviceName = "Standalone Phone-to-Phone Mesh",
                connectedDeviceAddress = "PHONE_MESH_ACTIVE",
                isConnected = true
            )
            dao.saveRadioConfig(initialConfig)
        }
    }
}
