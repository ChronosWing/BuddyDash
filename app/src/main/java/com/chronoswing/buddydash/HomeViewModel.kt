package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
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
    val hasCompletedLoad: Boolean = false,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
    val lastUpdatedAtMillis: Long? = null,
)

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var listFetchJob: Job? = null
    private var enrichJob: Job? = null
    private val manualRefreshGuard = RefreshGuard()
    private var pendingRefreshSource: RefreshSource? = null

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
                    )
                }
                if (hasCredentials) {
                    HomeLoadTiming.log("settings loaded")
                    if (!hadCredentials || !_uiState.value.hasCompletedLoad) {
                        loadPrinters(showLoading = _uiState.value.printers.isEmpty())
                    }
                }
            }
        }
    }

    fun onRefreshErrorShown() {
        _uiState.update { it.copy(refreshError = null) }
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
                    _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
                } else if (showLoading) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
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
                            _uiState.update {
                                it.copy(
                                    printers = emptyList(),
                                    isLoading = false,
                                    isRefreshing = false,
                                    isEnriching = false,
                                    hasCompletedLoad = true,
                                    error = null,
                                    refreshError = null,
                                    lastUpdatedAtMillis = System.currentTimeMillis(),
                                )
                            }
                            flushPendingRefresh()
                            return@launch
                        }

                        if (hadPrinters) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = true,
                                    isEnriching = true,
                                    refreshError = null,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    printers = basePrinters,
                                    isLoading = false,
                                    hasCompletedLoad = true,
                                    error = null,
                                    refreshError = null,
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
                                    refreshError = message,
                                    hasCompletedLoad = true,
                                )
                            } else {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isEnriching = false,
                                    error = message,
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
                    _uiState.update {
                        it.copy(
                            printers = enriched,
                            isEnriching = false,
                            isRefreshing = false,
                            lastUpdatedAtMillis = System.currentTimeMillis(),
                            refreshError = null,
                        )
                    }
                    HomeLoadTiming.log("secondary data applied to printer cards")
                    flushPendingRefresh()
                },
                onFailure = { error ->
                    if (BuddyDashDebug.enabled) {
                        Log.w(TAG_HOME_VM, "enrichPrinters failed source=$refreshSource", error)
                    }
                    _uiState.update {
                        it.copy(
                            isEnriching = false,
                            isRefreshing = false,
                            refreshError = error.toUserNetworkMessage("Could not refresh printers"),
                        )
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
        super.onCleared()
    }
}
