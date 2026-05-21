package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilamentParsingTest {

    @Test
    fun emptyTray_notLoaded() {
        assertFalse(
            isTrayLoaded(
                filamentType = null,
                colorHex = null,
                remainPercent = null,
            ),
        )
    }

    @Test
    fun loadedTray_byType() {
        assertTrue(
            isTrayLoaded(
                filamentType = "PLA",
                colorHex = "#FF0000",
                remainPercent = null,
            ),
        )
    }

    @Test
    fun trayColor_rrggbbaa_usesFirstSixDigits() {
        assertEquals("#FF0000", normalizeTrayColor("FF0000FF"))
        assertEquals("#00AE42", normalizeInventoryColor("00AE42FF"))
    }

    @Test
    fun allZeroColor_isNull() {
        assertNull(normalizeTrayColor("00000000"))
        assertNull(normalizeTrayColor("#000000"))
    }
}
