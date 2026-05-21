package com.chronoswing.buddydash.util

import kotlin.math.roundToInt

/** Returns null when ETA should be hidden (unavailable or unreliable). */
fun formatEta(seconds: Int?): String? {
    if (seconds == null || seconds <= 0) return null
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

/** UI display only — rounds to whole degrees. */
fun formatTemp(value: Double?): String {
    if (value == null) return "—"
    return "${value.roundToInt()}°C"
}

/** UI display only — rounds to whole degrees. */
fun formatTempShort(value: Double?): String {
    if (value == null) return "—"
    return "${value.roundToInt()}°"
}

fun buildPrintHeadline(activity: String, progressText: String?): String {
    if (progressText != null && progressText != "—") {
        return "$activity • $progressText"
    }
    return activity
}

fun formatPrintTempsLine(nozzle: Double?, bed: Double?): String {
    val nozzleText = formatTempShort(nozzle)
    val bedText = formatTempShort(bed)
    return "$nozzleText / $bedText"
}

fun formatHmsSummary(errorCount: Int): String =
    if (errorCount == 0) "HMS OK" else "HMS $errorCount"

fun formatProgress(progress: Float?): String {
    if (progress == null) return "—"
    return if (progress % 1f == 0f) {
        "${progress.toInt()}%"
    } else {
        String.format("%.1f%%", progress)
    }
}

fun formatConnection(connected: Boolean): String =
    if (connected) "Connected" else "Disconnected"

fun formatHmsHealth(errorCount: Int): String =
    if (errorCount == 0) "OK" else "$errorCount active error${if (errorCount == 1) "" else "s"}"
