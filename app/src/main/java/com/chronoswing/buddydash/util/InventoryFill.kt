package com.chronoswing.buddydash.util

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** Maps Bambuddy inventory assignment to an AMS / external slot. */
data class SlotInventoryKey(
    val amsId: Int,
    val trayId: Int,
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

/**
 * Remaining % from Bambuddy spool inventory (label_weight − weight_used).
 * Matches PrintersPage inventory fill when weight_used is tracked.
 */
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

fun parseInventoryFillByPrinter(assignments: JSONArray): Map<Int, Map<SlotInventoryKey, Int>> {
    val byPrinter = mutableMapOf<Int, MutableMap<SlotInventoryKey, Int>>()
    for (i in 0 until assignments.length()) {
        val row = assignments.optJSONObject(i) ?: continue
        val printerId = row.optInt("printer_id", -1)
        if (printerId < 0) continue
        val amsId = row.optInt("ams_id")
        val trayId = row.optInt("tray_id")
        val spool = row.optJSONObject("spool") ?: continue
        val fill = inventoryFillPercentFromSpool(spool) ?: continue
        byPrinter.getOrPut(printerId) { mutableMapOf() }[SlotInventoryKey(amsId, trayId)] = fill
    }
    return byPrinter
}

fun applyInventoryFill(
    slots: List<com.chronoswing.buddydash.data.model.FilamentSlot>,
    fillBySlot: Map<SlotInventoryKey, Int>,
): List<com.chronoswing.buddydash.data.model.FilamentSlot> =
    slots.map { slot ->
        val key = slot.inventoryKey ?: return@map slot
        val fill = fillBySlot[key] ?: return@map slot
        slot.copy(remainPercent = fill)
    }
