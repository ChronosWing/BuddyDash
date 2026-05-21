package com.chronoswing.buddydash.network

import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BambuddyApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Server liveness — Bambuddy exposes GET /health at the app root (not /api/health). */
    suspend fun checkServerHealth(serverUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            val baseUrl = normalizeBaseUrl(serverUrl) ?: return@withContext Result.failure(
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
            runApiCall(serverUrl, apiKey, "/api/v1/printers/") { body ->
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
                coroutineScope {
                    printers.map { printer ->
                        async {
                            val statusResult = fetchPrinterStatus(serverUrl, apiKey, printer.id)
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
    ): Result<PrinterStatus> =
        withContext(Dispatchers.IO) {
            runApiCall(serverUrl, apiKey, "/api/v1/printers/$printerId/status") { body ->
                parsePrinterStatus(JSONObject(body))
            }
        }

    private inline fun <T> runApiCall(
        serverUrl: String,
        apiKey: String,
        path: String,
        parse: (String) -> T,
    ): Result<T> {
        val baseUrl = normalizeBaseUrl(serverUrl)
        if (baseUrl == null) {
            return Result.failure(IllegalArgumentException("Server URL is required"))
        }
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return Result.failure(IllegalArgumentException("API key is required"))
        }

        val request = Request.Builder()
            .url("$baseUrl$path")
            .header("X-API-Key", trimmedKey)
            .get()
            .build()

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

    private fun normalizeBaseUrl(serverUrl: String): String? {
        var trimmed = serverUrl.trim().trimEnd('/')
        if (trimmed.endsWith("/api/v1")) {
            trimmed = trimmed.removeSuffix("/api/v1")
        }
        return trimmed.takeIf { it.isNotEmpty() }
    }

    /** Config row from GET /api/v1/printers — no live connection/state fields. */
    private fun parsePrinterFromConfig(json: JSONObject): Printer = Printer(
        id = json.getInt("id"),
        name = json.optString("name", "Printer ${json.getInt("id")}"),
        model = json.optString("model").takeIf { it.isNotBlank() },
        status = null,
        isOnline = null,
    )

    private fun mergePrinterWithStatus(printer: Printer, status: PrinterStatus?): Printer {
        if (status == null) return printer
        val displayStatus = when {
            status.hmsErrorCount > 0 -> "HMS error"
            !status.connected -> "Offline"
            else -> formatStateLabel(status.state)
        }
        return printer.copy(
            status = displayStatus,
            isOnline = status.connected,
        )
    }

    private fun parsePrinterStatus(json: JSONObject): PrinterStatus {
        val temperatures = json.optJSONObject("temperatures")
        val hmsErrors = json.optJSONArray("hms_errors")

        return PrinterStatus(
            connected = json.optBoolean("connected", false),
            state = json.optString("state").takeIf { it.isNotBlank() },
            progress = json.optDouble("progress")
                .takeIf { json.has("progress") && !json.isNull("progress") }
                ?.toFloat(),
            fileName = resolveFileName(json),
            remainingTimeSeconds = json.optInt("remaining_time")
                .takeIf { json.has("remaining_time") && !json.isNull("remaining_time") },
            nozzleTemp = temperatures?.optDouble("nozzle"),
            bedTemp = temperatures?.optDouble("bed"),
            hmsErrorCount = hmsErrors?.length() ?: 0,
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

    private fun formatStateLabel(state: String?): String? {
        if (state.isNullOrBlank()) return null
        return state.lowercase().replaceFirstChar { it.uppercase() }
    }
}
