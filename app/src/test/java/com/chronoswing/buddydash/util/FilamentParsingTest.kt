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
    fun opaqueBlack_sixDigitRgb() {
        assertEquals("#000000", normalizeTrayColor("000000"))
        assertEquals("#000000", normalizeInventoryColor("#000000"))
        assertEquals(1f, parseRgbaAlpha("000000"), 0.01f)
        assertFalse(isTranslucentEffect(null, parseRgbaAlpha("000000")))
    }

    @Test
    fun opaqueBlack_eightDigitRgba() {
        assertEquals("#000000", normalizeTrayColor("000000FF"))
        assertEquals(1f, parseRgbaAlpha("000000FF"), 0.01f)
        assertFalse(isTranslucentEffect(null, parseRgbaAlpha("000000FF")))
    }

    @Test
    fun transparentBlack_whenAlphaZero() {
        assertEquals("#000000", normalizeTrayColor("00000000"))
        assertTrue(parseRgbaAlpha("00000000") < 0.01f)
        assertTrue(isTranslucentEffect(null, parseRgbaAlpha("00000000")))
    }
}
