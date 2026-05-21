package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SpoolFieldDisplayTest {

    @Test
    fun formatSpoolCardTitle_omitsNullColor() {
        val spool = sampleSpool(colorName = null, brand = "KINGROON")
        assertEquals("KINGROON PETG", formatSpoolCardTitle(spool))
    }

    @Test
    fun formatSpoolCardTitle_includesColorWhenPresent() {
        val spool = sampleSpool(colorName = "Blue", brand = "KINGROON")
        assertEquals("KINGROON PETG Blue", formatSpoolCardTitle(spool))
    }

    @Test
    fun formatSpoolLocationLine_storageWhenUnassigned() {
        val spool = sampleSpool(assignment = null)
        assertEquals("Storage", formatSpoolLocationLine(spool))
    }

    @Test
    fun formatSpoolLocationLine_loadedWithoutNullSlot() {
        val spool = sampleSpool(
            assignment = SpoolSlotAssignment(
                printerId = 1,
                printerName = "BabyBroBot",
                slotLabel = "AMS-A1",
            ),
        )
        assertEquals("Loaded • BabyBroBot • AMS-A1", formatSpoolLocationLine(spool))
    }

    @Test
    fun isMeaningfulSpoolField_rejectsNullLiteral() {
        assertFalse(isMeaningfulSpoolField("null"))
        assertFalse(isMeaningfulSpoolField("  "))
    }

    @Test
    fun formatArchiveMaterialLine_omitsHexFromListLabel() {
        val archive = com.chronoswing.buddydash.data.model.PrintArchive(
            id = 1,
            displayName = "Test",
            printerId = null,
            printerName = null,
            printerModel = null,
            statusRaw = "completed",
            resultKind = com.chronoswing.buddydash.data.model.ArchiveResultKind.Success,
            startedAtIso = null,
            completedAtIso = null,
            durationSeconds = null,
            filamentUsage = null,
            filamentType = "PETG",
            filamentColor = "#FFFFFF",
            failureReason = null,
            totalLayers = null,
            quantity = null,
            projectName = null,
            slicedForModel = null,
            notes = null,
        )
        assertEquals("PETG", formatArchiveMaterialLine(archive))
    }

    private fun sampleSpool(
        brand: String? = null,
        colorName: String? = null,
        assignment: SpoolSlotAssignment? = null,
    ) = SpoolInventoryItem(
        id = 1,
        material = "PETG",
        brand = brand,
        colorName = colorName,
        swatch = FilamentSwatchColors(colorHexes = listOf("#0000FF")),
        remainPercent = 50,
        lowStockThresholdPct = null,
        isLowStock = false,
        displayName = "PETG",
        assignment = assignment,
    )
}
