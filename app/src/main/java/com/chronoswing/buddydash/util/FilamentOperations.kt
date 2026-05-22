package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi

enum class FilamentAction {
    Load,
    Unload,
}

enum class FilamentActionBlockReason {
    EndpointMissing,
    Offline,
    Printing,
    Busy,
    Error,
    SlotEmpty,
    NotActiveSlot,
}

data class FilamentActionAvailability(
    val allowed: Boolean,
    val reason: FilamentActionBlockReason? = null,
)

fun evaluateFilamentLoadAvailability(
    status: PrinterStatus?,
    slot: FilamentSlot,
    trayGlobalId: Int,
): FilamentActionAvailability {
    if (!BambuddyApi.hasAmsLoadEndpoint) {
        return FilamentActionAvailability(false, FilamentActionBlockReason.EndpointMissing)
    }
    if (trayGlobalId < 0) {
        return FilamentActionAvailability(false, FilamentActionBlockReason.SlotEmpty)
    }
    val base = evaluateFilamentPrinterAvailability(status)
    if (!base.allowed) return base
    if (!slot.isLoaded && slot.filamentType == null && slot.swatchColorHexes.isEmpty()) {
        return FilamentActionAvailability(false, FilamentActionBlockReason.SlotEmpty)
    }
    return FilamentActionAvailability(true)
}

fun evaluateFilamentUnloadAvailability(
    status: PrinterStatus?,
    isActiveSlot: Boolean,
): FilamentActionAvailability {
    if (!BambuddyApi.hasAmsUnloadEndpoint) {
        return FilamentActionAvailability(false, FilamentActionBlockReason.EndpointMissing)
    }
    val base = evaluateFilamentPrinterAvailability(status)
    if (!base.allowed) return base
    if (!isActiveSlot) {
        return FilamentActionAvailability(false, FilamentActionBlockReason.NotActiveSlot)
    }
    return FilamentActionAvailability(true)
}

private fun evaluateFilamentPrinterAvailability(
    status: PrinterStatus?,
): FilamentActionAvailability {
    if (status == null || !status.connected) {
        return FilamentActionAvailability(false, FilamentActionBlockReason.Offline)
    }
    return when (status.resolveActivityKind()) {
        PrinterActivityKind.Printing ->
            FilamentActionAvailability(false, FilamentActionBlockReason.Printing)
        PrinterActivityKind.Paused ->
            FilamentActionAvailability(false, FilamentActionBlockReason.Printing)
        PrinterActivityKind.Error ->
            FilamentActionAvailability(false, FilamentActionBlockReason.Error)
        PrinterActivityKind.Busy ->
            FilamentActionAvailability(false, FilamentActionBlockReason.Busy)
        PrinterActivityKind.Offline ->
            FilamentActionAvailability(false, FilamentActionBlockReason.Offline)
        PrinterActivityKind.Idle ->
            FilamentActionAvailability(true)
    }
}

fun filamentActionBlockMessage(reason: FilamentActionBlockReason): String = when (reason) {
    FilamentActionBlockReason.EndpointMissing -> "Not supported on this server"
    FilamentActionBlockReason.Offline -> "Unavailable while printer is offline"
    FilamentActionBlockReason.Printing -> "Unavailable while printing"
    FilamentActionBlockReason.Busy -> "Printer busy"
    FilamentActionBlockReason.Error -> "Unavailable while printer has an error"
    FilamentActionBlockReason.SlotEmpty -> "No filament in this slot"
    FilamentActionBlockReason.NotActiveSlot -> "Unload from the active slot only"
}
