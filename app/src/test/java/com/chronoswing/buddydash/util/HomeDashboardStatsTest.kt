package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
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

    private fun printer(id: Int, connected: Boolean, rawState: String) =
        Printer(
            id = id,
            name = "P$id",
            model = null,
            liveStatus = PrinterStatus(
                connected = connected,
                rawState = rawState,
                progress = null,
                fileName = null,
                remainingTimeSeconds = null,
                nozzleTemp = null,
                bedTemp = null,
            ),
        )
}
