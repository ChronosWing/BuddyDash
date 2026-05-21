package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.PrinterStatus
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

data class PrinterDetailUiState(
    val printerName: String = "",
    val status: PrinterStatus? = null,
    val maintenanceItems: List<MaintenanceItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
    val isClearingPlate: Boolean = false,
    val isControlBusy: Boolean = false,
    val plateClearSnackbar: PlateClearSnackbar? = null,
    val controlSnackbar: ControlSnackbar? = null,
)

enum class PlateClearSnackbar {
    Success,
    Failed,
}

enum class ControlSnackbar {
    Success,
    Failed,
}

class PrinterDetailViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private var printerId: Int = -1

    private val _uiState = MutableStateFlow(PrinterDetailUiState())
    val uiState: StateFlow<PrinterDetailUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.serverUrl,
                settingsRepository.apiKey,
                settingsRepository.cameraToken,
            ) { url, key, cameraToken -> Triple(url, key, cameraToken) }
                .collect { (url, key, cameraToken) ->
                    _uiState.update {
                        it.copy(
                            serverUrl = url,
                            apiKey = key,
                            cameraToken = cameraToken,
                            hasCredentials = url.isNotBlank() && key.isNotBlank(),
                        )
                    }
                    maybeLoadStatus()
                }
        }
    }

    fun init(printerId: Int, printerName: String) {
        this.printerId = printerId
        _uiState.update {
            it.copy(printerName = printerName, error = null)
        }
    }

    private fun maybeLoadStatus() {
        val state = _uiState.value
        if (printerId < 0 || !state.hasCredentials) return
        loadStatus()
    }

    fun loadStatus(showLoading: Boolean = true, fromPull: Boolean = false) {
        if (printerId < 0) return
        val state = _uiState.value
        if (!state.hasCredentials) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "Configure server URL and API key in Settings",
                )
            }
            return
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (fromPull) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else if (showLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val statusResult = coroutineScope {
                val statusDeferred = async {
                    apiClient.fetchPrinterStatus(state.serverUrl, state.apiKey, printerId)
                }
                val maintenanceDeferred = async {
                    apiClient.fetchMaintenance(state.serverUrl, state.apiKey, printerId)
                        .getOrNull()
                        ?.items
                        .orEmpty()
                }
                statusDeferred.await() to maintenanceDeferred.await()
            }

            statusResult.first.fold(
                onSuccess = { status ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            status = status,
                            maintenanceItems = statusResult.second,
                            error = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            status = null,
                            maintenanceItems = emptyList(),
                            error = error.message ?: "Failed to load printer status",
                        )
                    }
                },
            )
        }
    }

    fun markPlateClear() {
        if (printerId < 0) return
        val state = _uiState.value
        if (!state.hasCredentials) return

        viewModelScope.launch {
            _uiState.update { it.copy(isClearingPlate = true, error = null) }
            apiClient.clearPlate(state.serverUrl, state.apiKey, printerId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isClearingPlate = false,
                            plateClearSnackbar = PlateClearSnackbar.Success,
                        )
                    }
                    loadStatus(showLoading = false)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isClearingPlate = false,
                            plateClearSnackbar = PlateClearSnackbar.Failed,
                        )
                    }
                },
            )
        }
    }

    fun setPrintSpeed(mode: Int) = runControl {
        apiClient.setPrintSpeed(it.serverUrl, it.apiKey, printerId, mode)
    }

    fun pausePrint() = runControl {
        apiClient.pausePrint(it.serverUrl, it.apiKey, printerId)
    }

    fun resumePrint() = runControl {
        apiClient.resumePrint(it.serverUrl, it.apiKey, printerId)
    }

    fun stopPrint() = runControl {
        apiClient.stopPrint(it.serverUrl, it.apiKey, printerId)
    }

    fun toggleChamberLight() {
        val state = _uiState.value
        val current = state.status?.chamberLightOn ?: return
        runControl {
            apiClient.setChamberLight(it.serverUrl, it.apiKey, printerId, on = !current)
        }
    }

    private fun runControl(block: suspend (PrinterDetailUiState) -> Result<Unit>) {
        if (printerId < 0) return
        val state = _uiState.value
        if (!state.hasCredentials || state.isControlBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isControlBusy = true) }
            block(state).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isControlBusy = false,
                            controlSnackbar = ControlSnackbar.Success,
                        )
                    }
                    loadStatus(showLoading = false)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isControlBusy = false,
                            controlSnackbar = ControlSnackbar.Failed,
                        )
                    }
                },
            )
        }
    }

    fun onPlateClearSnackbarShown() {
        _uiState.update { it.copy(plateClearSnackbar = null) }
    }

    fun onControlSnackbarShown() {
        _uiState.update { it.copy(controlSnackbar = null) }
    }
}
