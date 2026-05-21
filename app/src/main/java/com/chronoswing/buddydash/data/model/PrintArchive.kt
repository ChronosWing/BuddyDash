package com.chronoswing.buddydash.data.model

/** Read-only archived print from GET /api/v1/archives/ (ArchiveResponse). */
data class PrintArchive(
    val id: Int,
    val displayName: String,
    val printerId: Int?,
    val printerName: String?,
    val printerModel: String?,
    val statusRaw: String,
    val resultKind: ArchiveResultKind,
    val startedAtIso: String?,
    val completedAtIso: String?,
    /** Archive record time; fallback when completion fields are absent. */
    val createdAtIso: String? = null,
    /**
     * Resolved completion instant (epoch ms) for stats range filters.
     * From completed/finished/end, then updated, then created — set at parse time.
     */
    val statsCompletedAtMillis: Long? = null,
    val durationSeconds: Int?,
    val filamentUsage: FilamentUsage?,
    val filamentType: String?,
    val filamentColor: String?,
    val failureReason: String?,
    val totalLayers: Int?,
    val quantity: Int?,
    val projectName: String?,
    val slicedForModel: String?,
    val notes: String?,
)

enum class ArchiveResultKind {
    Success,
    Failed,
    Cancelled,
    Other,
}
