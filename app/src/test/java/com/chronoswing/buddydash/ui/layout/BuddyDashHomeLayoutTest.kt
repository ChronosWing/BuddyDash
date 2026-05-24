package com.chronoswing.buddydash.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class BuddyDashHomeLayoutTest {

    @Test
    fun homePrinterGridColumnCount_singleColumnBelowBreakpoint() {
        assertEquals(1, homePrinterGridColumnCount(599))
        assertEquals(1, homePrinterGridColumnCount(360))
    }

    @Test
    fun homePrinterGridColumnCount_twoColumnsAtBreakpointAndAbove() {
        assertEquals(2, homePrinterGridColumnCount(600))
        assertEquals(2, homePrinterGridColumnCount(840))
    }
}
