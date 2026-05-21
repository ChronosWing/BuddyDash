package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.network.BambuddyApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val printers: List<Printer> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
    /** Epoch millis of last successful printers fetch (for passive refresh indicator). */
    val lastUpdatedAtMillis: Long? = null,
)

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

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

    fun loadPrinters(showLoading: Boolean = false, fromPull: Boolean = false) {
        val state = _uiState.value
        if (!state.hasCredentials) {
            _uiState.update { it.copy(error = "Configure server URL and API key in Settings") }
            return
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (fromPull) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else if (showLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            val result = apiClient.fetchPrintersWithStatus(state.serverUrl, state.apiKey)
            result.fold(
                onSuccess = { printers ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            printers = printers,
                            error = null,
                            lastUpdatedAtMillis = System.currentTimeMillis(),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            printers = emptyList(),
                            error = error.message ?: "Failed to load printers",
                        )
                    }
                },
            )
        }
    }
}
