package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.FilamentSlot
import org.json.JSONObject

private const val TAG_FILAMENT_ACTIVE = "BuddyDash/FilamentActive"

data class FilamentSourceGroup(
    val sourceLabel: String,
    val amsId: Int,
    val isExternal: Boolean,
    val slots: List<FilamentSlot>,
)

/** Group label for UI rows, e.g. AMS-A or Ext. */
fun formatAmsGroupLabel(amsId: Int, isExternal: Boolean = false): String {
    if (isExternal || amsId == EXTERNAL_AMS_ID) return "Ext"
    return formatAmsUnitLabel(amsId)
}

fun List<FilamentSlot>.groupByFilamentSource(): List<FilamentSourceGroup> {
    val byAms = linkedMapOf<Int, MutableList<FilamentSlot>>()
    val external = mutableListOf<FilamentSlot>()
    for (slot in this) {
        if (slot.isExternal) {
            external.add(slot)
        } else {
            val amsId = slot.amsId ?: continue
            byAms.getOrPut(amsId) { mutableListOf() }.add(slot)
        }
    }
    val groups = byAms.entries
        .sortedBy { (amsId, _) -> if (amsId >= 128) amsId - 128 else amsId }
        .map { (amsId, slots) ->
            FilamentSourceGroup(
                sourceLabel = formatAmsGroupLabel(amsId),
                amsId = amsId,
                isExternal = false,
                slots = slots.sortedBy { it.trayId ?: 0 },
            )
        }
        .toMutableList()
    if (external.isNotEmpty()) {
        groups.add(
            FilamentSourceGroup(
                sourceLabel = formatAmsGroupLabel(EXTERNAL_AMS_ID, isExternal = true),
                amsId = EXTERNAL_AMS_ID,
                isExternal = true,
                slots = external.sortedBy { it.trayId ?: 0 },
            ),
        )
    }
    return groups
}

/**
 * Resolves active toolhead filament from Bambuddy status fields.
 * Uses [tray_now] matched to tray MQTT [id] on each slot; does not guess when unmatched.
 */
fun resolveActiveFilamentSlot(
    statusJson: JSONObject,
    slots: List<FilamentSlot>,
    logRaw: Boolean = false,
): SlotInventoryKey? {
    val trayNow = if (statusJson.has("tray_now") && !statusJson.isNull("tray_now")) {
        statusJson.getInt("tray_now")
    } else {
        null
    }
    val stgCur = if (statusJson.has("stg_cur") && !statusJson.isNull("stg_cur")) {
        statusJson.getInt("stg_cur")
    } else {
        null
    }
    val stgCurName = statusJson.optString("stg_cur_name").takeIf { it.isNotBlank() }

    if (logRaw) {
        Log.d(
            TAG_FILAMENT_ACTIVE,
            "tray_now=$trayNow stg_cur=$stgCur stg_cur_name=$stgCurName " +
                "slotIds=${slots.map { "${it.label}:${it.mqttTrayId}" }}",
        )
    }

    if (trayNow == null || trayNow == 255) {
        return null
    }

    val byMqttId = slots.firstOrNull { slot ->
        slot.mqttTrayId != null && slot.mqttTrayId == trayNow
    }?.inventoryKey
    if (byMqttId != null) {
        if (logRaw) Log.d(TAG_FILAMENT_ACTIVE, "active match tray_now=$trayNow -> $byMqttId")
        return byMqttId
    }

    if (trayNow >= 254) {
        val external = slots.firstOrNull { it.isExternal }?.inventoryKey
        if (external != null && logRaw) {
            Log.d(TAG_FILAMENT_ACTIVE, "active external tray_now=$trayNow -> $external")
        }
        return external
    }

    if (logRaw) {
        Log.d(TAG_FILAMENT_ACTIVE, "no active slot match for tray_now=$trayNow")
    }
    return null
}

fun FilamentSlot.isActiveSlot(activeKey: SlotInventoryKey?): Boolean =
    activeKey != null && inventoryKey == activeKey
