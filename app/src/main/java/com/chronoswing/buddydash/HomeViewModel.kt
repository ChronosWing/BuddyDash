package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.SpoolsCacheRepository
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.HomeLoadTiming
import com.chronoswing.buddydash.util.RefreshGuard
import com.chronoswing.buddydash.util.RefreshIntervals
import com.chronoswing.buddydash.util.RefreshSource
import com.chronoswing.buddydash.util.cancelsInFlightRefresh
import com.chronoswing.buddydash.util.forcesHomeRefresh
import com.chronoswing.buddydash.util.isConnectionDisplayStale
import com.chronoswing.buddydash.util.isDataStale
import com.chronoswing.buddydash.util.logRefreshDecision
import com.chronoswing.buddydash.util.toUserNetworkMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG_HOME_VM = "BuddyDash/HomeVM"

data class HomeUiState(
    val printers: List<Printer> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    /** True while per-printer status/queue enrichment runs after the base list is shown. */
    val isEnriching: Boolean = false,
    val error: String? = null,
    val refreshError: String? = null,
    /** True while Home shows last-known printer cards after a failed refresh or disk hydrate. */
    val isStaleCachedData: Boolean = false,
    val hasCompletedLoad: Boolean = false,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
    /** True after settings DataStore has emitted at least once. */
    val settingsReady: Boolean = false,
    val lastUpdatedAtMillis: Long? = null,
    /** Null until spool inventory is known from cache or network. */
    val loadedSpoolCount: Int? = null,
)

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
    private val homePrintersCacheRepository: HomePrintersCacheRepository,
    private val spoolsCacheRepository: SpoolsCacheRepository,
) : ViewModel() {

    private var spoolCountJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var listFetchJob: Job? = null
    private var enrichJob: Job? = null
    private val manualRefreshGuard = RefreshGuard()
    private var pendingRefreshSource: RefreshSource? = null
    private var lastNetworkLoadServerKey: String? = null

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
                val hadCredentials = _uiState.value.hasCredentials
                _uiState.update {
                    it.copy(
                        serverUrl = url,
                        apiKey = key,
                        cameraToken = cameraToken,
                        hasCredentials = hasCredentials,
                        settingsReady = true,
                    )
                }
                if (hasCredentials) {
                    HomeLoadTiming.log("settings loaded")
                    val serverKey = HomePrintersCacheRepository.cacheServerKey(url)
                    val shouldNetworkLoad =
                        !hadCredentials || lastNetworkLoadServerKey != serverKey
                    viewModelScope.launch {
                        hydrateFromDiskCache(url)
                        hydrateSpoolCountFromDiskCache(url)
                        if (shouldNetworkLoad && serverKey != null) {
                            lastNetworkLoadServerKey = serverKey
                            loadPrinters(showLoading = _uiState.value.printers.isEmpty())
                        }
                    }
                } else {
                    lastNetworkLoadServerKey = null
                    viewModelScope.launch {
                        if (url.isNotBlank()) {
                            hydrateFromDiskCache(url)
                            hydrateSpoolCountFromDiskCache(url)
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isEnriching = false,
                                hasCompletedLoad = true,
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun hydrateFromDiskCache(serverUrl: String) {
        val snapshot = homePrintersCacheRepository.load(serverUrl)
        val newKey = HomePrintersCacheRepository.cacheServerKey(serverUrl)
        val currentKey = HomePrintersCacheRepository.cacheServerKey(_uiState.value.serverUrl)
        _uiState.update { state ->
            when {
                snapshot != null -> {
                    HomeLoadTiming.log("disk printer cache hydrated (count=${snapshot.printers.size})")
                    state.copy(
                        printers = snapshot.printers,
                        lastUpdatedAtMillis = snapshot.lastUpdatedAtMillis,
                        hasCompletedLoad = true,
                        isLoading = false,
                        error = null,
                        refreshError = null,
                        isStaleCachedData = true,
                    )
                }
                newKey != null && currentKey != null && newKey != currentKey -> {
                    state.copy(
                        printers = emptyList(),
                        lastUpdatedAtMillis = null,
                        hasCompletedLoad = false,
                        loadedSpoolCount = null,
                        error = null,
                        refreshError = null,
                        isStaleCachedData = false,
                    )
                }
                else -> state
            }
        }
    }

    private suspend fun hydrateSpoolCountFromDiskCache(serverUrl: String) {
        val count = spoolsCacheRepository.load(serverUrl)?.spools?.size ?: return
        _uiState.update { it.copy(loadedSpoolCount = count) }
    }

    private fun refreshLoadedSpoolCount() {
        val state = _uiState.value
        if (!state.hasCredentials) return
        spoolCountJob?.cancel()
        spoolCountJob = viewModelScope.launch {
            val credentials = _uiState.value
            val result = apiClient.fetchSpoolInventory(
                credentials.serverUrl,
                credentials.apiKey,
            )
            result.fold(
                onSuccess = { spools ->
                    val existing = spoolsCacheRepository.load(credentials.serverUrl)
                    val updatedAt = System.currentTimeMillis()
                    spoolsCacheRepository.save(
                        credentials.serverUrl,
                        spools,
                        existing?.printerFilamentActivityById.orEmpty(),
                        updatedAt,
                    )
                    _uiState.update { it.copy(loadedSpoolCount = spools.size) }
                },
                onFailure = { error ->
                    if (BuddyDashDebug.enabled) {
                        Log.w(TAG_HOME_VM, "refreshLoadedSpoolCount failed", error)
                    }
                },
            )
        }
    }

    fun refreshFromBottomNavReselect(currentRoute: String? = null) {
        requestRefresh(RefreshSource.BOTTOM_NAV_RESELECT, force = true, currentRoute = currentRoute)
    }

    fun refreshOnReturnFromDetail(currentRoute: String? = null) {
        requestRefresh(RefreshSource.RETURN_FROM_DETAIL, force = true, currentRoute = currentRoute)
    }

    fun refreshOnAppResume(currentRoute: String? = null) {
        requestRefresh(RefreshSource.APP_RESUME, force = true, currentRoute = currentRoute)
    }

    fun refreshManual() {
        requestRefresh(RefreshSource.MANUAL, force = true)
    }

    fun requestRefresh(
        source: RefreshSource,
        force: Boolean = false,
        currentRoute: String? = null,
    ) {
        val state = _uiState.value
        if (!state.hasCredentials) return

        val staleByAge = isDataStale(state.lastUpdatedAtMillis, RefreshIntervals.HOME_MS)
        val staleByConnection = isConnectionDisplayStale(state.lastUpdatedAtMillis)
        val stale = staleByAge || staleByConnection
        val shouldRefresh = force || source.forcesHomeRefresh(force) || stale

        logRefreshDecision(
            screen = "Home",
            source = source,
            currentRoute = currentRoute,
            lastUpdatedAtMillis = state.lastUpdatedAtMillis,
            intervalMs = RefreshIntervals.HOME_MS,
            stale = stale,
            refreshTriggered = shouldRefresh,
        )

        if (!shouldRefresh) return

        val refreshInFlight = listFetchJob?.isActive == true || enrichJob?.isActive == true
        if (refreshInFlight) {
            when (source) {
                RefreshSource.RETURN_FROM_DETAIL,
                RefreshSource.BOTTOM_NAV_RESELECT,
                RefreshSource.MANUAL,
                -> {
                    listFetchJob?.cancel()
                    enrichJob?.cancel()
                }
                RefreshSource.APP_RESUME -> {
                    pendingRefreshSource = source
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_HOME_VM, "refresh queued source=$source (in flight)")
                    }
                    return
                }
                RefreshSource.POLL -> return
            }
        }

        loadPrinters(
            showLoading = false,
            fromPull = source == RefreshSource.MANUAL,
            fromUser = source == RefreshSource.MANUAL,
            immediateRefresh = source.cancelsInFlightRefresh(),
            refreshSource = source,
        )
    }

    fun loadPrinters(
        showLoading: Boolean = false,
        fromPull: Boolean = false,
        fromUser: Boolean = false,
        immediateRefresh: Boolean = false,
        refreshSource: RefreshSource = RefreshSource.POLL,
    ) {
        val state = _uiState.value
        if (!state.hasCredentials) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isEnriching = false,
                    error = null,
                    refreshError = null,
                )
            }
            return
        }
        if (immediateRefresh) {
            listFetchJob?.cancel()
            enrichJob?.cancel()
        } else {
            if (fromUser && !fromPull && manualRefreshGuard.shouldSkipManualRefresh()) return
            if (fromUser && (listFetchJob?.isActive == true || enrichJob?.isActive == true)) return
            if (refreshSource == RefreshSource.POLL &&
                (listFetchJob?.isActive == true || enrichJob?.isActive == true)
            ) {
                return
            }
        }

        val hadPrinters = state.printers.isNotEmpty()
        val isInitialLoad = !hadPrinters
        if (BuddyDashDebug.enabled) {
            Log.d(
                TAG_HOME_VM,
                "loadPrinters source=$refreshSource showLoading=$showLoading fromPull=$fromPull " +
                    "fromUser=$fromUser immediateRefresh=$immediateRefresh hadPrinters=$hadPrinters",
            )
        }

        if (!immediateRefresh) {
            listFetchJob?.cancel()
            enrichJob?.cancel()
        }
        listFetchJob = viewModelScope.launch {
            try {
                if (!isInitialLoad) {
                    _uiState.update {
                        it.copy(
                            isRefreshing = true,
                            error = null,
                        )
                    }
                } else if (showLoading) {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            error = null,
                        )
                    }
                }

                val credentials = _uiState.value
                HomeLoadTiming.log("printer list request started")
                ensureActive()
                val listResult = apiClient.fetchPrinters(
                    credentials.serverUrl,
                    credentials.apiKey,
                )
                ensureActive()

                listResult.fold(
                    onSuccess = { basePrinters ->
                        HomeLoadTiming.log(
                            "printer list response (count=${basePrinters.size})",
                        )
                        if (basePrinters.isEmpty()) {
                            homePrintersCacheRepository.clear(credentials.serverUrl)
                            _uiState.update {
                                it.copy(
                                    printers = emptyList(),
                                    isLoading = false,
                                    isRefreshing = false,
                                    isEnriching = false,
                                    hasCompletedLoad = true,
                                    error = null,
                                    refreshError = null,
                                    isStaleCachedData = false,
                                    lastUpdatedAtMillis = System.currentTimeMillis(),
                                )
                            }
                            refreshLoadedSpoolCount()
                            flushPendingRefresh()
                            return@launch
                        }

                        refreshLoadedSpoolCount()
                        if (hadPrinters) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = true,
                                    isEnriching = true,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    printers = basePrinters,
                                    isLoading = false,
                                    hasCompletedLoad = true,
                                    error = null,
                                    isEnriching = true,
                                    isRefreshing = true,
                                )
                            }
                            HomeLoadTiming.log("first printer cards available in state")
                        }

                        enrichJob = viewModelScope.launch {
                            enrichPrinters(
                                credentials.serverUrl,
                                credentials.apiKey,
                                basePrinters,
                                refreshSource = refreshSource,
                            )
                        }
                    },
                    onFailure = { error ->
                        val message = error.toUserNetworkMessage(
                            if (hadPrinters) "Could not refresh printers" else "Failed to load printers",
                        )
                        _uiState.update { current ->
                            if (current.printers.isNotEmpty()) {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isEnriching = false,
                                    error = null,
                                    refreshError = message,
                                    isStaleCachedData = true,
                                    hasCompletedLoad = true,
                                )
                            } else {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isEnriching = false,
                                    error = message,
                                    refreshError = null,
                                    hasCompletedLoad = true,
                                )
                            }
                        }
                        flushPendingRefresh()
                    },
                )
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private suspend fun enrichPrinters(
        serverUrl: String,
        apiKey: String,
        basePrinters: List<Printer>,
        refreshSource: RefreshSource = RefreshSource.POLL,
    ) {
        try {
            currentCoroutineContext().ensureActive()
            val result = apiClient.enrichPrintersForHome(serverUrl, apiKey, basePrinters)
            currentCoroutineContext().ensureActive()
            result.fold(
                onSuccess = { enriched ->
                    val updatedAt = System.currentTimeMillis()
                    homePrintersCacheRepository.save(serverUrl, enriched, updatedAt)
                    _uiState.update {
                        it.copy(
                            printers = enriched,
                            isEnriching = false,
                            isRefreshing = false,
                            lastUpdatedAtMillis = updatedAt,
                            error = null,
                            refreshError = null,
                            isStaleCachedData = false,
                        )
                    }
                    HomeLoadTiming.log("secondary data applied to printer cards")
                    refreshLoadedSpoolCount()
                    flushPendingRefresh()
                },
                onFailure = { error ->
                    if (BuddyDashDebug.enabled) {
                        Log.w(TAG_HOME_VM, "enrichPrinters failed source=$refreshSource", error)
                    }
                    refreshLoadedSpoolCount()
                    _uiState.update { current ->
                        val message = error.toUserNetworkMessage("Could not refresh printers")
                        if (current.printers.isNotEmpty()) {
                            current.copy(
                                isEnriching = false,
                                isRefreshing = false,
                                error = null,
                                refreshError = message,
                                isStaleCachedData = true,
                            )
                        } else {
                            current.copy(
                                isEnriching = false,
                                isRefreshing = false,
                                refreshError = message,
                            )
                        }
                    }
                    flushPendingRefresh()
                },
            )
        } catch (e: CancellationException) {
            throw e
        }
    }

    private fun flushPendingRefresh() {
        val pending = pendingRefreshSource ?: return
        pendingRefreshSource = null
        if (BuddyDashDebug.enabled) {
            Log.d(TAG_HOME_VM, "running queued refresh source=$pending")
        }
        requestRefresh(pending, force = true)
    }

    override fun onCleared() {
        listFetchJob?.cancel()
        enrichJob?.cancel()
        spoolCountJob?.cancel()
        super.onCleared()
    }
}
