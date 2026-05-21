package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.ArchiveResultFilter
import com.chronoswing.buddydash.util.ArchiveStatsSnapshot
import com.chronoswing.buddydash.util.ArchiveStatsTimeRange
import com.chronoswing.buddydash.util.ArchivesSection
import com.chronoswing.buddydash.util.applyArchiveSearch
import com.chronoswing.buddydash.util.computeArchiveStats
import com.chronoswing.buddydash.util.filterArchivesForStatsRange
import com.chronoswing.buddydash.util.logArchiveStatsDateFilterSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArchivesUiState(
    val archives: List<PrintArchive> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
    val searchQuery: String = "",
    val filter: ArchiveResultFilter = ArchiveResultFilter.All,
    val section: ArchivesSection = ArchivesSection.History,
    val statsTimeRange: ArchiveStatsTimeRange = ArchiveStatsTimeRange.Last30Days,
) {
    val filteredArchives: List<PrintArchive> =
        applyArchiveSearch(archives, searchQuery, filter)

    val statsSnapshot: ArchiveStatsSnapshot =
        computeArchiveStats(filterArchivesForStatsRange(archives, statsTimeRange))
}

class ArchivesViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchivesUiState())
    val uiState: StateFlow<ArchivesUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.serverUrl,
                settingsRepository.apiKey,
                settingsRepository.cameraToken,
            ) { url, key, cameraToken ->
                Triple(url, key, cameraToken)
            }.collect { (url, key, cameraToken) ->
                _uiState.update {
                    it.copy(
                        serverUrl = url,
                        apiKey = key,
                        cameraToken = cameraToken,
                        hasCredentials = url.isNotBlank() && key.isNotBlank(),
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChange(filter: ArchiveResultFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun onSectionChange(section: ArchivesSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun onStatsTimeRangeChange(range: ArchiveStatsTimeRange) {
        _uiState.update { it.copy(statsTimeRange = range) }
    }

    fun loadArchives(showLoading: Boolean = false, fromPull: Boolean = false) {
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
            val result = apiClient.fetchArchives(state.serverUrl, state.apiKey)
            result.fold(
                onSuccess = { archives ->
                    logArchiveStatsDateFilterSummary(archives)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            archives = archives,
                            error = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            archives = emptyList(),
                            error = error.message ?: "Failed to load archives",
                        )
                    }
                },
            )
        }
    }
}
