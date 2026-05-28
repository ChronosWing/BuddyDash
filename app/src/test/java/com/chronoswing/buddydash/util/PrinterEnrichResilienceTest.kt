package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PrinterEnrichResilienceTest {

    @Test
    fun withEnrichFallback_addsDisconnectedStatusWhenMissing() {
        val printer = Printer(id = 1, name = "Offline", model = "X1C")
        val fallback = printer.withEnrichFallback()
        assertNotNull(fallback.liveStatus)
        assertFalse(fallback.liveStatus!!.connected)
        assertEquals(PrinterActivityKind.Offline, fallback.liveStatus!!.resolveActivityKind())
    }

    @Test
    fun withEnrichFallback_preservesExistingStatus() {
        val status = PrinterStatus(
            connected = true,
            rawState = "IDLE",
            progress = null,
            fileName = null,
            remainingTimeSeconds = null,
            nozzleTemp = null,
            bedTemp = null,
        )
        val printer = Printer(id = 2, name = "Online", model = null, liveStatus = status)
        assertEquals(status, printer.withEnrichFallback().liveStatus)
    }

    @Test
    fun parseDegradedPrinterStatus_handlesPartialOfflinePayload() {
        val json = JSONObject(
            """
            {"id":3,"connected":false,"state":"OFFLINE","hms_errors":null}
            """.trimIndent(),
        )
        val status = parseDegradedPrinterStatus(json)
        assertFalse(status.connected)
        assertEquals("OFFLINE", status.rawState)
        assertEquals(PrinterActivityKind.Offline, status.resolveActivityKind())
    }

    @Test
    fun optSafeInt_returnsNullForNonNumericValues() {
        val json = JSONObject("""{"tray_now":"invalid"}""")
        assertNull(json.optSafeInt("tray_now"))
    }

    @Test
    fun resolveActiveFilamentSlot_ignoresInvalidTrayNow() {
        val json = JSONObject("""{"tray_now":"bad"}""")
        assertNull(resolveActiveFilamentSlot(json, slots = emptyList()))
    }
}
