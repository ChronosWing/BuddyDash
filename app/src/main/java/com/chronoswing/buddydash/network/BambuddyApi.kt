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

    val hasClearPlateEndpoint: Boolean = true

    fun clearPlatePath(printerId: Int): String =
        CLEAR_PLATE_PATH.replace("{printer_id}", printerId.toString())

    fun printerStatusPath(printerId: Int): String =
        PRINTER_STATUS_PATH.replace("{printer_id}", printerId.toString())
}
