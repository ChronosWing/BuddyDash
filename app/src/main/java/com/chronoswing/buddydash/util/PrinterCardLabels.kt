package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus

data class PrinterCardLabels(
    val title: String,
    val subtitle: String?,
    val connection: String,
    val isConnected: Boolean,
    val currentActivity: String,
    val plateStatus: String?,
    val lastPrintResult: String?,
    val showLastPrint: Boolean,
    val progressText: String?,
    val progressFraction: Float?,
    val fileLine: String?,
    val nozzleTemp: String,
    val bedTemp: String,
    val hmsHealth: String,
    val hmsHasErrors: Boolean,
    val filamentSlots: List<FilamentSlot>,
)

fun Printer.toCardLabels(): PrinterCardLabels {
    val (title, subtitle) = displayNameAndModel(name, model)
    val status = liveStatus
    if (status == null) {
        return PrinterCardLabels(
            title = title,
            subtitle = subtitle,
            connection = "—",
            isConnected = false,
            currentActivity = "—",
            plateStatus = null,
            lastPrintResult = null,
            showLastPrint = false,
            progressText = null,
            progressFraction = null,
            fileLine = null,
            nozzleTemp = "—",
            bedTemp = "—",
            hmsHealth = "—",
            hmsHasErrors = false,
            filamentSlots = emptyList(),
        )
    }
    val detail = status.toDetailLabels()
    val raw = status.rawState?.uppercase()
    val isActivePrint = raw == "RUNNING" || raw == "PAUSE"

    val fileLine = when {
        isActivePrint && !status.fileName.isNullOrBlank() -> status.fileName
        detail.showLastPrintOnCard && !status.fileName.isNullOrBlank() -> status.fileName
        else -> null
    }

    return PrinterCardLabels(
        title = title,
        subtitle = subtitle,
        connection = if (status.connected) "Connected" else "Offline",
        isConnected = status.connected,
        currentActivity = detail.currentActivity,
        plateStatus = detail.plateStatus?.let { plate ->
            when (plate) {
                "Clear" -> "Plate clear"
                "Not clear" -> "Plate not clear"
                else -> plate
            }
        },
        lastPrintResult = detail.lastPrintResult,
        showLastPrint = detail.showLastPrintOnCard,
        progressText = if (isActivePrint) formatProgress(status.progress) else null,
        progressFraction = if (isActivePrint) {
            (status.progress ?: 0f).coerceIn(0f, 100f) / 100f
        } else {
            null
        },
        fileLine = fileLine,
        nozzleTemp = detail.nozzleTemp,
        bedTemp = detail.bedTemp,
        hmsHealth = detail.hmsHealth,
        hmsHasErrors = detail.hmsHasErrors,
        filamentSlots = status.filamentSlots,
    )
}
