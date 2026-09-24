package com.inception.android.lantern.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inception.android.lantern.model.LanternModelTier
import com.inception.android.lantern.model.ModelDownloadState
import com.inception.android.ui.theme.SpaceMonoFamily
import com.inception.android.ui.theme.SpaceGroteskFamily

// Nothing OS palette constants
private val NothingBlack = Color(0xFF000000)
private val NothingWarmLight = Color(0xFFF5F5F3)
private val NothingBorderColor = Color(0xFF252525)
private val NothingSurfaceDark = Color(0xFF0A0A0A)
private val NothingTextSecondary = Color(0xFF999999)
private val NothingTextDisabled = Color(0xFF666666)
private val NothingRed = Color(0xFFD71921)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanternModelHubSheet(
    viewModel: LanternViewModel,
    onDismiss: () -> Unit
) {
    val downloadStates by viewModel.modelManager.downloadStates.collectAsStateWithLifecycle()
    val activeTier by viewModel.modelManager.activeTier.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    com.inception.android.core.ui.component.sheet.InceptionBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NothingBlack)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ─── Header ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MODEL HUB",
                        fontFamily = SpaceMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                        color = NothingWarmLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ON-DEVICE AI ENGINES",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp,
                        color = NothingTextSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NothingTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info banner
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, NothingBorderColor)
            ) {
                Text(
                    text = "DOWNLOAD ONCE ON WI-FI. MODELS RUN 100% OFFLINE IN PRIVATE STORAGE.",
                    fontFamily = SpaceMonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    color = NothingTextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Tier 0: Pure Knowledge option
                item {
                    PureKnowledgeCard(
                        isActive = activeTier == null,
                        onSelect = { viewModel.setActiveModelTier(null) }
                    )
                }

                // Downloadable SLM tiers
                items(LanternModelTier.entries) { tier ->
                    val state = downloadStates[tier.tierId] ?: ModelDownloadState.Idle
                    val isActive = activeTier == tier

                    ModelTierCard(
                        tier = tier,
                        state = state,
                        isActive = isActive,
                        onDownload = { viewModel.downloadModel(tier) },
                        onCancel = { viewModel.cancelModelDownload(tier) },
                        onActivate = { viewModel.setActiveModelTier(tier) },
                        onDelete = { viewModel.deleteModel(tier) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PureKnowledgeCard(
    isActive: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (isActive) NothingWarmLight else NothingBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "VERIFIED MANUALS ONLY",
                        fontFamily = SpaceMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = NothingWarmLight
                    )
                    Text(
                        text = "0 MB",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 10.sp,
                        color = NothingTextDisabled
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fastest. Works on 1% battery. SQLite FTS5 search.",
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 12.sp,
                    color = NothingTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isActive) {
                Text(
                    text = "[ACTIVE]",
                    fontFamily = SpaceMonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = NothingWarmLight
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, NothingBorderColor),
                    onClick = onSelect
                ) {
                    Text(
                        text = "SELECT",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = NothingWarmLight,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelTierCard(
    tier: LanternModelTier,
    state: ModelDownloadState,
    isActive: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (isActive) NothingWarmLight else NothingBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tier.displayName.uppercase(),
                    fontFamily = SpaceMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = NothingWarmLight
                )

                if (isActive) {
                    Text(
                        text = "[ACTIVE]",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = NothingWarmLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tier.description,
                fontFamily = SpaceGroteskFamily,
                fontSize = 12.sp,
                color = NothingTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Specs row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${tier.downloadSizeBytes / 1_000_000} MB",
                    fontFamily = SpaceMonoFamily,
                    fontSize = 10.sp,
                    color = NothingTextDisabled
                )
                Text(
                    text = "~${tier.ramRequirementMb} MB RAM",
                    fontFamily = SpaceMonoFamily,
                    fontSize = 10.sp,
                    color = NothingTextDisabled
                )
                Text(
                    text = tier.parameterSize.uppercase(),
                    fontFamily = SpaceMonoFamily,
                    fontSize = 10.sp,
                    color = NothingTextDisabled
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                is ModelDownloadState.Idle -> {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, NothingBorderColor),
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = NothingWarmLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DOWNLOAD (${tier.downloadSizeBytes / 1_000_000} MB)",
                                fontFamily = SpaceMonoFamily,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = NothingWarmLight
                            )
                        }
                    }
                }

                is ModelDownloadState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DOWNLOADING...",
                                fontFamily = SpaceMonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = NothingTextSecondary
                            )
                            Text(
                                text = "${state.progressPercent}%",
                                fontFamily = SpaceMonoFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWarmLight
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = NothingWarmLight,
                            trackColor = NothingBorderColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, NothingBorderColor),
                            onClick = onCancel,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "CANCEL",
                                fontFamily = SpaceMonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = NothingTextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                is ModelDownloadState.Ready -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[READY OFFLINE]",
                            fontFamily = SpaceMonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            color = NothingWarmLight
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isActive) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = NothingWarmLight,
                                    onClick = onActivate
                                ) {
                                    Text(
                                        text = "ACTIVATE",
                                        fontFamily = SpaceMonoFamily,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = NothingBlack,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.5f)),
                                onClick = onDelete
                            ) {
                                Text(
                                    text = "DELETE",
                                    fontFamily = SpaceMonoFamily,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    color = NothingRed,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                is ModelDownloadState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "[ERROR] ${state.message}",
                            fontFamily = SpaceMonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            color = NothingRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, NothingBorderColor),
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "RETRY DOWNLOAD",
                                fontFamily = SpaceMonoFamily,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = NothingWarmLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
