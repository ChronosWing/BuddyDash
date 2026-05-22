package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilamentAssignOperationsTest {

    @Test
    fun evaluateSpoolAssignmentConflict_noneWhenStorage() {
        val spool = sampleSpool(assignment = null)
        assertEquals(
            SpoolAssignmentTargetConflict.None,
            evaluateSpoolAssignmentConflict(spool, 1, "A1", currentSlotAssignedSpoolId = null),
        )
    }

    @Test
    fun evaluateSpoolAssignmentConflict_alreadyOnTargetBySlotAssignment() {
        val spool = sampleSpool(
            assignment = SpoolSlotAssignment(printerId = 1, printerName = "Bot", slotLabel = "A1"),
        )
        assertEquals(
            SpoolAssignmentTargetConflict.AlreadyOnTarget,
            evaluateSpoolAssignmentConflict(spool, 1, "A1", currentSlotAssignedSpoolId = null),
        )
    }

    @Test
    fun evaluateSpoolAssignmentConflict_alreadyOnTargetByAssignedSpoolId() {
        val spool = sampleSpool(id = 42, assignment = null)
        assertEquals(
            SpoolAssignmentTargetConflict.AlreadyOnTarget,
            evaluateSpoolAssignmentConflict(spool, 1, "A1", currentSlotAssignedSpoolId = 42),
        )
    }

    @Test
    fun evaluateSpoolAssignmentConflict_assignedElsewhere() {
        val spool = sampleSpool(
            assignment = SpoolSlotAssignment(printerId = 2, printerName = "Other", slotLabel = "AMS-A2"),
        )
        val conflict = evaluateSpoolAssignmentConflict(spool, 1, "A1", currentSlotAssignedSpoolId = null)
        assertTrue(conflict is SpoolAssignmentTargetConflict.AssignedElsewhere)
        assertEquals(
            "Other • AMS-A2",
            formatSpoolAssignmentLocationBrief((conflict as SpoolAssignmentTargetConflict.AssignedElsewhere).assignment),
        )
    }

    private fun sampleSpool(
        id: Int = 1,
        assignment: SpoolSlotAssignment? = null,
    ): SpoolInventoryItem = SpoolInventoryItem(
        id = id,
        material = "PETG",
        displayName = "Test",
        swatch = FilamentSwatchColors(colorHexes = listOf("#FF0000")),
        remainPercent = 50,
        lowStockThresholdPct = 10,
        isLowStock = false,
        assignment = assignment,
    )
}
