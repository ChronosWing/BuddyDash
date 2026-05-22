package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.network.BambuddyApi

enum class FilamentAssignBlockReason {
    EndpointMissing,
    Offline,
    Printing,
    Busy,
    Error,
    SlotUnavailable,
}

data class FilamentAssignAvailability(
    val allowed: Boolean,
    val reason: FilamentAssignBlockReason? = null,
)

fun FilamentSlot.canAssignInventory(): Boolean =
    inventoryKey != null && BambuddyApi.hasInventoryAssignEndpoint

fun evaluateFilamentAssignAvailability(status: PrinterStatus?): FilamentAssignAvailability {
    if (!BambuddyApi.hasInventoryAssignEndpoint) {
        return FilamentAssignAvailability(false, FilamentAssignBlockReason.EndpointMissing)
    }
    if (status == null || !status.connected) {
        return FilamentAssignAvailability(false, FilamentAssignBlockReason.Offline)
    }
    return when (status.resolveActivityKind()) {
        PrinterActivityKind.Printing,
        PrinterActivityKind.Paused,
        -> FilamentAssignAvailability(false, FilamentAssignBlockReason.Printing)
        PrinterActivityKind.Error ->
            FilamentAssignAvailability(false, FilamentAssignBlockReason.Error)
        PrinterActivityKind.Busy ->
            FilamentAssignAvailability(false, FilamentAssignBlockReason.Busy)
        PrinterActivityKind.Offline ->
            FilamentAssignAvailability(false, FilamentAssignBlockReason.Offline)
        PrinterActivityKind.Idle ->
            FilamentAssignAvailability(true)
    }
}

fun filamentAssignBlockMessage(reason: FilamentAssignBlockReason): String = when (reason) {
    FilamentAssignBlockReason.EndpointMissing -> "Assignment not supported on this server"
    FilamentAssignBlockReason.Offline -> "Unavailable while printer is offline"
    FilamentAssignBlockReason.Printing -> "Unavailable while printing"
    FilamentAssignBlockReason.Busy -> "Printer busy"
    FilamentAssignBlockReason.Error -> "Unavailable while printer has an error"
    FilamentAssignBlockReason.SlotUnavailable -> "This slot cannot be assigned"
}

/** Prefer spools matching the slot material when the slot reports a type. */
fun filterSpoolsForSlotAssignment(
    spools: List<SpoolInventoryItem>,
    slot: FilamentSlot,
): List<SpoolInventoryItem> {
    val slotMaterial = normalizeFilamentType(slot.filamentType) ?: return spools
    val matching = spools.filter { spool ->
        val spoolMaterial = normalizeFilamentType(spool.material) ?: spool.material
        normalizeArchiveMaterialKey(slotMaterial) == normalizeArchiveMaterialKey(spoolMaterial)
    }
    return matching.ifEmpty { spools }
}
