package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.ArchiveReprintPrinter
import com.chronoswing.buddydash.util.DEBUG_LOG_ARCHIVE_DETAIL
import com.chronoswing.buddydash.util.DEBUG_LOG_ARCHIVE_REPRINT
import com.chronoswing.buddydash.util.QueueAndStartBlockReason
import com.chronoswing.buddydash.util.QueueAndStartReadiness
import com.chronoswing.buddydash.util.TAG_ARCHIVE_DETAIL
import com.chronoswing.buddydash.util.TAG_ARCHIVE_REPRINT
import com.chronoswing.buddydash.util.defaultArchiveReprintPrinterId
import com.chronoswing.buddydash.util.defaultArchiveReprintQuantity
import com.chronoswing.buddydash.util.ArchiveMaterialNavigation
import com.chronoswing.buddydash.util.archiveHasMaterialDisplay
import com.chronoswing.buddydash.util.buildArchiveSpoolLookupFilter
import com.chronoswing.buddydash.util.evaluateQueueAndStartReadiness
import com.chronoswing.buddydash.util.logArchiveDetailFieldMapping
import com.chronoswing.buddydash.util.resolveArchiveMaterialNavigation
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.util.resolveActivityKind
import com.chronoswing.buddydash.util.resolveArchiveReprintPrinters
import com.chronoswing.buddydash.util.resolvePlateKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ArchiveReprintSnackbar {
    Queued,
    Started,
    QueuedStartFailed,
    Failed,
}

data class ArchiveReprintSheetState(
    val isOpen: Boolean = false,
    val isLoadingPrinters: Boolean = false,
    val isLoadingReadiness: Boolean = false,
    val isSubmitting: Boolean = false,
    val compatiblePrinters: List<ArchiveReprintPrinter> = emptyList(),
    val hiddenIncompatibleCount: Int = 0,
    val selectedPrinterId: Int? = null,
    val quantity: Int = 1,
    val canQueueOnly: Boolean = false,
    val queueAndStartReadiness: QueueAndStartReadiness = QueueAndStartReadiness(canQueueAndStart = false),
) {
    val canQueueAndStart: Boolean get() = queueAndStartReadiness.canQueueAndStart
}

data class ArchiveDetailUiState(
    val archive: PrintArchive? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val settingsReady: Boolean = false,
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
    private var readinessJob: Job? = null
    private var queueJob: Job? = null
    private var spoolsJob: Job? = null
    private var cachedSpools: List<SpoolInventoryItem>? = null

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
                val hasCredentials = url.isNotBlank() && key.isNotBlank()
                if (DEBUG_LOG_ARCHIVE_DETAIL) {
                    Log.d(
                        TAG_ARCHIVE_DETAIL,
                        "settingsReady urlSet=${url.isNotBlank()} keySet=${key.isNotBlank()} " +
                            "hasCredentials=$hasCredentials archiveId=$archiveId",
                    )
                }
                _uiState.update {
                    it.copy(
                        serverUrl = url,
                        apiKey = key,
                        cameraToken = cameraToken,
                        hasCredentials = hasCredentials,
                        settingsReady = true,
                    )
                }
                maybeLoadArchive()
            }
        }
    }

    fun init(archiveId: Int) {
        this.archiveId = archiveId
        _uiState.update { it.copy(error = null) }
        maybeLoadArchive()
    }

    fun loadArchive() {
        maybeLoadArchive(force = true)
    }

    private fun maybeLoadArchive(force: Boolean = false) {
        val state = _uiState.value
        if (archiveId < 0) return
        if (!state.settingsReady) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            return
        }
        if (!force && state.archive != null) return

        if (!state.hasCredentials) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Configure server URL and API key in Settings",
                )
            }
            return
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val creds = _uiState.value
            _uiState.update { it.copy(isLoading = it.archive == null, error = null) }
            val result = apiClient.fetchArchive(
                serverUrl = creds.serverUrl,
                apiKey = creds.apiKey,
                archiveId = archiveId,
            )
            result.fold(
                onSuccess = { archive ->
                    logArchiveDetailFieldMapping(archive)
                    _uiState.update {
                        it.copy(isLoading = false, archive = archive, error = null)
                    }
                    loadSpoolsForMaterialLink(archive)
                },
                onFailure = { error ->
                    if (DEBUG_LOG_ARCHIVE_DETAIL) {
                        Log.e(TAG_ARCHIVE_DETAIL, "fetchArchive failed id=$archiveId", error)
                    }
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
        readinessJob?.cancel()
        _uiState.update {
            it.copy(reprintSheet = ArchiveReprintSheetState())
        }
    }

    fun onReprintPrinterSelected(printerId: Int) {
        _uiState.update { state ->
            state.copy(
                reprintSheet = state.reprintSheet.copy(
                    selectedPrinterId = printerId,
                    canQueueOnly = true,
                ),
            )
        }
        refreshPrinterReadiness(printerId)
    }

    fun onReprintQuantityChange(delta: Int) {
        _uiState.update { state ->
            val next = (state.reprintSheet.quantity + delta).coerceIn(1, 99)
            state.copy(reprintSheet = state.reprintSheet.copy(quantity = next))
        }
    }

    fun onConfirmQueueOnly() {
        submitQueue(startAfterQueue = false)
    }

    fun onConfirmQueueAndStart() {
        submitQueue(startAfterQueue = true)
    }

    fun onReprintSnackbarShown() {
        _uiState.update { it.copy(reprintSnackbar = null) }
    }

    private fun submitQueue(startAfterQueue: Boolean) {
        val state = _uiState.value
        val archive = state.archive ?: return
        val printerId = state.reprintSheet.selectedPrinterId ?: return
        if (!state.hasCredentials || state.reprintSheet.isSubmitting) return
        if (startAfterQueue && !state.reprintSheet.canQueueAndStart) return

        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            _uiState.update {
                it.copy(reprintSheet = it.reprintSheet.copy(isSubmitting = true))
            }
            val printer = state.reprintSheet.compatiblePrinters.find { it.id == printerId }
            val queueResult = apiClient.addArchiveToQueue(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                archiveId = archive.id,
                printerId = printerId,
                quantity = state.reprintSheet.quantity,
            )
            queueResult.fold(
                onSuccess = { queueItemId ->
                    if (!startAfterQueue) {
                        finishQueueSuccess(printerId, printer)
                        return@fold
                    }
                    val startResult = apiClient.startQueueItem(
                        serverUrl = state.serverUrl,
                        apiKey = state.apiKey,
                        queueItemId = queueItemId,
                    )
                    apiClient.fetchPrinterStatus(state.serverUrl, state.apiKey, printerId)
                    apiClient.fetchPrinterQueueSnapshot(state.serverUrl, state.apiKey, printerId)
                    startResult.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    reprintSheet = ArchiveReprintSheetState(),
                                    reprintSnackbar = ArchiveReprintSnackbar.Started,
                                    queuedPrinterId = printerId,
                                    queuedPrinterName = printer?.name,
                                    queuedPrinterModel = printer?.model,
                                )
                            }
                        },
                        onFailure = {
                            _uiState.update {
                                it.copy(
                                    reprintSheet = ArchiveReprintSheetState(),
                                    reprintSnackbar = ArchiveReprintSnackbar.QueuedStartFailed,
                                    queuedPrinterId = printerId,
                                    queuedPrinterName = printer?.name,
                                    queuedPrinterModel = printer?.model,
                                )
                            }
                        },
                    )
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

    private fun finishQueueSuccess(printerId: Int, printer: ArchiveReprintPrinter?) {
        _uiState.update {
            it.copy(
                reprintSheet = ArchiveReprintSheetState(),
                reprintSnackbar = ArchiveReprintSnackbar.Queued,
                queuedPrinterId = printerId,
                queuedPrinterName = printer?.name,
                queuedPrinterModel = printer?.model,
            )
        }
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
                                canQueueOnly = selectedId != null,
                            ),
                        )
                    }
                    selectedId?.let { refreshPrinterReadiness(it) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            reprintSheet = it.reprintSheet.copy(
                                isLoadingPrinters = false,
                                compatiblePrinters = emptyList(),
                                canQueueOnly = false,
                            ),
                            reprintSnackbar = ArchiveReprintSnackbar.Failed,
                        )
                    }
                },
            )
        }
    }

    fun onMaterialTap(onNavigate: (ArchiveMaterialNavigation) -> Unit) {
        val state = _uiState.value
        val archive = state.archive ?: return
        if (!state.hasCredentials || !archiveHasMaterialDisplay(archive)) return

        spoolsJob?.cancel()
        spoolsJob = viewModelScope.launch {
            val spools = cachedSpools ?: fetchSpoolInventoryCached(state.serverUrl, state.apiKey)
            val navigation = spools?.let { resolveArchiveMaterialNavigation(archive, it) }
                ?: ArchiveMaterialNavigation.SpoolsFiltered(
                    buildArchiveSpoolLookupFilter(archive),
                )
            onNavigate(navigation)
        }
    }

    private fun loadSpoolsForMaterialLink(archive: PrintArchive) {
        val state = _uiState.value
        if (!state.hasCredentials) return

        spoolsJob?.cancel()
        spoolsJob = viewModelScope.launch {
            fetchSpoolInventoryCached(state.serverUrl, state.apiKey)
        }
    }

    private suspend fun fetchSpoolInventoryCached(
        serverUrl: String,
        apiKey: String,
    ): List<SpoolInventoryItem>? =
        apiClient.fetchSpoolInventory(serverUrl, apiKey).fold(
            onSuccess = { spools ->
                cachedSpools = spools
                spools
            },
            onFailure = { null },
        )

    private fun refreshPrinterReadiness(printerId: Int) {
        val state = _uiState.value
        if (!state.hasCredentials) return

        readinessJob?.cancel()
        readinessJob = viewModelScope.launch {
            _uiState.update {
                it.copy(reprintSheet = it.reprintSheet.copy(isLoadingReadiness = true))
            }
            val statusResult = apiClient.fetchPrinterStatus(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                printerId = printerId,
            )
            val readiness = statusResult.fold(
                onSuccess = { status ->
                    if (DEBUG_LOG_ARCHIVE_REPRINT) {
                        Log.d(
                            TAG_ARCHIVE_REPRINT,
                            "readiness printerId=$printerId connected=${status.connected} " +
                                "raw=${status.rawState} activity=${status.resolveActivityKind()} " +
                                "plate=${status.resolvePlateKind()}",
                        )
                    }
                    evaluateQueueAndStartReadiness(status)
                },
                onFailure = { error ->
                    if (DEBUG_LOG_ARCHIVE_REPRINT) {
                        Log.e(TAG_ARCHIVE_REPRINT, "readiness fetch failed printerId=$printerId", error)
                    }
                    QueueAndStartReadiness(canQueueAndStart = false)
                },
            )
            _uiState.update {
                it.copy(
                    reprintSheet = it.reprintSheet.copy(
                        isLoadingReadiness = false,
                        queueAndStartReadiness = readiness,
                    ),
                )
            }
        }
    }
}
