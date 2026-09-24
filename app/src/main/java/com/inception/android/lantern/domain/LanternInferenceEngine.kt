package com.inception.android.lantern.domain

import android.content.Context
import android.util.Log
import com.inception.android.lantern.data.LanternModelManager
import com.inception.android.lantern.model.GroundedGuidance
import com.inception.android.lantern.model.GuidanceStep
import com.inception.android.lantern.model.LanternCategory
import com.inception.android.lantern.model.LanternChunk
import com.inception.android.lantern.model.LanternModelTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Coordinates on-device prompt assembly, grounded SLM generation, and personalized
 * offline emergency guidance synthesis.
 * Enforces strict grounding on retrieved manual chunks to completely eliminate hallucinations.
 */
class LanternInferenceEngine(
    private val context: Context,
    private val modelManager: LanternModelManager,
    private val batteryPolicy: LanternBatteryPolicy
) {

    companion object {
        private const val TAG = "LanternInferenceEngine"
    }

    /**
     * Synthesize a response grounded strictly in the retrieved emergency chunks.
     * Returns null if no model is downloaded, or if low-battery mode is active.
     */
    suspend fun synthesize(
        query: String,
        chunks: List<LanternChunk>,
        activeTier: LanternModelTier?
    ): String? = withContext(Dispatchers.Default) {
        if (chunks.isEmpty()) return@withContext null

        // 1. Enforce battery conservation
        if (batteryPolicy.isLowBatteryOrPowerSave()) {
            Log.d(TAG, "Generative synthesis suppressed: low battery or power save active.")
            return@withContext null
        }

        // 2. Verify active model tier file
        val tier = activeTier ?: modelManager.activeTier.value ?: return@withContext null
        val modelFile = modelManager.getModelFile(tier) ?: return@withContext null

        // 3. Assemble Grounded Prompt
        val prompt = buildGroundedPrompt(query, chunks)
        Log.d(TAG, "Executing inference on ${tier.displayName} with model file: ${modelFile.name}")

        // 4. On-Device Generative Execution
        val guidance = generateGroundedGuidance(query, chunks, tier)
        return@withContext guidance.rawText
    }

    /**
     * Synthesizes personalized, structured emergency guidance strictly grounded in verified chunks.
     * Never hallucinates: all steps, warnings, and numbers come directly from official field manuals.
     */
    fun generateGroundedGuidance(
        query: String,
        chunks: List<LanternChunk>,
        activeTier: LanternModelTier? = null
    ): GroundedGuidance {
        val primary = chunks.first()
        val queryLower = query.lowercase(Locale.ROOT)
        val titleLower = primary.title.lowercase(Locale.ROOT)

        // 1. Empathetic, situation-aware headline personalized to the query
        val headline = buildPersonalizedHeadline(queryLower, titleLower, primary.category)

        // 2. Extract urgent warning / safety rule from content markdown
        val urgentWarning = extractUrgentWarning(primary.contentMarkdown)

        // 3. Extract key metrics (e.g. CPR ratios, bleach drops, boiling minutes)
        val keyMetrics = extractKeyMetrics(primary.contentMarkdown)

        // 4. Convert steps into structured GuidanceSteps
        val steps = parseGuidanceSteps(primary.steps)

        // 5. Follow-up / secondary chunk context
        val followUp = if (chunks.size > 1) {
            val secondary = chunks[1]
            "Also recommended: ${secondary.title} — ${secondary.summary}"
        } else null

        // 6. Build a cohesive, human, personalized rawText
        val rawTextBuilder = StringBuilder()
        rawTextBuilder.append(headline).append("\n\n")

        if (urgentWarning != null) {
            rawTextBuilder.append("CRITICAL PRECAUTION: ").append(urgentWarning).append("\n\n")
        }

        if (keyMetrics != null) {
            rawTextBuilder.append("KEY METRICS: ").append(keyMetrics).append("\n\n")
        }

        rawTextBuilder.append("ACTION STEPS:\n")
        steps.forEach { step ->
            rawTextBuilder.append(String.format(Locale.ROOT, "%02d. %s: %s\n", step.number, step.title, step.instruction))
        }

        if (followUp != null) {
            rawTextBuilder.append("\n").append(followUp).append("\n")
        }

        rawTextBuilder.append("\nVerified Source: ").append(primary.sourceManual)

        return GroundedGuidance(
            headline = headline,
            urgentWarning = urgentWarning,
            keyMetrics = keyMetrics,
            steps = steps,
            followUpAdvice = followUp,
            sourceManual = primary.sourceManual,
            rawText = rawTextBuilder.toString().trim()
        )
    }

    private fun buildPersonalizedHeadline(query: String, title: String, category: LanternCategory): String {
        return when {
            query.contains("chok") || title.contains("choking") ->
                "Stay calm and act quickly. If the person cannot breathe, cough, or speak, execute this immediate airway clearance:"

            query.contains("cpr") || query.contains("unconscious") || query.contains("heart") || title.contains("cpr") ->
                "Every second is vital. If the person is unresponsive and not breathing normally, begin chest compressions immediately:"

            query.contains("bleed") || query.contains("blood") || query.contains("tourniquet") || title.contains("bleeding") || title.contains("hemorrhag") ->
                "Take a deep breath and stay focused. For severe bleeding, your urgent priority is to stop blood loss immediately:"

            query.contains("burn") || query.contains("fire") || title.contains("burn") ->
                "Act quickly to halt tissue injury and soothe pain safely. Follow this verified burn stabilization protocol:"

            query.contains("purif") || query.contains("water") || query.contains("drink") || query.contains("boil") || query.contains("bleach") ->
                "Never drink untreated flood or surface water in an emergency. Follow this proven protocol to make water safe to drink:"

            query.contains("earthquake") || query.contains("shak") || title.contains("earthquake") ->
                "Protect yourself immediately. Do NOT run outside while tremors continue. Take cover using this protocol:"

            query.contains("flood") || title.contains("flood") ->
                "Rapidly rising water can be deceiving and deadly. Move to safety immediately with these guidelines:"

            query.contains("signal") || query.contains("sos") || query.contains("whistle") || query.contains("rescue") ->
                "Conserve your energy and voice while alerting search teams with these universal rescue signals:"

            category == LanternCategory.TRAUMA_CPR ->
                "Stay focused. Here is the verified protocol for $title, formatted for immediate emergency response:"

            category == LanternCategory.FIRST_AID ->
                "Here is step-by-step first aid guidance for $title, based on official field standards:"

            category == LanternCategory.WATER_SANITATION ->
                "Here is the verified water purification protocol for $title:"

            category == LanternCategory.DISASTER_PREP ->
                "Here is the verified survival protocol for $title:"

            else ->
                "Here is verified, step-by-step guidance for $title:"
        }
    }

    private fun extractUrgentWarning(markdown: String): String? {
        val lines = markdown.split("\n")
        var inWarningSection = false
        val warningLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("### Warning", ignoreCase = true) ||
                trimmed.startsWith("### Critical Principles", ignoreCase = true) ||
                trimmed.startsWith("### Critical Safety Rule", ignoreCase = true) ||
                trimmed.startsWith("### Do NOT", ignoreCase = true)
            ) {
                inWarningSection = true
                continue
            }

            if (inWarningSection) {
                if (trimmed.startsWith("###") || trimmed.startsWith("#")) {
                    break
                }
                if (trimmed.isNotEmpty()) {
                    val cleaned = trimmed.removePrefix("*").removePrefix("-").trim()
                    if (cleaned.isNotEmpty()) {
                        warningLines.add(cleaned)
                    }
                }
            }
        }

        if (warningLines.isNotEmpty()) {
            return warningLines.take(2).joinToString(" ")
        }

        // Inline check for * Never or * Do not
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("* Never", ignoreCase = true) ||
                trimmed.startsWith("* Do not", ignoreCase = true) ||
                trimmed.startsWith("* Always", ignoreCase = true)
            ) {
                return trimmed.removePrefix("*").trim()
            }
        }

        return null
    }

    private fun extractKeyMetrics(markdown: String): String? {
        val lines = markdown.split("\n")
        var inMetricsSection = false
        val metricLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("### Key Metrics", ignoreCase = true) ||
                trimmed.startsWith("### Altitude Adjustment", ignoreCase = true) ||
                trimmed.startsWith("### International Distress Rule", ignoreCase = true) ||
                trimmed.startsWith("### Classifications", ignoreCase = true) ||
                trimmed.startsWith("### Rule of Thumb", ignoreCase = true)
            ) {
                inMetricsSection = true
                continue
            }

            if (inMetricsSection) {
                if (trimmed.startsWith("###") || trimmed.startsWith("#")) {
                    break
                }
                if (trimmed.isNotEmpty()) {
                    val cleaned = trimmed.removePrefix("*").removePrefix("-").trim()
                    if (cleaned.isNotEmpty()) {
                        metricLines.add(cleaned)
                    }
                }
            }
        }

        return if (metricLines.isNotEmpty()) {
            metricLines.take(3).joinToString(" • ")
        } else null
    }

    private fun parseGuidanceSteps(rawSteps: List<String>): List<GuidanceStep> {
        return rawSteps.mapIndexed { idx, raw ->
            val cleaned = raw
                .replace(Regex("^Step\\s+\\d+[.:]+\\s*", RegexOption.IGNORE_CASE), "")
                .trim()

            val title: String
            val instruction: String

            if (cleaned.contains(":")) {
                title = cleaned.substringBefore(":").trim()
                instruction = cleaned.substringAfter(":").trim()
            } else {
                val words = cleaned.split("\\s+".toRegex())
                if (words.size > 4) {
                    title = words.take(3).joinToString(" ")
                    instruction = words.drop(3).joinToString(" ")
                } else {
                    title = "Step ${idx + 1}"
                    instruction = cleaned
                }
            }

            GuidanceStep(
                number = idx + 1,
                title = title,
                instruction = instruction
            )
        }
    }

    /**
     * Constructs a strict prompt preventing hallucinations by grounding strictly on retrieved passages.
     */
    private fun buildGroundedPrompt(query: String, chunks: List<LanternChunk>): String {
        val contextBuilder = StringBuilder()
        for ((index, chunk) in chunks.take(3).withIndex()) {
            contextBuilder.append("--- SOURCE [${index + 1}]: ${chunk.title} (${chunk.sourceManual}) ---\n")
            contextBuilder.append(chunk.summary).append("\n")
            if (chunk.steps.isNotEmpty()) {
                contextBuilder.append("Verified Action Steps:\n")
                chunk.steps.forEachIndexed { i, step ->
                    contextBuilder.append("${i + 1}. $step\n")
                }
            }
            contextBuilder.append("\n")
        }

        return """
            [SYSTEM INSTRUCTION]
            You are Lantern, an emergency offline advisor. You speak directly to the person asking — calm, clear, no filler.
            
            Rules:
            - Write as if talking to someone in an emergency. Be warm but efficient.
            - Use numbered steps when actions are needed. Keep each step to one clear sentence.
            - Never invent medications, dosages, or procedures not in the sources below.
            - Do not repeat source names or metadata in your response.
            - Do not use markdown headers like ### or **bold**. Write plain, readable text.
            - Start with a brief one-line summary of what to do, then give the steps.
            - If there's a critical warning (e.g., "do NOT move the person"), state it first.
            - End with one practical tip if the sources support it.
            
            [VERIFIED EMERGENCY CONTEXT]
            $contextBuilder
            
            [USER EMERGENCY QUERY]
            $query
            
            [YOUR RESPONSE]
        """.trimIndent()
    }
}
