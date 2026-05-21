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
    fun applyInventory_usesInventoryColorOverTray() {
        val slots = listOf(
            FilamentSlot(
                label = "A1",
                filamentType = "PLA",
                colorHex = "#0000FF",
                amsId = 0,
                trayId = 0,
                isLoaded = true,
            ),
        )
        val merged = applyInventoryToSlots(
            slots = slots,
            inventoryBySlot = mapOf(
                SlotInventoryKey(0, 0) to SlotInventoryInfo(
                    remainPercent = 42,
                    colorHex = "#FF0000",
                    spoolId = 7,
                    spoolName = "Bambu PLA Red",
                ),
            ),
            printerName = "Test",
            logColors = false,
        )
        assertEquals(42, merged.first().remainPercent)
        assertEquals("#FF0000", merged.first().colorHex)
    }

    @Test
    fun applyInventory_colorWithoutFill() {
        val slots = listOf(
            FilamentSlot(
                label = "B2",
                filamentType = "PETG",
                colorHex = "#0000FF",
                amsId = 0,
                trayId = 1,
                isLoaded = true,
            ),
        )
        val merged = applyInventoryToSlots(
            slots = slots,
            inventoryBySlot = mapOf(
                SlotInventoryKey(0, 1) to SlotInventoryInfo(
                    remainPercent = null,
                    colorHex = "#00FF00",
                    spoolId = 3,
                    spoolName = "Green PETG",
                ),
            ),
            printerName = "Test",
            logColors = false,
        )
        assertNull(merged.first().remainPercent)
        assertEquals("#00FF00", merged.first().colorHex)
    }

    @Test
    fun externalTrayId_mapsGlobal254ToSlot0() {
        assertEquals(0, externalInventoryTrayId(254, 0))
        assertEquals(1, externalInventoryTrayId(255, 1))
    }
}
