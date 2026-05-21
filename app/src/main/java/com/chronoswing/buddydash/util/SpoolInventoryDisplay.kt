package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import org.json.JSONArray
import org.json.JSONObject

/** OpenAPI default when [AppSettings.low_stock_threshold] is unavailable. */
const val DEFAULT_LOW_STOCK_THRESHOLD_PCT = 20f

fun parseLowStockThreshold(settings: JSONObject?): Float {
    if (settings == null) return DEFAULT_LOW_STOCK_THRESHOLD_PCT
    val raw = settings.opt("low_stock_threshold")
    val value = when (raw) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull()
        else -> null
    } ?: return DEFAULT_LOW_STOCK_THRESHOLD_PCT
    return value.toFloat().coerceIn(0.1f, 99.9f)
}

fun isSpoolLowStock(
    remainPercent: Int?,
    spoolThresholdPct: Int?,
    globalThresholdPct: Float,
): Boolean {
    val remaining = remainPercent ?: return false
    val threshold = spoolThresholdPct?.toFloat() ?: globalThresholdPct
    return remaining <= threshold
}

fun parseSpoolFromResponse(json: JSONObject): SpoolInventoryItem {
    val id = json.optInt("id", -1)
    val material = json.optString("material").trim().ifBlank { "Unknown" }
    val subtype = json.optString("subtype").takeIf { it.isNotBlank() }
    val colorName = json.optString("color_name").takeIf { it.isNotBlank() }
    val brand = json.optString("brand").takeIf { it.isNotBlank() }
    val swatch = parseFilamentSwatchFromSpool(json)
    val remainPercent = inventoryFillPercentFromSpool(json)
    val lowThreshold = json.optInt("low_stock_threshold_pct", -1).takeIf { it in 1..99 }
    val displayName = spoolDisplayName(json)
    return SpoolInventoryItem(
        id = id,
        material = material,
        subtype = subtype,
        colorName = colorName,
        brand = brand,
        swatch = swatch,
        remainPercent = remainPercent,
        lowStockThresholdPct = lowThreshold,
        isLowStock = false,
        displayName = displayName,
    )
}

fun parseSpoolInventoryList(body: String, globalLowStockThresholdPct: Float): List<SpoolInventoryItem> {
    val array = JSONArray(body)
    return List(array.length()) { index ->
        val spool = parseSpoolFromResponse(array.getJSONObject(index))
        spool.copy(
            isLowStock = isSpoolLowStock(
                remainPercent = spool.remainPercent,
                spoolThresholdPct = spool.lowStockThresholdPct,
                globalThresholdPct = globalLowStockThresholdPct,
            ),
        )
    }.filter { it.id >= 0 }
}

fun parseSpoolAssignments(body: String): Map<Int, SpoolSlotAssignment> {
    val array = JSONArray(body)
    val bySpoolId = mutableMapOf<Int, SpoolSlotAssignment>()
    for (i in 0 until array.length()) {
        val row = array.optJSONObject(i) ?: continue
        val spoolId = row.optInt("spool_id", -1)
        if (spoolId < 0) continue
        val printerId = row.optInt("printer_id", -1)
        if (printerId < 0) continue
        val printerName = row.optString("printer_name").trim()
            .ifBlank { "Printer $printerId" }
        val amsId = row.optInt("ams_id")
        val trayId = row.optInt("tray_id")
        val slotLabel = row.optString("ams_label").trim().ifBlank {
            formatAssignmentSlotLabel(amsId, trayId)
        }
        bySpoolId[spoolId] = SpoolSlotAssignment(
            printerId = printerId,
            printerName = printerName,
            slotLabel = slotLabel,
        )
    }
    return bySpoolId
}

private fun formatAssignmentSlotLabel(amsId: Int, trayId: Int): String {
    if (amsId == EXTERNAL_AMS_ID) return "Ext"
    return formatAmsSlotLabel(
        amsId = amsId,
        trayId = trayId,
        isAmsHt = false,
        isExternal = false,
    )
}

fun mergeSpoolsWithAssignments(
    spools: List<SpoolInventoryItem>,
    assignmentsBySpoolId: Map<Int, SpoolSlotAssignment>,
): List<SpoolInventoryItem> =
    spools.map { spool ->
        spool.copy(assignment = assignmentsBySpoolId[spool.id])
    }

enum class SpoolInventoryFilter {
    All,
    Low,
    Loaded,
    Unloaded,
}

fun applySpoolInventorySearch(
    spools: List<SpoolInventoryItem>,
    query: String,
    filter: SpoolInventoryFilter,
): List<SpoolInventoryItem> {
    val normalizedQuery = query.trim().lowercase()
    return spools.filter { spool ->
        val matchesFilter = when (filter) {
            SpoolInventoryFilter.All -> true
            SpoolInventoryFilter.Low -> spool.isLowStock
            SpoolInventoryFilter.Loaded -> spool.assignment != null
            SpoolInventoryFilter.Unloaded -> spool.assignment == null
        }
        if (!matchesFilter) return@filter false
        if (normalizedQuery.isEmpty()) return@filter true
        val haystack = buildList {
            add(spool.displayName)
            add(spool.material)
            spool.subtype?.let { add(it) }
            spool.colorName?.let { add(it) }
            spool.brand?.let { add(it) }
            spool.assignment?.let {
                add(it.printerName)
                add(it.slotLabel)
            }
        }.joinToString(" ").lowercase()
        haystack.contains(normalizedQuery)
    }
}

fun formatSpoolMaterialLabel(material: String, subtype: String?): String =
    if (subtype.isNullOrBlank()) material else "$material · $subtype"
