package com.chronoswing.buddydash.data

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import com.chronoswing.buddydash.util.FilamentSwatchColors
import com.chronoswing.buddydash.util.PrinterActivityKind
import com.chronoswing.buddydash.util.PrinterFilamentActivity
import com.chronoswing.buddydash.util.SlotInventoryKey
import org.json.JSONArray
import org.json.JSONObject

data class SpoolsCacheSnapshot(
    val serverUrl: String,
    val spools: List<SpoolInventoryItem>,
    val printerFilamentActivityById: Map<Int, PrinterFilamentActivity>,
    val lastUpdatedAtMillis: Long,
)

object SpoolsCacheCodec {
    private const val VERSION = 1

    fun encode(snapshot: SpoolsCacheSnapshot): String =
        JSONObject()
            .put("version", VERSION)
            .put("server_url", snapshot.serverUrl)
            .put("last_updated_at_millis", snapshot.lastUpdatedAtMillis)
            .put("spools", encodeSpools(snapshot.spools))
            .put("printer_activity", encodeActivityMap(snapshot.printerFilamentActivityById))
            .toString()

    fun decode(json: String): SpoolsCacheSnapshot? {
        return try {
            val root = JSONObject(json)
            if (root.optInt("version", 0) != VERSION) return null
            val serverUrl = root.optString("server_url").takeIf { it.isNotBlank() } ?: return null
            val lastUpdated = root.optLong("last_updated_at_millis", 0L).takeIf { it > 0L } ?: return null
            SpoolsCacheSnapshot(
                serverUrl = serverUrl,
                spools = decodeSpools(root.optJSONArray("spools")),
                printerFilamentActivityById = decodeActivityMap(root.optJSONObject("printer_activity")),
                lastUpdatedAtMillis = lastUpdated,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeSpools(spools: List<SpoolInventoryItem>): JSONArray =
        JSONArray().apply { spools.forEach { put(encodeSpool(it)) } }

    private fun decodeSpools(array: JSONArray?): List<SpoolInventoryItem> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                decodeSpool(array.optJSONObject(i) ?: continue)?.let { add(it) }
            }
        }
    }

    internal fun encodeSpoolItem(spool: SpoolInventoryItem): JSONObject = encodeSpool(spool)

    internal fun decodeSpoolItem(obj: JSONObject): SpoolInventoryItem? = decodeSpool(obj)

    private fun encodeSpool(spool: SpoolInventoryItem): JSONObject =
        JSONObject()
            .put("id", spool.id)
            .put("material", spool.material)
            .putOptString("subtype", spool.subtype)
            .putOptString("color_name", spool.colorName)
            .putOptString("brand", spool.brand)
            .put("swatch", encodeSwatch(spool.swatch))
            .putOptInt("remain_percent", spool.remainPercent)
            .putOptInt("low_stock_threshold_pct", spool.lowStockThresholdPct)
            .put("is_low_stock", spool.isLowStock)
            .put("display_name", spool.displayName)
            .putOptObject("assignment", spool.assignment?.let { encodeAssignment(it) })
            .putOptInt("label_weight_grams", spool.labelWeightGrams)
            .putOptDouble("weight_used_grams", spool.weightUsedGrams)
            .putOptDouble("remaining_grams", spool.remainingGrams)
            .putOptString("tag_type", spool.tagType)
            .putOptString("data_origin", spool.dataOrigin)
            .putOptString("last_used_iso", spool.lastUsedIso)

    private fun decodeSpool(obj: JSONObject): SpoolInventoryItem? {
        val id = obj.optInt("id", -1)
        if (id < 0) return null
        val material = obj.optString("material").takeIf { it.isNotBlank() } ?: return null
        return SpoolInventoryItem(
            id = id,
            material = material,
            subtype = obj.optString("subtype").takeIf { it.isNotBlank() },
            colorName = obj.optString("color_name").takeIf { it.isNotBlank() },
            brand = obj.optString("brand").takeIf { it.isNotBlank() },
            swatch = decodeSwatch(obj.optJSONObject("swatch")),
            remainPercent = obj.optNullableInt("remain_percent"),
            lowStockThresholdPct = obj.optNullableInt("low_stock_threshold_pct"),
            isLowStock = obj.optBoolean("is_low_stock", false),
            displayName = obj.optString("display_name", material),
            assignment = obj.optJSONObject("assignment")?.let { decodeAssignment(it) },
            labelWeightGrams = obj.optNullableInt("label_weight_grams"),
            weightUsedGrams = obj.optNullableDouble("weight_used_grams"),
            remainingGrams = obj.optNullableDouble("remaining_grams"),
            tagType = obj.optString("tag_type").takeIf { it.isNotBlank() },
            dataOrigin = obj.optString("data_origin").takeIf { it.isNotBlank() },
            lastUsedIso = obj.optString("last_used_iso").takeIf { it.isNotBlank() },
        )
    }

    private fun encodeSwatch(swatch: FilamentSwatchColors): JSONObject =
        JSONObject()
            .put("color_hexes", encodeStringList(swatch.colorHexes))
            .put("is_translucent", swatch.isTranslucent)
            .put("alpha", swatch.alpha.toDouble())

    private fun decodeSwatch(obj: JSONObject?): FilamentSwatchColors {
        if (obj == null) return FilamentSwatchColors(emptyList())
        return FilamentSwatchColors(
            colorHexes = decodeStringList(obj.optJSONArray("color_hexes")),
            isTranslucent = obj.optBoolean("is_translucent", false),
            alpha = obj.optDouble("alpha", 1.0).toFloat(),
        )
    }

    private fun encodeAssignment(assignment: SpoolSlotAssignment): JSONObject =
        JSONObject()
            .put("printer_id", assignment.printerId)
            .put("printer_name", assignment.printerName)
            .put("slot_label", assignment.slotLabel)
            .putOptInt("ams_id", assignment.amsId)
            .putOptInt("tray_id", assignment.trayId)

    private fun decodeAssignment(obj: JSONObject): SpoolSlotAssignment =
        SpoolSlotAssignment(
            printerId = obj.optInt("printer_id", 0),
            printerName = obj.optString("printer_name", ""),
            slotLabel = obj.optString("slot_label", ""),
            amsId = obj.optNullableInt("ams_id"),
            trayId = obj.optNullableInt("tray_id"),
        )

    private fun encodeActivityMap(map: Map<Int, PrinterFilamentActivity>): JSONObject =
        JSONObject().apply {
            map.forEach { (printerId, activity) ->
                put(
                    printerId.toString(),
                    JSONObject()
                        .put("activity_kind", activity.activityKind.name)
                        .putOptObject(
                            "active_filament_slot",
                            activity.activeFilamentSlot?.let {
                                JSONObject().put("ams_id", it.amsId).put("tray_id", it.trayId)
                            },
                        ),
                )
            }
        }

    private fun decodeActivityMap(obj: JSONObject?): Map<Int, PrinterFilamentActivity> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<Int, PrinterFilamentActivity>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val printerId = key.toIntOrNull() ?: continue
            val entry = obj.optJSONObject(key) ?: continue
            val kind = runCatching {
                PrinterActivityKind.valueOf(entry.optString("activity_kind", "Idle"))
            }.getOrDefault(PrinterActivityKind.Idle)
            val slotObj = entry.optJSONObject("active_filament_slot")
            val activeKey = slotObj?.let {
                SlotInventoryKey(it.optInt("ams_id", 0), it.optInt("tray_id", 0))
            }
            out[printerId] = PrinterFilamentActivity(kind, activeKey)
        }
        return out
    }
}
