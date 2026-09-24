package com.inception.android.lantern.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Emergency categories supported by the Lantern Knowledge & Guidance engine.
 */
enum class LanternCategory(val displayName: String, val iconName: String) {
    FIRST_AID("First Aid", "medical_services"),
    WATER_SANITATION("Water & Hygiene", "water_drop"),
    TRAUMA_CPR("CPR & Trauma", "favorite"),
    DISASTER_PREP("Disaster Triage", "warning"),
    SHELTER_FIRE("Shelter & Fire", "local_fire_department"),
    SIGNALING("Emergency Signaling", "sos");

    companion object {
        fun fromString(value: String): LanternCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FIRST_AID
        }
    }
}

/**
 * Core unit of verified offline knowledge.
 */
@Parcelize
data class LanternChunk(
    val id: String,
    val title: String,
    val category: LanternCategory,
    val summary: String,
    val contentMarkdown: String,
    val steps: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val sourceManual: String
) : Parcelable

/**
 * Specifications for on-demand downloadable on-device Small Language Models.
 */
enum class LanternModelTier(
    val tierId: String,
    val displayName: String,
    val description: String,
    val parameterSize: String,
    val downloadSizeBytes: Long,
    val ramRequirementMb: Int,
    val downloadUrl: String,
    val fileName: String
) {
    TIER_1_LIGHTWEIGHT(
        tierId = "smollm2_360m_q4",
        displayName = "Ultra-Lightweight (SmolLM2)",
        description = "Optimized for budget devices and emergency low-power states.",
        parameterSize = "360M Params (4-bit)",
        downloadSizeBytes = 260_000_000L, // ~260 MB
        ramRequirementMb = 350,
        downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
        fileName = "smollm2_360m_q4.gguf"
    ),
    TIER_2_BALANCED(
        tierId = "qwen25_05b_q4",
        displayName = "Balanced Guidance (Qwen2.5)",
        description = "Excellent reasoning and step-by-step instruction synthesis for mid-range phones.",
        parameterSize = "0.5B Params (4-bit)",
        downloadSizeBytes = 400_000_000L, // ~400 MB
        ramRequirementMb = 700,
        downloadUrl = "https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
        fileName = "qwen25_05b_q4.gguf"
    ),
    TIER_3_PRECISION(
        tierId = "gemma2_2b_q4",
        displayName = "High Precision (Gemma-2)",
        description = "Deep medical & situational comprehension for flagship hardware.",
        parameterSize = "2B Params (4-bit)",
        downloadSizeBytes = 1_600_000_000L, // ~1.6 GB
        ramRequirementMb = 1800,
        downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
        fileName = "gemma2_2b_q4.gguf"
    )
}

/**
 * State of model download in the Model Hub.
 */
sealed class ModelDownloadState {
    data object Idle : ModelDownloadState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState()
    data class Ready(val localFilePath: String, val fileSizeBytes: Long) : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

data class GuidanceStep(
    val number: Int,
    val title: String,
    val instruction: String
)

/**
 * Structured, personalized emergency guidance synthesized strictly from verified local manuals.
 * Formatted for immediate comprehension in high-stress emergency scenarios.
 */
data class GroundedGuidance(
    val headline: String,
    val urgentWarning: String? = null,
    val keyMetrics: String? = null,
    val steps: List<GuidanceStep> = emptyList(),
    val followUpAdvice: String? = null,
    val sourceManual: String,
    val rawText: String
)

/**
 * Result returned by the Lantern search & synthesis pipeline.
 */
data class LanternSearchResult(
    val query: String,
    val category: LanternCategory?,
    val localChunks: List<LanternChunk>,
    val synthesizedResponse: String? = null,
    val matchedSnippet: String? = null,
    val latencyMs: Long = 0L,
    val isFromModel: Boolean = false,
    val isFromMesh: Boolean = false,
    val activeModelTier: LanternModelTier? = null,
    val guidance: GroundedGuidance? = null
)

/**
 * Payload broadcast across the mesh when local search has zero hits.
 */
data class LanternMeshQuery(
    val queryId: String,
    val senderPeerId: String,
    val queryText: String,
    val category: LanternCategory,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Response returned by a peer containing verified manual guidance.
 */
data class LanternMeshResponse(
    val queryId: String,
    val responderPeerId: String,
    val responderNickname: String,
    val matchedTitle: String,
    val guidanceSnippet: String,
    val steps: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
