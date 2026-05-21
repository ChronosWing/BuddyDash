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
    val activityKind: PrinterActivityKind,
    val progressCompact: String?,
    val plateKind: PlateIndicatorKind?,
    val currentActivity: String,
    val plateStatus: String?,
    val lastPrintResult: String?,
    val showLastPrint: Boolean,
    val printHeadline: String?,
    val progressFraction: Float?,
    val fileLine: String?,
    val etaLine: String?,
    val tempsLine: String?,
    val nozzleTemp: String,
    val bedTemp: String,
    val filamentSlots: List<FilamentSlot>,
    val activeFilamentSlot: SlotInventoryKey? = null,
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
            activityKind = PrinterActivityKind.Offline,
            progressCompact = null,
            plateKind = null,
            currentActivity = "—",
            plateStatus = null,
            lastPrintResult = null,
            showLastPrint = false,
            printHeadline = null,
            progressFraction = null,
            fileLine = null,
            etaLine = null,
            tempsLine = null,
            nozzleTemp = "—",
            bedTemp = "—",
            filamentSlots = emptyList(),
            activeFilamentSlot = null,
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
    val activityKind = status.resolveActivityKind()
    val progressCompact = activityKind.progressSuffix(status.progress)
    val plateKind = status.resolvePlateKind()
    val etaFormatted = formatEta(status.remainingTimeSeconds)
    val showEta = isActivePrint && etaFormatted != null

    return PrinterCardLabels(
        title = title,
        subtitle = subtitle,
        connection = if (status.connected) "Connected" else "Offline",
        isConnected = status.connected,
        isActivePrint = isActivePrint,
        activityKind = activityKind,
        progressCompact = progressCompact,
        plateKind = plateKind,
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
        tempsLine = if (status.showHomeCardTemps()) {
            formatPrintTempsLine(status.nozzleTemp, status.bedTemp)
        } else {
            null
        },
        nozzleTemp = detail.nozzleTemp,
        bedTemp = detail.bedTemp,
        filamentSlots = status.filamentSlots,
        activeFilamentSlot = status.activeFilamentSlot,
    )
}
