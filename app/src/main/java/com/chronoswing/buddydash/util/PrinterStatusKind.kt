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
    InUse,
    Clear,
    NotClear,
}

fun PrinterStatus.resolveActivityKind(): PrinterActivityKind {
    if (!connected) return PrinterActivityKind.Offline
    if (hasActiveFault()) return PrinterActivityKind.Error
    val raw = rawState?.uppercase()
    if (raw == "RUNNING") return PrinterActivityKind.Printing
    if (raw == "PAUSE") return PrinterActivityKind.Paused
    if (raw in IDLE_LIKE_STATES || raw.isNullOrBlank()) return PrinterActivityKind.Idle
    if (raw in BUSY_LIKE_STATES || raw in HOMING_LIKE_STATES) return PrinterActivityKind.Busy
    return PrinterActivityKind.Busy
}

fun PrinterStatus.resolvePlateKind(): PlateIndicatorKind? {
    if (!connected) return null
    val raw = rawState?.uppercase()
    if (raw == "RUNNING" || raw == "PAUSE") {
        return PlateIndicatorKind.InUse
    }
    return awaitingPlateClear?.let { awaiting ->
        if (awaiting) PlateIndicatorKind.NotClear else PlateIndicatorKind.Clear
    }
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

private val HOMING_LIKE_STATES = setOf(
    "HOMING", "HOME", "AUTO_HOME", "AUTOHOMING", "G28", "G28ING",
)
