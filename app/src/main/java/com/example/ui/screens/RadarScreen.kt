package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScreen(
    uiState: SosMeshUiState,
    onUpdateCoordinates: (Double, Double) -> Unit
) {
    var isEditingCoords by remember { mutableStateOf(false) }
    var latInput by remember { mutableStateOf(uiState.latitude?.toString() ?: "37.7749") }
    var lngInput by remember { mutableStateOf(uiState.longitude?.toString() ?: "-122.4194") }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.radar_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = MeshCyan
                )
                Button(
                    onClick = { isEditingCoords = !isEditingCoords },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("edit_coords_toggle_btn")
                ) {
                    Icon(
                        Icons.Default.EditLocation,
                        contentDescription = null,
                        tint = MeshCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditingCoords) "DONE" else "SET POS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TacticalTextPrimary
                    )
                }
            }
        }

        // Coordinate Override Dialog / Panel
        if (isEditingCoords) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.coordinate_editor_title),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MeshCyan
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = latInput,
                                onValueChange = { latInput = it },
                                label = { Text("Latitude") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lat_input_field"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TacticalTextPrimary,
                                    unfocusedTextColor = TacticalTextPrimary,
                                    focusedBorderColor = MeshCyan,
                                    unfocusedBorderColor = TacticalCardBorder
                                )
                            )
                            OutlinedTextField(
                                value = lngInput,
                                onValueChange = { lngInput = it },
                                label = { Text("Longitude") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lng_input_field"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TacticalTextPrimary,
                                    unfocusedTextColor = TacticalTextPrimary,
                                    focusedBorderColor = MeshCyan,
                                    unfocusedBorderColor = TacticalCardBorder
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val lat = latInput.toDoubleOrNull() ?: 37.7749
                                val lng = lngInput.toDoubleOrNull() ?: -122.4194
                                onUpdateCoordinates(lat, lng)
                                isEditingCoords = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MeshCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_coordinates_btn")
                        ) {
                            Text(
                                text = stringResource(R.string.save_coords),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Radar Canvas Visualization
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .testTag("radar_canvas")
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = (size.minDimension / 2f) - 10.dp.toPx()

                        // Draw concentric range rings (1 Hop, 2 Hops, 3 Hops)
                        val rings = listOf(0.33f, 0.66f, 1.0f)
                        rings.forEachIndexed { index, fraction ->
                            drawCircle(
                                color = MeshCyan.copy(alpha = 0.25f),
                                radius = maxRadius * fraction,
                                center = center,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }

                        // Crosshairs
                        drawLine(
                            color = MeshCyan.copy(alpha = 0.2f),
                            start = Offset(center.x, center.y - maxRadius),
                            end = Offset(center.x, center.y + maxRadius),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = MeshCyan.copy(alpha = 0.2f),
                            start = Offset(center.x - maxRadius, center.y),
                            end = Offset(center.x + maxRadius, center.y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Sweep Radar Line
                        val rad = Math.toRadians(sweepAngle.toDouble())
                        val sweepEnd = Offset(
                            (center.x + maxRadius * cos(rad)).toFloat(),
                            (center.y + maxRadius * sin(rad)).toFloat()
                        )
                        drawLine(
                            color = MeshCyan.copy(alpha = 0.7f),
                            start = center,
                            end = sweepEnd,
                            strokeWidth = 2.dp.toPx()
                        )

                        // Center Local Node Dot
                        drawCircle(
                            color = SecureGreen,
                            radius = 6.dp.toPx(),
                            center = center
                        )

                        // Surrounding nodes
                        uiState.nodes.forEachIndexed { i, node ->
                            val angle = (i * (360f / (uiState.nodes.size.coerceAtLeast(1))) + 45f)
                            val nRad = Math.toRadians(angle.toDouble())
                            val hopFraction = when (node.hopCount) {
                                1 -> 0.33f
                                2 -> 0.66f
                                else -> 0.95f
                            }
                            val nodePos = Offset(
                                (center.x + (maxRadius * hopFraction) * cos(nRad)).toFloat(),
                                (center.y + (maxRadius * hopFraction) * sin(nRad)).toFloat()
                            )

                            drawCircle(
                                color = if (node.hopCount == 1) MeshCyan else SignalAmber,
                                radius = 7.dp.toPx(),
                                center = nodePos
                            )
                        }
                    }

                    // Radar Legend Pill in Corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TacticalSurfaceVariant.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Rings: 1 Hop • 2 Hops • 3+ Hops",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TacticalTextSecondary
                        )
                    }
                }
            }
        }

        // Nodes List Header
        item {
            Text(
                text = "DISCOVERED MESH NODES (${uiState.nodes.size})",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = TacticalTextSecondary
            )
        }

        if (uiState.nodes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No mesh nodes in reach yet.\nUse Testbench in Hardware tab to simulate neighbors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TacticalTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.nodes) { node ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("node_card_${node.nodeId}"),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (node.isDirectNeighbor) SecureGreen else SignalAmber)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${node.alias} (${node.nodeId})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalTextPrimary
                                )
                            }
                            Text(
                                text = if (node.hopCount == 1) "DIRECT" else "${node.hopCount} HOPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (node.hopCount == 1) SecureGreen else SignalAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HW: ${node.hardwareModel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TacticalTextSecondary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.BatteryFull,
                                    contentDescription = null,
                                    tint = SecureGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${node.batteryPercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SecureGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "RSSI: ${node.rssi} dBm | SNR: ${node.snr} dB",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = TacticalTextSecondary
                            )
                            Text(
                                text = "Pos: ${node.latitude ?: "N/A"}, ${node.longitude ?: "N/A"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MeshCyan
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
