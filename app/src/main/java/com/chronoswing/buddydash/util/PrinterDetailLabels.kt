package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi

data class PrinterDetailLabels(
    val connection: String,
    val currentActivity: String,
    val lastPrintResult: String?,
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
)

fun PrinterStatus.toDetailLabels(): PrinterDetailLabels {
    val raw = rawState?.uppercase()
    val isPrinting = raw == "RUNNING"
    val isPaused = raw == "PAUSE"
    val isActivePrint = isPrinting || isPaused

    val currentActivity = when {
        !connected -> "Offline"
        hmsErrorCount > 0 && !isActivePrint -> "Error"
        isPrinting -> "Printing"
        isPaused -> "Paused"
        raw == "FAILED" -> "Error"
        raw == "FINISH" || raw == "IDLE" || raw.isNullOrBlank() -> "Idle"
        else -> formatStateLabel(raw) ?: "—"
    }

    val lastPrintResult = when (raw) {
        "FINISH" -> if (awaitingPlateClear == true) "Finished" else null
        "FAILED" -> "Failed"
        "STOP", "STOPPED", "CANCEL", "CANCELLED" -> "Cancelled"
        else -> null
    }

    val showFile = isActivePrint || (
        lastPrintResult != null && !fileName.isNullOrBlank()
    )
    val fileLabel = if (isActivePrint) "Current file" else "Last file"

    val showProgress = isActivePrint || (raw == "FINISH" && awaitingPlateClear == true)
    val progressTitle = when {
        isActivePrint -> "Progress"
        raw == "FINISH" -> "Last print progress"
        else -> "Progress"
    }
    val progressValue = when {
        isActivePrint -> formatProgress(progress)
        raw == "FINISH" -> "Completed"
        else -> formatProgress(progress)
    }
    val progressFraction = when {
        isActivePrint -> (progress ?: 0f).coerceIn(0f, 100f) / 100f
        raw == "FINISH" -> 1f
        else -> null
    }

    val plateStatus = awaitingPlateClear?.let { awaiting ->
        if (awaiting) "Not clear" else "Clear"
    }

    return PrinterDetailLabels(
        connection = formatConnection(connected),
        currentActivity = currentActivity,
        lastPrintResult = lastPrintResult,
        showFile = showFile,
        fileLabel = fileLabel,
        fileName = fileName.orEmpty(),
        showProgress = showProgress,
        progressTitle = progressTitle,
        progressValue = progressValue,
        progressFraction = progressFraction,
        plateStatus = plateStatus,
        showPlateClearAction = plateStatus == "Not clear",
        plateClearEndpointAvailable = BambuddyApi.hasClearPlateEndpoint,
        showEta = isActivePrint && (remainingTimeSeconds ?: 0) > 0,
        eta = formatEta(remainingTimeSeconds),
        nozzleTemp = formatTemp(nozzleTemp),
        bedTemp = formatTemp(bedTemp),
        hmsHealth = formatHmsHealth(hmsErrorCount),
        hmsHasErrors = hmsErrorCount > 0,
    )
}

private fun formatStateLabel(state: String): String? =
    state.lowercase().replaceFirstChar { it.uppercase() }
