package com.inception.android.lantern.mesh

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.inception.android.lantern.model.LanternCategory
import com.inception.android.lantern.model.LanternMeshQuery
import com.inception.android.lantern.model.LanternMeshResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Handles P2P Emergency Knowledge sharing across the local BLE mesh.
 * Broadcasts emergency query requests when a device has zero local hits.
 */
class LanternMeshCoordinator private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LanternMeshCoordinator"
        const val LANTERN_BROADCAST_PREFIX = "[LANTERN_QUERY]"
        const val LANTERN_RESPONSE_PREFIX = "[LANTERN_RESP]"

        @Volatile
        private var INSTANCE: LanternMeshCoordinator? = null

        fun getInstance(context: Context): LanternMeshCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanternMeshCoordinator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val gson = Gson()
    private val _meshResponses = MutableStateFlow<List<LanternMeshResponse>>(emptyList())
    val meshResponses: StateFlow<List<LanternMeshResponse>> = _meshResponses.asStateFlow()

    private val _isQueryingMesh = MutableStateFlow(false)
    val isQueryingMesh: StateFlow<Boolean> = _isQueryingMesh.asStateFlow()

    /**
     * Broadcast an emergency knowledge query to nearby mesh peers.
     */
    fun createQueryBroadcastMessage(queryText: String, category: LanternCategory, myPeerId: String): String {
        _isQueryingMesh.value = true
        val query = LanternMeshQuery(
            queryId = UUID.randomUUID().toString().take(8),
            senderPeerId = myPeerId,
            queryText = queryText,
            category = category
        )
        return "$LANTERN_BROADCAST_PREFIX ${gson.toJson(query)}"
    }

    /**
     * Process potential incoming Lantern packet from mesh chat stream.
     */
    fun processIncomingMessage(content: String, senderPeerId: String, senderNickname: String): Boolean {
        if (content.startsWith(LANTERN_RESPONSE_PREFIX)) {
            try {
                val json = content.removePrefix(LANTERN_RESPONSE_PREFIX).trim()
                val response = gson.fromJson(json, LanternMeshResponse::class.java)
                val current = _meshResponses.value.toMutableList()
                current.add(0, response)
                _meshResponses.value = current
                _isQueryingMesh.value = false
                Log.i(TAG, "Received verified Lantern response from ${response.responderNickname}: ${response.matchedTitle}")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing Lantern mesh response", e)
            }
        }
        return false
    }

    fun clearResponses() {
        _meshResponses.value = emptyList()
        _isQueryingMesh.value = false
    }
}
