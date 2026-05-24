package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterHmsError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BambuHmsWikiTest {

    @Test
    fun isLongHmsCode_matchesSixteenDigitPatterns() {
        assertTrue(BambuHmsLookup.isLongHmsCode("0300-8001-0001-0001"))
        assertTrue(BambuHmsLookup.isLongHmsCode("0300_8001_0001_0007"))
        assertFalse(BambuHmsLookup.isLongHmsCode("0xC010"))
        assertFalse(BambuHmsLookup.isLongHmsCode("0300-8001"))
    }

    @Test
    fun resolvePrinterModelWikiSlug_mapsKnownModels() {
        assertEquals("p1", BambuHmsLookup.resolvePrinterModelWikiSlug("Bambu Lab P1S"))
        assertEquals("a1", BambuHmsLookup.resolvePrinterModelWikiSlug("A1 mini"))
        assertEquals("x1", BambuHmsLookup.resolvePrinterModelWikiSlug("X1 Carbon"))
        assertEquals("h2d", BambuHmsLookup.resolvePrinterModelWikiSlug("H2D"))
        assertNull(BambuHmsLookup.resolvePrinterModelWikiSlug("Unknown Printer"))
    }

    @Test
    fun resolveWikiUrl_longCodeUsesModelSlug() {
        val entry = PrinterHmsError(code = "0300-8001-0001-0001")
        val url = BambuHmsLookup.resolveWikiUrl(entry, "P1S")
        assertEquals(
            "https://wiki.bambulab.com/en/p1/troubleshooting/hmscode/0300-8001-0001-0001",
            url,
        )
    }

    @Test
    fun resolveWikiUrl_longCodeFallsBackWhenModelUnknown() {
        val entry = PrinterHmsError(code = "0300-8001-0001-0001")
        val url = BambuHmsLookup.resolveWikiUrl(entry, "Mystery Model")
        assertEquals("https://wiki.bambulab.com/en/hms/error-code", url)
    }

    @Test
    fun resolveWikiUrl_shortCompoundUsesSharedPage() {
        val entry = PrinterHmsError(code = "0xC010", module = 5)
        val url = BambuHmsLookup.resolveWikiUrl(entry, "P1S")
        assertEquals("https://wiki.bambulab.com/en/hms/error-code", url)
    }

    @Test
    fun resolveWikiUrl_prefersVerifiedLookupUrl() {
        val entry = PrinterHmsError(code = "0x8011", module = 0x1200)
        val url = BambuHmsLookup.resolveWikiUrl(entry, "P1S")
        assertEquals("https://wiki.bambulab.com/en/hms/error-code", url)
    }
}
