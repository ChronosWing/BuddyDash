package com.chronoswing.buddydash.data.model

data class Printer(
    val id: Int,
    val name: String,
    val model: String?,
    val liveStatus: PrinterStatus? = null,
)

data class PrinterStatus(
    val connected: Boolean = false,
    val rawState: String?,
    val progress: Float?,
    val fileName: String?,
    val remainingTimeSeconds: Int?,
    val nozzleTemp: Double?,
    val bedTemp: Double?,
    val chamberTemp: Double? = null,
    val hmsErrorCount: Int = 0,
    /** Present only when the API includes `awaiting_plate_clear`; do not infer. */
    val awaitingPlateClear: Boolean? = null,
    val filamentSlots: List<FilamentSlot> = emptyList(),
    val amsUnits: List<AmsUnitInfo> = emptyList(),
    val wifiSignalDbm: Int? = null,
    val wiredNetwork: Boolean? = null,
    val doorOpen: Boolean? = null,
    val firmwareVersion: String? = null,
    val partFanPercent: Int? = null,
    val auxFanPercent: Int? = null,
    val chamberFanPercent: Int? = null,
    /** Bambuddy speed_level: 1=silent, 2=standard, 3=sport, 4=ludicrous */
    val speedLevel: Int? = null,
    val chamberLightOn: Boolean? = null,
)
