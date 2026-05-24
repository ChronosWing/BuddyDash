package com.chronoswing.buddydash.network

/**
 * Paths and operations from [bambuddy-openapi.json] (OpenAPI 3.1, Bambuddy v0.2.4).
 */
object BambuddyApi {
    /** POST Clear Plate — operationId: clear_plate_api_v1_printers__printer_id__clear_plate_post */
    const val CLEAR_PLATE_PATH = "/api/v1/printers/{printer_id}/clear-plate"

    /** POST — clear HMS/print errors (operationId: clear_hms_errors_…). */
    const val HMS_CLEAR_PATH = "/api/v1/printers/{printer_id}/hms/clear"

    /** GET — PrinterStatus includes awaiting_plate_clear */
    const val PRINTER_STATUS_PATH = "/api/v1/printers/{printer_id}/status"

    /** GET — current print job cover / thumbnail (operationId: get_printer_cover_…). */
    const val PRINTER_COVER_PATH = "/api/v1/printers/{printer_id}/cover"

    const val LIST_PRINTERS_PATH = "/api/v1/printers/"

    const val INVENTORY_ASSIGNMENTS_PATH = "/api/v1/inventory/assignments"

    /** GET — list spools (operationId: list_spools_api_v1_inventory_spools_get). */
    const val INVENTORY_SPOOLS_PATH = "/api/v1/inventory/spools"

    /** GET — spool usage history (operationId: get_spool_usage_history_…). */
    const val INVENTORY_SPOOL_USAGE_PATH = "/api/v1/inventory/spools/{spool_id}/usage"

    /** GET — application settings including low_stock_threshold (AppSettings). */
    const val SETTINGS_PATH = "/api/v1/settings"

    const val CHAMBER_LIGHT_PATH = "/api/v1/printers/{printer_id}/chamber-light"
    const val CAMERA_SNAPSHOT_PATH = "/api/v1/printers/{printer_id}/camera/snapshot"
    const val CAMERA_STREAM_PATH = "/api/v1/printers/{printer_id}/camera/stream"
    const val CAMERA_STOP_PATH = "/api/v1/printers/{printer_id}/camera/stop"
    const val PRINTER_FILES_PATH = "/api/v1/printers/{printer_id}/files"
    const val PRINT_PAUSE_PATH = "/api/v1/printers/{printer_id}/print/pause"
    const val PRINT_RESUME_PATH = "/api/v1/printers/{printer_id}/print/resume"
    const val PRINT_STOP_PATH = "/api/v1/printers/{printer_id}/print/stop"
    const val PRINT_SPEED_PATH = "/api/v1/printers/{printer_id}/print-speed"
    const val MAINTENANCE_PRINTER_PATH = "/api/v1/maintenance/printers/{printer_id}"
    /** POST — mark maintenance performed (operationId: perform_maintenance_…). */
    const val MAINTENANCE_PERFORM_PATH = "/api/v1/maintenance/items/{item_id}/perform"
    const val BED_JOG_PATH = "/api/v1/printers/{printer_id}/bed-jog"
    const val HOME_AXES_PATH = "/api/v1/printers/{printer_id}/home-axes"
    /** POST — load filament from AMS slot or external (tray_id query). */
    const val AMS_LOAD_PATH = "/api/v1/printers/{printer_id}/ams/load"
    /** POST — unload currently loaded filament. */
    const val AMS_UNLOAD_PATH = "/api/v1/printers/{printer_id}/ams/unload"
    const val PRINTER_DETAIL_PATH = "/api/v1/printers/{printer_id}"

    /** GET — list print queue (OpenAPI: list_queue_api_v1_queue__get). */
    const val QUEUE_PATH = "/api/v1/queue/"

    /** POST — add archive to queue (OpenAPI: add_to_queue_api_v1_queue__post, PrintQueueItemCreate). */
    const val QUEUE_ADD_PATH = "/api/v1/queue/"

    /** POST — start staged queue item (OpenAPI: start_queue_item_api_v1_queue__item_id__start_post). */
    const val QUEUE_ITEM_START_PATH = "/api/v1/queue/{item_id}/start"

    /**
     * POST — reprint archive directly (OpenAPI: reprint_archive_…). BuddyDash v1 uses [QUEUE_ADD_PATH]
     * with manual_start instead to avoid bypassing the queue.
     */
    const val ARCHIVE_REPRINT_PATH = "/api/v1/archives/{archive_id}/reprint"

    /** GET — list archives (operationId: list_archives_api_v1_archives__get). */
    const val ARCHIVES_PATH = "/api/v1/archives/"

    /** GET — single archive (operationId: get_archive_api_v1_archives__archive_id__get). */
    const val ARCHIVE_DETAIL_PATH = "/api/v1/archives/{archive_id}"

    const val ARCHIVE_THUMBNAIL_PATH = "/api/v1/archives/{archive_id}/thumbnail"
    const val ARCHIVE_PLATE_THUMBNAIL_PATH =
        "/api/v1/archives/{archive_id}/plate-thumbnail/{plate_index}"
    const val LIBRARY_FILE_THUMBNAIL_PATH = "/api/v1/library/files/{file_id}/thumbnail"
    const val LIBRARY_FILE_PLATE_THUMBNAIL_PATH =
        "/api/v1/library/files/{file_id}/plate-thumbnail/{plate_index}"

    val hasClearPlateEndpoint: Boolean = true
    val hasHmsClearEndpoint: Boolean = true
    val hasQueueEndpoint: Boolean = true
    val hasChamberLightEndpoint: Boolean = true
    val hasPrintControlEndpoints: Boolean = true
    val hasPrintSpeedEndpoint: Boolean = true
    val hasMaintenanceEndpoint: Boolean = true
    val hasMaintenancePerformEndpoint: Boolean = true
    val hasBedJogEndpoint: Boolean = true
    val hasHomeAxesEndpoint: Boolean = true
    val hasAmsLoadEndpoint: Boolean = true
    val hasAmsUnloadEndpoint: Boolean = true
    /** POST /api/v1/inventory/assignments — map spool to AMS/external tray. */
    val hasInventoryAssignEndpoint: Boolean = true
    /** DELETE /api/v1/inventory/assignments/{printer_id}/{ams_id}/{tray_id} */
    val hasInventoryUnassignEndpoint: Boolean = true
    val hasPrinterDetailEndpoint: Boolean = true
    val hasCameraEndpoint: Boolean = true
    val hasFilesEndpoint: Boolean = true
    val hasSpoolInventoryEndpoint: Boolean = true
    val hasSpoolUsageEndpoint: Boolean = true
    val hasArchivesEndpoint: Boolean = true
    val hasQueueAddEndpoint: Boolean = true
    val hasQueueStartEndpoint: Boolean = true

    fun inventoryAssignmentsPath(printerId: Int? = null): String =
        if (printerId != null) {
            "$INVENTORY_ASSIGNMENTS_PATH?printer_id=$printerId"
        } else {
            INVENTORY_ASSIGNMENTS_PATH
        }

    fun clearPlatePath(printerId: Int): String =
        CLEAR_PLATE_PATH.replace("{printer_id}", printerId.toString())

    fun hmsClearPath(printerId: Int): String =
        HMS_CLEAR_PATH.replace("{printer_id}", printerId.toString())

    fun printerStatusPath(printerId: Int): String =
        PRINTER_STATUS_PATH.replace("{printer_id}", printerId.toString())

    fun printerCoverPath(printerId: Int): String =
        PRINTER_COVER_PATH.replace("{printer_id}", printerId.toString())

    fun cameraSnapshotPath(printerId: Int): String =
        CAMERA_SNAPSHOT_PATH.replace("{printer_id}", printerId.toString())

    fun cameraStreamPath(printerId: Int): String =
        CAMERA_STREAM_PATH.replace("{printer_id}", printerId.toString())

    fun cameraStopPath(printerId: Int): String =
        CAMERA_STOP_PATH.replace("{printer_id}", printerId.toString())

    fun chamberLightPath(printerId: Int, on: Boolean): String =
        "${CHAMBER_LIGHT_PATH.replace("{printer_id}", printerId.toString())}?on=$on"

    fun printPausePath(printerId: Int): String =
        PRINT_PAUSE_PATH.replace("{printer_id}", printerId.toString())

    fun printResumePath(printerId: Int): String =
        PRINT_RESUME_PATH.replace("{printer_id}", printerId.toString())

    fun printStopPath(printerId: Int): String =
        PRINT_STOP_PATH.replace("{printer_id}", printerId.toString())

    fun printSpeedPath(printerId: Int, mode: Int): String =
        "${PRINT_SPEED_PATH.replace("{printer_id}", printerId.toString())}?mode=$mode"

    fun maintenancePrinterPath(printerId: Int): String =
        MAINTENANCE_PRINTER_PATH.replace("{printer_id}", printerId.toString())

    fun maintenancePerformPath(itemId: Int): String =
        MAINTENANCE_PERFORM_PATH.replace("{item_id}", itemId.toString())

    /** Relative Z jog in mm (positive = bed down, negative = bed up). */
    fun bedJogPath(printerId: Int, distanceMm: Float, force: Boolean = false): String {
        val base = BED_JOG_PATH.replace("{printer_id}", printerId.toString())
        val forceQuery = if (force) "&force=true" else ""
        return "$base?distance=$distanceMm$forceQuery"
    }

    fun homeAxesPath(printerId: Int): String =
        HOME_AXES_PATH.replace("{printer_id}", printerId.toString())

    fun amsLoadPath(printerId: Int, trayId: Int): String =
        "${AMS_LOAD_PATH.replace("{printer_id}", printerId.toString())}?tray_id=$trayId"

    fun amsUnloadPath(printerId: Int): String =
        AMS_UNLOAD_PATH.replace("{printer_id}", printerId.toString())

    fun inventoryUnassignPath(printerId: Int, amsId: Int, trayId: Int): String =
        "$INVENTORY_ASSIGNMENTS_PATH/$printerId/$amsId/$trayId"

    fun printerDetailPath(printerId: Int): String =
        PRINTER_DETAIL_PATH.replace("{printer_id}", printerId.toString())

    fun queuePath(printerId: Int): String = "$QUEUE_PATH?printer_id=$printerId"

    fun archivesPath(limit: Int = 50, offset: Int = 0, printerId: Int? = null): String {
        val params = buildList {
            add("limit=$limit")
            add("offset=$offset")
            printerId?.let { add("printer_id=$it") }
        }
        return "$ARCHIVES_PATH?${params.joinToString("&")}"
    }

    fun archiveDetailPath(archiveId: Int): String =
        ARCHIVE_DETAIL_PATH.replace("{archive_id}", archiveId.toString())

    fun archiveReprintPath(archiveId: Int, printerId: Int): String =
        "${ARCHIVE_REPRINT_PATH.replace("{archive_id}", archiveId.toString())}?printer_id=$printerId"

    fun queueItemStartPath(itemId: Int): String =
        QUEUE_ITEM_START_PATH.replace("{item_id}", itemId.toString())

    fun inventorySpoolsPath(includeArchived: Boolean = false): String =
        if (includeArchived) {
            "$INVENTORY_SPOOLS_PATH?include_archived=true"
        } else {
            INVENTORY_SPOOLS_PATH
        }

    fun spoolUsagePath(spoolId: Int, limit: Int = 50): String =
        INVENTORY_SPOOL_USAGE_PATH
            .replace("{spool_id}", spoolId.toString()) +
            "?limit=$limit"

    fun archiveThumbnailPath(archiveId: Int): String =
        ARCHIVE_THUMBNAIL_PATH.replace("{archive_id}", archiveId.toString())

    fun archivePlateThumbnailPath(archiveId: Int, plateIndex: Int): String =
        ARCHIVE_PLATE_THUMBNAIL_PATH
            .replace("{archive_id}", archiveId.toString())
            .replace("{plate_index}", plateIndex.toString())

    fun libraryFileThumbnailPath(fileId: Int): String =
        LIBRARY_FILE_THUMBNAIL_PATH.replace("{file_id}", fileId.toString())

    fun libraryFilePlateThumbnailPath(fileId: Int, plateIndex: Int): String =
        LIBRARY_FILE_PLATE_THUMBNAIL_PATH
            .replace("{file_id}", fileId.toString())
            .replace("{plate_index}", plateIndex.toString())
}
