package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import com.chronoswing.buddydash.util.FilamentSwatchColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoolInventoryDisplayTest {

    @Test
    fun isSpoolLowStock_usesPerSpoolThreshold() {
        assertTrue(isSpoolLowStock(remainPercent = 10, spoolThresholdPct = 15, globalThresholdPct = 20f))
        assertFalse(isSpoolLowStock(remainPercent = 18, spoolThresholdPct = 15, globalThresholdPct = 20f))
    }

    @Test
    fun isSpoolLowStock_fallsBackToGlobalThreshold() {
        assertTrue(isSpoolLowStock(remainPercent = 15, spoolThresholdPct = null, globalThresholdPct = 20f))
        assertFalse(isSpoolLowStock(remainPercent = 25, spoolThresholdPct = null, globalThresholdPct = 20f))
    }

    @Test
    fun formatSpoolCardTitle_doesNotIncludeNullLiteral() {
        val spool = sampleSpool(
            id = 1,
            material = "PETG",
            displayName = "KINGROON PETG",
            assignment = null,
        ).copy(brand = "KINGROON", colorName = null)
        assertEquals("KINGROON PETG", formatSpoolCardTitle(spool))
    }

    @Test
    fun applySpoolInventorySearch_matchesMaterialAndAssignment() {
        val spools = listOf(
            sampleSpool(id = 1, material = "PLA", displayName = "Bambu PLA", assignment = null),
            sampleSpool(
                id = 2,
                material = "PETG",
                displayName = "Orange PETG",
                assignment = SpoolSlotAssignment(1, "X1C", "A2"),
            ),
        )
        val pla = applySpoolInventorySearch(spools, "pla", SpoolInventoryFilter.All)
        assertEquals(1, pla.size)
        val printer = applySpoolInventorySearch(spools, "x1c", SpoolInventoryFilter.All)
        assertEquals(1, printer.size)
        val lowOnly = applySpoolInventorySearch(
            listOf(spools[0].copy(isLowStock = true), spools[1]),
            "",
            SpoolInventoryFilter.Low,
        )
        assertEquals(1, lowOnly.size)
    }

    private fun sampleSpool(
        id: Int,
        material: String,
        displayName: String,
        assignment: SpoolSlotAssignment?,
        isLowStock: Boolean = false,
    ) = SpoolInventoryItem(
        id = id,
        material = material,
        displayName = displayName,
        swatch = FilamentSwatchColors(colorHexes = listOf("#FF0000")),
        remainPercent = 50,
        lowStockThresholdPct = null,
        isLowStock = isLowStock,
        assignment = assignment,
    )
}
