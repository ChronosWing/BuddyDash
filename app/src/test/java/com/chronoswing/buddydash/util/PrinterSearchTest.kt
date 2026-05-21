package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer
import org.junit.Assert.assertEquals
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
}
