package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolUsageDirectIds
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import org.json.JSONArray
import org.json.JSONObject

/** Temporary: spool usage history diagnostics. Set false before release. */
val DEBUG_LOG_SPOOL_USAGE: Boolean get() = BuddyDashDebug.enabled

const val TAG_SPOOL_USAGE = "BuddyDash/SpoolUsage"

fun parseSpoolUsageHistoryList(body: String): List<SpoolUsageEntry> {
    val array = JSONArray(body)
    return buildList {
        for (i in 0 until array.length()) {
            parseSpoolUsageEntry(array.optJSONObject(i) ?: continue)?.let { add(it) }
        }
    }.sortedByDescending { it.createdAtIso }
}

private fun parseSpoolUsageEntry(json: JSONObject): SpoolUsageEntry? {
    val id = json.optInt("id", -1)
    val spoolId = json.optInt("spool_id", -1)
    if (id < 0 || spoolId < 0) return null
    val weightUsed = jsonPositiveDouble(json, "weight_used") ?: return null
    val createdAt = spoolJsonOptionalString(json, "created_at") ?: return null
    return SpoolUsageEntry(
        id = id,
        spoolId = spoolId,
        printerId = json.optInt("printer_id", -1).takeIf { it >= 0 },
        printName = spoolJsonOptionalString(json, "print_name"),
        weightUsedGrams = weightUsed,
        percentUsed = json.optInt("percent_used", -1).takeIf { it >= 0 },
        status = spoolJsonOptionalString(json, "status") ?: "unknown",
        createdAtIso = createdAt,
        directIds = resolveSpoolUsageDirectIds(json),
        plateNumber = resolveSpoolUsagePlateNumber(json),
        rawJson = json.toString(),
    )
}

/** Read optional direct IDs from usage JSON or nested [extra_data] (runtime only). */
fun resolveSpoolUsageDirectIds(json: JSONObject): SpoolUsageDirectIds {
    fun intField(vararg keys: String): Int? {
        for (key in keys) {
            spoolJsonPositiveInt(json, key)?.let { return it }
        }
        return null
    }
    val fromRoot = SpoolUsageDirectIds(
        archiveId = intField("archive_id", "archiveId", "print_archive_id", "printArchiveId"),
        historyId = intField("history_id", "historyId"),
        jobId = intField("job_id", "jobId"),
        taskId = intField("task_id", "taskId"),
        fileId = intField("file_id", "fileId"),
        printId = intField("print_id", "printId"),
    )
    val extra = json.optJSONObject("extra_data") ?: return fromRoot
    return SpoolUsageDirectIds(
        archiveId = fromRoot.archiveId
            ?: spoolJsonPositiveInt(extra, "archive_id", "archiveId", "print_archive_id"),
        historyId = fromRoot.historyId
            ?: spoolJsonPositiveInt(extra, "history_id", "historyId"),
        jobId = fromRoot.jobId ?: spoolJsonPositiveInt(extra, "job_id", "jobId"),
        taskId = fromRoot.taskId ?: spoolJsonPositiveInt(extra, "task_id", "taskId"),
        fileId = fromRoot.fileId ?: spoolJsonPositiveInt(extra, "file_id", "fileId"),
        printId = fromRoot.printId ?: spoolJsonPositiveInt(extra, "print_id", "printId"),
    )
}

private fun resolveSpoolUsagePlateNumber(json: JSONObject): Int? {
    spoolJsonPositiveInt(json, "plate_number", "plate_id", "plateNumber")?.let { return it }
    val extra = json.optJSONObject("extra_data") ?: return null
    return spoolJsonPositiveInt(extra, "plate_number", "plate_id")
}

private fun spoolJsonPositiveInt(json: JSONObject, vararg keys: String): Int? {
    for (key in keys) {
        if (!json.has(key) || json.isNull(key)) continue
        val value = json.optInt(key, -1)
        if (value >= 0) return value
    }
    return null
}

enum class SpoolUsageThumbnailSource {
    Archive,
    FileIcon,
}

data class SpoolUsageDisplayItem(
    val entry: SpoolUsageEntry,
    val archiveId: Int?,
    val linkKind: SpoolUsageArchiveLinkKind,
    val thumbnailSource: SpoolUsageThumbnailSource,
    val isTappable: Boolean,
    val displayName: String,
    val weightLine: String,
    val printerLine: String?,
    val linkResult: SpoolUsageArchiveLinkResult,
)

fun buildSpoolUsageDisplayItems(
    entries: List<SpoolUsageEntry>,
    archives: List<PrintArchive>,
    printerNamesById: Map<Int, String>,
    spoolMaterial: String? = null,
    spoolColorName: String? = null,
): List<SpoolUsageDisplayItem> =
    entries.map { entry ->
        val link = resolveSpoolUsageArchiveLink(
            entry = entry,
            archives = archives,
            spoolMaterial = spoolMaterial,
            spoolColorName = spoolColorName,
        )
        val archiveId = link.archiveId
        SpoolUsageDisplayItem(
            entry = entry,
            archiveId = archiveId,
            linkKind = link.kind,
            thumbnailSource = if (archiveId != null) {
                SpoolUsageThumbnailSource.Archive
            } else {
                SpoolUsageThumbnailSource.FileIcon
            },
            isTappable = archiveId != null,
            displayName = formatSpoolUsagePrintName(entry),
            weightLine = formatSpoolUsageWeightUsedLine(entry),
            printerLine = formatSpoolUsagePrinterLine(entry, printerNamesById),
            linkResult = link,
        )
    }

/** @deprecated Use [resolveSpoolUsageArchiveLink]. */
fun matchArchiveForSpoolUsage(
    entry: SpoolUsageEntry,
    archives: List<PrintArchive>,
    printerNamesById: Map<Int, String> = emptyMap(),
    spoolMaterial: String? = null,
    spoolColorName: String? = null,
): Int? = resolveSpoolUsageArchiveLink(
    entry = entry,
    archives = archives,
    spoolMaterial = spoolMaterial,
    spoolColorName = spoolColorName,
).archiveId

fun formatSpoolUsagePrintName(entry: SpoolUsageEntry): String {
    val raw = entry.printName?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    return if (raw != null) formatFilenameForDisplay(raw) else "Print #${entry.id}"
}

fun formatSpoolUsageWeight(entry: SpoolUsageEntry): String =
    formatFilamentWeightGrams(entry.weightUsedGrams)

fun formatSpoolUsageWeightUsedLine(entry: SpoolUsageEntry): String =
    "${formatFilamentWeightGrams(entry.weightUsedGrams)} used"

fun formatSpoolUsageDate(entry: SpoolUsageEntry): String? =
    formatSpoolLastUsed(entry.createdAtIso)

fun formatSpoolUsagePrinterLine(
    entry: SpoolUsageEntry,
    printerNamesById: Map<Int, String>,
): String? {
    val printerId = entry.printerId ?: return null
    return printerNamesById[printerId] ?: "Printer $printerId"
}

fun logSpoolUsageFetch(
    spoolId: Int,
    path: String,
    rawBody: String?,
    entries: List<SpoolUsageEntry>,
    error: Throwable? = null,
) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    if (error != null) {
        Log.e(
            TAG_SPOOL_USAGE,
            "spoolUsage spoolId=$spoolId endpoint=$path failed: ${error.message}",
            error,
        )
        return
    }
    Log.d(TAG_SPOOL_USAGE, "spoolUsage spoolId=$spoolId endpoint=$path count=${entries.size}")
    if (rawBody != null) {
        logFullJsonPayload(TAG_SPOOL_USAGE, "spoolUsageListRaw", rawBody)
    }
    entries.forEach { entry ->
        Log.d(TAG_SPOOL_USAGE, "--- spool usage history item usageHistoryId=${entry.id} ---")
        logFullJsonPayload(TAG_SPOOL_USAGE, "usageItemRaw", entry.rawJson)
    }
}

fun logSpoolUsageDisplayItems(spoolId: Int, items: List<SpoolUsageDisplayItem>) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    items.forEach { item ->
        val link = item.linkResult
        Log.d(
            TAG_SPOOL_USAGE,
            "spoolUsageRow spoolId=$spoolId usageHistoryId=${item.entry.id} " +
                "archiveId=${link.archiveId} linkKind=${link.kind} reason=${link.reason} " +
                "tappable=${item.isTappable} thumbnail=${item.thumbnailSource}",
        )
    }
}

/** @deprecated Use [resolveSpoolUsageDirectIds]. */
fun resolveSpoolUsageArchiveId(json: JSONObject): Int? =
    resolveSpoolUsageDirectIds(json).archiveId

private fun jsonPositiveDouble(json: JSONObject, key: String): Double? {
    if (!json.has(key) || json.isNull(key)) return null
    val parsed = json.optDouble(key, Double.NaN)
    if (!parsed.isNaN() && parsed >= 0.0) return parsed
    return json.optString(key).trim().toDoubleOrNull()?.takeIf { it >= 0.0 }
}
