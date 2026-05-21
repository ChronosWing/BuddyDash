package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.FilamentSlot
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG_FILAMENT_COLOR = "BuddyDash/FilamentColor"

/** Maps Bambuddy inventory assignment to an AMS / external slot (printer_id + ams_id + tray_id). */
data class SlotInventoryKey(
    val amsId: Int,
    val trayId: Int,
)

data class SlotInventoryInfo(
    val remainPercent: Int?,
    val swatch: FilamentSwatchColors,
    val spoolId: Int,
    val spoolName: String,
)

fun formatAmsSlotLabel(
    amsId: Int,
    trayId: Int,
    isAmsHt: Boolean,
    isExternal: Boolean,
): String {
    if (isExternal) return "Ext"
    val unitIndex = if (amsId >= 128) amsId - 128 else amsId
    val letter = ('A'.code + unitIndex).toChar()
    if (isAmsHt) return "HT-$letter"
    return "$letter${trayId + 1}"
}

/** tray_id for external spool assignments (ams_id 255); vt MQTT id is tray_id + 254. */
fun externalInventoryTrayId(vtTrayGlobalId: Int, fallbackIndex: Int): Int =
    if (vtTrayGlobalId >= 254) vtTrayGlobalId - 254 else fallbackIndex

const val EXTERNAL_AMS_ID = 255

fun inventoryFillPercent(labelWeight: Int, weightUsed: Double?): Int? {
    if (labelWeight <= 0) return null
    if (weightUsed == null) return null
    val remaining = (labelWeight - weightUsed).coerceAtLeast(0.0)
    return (remaining / labelWeight * 100.0).roundToInt().coerceIn(0, 100)
}

fun inventoryFillPercentFromSpool(spool: JSONObject): Int? {
    val labelWeight = spool.optInt("label_weight", 0)
    if (!spool.has("weight_used") || spool.isNull("weight_used")) {
        return inventoryFillPercent(labelWeight, null)
    }
    return inventoryFillPercent(labelWeight, spool.optDouble("weight_used"))
}

fun spoolDisplayName(spool: JSONObject): String {
    val id = spool.optInt("id", -1)
    val label = listOfNotNull(
        spool.optString("brand").takeIf { it.isNotBlank() },
        spool.optString("material").takeIf { it.isNotBlank() },
        spool.optString("color_name").takeIf { it.isNotBlank() },
    ).joinToString(" ")
    return label.ifBlank { if (id >= 0) "spool#$id" else "spool" }
}

fun parseSlotInventoryInfo(spool: JSONObject): SlotInventoryInfo {
    val swatch = parseFilamentSwatchFromSpool(spool)
    return SlotInventoryInfo(
        remainPercent = inventoryFillPercentFromSpool(spool),
        swatch = swatch,
        spoolId = spool.optInt("id", -1),
        spoolName = spoolDisplayName(spool),
    )
}

fun parseInventoryByPrinter(assignments: JSONArray): Map<Int, Map<SlotInventoryKey, SlotInventoryInfo>> {
    val byPrinter = mutableMapOf<Int, MutableMap<SlotInventoryKey, SlotInventoryInfo>>()
    for (i in 0 until assignments.length()) {
        val row = assignments.optJSONObject(i) ?: continue
        val printerId = row.optInt("printer_id", -1)
        if (printerId < 0) continue
        val amsId = row.optInt("ams_id")
        val trayId = row.optInt("tray_id")
        val spool = row.optJSONObject("spool") ?: continue
        byPrinter.getOrPut(printerId) { mutableMapOf() }[SlotInventoryKey(amsId, trayId)] =
            parseSlotInventoryInfo(spool)
    }
    return byPrinter
}

fun applyInventoryToSlots(
    slots: List<FilamentSlot>,
    inventoryBySlot: Map<SlotInventoryKey, SlotInventoryInfo>,
    printerName: String,
    logColors: Boolean = true,
): List<FilamentSlot> =
    slots.map { slot ->
        val key = slot.inventoryKey
        val inventory = key?.let { inventoryBySlot[it] }
        val swatch = inventory?.swatch ?: FilamentSwatchColors(
            colorHexes = slot.swatchColorHexes,
            isTranslucent = slot.isTranslucent,
            alpha = slot.colorAlpha,
        )
        if (logColors) {
            Log.d(
                TAG_FILAMENT_COLOR,
                "printer=$printerName slot=${slot.label} " +
                    "trayType=${slot.filamentType} " +
                    "inventorySpool=${inventory?.spoolName ?: "none"} " +
                    "inventorySpoolId=${inventory?.spoolId?.takeIf { it >= 0 } ?: "n/a"} " +
                    "colors=${swatch.colorHexes} " +
                    "translucent=${swatch.isTranslucent} alpha=${swatch.alpha}",
            )
        }
        slot.copy(
            swatchColorHexes = swatch.colorHexes,
            isTranslucent = swatch.isTranslucent,
            colorAlpha = swatch.alpha,
            remainPercent = inventory?.remainPercent ?: slot.remainPercent,
        )
    }
