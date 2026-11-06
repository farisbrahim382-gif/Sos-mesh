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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.EmergencyCrimsonDark
import com.example.ui.theme.MeshCyan
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TacticalCardBorder
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import com.example.ui.viewmodel.SosMeshUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SosScreen(
    uiState: SosMeshUiState,
    onStartCountdown: (type: String, severity: String) -> Unit,
    onCancelCountdown: () -> Unit,
    onDeactivateSos: () -> Unit
) {
    var selectedType by remember { mutableStateOf("MEDICAL") }
    var selectedSeverity by remember { mutableStateOf("CRITICAL") }
    var emergencyNote by remember { mutableStateOf("") }

    val categories = listOf(
        "MEDICAL" to stringResource(R.string.hazard_medical),
        "LOST" to stringResource(R.string.hazard_lost),
        "FIRE" to stringResource(R.string.hazard_fire),
        "ACCIDENT" to stringResource(R.string.hazard_accident),
        "DISASTER" to stringResource(R.string.hazard_disaster),
        "SECURITY" to stringResource(R.string.hazard_security),
        "GENERAL" to stringResource(R.string.hazard_general)
    )

    val severities = listOf(
        "CRITICAL" to stringResource(R.string.severity_critical),
        "URGENT" to stringResource(R.string.severity_urgent),
        "ADVISORY" to stringResource(R.string.severity_advisory)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sos_beacon_anim")
    val beaconPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        Text(
            text = stringResource(R.string.sos_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = EmergencyCrimson
        )
        Text(
            text = stringResource(R.string.sos_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = TacticalTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Emergency SOS Trigger Circle or Countdown
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isSosActive) {
                // Active beacon broadcasting state
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(EmergencyCrimson)
                        .border(4.dp, Color.White, CircleShape)
                        .testTag("sos_active_pulse_indicator"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "BEACON\nACTIVE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (uiState.isSosCountdownActive) {
                // 3-second safety countdown
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(EmergencyCrimson)
                        .testTag("sos_countdown_indicator"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.sosCountdownSeconds}",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "TRANSMITTING...",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Idle Standby Button
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(EmergencyCrimson)
                        .clickable {
                            onStartCountdown(selectedType, selectedSeverity)
                        }
                        .border(3.dp, EmergencyCrimsonDark, CircleShape)
                        .testTag("sos_main_trigger_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "HOLD SOS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "3 SEC SAFEGUARD",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Cancel countdown action if active
        AnimatedVisibility(visible = uiState.isSosCountdownActive) {
            Button(
                onClick = onCancelCountdown,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sos_cancel_countdown_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = TacticalTextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.sos_cancel_countdown, uiState.sosCountdownSeconds),
                    fontWeight = FontWeight.Bold,
                    color = TacticalTextPrimary
                )
            }
        }

        // Stop Beacon Action if active
        AnimatedVisibility(visible = uiState.isSosActive) {
            Button(
                onClick = onDeactivateSos,
                colors = ButtonDefaults.buttonColors(containerColor = SecureGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sos_stop_beacon_btn")
            ) {
                Text(
                    text = stringResource(R.string.sos_stop_beacon),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // Category Picker Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.sos_hazard_type),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TacticalTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (typeKey, label) ->
                        val isSelected = (selectedType == typeKey)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) EmergencyCrimson else TacticalSurfaceVariant,
                            border = if (isSelected) null else CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .clickable { selectedType = typeKey }
                                .testTag("hazard_chip_$typeKey")
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TacticalTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Severity Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SEVERITY LEVEL",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TacticalTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    severities.forEach { (sevKey, label) ->
                        val isSelected = (selectedSeverity == sevKey)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) {
                                when (sevKey) {
                                    "CRITICAL" -> EmergencyCrimson
                                    "URGENT" -> SignalAmber
                                    else -> MeshCyan
                                }
                            } else TacticalSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSeverity = sevKey }
                                .testTag("severity_chip_$sevKey")
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) Color.White else TacticalTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Location & Telemetry Snapshot Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.location_status_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TacticalTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = MeshCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAT: ${uiState.latitude ?: "UNKNOWN"}, LNG: ${uiState.longitude ?: "UNKNOWN"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = SecureGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device Battery: ${uiState.batteryPercent}% (Broadcast in Beacon)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TacticalTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
