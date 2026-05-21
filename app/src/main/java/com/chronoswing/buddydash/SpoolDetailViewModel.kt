package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.SpoolArchiveMatches
import com.chronoswing.buddydash.util.matchArchivesForSpool
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
    val archiveMatches: SpoolArchiveMatches = SpoolArchiveMatches(
        isExactSpoolId = false,
        archives = emptyList(),
    ),
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
                val archivesDeferred = async {
                    apiClient.fetchArchives(state.serverUrl, state.apiKey)
                }
                spoolsDeferred.await() to archivesDeferred.await()
            }
            val spoolsResult = result.first
            val archivesResult = result.second
            spoolsResult.fold(
                onSuccess = { spools ->
                    val spool = spools.find { it.id == spoolId }
                    if (spool == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                spool = null,
                                error = "Spool not found",
                            )
                        }
                        return@fold
                    }
                    val archives = archivesResult.getOrElse { emptyList() }
                    val matches = matchArchivesForSpool(spool, archives)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            spool = spool,
                            archiveMatches = matches,
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
