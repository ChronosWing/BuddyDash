package com.chronoswing.buddydash.network

import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterMaintenanceOverview
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.util.FilamentSwatchColors
import com.chronoswing.buddydash.util.SlotInventoryInfo
import com.chronoswing.buddydash.util.SlotInventoryKey
import com.chronoswing.buddydash.util.applyInventoryToSlots
import com.chronoswing.buddydash.util.EXTERNAL_AMS_ID
import com.chronoswing.buddydash.util.externalInventoryTrayId
import com.chronoswing.buddydash.util.formatAmsSlotLabel
import com.chronoswing.buddydash.util.formatAmsUnitLabel
import com.chronoswing.buddydash.util.formatNozzleDiameterDisplay
import com.chronoswing.buddydash.util.resolveActiveFilamentSlot
import com.chronoswing.buddydash.util.isTrayLoaded
import com.chronoswing.buddydash.util.normalizeFilamentType
import com.chronoswing.buddydash.util.normalizeTrayColor
import com.chronoswing.buddydash.util.parseInventoryByPrinter
import com.chronoswing.buddydash.util.etaDebugLogLine
import com.chronoswing.buddydash.util.parseRemainingTimeSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BambuddyApiClient {

    companion object {
        /** Temporary: log raw AMS / vt_tray JSON from status responses. Set false before release. */
        private const val DEBUG_LOG_FILAMENT_RAW = true
        private const val TAG_FILAMENT = "BuddyDash/Filament"
        /** Temporary: log ETA-related raw fields during active prints. Set false before release. */
        private const val DEBUG_LOG_ETA_RAW = true
        private const val TAG_ETA = "BuddyDash/Eta"
        /** Temporary: log newly parsed detail/status fields. Set false before release. */
        private const val DEBUG_LOG_DETAIL_RAW = true
        private const val TAG_DETAIL = "BuddyDash/Detail"
        private const val TAG_MOTION = "BuddyDash/Motion"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Server liveness — Bambuddy exposes GET /health at the app root (not /api/health). */
    suspend fun checkServerHealth(serverUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            val baseUrl = normalizeBambuddyBaseUrl(serverUrl) ?: return@withContext Result.failure(
                IllegalArgumentException("Server URL is required"),
            )
            runCatching {
                client.newCall(
                    Request.Builder().url("$baseUrl/health").get().build(),
                ).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw Exception("Server returned ${response.code}")
                    }
                    val status = JSONObject(body).optString("status", "healthy")
                    "Server healthy — $status"
                }
            }
        }

    /** Verifies API key by listing printers (the endpoint the app actually uses). */
    suspend fun testApiConnection(serverUrl: String, apiKey: String): Result<String> =
        withContext(Dispatchers.IO) {
            fetchPrinters(serverUrl, apiKey).map { printers ->
                val count = printers.size
                if (count == 0) {
                    "Connected — no printers configured"
                } else {
                    "Connected — $count printer${if (count == 1) "" else "s"} found"
                }
            }
        }

    suspend fun fetchPrinters(serverUrl: String, apiKey: String): Result<List<Printer>> =
        withContext(Dispatchers.IO) {
            runApiCall(serverUrl, apiKey, BambuddyApi.LIST_PRINTERS_PATH) { body ->
                val array = JSONArray(body)
                List(array.length()) { index ->
                    parsePrinterFromConfig(array.getJSONObject(index))
                }
            }
        }

    /** List printers merged with live status (connected, state, HMS) from /status. */
    suspend fun fetchPrintersWithStatus(serverUrl: String, apiKey: String): Result<List<Printer>> =
        withContext(Dispatchers.IO) {
            fetchPrinters(serverUrl, apiKey).mapCatching { printers ->
                val inventoryByPrinter = fetchInventoryByPrinter(serverUrl, apiKey, printerId = null)
                    .getOrElse { emptyMap() }
                coroutineScope {
                    printers.map { printer ->
                        async {
                            val statusResult = fetchPrinterStatus(
                                serverUrl = serverUrl,
                                apiKey = apiKey,
                                printerId = printer.id,
                                inventoryBySlot = inventoryByPrinter[printer.id],
                            )
                            mergePrinterWithStatus(printer, statusResult.getOrNull())
                        }
                    }.awaitAll()
                }
            }
        }

    suspend fun fetchPrinterStatus(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
        inventoryBySlot: Map<SlotInventoryKey, SlotInventoryInfo>? = null,
    ): Result<PrinterStatus> =
        withContext(Dispatchers.IO) {
            val inventory = inventoryBySlot ?: fetchInventoryByPrinter(serverUrl, apiKey, printerId)
                .getOrElse { emptyMap() }
                .getOrElse(printerId) { emptyMap() }
            runApiCall(serverUrl, apiKey, BambuddyApi.printerStatusPath(printerId)) { body ->
                parsePrinterStatus(JSONObject(body), inventory)
            }
        }

    private fun fetchInventoryByPrinter(
        serverUrl: String,
        apiKey: String,
        printerId: Int?,
    ): Result<Map<Int, Map<SlotInventoryKey, SlotInventoryInfo>>> =
        runApiCall(serverUrl, apiKey, BambuddyApi.inventoryAssignmentsPath(printerId)) { body ->
            parseInventoryByPrinter(JSONArray(body))
        }

    private inline fun <T> runApiCall(
        serverUrl: String,
        apiKey: String,
        path: String,
        method: String = "GET",
        parse: (String) -> T,
    ): Result<T> {
        val baseUrl = normalizeBambuddyBaseUrl(serverUrl)
        if (baseUrl == null) {
            return Result.failure(IllegalArgumentException("Server URL is required"))
        }
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return Result.failure(IllegalArgumentException("API key is required"))
        }

        val requestBuilder = Request.Builder()
            .url("$baseUrl$path")
            .header("X-API-Key", trimmedKey)

        val request = when (method) {
            "POST" -> requestBuilder.post("".toRequestBody("application/json".toMediaType())).build()
            else -> requestBuilder.get().build()
        }

        return runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching { JSONObject(body).optString("detail") }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                    throw Exception(
                        detail ?: "Server returned ${response.code}",
                    )
                }
                parse(body)
            }
        }
    }

    /** Config row from GET /api/v1/printers — no live connection/state fields. */
    private fun parsePrinterFromConfig(json: JSONObject): Printer = Printer(
        id = json.getInt("id"),
        name = json.optString("name", "Printer ${json.getInt("id")}"),
        model = json.optString("model").takeIf { it.isNotBlank() },
    )

    private fun mergePrinterWithStatus(printer: Printer, status: PrinterStatus?): Printer =
        printer.copy(liveStatus = status)

    suspend fun clearPlate(serverUrl: String, apiKey: String, printerId: Int): Result<String> =
        withContext(Dispatchers.IO) {
            if (!BambuddyApi.hasClearPlateEndpoint) {
                return@withContext Result.failure(
                    UnsupportedOperationException("Plate clear endpoint not found"),
                )
            }
            runApiCall(serverUrl, apiKey, BambuddyApi.clearPlatePath(printerId), method = "POST") { body ->
                JSONObject(body).optString("message", "Plate marked clear")
            }
        }

    suspend fun fetchMaintenance(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<PrinterMaintenanceOverview> =
        withContext(Dispatchers.IO) {
            if (!BambuddyApi.hasMaintenanceEndpoint) {
                return@withContext Result.failure(
                    UnsupportedOperationException("Maintenance endpoint not found"),
                )
            }
            runApiCall(serverUrl, apiKey, BambuddyApi.maintenancePrinterPath(printerId)) { body ->
                parseMaintenanceOverview(JSONObject(body))
            }
        }

    suspend fun setPrintSpeed(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
        mode: Int,
    ): Result<Unit> = postPrinterAction(serverUrl, apiKey, BambuddyApi.printSpeedPath(printerId, mode))

    suspend fun pausePrint(serverUrl: String, apiKey: String, printerId: Int): Result<Unit> =
        postPrinterAction(serverUrl, apiKey, BambuddyApi.printPausePath(printerId))

    suspend fun resumePrint(serverUrl: String, apiKey: String, printerId: Int): Result<Unit> =
        postPrinterAction(serverUrl, apiKey, BambuddyApi.printResumePath(printerId))

    suspend fun stopPrint(serverUrl: String, apiKey: String, printerId: Int): Result<Unit> =
        postPrinterAction(serverUrl, apiKey, BambuddyApi.printStopPath(printerId))

    suspend fun setChamberLight(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
        on: Boolean,
    ): Result<Unit> = postPrinterAction(serverUrl, apiKey, BambuddyApi.chamberLightPath(printerId, on))

    suspend fun bedJog(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
        distanceMm: Float,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasBedJogEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("Bed jog endpoint not found"),
            )
        }
        val path = BambuddyApi.bedJogPath(printerId, distanceMm)
        if (DEBUG_LOG_DETAIL_RAW) {
            Log.d(TAG_MOTION, "POST $path")
        }
        postPrinterAction(serverUrl, apiKey, path)
    }

    private suspend fun postPrinterAction(
        serverUrl: String,
        apiKey: String,
        path: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runApiCall(serverUrl, apiKey, path, method = "POST") { Unit }
    }

    private fun parsePrinterStatus(
        json: JSONObject,
        inventoryBySlot: Map<SlotInventoryKey, SlotInventoryInfo> = emptyMap(),
    ): PrinterStatus {
        val temperatures = json.optJSONObject("temperatures")
        val hmsErrors = json.optJSONArray("hms_errors")

        val awaitingPlateClear = if (json.has("awaiting_plate_clear") && !json.isNull("awaiting_plate_clear")) {
            json.getBoolean("awaiting_plate_clear")
        } else {
            null
        }

        val amsUnits = parseAmsUnits(json)
        val operational = parseOperationalFields(json, temperatures)
        val filament = parseFilamentSlots(json, inventoryBySlot)
        val metadata = parsePrinterMetadata(json)

        return PrinterStatus(
            connected = json.optBoolean("connected", false),
            rawState = json.optString("state").takeIf { it.isNotBlank() },
            progress = json.optDouble("progress")
                .takeIf { json.has("progress") && !json.isNull("progress") }
                ?.toFloat(),
            fileName = resolveFileName(json),
            remainingTimeSeconds = run {
                val rawState = json.optString("state").takeIf { it.isNotBlank() }
                val seconds = parseRemainingTimeSeconds(json)
                if (DEBUG_LOG_ETA_RAW) {
                    etaDebugLogLine(json, rawState, seconds)?.let { line ->
                        Log.d(TAG_ETA, line)
                    }
                }
                seconds
            },
            nozzleTemp = temperatures?.optDouble("nozzle"),
            bedTemp = temperatures?.optDouble("bed"),
            chamberTemp = operational.chamberTemp,
            hmsErrorCount = hmsErrors?.length() ?: 0,
            awaitingPlateClear = awaitingPlateClear,
            filamentSlots = filament.slots,
            activeFilamentSlot = filament.activeKey,
            amsUnits = amsUnits,
            wifiSignalDbm = operational.wifiSignalDbm,
            wiredNetwork = operational.wiredNetwork,
            doorOpen = operational.doorOpen,
            firmwareVersion = operational.firmwareVersion,
            partFanPercent = operational.partFanPercent,
            auxFanPercent = operational.auxFanPercent,
            chamberFanPercent = operational.chamberFanPercent,
            speedLevel = operational.speedLevel,
            chamberLightOn = operational.chamberLightOn,
            nozzleDiameterDisplay = metadata.nozzleDiameterDisplay,
        )
    }

    private data class PrinterMetadataFields(
        val nozzleDiameterDisplay: String?,
    )

    private fun parsePrinterMetadata(json: JSONObject): PrinterMetadataFields {
        val activeExtruder = json.optInt("active_extruder", 0)
        val nozzleDiameterDisplay = parseNozzleDiameterFromStatus(json, activeExtruder)

        if (DEBUG_LOG_DETAIL_RAW) {
            Log.d(
                TAG_DETAIL,
                "metadata active_extruder=$activeExtruder " +
                    "nozzles=${json.optJSONArray("nozzles")?.length() ?: 0} " +
                    "nozzle_rack=${json.optJSONArray("nozzle_rack")?.length() ?: 0} " +
                    "nozzleDiameter=$nozzleDiameterDisplay",
            )
        }

        return PrinterMetadataFields(nozzleDiameterDisplay = nozzleDiameterDisplay)
    }

    private fun parseNozzleDiameterFromStatus(json: JSONObject, activeExtruder: Int): String? {
        parseNozzleDiameterFromArray(json.optJSONArray("nozzles"), activeExtruder)
            ?.let { return it }
        return parseNozzleDiameterFromArray(json.optJSONArray("nozzle_rack"), activeExtruder)
    }

    private fun parseNozzleDiameterFromArray(array: JSONArray?, activeExtruder: Int): String? {
        if (array == null || array.length() == 0) return null
        val indices = buildList {
            add(activeExtruder)
            for (i in 0 until array.length()) {
                if (i != activeExtruder) add(i)
            }
        }
        for (index in indices) {
            val entry = array.optJSONObject(index) ?: continue
            formatNozzleDiameterDisplay(entry.optString("nozzle_diameter"))?.let { return it }
        }
        return null
    }

    private data class OperationalFields(
        val chamberTemp: Double?,
        val wifiSignalDbm: Int?,
        val wiredNetwork: Boolean?,
        val doorOpen: Boolean?,
        val firmwareVersion: String?,
        val partFanPercent: Int?,
        val auxFanPercent: Int?,
        val chamberFanPercent: Int?,
        val speedLevel: Int?,
        val chamberLightOn: Boolean?,
    )

    private fun parseOperationalFields(
        json: JSONObject,
        temperatures: JSONObject?,
    ): OperationalFields {
        val chamberTemp = parseChamberTemp(temperatures)
        val wifiSignalDbm = json.optInt("wifi_signal").takeIf { json.has("wifi_signal") && !json.isNull("wifi_signal") }
        val wiredNetwork = if (json.has("wired_network") && !json.isNull("wired_network")) {
            json.getBoolean("wired_network")
        } else {
            null
        }
        val doorOpen = if (json.has("door_open") && !json.isNull("door_open")) {
            json.getBoolean("door_open")
        } else {
            null
        }
        val firmwareVersion = json.optString("firmware_version").takeIf { it.isNotBlank() }
        val partFanPercent = json.optInt("cooling_fan_speed").takeIf {
            json.has("cooling_fan_speed") && !json.isNull("cooling_fan_speed")
        }
        val auxFanPercent = json.optInt("big_fan1_speed").takeIf {
            json.has("big_fan1_speed") && !json.isNull("big_fan1_speed")
        }
        val chamberFanPercent = json.optInt("big_fan2_speed").takeIf {
            json.has("big_fan2_speed") && !json.isNull("big_fan2_speed")
        }
        val speedLevel = json.optInt("speed_level").takeIf {
            json.has("speed_level") && !json.isNull("speed_level")
        }
        val chamberLightOn = if (json.has("chamber_light") && !json.isNull("chamber_light")) {
            json.getBoolean("chamber_light")
        } else {
            null
        }

        if (DEBUG_LOG_DETAIL_RAW) {
            Log.d(
                TAG_DETAIL,
                "operational wifi=$wifiSignalDbm wired=$wiredNetwork door=$doorOpen " +
                    "firmware=$firmwareVersion chamber=$chamberTemp fans=($partFanPercent,$auxFanPercent,$chamberFanPercent) " +
                    "speed=$speedLevel light=$chamberLightOn heatbreak=${json.opt("heatbreak_fan_speed")}",
            )
            temperatures?.keys()?.asSequence()?.toList()?.let { keys ->
                if (keys.isNotEmpty()) Log.d(TAG_DETAIL, "temperature keys=$keys")
            }
        }

        return OperationalFields(
            chamberTemp = chamberTemp,
            wifiSignalDbm = wifiSignalDbm,
            wiredNetwork = wiredNetwork,
            doorOpen = doorOpen,
            firmwareVersion = firmwareVersion,
            partFanPercent = partFanPercent,
            auxFanPercent = auxFanPercent,
            chamberFanPercent = chamberFanPercent,
            speedLevel = speedLevel,
            chamberLightOn = chamberLightOn,
        )
    }

    private fun parseChamberTemp(temperatures: JSONObject?): Double? {
        if (temperatures == null) return null
        for (key in listOf("chamber", "chamber_temp", "chamber_temperature")) {
            if (temperatures.has(key) && !temperatures.isNull(key)) {
                return temperatures.optDouble(key)
            }
        }
        return null
    }

    private fun parseAmsUnits(json: JSONObject): List<AmsUnitInfo> {
        val amsArray = json.optJSONArray("ams") ?: return emptyList()
        val units = mutableListOf<AmsUnitInfo>()
        for (unitIndex in 0 until amsArray.length()) {
            val unit = amsArray.optJSONObject(unitIndex) ?: continue
            val amsId = unit.optInt("id", unitIndex)
            val temp = unit.optDouble("temp").takeIf { unit.has("temp") && !unit.isNull("temp") }
            val humidity = unit.optInt("humidity").takeIf { unit.has("humidity") && !unit.isNull("humidity") }
            if (temp == null && humidity == null) continue
            units.add(
                AmsUnitInfo(
                    amsId = amsId,
                    label = formatAmsUnitLabel(amsId),
                    tempC = temp,
                    humidityPercent = humidity,
                ),
            )
            if (DEBUG_LOG_DETAIL_RAW) {
                Log.d(TAG_DETAIL, "ams unit $amsId temp=$temp humidity=$humidity")
            }
        }
        return units
    }

    private fun parseMaintenanceOverview(json: JSONObject): PrinterMaintenanceOverview {
        val itemsArray = json.optJSONArray("maintenance_items") ?: JSONArray()
        val items = List(itemsArray.length()) { index ->
            val item = itemsArray.getJSONObject(index)
            MaintenanceItem(
                name = item.optString("maintenance_type_name", "Maintenance"),
                isDue = item.optBoolean("is_due", false),
                isWarning = item.optBoolean("is_warning", false),
                enabled = item.optBoolean("enabled", true),
            )
        }
        val totalPrintHours = json.optDouble("total_print_hours")
            .takeIf { json.has("total_print_hours") && !json.isNull("total_print_hours") && it > 0.0 }
        if (DEBUG_LOG_DETAIL_RAW) {
            Log.d(
                TAG_DETAIL,
                "maintenance items=${items.size} due=${json.optInt("due_count")} " +
                    "warn=${json.optInt("warning_count")} total_print_hours=$totalPrintHours",
            )
        }
        return PrinterMaintenanceOverview(
            items = items,
            totalPrintHours = totalPrintHours,
        )
    }

    private data class FilamentParseResult(
        val slots: List<FilamentSlot>,
        val activeKey: SlotInventoryKey?,
    )

    private fun parseFilamentSlots(
        json: JSONObject,
        inventoryBySlot: Map<SlotInventoryKey, SlotInventoryInfo>,
    ): FilamentParseResult {
        val printerName = json.optString("name", "")
        if (DEBUG_LOG_FILAMENT_RAW) {
            val printerId = json.optInt("id", -1)
            Log.d(
                TAG_FILAMENT,
                "status printer=$printerId ($printerName) ams=${json.optJSONArray("ams")}",
            )
            Log.d(
                TAG_FILAMENT,
                "status printer=$printerId ($printerName) vt_tray=${json.optJSONArray("vt_tray")}",
            )
        }
        val slots = mutableListOf<FilamentSlot>()
        json.optJSONArray("ams")?.let { amsArray ->
            for (unitIndex in 0 until amsArray.length()) {
                val unit = amsArray.optJSONObject(unitIndex) ?: continue
                val amsId = unit.optInt("id", unitIndex)
                if (DEBUG_LOG_DETAIL_RAW) {
                    Log.d(
                        TAG_DETAIL,
                        "ams[$amsId] temp=${unit.opt("temp")} humidity=${unit.opt("humidity")}",
                    )
                }
                val trays = unit.optJSONArray("tray") ?: continue
                val isAmsHt = trays.length() == 1
                for (trayIndex in 0 until trays.length()) {
                    val trayJson = trays.optJSONObject(trayIndex) ?: continue
                    // Inventory assignments use 0-based slot index, not necessarily tray.id from MQTT.
                    slots.add(
                        parseTray(
                            tray = trayJson,
                            label = formatAmsSlotLabel(amsId, trayIndex, isAmsHt, isExternal = false),
                            amsId = amsId,
                            trayId = trayIndex,
                        ),
                    )
                }
            }
        }
        json.optJSONArray("vt_tray")?.let { vtArray ->
            for (i in 0 until vtArray.length()) {
                val trayJson = vtArray.optJSONObject(i) ?: continue
                val globalId = trayJson.optInt("id", 254 + i)
                val trayId = externalInventoryTrayId(globalId, i)
                slots.add(
                    parseTray(
                        tray = trayJson,
                        label = formatAmsSlotLabel(EXTERNAL_AMS_ID, trayId, isAmsHt = false, isExternal = true),
                        amsId = EXTERNAL_AMS_ID,
                        trayId = trayId,
                        isExternal = true,
                    ),
                )
            }
        }
        val enriched = applyInventoryToSlots(
            slots = slots,
            inventoryBySlot = inventoryBySlot,
            printerName = printerName.ifBlank { "printer ${json.optInt("id", -1)}" },
            logColors = DEBUG_LOG_FILAMENT_RAW,
        )
        val activeKey = resolveActiveFilamentSlot(
            statusJson = json,
            slots = enriched,
            logRaw = DEBUG_LOG_FILAMENT_RAW,
        )
        return FilamentParseResult(slots = enriched, activeKey = activeKey)
    }

    private fun parseTray(
        tray: JSONObject?,
        label: String,
        amsId: Int,
        trayId: Int,
        isExternal: Boolean = false,
    ): FilamentSlot {
        val trayJson = tray ?: JSONObject()
        val type = normalizeFilamentType(trayJson.optString("tray_type"))
        val traySwatch = FilamentSwatchColors.fromTrayColor(trayJson.optString("tray_color"))
        val meta = trayJson.optString("tray_id_name").takeIf { it.isNotBlank() }
        val loaded = isTrayLoaded(type, traySwatch.colorHexes.firstOrNull(), remainPercent = null, meta)
        if (DEBUG_LOG_FILAMENT_RAW) {
            Log.d(
                TAG_FILAMENT,
                "tray $label ams=$amsId tray=$trayId loaded=$loaded type=$type remainRaw=${trayJson.opt("remain")}",
            )
        }
        val mqttTrayId = if (trayJson.has("id") && !trayJson.isNull("id")) {
            trayJson.getInt("id")
        } else {
            null
        }
        return FilamentSlot(
            label = label,
            filamentType = type,
            swatchColorHexes = traySwatch.colorHexes,
            isTranslucent = traySwatch.isTranslucent,
            colorAlpha = traySwatch.alpha,
            remainPercent = null,
            metadata = meta,
            isExternal = isExternal,
            isLoaded = loaded,
            amsId = amsId,
            trayId = trayId,
            mqttTrayId = mqttTrayId,
        )
    }

    private fun resolveFileName(json: JSONObject): String? {
        json.optString("current_print").takeIf { it.isNotBlank() }?.let { return it }
        json.optString("subtask_name").takeIf { it.isNotBlank() }?.let { return it }
        json.optString("gcode_file").takeIf { it.isNotBlank() }?.let { path ->
            return path.substringAfterLast('/')
        }
        return null
    }

}
