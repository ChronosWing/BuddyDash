package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.PrinterCardPrefsRepository
import com.chronoswing.buddydash.data.PrinterCardVisibility
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.SpoolsCacheRepository
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.HomeLoadTiming
import com.chronoswing.buddydash.util.RefreshGuard
import com.chronoswing.buddydash.util.toggleSmartPlugPower
import com.chronoswing.buddydash.util.NfcActionOutcome
import com.chronoswing.buddydash.util.withEnrichFallbacks
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

/**
 * After app resume, suppress the connected=false flash for this many ms.
 *
 * The Bambulab API can transiently return connected=false for 1–3 s right after
 * the device wakes from sleep or a Fold is opened. This window covers that gap while
 * still showing real offline quickly on the next poll cycle (~15 s later).
 */
private const val RESUME_OFFLINE_FLASH_GUARD_MS = 8_000L

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
    /** True after the first network load attempt completes (success or failure). Gates stale/error UI. */
    val hasAttemptedNetworkLoad: Boolean = false,
    /** Null until spool inventory is known from cache or network. */
    val loadedSpoolCount: Int? = null,
    /** Debug-only multipliers for on-device header visual calibration (default 1). */
    val idleGlowMultiplier: Float = 1f,
    val headerAmbientMultiplier: Float = 1f,
    val printGlowMultiplier: Float = 1f,
    val debugForcePrintGlow: Boolean = false,
    val debugShowLogoGlowBounds: Boolean = false,
    /** Home card view mode: 0=Standard, 1=Minimal, 2=Detailed */
    val homeCardDensity: Int = 0,
    /** Per-printer card visibility overrides (printer ID → visibility). */
    val cardVisibility: Map<Int, PrinterCardVisibility> = emptyMap(),
    /** False until user has long-pressed a printer card at least once. */
    val hasUsedQuickActions: Boolean = false,
    /** Printer IDs with an in-flight home smart-outlet toggle. */
    val powerToggleInFlightIds: Set<Int> = emptySet(),
    /** One-shot outcome for home power toggle toast/snackbar. */
    val powerToggleOutcome: NfcActionOutcome? = null,
)

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
    private val homePrintersCacheRepository: HomePrintersCacheRepository,
    private val spoolsCacheRepository: SpoolsCacheRepository,
    private val printerCardPrefsRepository: PrinterCardPrefsRepository? = null,
) : ViewModel() {

    private var spoolCountJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var listFetchJob: Job? = null
    private var enrichJob: Job? = null
    private val manualRefreshGuard = RefreshGuard()
    private var pendingRefreshSource: RefreshSource? = null
    private var lastNetworkLoadServerKey: String? = null

    /**
     * Epoch-ms timestamp of the most recent [refreshOnAppResume] call, or null if the app
     * has not been resumed since this ViewModel was created. Used to extend the offline-flash
     * guard beyond first launch — see [enrichPrinters].
     */
    private var lastAppResumedAtMillis: Long? = null

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
        if (BuddyDashDebug.enabled) {
            viewModelScope.launch {
                combine(
                    settingsRepository.homeIdleGlowMultiplier,
                    settingsRepository.homeHeaderAmbientMultiplier,
                    settingsRepository.homePrintGlowMultiplier,
                    settingsRepository.homeDebugForcePrintGlow,
                    settingsRepository.homeDebugShowLogoGlowBounds,
                ) { idleGlow, headerAmbient, printGlow, forcePrintGlow, showGlowBounds ->
                    VisualTuningSnapshot(
                        idleGlow,
                        headerAmbient,
                        printGlow,
                        forcePrintGlow,
                        showGlowBounds,
                    )
                }.collect { tuning ->
                    _uiState.update {
                        it.copy(
                            idleGlowMultiplier = tuning.idleGlowMultiplier,
                            headerAmbientMultiplier = tuning.headerAmbientMultiplier,
                            printGlowMultiplier = tuning.printGlowMultiplier,
                            debugForcePrintGlow = tuning.debugForcePrintGlow,
                            debugShowLogoGlowBounds = tuning.debugShowLogoGlowBounds,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.homeCardDensity.collect { density ->
                _uiState.update { it.copy(homeCardDensity = density) }
            }
        }
        viewModelScope.launch {
            settingsRepository.hasUsedQuickActions.collect { used ->
                _uiState.update { it.copy(hasUsedQuickActions = used) }
            }
        }
    }

    private data class VisualTuningSnapshot(
        val idleGlowMultiplier: Float,
        val headerAmbientMultiplier: Float,
        val printGlowMultiplier: Float,
        val debugForcePrintGlow: Boolean,
        val debugShowLogoGlowBounds: Boolean,
    )

    private suspend fun hydrateFromDiskCache(serverUrl: String) {
        val snapshot = homePrintersCacheRepository.load(serverUrl)
        val newKey = HomePrintersCacheRepository.cacheServerKey(serverUrl)
        val currentKey = HomePrintersCacheRepository.cacheServerKey(_uiState.value.serverUrl)
        if (BuddyDashDebug.enabled) {
            Log.d(
                TAG_HOME_VM,
                "HomeVM: cache loaded=${snapshot != null} count=${snapshot?.printers?.size ?: 0} " +
                    "lastUpdatedAtMillis=${snapshot?.lastUpdatedAtMillis}",
            )
        }
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
                        hasAttemptedNetworkLoad = false,
                    )
                }
                else -> state
            }
        }
        refreshCardVisibility()
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
        lastAppResumedAtMillis = System.currentTimeMillis()
        if (BuddyDashDebug.enabled) {
            Log.d(TAG_HOME_VM, "HomeVM: app resumed route=$currentRoute")
        }
        requestRefresh(RefreshSource.APP_RESUME, force = true, currentRoute = currentRoute)
    }

    fun refreshManual() {
        requestRefresh(RefreshSource.MANUAL, force = true)
    }

    fun clearPrinterHmsErrors(printerId: Int, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.hasCredentials) {
                onComplete(Result.failure(IllegalStateException("Not configured")))
                return@launch
            }
            apiClient.clearHmsErrors(state.serverUrl, state.apiKey, printerId).fold(
                onSuccess = {
                    refreshSinglePrinter(printerId)
                    onComplete(Result.success(Unit))
                },
                onFailure = { error -> onComplete(Result.failure(error)) },
            )
        }
    }

    private suspend fun refreshSinglePrinter(printerId: Int) {
        val state = _uiState.value
        val base = state.printers.find { it.id == printerId } ?: return
        apiClient.enrichPrintersForHome(state.serverUrl, state.apiKey, listOf(base)).fold(
            onSuccess = { enriched ->
                val updatedPrinter = enriched.firstOrNull() ?: return
                val updatedAt = System.currentTimeMillis()
                val newPrinters = state.printers.map { printer ->
                    if (printer.id == printerId) updatedPrinter else printer
                }
                homePrintersCacheRepository.save(state.serverUrl, newPrinters, updatedAt)
                _uiState.update { current ->
                    current.copy(
                        printers = newPrinters,
                        lastUpdatedAtMillis = updatedAt,
                    )
                }
            },
            onFailure = { /* server clear succeeded; next poll will reconcile */ },
        )
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
                "HomeVM: refresh started source=$refreshSource showLoading=$showLoading " +
                    "fromPull=$fromPull fromUser=$fromUser immediateRefresh=$immediateRefresh " +
                    "hadPrinters=$hadPrinters",
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
                                    hasAttemptedNetworkLoad = true,
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
                                    hasAttemptedNetworkLoad = true,
                                )
                            } else {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isEnriching = false,
                                    error = message,
                                    refreshError = null,
                                    hasCompletedLoad = true,
                                    hasAttemptedNetworkLoad = true,
                                )
                            }
                        }
                        flushPendingRefresh()
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (BuddyDashDebug.enabled) {
                    Log.w(TAG_HOME_VM, "HomeVM: loadPrinters failed unexpectedly", e)
                }
                val message = e.toUserNetworkMessage(
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
                            hasAttemptedNetworkLoad = true,
                        )
                    } else {
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isEnriching = false,
                            error = message,
                            refreshError = null,
                            hasCompletedLoad = true,
                            hasAttemptedNetworkLoad = true,
                        )
                    }
                }
                flushPendingRefresh()
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
                    _uiState.update { current ->
                        // Offline-flash guard: preserve the last-known online state for any
                        // printer where the API transiently reports connected=false right after
                        // the device wakes up (typically 1–3 s at server startup).
                        //
                        // The guard activates in two situations:
                        //   1. First network load of the session (!hasAttemptedNetworkLoad) —
                        //      the app just launched and the cached state is the best truth.
                        //   2. First enrich within RESUME_OFFLINE_FLASH_GUARD_MS after an app
                        //      resume event (fold/unfold, background→foreground) — the API
                        //      connection may not be fully re-established yet.
                        //
                        // Once the guard window expires, fresh connected=false always wins so
                        // real confirmed-offline state still appears on the next poll (~15 s).
                        val resumedAt = lastAppResumedAtMillis
                        val msSinceResume = if (resumedAt != null) updatedAt - resumedAt else Long.MAX_VALUE
                        val isWithinResumeGuardWindow = msSinceResume in 0..RESUME_OFFLINE_FLASH_GUARD_MS
                        val useFlashGuard = !current.hasAttemptedNetworkLoad || isWithinResumeGuardWindow

                        if (BuddyDashDebug.enabled) {
                            val reason = when {
                                !current.hasAttemptedNetworkLoad -> "firstLoad"
                                isWithinResumeGuardWindow -> "resumeWindow(${msSinceResume}ms)"
                                else -> "none"
                            }
                            Log.d(
                                TAG_HOME_VM,
                                "HomeVM: refresh success flashGuard=$useFlashGuard reason=$reason " +
                                    "enrichedCount=${enriched.size}",
                            )
                        }

                        val cachedById = if (useFlashGuard) {
                            current.printers.associateBy { it.id }
                        } else {
                            emptyMap()
                        }

                        val finalPrinters = enriched.map { printer ->
                            val fresh = printer.liveStatus
                            if (useFlashGuard) {
                                val cached = cachedById[printer.id]?.liveStatus
                                if (cached?.connected == true && fresh?.connected != true) {
                                    if (BuddyDashDebug.enabled) {
                                        Log.d(
                                            TAG_HOME_VM,
                                            "HomeVM: offline suppressed printerId=${printer.id} " +
                                                "(cached=online fresh=offline guardActive)",
                                        )
                                    }
                                    printer.copy(liveStatus = cached)
                                } else {
                                    if (BuddyDashDebug.enabled && fresh?.connected == false) {
                                        Log.d(
                                            TAG_HOME_VM,
                                            "HomeVM: offline CONFIRMED printerId=${printer.id} " +
                                                "(cached was not online; showing offline)",
                                        )
                                    }
                                    printer
                                }
                            } else {
                                if (BuddyDashDebug.enabled && fresh?.connected == false) {
                                    Log.d(
                                        TAG_HOME_VM,
                                        "HomeVM: offline CONFIRMED printerId=${printer.id} " +
                                            "(guard not active; showing offline)",
                                    )
                                }
                                printer
                            }
                        }

                        current.copy(
                            printers = finalPrinters,
                            isEnriching = false,
                            isRefreshing = false,
                            lastUpdatedAtMillis = updatedAt,
                            error = null,
                            refreshError = null,
                            isStaleCachedData = false,
                            hasAttemptedNetworkLoad = true,
                        )
                    }
                    HomeLoadTiming.log("secondary data applied to printer cards")
                    refreshLoadedSpoolCount()
                    refreshCardVisibility()
                    flushPendingRefresh()
                },
                onFailure = { error ->
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_HOME_VM, "HomeVM: refresh failed source=$refreshSource error=${error.message}")
                    }
                    refreshLoadedSpoolCount()
                    _uiState.update { current ->
                        val message = error.toUserNetworkMessage("Could not refresh printers")
                        val fallbackPrinters = current.printers.withEnrichFallbacks()
                        if (fallbackPrinters.isNotEmpty()) {
                            current.copy(
                                printers = fallbackPrinters,
                                isEnriching = false,
                                isRefreshing = false,
                                error = null,
                                refreshError = message,
                                isStaleCachedData = true,
                                hasCompletedLoad = true,
                                hasAttemptedNetworkLoad = true,
                            )
                        } else {
                            current.copy(
                                isEnriching = false,
                                isRefreshing = false,
                                refreshError = message,
                                hasAttemptedNetworkLoad = true,
                            )
                        }
                    }
                    flushPendingRefresh()
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuddyDashDebug.enabled) {
                Log.w(TAG_HOME_VM, "HomeVM: enrichPrinters failed unexpectedly source=$refreshSource", e)
            }
            refreshLoadedSpoolCount()
            _uiState.update { current ->
                val fallbackPrinters = current.printers.withEnrichFallbacks()
                if (fallbackPrinters.isNotEmpty()) {
                    current.copy(
                        printers = fallbackPrinters,
                        isEnriching = false,
                        isRefreshing = false,
                        isStaleCachedData = true,
                        hasCompletedLoad = true,
                        hasAttemptedNetworkLoad = true,
                    )
                } else {
                    current.copy(
                        isEnriching = false,
                        isRefreshing = false,
                        hasAttemptedNetworkLoad = true,
                    )
                }
            }
            flushPendingRefresh()
        }
    }

    fun markQuickActionsUsed() {
        if (_uiState.value.hasUsedQuickActions) return
        viewModelScope.launch { settingsRepository.saveHasUsedQuickActions() }
    }

    fun toggleSmartPlugPower(printerId: Int) {
        val state = _uiState.value
        if (!state.hasCredentials || printerId in state.powerToggleInFlightIds) return
        val printer = state.printers.find { it.id == printerId } ?: return
        if (printer.smartPlugState == null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(powerToggleInFlightIds = it.powerToggleInFlightIds + printerId)
            }
            val result = toggleSmartPlugPower(
                apiClient = apiClient,
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                printer = printer,
            )
            _uiState.update { current ->
                val updatedPrinters = current.printers.map { p ->
                    if (p.id != printerId) p
                    else p.copy(
                        smartPlugState = result.updatedPlugState ?: p.smartPlugState,
                        liveStatus = result.updatedLiveStatus ?: p.liveStatus,
                    )
                }
                current.copy(
                    powerToggleInFlightIds = current.powerToggleInFlightIds - printerId,
                    powerToggleOutcome = result.outcome,
                    printers = updatedPrinters,
                )
            }
            if (result.outcome.tier == NfcActionOutcome.Tier.Success) {
                homePrintersCacheRepository.save(
                    serverUrl = state.serverUrl,
                    printers = _uiState.value.printers,
                    lastUpdatedAtMillis = System.currentTimeMillis(),
                )
            }
        }
    }

    fun consumePowerToggleOutcome(): NfcActionOutcome? {
        val outcome = _uiState.value.powerToggleOutcome ?: return null
        _uiState.update { it.copy(powerToggleOutcome = null) }
        return outcome
    }

    private fun refreshCardVisibility() {
        val repo = printerCardPrefsRepository ?: return
        val printerIds = _uiState.value.printers.map { it.id }
        if (printerIds.isEmpty()) return
        viewModelScope.launch {
            val map = mutableMapOf<Int, PrinterCardVisibility>()
            for (id in printerIds) {
                map[id] = repo.loadVisibility(id)
            }
            _uiState.update { it.copy(cardVisibility = map) }
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
