package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import org.json.JSONArray
import org.json.JSONObject

/** Temporary: spool usage history diagnostics. Set false before release. */
const val DEBUG_LOG_SPOOL_USAGE = true // set false before release

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
    )
}

fun formatSpoolUsagePrintName(entry: SpoolUsageEntry): String =
    entry.printName?.trim()?.takeIf { isMeaningfulSpoolField(it) }
        ?: "Print #${entry.id}"

fun formatSpoolUsageWeight(entry: SpoolUsageEntry): String =
    formatFilamentWeightGrams(entry.weightUsedGrams)

fun formatSpoolUsageDate(entry: SpoolUsageEntry): String? =
    formatSpoolLastUsed(entry.createdAtIso)

fun formatSpoolUsagePrinterLine(
    entry: SpoolUsageEntry,
    printerNamesById: Map<Int, String>,
): String? {
    val printerId = entry.printerId ?: return null
    val name = printerNamesById[printerId] ?: "Printer $printerId"
    return name
}

fun logSpoolUsageFetch(
    spoolId: Int,
    path: String,
    rawBodyPreview: String?,
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
    Log.d(
        TAG_SPOOL_USAGE,
        "spoolUsage spoolId=$spoolId endpoint=$path count=${entries.size} " +
            "preview=${rawBodyPreview?.take(200)}",
    )
    entries.take(5).forEach { entry ->
        Log.d(
            TAG_SPOOL_USAGE,
            "spoolUsageItem id=${entry.id} print=${entry.printName} " +
                "weight=${entry.weightUsedGrams}g printerId=${entry.printerId} " +
                "at=${entry.createdAtIso}",
        )
    }
}

private fun jsonPositiveDouble(json: JSONObject, key: String): Double? {
    if (!json.has(key) || json.isNull(key)) return null
    val parsed = json.optDouble(key, Double.NaN)
    if (!parsed.isNaN() && parsed >= 0.0) return parsed
    return json.optString(key).trim().toDoubleOrNull()?.takeIf { it >= 0.0 }
}
