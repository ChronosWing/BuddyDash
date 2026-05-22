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
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.util.FilamentSlotDisplay
import com.chronoswing.buddydash.util.PrinterFilamentActivity
import com.chronoswing.buddydash.util.SpoolAssignmentTargetConflict
import com.chronoswing.buddydash.util.SpoolInventoryCardUsage
import com.chronoswing.buddydash.util.evaluateSpoolAssignmentConflict
import com.chronoswing.buddydash.util.resolveActivityKind
import com.chronoswing.buddydash.util.resolveSpoolInventoryCardUsage
import com.chronoswing.buddydash.util.RefreshGuard
import com.chronoswing.buddydash.util.RefreshIntervals
import com.chronoswing.buddydash.util.RefreshSource
import com.chronoswing.buddydash.util.isConnectionDisplayStale
import com.chronoswing.buddydash.util.isDataStale
import com.chronoswing.buddydash.util.logRefreshDecision
import com.chronoswing.buddydash.util.SpoolInventoryFilter
import com.chronoswing.buddydash.util.applySpoolInventorySearch
import com.chronoswing.buddydash.util.buildFilamentSlotDisplays
import com.chronoswing.buddydash.util.filterSpoolsForSlotAssignment
import com.chronoswing.buddydash.util.formatSpoolCardTitle
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
    val refreshError: String? = null,
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
    val filamentSlotDisplays: List<FilamentSlotDisplay> = emptyList(),
    val inventorySpools: List<SpoolInventoryItem> = emptyList(),
    val filamentSlotSheet: FilamentSlotDisplay? = null,
    val filamentSpoolPickerOpen: Boolean = false,
    val spoolPickerSearchQuery: String = "",
    val assignSpoolConfirm: AssignSpoolConfirm? = null,
    val clearAssignmentConfirm: FilamentSlotDisplay? = null,
    val isFilamentAssignBusy: Boolean = false,
    val filamentAssignSnackbar: FilamentAssignSnackbar? = null,
    /** Queue item id for status=printing (thumbnail cache identity). */
    val printingQueueJobId: Int? = null,
    /** One-shot navigation to spool detail; consumed by UI before navigate. */
    val pendingSpoolDetailNavigationId: Int? = null,
)

data class AssignSpoolConfirm(
    val slotDisplay: FilamentSlotDisplay,
    val spool: SpoolInventoryItem,
    val conflict: com.chronoswing.buddydash.util.SpoolAssignmentTargetConflict =
        com.chronoswing.buddydash.util.SpoolAssignmentTargetConflict.None,
)

enum class FilamentAssignSnackbar {
    Assigned,
    AssignFailed,
    Cleared,
    ClearFailed,
}

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
    private var filamentInventoryJob: Job? = null
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

    fun onRefreshErrorShown() {
        _uiState.update { it.copy(refreshError = null) }
    }

    fun refreshOnAppResume(currentRoute: String? = null) {
        val state = _uiState.value
        if (printerId < 0 || !state.hasCredentials) return
        val staleByAge = isDataStale(state.lastStatusUpdatedAtMillis, RefreshIntervals.HOME_MS)
        val staleByConnection = isConnectionDisplayStale(state.lastStatusUpdatedAtMillis)
        val stale = staleByAge || staleByConnection || state.status == null
        logRefreshDecision(
            screen = "PrinterDetail",
            source = RefreshSource.APP_RESUME,
            currentRoute = currentRoute,
            lastUpdatedAtMillis = state.lastStatusUpdatedAtMillis,
            intervalMs = RefreshIntervals.HOME_MS,
            stale = stale,
            refreshTriggered = true,
        )
        if (fetchJob?.isActive == true) return
        loadStatus(showLoading = false)
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
                    refreshError = null,
                )
            }
            return
        }
        if (fromUser && !fromPull && manualRefreshGuard.shouldSkipManualRefresh()) return
        if (fromUser && fetchJob?.isActive == true) return

        val hasCachedData = state.status != null
        val isInitialLoad = !hasCachedData

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (!isInitialLoad) {
                _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
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
                                refreshError = error.toUserNetworkMessage("Could not refresh"),
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
                                printingQueueJobId = null,
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
                printingQueueJobId = queueSnapshot.printing?.id,
                error = null,
                refreshError = null,
                lastStatusUpdatedAtMillis = System.currentTimeMillis(),
            )
        }
        refreshFilamentSlotDisplays(status)
    }

    private fun refreshFilamentSlotDisplays(status: PrinterStatus) {
        filamentInventoryJob?.cancel()
        filamentInventoryJob = viewModelScope.launch {
            val state = _uiState.value
            if (!state.hasCredentials || printerId < 0) return@launch
            val inventoryResult = apiClient.fetchInventoryForPrinter(
                state.serverUrl,
                state.apiKey,
                printerId,
            )
            val spoolsResult = apiClient.fetchSpoolInventory(state.serverUrl, state.apiKey)
            val inventoryBySlot = inventoryResult.getOrElse { emptyMap() }
            val spools = spoolsResult.getOrElse { emptyList() }
            val spoolsById = spools.associateBy { it.id }
            val assignedToPrinter = spools.filter { it.assignment?.printerId == printerId }
            val displays = buildFilamentSlotDisplays(
                slots = status.filamentSlots,
                activeKey = status.activeFilamentSlot,
                printerId = printerId,
                inventoryBySlot = inventoryBySlot,
                spoolsById = spoolsById,
                spoolsAssignedToPrinter = assignedToPrinter,
            )
            if (BuddyDashDebug.enabled) {
                android.util.Log.d(
                    "BuddyDash/FilamentTab",
                    "slotDisplays=${displays.size} inventoryKeys=${inventoryBySlot.size} " +
                        "spools=${spools.size} matched=${displays.count { it.isTappable }}",
                )
            }
            _uiState.update {
                it.copy(
                    filamentSlotDisplays = displays,
                    inventorySpools = spools,
                )
            }
        }
    }

    fun openFilamentSlotSheet(display: FilamentSlotDisplay) {
        if (!display.canAssign) return
        _uiState.update {
            it.copy(
                filamentSlotSheet = display,
                filamentSpoolPickerOpen = false,
                spoolPickerSearchQuery = "",
                assignSpoolConfirm = null,
                clearAssignmentConfirm = null,
            )
        }
        logFilamentNavState("openFilamentSlotSheet")
    }

    fun dismissFilamentSlotSheet() {
        dismissFilamentTransientUi()
    }

    fun dismissFilamentTransientUi() {
        _uiState.update {
            it.copy(
                filamentSlotSheet = null,
                filamentSpoolPickerOpen = false,
                spoolPickerSearchQuery = "",
                assignSpoolConfirm = null,
                clearAssignmentConfirm = null,
                pendingSpoolDetailNavigationId = null,
            )
        }
        logFilamentNavState("dismissFilamentTransientUi")
    }

    fun viewSpoolFromSlot(spoolId: Int) {
        logFilamentNavState("viewSpoolFromSlot before")
        _uiState.update {
            it.copy(
                filamentSlotSheet = null,
                filamentSpoolPickerOpen = false,
                spoolPickerSearchQuery = "",
                assignSpoolConfirm = null,
                clearAssignmentConfirm = null,
                pendingSpoolDetailNavigationId = spoolId,
            )
        }
        logFilamentNavState("viewSpoolFromSlot after pendingNav=$spoolId")
    }

    fun consumePendingSpoolDetailNavigation(): Int? {
        var consumed: Int? = null
        _uiState.update {
            consumed = it.pendingSpoolDetailNavigationId
            it.copy(pendingSpoolDetailNavigationId = null)
        }
        if (BuddyDashDebug.enabled) {
            Log.d(
                "BuddyDash/FilamentNav",
                "consumePendingSpoolDetailNavigation consumed=${consumed != null} spoolId=$consumed",
            )
        }
        return consumed
    }

    private fun logFilamentNavState(tag: String) {
        if (!BuddyDashDebug.enabled) return
        val s = _uiState.value
        Log.d(
            "BuddyDash/FilamentNav",
            "$tag selectedSlot=${s.filamentSlotSheet?.slot?.label} " +
                "sheetVisible=${s.filamentSlotSheet != null} pickerOpen=${s.filamentSpoolPickerOpen} " +
                "assignConfirm=${s.assignSpoolConfirm != null} clearConfirm=${s.clearAssignmentConfirm != null} " +
                "pendingNav=${s.pendingSpoolDetailNavigationId}",
        )
    }

    fun openFilamentSpoolPicker() {
        _uiState.update {
            it.copy(
                filamentSpoolPickerOpen = true,
                spoolPickerSearchQuery = "",
            )
        }
    }

    fun dismissFilamentSpoolPicker() {
        _uiState.update { it.copy(filamentSpoolPickerOpen = false, spoolPickerSearchQuery = "") }
    }

    fun onSpoolPickerSearchChange(query: String) {
        _uiState.update { it.copy(spoolPickerSearchQuery = query) }
    }

    fun spoolsForPicker(): List<SpoolInventoryItem> {
        val state = _uiState.value
        val slot = state.filamentSlotSheet?.slot ?: return emptyList()
        val filtered = filterSpoolsForSlotAssignment(state.inventorySpools, slot)
        return applySpoolInventorySearch(filtered, state.spoolPickerSearchQuery, SpoolInventoryFilter.All)
    }

    fun requestAssignSpool(spool: SpoolInventoryItem) {
        val slotDisplay = _uiState.value.filamentSlotSheet ?: return
        val conflict = evaluateSpoolAssignmentConflict(
            spool = spool,
            targetPrinterId = printerId,
            targetSlotLabel = slotDisplay.slot.label,
            currentSlotAssignedSpoolId = slotDisplay.assignedSpoolId,
        )
        if (conflict is SpoolAssignmentTargetConflict.AlreadyOnTarget) {
            if (BuddyDashDebug.enabled) {
                Log.d("BuddyDash/FilamentTab", "requestAssignSpool skipped: already on target spool=${spool.id}")
            }
            return
        }
        _uiState.update {
            it.copy(
                assignSpoolConfirm = AssignSpoolConfirm(slotDisplay, spool, conflict),
                filamentSpoolPickerOpen = false,
            )
        }
    }

    fun pickerAssignmentConflict(spool: SpoolInventoryItem): SpoolAssignmentTargetConflict {
        val slotDisplay = _uiState.value.filamentSlotSheet ?: return SpoolAssignmentTargetConflict.None
        return evaluateSpoolAssignmentConflict(
            spool = spool,
            targetPrinterId = printerId,
            targetSlotLabel = slotDisplay.slot.label,
            currentSlotAssignedSpoolId = slotDisplay.assignedSpoolId,
        )
    }

    fun pickerCardUsageFor(spool: SpoolInventoryItem): SpoolInventoryCardUsage {
        val status = _uiState.value.status ?: return resolveSpoolInventoryCardUsage(spool, emptyMap())
        val activity = PrinterFilamentActivity(
            activityKind = status.resolveActivityKind(),
            activeFilamentSlot = status.activeFilamentSlot,
        )
        return resolveSpoolInventoryCardUsage(spool, mapOf(printerId to activity))
    }

    fun dismissAssignSpoolConfirm() {
        _uiState.update { it.copy(assignSpoolConfirm = null) }
    }

    fun confirmAssignSpool() {
        val confirm = _uiState.value.assignSpoolConfirm ?: return
        val key = confirm.slotDisplay.slot.inventoryKey ?: return
        _uiState.update { it.copy(assignSpoolConfirm = null, isFilamentAssignBusy = true) }
        viewModelScope.launch {
            val state = _uiState.value
            val result = apiClient.assignSpoolToSlot(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                printerId = printerId,
                amsId = key.amsId,
                trayId = key.trayId,
                spoolId = confirm.spool.id,
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isFilamentAssignBusy = false,
                            filamentAssignSnackbar = FilamentAssignSnackbar.Assigned,
                            filamentSlotSheet = null,
                        )
                    }
                    loadStatus(showLoading = false)
                },
                onFailure = { error ->
                    if (BuddyDashDebug.enabled) {
                        android.util.Log.w("BuddyDash/FilamentTab", "assign spool failed", error)
                    }
                    _uiState.update {
                        it.copy(
                            isFilamentAssignBusy = false,
                            filamentAssignSnackbar = FilamentAssignSnackbar.AssignFailed,
                        )
                    }
                },
            )
        }
    }

    fun requestClearSlotAssignment() {
        val sheet = _uiState.value.filamentSlotSheet ?: return
        if (sheet.assignedSpoolId == null) return
        _uiState.update { it.copy(clearAssignmentConfirm = sheet) }
    }

    fun dismissClearAssignmentConfirm() {
        _uiState.update { it.copy(clearAssignmentConfirm = null) }
    }

    fun confirmClearSlotAssignment() {
        val slotDisplay = _uiState.value.clearAssignmentConfirm ?: return
        val key = slotDisplay.slot.inventoryKey ?: return
        _uiState.update { it.copy(clearAssignmentConfirm = null, isFilamentAssignBusy = true) }
        viewModelScope.launch {
            val state = _uiState.value
            val result = apiClient.unassignSpoolFromSlot(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                printerId = printerId,
                amsId = key.amsId,
                trayId = key.trayId,
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isFilamentAssignBusy = false,
                            filamentAssignSnackbar = FilamentAssignSnackbar.Cleared,
                            filamentSlotSheet = null,
                        )
                    }
                    loadStatus(showLoading = false)
                },
                onFailure = { error ->
                    if (BuddyDashDebug.enabled) {
                        android.util.Log.w("BuddyDash/FilamentTab", "clear assignment failed", error)
                    }
                    _uiState.update {
                        it.copy(
                            isFilamentAssignBusy = false,
                            filamentAssignSnackbar = FilamentAssignSnackbar.ClearFailed,
                        )
                    }
                },
            )
        }
    }

    fun onFilamentAssignSnackbarShown() {
        _uiState.update { it.copy(filamentAssignSnackbar = null) }
    }

    fun assignConfirmSpoolTitle(): String {
        val confirm = _uiState.value.assignSpoolConfirm ?: return ""
        return formatSpoolCardTitle(confirm.spool)
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
        filamentInventoryJob?.cancel()
        super.onCleared()
    }
}

private data class StatusFetchBundle(
    val status: Result<PrinterStatus>,
    val maintenance: PrinterMaintenanceOverview?,
    val queue: PrinterQueueSnapshot,
    val machineInfo: PrinterMachineInfo?,
)
