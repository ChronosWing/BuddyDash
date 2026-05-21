package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilamentSwatchColorsTest {

    @Test
    fun parseExtraColorStops_splitsCommaSeparated() {
        assertEquals(
            listOf("#FF0000", "#00FF00", "#0000FF"),
            parseExtraColorStops("FF0000FF,00FF00FF,0000FFFF"),
        )
    }

    @Test
    fun mergeFilamentColorHexes_dedupesAndOrders() {
        assertEquals(
            listOf("#FF0000", "#00FF00"),
            mergeFilamentColorHexes("#FF0000", listOf("#00FF00")),
        )
    }

    @Test
    fun translucentEffect_detected() {
        assertTrue(isTranslucentEffect("translucent", alpha = 1f))
        assertTrue(isTranslucentEffect(null, alpha = 0.5f))
        assertFalse(isTranslucentEffect("matte", alpha = 1f))
    }

    @Test
    fun parseRgbaAlpha_readsAlphaChannel() {
        assertEquals(1f, parseRgbaAlpha("FF0000FF"), 0.01f)
        assertTrue(parseRgbaAlpha("FF000080") < 0.6f)
    }

    @Test
    fun fromTrayColor_opaqueBlack() {
        val swatch = FilamentSwatchColors.fromTrayColor("000000")
        assertEquals(listOf("#000000"), swatch.colorHexes)
        assertFalse(swatch.isTranslucent)
        assertEquals(1f, swatch.alpha, 0.01f)
    }
}
