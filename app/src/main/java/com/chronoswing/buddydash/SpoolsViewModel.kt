package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.SpoolsCacheRepository
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.ArchiveSpoolLookupFilter
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.RefreshGuard
import com.chronoswing.buddydash.util.RefreshIntervals
import com.chronoswing.buddydash.util.RefreshSource
import com.chronoswing.buddydash.util.isDataStale
import com.chronoswing.buddydash.util.logRefreshDecision
import com.chronoswing.buddydash.util.PrinterFilamentActivity
import com.chronoswing.buddydash.util.SpoolInventoryCardUsage
import com.chronoswing.buddydash.util.SpoolInventoryFilter
import com.chronoswing.buddydash.util.applySpoolInventorySearch
import com.chronoswing.buddydash.util.resolveSpoolInventoryCardUsage
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
    val isStaleCachedData: Boolean = false,
    /** True after the first fetch finishes (success or failure). */
    val hasCompletedLoad: Boolean = false,
    val lastUpdatedAtMillis: Long? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val hasCredentials: Boolean = false,
    val searchQuery: String = "",
    val filter: SpoolInventoryFilter = SpoolInventoryFilter.All,
    val archiveLookupFilter: ArchiveSpoolLookupFilter? = null,
    val printerFilamentActivityById: Map<Int, PrinterFilamentActivity> = emptyMap(),
    /** True after the first network load attempt completes (success or failure). Gates stale/error UI. */
    val hasAttemptedNetworkLoad: Boolean = false,
) {
    val showArchiveMatchHeader: Boolean get() = archiveLookupFilter != null

    fun cardUsageFor(spool: SpoolInventoryItem): SpoolInventoryCardUsage =
        resolveSpoolInventoryCardUsage(spool, printerFilamentActivityById)

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
    private val spoolsCacheRepository: SpoolsCacheRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpoolsUiState())
    val uiState: StateFlow<SpoolsUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private val manualRefreshGuard = RefreshGuard()
    private var lastNetworkLoadServerKey: String? = null

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
                if (hasCredentials) {
                    val serverKey = HomePrintersCacheRepository.cacheServerKey(url)
                    val shouldNetworkLoad =
                        !hadCredentials || lastNetworkLoadServerKey != serverKey
                    viewModelScope.launch {
                        hydrateFromDiskCache(url)
                        if (shouldNetworkLoad && serverKey != null) {
                            lastNetworkLoadServerKey = serverKey
                            loadSpools(showLoading = _uiState.value.spools.isEmpty())
                        }
                    }
                } else {
                    lastNetworkLoadServerKey = null
                }
            }
        }
    }

    private suspend fun hydrateFromDiskCache(serverUrl: String) {
        val snapshot = spoolsCacheRepository.load(serverUrl) ?: return
        _uiState.update {
            it.copy(
                spools = snapshot.spools,
                printerFilamentActivityById = snapshot.printerFilamentActivityById,
                lastUpdatedAtMillis = snapshot.lastUpdatedAtMillis,
                hasCompletedLoad = true,
                isLoading = false,
                error = null,
                refreshError = null,
                isStaleCachedData = true,
            )
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
        applySectionRootFromNavigation()
        loadSpools(showLoading = false, fromBottomNavReselect = true)
    }

    fun refreshOnAppResume(currentRoute: String? = null) {
        val state = _uiState.value
        if (!state.hasCredentials || !state.hasCompletedLoad) return
        val stale = isDataStale(state.lastUpdatedAtMillis, RefreshIntervals.SPOOLS_MS)
        logRefreshDecision(
            screen = "Spools",
            source = RefreshSource.APP_RESUME,
            currentRoute = currentRoute,
            lastUpdatedAtMillis = state.lastUpdatedAtMillis,
            intervalMs = RefreshIntervals.SPOOLS_MS,
            stale = stale,
            refreshTriggered = stale,
        )
        if (!stale) return
        if (fetchJob?.isActive == true) return
        loadSpools(showLoading = false)
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
                    _uiState.update { it.copy(isRefreshing = true) }
                } else if (showLoading) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }

                val credentials = _uiState.value
                ensureActive()
                val spoolsResult = apiClient.fetchSpoolInventory(
                    credentials.serverUrl,
                    credentials.apiKey,
                )
                val activityResult = apiClient.fetchPrinterFilamentActivityById(
                    credentials.serverUrl,
                    credentials.apiKey,
                )
                ensureActive()

                spoolsResult.fold(
                    onSuccess = { spools ->
                        val activityById = activityResult.getOrElse { emptyMap() }
                        if (BuddyDashDebug.enabled) {
                            val printingCount = spools.count {
                                resolveSpoolInventoryCardUsage(it, activityById) ==
                                    SpoolInventoryCardUsage.Printing
                            }
                            Log.d(
                                TAG_SPOOLS_VM,
                                "loadSpools success mappedCount=${spools.size} " +
                                    "uiState=${_uiState.value.spools.size}->${spools.size} " +
                                    "printingSpools=$printingCount printers=${activityById.size}",
                            )
                            if (fromBottomNavReselect) {
                                Log.d(TAG_SPOOLS_VM, "bottomNavReselect refresh success")
                            }
                        }
                        val updatedAt = System.currentTimeMillis()
                        spoolsCacheRepository.save(
                            credentials.serverUrl,
                            spools,
                            activityById,
                            updatedAt,
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                spools = spools,
                                printerFilamentActivityById = activityById,
                                error = null,
                                refreshError = null,
                                isStaleCachedData = false,
                                hasCompletedLoad = true,
                                lastUpdatedAtMillis = updatedAt,
                                hasAttemptedNetworkLoad = true,
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
                                    isStaleCachedData = true,
                                    hasCompletedLoad = true,
                                    hasAttemptedNetworkLoad = true,
                                )
                            } else {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = message,
                                    refreshError = null,
                                    hasCompletedLoad = true,
                                    hasAttemptedNetworkLoad = true,
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
