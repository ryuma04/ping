package com.inception.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inception.android.mesh.ActiveSosAlert

/**
 * Compact, high-visibility emergency banner displayed persistently across chat views
 * when a local or remote distress beacon is active (FR-SOS-04, FR-SOS-05).
 */
@Composable
fun EmergencySosBanner(
    primaryAlert: ActiveSosAlert?,
    onCancelMySos: () -> Unit,
    onDismissRemoteAlert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = primaryAlert != null,
        enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
        exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(200)) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        if (primaryAlert == null) return@AnimatedVisibility

        val isSelf = primaryAlert.senderNickname == "You" || primaryAlert.senderPeerID == "self"

        // Pulsing glow animation for distress indicator
        val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFFE53935).copy(alpha = pulseAlpha),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF320A0A),
                            Color(0xFF1E0606)
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Main Row: Beacon Dot + Title + Status Badge + Battery + Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left cluster: Beacon + Title + Status + Telemetry
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Pulsing red beacon dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = pulseAlpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }

                        // Title
                        Text(
                            text = if (isSelf) "SOS ACTIVE" else "SOS: ${primaryAlert.senderNickname}",
                            color = Color(0xFFFFD2D2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE53935))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = primaryAlert.payload.status.displayName.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Battery pill
                        if (primaryAlert.payload.batteryPct >= 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x33FFFFFF))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BatteryAlert,
                                    contentDescription = null,
                                    tint = if (primaryAlert.payload.batteryPct <= 15) Color(0xFFFF5252) else Color(0xFFFFD54F),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${primaryAlert.payload.batteryPct}%",
                                    color = Color(0xFFEEEEEE),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Geohash pill
                        if (!primaryAlert.payload.geohash.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x33FFFFFF))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = primaryAlert.payload.geohash,
                                    color = Color(0xFFEEEEEE),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Right action: "I AM SAFE" button or dismiss
                    if (isSelf) {
                        Button(
                            onClick = onCancelMySos,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .padding(start = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "I AM SAFE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { onDismissRemoteAlert(primaryAlert.senderPeerID) },
                            modifier = Modifier
                                .size(24.dp)
                                .padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFFFA4A4),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Note subline (only shown if user entered non-blank details)
                if (primaryAlert.payload.note.isNotBlank()) {
                    Text(
                        text = "“${primaryAlert.payload.note}”",
                        color = Color(0xFFFFDADA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}
