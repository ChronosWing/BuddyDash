package com.chronoswing.buddydash.data.model

/** Upcoming queued print (read-only); excludes the job currently printing on the printer. */
data class PrintQueueJob(
    val id: Int,
    val position: Int,
    val displayName: String,
    /**
     * True when `library_file_thumbnail` is set in queue JSON (filesystem path hint; not an image URL).
     */
    val hasLibraryThumbnail: Boolean = false,
    /**
     * True when `archive_thumbnail` is set in queue JSON (filesystem path hint; not an image URL).
     */
    val hasArchiveThumbnail: Boolean = false,
    val libraryFileId: Int? = null,
    val archiveId: Int? = null,
    /** Bambuddy `plate_id` — 1-based plate index for plate-thumbnail endpoints. */
    val plateId: Int? = null,
    val estimatedDurationSeconds: Int? = null,
)
