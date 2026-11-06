package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mesh.transport.ConnectionState
import com.example.ui.navigation.Screen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HardwareScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.RadarScreen
import com.example.ui.screens.SosScreen
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.MeshCyan
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SosMeshTheme
import com.example.ui.theme.TacticalCardBorder
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import com.example.ui.viewmodel.SosMeshViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SosMeshViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

            // Dynamic RTL / LTR layout direction based on selected language
            val layoutDirection = if (uiState.selectedLanguage == "ar") {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            // Permissions launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Handled gracefully */ }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }

            SosMeshTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    BackHandler(enabled = currentScreen != Screen.Dashboard) {
                        currentScreen = Screen.Dashboard
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (uiState.connectionState) {
                                                        ConnectionState.CONNECTED -> SecureGreen
                                                        ConnectionState.CONNECTING,
                                                        ConnectionState.SCANNING -> SignalAmber
                                                        else -> EmergencyCrimson
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = TacticalTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "[${uiState.localNodeId}]",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = MeshCyan
                                        )
                                    }
                                },
                                actions = {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .clip(CircleShape)
                                            .background(TacticalSurfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.isSosActive) "SOS BROADCASTING" else "RELAY READY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.isSosActive) EmergencyCrimson else SecureGreen
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = TacticalSurface,
                                    titleContentColor = TacticalTextPrimary
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = TacticalSurface,
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .testTag("bottom_nav_bar")
                            ) {
                                Screen.bottomNavItems.forEach { screen ->
                                    val isSelected = (currentScreen == screen)
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = stringResource(screen.titleRes),
                                                tint = if (isSelected) {
                                                    if (screen == Screen.Sos) EmergencyCrimson else MeshCyan
                                                } else {
                                                    TacticalTextSecondary
                                                }
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(screen.titleRes),
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) {
                                                    if (screen == Screen.Sos) EmergencyCrimson else MeshCyan
                                                } else {
                                                    TacticalTextSecondary
                                                }
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = TacticalSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_item_${screen.route}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                Screen.Dashboard -> DashboardScreen(
                                    uiState = uiState,
                                    onNavigateToSos = { currentScreen = Screen.Sos },
                                    onNavigateToMessages = { currentScreen = Screen.Messages },
                                    onNavigateToRadar = { currentScreen = Screen.Radar },
                                    onNavigateToHardware = { currentScreen = Screen.Hardware },
                                    onToggleLanguage = { viewModel.toggleLanguage() }
                                )
                                Screen.Sos -> SosScreen(
                                    uiState = uiState,
                                    onStartCountdown = { type, severity ->
                                        viewModel.startSosCountdown(type, severity)
                                    },
                                    onCancelCountdown = { viewModel.cancelSosCountdown() },
                                    onDeactivateSos = { viewModel.deactivateSosAlert() }
                                )
                                Screen.Messages -> MessagesScreen(
                                    uiState = uiState,
                                    onSelectChannel = { viewModel.selectChannel(it) },
                                    onSendMessage = { text, priority ->
                                        viewModel.sendMessage(text, priority)
                                    }
                                )
                                Screen.Radar -> RadarScreen(
                                    uiState = uiState,
                                    onUpdateCoordinates = { lat, lng ->
                                        viewModel.updateCoordinates(lat, lng)
                                    }
                                )
                                Screen.Hardware -> HardwareScreen(
                                    uiState = uiState,
                                    onToggleTransportChannel = { channel, enabled ->
                                        viewModel.setTransportChannelEnabled(channel, enabled)
                                    },
                                    onSelectRegionalBand = { viewModel.switchRegionalBand(it) },
                                    onStartBleScan = { viewModel.startBleScan() },
                                    onStopBleScan = { viewModel.stopBleScan() },
                                    onConnectBleDevice = { viewModel.connectBleDevice(it) },
                                    onInjectHopTest = { viewModel.injectVirtualHopTest() },
                                    onInjectSosTest = { viewModel.injectVirtualSos() },
                                    onTogglePacketLoss = { viewModel.toggleSimulatedPacketLoss() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
