package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.PrinterMachineInfo
import com.chronoswing.buddydash.data.model.PrinterMaintenanceOverview
import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.data.model.PrinterQueueSnapshot
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.BED_JOG_STEP_OPTIONS_MM
import com.chronoswing.buddydash.util.StartNextQueuedPrintReadiness
import com.chronoswing.buddydash.util.DEBUG_LOG_ARCHIVE_REPRINT
import com.chronoswing.buddydash.util.TAG_ARCHIVE_REPRINT
import com.chronoswing.buddydash.util.evaluateStartNextQueuedPrintReadiness
import com.chronoswing.buddydash.util.ControlAction
import com.chronoswing.buddydash.util.ControlFeedback
import com.chronoswing.buddydash.util.logControlFailure
import com.chronoswing.buddydash.util.PRINTER_STATUS_SETTLE_MS
import com.chronoswing.buddydash.util.isTransientState
import com.chronoswing.buddydash.util.RefreshGuard
import com.chronoswing.buddydash.util.motionDebugLog
import com.chronoswing.buddydash.util.toUserNetworkMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val printerModel: String? = null,
    val status: PrinterStatus? = null,
    val maintenanceItems: List<MaintenanceItem> = emptyList(),
    val totalPrintHours: Double? = null,
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
    val controlFeedback: ControlFeedback? = null,
    val isMaintenanceResetBusy: Boolean = false,
    val maintenanceResetSnackbar: MaintenanceResetSnackbar? = null,
    /** Epoch millis of last successful status fetch (for passive refresh indicator). */
    val lastStatusUpdatedAtMillis: Long? = null,
    val queueUpcoming: List<PrintQueueJob> = emptyList(),
    val startNextQueuedPrintReadiness: StartNextQueuedPrintReadiness =
        StartNextQueuedPrintReadiness(canStart = false),
    val isStartingQueuedPrint: Boolean = false,
    val startQueuedPrintSnackbar: StartQueuedPrintSnackbar? = null,
    /** Filament for active print: status JSON, else queue job with status=printing. */
    val activePrintFilamentUsage: FilamentUsage? = null,
    val machineInfo: PrinterMachineInfo? = null,
    val bedJogStepMm: Float = BED_JOG_STEP_OPTIONS_MM[1],
)

enum class StartQueuedPrintSnackbar {
    Started,
    Failed,
}

enum class PlateClearSnackbar {
    Success,
    Failed,
}

enum class MaintenanceResetSnackbar {
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
    private val manualRefreshGuard = RefreshGuard()

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

    fun init(printerId: Int, printerName: String, printerModel: String? = null) {
        this.printerId = printerId
        _uiState.update {
            it.copy(printerName = printerName, printerModel = printerModel, error = null)
        }
    }

    private fun maybeLoadStatus() {
        val state = _uiState.value
        if (printerId < 0 || !state.hasCredentials) return
        loadStatus()
    }

    fun loadStatus(
        showLoading: Boolean = true,
        fromPull: Boolean = false,
        fromUser: Boolean = false,
    ) {
        if (printerId < 0) return
        val state = _uiState.value
        if (!state.hasCredentials) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                )
            }
            return
        }
        if (fromUser && !fromPull && manualRefreshGuard.shouldSkipManualRefresh()) return
        if (fromUser && fetchJob?.isActive == true) return

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
                    apiClient.fetchMaintenance(state.serverUrl, state.apiKey, printerId).getOrNull()
                }
                val queueDeferred = async {
                    apiClient.fetchPrinterQueueSnapshot(state.serverUrl, state.apiKey, printerId)
                        .getOrNull()
                        ?: PrinterQueueSnapshot()
                }
                val machineInfoDeferred = async {
                    apiClient.fetchPrinterMachineInfo(state.serverUrl, state.apiKey, printerId)
                        .getOrNull()
                }
                StatusFetchBundle(
                    status = statusDeferred.await(),
                    maintenance = maintenanceDeferred.await(),
                    queue = queueDeferred.await(),
                    machineInfo = machineInfoDeferred.await(),
                )
            }

            statusResult.status.fold(
                onSuccess = { status ->
                    applyStatusFetchResult(status, statusResult)
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        val hadData = current.status != null
                        if (hadData) {
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = error.toUserNetworkMessage("Failed to load printer status"),
                            )
                        } else {
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                status = null,
                                maintenanceItems = emptyList(),
                                totalPrintHours = null,
                                queueUpcoming = emptyList(),
                                startNextQueuedPrintReadiness =
                                    StartNextQueuedPrintReadiness(canStart = false),
                                activePrintFilamentUsage = null,
                                error = error.toUserNetworkMessage("Failed to load printer status"),
                            )
                        }
                    }
                },
            )
        }
    }

    private fun applyStatusFetchResult(status: PrinterStatus, bundle: StatusFetchBundle) {
        val queueSnapshot = bundle.queue
        val activeFilament = status.filamentUsage
            ?: queueSnapshot.printing?.filamentUsage
        val upcoming = queueSnapshot.upcoming
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                status = status,
                maintenanceItems = bundle.maintenance?.items.orEmpty(),
                totalPrintHours = bundle.maintenance?.totalPrintHours,
                machineInfo = bundle.machineInfo,
                queueUpcoming = upcoming,
                startNextQueuedPrintReadiness = evaluateStartNextQueuedPrintReadiness(
                    status = status,
                    queuedItemCount = upcoming.size,
                ),
                activePrintFilamentUsage = activeFilament,
                error = null,
                lastStatusUpdatedAtMillis = System.currentTimeMillis(),
            )
        }
    }

    private suspend fun refreshStatusAfterControl() {
        val state = _uiState.value
        if (printerId < 0 || !state.hasCredentials) return

        fun applyMotionStatus(status: PrinterStatus) {
            _uiState.update { current ->
                val upcoming = current.queueUpcoming
                current.copy(
                    status = status,
                    startNextQueuedPrintReadiness = evaluateStartNextQueuedPrintReadiness(
                        status = status,
                        queuedItemCount = upcoming.size,
                    ),
                    activePrintFilamentUsage = status.filamentUsage
                        ?: current.activePrintFilamentUsage,
                    lastStatusUpdatedAtMillis = System.currentTimeMillis(),
                    error = null,
                )
            }
        }

        delay(PRINTER_STATUS_SETTLE_MS)
        apiClient.fetchPrinterStatus(state.serverUrl, state.apiKey, printerId).fold(
            onSuccess = { status ->
                if (!status.isTransientState()) applyMotionStatus(status)
            },
            onFailure = { },
        )
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

    fun setPrintSpeed(mode: Int) = runControl(ControlAction.PrintSpeed) {
        apiClient.setPrintSpeed(it.serverUrl, it.apiKey, printerId, mode)
    }

    fun pausePrint() = runControl(ControlAction.Pause) {
        apiClient.pausePrint(it.serverUrl, it.apiKey, printerId)
    }

    fun resumePrint() = runControl(ControlAction.Resume) {
        apiClient.resumePrint(it.serverUrl, it.apiKey, printerId)
    }

    fun stopPrint() = runControl(ControlAction.Stop) {
        apiClient.stopPrint(it.serverUrl, it.apiKey, printerId)
    }

    fun toggleChamberLight() {
        val state = _uiState.value
        val current = state.status?.chamberLightOn ?: return
        runControl(ControlAction.ChamberLight) {
            apiClient.setChamberLight(it.serverUrl, it.apiKey, printerId, on = !current)
        }
    }

    /** Release server camera stream resources when the live viewer closes. */
    fun stopCameraStream() {
        val state = _uiState.value
        if (printerId < 0 || !state.hasCredentials) return
        viewModelScope.launch {
            apiClient.stopCameraStream(state.serverUrl, state.apiKey, printerId)
        }
    }

    fun setBedJogStepMm(stepMm: Float) {
        if (stepMm in BED_JOG_STEP_OPTIONS_MM) {
            _uiState.update { it.copy(bedJogStepMm = stepMm) }
        }
    }

    /** Bed up (toward nozzle) — negative Z per Bambuddy API. */
    fun jogBedUp() = jogBed(-_uiState.value.bedJogStepMm, action = "up")

    /** Bed down (away from nozzle) — positive Z per Bambuddy API. */
    fun jogBedDown() = jogBed(_uiState.value.bedJogStepMm, action = "down")

    fun homePrinter() = runControl(ControlAction.HomeAxes) {
        apiClient.homeAxes(it.serverUrl, it.apiKey, printerId)
    }

    private fun jogBed(distanceMm: Float, action: String) = runControl(ControlAction.BedJog) {
        motionDebugLog(action, printerId, distanceMm)
        apiClient.bedJog(it.serverUrl, it.apiKey, printerId, distanceMm)
    }

    private fun runControl(
        action: ControlAction,
        block: suspend (PrinterDetailUiState) -> Result<Unit>,
    ) {
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
                            controlFeedback = ControlFeedback(
                                action = action,
                                success = true,
                            ),
                        )
                    }
                    when (action) {
                        ControlAction.HomeAxes,
                        ControlAction.BedJog,
                        -> refreshStatusAfterControl()
                        else -> loadStatus(showLoading = false)
                    }
                },
                onFailure = { error ->
                    val detail = error.message ?: error.toString()
                    logControlFailure(action, printerId, detail, error)
                    _uiState.update {
                        it.copy(
                            isControlBusy = false,
                            controlFeedback = ControlFeedback(
                                action = action,
                                success = false,
                                logDetail = detail,
                            ),
                        )
                    }
                },
            )
        }
    }

    fun onPlateClearSnackbarShown() {
        _uiState.update { it.copy(plateClearSnackbar = null) }
    }

    fun onControlFeedbackShown() {
        _uiState.update { it.copy(controlFeedback = null) }
    }

    fun performMaintenanceReset(itemId: Int) {
        if (printerId < 0 || itemId <= 0) return
        val state = _uiState.value
        if (!state.hasCredentials || state.isMaintenanceResetBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMaintenanceResetBusy = true) }
            apiClient.performMaintenance(state.serverUrl, state.apiKey, itemId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isMaintenanceResetBusy = false,
                            maintenanceResetSnackbar = MaintenanceResetSnackbar.Success,
                        )
                    }
                    loadStatus(showLoading = false)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isMaintenanceResetBusy = false,
                            maintenanceResetSnackbar = MaintenanceResetSnackbar.Failed,
                        )
                    }
                },
            )
        }
    }

    fun onMaintenanceResetSnackbarShown() {
        _uiState.update { it.copy(maintenanceResetSnackbar = null) }
    }

    fun startNextQueuedPrint() {
        if (printerId < 0) return
        val state = _uiState.value
        if (!state.hasCredentials || state.isStartingQueuedPrint) return
        if (!state.startNextQueuedPrintReadiness.canStart) return
        val nextItemId = state.queueUpcoming.firstOrNull()?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isStartingQueuedPrint = true) }
            apiClient.startQueueItem(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                queueItemId = nextItemId,
            ).fold(
                onSuccess = {
                    loadStatus(showLoading = false)
                    _uiState.update {
                        it.copy(
                            isStartingQueuedPrint = false,
                            startQueuedPrintSnackbar = StartQueuedPrintSnackbar.Started,
                        )
                    }
                },
                onFailure = { error ->
                    if (DEBUG_LOG_ARCHIVE_REPRINT) {
                        Log.e(
                            TAG_ARCHIVE_REPRINT,
                            "startNextQueuedPrint failed printerId=$printerId itemId=$nextItemId",
                            error,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isStartingQueuedPrint = false,
                            startQueuedPrintSnackbar = StartQueuedPrintSnackbar.Failed,
                        )
                    }
                },
            )
        }
    }

    fun onStartQueuedPrintSnackbarShown() {
        _uiState.update { it.copy(startQueuedPrintSnackbar = null) }
    }

    override fun onCleared() {
        fetchJob?.cancel()
        super.onCleared()
    }
}

private data class StatusFetchBundle(
    val status: Result<PrinterStatus>,
    val maintenance: PrinterMaintenanceOverview?,
    val queue: PrinterQueueSnapshot,
    val machineInfo: PrinterMachineInfo?,
)
