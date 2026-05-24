package com.chronoswing.buddydash.network

import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.PrinterMachineInfo
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.data.model.PrinterQueueSnapshot
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.PrinterMaintenanceOverview
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.util.FilamentSwatchColors
import com.chronoswing.buddydash.util.SlotInventoryInfo
import com.chronoswing.buddydash.util.SlotInventoryKey
import com.chronoswing.buddydash.util.applyInventoryToSlots
import com.chronoswing.buddydash.util.EXTERNAL_AMS_ID
import com.chronoswing.buddydash.util.externalInventoryTrayId
import com.chronoswing.buddydash.util.formatAmsSlotLabel
import com.chronoswing.buddydash.util.MaintenanceHomeIndicator
import com.chronoswing.buddydash.util.formatAmsUnitLabel
import com.chronoswing.buddydash.util.resolveMaintenanceHomeIndicator
import com.chronoswing.buddydash.util.isAmsLiteModule
import com.chronoswing.buddydash.util.isMeaningfulAmsHumidity
import com.chronoswing.buddydash.util.isMeaningfulAmsTemp
import com.chronoswing.buddydash.util.formatNozzleDiameterDisplay
import com.chronoswing.buddydash.util.resolveActiveFilamentSlot
import com.chronoswing.buddydash.util.isTrayLoaded
import com.chronoswing.buddydash.util.normalizeFilamentType
import com.chronoswing.buddydash.util.normalizeTrayColor
import com.chronoswing.buddydash.util.mergeSpoolsWithAssignments
import com.chronoswing.buddydash.util.parseInventoryByPrinter
import com.chronoswing.buddydash.util.parseLowStockThreshold
import com.chronoswing.buddydash.util.parseSpoolAssignments
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.DEBUG_LOG_SPOOL_USAGE
import com.chronoswing.buddydash.util.toUserNetworkMessage
import com.chronoswing.buddydash.util.logFullJsonPayload
import com.chronoswing.buddydash.util.logSpoolUsageFetch
import com.chronoswing.buddydash.util.TAG_SPOOL_USAGE_LINK
import com.chronoswing.buddydash.util.PrinterFilamentActivity
import com.chronoswing.buddydash.util.PrinterActivityKind
import com.chronoswing.buddydash.util.parseSpoolInventoryList
import com.chronoswing.buddydash.util.resolveActivityKind
import com.chronoswing.buddydash.util.parseSpoolUsageHistoryList
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import com.chronoswing.buddydash.util.etaDebugLogLine
import com.chronoswing.buddydash.util.parsePrinterHmsErrors
import com.chronoswing.buddydash.util.logPrinterStatusMapping
import com.chronoswing.buddydash.util.parsePrinterStatusFaultMessages
import com.chronoswing.buddydash.util.parseRemainingTimeSeconds
import com.chronoswing.buddydash.util.DEBUG_LOG_FILAMENT_USAGE
import com.chronoswing.buddydash.util.TAG_FILAMENT_USAGE
import com.chronoswing.buddydash.util.formatFilamentUsageCompact
import com.chronoswing.buddydash.util.logFilamentUsageDiscovery
import com.chronoswing.buddydash.util.queueDurationFieldCandidates
import com.chronoswing.buddydash.util.queueFilamentUsageFieldCandidates
import com.chronoswing.buddydash.util.resolveFilamentUsageFromJson
import com.chronoswing.buddydash.util.ARCHIVES_LIST_DEFAULT_LIMIT
import com.chronoswing.buddydash.util.DEBUG_LOG_ARCHIVE_REPRINT
import com.chronoswing.buddydash.util.DEBUG_LOG_ARCHIVES
import com.chronoswing.buddydash.util.TAG_ARCHIVE_REPRINT
import com.chronoswing.buddydash.util.TAG_ARCHIVES
import com.chronoswing.buddydash.util.archiveDisplayFieldsSample
import com.chronoswing.buddydash.util.logArchiveStatsDateItemDebug
import com.chronoswing.buddydash.util.parsePrintArchive
import com.chronoswing.buddydash.util.resolveQueueFilamentUsage
import com.chronoswing.buddydash.util.queueImageIdFieldCandidates
import com.chronoswing.buddydash.util.queueJsonPositiveInt
import com.chronoswing.buddydash.util.queueNameFieldCandidates
import com.chronoswing.buddydash.util.queueThumbnailHintCandidates
import com.chronoswing.buddydash.util.resolveQueueDisplayName
import com.chronoswing.buddydash.util.resolveQueueDurationSeconds
import com.chronoswing.buddydash.util.queueHasThumbnailHint
import com.chronoswing.buddydash.util.queueJsonPlateId
import com.chronoswing.buddydash.util.resolveQueueThumbnailSource
import com.chronoswing.buddydash.network.queueJobThumbnailUrl
import com.chronoswing.buddydash.util.HomeLoadTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
        private val debugLogFilamentRaw: Boolean get() = BuddyDashDebug.enabled
        private const val TAG_FILAMENT = "BuddyDash/Filament"
        private val debugLogEtaRaw: Boolean get() = BuddyDashDebug.enabled
        private const val TAG_ETA = "BuddyDash/Eta"
        private val debugLogDetailRaw: Boolean get() = BuddyDashDebug.enabled
        private const val TAG_DETAIL = "BuddyDash/Detail"
        private const val TAG_MOTION = "BuddyDash/Motion"
        private val debugLogQueue: Boolean get() = BuddyDashDebug.enabled
        private const val TAG_QUEUE = "BuddyDash/Queue"
        private const val QUEUE_STATUS_PENDING = "pending"
        private const val QUEUE_STATUS_PRINTING = "printing"
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
            }.also { result ->
                if (BuddyDashDebug.enabled) {
                    result.fold(
                        onSuccess = { HomeLoadTiming.log("fetchPrinters parsed count=${it.size}") },
                        onFailure = { Log.w(TAG_DETAIL, "fetchPrinters failed", it) },
                    )
                }
            }
        }

    /** List printers merged with live status (connected, state, HMS) from /status. */
    suspend fun fetchPrintersWithStatus(serverUrl: String, apiKey: String): Result<List<Printer>> =
        withContext(Dispatchers.IO) {
            fetchPrinters(serverUrl, apiKey).mapCatching { printers ->
                enrichPrintersForHome(serverUrl, apiKey, printers).getOrThrow()
            }
        }

    /** Per-printer activity + active tray for spool inventory printing indicators. */
    suspend fun fetchPrinterFilamentActivityById(
        serverUrl: String,
        apiKey: String,
    ): Result<Map<Int, PrinterFilamentActivity>> =
        withContext(Dispatchers.IO) {
            fetchPrinters(serverUrl, apiKey).mapCatching { printers ->
                if (printers.isEmpty()) return@mapCatching emptyMap()
                coroutineScope {
                    val semaphore = Semaphore(4)
                    printers.map { printer ->
                        async {
                            semaphore.withPermit {
                                val status = fetchPrinterStatus(
                                    serverUrl = serverUrl,
                                    apiKey = apiKey,
                                    printerId = printer.id,
                                ).getOrNull()
                                printer.id to PrinterFilamentActivity(
                                    activityKind = status?.resolveActivityKind()
                                        ?: PrinterActivityKind.Offline,
                                    activeFilamentSlot = status?.activeFilamentSlot,
                                )
                            }
                        }
                    }.awaitAll().toMap()
                }
            }
        }

    /**
     * Enriches base printer config rows with status, maintenance, and queue counts.
     * Safe to call after the list is already on screen (parallel per printer, limited concurrency).
     */
    suspend fun enrichPrintersForHome(
        serverUrl: String,
        apiKey: String,
        printers: List<Printer>,
    ): Result<List<Printer>> =
        withContext(Dispatchers.IO) {
            if (printers.isEmpty()) return@withContext Result.success(emptyList())
            runCatching {
                HomeLoadTiming.log("secondary enrichment started (count=${printers.size})")
                coroutineScope {
                    val inventoryDeferred = async {
                        fetchInventoryByPrinter(serverUrl, apiKey, printerId = null)
                            .getOrElse { emptyMap() }
                    }
                    val inventoryByPrinter = inventoryDeferred.await()
                    val concurrency = Semaphore(4)
                    printers.map { printer ->
                        async {
                            concurrency.withPermit {
                                enrichPrinterForHome(
                                    serverUrl = serverUrl,
                                    apiKey = apiKey,
                                    printer = printer,
                                    inventoryBySlot = inventoryByPrinter[printer.id],
                                )
                            }
                        }
                    }.awaitAll()
                }.also {
                    HomeLoadTiming.log("secondary enrichment finished (count=${it.size})")
                }
            }
        }

    private suspend fun enrichPrinterForHome(
        serverUrl: String,
        apiKey: String,
        printer: Printer,
        inventoryBySlot: Map<SlotInventoryKey, SlotInventoryInfo>?,
    ): Printer {
        val statusResult = fetchPrinterStatus(
            serverUrl = serverUrl,
            apiKey = apiKey,
            printerId = printer.id,
            inventoryBySlot = inventoryBySlot,
        )
        val maintenanceOverview = if (BambuddyApi.hasMaintenanceEndpoint) {
            fetchMaintenance(serverUrl, apiKey, printer.id).getOrNull()
        } else {
            null
        }
        val maintenanceIndicator = maintenanceOverview
            ?.items
            ?.let { resolveMaintenanceHomeIndicator(it) }
            ?: MaintenanceHomeIndicator.None
        val pendingQueueCount = if (BambuddyApi.hasQueueEndpoint) {
            fetchPrintQueue(serverUrl, apiKey, printer.id)
                .getOrNull()
                ?.size
                ?: 0
        } else {
            0
        }
        return printer.copy(
            liveStatus = statusResult.getOrNull(),
            maintenanceIndicator = maintenanceIndicator,
            maintenanceItems = maintenanceOverview?.items.orEmpty(),
            maintenanceTotalPrintHours = maintenanceOverview?.totalPrintHours,
            pendingQueueCount = pendingQueueCount,
        )
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
        postBody: String? = null,
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
            "POST" -> {
                val body = (postBody ?: "{}").toRequestBody("application/json".toMediaType())
                requestBuilder.post(body).build()
            }
            else -> requestBuilder.get().build()
        }

        val result = runCatching {
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
        return result.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(Exception(it.toUserNetworkMessage("Request failed"))) },
        )
    }

    /** Config row from GET /api/v1/printers — no live connection/state fields. */
    private fun parsePrinterFromConfig(json: JSONObject): Printer = Printer(
        id = json.getInt("id"),
        name = json.optString("name", "Printer ${json.getInt("id")}"),
        model = json.optString("model").takeIf { it.isNotBlank() },
    )

    private fun parsePrinterMachineInfo(json: JSONObject): PrinterMachineInfo =
        PrinterMachineInfo(
            serialNumber = json.optString("serial_number").takeIf { it.isNotBlank() },
            ipAddress = json.optString("ip_address").takeIf { it.isNotBlank() },
            model = json.optString("model").takeIf { it.isNotBlank() },
            location = json.optString("location").takeIf { it.isNotBlank() },
            updatedAtIso = json.optString("updated_at").takeIf { it.isNotBlank() },
            nozzleCount = json.optInt("nozzle_count").takeIf {
                json.has("nozzle_count") && !json.isNull("nozzle_count") && it > 0
            },
            autoArchiveEnabled = if (json.has("auto_archive") && !json.isNull("auto_archive")) {
                json.getBoolean("auto_archive")
            } else {
                null
            },
        )

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

    suspend fun clearHmsErrors(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasHmsClearEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("HMS clear endpoint not found"),
            )
        }
        runApiCall(
            serverUrl = serverUrl,
            apiKey = apiKey,
            path = BambuddyApi.hmsClearPath(printerId),
            method = "POST",
        ) { Unit }
    }

    suspend fun performMaintenance(
        serverUrl: String,
        apiKey: String,
        itemId: Int,
        notes: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasMaintenancePerformEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("Maintenance perform endpoint not found"),
            )
        }
        val body = JSONObject().apply {
            if (!notes.isNullOrBlank()) put("notes", notes)
        }
        if (debugLogDetailRaw) {
            Log.d(TAG_DETAIL, "POST ${BambuddyApi.maintenancePerformPath(itemId)} body=$body")
        }
        runApiCall(
            serverUrl = serverUrl,
            apiKey = apiKey,
            path = BambuddyApi.maintenancePerformPath(itemId),
            method = "POST",
            postBody = body.toString(),
        ) { Unit }
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

    /** Pending queue jobs for [printerId] (excludes status=printing). */
    suspend fun fetchPrintQueue(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<List<PrintQueueJob>> =
        fetchPrinterQueueSnapshot(serverUrl, apiKey, printerId).map { it.upcoming }

    /** Full queue snapshot: pending jobs plus optional in-progress printing job. */
    suspend fun fetchPrinterQueueSnapshot(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<PrinterQueueSnapshot> =
        withContext(Dispatchers.IO) {
            if (!BambuddyApi.hasQueueEndpoint) {
                return@withContext Result.failure(
                    UnsupportedOperationException("Queue endpoint not found"),
                )
            }
            runApiCall(serverUrl, apiKey, BambuddyApi.queuePath(printerId)) { body ->
                parsePrintQueueResponse(body)
            }
        }

    /**
     * Spool inventory for the Spools tab: list spools, slot assignments, and low-stock threshold.
     */
    suspend fun fetchSpoolInventory(
        serverUrl: String,
        apiKey: String,
    ): Result<List<SpoolInventoryItem>> =
        withContext(Dispatchers.IO) {
            if (!BambuddyApi.hasSpoolInventoryEndpoint) {
                if (BuddyDashDebug.enabled) {
                    Log.w(TAG_SPOOL_USAGE_LINK, "fetchSpoolInventory: endpoint not available")
                }
                return@withContext Result.failure(
                    UnsupportedOperationException("Spool inventory endpoint not found"),
                )
            }
            if (BuddyDashDebug.enabled) {
                Log.d(
                    TAG_SPOOL_USAGE_LINK,
                    "fetchSpoolInventory: GET ${BambuddyApi.inventorySpoolsPath()}",
                )
            }
            runCatching {
                coroutineScope {
                    val settingsDeferred = async {
                        runApiCall(serverUrl, apiKey, BambuddyApi.SETTINGS_PATH) { body ->
                            JSONObject(body)
                        }.getOrNull()
                    }
                    val spoolsDeferred = async {
                        runApiCall(serverUrl, apiKey, BambuddyApi.inventorySpoolsPath()) { body ->
                            body
                        }.getOrThrow()
                    }
                    val assignmentsDeferred = async {
                        runApiCall(serverUrl, apiKey, BambuddyApi.inventoryAssignmentsPath()) { body ->
                            body
                        }.getOrThrow()
                    }
                    val globalThreshold = parseLowStockThreshold(settingsDeferred.await())
                    val spools = parseSpoolInventoryList(spoolsDeferred.await(), globalThreshold)
                    val assignments = parseSpoolAssignments(assignmentsDeferred.await())
                    val merged = mergeSpoolsWithAssignments(spools, assignments)
                        .sortedWith(
                            compareBy<SpoolInventoryItem> { it.assignment == null }
                                .thenBy { it.displayName.lowercase() },
                        )
                    if (BuddyDashDebug.enabled) {
                        Log.d(
                            TAG_SPOOL_USAGE_LINK,
                            "fetchSpoolInventory: parsed=${spools.size} assignments=${assignments.size} " +
                                "merged=${merged.size}",
                        )
                    }
                    merged
                }
            }.also { result ->
                if (BuddyDashDebug.enabled) {
                    result.fold(
                        onSuccess = { Log.d(TAG_SPOOL_USAGE_LINK, "fetchSpoolInventory: success count=${it.size}") },
                        onFailure = { Log.w(TAG_SPOOL_USAGE_LINK, "fetchSpoolInventory: failed", it) },
                    )
                }
            }
        }

    /**
     * Exact usage history for one spool (GET /api/v1/inventory/spools/{spool_id}/usage).
     * Returns an empty list when the endpoint is missing or the request fails.
     */
    suspend fun fetchSpoolUsageHistory(
        serverUrl: String,
        apiKey: String,
        spoolId: Int,
        limit: Int = 50,
    ): Result<List<SpoolUsageEntry>> =
        withContext(Dispatchers.IO) {
            if (!BambuddyApi.hasSpoolUsageEndpoint) {
                return@withContext Result.success(emptyList())
            }
            val path = BambuddyApi.spoolUsagePath(spoolId, limit)
            runApiCall(serverUrl, apiKey, path) { body ->
                val entries = parseSpoolUsageHistoryList(body)
                logSpoolUsageFetch(spoolId, path, rawBody = body, entries = entries)
                entries
            }.recover { error ->
                logSpoolUsageFetch(
                    spoolId = spoolId,
                    path = path,
                    rawBody = null,
                    entries = emptyList(),
                    error = error,
                )
                emptyList()
            }
        }

    suspend fun fetchArchives(
        serverUrl: String,
        apiKey: String,
        limit: Int = ARCHIVES_LIST_DEFAULT_LIMIT,
        offset: Int = 0,
        printerId: Int? = null,
    ): Result<List<PrintArchive>> =
        withContext(Dispatchers.IO) {
            if (!BambuddyApi.hasArchivesEndpoint) {
                return@withContext Result.failure(
                    UnsupportedOperationException("Archives endpoint not found"),
                )
            }
            runCatching {
                val printers = fetchPrinters(serverUrl, apiKey).getOrElse { emptyList() }
                val namesById = printers.associate { it.id to it.name }
                val modelsById = printers.mapNotNull { p -> p.model?.let { p.id to it } }.toMap()
                val path = BambuddyApi.archivesPath(
                    limit = limit,
                    offset = offset,
                    printerId = printerId,
                )
                val archives = runApiCall(serverUrl, apiKey, path) { body ->
                    if (DEBUG_LOG_SPOOL_USAGE) {
                        logFullJsonPayload(TAG_SPOOL_USAGE_LINK, "archivesListRaw", body)
                    }
                    parseArchivesList(body, namesById, modelsById)
                }.getOrThrow()
                if (DEBUG_LOG_ARCHIVES) {
                    Log.d(TAG_ARCHIVES, "GET $path count=${archives.size}")
                    archives.take(3).forEach { archive ->
                        Log.d(
                            TAG_ARCHIVES,
                            "archive id=${archive.id} name=${archive.displayName} " +
                                "status=${archive.statusRaw} kind=${archive.resultKind} " +
                                "printer=${archive.printerName} duration=${archive.durationSeconds} " +
                                "filament=${archive.filamentUsage?.weightGrams}",
                        )
                    }
                }
                archives.sortedByDescending { it.completedAtIso ?: it.startedAtIso.orEmpty() }
            }
        }

    suspend fun fetchArchive(
        serverUrl: String,
        apiKey: String,
        archiveId: Int,
    ): Result<PrintArchive> =
        withContext(Dispatchers.IO) {
            if (!BambuddyApi.hasArchivesEndpoint) {
                return@withContext Result.failure(
                    UnsupportedOperationException("Archives endpoint not found"),
                )
            }
            runCatching {
                val printers = fetchPrinters(serverUrl, apiKey).getOrElse { emptyList() }
                val namesById = printers.associate { it.id to it.name }
                val modelsById = printers.mapNotNull { p -> p.model?.let { p.id to it } }.toMap()
                val path = BambuddyApi.archiveDetailPath(archiveId)
                runApiCall(serverUrl, apiKey, path) { body ->
                    val json = JSONObject(body)
                    if (DEBUG_LOG_ARCHIVES) {
                        Log.d(TAG_ARCHIVES, "GET $path fields=${archiveDisplayFieldsSample(json)}")
                    }
                    parsePrintArchive(json, namesById, modelsById)
                        ?: throw IllegalStateException("Invalid archive response")
                }.getOrThrow()
            }
        }

    /**
     * Queue an archive for printing via POST /api/v1/queue/ (PrintQueueItemCreate).
     * Uses manual_start so the job stays queued until started from the printer UI.
     */
    suspend fun addArchiveToQueue(
        serverUrl: String,
        apiKey: String,
        archiveId: Int,
        printerId: Int,
        quantity: Int = 1,
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasQueueAddEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("Queue add endpoint not found"),
            )
        }
        val path = BambuddyApi.QUEUE_ADD_PATH
        val payload = JSONObject().apply {
            put("archive_id", archiveId)
            put("printer_id", printerId)
            put("quantity", quantity.coerceIn(1, 99))
            put("manual_start", true)
        }
        if (DEBUG_LOG_ARCHIVE_REPRINT) {
            Log.d(
                TAG_ARCHIVE_REPRINT,
                "endpoint=POST $path (not ${BambuddyApi.ARCHIVE_REPRINT_PATH}) payload=$payload",
            )
        }
        runApiCall(serverUrl, apiKey, path, method = "POST", postBody = payload.toString()) { body ->
            if (DEBUG_LOG_ARCHIVE_REPRINT) {
                Log.d(TAG_ARCHIVE_REPRINT, "response=$body")
            }
            val json = JSONObject(body)
            val itemId = json.optInt("id", -1)
            if (itemId < 0) {
                throw IllegalStateException("Invalid queue item response")
            }
            itemId
        }
    }.onFailure { error ->
        if (DEBUG_LOG_ARCHIVE_REPRINT) {
            Log.e(TAG_ARCHIVE_REPRINT, "queueArchive failed", error)
        }
    }

    suspend fun startQueueItem(
        serverUrl: String,
        apiKey: String,
        queueItemId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasQueueStartEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("Queue start endpoint not found"),
            )
        }
        val path = BambuddyApi.queueItemStartPath(queueItemId)
        if (DEBUG_LOG_ARCHIVE_REPRINT) {
            Log.d(TAG_ARCHIVE_REPRINT, "endpoint=POST $path")
        }
        runApiCall(serverUrl, apiKey, path, method = "POST", postBody = "{}") { body ->
            if (DEBUG_LOG_ARCHIVE_REPRINT) {
                Log.d(TAG_ARCHIVE_REPRINT, "startQueueItem response=$body")
            }
        }
    }.onFailure { error ->
        if (DEBUG_LOG_ARCHIVE_REPRINT) {
            Log.e(TAG_ARCHIVE_REPRINT, "startQueueItem failed itemId=$queueItemId", error)
        }
    }

    private fun parseArchivesList(
        body: String,
        printerNamesById: Map<Int, String>,
        printerModelsById: Map<Int, String>,
    ): List<PrintArchive> {
        val array = JSONArray(body)
        if (DEBUG_LOG_ARCHIVES) {
            Log.d(TAG_ARCHIVES, "parseArchivesList rawCount=${array.length()}")
            if (array.length() > 0) {
                val sample = array.getJSONObject(0)
                Log.d(TAG_ARCHIVES, "fieldsSample=${archiveDisplayFieldsSample(sample)}")
            }
        }
        return buildList {
            for (i in 0 until array.length()) {
                val json = array.optJSONObject(i) ?: continue
                parsePrintArchive(json, printerNamesById, printerModelsById)?.let { archive ->
                    if (DEBUG_LOG_ARCHIVES) {
                        logArchiveStatsDateItemDebug(archive.displayName, json, archive)
                    }
                    add(archive)
                }
            }
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

    /** Stop server-side camera stream when the viewer closes (OpenAPI: POST …/camera/stop). */
    suspend fun stopCameraStream(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        postPrinterAction(serverUrl, apiKey, BambuddyApi.cameraStopPath(printerId))
    }

    suspend fun fetchPrinterMachineInfo(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<PrinterMachineInfo> =
        withContext(Dispatchers.IO) {
            if (!BambuddyApi.hasPrinterDetailEndpoint) {
                return@withContext Result.failure(
                    UnsupportedOperationException("Printer detail endpoint not found"),
                )
            }
            runApiCall(serverUrl, apiKey, BambuddyApi.printerDetailPath(printerId)) { body ->
                parsePrinterMachineInfo(JSONObject(body))
            }
        }

    suspend fun homeAxes(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasHomeAxesEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("Home axes endpoint not found"),
            )
        }
        postPrinterAction(serverUrl, apiKey, BambuddyApi.homeAxesPath(printerId))
    }

    suspend fun amsLoadFilament(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
        trayId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasAmsLoadEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("AMS load endpoint not found"),
            )
        }
        postPrinterAction(serverUrl, apiKey, BambuddyApi.amsLoadPath(printerId, trayId))
    }

    suspend fun amsUnloadFilament(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasAmsUnloadEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("AMS unload endpoint not found"),
            )
        }
        postPrinterAction(serverUrl, apiKey, BambuddyApi.amsUnloadPath(printerId))
    }

    suspend fun fetchInventoryForPrinter(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): Result<Map<SlotInventoryKey, SlotInventoryInfo>> =
        withContext(Dispatchers.IO) {
            fetchInventoryByPrinter(serverUrl, apiKey, printerId)
                .map { it[printerId].orEmpty() }
        }

    suspend fun assignSpoolToSlot(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
        amsId: Int,
        trayId: Int,
        spoolId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasInventoryAssignEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("Inventory assign endpoint not found"),
            )
        }
        val body = JSONObject().apply {
            put("spool_id", spoolId)
            put("printer_id", printerId)
            put("ams_id", amsId)
            put("tray_id", trayId)
        }
        runApiCall(
            serverUrl = serverUrl,
            apiKey = apiKey,
            path = BambuddyApi.INVENTORY_ASSIGNMENTS_PATH,
            method = "POST",
            postBody = body.toString(),
        ) { Unit }
    }

    suspend fun unassignSpoolFromSlot(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
        amsId: Int,
        trayId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BambuddyApi.hasInventoryUnassignEndpoint) {
            return@withContext Result.failure(
                UnsupportedOperationException("Inventory unassign endpoint not found"),
            )
        }
        runApiCall(
            serverUrl = serverUrl,
            apiKey = apiKey,
            path = BambuddyApi.inventoryUnassignPath(printerId, amsId, trayId),
            method = "DELETE",
        ) { Unit }
    }

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
        if (debugLogDetailRaw) {
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
        val hmsErrorsArray = json.optJSONArray("hms_errors")
        if (BuddyDashDebug.enabled) {
            val hmsRaw = json.opt("hms_errors")
            Log.d(
                TAG_DETAIL,
                "HMS printer=${json.optInt("id", -1)} " +
                    "hms_errors_present=${json.has("hms_errors")} " +
                    "hms_errors_null=${json.isNull("hms_errors")} " +
                    "hms_raw_type=${hmsRaw?.javaClass?.simpleName} " +
                    "hms_array_len=${hmsErrorsArray?.length() ?: -1} " +
                    "hms_raw=${hmsRaw?.toString()?.take(300)}",
            )
        }
        val hmsErrors = parsePrinterHmsErrors(hmsErrorsArray)
        val statusFaultMessages = parsePrinterStatusFaultMessages(json)

        val awaitingPlateClear = if (json.has("awaiting_plate_clear") && !json.isNull("awaiting_plate_clear")) {
            json.getBoolean("awaiting_plate_clear")
        } else {
            null
        }

        val amsUnits = parseAmsUnits(json)
        val operational = parseOperationalFields(json, temperatures)
        val filament = parseFilamentSlots(json, inventoryBySlot)
        val metadata = parsePrinterMetadata(json)
        val filamentUsage = resolveFilamentUsageFromJson(json)
        if (DEBUG_LOG_FILAMENT_USAGE) {
            logFilamentUsageDiscovery(
                tag = TAG_FILAMENT_USAGE,
                context = "printerStatus printer=${json.optInt("id", -1)}",
                json = json,
                resolved = filamentUsage,
            )
        }

        val status = PrinterStatus(
            connected = json.optBoolean("connected", false),
            rawState = json.optString("state").takeIf { it.isNotBlank() },
            progress = json.optDouble("progress")
                .takeIf { json.has("progress") && !json.isNull("progress") }
                ?.toFloat(),
            fileName = resolveFileName(json),
            currentPrint = json.optString("current_print").takeIf { it.isNotBlank() },
            subtaskName = json.optString("subtask_name").takeIf { it.isNotBlank() },
            gcodeFile = json.optString("gcode_file").takeIf { it.isNotBlank() },
            coverUrl = json.optString("cover_url").takeIf { it.isNotBlank() },
            remainingTimeSeconds = run {
                val rawState = json.optString("state").takeIf { it.isNotBlank() }
                val seconds = parseRemainingTimeSeconds(json)
                if (debugLogEtaRaw) {
                    etaDebugLogLine(json, rawState, seconds)?.let { line ->
                        Log.d(TAG_ETA, line)
                    }
                }
                seconds
            },
            nozzleTemp = temperatures?.optDouble("nozzle"),
            bedTemp = temperatures?.optDouble("bed"),
            chamberTemp = operational.chamberTemp,
            hmsErrors = hmsErrors,
            statusFaultMessages = statusFaultMessages,
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
            developerMode = operational.developerMode,
            nozzleDiameterDisplay = metadata.nozzleDiameterDisplay,
            filamentUsage = filamentUsage,
        )
        logPrinterStatusMapping(
            json = json,
            status = status,
            context = "printer=${json.optInt("id", -1)}",
        )
        return status
    }

    private data class PrinterMetadataFields(
        val nozzleDiameterDisplay: String?,
    )

    private fun parsePrinterMetadata(json: JSONObject): PrinterMetadataFields {
        val activeExtruder = json.optInt("active_extruder", 0)
        val nozzleDiameterDisplay = parseNozzleDiameterFromStatus(json, activeExtruder)

        if (debugLogDetailRaw) {
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
        val developerMode: Boolean?,
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
        val developerMode = if (json.has("developer_mode") && !json.isNull("developer_mode")) {
            json.getBoolean("developer_mode")
        } else {
            null
        }

        if (debugLogDetailRaw) {
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
            developerMode = developerMode,
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
            val moduleType = unit.optString("module_type").takeIf { it.isNotBlank() }
            val isLite = isAmsLiteModule(moduleType)
            val tempRaw = unit.optDouble("temp").takeIf { unit.has("temp") && !unit.isNull("temp") }
            val humidityRaw = unit.optInt("humidity").takeIf { unit.has("humidity") && !unit.isNull("humidity") }
            val temp = tempRaw.takeIf { isMeaningfulAmsTemp(it, isLite) }
            val humidity = humidityRaw.takeIf { isMeaningfulAmsHumidity(it, isLite) }
            if (temp == null && humidity == null) continue
            units.add(
                AmsUnitInfo(
                    amsId = amsId,
                    label = formatAmsUnitLabel(amsId),
                    moduleType = moduleType,
                    tempC = temp,
                    humidityPercent = humidity,
                ),
            )
            if (debugLogDetailRaw) {
                Log.d(
                    TAG_DETAIL,
                    "ams unit $amsId module_type=$moduleType lite=$isLite temp=$temp humidity=$humidity " +
                        "raw=($tempRaw,$humidityRaw)",
                )
            }
        }
        return units
    }

    private fun parseMaintenanceOverview(json: JSONObject): PrinterMaintenanceOverview {
        val itemsArray = json.optJSONArray("maintenance_items") ?: JSONArray()
        val items = List(itemsArray.length()) { index ->
            val item = itemsArray.getJSONObject(index)
            val isDue = item.optBoolean("is_due", false)
            val isWarning = item.optBoolean("is_warning", false)
            val hoursUntilDue = item.optDouble("hours_until_due").takeIf {
                item.has("hours_until_due") && !item.isNull("hours_until_due")
            }
            val daysUntilDue = item.optDouble("days_until_due").takeIf {
                item.has("days_until_due") && !item.isNull("days_until_due")
            }
            val intervalHours = item.optDouble("interval_hours").takeIf {
                item.has("interval_hours") && !item.isNull("interval_hours")
            }
            val hoursSinceMaintenance = item.optDouble("hours_since_maintenance").takeIf {
                item.has("hours_since_maintenance") && !item.isNull("hours_since_maintenance")
            }
            val intervalType = item.optString("interval_type").takeIf { it.isNotBlank() }
            if (debugLogDetailRaw) {
                Log.d(
                    TAG_DETAIL,
                    "maintenance raw id=${item.optInt("id")} name=${item.optString("maintenance_type_name")} " +
                        "is_due=$isDue is_warning=$isWarning hours_until_due=$hoursUntilDue days_until_due=$daysUntilDue " +
                        "interval_hours=$intervalHours hours_since=$hoursSinceMaintenance interval_type=$intervalType",
                )
            }
            MaintenanceItem(
                id = item.optInt("id", -1),
                name = item.optString("maintenance_type_name", "Maintenance"),
                isDue = isDue,
                isWarning = isWarning,
                enabled = item.optBoolean("enabled", true),
                hoursUntilDue = hoursUntilDue,
                daysUntilDue = daysUntilDue,
                intervalHours = intervalHours,
                hoursSinceMaintenance = hoursSinceMaintenance,
                intervalType = intervalType,
            )
        }
        val totalPrintHours = json.optDouble("total_print_hours")
            .takeIf { json.has("total_print_hours") && !json.isNull("total_print_hours") && it > 0.0 }
        if (debugLogDetailRaw) {
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
        if (debugLogFilamentRaw) {
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
                if (debugLogDetailRaw) {
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
            logColors = debugLogFilamentRaw,
        )
        val activeKey = resolveActiveFilamentSlot(
            statusJson = json,
            slots = enriched,
            logRaw = debugLogFilamentRaw,
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
        if (debugLogFilamentRaw) {
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

    private fun parsePrintQueueResponse(body: String): PrinterQueueSnapshot {
        val array = JSONArray(body)
        val parsed = mutableListOf<Triple<PrintQueueJob, String, JSONObject>>()
        for (index in 0 until array.length()) {
            val json = array.getJSONObject(index)
            val status = json.optString("status", "")
            val job = parsePrintQueueItem(json)
            parsed.add(Triple(job, status, json))
        }
        if (debugLogQueue) {
            Log.d(
                TAG_QUEUE,
                "GET queue rawCount=${array.length()} " +
                    "statuses=${parsed.map { it.second }.distinct()} " +
                    "fieldsSample=${parsed.firstOrNull()?.third?.names()?.let { arr ->
                        (0 until arr.length()).map { i -> arr.getString(i) }.take(12)
                    }}",
            )
            parsed.forEach { (job, status, json) ->
                Log.d(TAG_QUEUE, "item raw=${json.toString()}")
                Log.d(TAG_QUEUE, "item id=${job.id} pos=${job.position} status=$status resolvedName=${job.displayName}")
                Log.d(TAG_QUEUE, "nameFields=${queueNameFieldCandidates(json)}")
                Log.d(TAG_QUEUE, "thumbHints=${queueThumbnailHintCandidates(json)}")
                Log.d(TAG_QUEUE, "imageIds=${queueImageIdFieldCandidates(json)}")
                val thumbResolution = resolveQueueThumbnailSource(job)
                Log.d(
                    TAG_QUEUE,
                    "thumbSource=${thumbResolution.source} apiPath=${thumbResolution.apiPath} " +
                        "displayName=${job.displayName}",
                )
                Log.d(
                    TAG_QUEUE,
                    "durationFields=${queueDurationFieldCandidates(json)} resolvedSeconds=${job.estimatedDurationSeconds}",
                )
                if (DEBUG_LOG_FILAMENT_USAGE) {
                    Log.d(
                        TAG_FILAMENT_USAGE,
                        "queueItem id=${job.id} status=$status " +
                            "filamentFields=${queueFilamentUsageFieldCandidates(json)} " +
                            "display=${formatFilamentUsageCompact(job.filamentUsage)}",
                    )
                    logFilamentUsageDiscovery(
                        tag = TAG_FILAMENT_USAGE,
                        context = "queueItem id=${job.id} status=$status",
                        json = json,
                        resolved = job.filamentUsage,
                    )
                }
            }
        }
        val printing = parsed
            .filter { (_, status, _) -> status.equals(QUEUE_STATUS_PRINTING, ignoreCase = true) }
            .map { (job, _, _) -> job }
            .minByOrNull { it.position }
        val upcoming = parsed
            .filter { (_, status, _) -> status == QUEUE_STATUS_PENDING }
            .map { (job, _, _) -> job }
            .sortedBy { it.position }
        if (debugLogQueue) {
            Log.d(
                TAG_QUEUE,
                "upcomingCount=${upcoming.size} printing=${printing?.id} " +
                    "(pending + optional printing job for filament metadata)",
            )
        }
        return PrinterQueueSnapshot(upcoming = upcoming, printing = printing)
    }

    private fun parsePrintQueueItem(json: JSONObject): PrintQueueJob = PrintQueueJob(
        id = json.getInt("id"),
        position = json.optInt("position", 0),
        displayName = resolveQueueDisplayName(json),
        hasLibraryThumbnail = queueHasThumbnailHint(json, "library_file_thumbnail"),
        hasArchiveThumbnail = queueHasThumbnailHint(json, "archive_thumbnail"),
        libraryFileId = queueJsonPositiveInt(json, "library_file_id"),
        archiveId = queueJsonPositiveInt(json, "archive_id"),
        plateId = queueJsonPlateId(json),
        estimatedDurationSeconds = resolveQueueDurationSeconds(json),
        filamentUsage = resolveQueueFilamentUsage(json),
    )

}
