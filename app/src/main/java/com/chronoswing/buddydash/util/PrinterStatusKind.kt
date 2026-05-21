package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus

enum class PrinterActivityKind {
    Idle,
    Printing,
    Paused,
    Error,
    Offline,
    Busy,
}

enum class PlateIndicatorKind {
    Clear,
    NotClear,
}

fun PrinterStatus.resolveActivityKind(): PrinterActivityKind {
    if (!connected) return PrinterActivityKind.Offline
    val raw = rawState?.uppercase()
    if (raw == "RUNNING") {
        return if (hmsErrorCount > 0) PrinterActivityKind.Error else PrinterActivityKind.Printing
    }
    if (raw == "PAUSE") {
        return if (hmsErrorCount > 0) PrinterActivityKind.Error else PrinterActivityKind.Paused
    }
    if (raw in IDLE_LIKE_STATES || raw.isNullOrBlank()) {
        return if (hmsErrorCount > 0) PrinterActivityKind.Error else PrinterActivityKind.Idle
    }
    if (raw in BUSY_LIKE_STATES) {
        return if (hmsErrorCount > 0) PrinterActivityKind.Error else PrinterActivityKind.Busy
    }
    if (hmsErrorCount > 0) return PrinterActivityKind.Error
    return PrinterActivityKind.Busy
}

fun PrinterStatus.resolvePlateKind(): PlateIndicatorKind? =
    awaitingPlateClear?.let { awaiting ->
        if (awaiting) PlateIndicatorKind.NotClear else PlateIndicatorKind.Clear
    }

fun PrinterActivityKind.progressSuffix(progress: Float?): String? {
    if (this != PrinterActivityKind.Printing) return null
    val text = formatProgress(progress)
    return text.takeIf { it != "—" }
}

/** Home cards only — hide idle/ready temps; detail Status tab always shows them. */
fun PrinterStatus.showHomeCardTemps(): Boolean {
    if (!connected) return false
    val raw = rawState?.uppercase() ?: return false
    if (raw in IDLE_LIKE_STATES || raw == "IDLE") return false
    if (raw == "RUNNING" || raw == "PAUSE") return true
    if (raw in BUSY_LIKE_STATES) return true
    if (raw.contains("HEAT") || raw.contains("COOL")) return true
    if (hmsErrorCount > 0) return true
    return false
}

private val IDLE_LIKE_STATES = setOf("IDLE", "FINISH", "FAILED")
private val BUSY_LIKE_STATES = setOf(
    "PREPARE", "PREPARING", "SLICING", "CALIBRATING", "BUSY", "INITIALIZING",
)
