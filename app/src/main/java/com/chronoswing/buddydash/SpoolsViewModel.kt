package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.ArchiveSpoolLookupFilter
import com.chronoswing.buddydash.util.SpoolInventoryFilter
import com.chronoswing.buddydash.util.applySpoolInventorySearch
import com.chronoswing.buddydash.util.spoolMatchesArchiveLookupFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpoolsUiState(
    val spools: List<SpoolInventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val hasCredentials: Boolean = false,
    val searchQuery: String = "",
    val filter: SpoolInventoryFilter = SpoolInventoryFilter.All,
    val archiveLookupFilter: ArchiveSpoolLookupFilter? = null,
) {
    val showArchiveMatchHeader: Boolean get() = archiveLookupFilter != null

    fun filteredSpools(): List<SpoolInventoryItem> {
        var pool = spools
        archiveLookupFilter?.let { lookup ->
            pool = pool.filter { spoolMatchesArchiveLookupFilter(it, lookup) }
        }
        return applySpoolInventorySearch(pool, searchQuery, filter)
    }
}

class SpoolsViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpoolsUiState())
    val uiState: StateFlow<SpoolsUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.serverUrl,
                settingsRepository.apiKey,
            ) { url, key ->
                url to key
            }.collect { (url, key) ->
                _uiState.update {
                    it.copy(
                        serverUrl = url,
                        apiKey = key,
                        hasCredentials = url.isNotBlank() && key.isNotBlank(),
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChange(filter: SpoolInventoryFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun applyInitialSearchQuery(query: String) {
        if (query.isBlank()) return
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun applyArchiveMaterialLookup(lookupFilter: ArchiveSpoolLookupFilter) {
        _uiState.update {
            it.copy(
                searchQuery = "",
                archiveLookupFilter = lookupFilter,
            )
        }
    }

    fun clearArchiveLookupFilter() {
        _uiState.update {
            it.copy(
                archiveLookupFilter = null,
                searchQuery = "",
            )
        }
    }

    fun loadSpools(showLoading: Boolean = false, fromPull: Boolean = false) {
        val state = _uiState.value
        if (!state.hasCredentials) {
            _uiState.update { it.copy(error = "Configure server URL and API key in Settings") }
            return
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (fromPull) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else if (showLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            val result = apiClient.fetchSpoolInventory(state.serverUrl, state.apiKey)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    spools = result.getOrElse { emptyList() },
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }
}
