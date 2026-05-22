package com.chronoswing.buddydash.data

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolUsageDirectIds
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import org.json.JSONArray
import org.json.JSONObject

data class SpoolDetailCacheSnapshot(
    val serverUrl: String,
    val spoolId: Int,
    val spool: SpoolInventoryItem,
    val usageHistory: List<SpoolUsageEntry>,
    val printerNamesById: Map<Int, String>,
    val lastUpdatedAtMillis: Long,
)

object SpoolDetailCacheCodec {
    private const val VERSION = 1

    fun encode(snapshot: SpoolDetailCacheSnapshot): String =
        JSONObject()
            .put("version", VERSION)
            .put("server_url", snapshot.serverUrl)
            .put("spool_id", snapshot.spoolId)
            .put("last_updated_at_millis", snapshot.lastUpdatedAtMillis)
            .put("spool", SpoolsCacheCodec.encodeSpoolItem(snapshot.spool))
            .put("usage_history", encodeUsageHistory(snapshot.usageHistory))
            .put("printer_names", encodePrinterNames(snapshot.printerNamesById))
            .toString()

    fun decode(json: String): SpoolDetailCacheSnapshot? {
        return try {
            val root = JSONObject(json)
            if (root.optInt("version", 0) != VERSION) return null
            val serverUrl = root.optString("server_url").takeIf { it.isNotBlank() } ?: return null
            val spoolId = root.optInt("spool_id", -1)
            if (spoolId < 0) return null
            val spoolObj = root.optJSONObject("spool") ?: return null
            val spool = SpoolsCacheCodec.decodeSpoolItem(spoolObj) ?: return null
            val lastUpdated = root.optLong("last_updated_at_millis", 0L).takeIf { it > 0L } ?: return null
            SpoolDetailCacheSnapshot(
                serverUrl = serverUrl,
                spoolId = spoolId,
                spool = spool,
                usageHistory = decodeUsageHistory(root.optJSONArray("usage_history")),
                printerNamesById = decodePrinterNames(root.optJSONObject("printer_names")),
                lastUpdatedAtMillis = lastUpdated,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeUsageHistory(entries: List<SpoolUsageEntry>): JSONArray =
        JSONArray().apply {
            entries.forEach { entry ->
                put(
                    JSONObject()
                        .put("id", entry.id)
                        .put("spool_id", entry.spoolId)
                        .putOptInt("printer_id", entry.printerId)
                        .putOptString("print_name", entry.printName)
                        .put("weight_used_grams", entry.weightUsedGrams)
                        .putOptInt("percent_used", entry.percentUsed)
                        .put("status", entry.status)
                        .put("created_at_iso", entry.createdAtIso)
                        .putOptInt("plate_number", entry.plateNumber)
                        .put("direct_ids", encodeDirectIds(entry.directIds)),
                )
            }
        }

    private fun encodeDirectIds(ids: SpoolUsageDirectIds): JSONObject =
        JSONObject()
            .putOptInt("archive_id", ids.archiveId)
            .putOptInt("history_id", ids.historyId)
            .putOptInt("job_id", ids.jobId)
            .putOptInt("task_id", ids.taskId)
            .putOptInt("file_id", ids.fileId)
            .putOptInt("print_id", ids.printId)

    private fun decodeDirectIds(obj: JSONObject?): SpoolUsageDirectIds {
        if (obj == null) return SpoolUsageDirectIds()
        return SpoolUsageDirectIds(
            archiveId = obj.optNullableInt("archive_id"),
            historyId = obj.optNullableInt("history_id"),
            jobId = obj.optNullableInt("job_id"),
            taskId = obj.optNullableInt("task_id"),
            fileId = obj.optNullableInt("file_id"),
            printId = obj.optNullableInt("print_id"),
        )
    }

    private fun decodeUsageHistory(array: JSONArray?): List<SpoolUsageEntry> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optInt("id", -1)
                val spoolId = obj.optInt("spool_id", -1)
                if (id < 0 || spoolId < 0) continue
                add(
                    SpoolUsageEntry(
                        id = id,
                        spoolId = spoolId,
                        printerId = obj.optNullableInt("printer_id"),
                        printName = obj.optString("print_name").takeIf { it.isNotBlank() },
                        weightUsedGrams = obj.optDouble("weight_used_grams", 0.0),
                        percentUsed = obj.optNullableInt("percent_used"),
                        status = obj.optString("status", "unknown"),
                        createdAtIso = obj.optString("created_at_iso", ""),
                        directIds = decodeDirectIds(obj.optJSONObject("direct_ids")),
                        plateNumber = obj.optNullableInt("plate_number"),
                    ),
                )
            }
        }
    }

    private fun encodePrinterNames(names: Map<Int, String>): JSONObject =
        JSONObject().apply {
            names.forEach { (id, name) -> put(id.toString(), name) }
        }

    private fun decodePrinterNames(obj: JSONObject?): Map<Int, String> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<Int, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val id = key.toIntOrNull() ?: continue
            out[id] = obj.optString(key, "")
        }
        return out
    }
}
