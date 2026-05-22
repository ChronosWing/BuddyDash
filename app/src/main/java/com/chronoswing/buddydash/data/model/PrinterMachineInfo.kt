package com.chronoswing.buddydash.data.model

/** Static printer record from GET /api/v1/printers/{printer_id} (PrinterResponse). */
data class PrinterMachineInfo(
    val serialNumber: String? = null,
    val ipAddress: String? = null,
    val model: String? = null,
    val location: String? = null,
    val updatedAtIso: String? = null,
)
