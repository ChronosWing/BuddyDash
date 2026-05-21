package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi

// DEBUG: Set to false before release — keeps "Mark plate clear" visible for testing.
private const val DEBUG_ALWAYS_SHOW_CLEAR_BUTTON = false

data class PrinterDetailLabels(
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
    val nozzleTemp: String,
    val bedTemp: String,
    val hmsHealth: String,
    val hmsHasErrors: Boolean,
    val filamentSlots: List<FilamentSlot>,
)

fun PrinterStatus.toDetailLabels(): PrinterDetailLabels {
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

    return PrinterDetailLabels(
        connection = formatConnection(connected),
        currentActivity = state.currentActivity,
        lastPrintResult = state.lastPrintResult,
        showLastPrintOnCard = state.showLastPrintOnCard,
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
        showEta = isActivePrint && (remainingTimeSeconds ?: 0) > 0,
        eta = formatEta(remainingTimeSeconds),
        nozzleTemp = formatTemp(nozzleTemp),
        bedTemp = formatTemp(bedTemp),
        hmsHealth = formatHmsHealth(hmsErrorCount),
        hmsHasErrors = hmsErrorCount > 0,
        filamentSlots = filamentSlots,
    )
}
