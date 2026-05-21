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

    val hasClearPlateEndpoint: Boolean = true
    val hasChamberLightEndpoint: Boolean = true
    val hasPrintControlEndpoints: Boolean = true
    val hasPrintSpeedEndpoint: Boolean = true
    val hasMaintenanceEndpoint: Boolean = true
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
}
