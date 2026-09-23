package com.inception.android.lantern.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inception.android.lantern.model.LanternCategory
import com.inception.android.lantern.model.LanternChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

/**
 * Repository responsible for loading pre-bundled offline manuals into SQLite FTS5
 * and executing fast offline queries.
 */
class LanternKnowledgeRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LanternKnowledgeRepo"

        @Volatile
        private var INSTANCE: LanternKnowledgeRepository? = null

        fun getInstance(context: Context): LanternKnowledgeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanternKnowledgeRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val dbHelper = LanternDatabaseHelper(context)
    private val gson = Gson()
    private var isInitialized = false

    /**
     * Initializes repository, loading bundled JSON manuals from assets on first run.
     */
    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        try {
            val count = dbHelper.getChunkCount()
            if (count == 0) {
                Log.i(TAG, "Database empty. Ingesting pre-bundled emergency manuals...")
                loadBundledAssetManuals()
            } else {
                Log.d(TAG, "Knowledge base ready with $count pre-indexed emergency manual chunks.")
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Lantern Knowledge Base", e)
        }
    }

    private fun loadBundledAssetManuals() {
        val assetFiles = listOf(
            "lantern/first_aid_cpr.json",
            "lantern/water_sanitation.json",
            "lantern/disaster_survival.json"
        )

        val listType = object : TypeToken<List<LanternChunk>>() {}.type

        for (fileName in assetFiles) {
            try {
                context.assets.open(fileName).use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val chunks: List<LanternChunk> = gson.fromJson(reader, listType)
                        for (chunk in chunks) {
                            dbHelper.insertOrUpdateChunk(chunk)
                        }
                        Log.i(TAG, "Indexed ${chunks.size} chunks from $fileName")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load asset file: $fileName", e)
            }
        }
    }

    /**
     * Search manual chunks using native FTS5 BM25.
     */
    suspend fun search(
        query: String,
        categoryFilter: LanternCategory? = null,
        limit: Int = 5
    ): List<Pair<LanternChunk, String?>> = withContext(Dispatchers.IO) {
        initializeIfNeeded()
        return@withContext dbHelper.searchChunks(query, categoryFilter, limit)
    }

    /**
     * Retrieve all chunks under a category.
     */
    suspend fun getChunksByCategory(category: LanternCategory?, limit: Int = 10): List<LanternChunk> = withContext(Dispatchers.IO) {
        initializeIfNeeded()
        return@withContext dbHelper.getAllByCategory(category, limit)
    }
}
