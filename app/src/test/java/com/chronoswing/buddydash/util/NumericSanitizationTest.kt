package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumericSanitizationTest {

    @Test
    fun finiteOrNull_rejectsNaNAndInfinity() {
        assertNull(Double.NaN.finiteOrNull())
        assertNull(Double.POSITIVE_INFINITY.finiteOrNull())
        assertEquals(42.0, 42.0.finiteOrNull())
    }

    @Test
    fun percentOrNull_rejectsInvalidProgress() {
        assertNull(Float.NaN.percentOrNull())
        assertNull((-1f).percentOrNull())
        assertNull(150f.percentOrNull())
        assertEquals(50f, 50f.percentOrNull())
    }

    @Test
    fun clampFinite_neverReturnsNaN() {
        assertEquals(0f, Float.NaN.clampFinite(0f, 1f))
        assertEquals(0.75f, 0.75f.clampFinite(0f, 1f))
        assertEquals(1f, 2f.clampFinite(0f, 1f))
    }

    @Test
    fun formatTemp_handlesNaNWithoutCrashing() {
        assertEquals("—", formatTemp(Double.NaN))
        assertEquals("—", formatTemp(null))
        assertEquals("220°C", formatTemp(220.0))
    }

    @Test
    fun formatProgress_handlesNaNWithoutCrashing() {
        assertEquals("—", formatProgress(Float.NaN))
        assertEquals("—", formatProgress(null))
        assertEquals("50%", formatProgress(50f))
    }

    @Test
    fun offlineDetailLabels_ignoreNaNProgress() {
        val status = PrinterStatus(
            connected = false,
            rawState = null,
            progress = Float.NaN,
            fileName = null,
            remainingTimeSeconds = null,
            nozzleTemp = Double.NaN,
            bedTemp = Double.NaN,
        )
        val labels = status.toDetailLabels()
        assertFalse(labels.isActivePrint)
        assertNull(labels.progressFraction)
        assertEquals("—", labels.nozzleTemp)
        assertEquals("—", labels.bedTemp)
    }
}
