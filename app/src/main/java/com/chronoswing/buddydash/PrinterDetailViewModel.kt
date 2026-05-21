package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrinterDetailUiState(
    val printerName: String = "",
    val status: PrinterStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
)

class PrinterDetailViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private var printerId: Int = -1

    private val _uiState = MutableStateFlow(PrinterDetailUiState())
    val uiState: StateFlow<PrinterDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.serverUrl,
                settingsRepository.apiKey,
            ) { url, key -> url to key }
                .collect { (url, key) ->
                    _uiState.update { it.copy(serverUrl = url, apiKey = key) }
                }
        }
    }

    fun init(printerId: Int, printerName: String) {
        this.printerId = printerId
        _uiState.update { it.copy(printerName = printerName) }
        loadStatus()
    }

    fun loadStatus() {
        if (printerId < 0) return
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.apiKey.isBlank()) {
            _uiState.update { it.copy(error = "Configure server URL and API key in Settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            apiClient.fetchPrinterStatus(state.serverUrl, state.apiKey, printerId).fold(
                onSuccess = { status ->
                    _uiState.update {
                        it.copy(isLoading = false, status = status, error = null)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = null,
                            error = error.message ?: "Failed to load printer status",
                        )
                    }
                },
            )
        }
    }
}
