package com.chronoswing.buddydash.network

/**
 * Paths and operations from [bambuddy-openapi.json] (OpenAPI 3.1, Bambuddy v0.2.4).
 */
object BambuddyApi {
    /** POST Clear Plate — operationId: clear_plate_api_v1_printers__printer_id__clear_plate_post */
    const val CLEAR_PLATE_PATH = "/api/v1/printers/{printer_id}/clear-plate"

    /** GET — PrinterStatus includes awaiting_plate_clear */
    const val PRINTER_STATUS_PATH = "/api/v1/printers/{printer_id}/status"

    const val LIST_PRINTERS_PATH = "/api/v1/printers/"

    const val INVENTORY_ASSIGNMENTS_PATH = "/api/v1/inventory/assignments"

    const val CHAMBER_LIGHT_PATH = "/api/v1/printers/{printer_id}/chamber-light"
    const val CAMERA_SNAPSHOT_PATH = "/api/v1/printers/{printer_id}/camera/snapshot"
    const val PRINTER_FILES_PATH = "/api/v1/printers/{printer_id}/files"

    val hasClearPlateEndpoint: Boolean = true
    /** Present in OpenAPI; not wired in BuddyDash v1. */
    val hasChamberLightEndpoint: Boolean = true
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
}
