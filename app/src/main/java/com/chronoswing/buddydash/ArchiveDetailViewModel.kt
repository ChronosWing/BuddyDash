package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.network.BambuddyApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArchiveDetailUiState(
    val archive: PrintArchive? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
)

class ArchiveDetailViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private var archiveId: Int = -1
    private var fetchJob: Job? = null

    private val _uiState = MutableStateFlow(ArchiveDetailUiState())
    val uiState: StateFlow<ArchiveDetailUiState> = _uiState.asStateFlow()

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

    fun init(archiveId: Int) {
        this.archiveId = archiveId
        loadArchive()
    }

    fun loadArchive() {
        if (archiveId < 0) return
        val state = _uiState.value
        if (!state.hasCredentials) {
            _uiState.update { it.copy(error = "Configure server URL and API key in Settings") }
            return
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.archive == null, error = null) }
            val result = apiClient.fetchArchive(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                archiveId = archiveId,
            )
            result.fold(
                onSuccess = { archive ->
                    _uiState.update {
                        it.copy(isLoading = false, archive = archive, error = null)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load archive",
                        )
                    }
                },
            )
        }
    }
}
