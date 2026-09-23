package com.inception.android.lantern.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inception.android.lantern.data.LanternModelManager
import com.inception.android.lantern.domain.LanternBatteryPolicy
import com.inception.android.lantern.domain.LanternSearchEngine
import com.inception.android.lantern.mesh.LanternMeshCoordinator
import com.inception.android.lantern.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing Lantern UI states, search execution, model downloading, and mesh queries.
 */
class LanternViewModel(application: Application) : AndroidViewModel(application) {

    private val searchEngine = LanternSearchEngine.getInstance(application)
    val modelManager = LanternModelManager.getInstance(application)
    val meshCoordinator = LanternMeshCoordinator.getInstance(application)
    val batteryPolicy = LanternBatteryPolicy(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<LanternCategory?>(null)
    val selectedCategory: StateFlow<LanternCategory?> = _selectedCategory.asStateFlow()

    private val _searchResult = MutableStateFlow<LanternSearchResult?>(null)
    val searchResult: StateFlow<LanternSearchResult?> = _searchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showModelHub = MutableStateFlow(false)
    val showModelHub: StateFlow<Boolean> = _showModelHub.asStateFlow()

    private val _selectedChunkForDetail = MutableStateFlow<LanternChunk?>(null)
    val selectedChunkForDetail: StateFlow<LanternChunk?> = _selectedChunkForDetail.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Load initial category chunks
        loadDefaultCategory(null)
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            loadDefaultCategory(_selectedCategory.value)
            return
        }

        searchJob = viewModelScope.launch {
            delay(150) // Small debounce for typing
            _isLoading.value = true
            try {
                val result = searchEngine.query(query, _selectedCategory.value)
                _searchResult.value = result
            } catch (t: Throwable) {
                android.util.Log.e("LanternViewModel", "Error querying Lantern", t)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onCategorySelected(category: LanternCategory?) {
        val newCategory = if (_selectedCategory.value == category) null else category
        _selectedCategory.value = newCategory

        if (_searchQuery.value.isNotBlank()) {
            onQueryChanged(_searchQuery.value)
        } else {
            loadDefaultCategory(newCategory)
        }
    }

    private fun loadDefaultCategory(category: LanternCategory?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = searchEngine.loadCategoryManuals(category)
                _searchResult.value = result
            } catch (t: Throwable) {
                android.util.Log.e("LanternViewModel", "Error loading category manuals", t)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openModelHub() {
        _showModelHub.value = true
    }

    fun closeModelHub() {
        _showModelHub.value = false
    }

    fun selectChunkDetail(chunk: LanternChunk?) {
        _selectedChunkForDetail.value = chunk
    }

    fun downloadModel(tier: LanternModelTier) {
        modelManager.startDownload(tier)
    }

    fun cancelModelDownload(tier: LanternModelTier) {
        modelManager.cancelDownload(tier)
    }

    fun deleteModel(tier: LanternModelTier) {
        modelManager.deleteModel(tier)
    }

    fun setActiveModelTier(tier: LanternModelTier?) {
        modelManager.setActiveTier(tier)
        // Re-execute current query with newly active tier
        if (_searchQuery.value.isNotBlank()) {
            onQueryChanged(_searchQuery.value)
        }
    }
}
