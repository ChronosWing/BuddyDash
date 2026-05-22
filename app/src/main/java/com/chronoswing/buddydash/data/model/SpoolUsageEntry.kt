package com.chronoswing.buddydash.data.model

/** Runtime IDs from usage JSON that may map to an archived print (not all are archive primary keys). */
data class SpoolUsageDirectIds(
    val archiveId: Int? = null,
    val historyId: Int? = null,
    val jobId: Int? = null,
    val taskId: Int? = null,
    val fileId: Int? = null,
    val printId: Int? = null,
) {
    /** Priority order when resolving against [PrintArchive.id]. */
    fun resolutionOrder(): List<Pair<String, Int>> = listOfNotNull(
        archiveId?.let { "archive_id" to it },
        historyId?.let { "history_id" to it },
        jobId?.let { "job_id" to it },
        taskId?.let { "task_id" to it },
        printId?.let { "print_id" to it },
        fileId?.let { "file_id" to it },
    )
}

/** One row from GET /api/v1/inventory/spools/{spool_id}/usage (SpoolUsageHistoryResponse). */
data class SpoolUsageEntry(
    /** [SpoolUsageHistory.id] — usage record PK, not archive id. */
    val id: Int,
    val spoolId: Int,
    val printerId: Int?,
    val printName: String?,
    val weightUsedGrams: Double,
    val percentUsed: Int?,
    val status: String,
    val createdAtIso: String,
    val directIds: SpoolUsageDirectIds = SpoolUsageDirectIds(),
    val plateNumber: Int? = null,
    /** Full API object for discovery logging (Bambuddy may omit archive_id from schema). */
    val rawJson: String = "",
)
