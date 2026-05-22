package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.ArchivePrinterFilter
import com.chronoswing.buddydash.util.ArchiveResultFilter
import com.chronoswing.buddydash.util.ArchiveStatsSnapshot
import com.chronoswing.buddydash.util.ArchiveStatsTimeRange
import com.chronoswing.buddydash.util.ArchivesSection
import com.chronoswing.buddydash.util.applyArchiveListFilters
import com.chronoswing.buddydash.util.computeArchiveStats
import com.chronoswing.buddydash.util.filterArchivesForStatsRange
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.logArchiveStatsDateFilterSummary
import com.chronoswing.buddydash.util.RefreshGuard
import com.chronoswing.buddydash.util.toUserNetworkMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG_ARCHIVES_VM = "BuddyDash/ArchivesVM"

data class ArchivesUiState(
    val archives: List<PrintArchive> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val refreshError: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    /** True after settings DataStore has emitted at least once. */
    val settingsReady: Boolean = false,
    val hasCredentials: Boolean = false,
    val searchQuery: String = "",
    val filter: ArchiveResultFilter = ArchiveResultFilter.All,
    val section: ArchivesSection = ArchivesSection.History,
    val statsTimeRange: ArchiveStatsTimeRange = ArchiveStatsTimeRange.Last30Days,
    val printerFilter: ArchivePrinterFilter? = null,
) {
    val filteredArchives: List<PrintArchive> =
        applyArchiveListFilters(
            archives = archives,
            query = searchQuery,
            filter = filter,
            printerId = printerFilter?.printerId,
        )

    val statsSnapshot: ArchiveStatsSnapshot =
        computeArchiveStats(
            filterArchivesForStatsRange(
                if (printerFilter != null) {
                    archives.filter { it.printerId == printerFilter.printerId }
                } else {
                    archives
                },
                statsTimeRange,
            ),
        )
}

class ArchivesViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchivesUiState())
    val uiState: StateFlow<ArchivesUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private val manualRefreshGuard = RefreshGuard()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.serverUrl,
                settingsRepository.apiKey,
                settingsRepository.cameraToken,
            ) { url, key, cameraToken ->
                Triple(url, key, cameraToken)
            }.collect { (url, key, cameraToken) ->
                val hasCredentials = url.isNotBlank() && key.isNotBlank()
                val wasReady = _uiState.value.settingsReady
                _uiState.update {
                    it.copy(
                        serverUrl = url,
                        apiKey = key,
                        cameraToken = cameraToken,
                        hasCredentials = hasCredentials,
                        settingsReady = true,
                        error = if (!wasReady) null else it.error,
                    )
                }
                if (!wasReady && hasCredentials) {
                    loadArchives(showLoading = _uiState.value.archives.isEmpty())
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun applyInitialSearchQuery(query: String) {
        if (query.isBlank()) return
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearSearchQuery() {
        if (_uiState.value.searchQuery.isEmpty()) return
        _uiState.update { it.copy(searchQuery = "") }
    }

    fun onRefreshErrorShown() {
        _uiState.update { it.copy(refreshError = null) }
    }

    fun refreshFromBottomNavReselect() {
        if (BuddyDashDebug.enabled) {
            Log.d(TAG_ARCHIVES_VM, "refreshFromBottomNavReselect")
        }
        _uiState.update {
            it.copy(
                searchQuery = "",
                printerFilter = null,
                refreshError = null,
            )
        }
        loadArchives(showLoading = false, fromBottomNavReselect = true)
    }

    fun applyPrinterFilter(printerId: Int, printerName: String) {
        val filter = ArchivePrinterFilter(printerId, printerName)
        val current = _uiState.value
        if (current.printerFilter == filter && current.section == ArchivesSection.History) return
        _uiState.update {
            it.copy(
                printerFilter = filter,
                section = ArchivesSection.History,
            )
        }
        loadArchives(showLoading = current.archives.isEmpty())
    }

    fun clearPrinterFilter() {
        if (_uiState.value.printerFilter == null) return
        _uiState.update { it.copy(printerFilter = null) }
        loadArchives(showLoading = _uiState.value.archives.isEmpty())
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

    fun loadArchives(
        showLoading: Boolean = false,
        fromPull: Boolean = false,
        fromUser: Boolean = false,
        fromBottomNavReselect: Boolean = false,
    ) {
        val state = _uiState.value
        if (!state.settingsReady) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            return
        }
        if (!state.hasCredentials) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                )
            }
            return
        }
        if (fromBottomNavReselect) {
            fetchJob?.cancel()
        } else {
            if (fromUser && !fromPull && manualRefreshGuard.shouldSkipManualRefresh()) return
            if (fromUser && fetchJob?.isActive == true) return
        }

        val hadArchives = state.archives.isNotEmpty()
        val isInitialLoad = !hadArchives
        if (BuddyDashDebug.enabled) {
            Log.d(
                TAG_ARCHIVES_VM,
                "loadArchives showLoading=$showLoading fromPull=$fromPull fromUser=$fromUser " +
                    "fromBottomNavReselect=$fromBottomNavReselect hadArchives=$hadArchives",
            )
        }

        if (!fromBottomNavReselect) {
            fetchJob?.cancel()
        }
        fetchJob = viewModelScope.launch {
            val credentials = _uiState.value
            if (!credentials.hasCredentials ||
                credentials.serverUrl.isBlank() ||
                credentials.apiKey.isBlank()
            ) {
                return@launch
            }
            if (!isInitialLoad) {
                _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
            } else if (showLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            val result = apiClient.fetchArchives(
                credentials.serverUrl,
                credentials.apiKey,
                printerId = credentials.printerFilter?.printerId,
            )
            result.fold(
                onSuccess = { archives ->
                    if (fromBottomNavReselect && BuddyDashDebug.enabled) {
                        Log.d(TAG_ARCHIVES_VM, "bottomNavReselect refresh success")
                    }
                    logArchiveStatsDateFilterSummary(archives)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            archives = archives,
                            error = null,
                            refreshError = null,
                        )
                    }
                },
                onFailure = { error ->
                    val fallback = if (hadArchives) {
                        "Could not refresh"
                    } else {
                        "Failed to load archives"
                    }
                    val message = error.toUserNetworkMessage(fallback)
                    if (fromBottomNavReselect && BuddyDashDebug.enabled) {
                        Log.w(TAG_ARCHIVES_VM, "bottomNavReselect refresh failure", error)
                    }
                    _uiState.update { current ->
                        if (current.archives.isNotEmpty()) {
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                refreshError = message,
                            )
                        } else {
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = message,
                                refreshError = null,
                            )
                        }
                    }
                },
            )
        }
    }

    override fun onCleared() {
        fetchJob?.cancel()
        super.onCleared()
    }
}
