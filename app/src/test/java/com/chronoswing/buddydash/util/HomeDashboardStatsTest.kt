package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDashboardStatsTest {

    @Test
    fun homePrinterDashboardCounts_connectedAndPrinting() {
        val printers = listOf(
            printer(id = 1, connected = true, rawState = "RUNNING"),
            printer(id = 2, connected = true, rawState = "IDLE"),
            printer(id = 3, connected = false, rawState = "RUNNING"),
        )
        val counts = printers.homePrinterDashboardCounts()
        assertEquals(2, counts.online)
        assertEquals(1, counts.printing)
    }

    @Test
    fun homeDashboardVisibleChips_hidesZerosWhenAnotherPositive() {
        val chips = homeDashboardVisibleChips(online = 2, printing = 0, loadedSpools = 5)
        assertEquals(2, chips.size)
        assertTrue(chips.none { it.kind == HomeDashboardChipKind.Printing })
    }

    @Test
    fun homeDashboardVisibleChips_showsAllWhenAllZero() {
        val chips = homeDashboardVisibleChips(online = 0, printing = 0, loadedSpools = 0)
        assertEquals(3, chips.size)
    }

    @Test
    fun homeDashboardVisibleChips_omitsLoadedUntilKnown() {
        val chips = homeDashboardVisibleChips(online = 1, printing = 0, loadedSpools = null)
        assertEquals(1, chips.size)
        assertEquals(HomeDashboardChipKind.Online, chips.single().kind)
    }

    private fun printer(id: Int, connected: Boolean, rawState: String) =
        Printer(
            id = id,
            name = "P$id",
            model = null,
            liveStatus = PrinterStatus(connected = connected, rawState = rawState, progress = null, fileName = null, remainingTimeSeconds = null, nozzleTemp = null, bedTemp = null),
        )
}
