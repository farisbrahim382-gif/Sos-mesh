package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
fun DashboardScreen(
    uiState: SosMeshUiState,
    onNavigateToSos: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToHardware: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Tactical Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MeshCyan
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SecureGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.node_id_label)}: ${uiState.localNodeId}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = TacticalTextSecondary
                        )
                    }
                }

                // Language toggle pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TacticalSurfaceVariant,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .clickable { onToggleLanguage() }
                        .testTag("language_toggle_btn")
                ) {
                    Text(
                        text = stringResource(R.string.toggle_language),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TacticalTextPrimary
                    )
                }
            }
        }

        // Active Emergency SOS Banner (if any alert is active in the mesh)
        item {
            AnimatedVisibility(visible = uiState.activeAlerts.isNotEmpty()) {
                val activeAlert = uiState.activeAlerts.firstOrNull()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, EmergencyCrimson, RoundedCornerShape(12.dp))
                        .testTag("active_sos_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = EmergencyCrimson.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(EmergencyCrimson)
                                .alpha(pulseAlpha)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SOS ALERT: ${activeAlert?.emergencyType ?: "DISTRESS"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmergencyCrimson
                            )
                            Text(
                                text = "Origin: ${activeAlert?.originNodeId ?: "UNKNOWN"} | Battery: ${activeAlert?.batteryPercent ?: 0}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TacticalTextPrimary
                            )
                        }
                        Button(
                            onClick = onNavigateToSos,
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("sos_banner_action_btn")
                        ) {
                            Text("VIEW", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Telemetry HUD Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SYSTEM TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Transceiver
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CellTower,
                                    contentDescription = null,
                                    tint = MeshCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TRANSCEIVER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalTextSecondary
                                )
                            }
                            Text(
                                text = uiState.activeTransport.take(18),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TacticalTextPrimary
                            )
                        }

                        // Battery
                        Column(modifier = Modifier.weight(0.7f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.BatteryFull,
                                    contentDescription = null,
                                    tint = SecureGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BATTERY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalTextSecondary
                                )
                            }
                            Text(
                                text = "${uiState.batteryPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SecureGreen
                            )
                        }

                        // Nodes In Reach
                        Column(modifier = Modifier.weight(0.7f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = SignalAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "NODES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalTextSecondary
                                )
                            }
                            Text(
                                text = "${uiState.nodes.size} Active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SignalAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Location Telemetry Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = MeshCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "POS: ${uiState.latitude ?: "N/A"}, ${uiState.longitude ?: "N/A"} (${uiState.locationSource})",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TacticalTextSecondary
                        )
                    }
                }
            }
        }

        // Primary Action Controls (Large 48dp+ buttons)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToSos,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("dashboard_sos_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_broadcast_sos),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onNavigateToMessages,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("dashboard_compose_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = MeshCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_compose_message),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TacticalTextPrimary
                    )
                }
            }
        }

        // Live Packet Activity Stream Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_packets),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TacticalTextSecondary
                )
                Text(
                    text = "${uiState.messages.size} logged",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TacticalTextSecondary
                )
            }
        }

        if (uiState.messages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CellTower,
                            contentDescription = null,
                            tint = TacticalTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No mesh packets received yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TacticalTextSecondary
                        )
                    }
                }
            }
        } else {
            items(uiState.messages.takeLast(10).reversed()) { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("packet_item_${msg.messageId}"),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (msg.priority) {
                                        2 -> EmergencyCrimson
                                        1 -> SignalAmber
                                        else -> MeshCyan
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg.senderNodeId,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.priority == 2) EmergencyCrimson else TacticalTextPrimary
                                )
                                Text(
                                    text = "Hops: ${msg.hopsTraveled}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MeshCyan
                                )
                            }
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = TacticalTextPrimary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
