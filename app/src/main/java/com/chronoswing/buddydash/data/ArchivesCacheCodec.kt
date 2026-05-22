package com.chronoswing.buddydash.data

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.PrintArchive
import org.json.JSONArray
import org.json.JSONObject

data class ArchivesCacheSnapshot(
    val serverUrl: String,
    val printerFilterId: Int?,
    val archives: List<PrintArchive>,
    val lastUpdatedAtMillis: Long,
)

object ArchivesCacheCodec {
    private const val VERSION = 1

    fun encode(snapshot: ArchivesCacheSnapshot): String =
        JSONObject()
            .put("version", VERSION)
            .put("server_url", snapshot.serverUrl)
            .putOptInt("printer_filter_id", snapshot.printerFilterId)
            .put("last_updated_at_millis", snapshot.lastUpdatedAtMillis)
            .put("archives", encodeArchives(snapshot.archives))
            .toString()

    fun decode(json: String): ArchivesCacheSnapshot? {
        return try {
            val root = JSONObject(json)
            if (root.optInt("version", 0) != VERSION) return null
            val serverUrl = root.optString("server_url").takeIf { it.isNotBlank() } ?: return null
            val lastUpdated = root.optLong("last_updated_at_millis", 0L).takeIf { it > 0L } ?: return null
            ArchivesCacheSnapshot(
                serverUrl = serverUrl,
                printerFilterId = root.optNullableInt("printer_filter_id"),
                archives = decodeArchives(root.optJSONArray("archives")),
                lastUpdatedAtMillis = lastUpdated,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodeArchives(archives: List<PrintArchive>): JSONArray =
        JSONArray().apply { archives.forEach { put(encodeArchive(it)) } }

    fun decodeArchives(array: JSONArray?): List<PrintArchive> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                decodeArchive(array.optJSONObject(i) ?: continue)?.let { add(it) }
            }
        }
    }

    fun encodeArchive(archive: PrintArchive): JSONObject =
        JSONObject()
            .put("id", archive.id)
            .put("display_name", archive.displayName)
            .putOptString("filename", archive.filename)
            .putOptInt("printer_id", archive.printerId)
            .putOptString("printer_name", archive.printerName)
            .putOptString("printer_model", archive.printerModel)
            .put("status_raw", archive.statusRaw)
            .put("result_kind", archive.resultKind.name)
            .putOptString("started_at_iso", archive.startedAtIso)
            .putOptString("completed_at_iso", archive.completedAtIso)
            .putOptString("created_at_iso", archive.createdAtIso)
            .putOptLong("stats_completed_at_millis", archive.statsCompletedAtMillis)
            .putOptInt("duration_seconds", archive.durationSeconds)
            .putOptObject("filament_usage", archive.filamentUsage?.let { encodeFilamentUsage(it) })
            .putOptString("filament_type", archive.filamentType)
            .putOptString("filament_color", archive.filamentColor)
            .putOptInt("spool_id", archive.spoolId)
            .putOptInt("plate_number", archive.plateNumber)
            .putOptString("content_hash", archive.contentHash)
            .putOptString("failure_reason", archive.failureReason)
            .putOptInt("total_layers", archive.totalLayers)
            .putOptInt("quantity", archive.quantity)
            .putOptString("project_name", archive.projectName)
            .putOptString("sliced_for_model", archive.slicedForModel)
            .putOptString("notes", archive.notes)

    fun decodeArchive(obj: JSONObject): PrintArchive? {
        val id = obj.optInt("id", -1)
        if (id < 0) return null
        val displayName = obj.optString("display_name").takeIf { it.isNotBlank() } ?: return null
        val resultKind = runCatching {
            ArchiveResultKind.valueOf(obj.optString("result_kind", "Other"))
        }.getOrDefault(ArchiveResultKind.Other)
        return PrintArchive(
            id = id,
            displayName = displayName,
            filename = obj.optString("filename").takeIf { it.isNotBlank() },
            printerId = obj.optNullableInt("printer_id"),
            printerName = obj.optString("printer_name").takeIf { it.isNotBlank() },
            printerModel = obj.optString("printer_model").takeIf { it.isNotBlank() },
            statusRaw = obj.optString("status_raw", ""),
            resultKind = resultKind,
            startedAtIso = obj.optString("started_at_iso").takeIf { it.isNotBlank() },
            completedAtIso = obj.optString("completed_at_iso").takeIf { it.isNotBlank() },
            createdAtIso = obj.optString("created_at_iso").takeIf { it.isNotBlank() },
            statsCompletedAtMillis = obj.optNullableLong("stats_completed_at_millis"),
            durationSeconds = obj.optNullableInt("duration_seconds"),
            filamentUsage = obj.optJSONObject("filament_usage")?.let { decodeFilamentUsage(it) },
            filamentType = obj.optString("filament_type").takeIf { it.isNotBlank() },
            filamentColor = obj.optString("filament_color").takeIf { it.isNotBlank() },
            spoolId = obj.optNullableInt("spool_id"),
            plateNumber = obj.optNullableInt("plate_number"),
            contentHash = obj.optString("content_hash").takeIf { it.isNotBlank() },
            failureReason = obj.optString("failure_reason").takeIf { it.isNotBlank() },
            totalLayers = obj.optNullableInt("total_layers"),
            quantity = obj.optNullableInt("quantity"),
            projectName = obj.optString("project_name").takeIf { it.isNotBlank() },
            slicedForModel = obj.optString("sliced_for_model").takeIf { it.isNotBlank() },
            notes = obj.optString("notes").takeIf { it.isNotBlank() },
        )
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
}
