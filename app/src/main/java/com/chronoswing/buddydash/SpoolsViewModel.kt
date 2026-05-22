package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.ArchiveSpoolLookupFilter
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.RefreshGuard
import com.chronoswing.buddydash.util.SpoolInventoryFilter
import com.chronoswing.buddydash.util.applySpoolInventorySearch
import com.chronoswing.buddydash.util.spoolMatchesArchiveLookupFilter
import com.chronoswing.buddydash.util.toUserNetworkMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG_SPOOLS_VM = "BuddyDash/SpoolsVM"

data class SpoolsUiState(
    val spools: List<SpoolInventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    /** Blocking error when inventory never loaded successfully. */
    val error: String? = null,
    /** Snackbar-only error after a failed refresh when spools are already shown. */
    val refreshError: String? = null,
    /** True after the first fetch finishes (success or failure). */
    val hasCompletedLoad: Boolean = false,
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
    private val manualRefreshGuard = RefreshGuard()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.serverUrl,
                settingsRepository.apiKey,
            ) { url, key ->
                url to key
            }.collect { (url, key) ->
                val hasCredentials = url.isNotBlank() && key.isNotBlank()
                val hadCredentials = _uiState.value.hasCredentials
                _uiState.update {
                    it.copy(
                        serverUrl = url,
                        apiKey = key,
                        hasCredentials = hasCredentials,
                    )
                }
                if (hasCredentials && (!hadCredentials || !_uiState.value.hasCompletedLoad)) {
                    loadSpools(showLoading = _uiState.value.spools.isEmpty())
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

    fun onRefreshErrorShown() {
        _uiState.update { it.copy(refreshError = null) }
    }

    /** Clears archive lookup and search when bottom nav opens unfiltered Spools root. */
    fun applySectionRootFromNavigation() {
        _uiState.update {
            it.copy(
                archiveLookupFilter = null,
                searchQuery = "",
            )
        }
    }

    fun refreshFromBottomNavReselect() {
        if (BuddyDashDebug.enabled) {
            Log.d(TAG_SPOOLS_VM, "refreshFromBottomNavReselect")
        }
        applySectionRootFromNavigation()
        loadSpools(showLoading = false, fromBottomNavReselect = true)
    }

    fun loadSpools(
        showLoading: Boolean = false,
        fromPull: Boolean = false,
        fromUser: Boolean = false,
        fromBottomNavReselect: Boolean = false,
    ) {
        val state = _uiState.value
        if (!state.hasCredentials) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    refreshError = null,
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

        val hadSpools = state.spools.isNotEmpty()
        val isInitialLoad = !hadSpools
        if (BuddyDashDebug.enabled) {
            Log.d(
                TAG_SPOOLS_VM,
                "loadSpools start showLoading=$showLoading fromPull=$fromPull fromUser=$fromUser " +
                    "fromBottomNavReselect=$fromBottomNavReselect hadSpools=$hadSpools " +
                    "hasCompletedLoad=${state.hasCompletedLoad} " +
                    "endpoint=${BambuddyApi.inventorySpoolsPath()}",
            )
        }

        if (!fromBottomNavReselect) {
            fetchJob?.cancel()
        }
        fetchJob = viewModelScope.launch {
            try {
                if (!isInitialLoad) {
                    _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
                } else if (showLoading) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }

                val credentials = _uiState.value
                ensureActive()
                val result = apiClient.fetchSpoolInventory(
                    credentials.serverUrl,
                    credentials.apiKey,
                )
                ensureActive()

                result.fold(
                    onSuccess = { spools ->
                        if (BuddyDashDebug.enabled) {
                            Log.d(
                                TAG_SPOOLS_VM,
                                "loadSpools success mappedCount=${spools.size} " +
                                    "uiState=${_uiState.value.spools.size}->${spools.size}",
                            )
                            if (fromBottomNavReselect) {
                                Log.d(TAG_SPOOLS_VM, "bottomNavReselect refresh success")
                            }
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                spools = spools,
                                error = null,
                                refreshError = null,
                                hasCompletedLoad = true,
                            )
                        }
                    },
                    onFailure = { error ->
                        val fallback = if (hadSpools) {
                            "Could not refresh"
                        } else {
                            "Failed to load spools"
                        }
                        val message = error.toUserNetworkMessage(fallback)
                        if (BuddyDashDebug.enabled) {
                            Log.w(TAG_SPOOLS_VM, "loadSpools failure: $message", error)
                            if (fromBottomNavReselect) {
                                Log.w(TAG_SPOOLS_VM, "bottomNavReselect refresh failure", error)
                            }
                        }
                        _uiState.update { current ->
                            if (current.spools.isNotEmpty()) {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    refreshError = message,
                                    hasCompletedLoad = true,
                                )
                            } else {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = message,
                                    refreshError = null,
                                    hasCompletedLoad = true,
                                )
                            }
                        }
                    },
                )
            } catch (e: CancellationException) {
                if (BuddyDashDebug.enabled) {
                    Log.d(TAG_SPOOLS_VM, "loadSpools cancelled (keeping prior list)")
                }
                throw e
            }
        }
    }

    override fun onCleared() {
        fetchJob?.cancel()
        super.onCleared()
    }
}
