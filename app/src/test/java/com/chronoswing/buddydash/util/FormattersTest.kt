package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun formatTemp_roundsToWholeDegrees() {
        assertEquals("255°C", formatTemp(255.0))
        assertEquals("70°C", formatTemp(70.0))
        assertEquals("221°C", formatTemp(220.6))
        assertEquals("255°", formatTempShort(255.0))
    }
}
