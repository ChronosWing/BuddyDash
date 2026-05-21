package com.chronoswing.buddydash.data.model

/** One row from GET /api/v1/inventory/spools/{spool_id}/usage (SpoolUsageHistoryResponse). */
data class SpoolUsageEntry(
    val id: Int,
    val spoolId: Int,
    val printerId: Int?,
    val printName: String?,
    val weightUsedGrams: Double,
    val percentUsed: Int?,
    val status: String,
    val createdAtIso: String,
)
