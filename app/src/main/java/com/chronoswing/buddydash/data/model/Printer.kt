package com.chronoswing.buddydash.data.model

data class Printer(
    val id: Int,
    val name: String,
    val model: String?,
    val status: String?,
    val isOnline: Boolean?,
)

data class PrinterStatus(
    val connected: Boolean = false,
    val rawState: String?,
    val progress: Float?,
    val fileName: String?,
    val remainingTimeSeconds: Int?,
    val nozzleTemp: Double?,
    val bedTemp: Double?,
    val hmsErrorCount: Int = 0,
    /** Present only when the API includes `awaiting_plate_clear`; do not infer. */
    val awaitingPlateClear: Boolean? = null,
)
