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

/** Seconds since last successful status refresh; null if never updated. */
fun statusRefreshAgeSeconds(
    updatedAtMillis: Long?,
    nowMillis: Long = System.currentTimeMillis(),
): Long? {
    if (updatedAtMillis == null) return null
    return ((nowMillis - updatedAtMillis) / 1000L).coerceAtLeast(0L)
}

enum class StatusRefreshFreshness {
    /** 0–10s — live telemetry. */
    Live,
    /** 10–30s — slightly stale. */
    Aging,
    /** 30–60s — stale connection. */
    Stale,
    /** 60s+ — strongly stale. */
    ConnectionStale,
}

fun resolveStatusRefreshFreshness(
    updatedAtMillis: Long?,
    nowMillis: Long = System.currentTimeMillis(),
): StatusRefreshFreshness? {
    val seconds = statusRefreshAgeSeconds(updatedAtMillis, nowMillis) ?: return null
    return when {
        seconds < 10L -> StatusRefreshFreshness.Live
        seconds < 30L -> StatusRefreshFreshness.Aging
        seconds < 60L -> StatusRefreshFreshness.Stale
        else -> StatusRefreshFreshness.ConnectionStale
    }
}

/** Relative time since last successful status refresh, e.g. "Just now", "4s ago", "1m ago". */
fun formatStatusUpdatedAgo(updatedAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String? {
    val seconds = statusRefreshAgeSeconds(updatedAtMillis, nowMillis) ?: return null
    return when {
        seconds < 3L -> "Just now"
        seconds < 60L -> "${seconds}s ago"
        seconds < 3_600L -> "${seconds / 60L}m ago"
        else -> "${seconds / 3_600L}h ago"
    }
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

fun formatHmsSummary(severity: HmsSeverity): String = when (severity) {
    HmsSeverity.Ok -> "HMS OK"
    HmsSeverity.Warning -> "HMS Warning"
    HmsSeverity.Error -> "HMS Error"
    HmsSeverity.Unknown -> "HMS Unknown"
}

@Deprecated("Pass HmsSeverity instead of error count", ReplaceWith("formatHmsSummary(severity)"))
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

fun formatHmsHealth(severity: HmsSeverity): String = when (severity) {
    HmsSeverity.Ok -> "OK"
    HmsSeverity.Warning -> "Warning"
    HmsSeverity.Error -> "Error"
    HmsSeverity.Unknown -> "Unknown"
}

@Deprecated("Pass HmsSeverity instead of error count", ReplaceWith("formatHmsHealth(severity)"))
fun formatHmsHealth(errorCount: Int): String =
    if (errorCount == 0) "OK" else "$errorCount active error${if (errorCount == 1) "" else "s"}"
