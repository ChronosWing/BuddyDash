package com.chronoswing.buddydash.util

fun formatEta(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "—"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${secs}s"
    }
}

fun formatTemp(value: Double?): String {
    if (value == null) return "—"
    return if (value % 1.0 == 0.0) {
        "${value.toInt()}°C"
    } else {
        String.format("%.1f°C", value)
    }
}

fun formatTempShort(value: Double?): String {
    if (value == null) return "—"
    return "${value.toInt()}°"
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
