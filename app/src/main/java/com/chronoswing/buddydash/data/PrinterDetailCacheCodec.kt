package com.chronoswing.buddydash.data

import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.data.model.PrinterMachineInfo
import com.chronoswing.buddydash.data.model.PrinterSmartPlugState
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.data.model.SmartPlugConfig
import com.chronoswing.buddydash.data.model.SmartPlugEnergyReading
import com.chronoswing.buddydash.data.model.SmartPlugLiveStatus
import com.chronoswing.buddydash.data.model.SmartOutletPowerState
import com.chronoswing.buddydash.data.model.parseSmartOutletPowerState
import org.json.JSONArray
import org.json.JSONObject

data class PrinterDetailCacheSnapshot(
    val serverUrl: String,
    val printerId: Int,
    val printerName: String,
    val printerModel: String?,
    val lastUpdatedAtMillis: Long,
    val status: PrinterStatus?,
    val maintenanceItems: List<MaintenanceItem>,
    val totalPrintHours: Double?,
    val queueUpcoming: List<PrintQueueJob>,
    val machineInfo: PrinterMachineInfo?,
    val smartPlugState: PrinterSmartPlugState? = null,
    val printingQueueJobId: Int?,
)

object PrinterDetailCacheCodec {
    private const val VERSION = 1

    fun encode(snapshot: PrinterDetailCacheSnapshot): String =
        JSONObject()
            .put("version", VERSION)
            .put("server_url", snapshot.serverUrl)
            .put("printer_id", snapshot.printerId)
            .put("printer_name", snapshot.printerName)
            .putOptString("printer_model", snapshot.printerModel)
            .put("last_updated_at_millis", snapshot.lastUpdatedAtMillis)
            .putOptObject("status", snapshot.status?.let { HomePrintersCacheCodec.encodePrinterStatusForCache(it) })
            .put("maintenance_items", encodeMaintenanceItems(snapshot.maintenanceItems))
            .putOptDouble("total_print_hours", snapshot.totalPrintHours)
            .put("queue_upcoming", encodeQueueJobs(snapshot.queueUpcoming))
            .putOptObject("machine_info", snapshot.machineInfo?.let { encodeMachineInfo(it) })
            .putOptObject("smart_plug", snapshot.smartPlugState?.let { encodeSmartPlugState(it) })
            .putOptInt("printing_queue_job_id", snapshot.printingQueueJobId)
            .toString()

    fun decode(json: String): PrinterDetailCacheSnapshot? {
        return try {
            val root = JSONObject(json)
            if (root.optInt("version", 0) != VERSION) return null
            val serverUrl = root.optString("server_url").takeIf { it.isNotBlank() } ?: return null
            val printerId = root.optInt("printer_id", -1)
            if (printerId < 0) return null
            val lastUpdated = root.optLong("last_updated_at_millis", 0L).takeIf { it > 0L } ?: return null
            PrinterDetailCacheSnapshot(
                serverUrl = serverUrl,
                printerId = printerId,
                printerName = root.optString("printer_name", ""),
                printerModel = root.optString("printer_model").takeIf { it.isNotBlank() },
                lastUpdatedAtMillis = lastUpdated,
                status = root.optJSONObject("status")?.let { HomePrintersCacheCodec.decodePrinterStatusForCache(it) },
                maintenanceItems = decodeMaintenanceItems(root.optJSONArray("maintenance_items")),
                totalPrintHours = root.optNullableDouble("total_print_hours"),
                queueUpcoming = decodeQueueJobs(root.optJSONArray("queue_upcoming")),
                machineInfo = root.optJSONObject("machine_info")?.let { decodeMachineInfo(it) },
                smartPlugState = root.optJSONObject("smart_plug")?.let { decodeSmartPlugState(it) },
                printingQueueJobId = root.optNullableInt("printing_queue_job_id"),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeMaintenanceItems(items: List<MaintenanceItem>): JSONArray =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject()
                        .put("id", item.id)
                        .put("name", item.name)
                        .put("is_due", item.isDue)
                        .put("is_warning", item.isWarning)
                        .put("enabled", item.enabled)
                        .putOptDouble("hours_until_due", item.hoursUntilDue)
                        .putOptDouble("days_until_due", item.daysUntilDue)
                        .putOptDouble("interval_hours", item.intervalHours)
                        .putOptDouble("hours_since_maintenance", item.hoursSinceMaintenance)
                        .putOptString("interval_type", item.intervalType),
                )
            }
        }

    private fun decodeMaintenanceItems(array: JSONArray?): List<MaintenanceItem> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    MaintenanceItem(
                        id = obj.optInt("id", 0),
                        name = obj.optString("name", ""),
                        isDue = obj.optBoolean("is_due", false),
                        isWarning = obj.optBoolean("is_warning", false),
                        enabled = obj.optBoolean("enabled", true),
                        hoursUntilDue = obj.optNullableDouble("hours_until_due"),
                        daysUntilDue = obj.optNullableDouble("days_until_due"),
                        intervalHours = obj.optNullableDouble("interval_hours"),
                        hoursSinceMaintenance = obj.optNullableDouble("hours_since_maintenance"),
                        intervalType = obj.optString("interval_type").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    private fun encodeQueueJobs(jobs: List<PrintQueueJob>): JSONArray =
        JSONArray().apply {
            jobs.forEach { job ->
                put(
                    JSONObject()
                        .put("id", job.id)
                        .put("position", job.position)
                        .put("display_name", job.displayName)
                        .put("has_library_thumbnail", job.hasLibraryThumbnail)
                        .put("has_archive_thumbnail", job.hasArchiveThumbnail)
                        .putOptInt("library_file_id", job.libraryFileId)
                        .putOptInt("archive_id", job.archiveId)
                        .putOptInt("plate_id", job.plateId)
                        .putOptInt("estimated_duration_seconds", job.estimatedDurationSeconds)
                        .putOptObject("filament_usage", job.filamentUsage?.let { encodeFilamentUsage(it) }),
                )
            }
        }

    private fun decodeQueueJobs(array: JSONArray?): List<PrintQueueJob> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    PrintQueueJob(
                        id = obj.optInt("id", 0),
                        position = obj.optInt("position", 0),
                        displayName = obj.optString("display_name", ""),
                        hasLibraryThumbnail = obj.optBoolean("has_library_thumbnail", false),
                        hasArchiveThumbnail = obj.optBoolean("has_archive_thumbnail", false),
                        libraryFileId = obj.optNullableInt("library_file_id"),
                        archiveId = obj.optNullableInt("archive_id"),
                        plateId = obj.optNullableInt("plate_id"),
                        estimatedDurationSeconds = obj.optNullableInt("estimated_duration_seconds"),
                        filamentUsage = obj.optJSONObject("filament_usage")?.let { decodeFilamentUsage(it) },
                    ),
                )
            }
        }
    }

    private fun encodeMachineInfo(info: PrinterMachineInfo): JSONObject =
        JSONObject()
            .putOptString("serial_number", info.serialNumber)
            .putOptString("ip_address", info.ipAddress)
            .putOptString("model", info.model)
            .putOptString("location", info.location)
            .putOptString("updated_at_iso", info.updatedAtIso)
            .putOptInt("nozzle_count", info.nozzleCount)
            .putOptBoolean("auto_archive_enabled", info.autoArchiveEnabled)

    private fun decodeMachineInfo(obj: JSONObject): PrinterMachineInfo =
        PrinterMachineInfo(
            serialNumber = obj.optString("serial_number").takeIf { it.isNotBlank() },
            ipAddress = obj.optString("ip_address").takeIf { it.isNotBlank() },
            model = obj.optString("model").takeIf { it.isNotBlank() },
            location = obj.optString("location").takeIf { it.isNotBlank() },
            updatedAtIso = obj.optString("updated_at_iso").takeIf { it.isNotBlank() },
            nozzleCount = obj.optNullableInt("nozzle_count"),
            autoArchiveEnabled = obj.optNullableBoolean("auto_archive_enabled"),
        )

    private fun encodeSmartPlugState(state: PrinterSmartPlugState): JSONObject =
        JSONObject()
            .put("config", encodeSmartPlugConfig(state.config))
            .putOptObject("live_status", state.liveStatus?.let { encodeSmartPlugLiveStatus(it) })
            .putOptLong("last_updated_at_millis", state.lastUpdatedAtMillis)

    private fun decodeSmartPlugState(obj: JSONObject): PrinterSmartPlugState? {
        val configObj = obj.optJSONObject("config") ?: return null
        return PrinterSmartPlugState(
            config = decodeSmartPlugConfig(configObj),
            liveStatus = obj.optJSONObject("live_status")?.let { decodeSmartPlugLiveStatus(it) },
            lastUpdatedAtMillis = obj.optNullableLong("last_updated_at_millis"),
        )
    }

    private fun encodeSmartPlugConfig(config: SmartPlugConfig): JSONObject =
        JSONObject()
            .put("id", config.id)
            .put("name", config.name)
            .putOptString("last_state", config.lastState)
            .putOptString("last_checked_iso", config.lastCheckedIso)

    private fun decodeSmartPlugConfig(obj: JSONObject): SmartPlugConfig =
        SmartPlugConfig(
            id = obj.optInt("id", 0),
            name = obj.optString("name", "Smart plug"),
            lastState = obj.optString("last_state").takeIf { it.isNotBlank() },
            lastCheckedIso = obj.optString("last_checked_iso").takeIf { it.isNotBlank() },
        )

    private fun encodeSmartPlugLiveStatus(status: SmartPlugLiveStatus): JSONObject =
        JSONObject()
            .put("power_state", status.powerState.name)
            .put("reachable", status.reachable)
            .putOptString("device_name", status.deviceName)
            .putOptObject("energy", status.energy?.let { encodeSmartPlugEnergy(it) })

    private fun decodeSmartPlugLiveStatus(obj: JSONObject): SmartPlugLiveStatus {
        val powerState = runCatching {
            SmartOutletPowerState.valueOf(obj.optString("power_state", SmartOutletPowerState.Unknown.name))
        }.getOrDefault(SmartOutletPowerState.Unknown)
        return SmartPlugLiveStatus(
            powerState = powerState,
            reachable = obj.optBoolean("reachable", true),
            deviceName = obj.optString("device_name").takeIf { it.isNotBlank() },
            energy = obj.optJSONObject("energy")?.let { decodeSmartPlugEnergy(it) },
        )
    }

    private fun encodeSmartPlugEnergy(energy: SmartPlugEnergyReading): JSONObject =
        JSONObject()
            .putOptDouble("power_watts", energy.powerWatts)
            .putOptDouble("voltage_volts", energy.voltageVolts)
            .putOptDouble("current_amps", energy.currentAmps)

    private fun decodeSmartPlugEnergy(obj: JSONObject): SmartPlugEnergyReading =
        SmartPlugEnergyReading(
            powerWatts = obj.optNullableDouble("power_watts"),
            voltageVolts = obj.optNullableDouble("voltage_volts"),
            currentAmps = obj.optNullableDouble("current_amps"),
        )

    private fun encodeFilamentUsage(usage: FilamentUsage): JSONObject =
        JSONObject()
            .putOptDouble("weight_grams", usage.weightGrams)
            .putOptDouble("length_meters", usage.lengthMeters)

    private fun decodeFilamentUsage(obj: JSONObject): FilamentUsage =
        FilamentUsage(
            weightGrams = obj.optNullableDouble("weight_grams"),
            lengthMeters = obj.optNullableDouble("length_meters"),
        )
}
