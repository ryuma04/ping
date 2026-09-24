package com.inception.android.lantern.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inception.android.lantern.model.LanternCategory
import com.inception.android.lantern.model.LanternChunk
import com.inception.android.ui.theme.SpaceMonoFamily
import com.inception.android.ui.theme.SpaceGroteskFamily
import com.inception.android.ui.theme.NothingBorder
import com.inception.android.ui.theme.NothingSurface
import com.inception.android.ui.theme.NothingSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanternSheet(
    viewModel: LanternViewModel,
    onDismiss: () -> Unit,
    onBroadcastMeshQuery: ((String, LanternCategory) -> Unit)? = null
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showModelHub by viewModel.showModelHub.collectAsStateWithLifecycle()
    val selectedChunkForDetail by viewModel.selectedChunkForDetail.collectAsStateWithLifecycle()
    val activeTier by viewModel.modelManager.activeTier.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    com.inception.android.core.ui.component.sheet.InceptionBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Lantern",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lantern",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = { viewModel.openModelHub() },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        label = {
                            Text(
                                text = (activeTier?.displayName?.substringBefore(" ") ?: "Fast FTS5").uppercase(),
                                fontFamily = SpaceMonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (activeTier != null) Icons.Default.AutoAwesome else Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.openModelHub() }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Model Hub Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = {
                    Text(
                        "// SEARCH GUIDANCE (E.G. WATER, CPR)...",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 14.sp
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (selectedCategory == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    label = {
                        Text(
                            "[ALL PROTOCOLS]",
                            fontFamily = SpaceMonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                )
                LanternCategory.entries.forEach { category ->
                    val isSel = selectedCategory == category
                    FilterChip(
                        selected = isSel,
                        onClick = { viewModel.onCategorySelected(category) },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        label = {
                            Text(
                                "[${category.displayName.uppercase()}]",
                                fontFamily = SpaceMonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val chunks = searchResult?.localChunks ?: emptyList()
                val synthesized = searchResult?.synthesizedResponse

                if (chunks.isEmpty() && searchQuery.isNotBlank()) {
                    // Zero hits state -> Ask the Mesh fallback
                    ZeroHitsFallback(
                        query = searchQuery,
                        category = selectedCategory ?: LanternCategory.FIRST_AID,
                        onAskMesh = {
                            onBroadcastMeshQuery?.invoke(searchQuery, selectedCategory ?: LanternCategory.FIRST_AID)
                        }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 16.dp)
                    ) {
                        // AI Synthesized Response Card (if available)
                        if (!synthesized.isNullOrBlank()) {
                            item {
                                GroundedSynthesisCard(
                                    responseMarkdown = synthesized,
                                    tier = activeTier,
                                    latencyMs = searchResult?.latencyMs ?: 0L
                                )
                            }
                        }

                        // Verified Manual Cards
                        items(chunks) { chunk ->
                            ManualChunkCard(
                                chunk = chunk,
                                onClick = { viewModel.selectChunkDetail(chunk) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Model Hub Sheet
    if (showModelHub) {
        LanternModelHubSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.closeModelHub() }
        )
    }

    // Detail Dialog for Full Manual
    selectedChunkForDetail?.let { chunk ->
        ManualDetailDialog(
            chunk = chunk,
            onDismiss = { viewModel.selectChunkDetail(null) }
        )
    }
}

@Composable
private fun GroundedSynthesisCard(
    responseMarkdown: String,
    tier: com.inception.android.lantern.model.LanternModelTier?,
    latencyMs: Long
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI SYNTHESIZED GUIDANCE",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = SpaceMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${tier?.parameterSize ?: ""} [${latencyMs}MS]".uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = SpaceMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = responseMarkdown,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = SpaceGroteskFamily,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun ManualChunkCard(
    chunk: LanternChunk,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chunk.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "[${chunk.category.displayName.uppercase()}]",
                    fontFamily = SpaceMonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = chunk.summary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = SpaceGroteskFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (chunk.steps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    chunk.steps.take(3).forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = String.format("%02d.", index + 1),
                                fontFamily = SpaceMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                modifier = Modifier.width(26.dp)
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = SpaceGroteskFamily,
                                maxLines = 2
                            )
                        }
                    }
                    if (chunk.steps.size > 3) {
                        Text(
                            text = "+ ${chunk.steps.size - 3} MORE STEPS",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = SpaceMonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[MANUAL: ${chunk.sourceManual.uppercase()}]",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = SpaceMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "[VIEW FULL]",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = SpaceMonoFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ZeroHitsFallback(
    query: String,
    category: LanternCategory,
    onAskMesh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Local Manual Found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your offline library does not have an entry for \"$query\". You can broadcast an emergency query across the local BLE mesh.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAskMesh,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ask the Mesh")
        }
    }
}

@Composable
private fun ManualDetailDialog(
    chunk: LanternChunk,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = chunk.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Source: ${chunk.sourceManual}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = chunk.contentMarkdown,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                    if (chunk.steps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Action Steps:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        chunk.steps.forEachIndexed { i, step ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("${i + 1}. ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(step, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
