package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryFillTest {

    @Test
    fun inventoryFill_fromSpoolWeight() {
        assertEquals(75, inventoryFillPercent(labelWeight = 1000, weightUsed = 250.0))
    }

    @Test
    fun inventoryFill_nullWhenWeightUsedMissing() {
        assertNull(inventoryFillPercent(labelWeight = 1000, weightUsed = null))
    }

    @Test
    fun inventoryFill_brandNewSpool() {
        assertEquals(100, inventoryFillPercent(labelWeight = 1000, weightUsed = 0.0))
    }

    @Test
    fun applyInventoryFill_mergesBySlotKey() {
        val slots = listOf(
            FilamentSlot(
                label = "A1",
                filamentType = "PLA",
                colorHex = "#FF0000",
                amsId = 0,
                trayId = 0,
                isLoaded = true,
            ),
        )
        val merged = applyInventoryFill(
            slots,
            mapOf(SlotInventoryKey(0, 0) to 42),
        )
        assertEquals(42, merged.first().remainPercent)
    }

    @Test
    fun externalTrayId_mapsGlobal254ToSlot0() {
        assertEquals(0, externalInventoryTrayId(254, 0))
        assertEquals(1, externalInventoryTrayId(255, 1))
    }
}
