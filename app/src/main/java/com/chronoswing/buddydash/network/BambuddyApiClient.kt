package com.chronoswing.buddydash.network

import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.util.FilamentSwatchColors
import com.chronoswing.buddydash.util.SlotInventoryInfo
import com.chronoswing.buddydash.util.SlotInventoryKey
import com.chronoswing.buddydash.util.applyInventoryToSlots
import com.chronoswing.buddydash.util.EXTERNAL_AMS_ID
import com.chronoswing.buddydash.util.externalInventoryTrayId
import com.chronoswing.buddydash.util.formatAmsSlotLabel
import com.chronoswing.buddydash.util.isTrayLoaded
import com.chronoswing.buddydash.util.normalizeFilamentType
import com.chronoswing.buddydash.util.normalizeTrayColor
import com.chronoswing.buddydash.util.parseInventoryByPrinter
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

        return PrinterStatus(
            connected = json.optBoolean("connected", false),
            rawState = json.optString("state").takeIf { it.isNotBlank() },
            progress = json.optDouble("progress")
                .takeIf { json.has("progress") && !json.isNull("progress") }
                ?.toFloat(),
            fileName = resolveFileName(json),
            remainingTimeSeconds = json.optInt("remaining_time")
                .takeIf { json.has("remaining_time") && !json.isNull("remaining_time") },
            nozzleTemp = temperatures?.optDouble("nozzle"),
            bedTemp = temperatures?.optDouble("bed"),
            hmsErrorCount = hmsErrors?.length() ?: 0,
            awaitingPlateClear = awaitingPlateClear,
            filamentSlots = parseFilamentSlots(json, inventoryBySlot),
        )
    }

    private fun parseFilamentSlots(
        json: JSONObject,
        inventoryBySlot: Map<SlotInventoryKey, SlotInventoryInfo>,
    ): List<FilamentSlot> {
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
        return applyInventoryToSlots(
            slots = slots,
            inventoryBySlot = inventoryBySlot,
            printerName = printerName.ifBlank { "printer ${json.optInt("id", -1)}" },
            logColors = DEBUG_LOG_FILAMENT_RAW,
        )
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
