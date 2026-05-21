package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import com.chronoswing.buddydash.network.BambuddyApiClient
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
    val printerNamesById: Map<Int, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
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
                Triple(
                    spoolsDeferred.await(),
                    usageDeferred.await(),
                    printersDeferred.await(),
                )
            }
            val spoolsResult = result.first
            val usageResult = result.second
            val printersResult = result.third
            spoolsResult.fold(
                onSuccess = { spools ->
                    val spool = spools.find { it.id == spoolId }
                    if (spool == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                spool = null,
                                usageHistory = emptyList(),
                                error = "Spool not found",
                            )
                        }
                        return@fold
                    }
                    val usageHistory = usageResult.getOrElse { emptyList() }
                    val printerNamesById = printersResult.getOrElse { emptyList() }
                        .associate { it.id to it.name }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            spool = spool,
                            usageHistory = usageHistory,
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
