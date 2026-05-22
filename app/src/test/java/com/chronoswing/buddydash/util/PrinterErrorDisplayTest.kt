package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterErrorDisplayTest {

    @Test
    fun parsePrinterHmsErrors_readsOpenApiFields() {
        val array = JSONArray(
            """[{"code":"0300-8001-0001-0001","attr":0,"module":2,"severity":1}]""",
        )
        val errors = parsePrinterHmsErrors(array)
        assertEquals(1, errors.size)
        assertEquals("0300-8001-0001-0001", errors.first().code)
        assertEquals(2, errors.first().module)
        assertEquals(1, errors.first().severity)
    }

    @Test
    fun parsePrinterHmsErrors_usesRuntimeMessageWhenPresent() {
        val array = JSONArray(
            """[{"code":"0300-8001","message":"Homing failed: toolhead blocked"}]""",
        )
        val line = parsePrinterHmsErrors(array).first().toDisplayLine()
        assertEquals("Homing failed: toolhead blocked", line)
    }

    @Test
    fun parsePrinterStatusErrorMessages_readsTopLevelFields() {
        val json = JSONObject(
            """{"error_code":"E001","error_message":"Axis stuck","status_reason":"homing"}""",
        )
        val messages = parsePrinterStatusFaultMessages(json)
        assertTrue(messages.any { it.contains("Axis stuck") })
        assertTrue(messages.any { it.contains("E001") })
        assertTrue(messages.any { it.contains("homing") })
    }

    @Test
    fun resolvePrinterErrorDisplay_showsFaultHmsLine() {
        val status = PrinterStatus(
            connected = true,
            rawState = "IDLE",
            fileName = null,
            progress = null,
            remainingTimeSeconds = null,
            nozzleTemp = null,
            bedTemp = null,
            hmsErrors = listOf(PrinterHmsError("0300-0C00-0001-0007")),
        )
        val display = status.resolvePrinterErrorDisplay("No details")
        assertTrue(display.showCard)
        assertTrue(display.hasKnownDetails)
        assertEquals(1, display.lines.size)
    }

    @Test
    fun resolvePrinterErrorDisplay_hidesCardWhenHealthy() {
        val status = PrinterStatus(
            connected = true,
            rawState = "IDLE",
            fileName = null,
            progress = null,
            remainingTimeSeconds = null,
            nozzleTemp = null,
            bedTemp = null,
        )
        val display = status.resolvePrinterErrorDisplay("No details")
        assertFalse(display.showCard)
    }
}
