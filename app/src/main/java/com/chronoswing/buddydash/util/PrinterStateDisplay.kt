package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus

/** Separates live printer activity from the previous job outcome. */
data class PrinterStateDisplay(
    val currentActivity: String,
    val lastPrintResult: String?,
    val showLastPrintOnCard: Boolean,
)

private val IDLE_LIKE_STATES = setOf("IDLE", "FINISH", "FAILED")
private val BUSY_LIKE_STATES = setOf(
    "PREPARE", "PREPARING", "SLICING", "CALIBRATING", "BUSY", "INITIALIZING",
    "HOMING", "HOME", "AUTO_HOME", "AUTOHOMING",
)

fun PrinterStatus.toStateDisplay(): PrinterStateDisplay {
    val raw = rawState?.uppercase()
    val isPrinting = raw == "RUNNING"
    val isPaused = raw == "PAUSE"
    val isActivePrint = isPrinting || isPaused

    val lastPrintResult = resolveLastPrintResult(raw)
    val currentActivity = resolveCurrentActivity(
        connected = connected,
        raw = raw,
        isPrinting = isPrinting,
        isPaused = isPaused,
        isActivePrint = isActivePrint,
        hasActiveFault = hasActiveFault(),
    )

    val showLastPrintOnCard = when (lastPrintResult) {
        "Failed", "Cancelled" -> true
        "Finished" -> awaitingPlateClear == true
        else -> false
    }

    return PrinterStateDisplay(
        currentActivity = currentActivity,
        lastPrintResult = lastPrintResult,
        showLastPrintOnCard = showLastPrintOnCard,
    )
}

private fun resolveCurrentActivity(
    connected: Boolean,
    raw: String?,
    isPrinting: Boolean,
    isPaused: Boolean,
    isActivePrint: Boolean,
    hasActiveFault: Boolean,
): String {
    if (!connected) return "Offline"
    if (hasActiveFault) return "Error"
    if (isPrinting) return "Printing"
    if (isPaused) return "Paused"
    if (raw in IDLE_LIKE_STATES || raw.isNullOrBlank()) return "Idle"
    if (raw in BUSY_LIKE_STATES) return "Busy"
    return formatStateLabel(raw)
}

private fun formatStateLabel(state: String): String =
    state.lowercase().replaceFirstChar { it.uppercase() }

private fun resolveLastPrintResult(raw: String?): String? = when (raw) {
    "FINISH" -> "Finished"
    "FAILED" -> "Failed"
    "STOP", "STOPPED", "CANCEL", "CANCELLED" -> "Cancelled"
    else -> null
}

fun displayNameAndModel(name: String, model: String?): Pair<String, String?> {
    val trimmedModel = model?.trim()?.takeIf { it.isNotEmpty() }
    if (trimmedModel == null) return name to null
    if (name.trim().equals(trimmedModel, ignoreCase = true)) return name to null
    return name to trimmedModel
}

fun normalizeFilamentType(type: String?): String? {
    val trimmed = type?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (trimmed.equals("null", ignoreCase = true)) return null
    return trimmed
}
