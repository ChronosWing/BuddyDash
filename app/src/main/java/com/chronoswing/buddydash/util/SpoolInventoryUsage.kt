package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment

enum class SpoolInventoryCardUsage {
    Normal,
    InUse,
    Printing,
}

/** Live filament activity for one printer (status + active tray). */
data class PrinterFilamentActivity(
    val activityKind: PrinterActivityKind,
    val activeFilamentSlot: SlotInventoryKey?,
)

fun SpoolSlotAssignment.slotInventoryKey(): SlotInventoryKey? {
    val ams = amsId ?: return null
    val tray = trayId ?: return null
    return SlotInventoryKey(ams, tray)
}

fun isAssignmentOnActiveSlot(
    assignment: SpoolSlotAssignment,
    activeKey: SlotInventoryKey?,
): Boolean {
    val key = assignment.slotInventoryKey() ?: return false
    return activeKey != null && key == activeKey
}

fun isSpoolActivelyPrinting(
    spool: SpoolInventoryItem,
    printerActivityById: Map<Int, PrinterFilamentActivity>,
): Boolean {
    val assignment = spool.assignment ?: return false
    val activity = printerActivityById[assignment.printerId] ?: return false
    if (activity.activityKind != PrinterActivityKind.Printing) return false
    return isAssignmentOnActiveSlot(assignment, activity.activeFilamentSlot)
}

fun resolveSpoolInventoryCardUsage(
    spool: SpoolInventoryItem,
    printerActivityById: Map<Int, PrinterFilamentActivity>,
): SpoolInventoryCardUsage = when {
    isSpoolActivelyPrinting(spool, printerActivityById) -> SpoolInventoryCardUsage.Printing
    spool.assignment != null -> SpoolInventoryCardUsage.InUse
    else -> SpoolInventoryCardUsage.Normal
}
