package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.SpoolUsageDisplayItem
import com.chronoswing.buddydash.util.SPOOL_DETAIL_ARCHIVES_LIMIT
import com.chronoswing.buddydash.util.buildSpoolUsageDisplayItems
import com.chronoswing.buddydash.util.logSpoolUsageArchiveDiscovery
import com.chronoswing.buddydash.util.logSpoolUsageDisplayItems
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpoolDetailUiState(
    val spool: SpoolInventoryItem? = null,
    val usageHistory: List<SpoolUsageEntry> = emptyList(),
    val usageDisplayItems: List<SpoolUsageDisplayItem> = emptyList(),
    val printerNamesById: Map<Int, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
)

class SpoolDetailViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private var spoolId: Int = -1
    private var fetchJob: Job? = null

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
                maybeLoad()
            }
        }
    }

    fun init(spoolId: Int) {
        this.spoolId = spoolId
        maybeLoad(force = true)
    }

    fun load(force: Boolean = false) {
        maybeLoad(force = force)
    }

    private fun maybeLoad(force: Boolean = false) {
        if (spoolId < 0) return
        val state = _uiState.value
        if (!state.hasCredentials) return
        if (!force && state.spool != null) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.spool == null, error = null) }
            val result = coroutineScope {
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
                            )
                        }
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            spool = spool,
                            usageHistory = usageHistory,
                            usageDisplayItems = usageDisplayItems,
                            printerNamesById = printerNamesById,
                            error = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load spool",
                        )
                    }
                },
            )
        }
    }
}

private data class SpoolDetailFetchBundle(
    val spools: Result<List<SpoolInventoryItem>>,
    val usageHistory: List<SpoolUsageEntry>,
    val printers: Result<List<Printer>>,
    val archives: List<PrintArchive>,
)
