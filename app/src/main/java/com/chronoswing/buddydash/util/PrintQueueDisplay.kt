package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.network.BambuddyApi
import org.json.JSONObject

/** Shown when no queue item name field has a usable value. */
const val QUEUE_DISPLAY_NAME_FALLBACK = "Unnamed print"

/** Max upcoming jobs shown on printer Detail Status tab. */
const val DETAIL_QUEUE_VISIBLE_LIMIT = 3

/** Keys tried in order for queue job display name (Bambuddy + common aliases). */
val QUEUE_DISPLAY_NAME_KEYS = listOf(
    "library_file_name",
    "archive_name",
    "name",
    "title",
    "file_name",
    "filename",
    "file",
    "task_name",
    "project_name",
    "plate_name",
    "gcode_file",
    "source_file",
    "print_name",
    "batch_name",
)

/** Raw JSON keys logged for discovery (not used as image URLs). */
val QUEUE_THUMBNAIL_HINT_KEYS = listOf(
    "library_file_thumbnail",
    "archive_thumbnail",
)

val QUEUE_IMAGE_ID_KEYS = listOf(
    "id",
    "library_file_id",
    "archive_id",
    "plate_id",
    "project_id",
    "task_id",
    "file_id",
    "queue_id",
    "job_id",
    "printer_id",
)

val QUEUE_DURATION_KEYS = listOf(
    "print_time_seconds",
    "estimated_time_seconds",
    "duration_seconds",
    "remaining_time_seconds",
)

/** Re-export for queue parsing / debug logs. */
val QUEUE_FILAMENT_USAGE_WEIGHT_KEYS = FILAMENT_USAGE_WEIGHT_GRAMS_KEYS

val QUEUE_FILAMENT_USAGE_LENGTH_KEYS = FILAMENT_USAGE_LENGTH_METERS_KEYS

/** Matches Bambuddy web queue thumbnail routing (QueuePage.tsx). */
enum class QueueThumbnailSource {
    LIBRARY_PLATE,
    LIBRARY,
    ARCHIVE_PLATE,
    ARCHIVE,
    NONE,
}

data class QueueThumbnailResolution(
    val source: QueueThumbnailSource,
    val apiPath: String?,
)

fun isUsableQueueFieldValue(raw: String?): Boolean {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return false
    if (trimmed.equals("null", ignoreCase = true)) return false
    if (trimmed.equals("undefined", ignoreCase = true)) return false
    return true
}

fun normalizeQueueFieldValue(key: String, raw: String?): String? {
    if (!isUsableQueueFieldValue(raw)) return null
    val trimmed = raw!!.trim()
    return when (key) {
        "gcode_file", "source_file", "file" ->
            trimmed.substringAfterLast('/').trim().takeIf { isUsableQueueFieldValue(it) }
        else -> trimmed
    }
}

fun queueJsonStringField(json: JSONObject, key: String): String? {
    if (!json.has(key) || json.isNull(key)) return null
    val raw = when (val value = json.opt(key)) {
        is String -> value
        is Number, is Boolean -> value.toString()
        else -> return null
    }
    return normalizeQueueFieldValue(key, raw)
}

fun resolveQueueDisplayName(json: JSONObject): String {
    for (key in QUEUE_DISPLAY_NAME_KEYS) {
        queueJsonStringField(json, key)?.let { return it }
    }
    return QUEUE_DISPLAY_NAME_FALLBACK
}

fun resolveQueueDurationSeconds(json: JSONObject): Int? {
    for (key in QUEUE_DURATION_KEYS) {
        if (!json.has(key) || json.isNull(key)) continue
        val seconds = json.optInt(key, -1).takeIf { it > 0 }
            ?: json.optLong(key, -1L).takeIf { it > 0L }?.toInt()
        if (seconds != null && seconds > 0) return seconds
    }
    return null
}

/**
 * Bambuddy stores filesystem paths in `archive_thumbnail` / `library_file_thumbnail`;
 * the web UI only uses them as a “has thumbnail” flag, then loads via API routes + stream token.
 */
fun queueHasThumbnailHint(json: JSONObject, field: String): Boolean {
    if (!json.has(field) || json.isNull(field)) return false
    return when (val value = json.opt(field)) {
        is String -> isUsableQueueFieldValue(value)
        else -> false
    }
}

/** Bambuddy `plate_id` is 1-based (Metadata/plate_{N}.png). */
fun queueJsonPlateId(json: JSONObject): Int? {
    if (!json.has("plate_id") || json.isNull("plate_id")) return null
    return json.optInt("plate_id", -1).takeIf { it > 0 }
}

fun queueJsonPositiveInt(json: JSONObject, key: String): Int? {
    if (!json.has(key) || json.isNull(key)) return null
    val value = json.optInt(key, -1)
    return value.takeIf { it > 0 }
}

fun queueNameFieldCandidates(json: JSONObject): Map<String, String?> =
    QUEUE_DISPLAY_NAME_KEYS.associateWith { queueJsonStringField(json, it) }

fun queueThumbnailHintCandidates(json: JSONObject): Map<String, String?> =
    QUEUE_THUMBNAIL_HINT_KEYS.associateWith { key ->
        if (!json.has(key) || json.isNull(key)) null else json.optString(key, "")
    }

fun queueImageIdFieldCandidates(json: JSONObject): Map<String, String?> =
    QUEUE_IMAGE_ID_KEYS.associateWith { key ->
        if (!json.has(key) || json.isNull(key)) null else json.opt(key)?.toString()
    }

fun queueDurationFieldCandidates(json: JSONObject): Map<String, String?> =
    QUEUE_DURATION_KEYS.associateWith { key ->
        if (!json.has(key) || json.isNull(key)) null else json.opt(key)?.toString()
    }

fun queueFilamentUsageFieldCandidates(json: JSONObject): Map<String, String?> =
    filamentUsageWeightFieldCandidates(json) + filamentUsageLengthFieldCandidates(json)

fun resolveQueueFilamentUsage(json: JSONObject): FilamentUsage? =
    resolveFilamentUsageFromJson(json)

/** Duration + filament on one line, e.g. "44m • 🧵 12g". */
fun formatQueueDurationAndFilament(durationSeconds: Int?, usage: FilamentUsage?): String? {
    val duration = formatQueueDuration(durationSeconds)
    val filament = formatFilamentUsageCompact(usage)
    return when {
        duration != null && filament != null -> "$duration • $filament"
        duration != null -> duration
        filament != null -> filament
        else -> null
    }
}

/** Same routing as Bambuddy web `SortableQueueItem` in QueuePage.tsx. */
fun resolveQueueThumbnailSource(job: PrintQueueJob): QueueThumbnailResolution {
    val plateId = job.plateId
    when {
        job.hasLibraryThumbnail && job.libraryFileId != null -> {
            val fileId = job.libraryFileId
            return if (plateId != null) {
                QueueThumbnailResolution(
                    source = QueueThumbnailSource.LIBRARY_PLATE,
                    apiPath = BambuddyApi.libraryFilePlateThumbnailPath(fileId, plateId),
                )
            } else {
                QueueThumbnailResolution(
                    source = QueueThumbnailSource.LIBRARY,
                    apiPath = BambuddyApi.libraryFileThumbnailPath(fileId),
                )
            }
        }
        job.hasArchiveThumbnail && job.archiveId != null -> {
            val archiveId = job.archiveId
            return if (plateId != null) {
                QueueThumbnailResolution(
                    source = QueueThumbnailSource.ARCHIVE_PLATE,
                    apiPath = BambuddyApi.archivePlateThumbnailPath(archiveId, plateId),
                )
            } else {
                QueueThumbnailResolution(
                    source = QueueThumbnailSource.ARCHIVE,
                    apiPath = BambuddyApi.archiveThumbnailPath(archiveId),
                )
            }
        }
        job.libraryFileId != null -> {
            val fileId = job.libraryFileId
            return if (plateId != null) {
                QueueThumbnailResolution(
                    source = QueueThumbnailSource.LIBRARY_PLATE,
                    apiPath = BambuddyApi.libraryFilePlateThumbnailPath(fileId, plateId),
                )
            } else {
                QueueThumbnailResolution(
                    source = QueueThumbnailSource.LIBRARY,
                    apiPath = BambuddyApi.libraryFileThumbnailPath(fileId),
                )
            }
        }
        job.archiveId != null -> {
            val archiveId = job.archiveId
            return if (plateId != null) {
                QueueThumbnailResolution(
                    source = QueueThumbnailSource.ARCHIVE_PLATE,
                    apiPath = BambuddyApi.archivePlateThumbnailPath(archiveId, plateId),
                )
            } else {
                QueueThumbnailResolution(
                    source = QueueThumbnailSource.ARCHIVE,
                    apiPath = BambuddyApi.archiveThumbnailPath(archiveId),
                )
            }
        }
    }
    return QueueThumbnailResolution(QueueThumbnailSource.NONE, null)
}

/** Estimated print duration from Bambuddy `print_time_seconds` when present. */
fun formatQueueDuration(seconds: Int?): String? {
    if (seconds == null || seconds <= 0) return null
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
