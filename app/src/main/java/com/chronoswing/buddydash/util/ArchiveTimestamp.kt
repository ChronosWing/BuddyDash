package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrintArchive
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Temporary: stats date-field discovery. Set false before release. */
const val DEBUG_LOG_ARCHIVE_STATS_DATE = true

const val TAG_ARCHIVE_STATS_DATE = "BuddyDash/ArchiveStatsDate"

/** JSON keys in priority order for stats completion time (snake_case API). */
val ARCHIVE_STATS_COMPLETION_FIELD_KEYS = listOf(
    "completed_at",
    "finished_at",
    "end_time",
    "updated_at",
    "created_at",
)

/** Extra keys logged for discovery only (not used for filtering priority). */
val ARCHIVE_STATS_DATE_DISCOVERY_KEYS = listOf(
    "completed_at",
    "completedAt",
    "finished_at",
    "finishedAt",
    "end_time",
    "endTime",
    "updated_at",
    "updatedAt",
    "created_at",
    "createdAt",
    "started_at",
    "startedAt",
    "timestamp",
    "date",
)

fun resolveArchiveStatsCompletedMillis(json: JSONObject): Long? =
    resolveArchiveStatsInstantFromJson(json)?.toEpochMilli()

fun resolveArchiveStatsInstantFromJson(json: JSONObject): Instant? {
    for (key in ARCHIVE_STATS_COMPLETION_FIELD_KEYS) {
        // opt() only — json.has() is not available in JVM unit tests (Android stub).
        parseArchiveTimestampValue(json.opt(key))?.let { return it }
    }
    return null
}

fun resolveArchiveStatsInstant(archive: PrintArchive): Instant? {
    archive.statsCompletedAtMillis?.let { return Instant.ofEpochMilli(it) }
    return parseArchiveTimestamp(archive.completedAtIso)
        ?: parseArchiveTimestamp(archive.createdAtIso)
}

fun archiveStatsRangeCutoff(
    range: ArchiveStatsTimeRange,
    zone: ZoneId = ZoneId.systemDefault(),
    now: ZonedDateTime = ZonedDateTime.now(zone),
): Instant? = when (range) {
    ArchiveStatsTimeRange.AllTime -> null
    ArchiveStatsTimeRange.Last7Days -> now.minusDays(7).toInstant()
    ArchiveStatsTimeRange.Last30Days -> now.minusDays(30).toInstant()
}

fun archivePassesStatsRange(
    archive: PrintArchive,
    range: ArchiveStatsTimeRange,
    zone: ZoneId = ZoneId.systemDefault(),
    now: ZonedDateTime = ZonedDateTime.now(zone),
): Boolean {
    if (range == ArchiveStatsTimeRange.AllTime) return true
    val cutoff = archiveStatsRangeCutoff(range, zone, now) ?: return true
    val completed = resolveArchiveStatsInstant(archive) ?: return false
    return !completed.isBefore(cutoff)
}

fun archiveDateFieldsDiscovery(json: JSONObject): Map<String, String?> =
    ARCHIVE_STATS_DATE_DISCOVERY_KEYS.associateWith { key ->
        val value = json.opt(key)
        if (value == null || value == JSONObject.NULL) null else value.toString()
    }

fun logArchiveStatsDateFilterSummary(
    archives: List<PrintArchive>,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    if (!DEBUG_LOG_ARCHIVE_STATS_DATE) return
    val now = ZonedDateTime.now(zone)
    val parsed = archives.count { resolveArchiveStatsInstant(it) != null }
    val in7d = archives.count { archivePassesStatsRange(it, ArchiveStatsTimeRange.Last7Days, zone, now) }
    val in30d = archives.count { archivePassesStatsRange(it, ArchiveStatsTimeRange.Last30Days, zone, now) }
    Log.d(
        TAG_ARCHIVE_STATS_DATE,
        "summary total=${archives.size} parsedDates=$parsed in7d=$in7d in30d=$in30d allTime=${archives.size} " +
            "cutoff7d=${archiveStatsRangeCutoff(ArchiveStatsTimeRange.Last7Days, zone, now)} " +
            "cutoff30d=${archiveStatsRangeCutoff(ArchiveStatsTimeRange.Last30Days, zone, now)} " +
            "zone=$zone now=$now",
    )
}

fun logArchiveStatsDateItemDebug(
    displayName: String,
    json: JSONObject,
    archive: PrintArchive,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    if (!DEBUG_LOG_ARCHIVE_STATS_DATE) return
    val now = ZonedDateTime.now(zone)
    val fields = archiveDateFieldsDiscovery(json)
    val selected = resolveArchiveStatsInstant(archive)
    val passes7 = archivePassesStatsRange(archive, ArchiveStatsTimeRange.Last7Days, zone, now)
    val passes30 = archivePassesStatsRange(archive, ArchiveStatsTimeRange.Last30Days, zone, now)
    Log.d(
        TAG_ARCHIVE_STATS_DATE,
        "item name=$displayName fields=$fields selectedInstant=$selected " +
            "statsCompletedAtMillis=${archive.statsCompletedAtMillis} passes7d=$passes7 passes30d=$passes30",
    )
}

/**
 * Parse API date/time values for stats filtering.
 * Supports ISO-8601 (Z or offset), local ISO without zone (system default), epoch sec/ms.
 */
fun parseArchiveTimestamp(raw: String?): Instant? {
    if (raw.isNullOrBlank() || raw.equals("null", ignoreCase = true)) return null
    val trimmed = raw.trim()
    trimmed.toLongOrNull()?.let { return epochNumberToInstant(it) }
    parseInstantIso(trimmed)?.let { return it }
    parseOffsetDateTime(trimmed)?.let { return it }
    parseLocalDateTime(trimmed)?.let { return it }
    if (trimmed.contains(' ')) {
        parseLocalDateTime(trimmed.replace(' ', 'T'))?.let { return it }
    }
    return null
}

private fun parseArchiveTimestampValue(value: Any?): Instant? = when (value) {
    null -> null
    is Number -> epochNumberToInstant(value.toLong())
    is String -> parseArchiveTimestamp(value)
    else -> parseArchiveTimestamp(value.toString())
}

private fun epochNumberToInstant(value: Long): Instant? {
    if (value <= 0L) return null
    return if (value < 1_000_000_000_000L) {
        Instant.ofEpochSecond(value)
    } else {
        Instant.ofEpochMilli(value)
    }
}

private fun parseInstantIso(text: String): Instant? =
    try {
        Instant.parse(text)
    } catch (_: DateTimeParseException) {
        null
    }

private fun parseOffsetDateTime(text: String): Instant? =
    try {
        OffsetDateTime.parse(text).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }

private fun parseLocalDateTime(text: String): Instant? {
    val formatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    )
    for (formatter in formatters) {
        try {
            val ldt = LocalDateTime.parse(text, formatter)
            return ldt.atZone(ZoneId.systemDefault()).toInstant()
        } catch (_: DateTimeParseException) {
            // try next
        }
    }
    return null
}
