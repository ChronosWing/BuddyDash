package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val statusMessage: String? = null,
    val isSuccess: Boolean? = null,
    val isLoading: Boolean = false,
    val saved: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.serverUrl.collect { url ->
                _uiState.update { it.copy(serverUrl = url) }
            }
        }
        viewModelScope.launch {
            settingsRepository.apiKey.collect { key ->
                _uiState.update { it.copy(apiKey = key) }
            }
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, saved = false) }
    }

    fun onApiKeyChange(key: String) {
        _uiState.update { it.copy(apiKey = key, saved = false) }
    }

    fun saveSettings() {
        val state = _uiState.value
        viewModelScope.launch {
            settingsRepository.saveServerUrl(state.serverUrl)
            settingsRepository.saveApiKey(state.apiKey)
            _uiState.update {
                it.copy(saved = true, statusMessage = null, isSuccess = null)
            }
        }
    }

    fun testConnection() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, statusMessage = null, isSuccess = null)
            }
            val result = apiClient.testApiConnection(state.serverUrl, state.apiKey)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = result.isSuccess,
                    statusMessage = result.fold(
                        onSuccess = { message -> message },
                        onFailure = { error -> error.message ?: "Connection failed" },
                    ),
                )
            }
        }
    }
}
