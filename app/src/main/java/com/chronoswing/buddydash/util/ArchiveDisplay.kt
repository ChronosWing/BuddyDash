package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.PrintArchive
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeParseException

const val ARCHIVE_DISPLAY_NAME_FALLBACK = "Unnamed print"

/** Temporary: log archive API field discovery. Set false before release. */
val DEBUG_LOG_ARCHIVES: Boolean get() = BuddyDashDebug.enabled

const val TAG_ARCHIVES = "BuddyDash/Archives"

/** Temporary: archive detail config + field mapping. Set false before release. */
val DEBUG_LOG_ARCHIVE_DETAIL: Boolean get() = BuddyDashDebug.enabled

const val TAG_ARCHIVE_DETAIL = "BuddyDash/ArchiveDetail"

const val ARCHIVES_LIST_DEFAULT_LIMIT = 200

fun resolveArchiveDisplayName(json: JSONObject): String {
    val name = listOf("print_name", "filename", "project_name")
        .mapNotNull { key ->
            json.optString(key).trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        }
        .firstOrNull()
    return name ?: ARCHIVE_DISPLAY_NAME_FALLBACK
}

fun parseArchiveResultKind(statusRaw: String): ArchiveResultKind {
    val normalized = statusRaw.trim().lowercase()
    return when {
        normalized in SUCCESS_STATUS_VALUES -> ArchiveResultKind.Success
        normalized in FAILED_STATUS_VALUES -> ArchiveResultKind.Failed
        normalized in CANCELLED_STATUS_VALUES -> ArchiveResultKind.Cancelled
        normalized.isBlank() -> ArchiveResultKind.Other
        else -> ArchiveResultKind.Other
    }
}

private val SUCCESS_STATUS_VALUES = setOf(
    "success",
    "completed",
    "complete",
    "finished",
    "ok",
)

private val FAILED_STATUS_VALUES = setOf(
    "failed",
    "failure",
    "error",
    "fail",
)

private val CANCELLED_STATUS_VALUES = setOf(
    "cancelled",
    "canceled",
    "stopped",
    "aborted",
)

fun resolveArchiveDurationSeconds(json: JSONObject): Int? {
    val actual = json.optInt("actual_time_seconds", -1).takeIf { it > 0 }
    if (actual != null) return actual
    return json.optInt("print_time_seconds", -1).takeIf { it > 0 }
}

fun parsePrintArchive(
    json: JSONObject,
    printerNamesById: Map<Int, String> = emptyMap(),
    printerModelsById: Map<Int, String> = emptyMap(),
): PrintArchive? {
    val id = json.optInt("id", -1)
    if (id < 0) return null
    val statusRaw = json.optString("status", "")
    val printerId = json.optInt("printer_id", -1).takeIf { it >= 0 }
    val grams = jsonPositiveDouble(json, "filament_used_grams")
    val filamentUsage = grams?.let { FilamentUsage(weightGrams = it) }
    return PrintArchive(
        id = id,
        displayName = resolveArchiveDisplayName(json),
        filename = jsonOptionalString(json, "filename"),
        printerId = printerId,
        printerName = printerId?.let { printerNamesById[it] },
        printerModel = printerId?.let { printerModelsById[it] },
        statusRaw = statusRaw,
        resultKind = parseArchiveResultKind(statusRaw),
        startedAtIso = jsonOptionalString(json, "started_at"),
        completedAtIso = jsonOptionalString(json, "completed_at"),
        createdAtIso = jsonOptionalString(json, "created_at"),
        statsCompletedAtMillis = resolveArchiveStatsCompletedMillis(json),
        durationSeconds = resolveArchiveDurationSeconds(json),
        filamentUsage = filamentUsage,
        filamentType = jsonOptionalString(json, "filament_type"),
        filamentColor = jsonOptionalString(json, "filament_color"),
        spoolId = resolveArchiveSpoolId(json),
        plateNumber = resolveArchivePlateNumber(json),
        contentHash = jsonOptionalString(json, "content_hash"),
        failureReason = jsonOptionalString(json, "failure_reason"),
        totalLayers = json.optInt("total_layers", -1).takeIf { it > 0 },
        quantity = json.optInt("quantity", -1).takeIf { it > 0 },
        projectName = jsonOptionalString(json, "project_name"),
        slicedForModel = jsonOptionalString(json, "sliced_for_model"),
        notes = jsonOptionalString(json, "notes"),
    )
}

private fun jsonOptionalString(json: JSONObject, key: String): String? =
    json.optString(key).trim().takeIf { isMeaningfulArchiveField(it) }

private fun resolveArchivePlateNumber(json: JSONObject): Int? {
    json.optInt("plate_number", -1).takeIf { it >= 0 }?.let { return it }
    json.optInt("plate_id", -1).takeIf { it >= 0 }?.let { return it }
    val extra = json.optJSONObject("extra_data") ?: return null
    return extra.optInt("plate_number", -1).takeIf { it >= 0 }
        ?: extra.optInt("plate_id", -1).takeIf { it >= 0 }
}

/** Non-empty text that is not the literal string "null". */
fun isMeaningfulArchiveField(value: String?): Boolean {
    val trimmed = value?.trim() ?: return false
    return trimmed.isNotBlank() && !trimmed.equals("null", ignoreCase = true)
}

fun shouldShowArchiveFailureReason(archive: PrintArchive): Boolean {
    if (!isMeaningfulArchiveField(archive.failureReason)) return false
    return when (archive.resultKind) {
        ArchiveResultKind.Failed,
        ArchiveResultKind.Cancelled,
        -> true
        ArchiveResultKind.Other -> {
            val status = archive.statusRaw.trim().lowercase()
            status.contains("fail") || status.contains("error") || status.contains("cancel")
        }
        ArchiveResultKind.Success -> false
    }
}

/** Material type label for detail (no raw hex). */
fun formatArchiveDetailMaterialType(archive: PrintArchive): String? {
    val type = archive.filamentType?.trim()?.takeIf { isMeaningfulArchiveField(it) }
    return type?.let { normalizeFilamentType(it)?.uppercase() ?: type.uppercase() }
}

/** Parsed swatch for archive filament color (solid, multi-stop, translucent). */
fun parseArchiveFilamentSwatch(archive: PrintArchive): FilamentSwatchColors? {
    val raw = archive.filamentColor?.trim()?.takeIf { isMeaningfulArchiveField(it) } ?: return null
    val hexes = if (raw.contains(',')) {
        parseExtraColorStops(raw)
    } else {
        listOfNotNull(normalizeInventoryColor(raw))
    }
    if (hexes.isEmpty()) return null
    val alphaSource = raw.split(',').firstOrNull()?.trim()
    val alpha = parseRgbaAlpha(alphaSource)
    return FilamentSwatchColors(
        colorHexes = hexes,
        isTranslucent = isTranslucentEffect(null, alpha),
        alpha = alpha,
    )
}

fun archiveFilamentColorHexes(archive: PrintArchive): List<String> =
    parseArchiveFilamentSwatch(archive)?.colorHexes.orEmpty()

fun archiveHasMaterialDisplay(archive: PrintArchive): Boolean =
    formatArchiveDetailMaterialType(archive) != null || parseArchiveFilamentSwatch(archive) != null

fun logArchiveDetailFieldMapping(archive: PrintArchive) {
    if (!DEBUG_LOG_ARCHIVE_DETAIL) return
    Log.d(
        TAG_ARCHIVE_DETAIL,
        "detailFields id=${archive.id} failure=${archive.failureReason} project=${archive.projectName} " +
            "notes=${archive.notes} material=${archive.filamentType}/${archive.filamentColor} " +
            "spoolId=${archive.spoolId} showFailure=${shouldShowArchiveFailureReason(archive)}",
    )
}

private fun jsonPositiveDouble(json: JSONObject, key: String): Double? {
    if (!json.has(key) || json.isNull(key)) return null
    return when (val value = json.opt(key)) {
        is Number -> value.toDouble().takeIf { it > 0.0 }
        is String -> value.toDoubleOrNull()?.takeIf { it > 0.0 }
        else -> null
    }
}

fun archiveDisplayFieldsSample(json: JSONObject): Map<String, String?> {
    val keys = listOf(
        "id", "print_name", "filename", "status", "printer_id", "failure_reason",
        "started_at", "completed_at", "created_at", "updated_at", "actual_time_seconds", "print_time_seconds",
        "filament_used_grams", "filament_type", "filament_color", "total_layers",
        "quantity", "project_name", "sliced_for_model", "thumbnail_path",
    )
    return keys.associateWith { key ->
        if (!json.has(key) || json.isNull(key)) null else json.opt(key)?.toString()
    }
}

enum class ArchiveResultFilter {
    All,
    Success,
    Failed,
    Cancelled,
}

data class ArchivePrinterFilter(
    val printerId: Int,
    val printerName: String,
)

fun applyArchiveListFilters(
    archives: List<PrintArchive>,
    query: String,
    filter: ArchiveResultFilter,
    printerId: Int? = null,
): List<PrintArchive> {
    val scoped = if (printerId != null) {
        archives.filter { it.printerId == printerId }
    } else {
        archives
    }
    return applyArchiveSearch(scoped, query, filter)
}

fun applyArchiveSearch(
    archives: List<PrintArchive>,
    query: String,
    filter: ArchiveResultFilter,
): List<PrintArchive> {
    val normalizedQuery = query.trim().lowercase()
    return archives.filter { archive ->
        val matchesFilter = when (filter) {
            ArchiveResultFilter.All -> true
            ArchiveResultFilter.Success -> archive.resultKind == ArchiveResultKind.Success
            ArchiveResultFilter.Failed -> archive.resultKind == ArchiveResultKind.Failed
            ArchiveResultFilter.Cancelled -> archive.resultKind == ArchiveResultKind.Cancelled
        }
        if (!matchesFilter) return@filter false
        if (normalizedQuery.isEmpty()) return@filter true
        val haystack = buildList {
            add(archive.displayName)
            archive.printerName?.let { add(it) }
            archive.printerModel?.let { add(it) }
            archive.filamentType?.let { add(it) }
            archive.filamentColor?.let { add(it) }
            archive.projectName?.let { add(it) }
            archive.statusRaw.let { add(it) }
        }.joinToString(" ").lowercase()
        haystack.contains(normalizedQuery)
    }
}

/** Reuse queue duration formatting for print time. */
fun formatArchiveDuration(seconds: Int?): String? = formatQueueDuration(seconds)

fun formatArchiveFilamentWeight(usage: FilamentUsage?): String? =
    usage?.weightGrams?.let { formatFilamentWeightGrams(it) }

fun formatArchiveCompletedAgo(isoTimestamp: String?, nowMillis: Long = System.currentTimeMillis()): String? {
    val instant = parseIsoInstant(isoTimestamp) ?: return null
    val seconds = ((nowMillis - instant.toEpochMilli()) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 60L -> "Just now"
        seconds < 3_600L -> "${seconds / 60L}m ago"
        seconds < 86_400L -> "${seconds / 3_600L}h ago"
        seconds < 604_800L -> "${seconds / 86_400L}d ago"
        else -> formatArchiveDate(isoTimestamp)
    }
}

fun formatArchiveDate(isoTimestamp: String?): String? {
    val instant = parseIsoInstant(isoTimestamp) ?: return null
    return java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
        .withZone(java.time.ZoneId.systemDefault())
        .format(instant)
}

private fun parseIsoInstant(iso: String?): Instant? {
    if (iso.isNullOrBlank()) return null
    return try {
        Instant.parse(iso)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun formatArchivePrinterLine(archive: PrintArchive): String? {
    val name = archive.printerName?.takeIf { it.isNotBlank() }
    val model = archive.printerModel?.takeIf { it.isNotBlank() }
    return when {
        name != null && model != null -> "$name • $model"
        name != null -> name
        model != null -> model
        archive.printerId != null -> "Printer ${archive.printerId}"
        else -> null
    }
}

fun formatArchiveStatusLabel(kind: ArchiveResultKind, statusRaw: String): String =
    when (kind) {
        ArchiveResultKind.Success -> "Success"
        ArchiveResultKind.Failed -> "Failed"
        ArchiveResultKind.Cancelled -> "Cancelled"
        ArchiveResultKind.Other -> statusRaw.trim().replaceFirstChar { it.uppercase() }
            .takeIf { it.isNotBlank() } ?: "—"
    }

/** List card subtitle: `Success • Printer` */
fun formatArchiveStatusPrinterLine(archive: PrintArchive): String {
    val status = formatArchiveStatusLabel(archive.resultKind, archive.statusRaw)
    val printer = formatArchivePrinterLine(archive)
    return if (printer != null) "$status • $printer" else status
}

/** List card meta: `3h 12m • 184g • 2d ago` */
fun formatArchiveListMetaLine(archive: PrintArchive, nowMillis: Long = System.currentTimeMillis()): String? {
    val parts = buildList {
        formatArchiveDuration(archive.durationSeconds)?.let { add(it) }
        formatArchiveFilamentWeight(archive.filamentUsage)?.let { add(it) }
        formatArchiveCompletedAgo(archive.completedAtIso ?: archive.startedAtIso, nowMillis)?.let { add(it) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

/** List/detail material type label only (no raw hex). */
fun formatArchiveMaterialLine(archive: PrintArchive): String? =
    formatArchiveDetailMaterialType(archive)

fun formatArchivePlateLine(archive: PrintArchive): String? {
    val layers = archive.totalLayers?.let { "$it layers" }
    val qty = archive.quantity?.takeIf { it > 1 }?.let { "Qty $it" }
    return when {
        layers != null && qty != null -> "$layers • $qty"
        layers != null -> layers
        qty != null -> qty
        else -> null
    }
}
