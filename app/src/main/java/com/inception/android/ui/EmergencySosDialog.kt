package com.inception.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inception.android.model.EmergencyStatus

/**
 * Dialog opened upon releasing the 3-second SOS press-and-hold trigger (FR-SOS-01).
 * Displays telemetry (battery %, geohash) and lets the user choose an emergency
 * status chip and optional note before flooding the mesh.
 */
@Composable
fun EmergencySosDialog(
    show: Boolean,
    batteryPct: Int,
    geohash: String?,
    onDismiss: () -> Unit,
    onConfirmBroadcast: (EmergencyStatus, String) -> Unit
) {
    if (!show) return

    var selectedStatus by remember { mutableStateOf(EmergencyStatus.MEDICAL) }
    var noteText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .border(1.5.dp, Color(0xFFE53935), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1414),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE53935)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Emergency Distress Beacon",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Floods all nearby mesh devices (TTL=7)",
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp
                        )
                    }
                }

                // Telemetry summary card
                Surface(
                    color = Color(0xFF2A1C1C),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Battery
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BatteryAlert,
                                contentDescription = null,
                                tint = if (batteryPct <= 15) Color(0xFFFF5252) else Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Battery: ${if (batteryPct >= 0) "$batteryPct%" else "N/A"}",
                                color = Color(0xFFDDDDDD),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Geohash
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Location: ${geohash ?: "Unavailable"}",
                                color = Color(0xFFDDDDDD),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Emergency Status Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "EMERGENCY CATEGORY",
                        color = Color(0xFFBBBBBB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )

                    val statusOptions = listOf(
                        Triple(EmergencyStatus.MEDICAL, "Medical", Icons.Filled.MedicalServices),
                        Triple(EmergencyStatus.TRAPPED, "Trapped", Icons.Filled.Lock),
                        Triple(EmergencyStatus.EVACUATING, "Evacuating", Icons.Filled.DirectionsRun),
                        Triple(EmergencyStatus.ASSISTANCE, "Assistance", Icons.Filled.Handshake)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.take(2).forEach { (status, label, icon) ->
                            StatusOptionChip(
                                label = label,
                                icon = icon,
                                isSelected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.drop(2).forEach { (status, label, icon) ->
                            StatusOptionChip(
                                label = label,
                                icon = icon,
                                isSelected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Optional note input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DETAILS (OPTIONAL)",
                        color = Color(0xFFBBBBBB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { if (it.length <= 120) noteText = it },
                        placeholder = {
                            Text(
                                "e.g., Injured leg, 2 people trapped, need water...",
                                color = Color(0xFF777777),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFF443333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFFE53935),
                            focusedContainerColor = Color(0xFF251A1A),
                            unfocusedContainerColor = Color(0xFF251A1A)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFAAAAAA)
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onConfirmBroadcast(selectedStatus, noteText.trim())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.6f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CellTower,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BROADCAST SOS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusOptionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFE53935) else Color(0xFF443333),
                shape = RoundedCornerShape(10.dp)
            ),
        color = if (isSelected) Color(0xFF4A1818) else Color(0xFF221717),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFFF5252) else Color(0xFFAAAAAA),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
