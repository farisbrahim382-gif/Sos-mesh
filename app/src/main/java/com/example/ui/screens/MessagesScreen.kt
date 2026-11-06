package com.example.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.mesh.PacketPriority
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(
    uiState: SosMeshUiState,
    onSelectChannel: (String) -> Unit,
    onSendMessage: (String, PacketPriority) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var priorityState by remember { mutableStateOf(PacketPriority.NORMAL) }

    val channels = listOf(
        "#all-mesh-broadcast" to stringResource(R.string.channel_broadcast),
        "#medical-response" to stringResource(R.string.channel_medical),
        "#sar-operations" to stringResource(R.string.channel_rescue)
    )

    val currentChannelMessages = uiState.messages.filter {
        it.channelName == uiState.selectedChannel || it.priority == 2 // SOS messages appear in all channels
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentChannelMessages.size) {
        if (currentChannelMessages.isNotEmpty()) {
            listState.animateScrollToItem(currentChannelMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Channels Tabs
        ScrollableTabRow(
            selectedTabIndex = channels.indexOfFirst { it.first == uiState.selectedChannel }.coerceAtLeast(0),
            containerColor = TacticalSurface,
            contentColor = MeshCyan,
            edgePadding = 12.dp
        ) {
            channels.forEach { (channelKey, label) ->
                val selected = (uiState.selectedChannel == channelKey)
                Tab(
                    selected = selected,
                    onClick = { onSelectChannel(channelKey) },
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (selected) MeshCyan else TacticalTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_$channelKey")
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentChannelMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages on ${uiState.selectedChannel}\nType below to broadcast offline via mesh",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TacticalTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(currentChannelMessages) { msg ->
                    val isFromMe = (msg.senderNodeId == uiState.localNodeId)
                    val isSos = (msg.priority == 2)
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(msg.timestamp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .testTag("msg_card_${msg.messageId}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSos -> EmergencyCrimson.copy(alpha = 0.2f)
                                    isFromMe -> TacticalSurfaceVariant
                                    else -> TacticalSurface
                                }
                            ),
                            border = if (isSos) {
                                CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmergencyCrimson))
                            } else {
                                CardDefaults.outlinedCardBorder()
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSos -> EmergencyCrimson
                                                        isFromMe -> SecureGreen
                                                        else -> MeshCyan
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isFromMe) "YOU (${msg.senderNodeId})" else msg.senderNodeId,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSos) EmergencyCrimson else MeshCyan
                                        )
                                    }

                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = TacticalTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = msg.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TacticalTextPrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.hops_label, msg.hopsTraveled),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = TacticalTextSecondary
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = msg.deliveryStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = when (msg.deliveryStatus) {
                                                "ACKNOWLEDGED" -> SecureGreen
                                                "RELAYED" -> MeshCyan
                                                "TRANSMITTED" -> SignalAmber
                                                else -> TacticalTextSecondary
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = when (msg.deliveryStatus) {
                                                "ACKNOWLEDGED" -> Icons.Default.DoneAll
                                                "RELAYED" -> Icons.Default.Done
                                                else -> Icons.Default.Schedule
                                            },
                                            contentDescription = null,
                                            tint = when (msg.deliveryStatus) {
                                                "ACKNOWLEDGED" -> SecureGreen
                                                "RELAYED" -> MeshCyan
                                                else -> TacticalTextSecondary
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Priority Quick Select
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRIORITY:",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = TacticalTextSecondary
            )
            listOf(
                PacketPriority.NORMAL to stringResource(R.string.msg_priority_normal),
                PacketPriority.HIGH to stringResource(R.string.msg_priority_high),
                PacketPriority.CRITICAL_SOS to stringResource(R.string.msg_priority_sos)
            ).forEach { (prio, label) ->
                val selected = (priorityState == prio)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) {
                        when (prio) {
                            PacketPriority.CRITICAL_SOS -> EmergencyCrimson
                            PacketPriority.HIGH -> SignalAmber
                            else -> TacticalSurfaceVariant
                        }
                    } else Color.Transparent,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .clickable { priorityState = prio }
                        .testTag("priority_tag_${prio.name}")
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else TacticalTextSecondary
                    )
                }
            }
        }

        // Bottom Compose Input Bar
        Surface(
            color = TacticalSurface,
            tonalElevation = 4.dp,
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.send_message_hint),
                            color = TacticalTextSecondary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TacticalTextPrimary,
                        unfocusedTextColor = TacticalTextPrimary,
                        focusedContainerColor = TacticalSurfaceVariant,
                        unfocusedContainerColor = TacticalSurfaceVariant,
                        focusedBorderColor = MeshCyan,
                        unfocusedBorderColor = TacticalCardBorder
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput, priorityState)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (priorityState == PacketPriority.CRITICAL_SOS) EmergencyCrimson else MeshCyan)
                        .testTag("send_message_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_btn),
                        tint = Color.Black
                    )
                }
            }
        }
    }
}
