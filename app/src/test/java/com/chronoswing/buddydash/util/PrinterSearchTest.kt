package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterSearchTest {

    @Test
    fun filterPrinters_matchesNameAndModelCaseInsensitive() {
        val printers = listOf(
            Printer(id = 1, name = "Shop P1S", model = "P1S"),
            Printer(id = 2, name = "Office A1", model = "A1 mini"),
        )
        assertEquals(1, filterPrintersForSearch(printers, "p1s").size)
        assertEquals(1, filterPrintersForSearch(printers, "a1").size)
        assertEquals(2, filterPrintersForSearch(printers, "").size)
    }

    @Test
    fun printerMatchesSearchQuery_emptyQueryMatchesAll() {
        val printer = Printer(id = 1, name = "Test", model = "X1")
        assertTrue(printerMatchesSearchQuery(printer, ""))
    }

    @Test
    fun activeFilter_includesBusyExcludesIdleAndOffline() {
        val printing = printer(id = 1, connected = true, rawState = "RUNNING")
        val busy = printer(id = 2, connected = true, rawState = "CALIBRATING")
        val idle = printer(id = 3, connected = true, rawState = "IDLE")
        val offline = printer(id = 4, connected = false, rawState = "IDLE")
        assertTrue(printerMatchesActiveFilter(printing))
        assertTrue(printerMatchesActiveFilter(busy))
        assertFalse(printerMatchesActiveFilter(idle))
        assertFalse(printerMatchesActiveFilter(offline))
    }

    @Test
    fun needsAttention_includesDueHmsAndExcludesDueSoon() {
        val dueMaint = Printer(
            id = 1,
            name = "A",
            model = null,
            liveStatus = status(connected = true, rawState = "IDLE"),
            maintenanceIndicator = MaintenanceHomeIndicator.Due,
        )
        val dueSoon = Printer(
            id = 2,
            name = "B",
            model = null,
            liveStatus = status(connected = true, rawState = "IDLE"),
            maintenanceIndicator = MaintenanceHomeIndicator.DueSoon,
        )
        val hms = printer(id = 3, connected = true, rawState = "IDLE", hms = 2)
        assertTrue(printerMatchesNeedsAttentionFilter(dueMaint))
        assertFalse(printerMatchesNeedsAttentionFilter(dueSoon))
        assertTrue(printerMatchesNeedsAttentionFilter(hms))
    }

    @Test
    fun applyHomePrinterSearch_combinesQueryAndFilter() {
        val printers = listOf(
            printer(id = 1, name = "Brother P1", connected = true, rawState = "RUNNING"),
            printer(id = 2, name = "Brother A1", connected = true, rawState = "IDLE"),
            printer(id = 3, name = "Shop", connected = true, rawState = "RUNNING"),
        )
        val result = applyHomePrinterSearch(printers, "bro", HomePrinterSearchFilter.Active)
        assertEquals(1, result.size)
        assertEquals(1, result.first().id)
    }

    private fun printer(
        id: Int,
        name: String = "Printer",
        connected: Boolean,
        rawState: String,
        hms: Int = 0,
    ) = Printer(
        id = id,
        name = name,
        model = null,
        liveStatus = status(connected = connected, rawState = rawState, hms = hms),
    )

    private fun status(
        connected: Boolean,
        rawState: String,
        hms: Int = 0,
    ) = PrinterStatus(
        connected = connected,
        rawState = rawState,
        progress = null,
        fileName = null,
        remainingTimeSeconds = null,
        nozzleTemp = null,
        bedTemp = null,
        hmsErrors = List(hms) { index ->
            PrinterHmsError(code = "0300-0C00-0001-000$index")
        },
    )
}
