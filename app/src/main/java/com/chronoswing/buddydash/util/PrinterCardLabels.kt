package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus

data class PrinterCardLabels(
    val title: String,
    val subtitle: String?,
    val connection: String,
    val isConnected: Boolean,
    val isActivePrint: Boolean,
    val currentActivity: String,
    val plateStatus: String?,
    val lastPrintResult: String?,
    val showLastPrint: Boolean,
    val printHeadline: String?,
    val progressFraction: Float?,
    val fileLine: String?,
    val etaLine: String?,
    val tempsLine: String?,
    val hmsSummary: String,
    val hmsHasErrors: Boolean,
    val nozzleTemp: String,
    val bedTemp: String,
    val hmsHealth: String,
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
            isActivePrint = false,
            currentActivity = "—",
            plateStatus = null,
            lastPrintResult = null,
            showLastPrint = false,
            printHeadline = null,
            progressFraction = null,
            fileLine = null,
            etaLine = null,
            tempsLine = null,
            hmsSummary = "—",
            hmsHasErrors = false,
            nozzleTemp = "—",
            bedTemp = "—",
            hmsHealth = "—",
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

    val progressText = if (isActivePrint) formatProgress(status.progress) else null
    val etaFormatted = formatEta(status.remainingTimeSeconds)
    val showEta = isActivePrint && etaFormatted != "—"

    return PrinterCardLabels(
        title = title,
        subtitle = subtitle,
        connection = if (status.connected) "Connected" else "Offline",
        isConnected = status.connected,
        isActivePrint = isActivePrint,
        currentActivity = detail.currentActivity,
        plateStatus = detail.plateStatus?.let { plate ->
            when (plate) {
                "Clear" -> "Plate clear"
                "Not clear" -> "Plate not clear"
                else -> plate
            }
        },
        lastPrintResult = detail.lastPrintResult,
        showLastPrint = !isActivePrint && detail.showLastPrintOnCard,
        printHeadline = if (isActivePrint) {
            buildPrintHeadline(detail.currentActivity, progressText)
        } else {
            null
        },
        progressFraction = if (isActivePrint) {
            (status.progress ?: 0f).coerceIn(0f, 100f) / 100f
        } else {
            null
        },
        fileLine = fileLine,
        etaLine = if (showEta) "ETA $etaFormatted" else null,
        tempsLine = if (isActivePrint) {
            formatPrintTempsLine(status.nozzleTemp, status.bedTemp)
        } else {
            null
        },
        hmsSummary = formatHmsSummary(status.hmsErrorCount),
        hmsHasErrors = detail.hmsHasErrors,
        nozzleTemp = detail.nozzleTemp,
        bedTemp = detail.bedTemp,
        hmsHealth = detail.hmsHealth,
        filamentSlots = status.filamentSlots,
    )
}
