package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoolInventoryUsageTest {

    @Test
    fun resolveSpoolInventoryCardUsage_printingWhenActiveSlotMatches() {
        val spool = sampleSpool(
            assignment = SpoolSlotAssignment(
                printerId = 1,
                printerName = "Bot",
                slotLabel = "A1",
                amsId = 0,
                trayId = 1,
            ),
        )
        val activity = PrinterFilamentActivity(
            activityKind = PrinterActivityKind.Printing,
            activeFilamentSlot = SlotInventoryKey(0, 1),
        )
        assertEquals(
            SpoolInventoryCardUsage.Printing,
            resolveSpoolInventoryCardUsage(spool, mapOf(1 to activity)),
        )
    }

    @Test
    fun resolveSpoolInventoryCardUsage_inUseWhenAssignedButIdle() {
        val spool = sampleSpool(
            assignment = SpoolSlotAssignment(1, "Bot", "A1", amsId = 0, trayId = 1),
        )
        val activity = PrinterFilamentActivity(
            activityKind = PrinterActivityKind.Idle,
            activeFilamentSlot = SlotInventoryKey(0, 1),
        )
        assertEquals(
            SpoolInventoryCardUsage.InUse,
            resolveSpoolInventoryCardUsage(spool, mapOf(1 to activity)),
        )
    }

    @Test
    fun resolveSpoolInventoryCardUsage_inUseWhenPrintingOnDifferentSlot() {
        val spool = sampleSpool(
            assignment = SpoolSlotAssignment(1, "Bot", "A2", amsId = 0, trayId = 2),
        )
        val activity = PrinterFilamentActivity(
            activityKind = PrinterActivityKind.Printing,
            activeFilamentSlot = SlotInventoryKey(0, 1),
        )
        assertEquals(
            SpoolInventoryCardUsage.InUse,
            resolveSpoolInventoryCardUsage(spool, mapOf(1 to activity)),
        )
    }

    @Test
    fun isSpoolActivelyPrinting_falseWithoutAssignment() {
        val spool = sampleSpool(assignment = null)
        assertFalse(isSpoolActivelyPrinting(spool, emptyMap()))
    }

    @Test
    fun isAssignmentOnActiveSlot_matchesKey() {
        val assignment = SpoolSlotAssignment(1, "Bot", "A1", amsId = 0, trayId = 0)
        assertTrue(isAssignmentOnActiveSlot(assignment, SlotInventoryKey(0, 0)))
        assertFalse(isAssignmentOnActiveSlot(assignment, SlotInventoryKey(0, 1)))
    }

    private fun sampleSpool(assignment: SpoolSlotAssignment?) = SpoolInventoryItem(
        id = 10,
        material = "PETG",
        displayName = "Test",
        swatch = FilamentSwatchColors(colorHexes = listOf("#FF0000")),
        remainPercent = 50,
        lowStockThresholdPct = 10,
        isLowStock = false,
        assignment = assignment,
    )
}
