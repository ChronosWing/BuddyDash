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
    fun homePrinterGridColumnCount_delegatesToExpandedGridHelper() {
        assertEquals(2, homePrinterGridColumnCount(600))
        assertEquals(1, homePrinterGridColumnCount(599))
    }

    @Test
    fun buddyDashExpandedGridColumnCount_twoColumnsAtBreakpointAndAbove() {
        assertEquals(2, buddyDashExpandedGridColumnCount(600))
        assertEquals(2, buddyDashExpandedGridColumnCount(840))
    }

    @Test
    fun buddyDashExpandedGridColumnCount_singleColumnBelowBreakpoint() {
        assertEquals(1, buddyDashExpandedGridColumnCount(599))
        assertEquals(1, buddyDashExpandedGridColumnCount(360))
    }
}
