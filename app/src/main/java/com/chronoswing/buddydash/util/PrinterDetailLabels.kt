package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi

// DEBUG: Set to false before release — keeps "Mark plate clear" visible for testing.
private const val DEBUG_ALWAYS_SHOW_CLEAR_BUTTON = false

data class PrinterDetailLabels(
    val isActivePrint: Boolean,
    val activityKind: PrinterActivityKind,
    val progressCompact: String?,
    val plateKind: PlateIndicatorKind?,
    val connection: String,
    val currentActivity: String,
    val lastPrintResult: String?,
    val showLastPrintOnCard: Boolean,
    val showFile: Boolean,
    val fileLabel: String,
    val fileName: String,
    val showProgress: Boolean,
    val progressTitle: String,
    val progressValue: String,
    val progressFraction: Float?,
    val plateStatus: String?,
    val showPlateClearAction: Boolean,
    val plateClearEndpointAvailable: Boolean,
    val showEta: Boolean,
    val eta: String,
    val tempsLine: String?,
    val hmsSummary: String,
    val nozzleTemp: String,
    val bedTemp: String,
    val hmsHealth: String,
    val hmsHasErrors: Boolean,
    val filamentSlots: List<FilamentSlot>,
    val activeFilamentSlot: SlotInventoryKey? = null,
    val printerRawState: String? = null,
    val cardMicroMotion: CardMicroMotion = CardMicroMotion.None,
    val amsUnits: List<AmsUnitInfo>,
    val showConnectivitySection: Boolean,
    val wifiCompact: String?,
    val doorLine: String?,
    val firmwareLine: String?,
    val totalPrintTimeCompact: String? = null,
    val nozzleDiameterCompact: String? = null,
    val chamberTempCompact: String?,
    val showFansSection: Boolean,
    val partFanPercent: Int?,
    val auxFanPercent: Int?,
    val chamberFanPercent: Int?,
    val showPrintSpeedSection: Boolean,
    val printSpeedLabel: String?,
    val speedLevel: Int?,
    val chamberLightOn: Boolean?,
    val canControlPrint: Boolean,
    val canPause: Boolean,
    val canResume: Boolean,
    val canStop: Boolean,
    val canToggleLight: Boolean,
    val motionLayout: PrinterMotionLayout,
    val showMotionControls: Boolean,
    val canUseMotionControls: Boolean,
    val maintenanceItems: List<MaintenanceItem>,
)

fun PrinterStatus.toDetailLabels(
    maintenanceItems: List<MaintenanceItem> = emptyList(),
    totalPrintHours: Double? = null,
    printerModel: String? = null,
): PrinterDetailLabels {
    val raw = rawState?.uppercase()
    val isPrinting = raw == "RUNNING"
    val isPaused = raw == "PAUSE"
    val isActivePrint = isPrinting || isPaused
    val state = toStateDisplay()

    val showFile = isActivePrint || (
        state.lastPrintResult != null && !fileName.isNullOrBlank()
    )
    val fileLabel = if (isActivePrint) "Current file" else "Last file"

    val showProgress = isActivePrint
    val progressTitle = "Progress"
    val progressValue = formatProgress(progress)
    val progressFraction = if (isActivePrint) {
        (progress ?: 0f).coerceIn(0f, 100f) / 100f
    } else {
        null
    }

    val plateStatus = awaitingPlateClear?.let { awaiting ->
        if (awaiting) "Not clear" else "Clear"
    }

    val wifiCompact = formatWifiCompact(wifiSignalDbm, wiredNetwork)
    val doorLine = formatDoorState(doorOpen)
    val firmwareLine = firmwareVersion?.takeIf { it.isNotBlank() }
    val chamberTempCompact = formatChamberTempCompact(chamberTemp)
    val printSpeedLabel = formatPrintSpeedLevel(speedLevel)
    val canControlPrint = connected && isActivePrint
    val motionLayout = if (connected) {
        resolveMotionLayout(printerModel)
    } else {
        PrinterMotionLayout.Hidden
    }

    val activityKind = resolveActivityKind()
    return PrinterDetailLabels(
        isActivePrint = isActivePrint,
        activityKind = activityKind,
        progressCompact = activityKind.progressSuffix(progress),
        plateKind = resolvePlateKind(),
        connection = formatConnection(connected),
        currentActivity = state.currentActivity,
        lastPrintResult = state.lastPrintResult,
        showLastPrintOnCard = !isActivePrint && state.showLastPrintOnCard,
        showFile = showFile,
        fileLabel = fileLabel,
        fileName = fileName.orEmpty(),
        showProgress = showProgress,
        progressTitle = progressTitle,
        progressValue = progressValue,
        progressFraction = progressFraction,
        plateStatus = plateStatus,
        showPlateClearAction = plateStatus == "Not clear" || DEBUG_ALWAYS_SHOW_CLEAR_BUTTON,
        plateClearEndpointAvailable = BambuddyApi.hasClearPlateEndpoint,
        showEta = isActivePrint && formatEta(remainingTimeSeconds) != null,
        eta = formatEta(remainingTimeSeconds).orEmpty(),
        tempsLine = if (isActivePrint) formatPrintTempsLine(nozzleTemp, bedTemp) else null,
        hmsSummary = formatHmsSummary(hmsErrorCount),
        nozzleTemp = formatTemp(nozzleTemp),
        bedTemp = formatTemp(bedTemp),
        hmsHealth = formatHmsHealth(hmsErrorCount),
        hmsHasErrors = hmsErrorCount > 0,
        filamentSlots = filamentSlots,
        activeFilamentSlot = activeFilamentSlot,
        printerRawState = rawState,
        cardMicroMotion = resolveCardMicroMotion(activityKind, rawState),
        amsUnits = amsUnits,
        showConnectivitySection = hasConnectivitySection(totalPrintHours),
        wifiCompact = wifiCompact,
        doorLine = doorLine,
        firmwareLine = firmwareLine,
        totalPrintTimeCompact = formatTotalPrintTimeCompact(totalPrintHours),
        nozzleDiameterCompact = nozzleDiameterDisplay,
        chamberTempCompact = chamberTempCompact,
        showFansSection = hasFansSection(),
        partFanPercent = partFanPercent,
        auxFanPercent = auxFanPercent,
        chamberFanPercent = chamberFanPercent,
        showPrintSpeedSection = hasPrintSpeedSection(),
        printSpeedLabel = printSpeedLabel,
        speedLevel = speedLevel,
        chamberLightOn = chamberLightOn,
        canControlPrint = canControlPrint,
        canPause = canControlPrint && isPrinting,
        canResume = connected && isPaused,
        canStop = canControlPrint,
        canToggleLight = connected && chamberLightOn != null,
        motionLayout = motionLayout,
        showMotionControls = motionLayout != PrinterMotionLayout.Hidden,
        canUseMotionControls = motionLayout != PrinterMotionLayout.Hidden && canAdjustBedWhenIdle(),
        maintenanceItems = maintenanceItems,
    )
}
