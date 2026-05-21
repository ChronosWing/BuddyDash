package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.ArchiveReprintPrinter
import com.chronoswing.buddydash.util.defaultArchiveReprintPrinterId
import com.chronoswing.buddydash.util.defaultArchiveReprintQuantity
import com.chronoswing.buddydash.util.resolveArchiveReprintPrinters
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ArchiveReprintSnackbar {
    Success,
    Failed,
}

data class ArchiveReprintSheetState(
    val isOpen: Boolean = false,
    val isLoadingPrinters: Boolean = false,
    val isSubmitting: Boolean = false,
    val compatiblePrinters: List<ArchiveReprintPrinter> = emptyList(),
    val hiddenIncompatibleCount: Int = 0,
    val selectedPrinterId: Int? = null,
    val quantity: Int = 1,
    val canSubmit: Boolean = false,
)

data class ArchiveDetailUiState(
    val archive: PrintArchive? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
    val reprintSheet: ArchiveReprintSheetState = ArchiveReprintSheetState(),
    val reprintSnackbar: ArchiveReprintSnackbar? = null,
    val queuedPrinterId: Int? = null,
    val queuedPrinterName: String? = null,
    val queuedPrinterModel: String? = null,
)

class ArchiveDetailViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private var archiveId: Int = -1
    private var fetchJob: Job? = null
    private var printersJob: Job? = null
    private var queueJob: Job? = null

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

    fun onQueueAgainClick() {
        val archive = _uiState.value.archive ?: return
        _uiState.update {
            it.copy(
                reprintSheet = ArchiveReprintSheetState(
                    isOpen = true,
                    isLoadingPrinters = true,
                    quantity = defaultArchiveReprintQuantity(archive),
                ),
            )
        }
        loadReprintPrinters()
    }

    fun onDismissReprintSheet() {
        if (_uiState.value.reprintSheet.isSubmitting) return
        _uiState.update {
            it.copy(reprintSheet = ArchiveReprintSheetState())
        }
    }

    fun onReprintPrinterSelected(printerId: Int) {
        _uiState.update { state ->
            state.copy(
                reprintSheet = state.reprintSheet.copy(
                    selectedPrinterId = printerId,
                    canSubmit = state.reprintSheet.compatiblePrinters.isNotEmpty(),
                ),
            )
        }
    }

    fun onReprintQuantityChange(delta: Int) {
        _uiState.update { state ->
            val next = (state.reprintSheet.quantity + delta).coerceIn(1, 99)
            state.copy(reprintSheet = state.reprintSheet.copy(quantity = next))
        }
    }

    fun onConfirmQueuePrint() {
        val state = _uiState.value
        val archive = state.archive ?: return
        val printerId = state.reprintSheet.selectedPrinterId ?: return
        if (!state.hasCredentials || state.reprintSheet.isSubmitting) return

        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            _uiState.update {
                it.copy(reprintSheet = it.reprintSheet.copy(isSubmitting = true))
            }
            val printer = state.reprintSheet.compatiblePrinters.find { it.id == printerId }
            val result = apiClient.addArchiveToQueue(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                archiveId = archive.id,
                printerId = printerId,
                quantity = state.reprintSheet.quantity,
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            reprintSheet = ArchiveReprintSheetState(),
                            reprintSnackbar = ArchiveReprintSnackbar.Success,
                            queuedPrinterId = printerId,
                            queuedPrinterName = printer?.name,
                            queuedPrinterModel = printer?.model,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            reprintSheet = it.reprintSheet.copy(isSubmitting = false),
                            reprintSnackbar = ArchiveReprintSnackbar.Failed,
                        )
                    }
                },
            )
        }
    }

    fun onReprintSnackbarShown() {
        _uiState.update { it.copy(reprintSnackbar = null) }
    }

    private fun loadReprintPrinters() {
        val state = _uiState.value
        val archive = state.archive ?: return
        if (!state.hasCredentials) return

        printersJob?.cancel()
        printersJob = viewModelScope.launch {
            val result = apiClient.fetchPrinters(state.serverUrl, state.apiKey)
            result.fold(
                onSuccess = { printers ->
                    val options = resolveArchiveReprintPrinters(printers, archive)
                    val selectedId = defaultArchiveReprintPrinterId(archive, options.compatible)
                    _uiState.update {
                        it.copy(
                            reprintSheet = it.reprintSheet.copy(
                                isLoadingPrinters = false,
                                compatiblePrinters = options.compatible,
                                hiddenIncompatibleCount = options.hiddenIncompatibleCount,
                                selectedPrinterId = selectedId,
                                canSubmit = selectedId != null,
                            ),
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            reprintSheet = it.reprintSheet.copy(
                                isLoadingPrinters = false,
                                compatiblePrinters = emptyList(),
                                canSubmit = false,
                            ),
                            reprintSnackbar = ArchiveReprintSnackbar.Failed,
                        )
                    }
                },
            )
        }
    }
}
