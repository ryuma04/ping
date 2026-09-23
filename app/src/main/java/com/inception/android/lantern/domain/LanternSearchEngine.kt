package com.inception.android.lantern.domain

import android.content.Context
import android.util.Log
import com.inception.android.lantern.data.LanternKnowledgeRepository
import com.inception.android.lantern.data.LanternModelManager
import com.inception.android.lantern.model.LanternCategory
import com.inception.android.lantern.model.LanternSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main coordinator for query classification, FTS5 retrieval, and SLM synthesis.
 */
class LanternSearchEngine private constructor(context: Context) {

    companion object {
        private const val TAG = "LanternSearchEngine"

        @Volatile
        private var INSTANCE: LanternSearchEngine? = null

        fun getInstance(context: Context): LanternSearchEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanternSearchEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val repository = LanternKnowledgeRepository.getInstance(context)
    private val modelManager = LanternModelManager.getInstance(context)
    private val batteryPolicy = LanternBatteryPolicy(context)
    private val inferenceEngine = LanternInferenceEngine(context, modelManager, batteryPolicy)

    suspend fun query(
        rawQuery: String,
        selectedCategory: LanternCategory? = null
    ): LanternSearchResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val trimmed = rawQuery.trim()

        // 1. Classify intent if category not manually selected
        val effectiveCategory = selectedCategory ?: LanternQueryClassifier.classify(trimmed)

        // 2. Perform FTS5 BM25 search
        val matches = repository.search(trimmed, effectiveCategory, limit = 5)
        val chunks = matches.map { it.first }
        val topSnippet = matches.firstOrNull()?.second

        // 3. Attempt SLM synthesis if model is active and battery permits
        val activeTier = modelManager.activeTier.value
        val synthesized = if (chunks.isNotEmpty() && activeTier != null) {
            inferenceEngine.synthesize(trimmed, chunks, activeTier)
        } else {
            null
        }

        val latency = System.currentTimeMillis() - startTime
        Log.d(TAG, "Search completed for '$trimmed': ${chunks.size} chunks, synth=${synthesized != null} in ${latency}ms")

        return@withContext LanternSearchResult(
            query = trimmed,
            category = effectiveCategory,
            localChunks = chunks,
            synthesizedResponse = synthesized,
            matchedSnippet = topSnippet,
            latencyMs = latency,
            isFromModel = synthesized != null,
            isFromMesh = false,
            activeModelTier = activeTier
        )
    }

    suspend fun loadCategoryManuals(category: LanternCategory?): LanternSearchResult = withContext(Dispatchers.Default) {
        val chunks = repository.getChunksByCategory(category, limit = 10)
        return@withContext LanternSearchResult(
            query = "",
            category = category,
            localChunks = chunks,
            synthesizedResponse = null,
            matchedSnippet = null,
            latencyMs = 0L,
            isFromModel = false,
            isFromMesh = false,
            activeModelTier = modelManager.activeTier.value
        )
    }
}
