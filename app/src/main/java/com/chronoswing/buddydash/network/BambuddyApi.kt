package com.chronoswing.buddydash.network

/**
 * Paths and operations from [bambuddy-openapi.json] (OpenAPI 3.1, Bambuddy v0.2.4).
 */
object BambuddyApi {
    /** POST Clear Plate — operationId: clear_plate_api_v1_printers__printer_id__clear_plate_post */
    const val CLEAR_PLATE_PATH = "/api/v1/printers/{printer_id}/clear-plate"

    /** GET — PrinterStatus includes awaiting_plate_clear */
    const val PRINTER_STATUS_PATH = "/api/v1/printers/{printer_id}/status"

    /** GET — current print job cover / thumbnail (operationId: get_printer_cover_…). */
    const val PRINTER_COVER_PATH = "/api/v1/printers/{printer_id}/cover"

    const val LIST_PRINTERS_PATH = "/api/v1/printers/"

    const val INVENTORY_ASSIGNMENTS_PATH = "/api/v1/inventory/assignments"

    const val CHAMBER_LIGHT_PATH = "/api/v1/printers/{printer_id}/chamber-light"
    const val CAMERA_SNAPSHOT_PATH = "/api/v1/printers/{printer_id}/camera/snapshot"
    const val PRINTER_FILES_PATH = "/api/v1/printers/{printer_id}/files"
    const val PRINT_PAUSE_PATH = "/api/v1/printers/{printer_id}/print/pause"
    const val PRINT_RESUME_PATH = "/api/v1/printers/{printer_id}/print/resume"
    const val PRINT_STOP_PATH = "/api/v1/printers/{printer_id}/print/stop"
    const val PRINT_SPEED_PATH = "/api/v1/printers/{printer_id}/print-speed"
    const val MAINTENANCE_PRINTER_PATH = "/api/v1/maintenance/printers/{printer_id}"
    /** POST — mark maintenance performed (operationId: perform_maintenance_…). */
    const val MAINTENANCE_PERFORM_PATH = "/api/v1/maintenance/items/{item_id}/perform"
    const val BED_JOG_PATH = "/api/v1/printers/{printer_id}/bed-jog"

    /** GET — list print queue (OpenAPI: list_queue_api_v1_queue__get). */
    const val QUEUE_PATH = "/api/v1/queue/"

    const val ARCHIVE_THUMBNAIL_PATH = "/api/v1/archives/{archive_id}/thumbnail"
    const val ARCHIVE_PLATE_THUMBNAIL_PATH =
        "/api/v1/archives/{archive_id}/plate-thumbnail/{plate_index}"
    const val LIBRARY_FILE_THUMBNAIL_PATH = "/api/v1/library/files/{file_id}/thumbnail"
    const val LIBRARY_FILE_PLATE_THUMBNAIL_PATH =
        "/api/v1/library/files/{file_id}/plate-thumbnail/{plate_index}"

    val hasClearPlateEndpoint: Boolean = true
    val hasQueueEndpoint: Boolean = true
    val hasChamberLightEndpoint: Boolean = true
    val hasPrintControlEndpoints: Boolean = true
    val hasPrintSpeedEndpoint: Boolean = true
    val hasMaintenanceEndpoint: Boolean = true
    val hasMaintenancePerformEndpoint: Boolean = true
    val hasBedJogEndpoint: Boolean = true
    val hasCameraEndpoint: Boolean = true
    val hasFilesEndpoint: Boolean = true

    fun inventoryAssignmentsPath(printerId: Int? = null): String =
        if (printerId != null) {
            "$INVENTORY_ASSIGNMENTS_PATH?printer_id=$printerId"
        } else {
            INVENTORY_ASSIGNMENTS_PATH
        }

    fun clearPlatePath(printerId: Int): String =
        CLEAR_PLATE_PATH.replace("{printer_id}", printerId.toString())

    fun printerStatusPath(printerId: Int): String =
        PRINTER_STATUS_PATH.replace("{printer_id}", printerId.toString())

    fun printerCoverPath(printerId: Int): String =
        PRINTER_COVER_PATH.replace("{printer_id}", printerId.toString())

    fun cameraSnapshotPath(printerId: Int): String =
        CAMERA_SNAPSHOT_PATH.replace("{printer_id}", printerId.toString())

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

    fun queuePath(printerId: Int): String = "$QUEUE_PATH?printer_id=$printerId"

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
