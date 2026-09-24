package com.inception.android.lantern.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inception.android.lantern.model.GroundedGuidance
import com.inception.android.lantern.model.GuidanceStep
import com.inception.android.lantern.model.LanternCategory
import com.inception.android.lantern.model.LanternChunk
import com.inception.android.ui.theme.SpaceMonoFamily
import com.inception.android.ui.theme.SpaceGroteskFamily
import kotlinx.coroutines.launch
import java.util.Locale

// Nothing OS palette constants
private val NothingBlack = Color(0xFF000000)
private val NothingWarmLight = Color(0xFFF5F5F3)
private val NothingBorderColor = Color(0xFF252525)
private val NothingSurfaceDark = Color(0xFF0A0A0A)
private val NothingCardBackground = Color(0xFF0E0E0E)
private val NothingTextSecondary = Color(0xFF999999)
private val NothingTextDisabled = Color(0xFF666666)
private val NothingRed = Color(0xFFD71921)
private val NothingWarningBackground = Color(0xFF1F0C0D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanternSheet(
    viewModel: LanternViewModel,
    onDismiss: () -> Unit,
    onBroadcastMeshQuery: ((String, LanternCategory) -> Unit)? = null
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showModelHub by viewModel.showModelHub.collectAsStateWithLifecycle()
    val activeTier by viewModel.modelManager.activeTier.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    // Conversation history state
    val conversationHistory = remember { mutableStateListOf<RagMessage>() }

    // Helper to send a RAG query
    val sendQuery: (String) -> Unit = { queryText ->
        val trimmed = queryText.trim()
        if (trimmed.isNotBlank()) {
            conversationHistory.add(RagMessage.UserQuery(trimmed))
            focusManager.clearFocus()
            viewModel.onQueryChanged("")
            coroutineScope.launch {
                listState.animateScrollToItem(conversationHistory.size - 1)
            }
            viewModel.executeRagQuery(trimmed) { result ->
                if (result.guidance != null) {
                    conversationHistory.add(
                        RagMessage.AiResponse(
                            text = result.guidance.rawText,
                            tierName = result.activeModelTier?.displayName,
                            latencyMs = result.latencyMs,
                            sourceCount = result.localChunks.size,
                            guidance = result.guidance
                        )
                    )
                } else if (!result.synthesizedResponse.isNullOrBlank()) {
                    conversationHistory.add(
                        RagMessage.AiResponse(
                            text = result.synthesizedResponse,
                            tierName = result.activeModelTier?.displayName,
                            latencyMs = result.latencyMs,
                            sourceCount = result.localChunks.size,
                            guidance = null
                        )
                    )
                } else {
                    conversationHistory.add(
                        RagMessage.NoResults(query = trimmed)
                    )
                }
                coroutineScope.launch {
                    listState.animateScrollToItem(conversationHistory.size - 1)
                }
            }
        }
    }

    com.inception.android.core.ui.component.sheet.InceptionBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NothingBlack)
        ) {
            // ─── Header ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PING AI",
                        fontFamily = SpaceMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = NothingWarmLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "EMERGENCY GUIDANCE ENGINE",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp,
                        color = NothingTextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Model tier indicator chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, NothingBorderColor),
                        onClick = { viewModel.openModelHub() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (activeTier != null) Color(0xFF00E676) else NothingTextDisabled)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (activeTier?.displayName?.substringBefore(" (")
                                    ?: "LOCAL RAG").uppercase(Locale.ROOT),
                                fontFamily = SpaceMonoFamily,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                color = if (activeTier != null) NothingWarmLight else NothingTextSecondary
                            )
                        }
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
            }

            // ─── Thin separator ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NothingBorderColor)
            )

            // ─── Conversation Area ───
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (conversationHistory.isEmpty() && !isLoading) {
                    // Empty state — prompt chips and guidance overview
                    EmptyRagState(
                        activeTierName = activeTier?.displayName,
                        onPromptClick = sendQuery,
                        onOpenModelHub = { viewModel.openModelHub() }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(conversationHistory) { message ->
                            when (message) {
                                is RagMessage.UserQuery -> UserQueryBubble(message.text)
                                is RagMessage.AiResponse -> AiResponseCard(
                                    response = message.text,
                                    tier = message.tierName,
                                    latencyMs = message.latencyMs,
                                    sourceCount = message.sourceCount,
                                    guidance = message.guidance,
                                    onBroadcast = {
                                        onBroadcastMeshQuery?.invoke(
                                            message.text.take(120),
                                            LanternCategory.FIRST_AID
                                        )
                                    }
                                )
                                is RagMessage.NoResults -> NoResultsCard(
                                    query = message.query,
                                    onAskMesh = {
                                        onBroadcastMeshQuery?.invoke(
                                            message.query,
                                            LanternCategory.FIRST_AID
                                        )
                                    }
                                )
                            }
                        }

                        // Loading indicator
                        if (isLoading) {
                            item {
                                LoadingIndicator()
                            }
                        }
                    }
                }
            }

            // ─── Thin separator ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NothingBorderColor)
            )

            // ─── Input Bar ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = {
                        Text(
                            "ASK ABOUT CPR, BLEEDING, WATER, BURNS...",
                            fontFamily = SpaceMonoFamily,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            color = NothingTextDisabled
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 14.sp,
                        color = NothingWarmLight
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { sendQuery(searchQuery) }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingWarmLight,
                        unfocusedBorderColor = NothingBorderColor,
                        cursorColor = NothingWarmLight,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Send button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (searchQuery.isNotBlank()) NothingWarmLight else Color.Transparent,
                    border = BorderStroke(1.dp, if (searchQuery.isNotBlank()) NothingWarmLight else NothingBorderColor),
                    onClick = { sendQuery(searchQuery) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (searchQuery.isNotBlank()) NothingBlack else NothingTextDisabled,
                            modifier = Modifier.size(20.dp)
                        )
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
}

// ─── Message Data Types ───

sealed class RagMessage {
    data class UserQuery(val text: String) : RagMessage()
    data class AiResponse(
        val text: String,
        val tierName: String?,
        val latencyMs: Long,
        val sourceCount: Int,
        val guidance: GroundedGuidance? = null
    ) : RagMessage()
    data class NoResults(val query: String) : RagMessage()
}

// ─── UI Components ───

@Composable
private fun EmptyRagState(
    activeTierName: String?,
    onPromptClick: (String) -> Unit,
    onOpenModelHub: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "// 00",
            fontFamily = SpaceMonoFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp,
            color = NothingBorderColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "EMERGENCY GUIDANCE ENGINE",
            fontFamily = SpaceMonoFamily,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            color = NothingWarmLight
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ask any first-aid, CPR, or disaster survival question.\nSynthesized with zero hallucination from verified offline manuals.",
            fontFamily = SpaceGroteskFamily,
            fontSize = 13.sp,
            color = NothingTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "QUICK EMERGENCY PROTOCOLS",
            fontFamily = SpaceMonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            color = NothingTextDisabled
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick prompt chips
        val prompts = listOf(
            "How to perform Adult CPR",
            "Stop severe arterial bleeding with tourniquet",
            "Choking adult Heimlich maneuver",
            "Purify water using household bleach",
            "Emergency burn treatment protocol",
            "Earthquake survival drop cover hold",
            "Emergency distress signals for rescue"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            prompts.take(5).forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NothingCardBackground,
                    border = BorderStroke(1.dp, NothingBorderColor),
                    onClick = { onPromptClick(prompt) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prompt.uppercase(Locale.ROOT),
                            fontFamily = SpaceMonoFamily,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            color = NothingWarmLight,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = NothingTextDisabled,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Model Tier Status
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, NothingBorderColor),
            onClick = onOpenModelHub
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = NothingTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (activeTierName != null) {
                        "ACTIVE ENGINE: ${activeTierName.substringBefore(" (").uppercase(Locale.ROOT)}"
                    } else {
                        "ENGINE: ON-DEVICE LOCAL RAG (TAP TO ADD SLM)"
                    },
                    fontFamily = SpaceMonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    color = NothingTextSecondary
                )
            }
        }
    }
}

@Composable
private fun UserQueryBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            color = Color(0xFF161616),
            border = BorderStroke(1.dp, NothingBorderColor)
        ) {
            Text(
                text = text,
                fontFamily = SpaceGroteskFamily,
                fontSize = 14.sp,
                color = NothingWarmLight,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp)
            )
        }
    }
}

@Composable
private fun AiResponseCard(
    response: String,
    tier: String?,
    latencyMs: Long,
    sourceCount: Int,
    guidance: GroundedGuidance?,
    onBroadcast: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Metadata header line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "PING AI",
                    fontFamily = SpaceMonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWarmLight
                )
                Text(
                    text = "// ${(tier?.substringBefore(" (") ?: "LOCAL RAG").uppercase(Locale.ROOT)} [GROUNDED]",
                    fontFamily = SpaceMonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    color = NothingTextDisabled
                )
            }

            Text(
                text = "${latencyMs}MS",
                fontFamily = SpaceMonoFamily,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = NothingTextDisabled
            )
        }

        // Main response container
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            color = NothingCardBackground,
            border = BorderStroke(1.dp, NothingBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (guidance != null) {
                    // Render Structured Grounded Guidance
                    StructuredGuidanceContent(guidance = guidance)
                } else {
                    // Fallback to formatted text
                    FormattedResponseText(response)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NothingBorderColor)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons: Copy & Broadcast
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sourceCount} SOURCE${if (sourceCount != 1) "S" else ""} VERIFIED",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = NothingTextDisabled
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, NothingBorderColor),
                            onClick = {
                                val textToCopy = guidance?.rawText ?: response
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                copied = true
                            }
                        ) {
                            Text(
                                text = if (copied) "COPIED" else "COPY",
                                fontFamily = SpaceMonoFamily,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                color = if (copied) Color(0xFF00E676) else NothingTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, NothingBorderColor),
                            onClick = onBroadcast
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CellTower,
                                    contentDescription = null,
                                    tint = NothingTextSecondary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "MESH",
                                    fontFamily = SpaceMonoFamily,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                    color = NothingTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders structured emergency guidance:
 * - Situation-aware opening headline
 * - Urgent safety warning banner
 * - Key metrics specification pill
 * - Discrete, numbered step cards
 * - Follow-up advice & verified authority attribution
 */
@Composable
private fun StructuredGuidanceContent(guidance: GroundedGuidance) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Personalized Headline
        Text(
            text = guidance.headline,
            fontFamily = SpaceGroteskFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NothingWarmLight,
            lineHeight = 21.sp
        )

        // 2. Urgent Precaution Banner (if present)
        if (!guidance.urgentWarning.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = NothingWarningBackground,
                border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = NothingRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "CRITICAL PRECAUTION",
                            fontFamily = SpaceMonoFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = guidance.urgentWarning,
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 13.sp,
                            color = NothingWarmLight,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 3. Key Metrics Pill Bar (if present)
        if (!guidance.keyMetrics.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = NothingBlack,
                border = BorderStroke(1.dp, NothingBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SPECS:",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = NothingTextSecondary
                    )
                    Text(
                        text = guidance.keyMetrics.uppercase(Locale.ROOT),
                        fontFamily = SpaceMonoFamily,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = NothingWarmLight
                    )
                }
            }
        }

        // 4. Action Steps
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            guidance.steps.forEach { step ->
                StepCard(step = step)
            }
        }

        // 5. Follow-Up Advice
        if (!guidance.followUpAdvice.isNullOrBlank()) {
            Text(
                text = guidance.followUpAdvice,
                fontFamily = SpaceGroteskFamily,
                fontSize = 13.sp,
                color = NothingTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 6. Source Attribution
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = NothingTextDisabled,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = guidance.sourceManual.uppercase(Locale.ROOT),
                fontFamily = SpaceMonoFamily,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = NothingTextDisabled
            )
        }
    }
}

@Composable
private fun StepCard(step: GuidanceStep) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = NothingBlack,
        border = BorderStroke(1.dp, NothingBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step number pill
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = Color(0xFF1C1C1C),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = String.format(Locale.ROOT, "%02d", step.number),
                        fontFamily = SpaceMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWarmLight
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title.uppercase(Locale.ROOT),
                    fontFamily = SpaceMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = NothingWarmLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = step.instruction,
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 13.sp,
                    color = NothingWarmLight.copy(alpha = 0.85f),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

/**
 * Fallback renderer for arbitrary formatted markdown responses.
 */
@Composable
private fun FormattedResponseText(response: String) {
    val lines = response.split("\n")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> Spacer(modifier = Modifier.height(4.dp))
                trimmed.startsWith("---") -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(NothingBorderColor)
                    )
                }
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val stepNum = trimmed.substringBefore(".").trim()
                    val stepText = trimmed.substringAfter(".").trim()
                        .replace("**", "")
                        .replace(Regex("^Step \\d+:\\s*", RegexOption.IGNORE_CASE), "")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.ROOT, "%02d", stepNum.toIntOrNull() ?: 0),
                            fontFamily = SpaceMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWarmLight,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = stepText,
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 14.sp,
                            color = NothingWarmLight.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    }
                }
                trimmed.startsWith("###") || trimmed.startsWith("**") -> {
                    val cleaned = trimmed
                        .removePrefix("###")
                        .replace("**", "")
                        .trim()
                    Text(
                        text = cleaned.uppercase(Locale.ROOT),
                        fontFamily = SpaceMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingWarmLight,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                else -> {
                    val cleaned = trimmed.replace("**", "")
                    Text(
                        text = cleaned,
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 14.sp,
                        color = NothingWarmLight.copy(alpha = 0.85f),
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NoResultsCard(
    query: String,
    onAskMesh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "PING AI // NO LOCAL MATCH",
            fontFamily = SpaceMonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = NothingTextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            color = NothingCardBackground,
            border = BorderStroke(1.dp, NothingBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No verified manual found for \"$query\" in local storage.",
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 13.sp,
                    color = NothingTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, NothingBorderColor),
                    onClick = onAskMesh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = NothingWarmLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "BROADCAST QUERY TO MESH",
                            fontFamily = SpaceMonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = NothingWarmLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PING AI",
            fontFamily = SpaceMonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = NothingTextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "[SYNTHESIZING GROUNDED GUIDANCE...]",
            fontFamily = SpaceMonoFamily,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            color = NothingTextDisabled
        )
    }
}
