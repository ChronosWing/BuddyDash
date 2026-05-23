package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.SpoolDetailCacheRepository
import com.chronoswing.buddydash.data.SpoolDetailCacheSnapshot
import com.chronoswing.buddydash.data.SpoolsCacheRepository
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.OfflineUiResolution
import com.chronoswing.buddydash.util.SPOOL_DETAIL_ARCHIVES_LIMIT
import com.chronoswing.buddydash.util.SpoolDetailCacheSource
import com.chronoswing.buddydash.util.SpoolUsageDisplayItem
import com.chronoswing.buddydash.util.buildSpoolUsageDisplayItems
import com.chronoswing.buddydash.util.logOfflineLoadState
import com.chronoswing.buddydash.util.logSpoolDetailCacheRead
import com.chronoswing.buddydash.util.logSpoolDetailCacheWrite
import com.chronoswing.buddydash.util.logSpoolUsageArchiveDiscovery
import com.chronoswing.buddydash.util.logSpoolUsageDisplayItems
import com.chronoswing.buddydash.util.toUserNetworkMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val SPOOL_DETAIL_FETCH_TIMEOUT_MS = 30_000L
private const val OFFLINE_NO_CACHE_MARKER = "offline_no_cache"

data class SpoolDetailUiState(
    val spool: SpoolInventoryItem? = null,
    val usageHistory: List<SpoolUsageEntry> = emptyList(),
    val usageDisplayItems: List<SpoolUsageDisplayItem> = emptyList(),
    val printerNamesById: Map<Int, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isStaleCachedData: Boolean = false,
    val isLimitedFromListCache: Boolean = false,
    val hasCompletedLoad: Boolean = false,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
    /** True after the first network load attempt completes (success or failure). Gates stale/error UI. */
    val hasAttemptedNetworkLoad: Boolean = false,
)

class SpoolDetailViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
    private val spoolsCacheRepository: SpoolsCacheRepository,
    private val spoolDetailCacheRepository: SpoolDetailCacheRepository,
) : ViewModel() {

    private var spoolId: Int = -1
    private var fetchJob: Job? = null
    private var initJob: Job? = null

    private val _uiState = MutableStateFlow(SpoolDetailUiState())
    val uiState: StateFlow<SpoolDetailUiState> = _uiState.asStateFlow()

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

    fun init(spoolId: Int) {
        this.spoolId = spoolId
        initJob?.cancel()
        fetchJob?.cancel()
        initJob = viewModelScope.launch {
            syncCredentialsFromSettings()
            val source = resolveCachedSpool()
            val hasCache = source != SpoolDetailCacheSource.None
            val state = _uiState.value
            _uiState.update {
                it.copy(
                    hasCompletedLoad = true,
                    isLoading = false,
                    error = if (!hasCache && !state.hasCredentials) {
                        OFFLINE_NO_CACHE_MARKER
                    } else {
                        null
                    },
                )
            }
            logSpoolDetailCacheRead(spoolId, source)
            logOfflineLoadState(
                screen = "SpoolDetail",
                onlineAttempt = state.hasCredentials,
                cacheResult = source.name,
                finalState = when {
                    !hasCache -> OfflineUiResolution.NoCacheOffline
                    source == SpoolDetailCacheSource.ListFallback ->
                        OfflineUiResolution.LimitedFromCache
                    else -> OfflineUiResolution.StaleWithCache
                },
            )
            if (state.hasCredentials) {
                loadFromNetwork(isRefresh = hasCache)
            }
        }
    }

    private suspend fun syncCredentialsFromSettings() {
        val url = settingsRepository.serverUrl.first()
        val key = settingsRepository.apiKey.first()
        val cameraToken = settingsRepository.cameraToken.first()
        _uiState.update {
            it.copy(
                serverUrl = url,
                apiKey = key,
                cameraToken = cameraToken,
                hasCredentials = url.isNotBlank() && key.isNotBlank(),
            )
        }
    }

    private suspend fun resolveCachedSpool(): SpoolDetailCacheSource {
        if (spoolId < 0) return SpoolDetailCacheSource.None
        val serverUrl = _uiState.value.serverUrl
        spoolDetailCacheRepository.load(serverUrl, spoolId)?.let { snapshot ->
            applyDetailSnapshot(snapshot, stale = true)
            return SpoolDetailCacheSource.FullDetail
        }
        spoolsCacheRepository.findSpool(serverUrl, spoolId)?.let { spool ->
            _uiState.update {
                it.copy(
                    spool = spool,
                    usageHistory = emptyList(),
                    usageDisplayItems = emptyList(),
                    printerNamesById = emptyMap(),
                    isStaleCachedData = true,
                    isLimitedFromListCache = true,
                )
            }
            return SpoolDetailCacheSource.ListFallback
        }
        return SpoolDetailCacheSource.None
    }

    private fun applyDetailSnapshot(snapshot: SpoolDetailCacheSnapshot, stale: Boolean) {
        val usageDisplayItems = buildSpoolUsageDisplayItems(
            entries = snapshot.usageHistory,
            archives = emptyList(),
            printerNamesById = snapshot.printerNamesById,
            spoolMaterial = snapshot.spool.material,
            spoolColorName = snapshot.spool.colorName,
        )
        _uiState.update {
            it.copy(
                spool = snapshot.spool,
                usageHistory = snapshot.usageHistory,
                usageDisplayItems = usageDisplayItems,
                printerNamesById = snapshot.printerNamesById,
                isStaleCachedData = stale,
                isLimitedFromListCache = false,
            )
        }
    }

    fun load(force: Boolean = false) {
        if (spoolId < 0) return
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (!_uiState.value.hasCompletedLoad) {
                resolveCachedSpool()
                _uiState.update { it.copy(hasCompletedLoad = true, isLoading = false) }
            }
            if (force || _uiState.value.hasCredentials) {
                loadFromNetwork(isRefresh = _uiState.value.spool != null)
            }
        }
    }

    private suspend fun loadFromNetwork(isRefresh: Boolean) {
        val state = _uiState.value
        if (!state.hasCredentials) return
        if (!isRefresh) {
            _uiState.update { it.copy(isLoading = it.spool == null, error = null) }
        }
        val result = withTimeoutOrNull(SPOOL_DETAIL_FETCH_TIMEOUT_MS) {
            coroutineScope {
                val spoolsDeferred = async {
                    apiClient.fetchSpoolInventory(state.serverUrl, state.apiKey)
                }
                val usageDeferred = async {
                    apiClient.fetchSpoolUsageHistory(state.serverUrl, state.apiKey, spoolId)
                }
                val printersDeferred = async {
                    apiClient.fetchPrinters(state.serverUrl, state.apiKey)
                }
                val usageHistory = usageDeferred.await().getOrElse { emptyList() }
                val archivesDeferred = if (usageHistory.isNotEmpty()) {
                    async {
                        apiClient.fetchArchives(
                            state.serverUrl,
                            state.apiKey,
                            limit = SPOOL_DETAIL_ARCHIVES_LIMIT,
                        )
                    }
                } else {
                    null
                }
                SpoolDetailFetchBundle(
                    spools = spoolsDeferred.await(),
                    usageHistory = usageHistory,
                    printers = printersDeferred.await(),
                    archives = archivesDeferred?.await()?.getOrElse { emptyList() } ?: emptyList(),
                )
            }
        }
        if (result == null) {
            finishNetworkFailure(
                error = Exception("Request timed out"),
                hadCache = state.spool != null,
            )
            return
        }
        val spoolsResult = result.spools
        val usageHistory = result.usageHistory
        val printersResult = result.printers
        val archives = result.archives
        spoolsResult.fold(
            onSuccess = { spools ->
                val spool = spools.find { it.id == spoolId }
                if (spool == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            spool = null,
                            usageHistory = emptyList(),
                            usageDisplayItems = emptyList(),
                            error = "Spool not found",
                            isStaleCachedData = false,
                            isLimitedFromListCache = false,
                        )
                    }
                    logOfflineLoadState(
                        screen = "SpoolDetail",
                        onlineAttempt = true,
                        cacheResult = "network_not_found",
                        finalState = OfflineUiResolution.LoadedFresh,
                    )
                    return@fold
                }
                val printerNamesById = printersResult.getOrElse { emptyList() }
                    .associate { it.id to it.name }
                logSpoolUsageArchiveDiscovery(
                    spoolId = spoolId,
                    usageEntries = usageHistory,
                    archivesRawJson = null,
                )
                val usageDisplayItems = buildSpoolUsageDisplayItems(
                    entries = usageHistory,
                    archives = archives,
                    printerNamesById = printerNamesById,
                    spoolMaterial = spool.material,
                    spoolColorName = spool.colorName,
                )
                logSpoolUsageDisplayItems(spoolId, usageDisplayItems)
                val updatedAt = System.currentTimeMillis()
                val writeOk = spoolDetailCacheRepository.save(
                    SpoolDetailCacheSnapshot(
                        serverUrl = state.serverUrl,
                        spoolId = spoolId,
                        spool = spool,
                        usageHistory = usageHistory,
                        printerNamesById = printerNamesById,
                        lastUpdatedAtMillis = updatedAt,
                    ),
                )
                logSpoolDetailCacheWrite(spoolId, writeOk)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        spool = spool,
                        usageHistory = usageHistory,
                        usageDisplayItems = usageDisplayItems,
                        printerNamesById = printerNamesById,
                        error = null,
                        isStaleCachedData = false,
                        isLimitedFromListCache = false,
                        hasCompletedLoad = true,
                        hasAttemptedNetworkLoad = true,
                    )
                }
                logOfflineLoadState(
                    screen = "SpoolDetail",
                    onlineAttempt = true,
                    cacheResult = "network_ok",
                    finalState = OfflineUiResolution.LoadedFresh,
                )
            },
            onFailure = { error ->
                finishNetworkFailure(error, hadCache = state.spool != null)
            },
        )
    }

    private suspend fun finishNetworkFailure(error: Throwable, hadCache: Boolean) {
        if (!hadCache) {
            when (resolveCachedSpool()) {
                SpoolDetailCacheSource.None -> Unit
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            hasCompletedLoad = true,
                            hasAttemptedNetworkLoad = true,
                        )
                    }
                    logOfflineLoadState(
                        screen = "SpoolDetail",
                        onlineAttempt = true,
                        cacheResult = "failure_with_cache",
                        finalState = if (_uiState.value.isLimitedFromListCache) {
                            OfflineUiResolution.LimitedFromCache
                        } else {
                            OfflineUiResolution.StaleWithCache
                        },
                    )
                    return
                }
            }
        }
        _uiState.update {
            if (hadCache) {
                it.copy(
                    isLoading = false,
                    error = null,
                    isStaleCachedData = true,
                    hasCompletedLoad = true,
                    hasAttemptedNetworkLoad = true,
                )
            } else {
                it.copy(
                    isLoading = false,
                    error = OFFLINE_NO_CACHE_MARKER,
                    hasCompletedLoad = true,
                    hasAttemptedNetworkLoad = true,
                )
            }
        }
        logOfflineLoadState(
            screen = "SpoolDetail",
            onlineAttempt = true,
            cacheResult = if (hadCache) "failure_stale" else "failure_no_cache",
            finalState = if (hadCache) {
                OfflineUiResolution.StaleWithCache
            } else {
                OfflineUiResolution.NoCacheOffline
            },
        )
    }
}

private data class SpoolDetailFetchBundle(
    val spools: Result<List<SpoolInventoryItem>>,
    val usageHistory: List<SpoolUsageEntry>,
    val printers: Result<List<Printer>>,
    val archives: List<PrintArchive>,
)
