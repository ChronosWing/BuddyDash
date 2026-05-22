package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilamentSlotInventoryTest {

    @Test
    fun encodeBambuddyTrayId_amsSlot() {
        val slot = FilamentSlot(
            label = "A2",
            filamentType = "PETG",
            swatchColorHexes = listOf("#0000FF"),
            amsId = 1,
            trayId = 1,
            isLoaded = true,
        )
        assertEquals(5, encodeBambuddyTrayId(slot))
    }

    @Test
    fun encodeBambuddyTrayId_external() {
        val slot = FilamentSlot(
            label = "Ext",
            filamentType = "TPU",
            swatchColorHexes = listOf("#00FF00"),
            amsId = EXTERNAL_AMS_ID,
            trayId = 0,
            isLoaded = true,
            isExternal = true,
        )
        assertEquals(254, encodeBambuddyTrayId(slot))
    }

    @Test
    fun buildFilamentSlotDisplays_assignmentMatch_usesSpoolTitle() {
        val spool = sampleSpool(
            id = 42,
            brand = "KINGROON",
            colorName = "Blue",
            material = "PETG",
            subtype = "Basic",
        )
        val slot = FilamentSlot(
            label = "A1",
            filamentType = "PETG",
            swatchColorHexes = listOf("#0000FF"),
            amsId = 0,
            trayId = 0,
            isLoaded = true,
            inventorySpoolId = 42,
        )
        val display = buildFilamentSlotDisplays(
            slots = listOf(slot),
            activeKey = SlotInventoryKey(0, 0),
            printerId = 1,
            inventoryBySlot = emptyMap(),
            spoolsById = mapOf(42 to spool),
            spoolsAssignedToPrinter = listOf(spool),
        ).single()

        assertEquals(FilamentSpoolMatchKind.Assignment, display.matchKind)
        assertEquals("KINGROON PETG Blue", display.primaryTitle)
        assertEquals("PETG · Basic", display.subtitle)
        assertTrue(display.isTappable)
        assertEquals(42, display.spoolId)
    }

    @Test
    fun buildFilamentSlotDisplays_heuristicOnlyWhenUnique() {
        val blue = sampleSpool(
            id = 1,
            brand = "SUNLU",
            colorName = "Blue",
            material = "TPU",
            swatchHex = "#0000FF",
        )
        val duplicateBlue = blue.copy(id = 2, brand = "OTHER", displayName = "OTHER TPU")
        val slot = FilamentSlot(
            label = "Ext",
            filamentType = "TPU",
            swatchColorHexes = listOf("#0000FF"),
            amsId = EXTERNAL_AMS_ID,
            trayId = 0,
            isLoaded = true,
            isExternal = true,
        )
        val ambiguous = buildFilamentSlotDisplays(
            slots = listOf(slot),
            activeKey = null,
            printerId = 1,
            inventoryBySlot = emptyMap(),
            spoolsById = mapOf(1 to blue, 2 to duplicateBlue),
            spoolsAssignedToPrinter = listOf(blue, duplicateBlue),
        ).single()
        assertEquals(FilamentSpoolMatchKind.None, ambiguous.matchKind)
        assertFalse(ambiguous.isTappable)
        assertNull(ambiguous.spoolId)

        val unique = buildFilamentSlotDisplays(
            slots = listOf(slot),
            activeKey = null,
            printerId = 1,
            inventoryBySlot = emptyMap(),
            spoolsById = mapOf(1 to blue),
            spoolsAssignedToPrinter = listOf(blue),
        ).single()
        assertEquals(FilamentSpoolMatchKind.Heuristic, unique.matchKind)
        assertTrue(unique.isTappable)
        assertEquals("SUNLU TPU Blue", unique.primaryTitle)
    }

    @Test
    fun buildFilamentSlotDisplays_noMatch_fallsBackToFilamentType() {
        val slot = FilamentSlot(
            label = "A3",
            filamentType = "PETG",
            swatchColorHexes = emptyList(),
            amsId = 0,
            trayId = 2,
            isLoaded = true,
        )
        val display = buildFilamentSlotDisplays(
            slots = listOf(slot),
            activeKey = null,
            printerId = 1,
            inventoryBySlot = emptyMap(),
            spoolsById = emptyMap(),
            spoolsAssignedToPrinter = emptyList(),
        ).single()

        assertEquals("PETG", display.primaryTitle)
        assertNull(display.subtitle)
        assertFalse(display.isTappable)
    }

    private fun sampleSpool(
        id: Int,
        brand: String?,
        colorName: String?,
        material: String,
        subtype: String? = null,
        swatchHex: String = "#0000FF",
    ) = SpoolInventoryItem(
        id = id,
        material = material,
        subtype = subtype,
        colorName = colorName,
        brand = brand,
        swatch = FilamentSwatchColors(colorHexes = listOf(swatchHex)),
        remainPercent = 80,
        lowStockThresholdPct = null,
        isLowStock = false,
        displayName = "$brand $material",
        assignment = SpoolSlotAssignment(
            printerId = 1,
            printerName = "Printer",
            slotLabel = "A1",
        ),
    )
}
