package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.mesh.transport.ConnectionState
import com.example.mesh.transport.TransportChannel
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.MeshCyan
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TacticalCardBorder
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import com.example.ui.viewmodel.SosMeshUiState

@Composable
fun HardwareScreen(
    uiState: SosMeshUiState,
    onToggleTransportChannel: (TransportChannel, Boolean) -> Unit,
    onSelectRegionalBand: (String) -> Unit,
    onStartBleScan: () -> Unit,
    onStopBleScan: () -> Unit,
    onConnectBleDevice: (String) -> Unit,
    onInjectHopTest: () -> Unit,
    onInjectSosTest: () -> Unit,
    onTogglePacketLoss: () -> Unit
) {
    var showEsp32Firmware by remember { mutableStateOf(false) }

    val bands = listOf(
        "US915" to stringResource(R.string.freq_us915),
        "EU868" to stringResource(R.string.freq_eu868),
        "AS923" to stringResource(R.string.freq_as923),
        "ISM433" to stringResource(R.string.freq_433)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NEARBY DEVICES & MULTI-TRANSPORT MESH",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = MeshCyan
            )
            Text(
                text = "Autonomous Phone-to-Phone Wireless Links (Wi-Fi, Bluetooth) & Optional LoRa",
                style = MaterialTheme.typography.bodySmall,
                color = TacticalTextSecondary
            )
        }

        // Active Communication Engine Channels (Phone-to-Phone + Hardware)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "MULTI-TRANSPORT CHANNELS (HYBRID MESH)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    uiState.transportsStatus.forEach { transport ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TacticalSurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (transport.channel) {
                                        TransportChannel.WIFI_LAN -> Icons.Default.Wifi
                                        TransportChannel.WIFI_DIRECT -> Icons.Default.WifiTethering
                                        TransportChannel.BLUETOOTH_CLASSIC -> Icons.Default.Bluetooth
                                        TransportChannel.BLUETOOTH_LE -> Icons.Default.BluetoothConnected
                                        TransportChannel.OPTIONAL_LORA -> Icons.Default.CellTower
                                        TransportChannel.VIRTUAL_TESTBENCH -> Icons.Default.PlayArrow
                                    },
                                    contentDescription = null,
                                    tint = if (transport.isEnabled) MeshCyan else TacticalTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = transport.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (transport.isEnabled) TacticalTextPrimary else TacticalTextSecondary
                                    )
                                    Text(
                                        text = "Status: ${transport.connectionState.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = when (transport.connectionState) {
                                            ConnectionState.CONNECTED -> SecureGreen
                                            ConnectionState.CONNECTING, ConnectionState.SCANNING -> SignalAmber
                                            else -> TacticalTextSecondary
                                        }
                                    )
                                }
                            }

                            Switch(
                                checked = transport.isEnabled,
                                onCheckedChange = { enabled ->
                                    onToggleTransportChannel(transport.channel, enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MeshCyan,
                                    checkedTrackColor = MeshCyan.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("toggle_${transport.channel.name}")
                            )
                        }
                    }
                }
            }
        }

        // Optional External LoRa & BLE Hardware Manager
        val isLoRaEnabled = uiState.transportsStatus.find { it.channel == TransportChannel.OPTIONAL_LORA }?.isEnabled == true
        if (isLoRaEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshCyan))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CellTower,
                                    contentDescription = null,
                                    tint = MeshCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "OPTIONAL LORA TRANSCEIVER (ESP32 SX1262)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Nordic UART Service (NUS) UUID: 6E400001-B5A3-F393-E0A9-E50E24DCCA9E",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TacticalTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onStartBleScan,
                                colors = ButtonDefaults.buttonColors(containerColor = MeshCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scan_ble_btn")
                            ) {
                                Text(
                                    text = "SCAN FOR ESP32",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }

                            Button(
                                onClick = onStopBleScan,
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.6f)
                                    .testTag("stop_ble_scan_btn")
                            ) {
                                Text(
                                    text = "STOP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalTextPrimary
                                )
                            }
                        }

                        if (uiState.discoveredBleDevices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "DISCOVERED TRANSCEIVERS (${uiState.discoveredBleDevices.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = TacticalTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.discoveredBleDevices.forEach { dev ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = TacticalSurfaceVariant,
                                        border = CardDefaults.outlinedCardBorder(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = dev.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TacticalTextPrimary
                                                )
                                                Text(
                                                    text = "${dev.address} | RSSI: ${dev.rssi} dBm",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = TacticalTextSecondary
                                                )
                                            }

                                            Button(
                                                onClick = { onConnectBleDevice(dev.address) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SecureGreen),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.testTag("connect_ble_${dev.address}")
                                            ) {
                                                Text("CONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Regional Frequency Plan Preset Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.freq_preset_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val currentBand = uiState.radioConfig?.regionalBand ?: "US915"

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bands.forEach { (bandKey, label) ->
                            val isSelected = (currentBand == bandKey)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TacticalSurfaceVariant else TacticalSurface,
                                border = if (isSelected) {
                                    CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshCyan))
                                } else {
                                    CardDefaults.outlinedCardBorder()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectRegionalBand(bandKey) }
                                    .testTag("band_option_$bandKey")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MeshCyan else TacticalTextPrimary
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MeshCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3-Device Testbench Interactive Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = SignalAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.testbench_header),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TacticalTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.testbench_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TacticalTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onInjectHopTest,
                            colors = ButtonDefaults.buttonColors(containerColor = MeshCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inject_hop_test_btn")
                        ) {
                            Text(
                                text = "INJECT 3-HOP MSG",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        Button(
                            onClick = onInjectSosTest,
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inject_sos_test_btn")
                        ) {
                            Text(
                                text = "INJECT SOS PACKET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.testbench_drop_packet_toggle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TacticalTextPrimary
                        )
                        Switch(
                            checked = uiState.simulatedLossEnabled,
                            onCheckedChange = { onTogglePacketLoss() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SignalAmber,
                                checkedTrackColor = SignalAmber.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("packet_loss_switch")
                        )
                    }
                }
            }
        }

        // ESP32 Companion Firmware Sketch Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEsp32Firmware = !showEsp32Firmware },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MeshCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.esp32_firmware_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TacticalTextPrimary
                            )
                        }
                        Text(
                            text = if (showEsp32Firmware) "HIDE" else "VIEW CODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MeshCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.esp32_firmware_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TacticalTextSecondary
                    )

                    AnimatedVisibility(visible = showEsp32Firmware) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = """
// ESP32 + SX1262 LoRa BLE Transceiver Firmware (Nordic UART Service)
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <RadioLib.h>

SX1262 radio = new Module(8, 14, 12, 13);
#define SERVICE_UUID "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define RX_UUID      "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define TX_UUID      "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

BLECharacteristic *pTxCharacteristic;

class BleRxCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pChar) {
        String rx = pChar->getValue();
        if (rx.length() > 0) {
            radio.transmit((uint8_t*)rx.data(), rx.length());
        }
    }
};

void setup() {
  Serial.begin(115200);
  radio.begin(915.0, 125.0, 9, 7, 0x12, 22);
  BLEDevice::init("ESP32-LORA-SX1262");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(TX_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());
  BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(RX_UUID, BLECharacteristic::PROPERTY_WRITE);
  pRxCharacteristic->setCallbacks(new BleRxCallbacks());
  pService->start();
  pServer->getAdvertising()->start();
}

void loop() {
  uint8_t buf[256];
  int state = radio.readData(buf, sizeof(buf));
  if (state == RADIOLIB_ERR_NONE) {
    pTxCharacteristic->setValue(buf, radio.getPacketLength());
    pTxCharacteristic->notify();
  }
}
                                    """.trimIndent(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = SecureGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diagnostics & Cryptographic Security Specs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.diagnostics_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sec_cipher),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.sec_integrity),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.sec_replay),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.storage_persisted),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = SecureGreen
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
