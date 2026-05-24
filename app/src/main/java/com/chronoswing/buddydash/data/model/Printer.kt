package com.chronoswing.buddydash.data.model

import com.chronoswing.buddydash.util.MaintenanceHomeIndicator
import com.chronoswing.buddydash.util.isFault
import com.chronoswing.buddydash.util.SlotInventoryKey

data class Printer(
    val id: Int,
    val name: String,
    val model: String?,
    val liveStatus: PrinterStatus? = null,
    val maintenanceIndicator: MaintenanceHomeIndicator = MaintenanceHomeIndicator.None,
    /** Attention-worthy maintenance rows from the last home enrich fetch. */
    val maintenanceItems: List<MaintenanceItem> = emptyList(),
    val maintenanceTotalPrintHours: Double? = null,
    /** Count of pending queue jobs from GET /api/v1/queue (excludes currently printing). */
    val pendingQueueCount: Int = 0,
)

data class PrinterStatus(
    val connected: Boolean = false,
    val rawState: String?,
    val progress: Float?,
    val fileName: String?,
    /** Raw `current_print` from status JSON. */
    val currentPrint: String? = null,
    val subtaskName: String? = null,
    val gcodeFile: String? = null,
    /** `cover_url` hint from status (path or URL); used for thumbnail cache identity. */
    val coverUrl: String? = null,
    val remainingTimeSeconds: Int?,
    val nozzleTemp: Double?,
    val bedTemp: Double?,
    val chamberTemp: Double? = null,
    val hmsErrors: List<PrinterHmsError> = emptyList(),
    /** Explicit fault text from top-level status fields (not warnings/notifications). */
    val statusFaultMessages: List<String> = emptyList(),
    /** Present only when the API includes `awaiting_plate_clear`; do not infer. */
    val awaitingPlateClear: Boolean? = null,
    val filamentSlots: List<FilamentSlot> = emptyList(),
    /** Active toolhead tray from `tray_now` when matched to a known slot; null if unknown. */
    val activeFilamentSlot: SlotInventoryKey? = null,
    val amsUnits: List<AmsUnitInfo> = emptyList(),
    val wifiSignalDbm: Int? = null,
    val wiredNetwork: Boolean? = null,
    val developerMode: Boolean? = null,
    val doorOpen: Boolean? = null,
    val firmwareVersion: String? = null,
    val partFanPercent: Int? = null,
    val auxFanPercent: Int? = null,
    val chamberFanPercent: Int? = null,
    /** Bambuddy speed_level: 1=silent, 2=standard, 3=sport, 4=ludicrous */
    val speedLevel: Int? = null,
    val chamberLightOn: Boolean? = null,
    /** Formatted nozzle diameter for display, e.g. "0.4 mm"; from `nozzles` / `nozzle_rack`. */
    val nozzleDiameterDisplay: String? = null,
    /** Filament usage when present on status JSON (OpenAPI has no field; may appear at runtime). */
    val filamentUsage: FilamentUsage? = null,
) {
    /** Count of HMS entries that are faults (excludes warnings/notifications). */
    val hmsErrorCount: Int get() = hmsErrors.count { it.isFault() }
}
