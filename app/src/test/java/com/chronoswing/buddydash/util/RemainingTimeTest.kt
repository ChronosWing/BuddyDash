package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemainingTimeTest {

    @Test
    fun remainingMinutesToSeconds_convertsApiMinutes() {
        assertEquals(82 * 60, remainingMinutesToSeconds(82))
    }

    @Test
    fun remainingMinutesToSeconds_hidesZeroOrMissing() {
        assertNull(remainingMinutesToSeconds(null))
        assertNull(remainingMinutesToSeconds(0))
    }

    @Test
    fun formatEta_displayFormats() {
        assertEquals("1h 12m", formatEta(72 * 60))
        assertEquals("2h 03m", formatEta(2 * 3600 + 3 * 60))
        assertEquals("5m", formatEta(5 * 60))
        assertEquals("<1m", formatEta(30))
        assertNull(formatEta(null))
        assertNull(formatEta(0))
    }
}
