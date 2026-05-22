package com.chronoswing.buddydash.data

import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.util.MaintenanceHomeIndicator
import com.chronoswing.buddydash.util.SlotInventoryKey
import org.json.JSONArray
import org.json.JSONObject

data class HomePrintersCacheSnapshot(
    val serverUrl: String,
    val printers: List<Printer>,
    val lastUpdatedAtMillis: Long,
)

/** JSON codec for persisted Home printer cards (last-known successful enrich). */
object HomePrintersCacheCodec {
    private const val VERSION = 1

    fun encode(snapshot: HomePrintersCacheSnapshot): String =
        JSONObject()
            .put("version", VERSION)
            .put("server_url", snapshot.serverUrl)
            .put("last_updated_at_millis", snapshot.lastUpdatedAtMillis)
            .put("printers", encodePrinters(snapshot.printers))
            .toString()

    fun decode(json: String): HomePrintersCacheSnapshot? {
        return try {
            val root = JSONObject(json)
            if (root.optInt("version", 0) != VERSION) return null
            val serverUrl = root.optString("server_url").takeIf { it.isNotBlank() } ?: return null
            val lastUpdated = root.optLong("last_updated_at_millis", 0L).takeIf { it > 0L } ?: return null
            val printers = decodePrinters(root.optJSONArray("printers")) ?: return null
            HomePrintersCacheSnapshot(
                serverUrl = serverUrl,
                printers = printers,
                lastUpdatedAtMillis = lastUpdated,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun encodePrinters(printers: List<Printer>): JSONArray =
        JSONArray().apply {
            printers.forEach { put(encodePrinter(it)) }
        }

    private fun decodePrinters(array: JSONArray?): List<Printer>? {
        if (array == null) return emptyList()
        val out = mutableListOf<Printer>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            decodePrinter(obj)?.let { out.add(it) }
        }
        return out
    }

    private fun encodePrinter(printer: Printer): JSONObject =
        JSONObject()
            .put("id", printer.id)
            .put("name", printer.name)
            .putOptString("model", printer.model)
            .put("pending_queue_count", printer.pendingQueueCount)
            .put("maintenance_indicator", printer.maintenanceIndicator.name)
            .putOptObject("live_status", printer.liveStatus?.let { encodeStatus(it) })

    private fun decodePrinter(obj: JSONObject): Printer? {
        val id = obj.optInt("id", -1)
        if (id < 0) return null
        val name = obj.optString("name").takeIf { it.isNotBlank() } ?: return null
        val maintenance = runCatching {
            MaintenanceHomeIndicator.valueOf(obj.optString("maintenance_indicator", "None"))
        }.getOrDefault(MaintenanceHomeIndicator.None)
        return Printer(
            id = id,
            name = name,
            model = obj.optString("model").takeIf { it.isNotBlank() },
            liveStatus = obj.optJSONObject("live_status")?.let { decodeStatus(it) },
            maintenanceIndicator = maintenance,
            pendingQueueCount = obj.optInt("pending_queue_count", 0),
        )
    }

    private fun encodeStatus(status: PrinterStatus): JSONObject =
        JSONObject()
            .put("connected", status.connected)
            .putOptString("raw_state", status.rawState)
            .putOptDouble("progress", status.progress?.toDouble())
            .putOptString("file_name", status.fileName)
            .putOptString("current_print", status.currentPrint)
            .putOptString("subtask_name", status.subtaskName)
            .putOptString("gcode_file", status.gcodeFile)
            .putOptString("cover_url", status.coverUrl)
            .putOptInt("remaining_time_seconds", status.remainingTimeSeconds)
            .putOptDouble("nozzle_temp", status.nozzleTemp)
            .putOptDouble("bed_temp", status.bedTemp)
            .putOptDouble("chamber_temp", status.chamberTemp)
            .put("hms_errors", encodeHmsErrors(status.hmsErrors))
            .put("status_fault_messages", JSONArray(status.statusFaultMessages))
            .putOptBoolean("awaiting_plate_clear", status.awaitingPlateClear)
            .put("filament_slots", encodeFilamentSlots(status.filamentSlots))
            .putOptObject("active_filament_slot", status.activeFilamentSlot?.let { encodeSlotKey(it) })
            .put("ams_units", encodeAmsUnits(status.amsUnits))
            .putOptInt("wifi_signal_dbm", status.wifiSignalDbm)
            .putOptBoolean("wired_network", status.wiredNetwork)
            .putOptBoolean("developer_mode", status.developerMode)
            .putOptBoolean("door_open", status.doorOpen)
            .putOptString("firmware_version", status.firmwareVersion)
            .putOptInt("part_fan_percent", status.partFanPercent)
            .putOptInt("aux_fan_percent", status.auxFanPercent)
            .putOptInt("chamber_fan_percent", status.chamberFanPercent)
            .putOptInt("speed_level", status.speedLevel)
            .putOptBoolean("chamber_light_on", status.chamberLightOn)
            .putOptString("nozzle_diameter_display", status.nozzleDiameterDisplay)
            .putOptObject("filament_usage", status.filamentUsage?.let { encodeFilamentUsage(it) })

    private fun decodeStatus(obj: JSONObject): PrinterStatus =
        PrinterStatus(
            connected = obj.optBoolean("connected", false),
            rawState = obj.optString("raw_state").takeIf { it.isNotBlank() },
            progress = obj.optDouble("progress").takeIf { !it.isNaN() }?.toFloat(),
            fileName = obj.optString("file_name").takeIf { it.isNotBlank() },
            currentPrint = obj.optString("current_print").takeIf { it.isNotBlank() },
            subtaskName = obj.optString("subtask_name").takeIf { it.isNotBlank() },
            gcodeFile = obj.optString("gcode_file").takeIf { it.isNotBlank() },
            coverUrl = obj.optString("cover_url").takeIf { it.isNotBlank() },
            remainingTimeSeconds = obj.optNullableInt("remaining_time_seconds"),
            nozzleTemp = obj.optNullableDouble("nozzle_temp"),
            bedTemp = obj.optNullableDouble("bed_temp"),
            chamberTemp = obj.optNullableDouble("chamber_temp"),
            hmsErrors = decodeHmsErrors(obj.optJSONArray("hms_errors")),
            statusFaultMessages = decodeStringList(obj.optJSONArray("status_fault_messages")),
            awaitingPlateClear = obj.optNullableBoolean("awaiting_plate_clear"),
            filamentSlots = decodeFilamentSlots(obj.optJSONArray("filament_slots")),
            activeFilamentSlot = obj.optJSONObject("active_filament_slot")?.let { decodeSlotKey(it) },
            amsUnits = decodeAmsUnits(obj.optJSONArray("ams_units")),
            wifiSignalDbm = obj.optNullableInt("wifi_signal_dbm"),
            wiredNetwork = obj.optNullableBoolean("wired_network"),
            developerMode = obj.optNullableBoolean("developer_mode"),
            doorOpen = obj.optNullableBoolean("door_open"),
            firmwareVersion = obj.optString("firmware_version").takeIf { it.isNotBlank() },
            partFanPercent = obj.optNullableInt("part_fan_percent"),
            auxFanPercent = obj.optNullableInt("aux_fan_percent"),
            chamberFanPercent = obj.optNullableInt("chamber_fan_percent"),
            speedLevel = obj.optNullableInt("speed_level"),
            chamberLightOn = obj.optNullableBoolean("chamber_light_on"),
            nozzleDiameterDisplay = obj.optString("nozzle_diameter_display").takeIf { it.isNotBlank() },
            filamentUsage = obj.optJSONObject("filament_usage")?.let { decodeFilamentUsage(it) },
        )

    private fun encodeHmsErrors(errors: List<PrinterHmsError>): JSONArray =
        JSONArray().apply {
            errors.forEach { err ->
                put(
                    JSONObject()
                        .put("code", err.code)
                        .put("attr", err.attr)
                        .putOptInt("module", err.module)
                        .putOptInt("severity", err.severity)
                        .putOptString("detail", err.detail),
                )
            }
        }

    private fun decodeHmsErrors(array: JSONArray?): List<PrinterHmsError> {
        if (array == null) return emptyList()
        val out = mutableListOf<PrinterHmsError>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val code = obj.optString("code").takeIf { it.isNotBlank() } ?: continue
            out.add(
                PrinterHmsError(
                    code = code,
                    attr = obj.optInt("attr", 0),
                    module = obj.optNullableInt("module"),
                    severity = obj.optNullableInt("severity"),
                    detail = obj.optString("detail").takeIf { it.isNotBlank() },
                ),
            )
        }
        return out
    }

    private fun encodeFilamentSlots(slots: List<FilamentSlot>): JSONArray =
        JSONArray().apply {
            slots.forEach { slot ->
                put(
                    JSONObject()
                        .put("label", slot.label)
                        .putOptString("filament_type", slot.filamentType)
                        .put("swatch_color_hexes", JSONArray(slot.swatchColorHexes))
                        .put("is_translucent", slot.isTranslucent)
                        .put("color_alpha", slot.colorAlpha.toDouble())
                        .putOptInt("remain_percent", slot.remainPercent)
                        .putOptString("metadata", slot.metadata)
                        .put("is_external", slot.isExternal)
                        .put("is_loaded", slot.isLoaded)
                        .putOptInt("ams_id", slot.amsId)
                        .putOptInt("tray_id", slot.trayId)
                        .putOptInt("mqtt_tray_id", slot.mqttTrayId)
                        .putOptInt("inventory_spool_id", slot.inventorySpoolId),
                )
            }
        }

    private fun decodeFilamentSlots(array: JSONArray?): List<FilamentSlot> {
        if (array == null) return emptyList()
        val out = mutableListOf<FilamentSlot>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val label = obj.optString("label").takeIf { it.isNotBlank() } ?: continue
            out.add(
                FilamentSlot(
                    label = label,
                    filamentType = obj.optString("filament_type").takeIf { it.isNotBlank() },
                    swatchColorHexes = decodeStringList(obj.optJSONArray("swatch_color_hexes")),
                    isTranslucent = obj.optBoolean("is_translucent", false),
                    colorAlpha = obj.optDouble("color_alpha", 1.0).toFloat(),
                    remainPercent = obj.optNullableInt("remain_percent"),
                    metadata = obj.optString("metadata").takeIf { it.isNotBlank() },
                    isExternal = obj.optBoolean("is_external", false),
                    isLoaded = obj.optBoolean("is_loaded", true),
                    amsId = obj.optNullableInt("ams_id"),
                    trayId = obj.optNullableInt("tray_id"),
                    mqttTrayId = obj.optNullableInt("mqtt_tray_id"),
                    inventorySpoolId = obj.optNullableInt("inventory_spool_id"),
                ),
            )
        }
        return out
    }

    private fun encodeAmsUnits(units: List<AmsUnitInfo>): JSONArray =
        JSONArray().apply {
            units.forEach { unit ->
                put(
                    JSONObject()
                        .put("ams_id", unit.amsId)
                        .put("label", unit.label)
                        .putOptString("module_type", unit.moduleType)
                        .putOptDouble("temp_c", unit.tempC)
                        .putOptInt("humidity_percent", unit.humidityPercent),
                )
            }
        }

    private fun decodeAmsUnits(array: JSONArray?): List<AmsUnitInfo> {
        if (array == null) return emptyList()
        val out = mutableListOf<AmsUnitInfo>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            out.add(
                AmsUnitInfo(
                    amsId = obj.optInt("ams_id", 0),
                    label = obj.optString("label", ""),
                    moduleType = obj.optString("module_type").takeIf { it.isNotBlank() },
                    tempC = obj.optNullableDouble("temp_c"),
                    humidityPercent = obj.optNullableInt("humidity_percent"),
                ),
            )
        }
        return out
    }

    private fun encodeSlotKey(key: SlotInventoryKey): JSONObject =
        JSONObject()
            .put("ams_id", key.amsId)
            .put("tray_id", key.trayId)

    private fun decodeSlotKey(obj: JSONObject): SlotInventoryKey? {
        val amsId = obj.optInt("ams_id", -1)
        val trayId = obj.optInt("tray_id", -1)
        if (amsId < 0 || trayId < 0) return null
        return SlotInventoryKey(amsId, trayId)
    }

    private fun encodeFilamentUsage(usage: FilamentUsage): JSONObject =
        JSONObject()
            .putOptDouble("weight_grams", usage.weightGrams)
            .putOptDouble("length_meters", usage.lengthMeters)

    private fun decodeFilamentUsage(obj: JSONObject): FilamentUsage =
        FilamentUsage(
            weightGrams = obj.optNullableDouble("weight_grams"),
            lengthMeters = obj.optNullableDouble("length_meters"),
        )

    private fun decodeStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                array.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    private fun JSONObject.putOptString(key: String, value: String?): JSONObject {
        if (value != null) put(key, value)
        return this
    }

    private fun JSONObject.putOptObject(key: String, value: JSONObject?): JSONObject {
        if (value != null) put(key, value)
        return this
    }

    private fun JSONObject.putOptDouble(key: String, value: Double?): JSONObject {
        if (value != null) put(key, value)
        return this
    }

    private fun JSONObject.putOptInt(key: String, value: Int?): JSONObject {
        if (value != null) put(key, value)
        return this
    }

    private fun JSONObject.putOptBoolean(key: String, value: Boolean?): JSONObject {
        if (value != null) put(key, value)
        return this
    }

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) {
            val v = optDouble(key)
            if (v.isNaN()) null else v
        } else {
            null
        }

    private fun JSONObject.optNullableBoolean(key: String): Boolean? =
        if (has(key) && !isNull(key)) optBoolean(key) else null

    fun encodePrinterStatusForCache(status: PrinterStatus): JSONObject = encodeStatus(status)

    fun decodePrinterStatusForCache(obj: JSONObject): PrinterStatus = decodeStatus(obj)
}
