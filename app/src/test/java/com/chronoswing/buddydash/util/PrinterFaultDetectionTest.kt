package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterFaultDetectionTest {

    @Test
    fun isFault_trueForHmsErrorAlertLevel() {
        val fault = PrinterHmsError(code = "0300-0C00-0001-0007")
        assertTrue(fault.isFault())
    }

    @Test
    fun isFault_falseForHmsNotificationAlertLevel() {
        val notification = PrinterHmsError(code = "0300-0C00-0003-0007")
        assertFalse(notification.isFault())
    }

    @Test
    fun isFault_falseForHmsWarningAlertLevel() {
        val warning = PrinterHmsError(code = "0300-0C00-0002-0007")
        assertFalse(warning.isFault())
    }

    @Test
    fun resolveActivityKind_homingWithNotificationHms_isBusyNotError() {
        val status = PrinterStatus(
            connected = true,
            rawState = "HOMING",
            fileName = null,
            progress = null,
            remainingTimeSeconds = null,
            nozzleTemp = null,
            bedTemp = null,
            hmsErrors = listOf(PrinterHmsError(code = "0300-0C00-0003-0001")),
        )
        assertEquals(PrinterActivityKind.Busy, status.resolveActivityKind())
        assertFalse(status.hasActiveFault())
    }

    @Test
    fun resolveActivityKind_idleWithFaultHms_isError() {
        val status = PrinterStatus(
            connected = true,
            rawState = "IDLE",
            fileName = null,
            progress = null,
            remainingTimeSeconds = null,
            nozzleTemp = null,
            bedTemp = null,
            hmsErrors = listOf(PrinterHmsError(code = "0300-0C00-0001-0007")),
        )
        assertEquals(PrinterActivityKind.Error, status.resolveActivityKind())
        assertEquals(1, status.hmsErrorCount)
    }

    @Test
    fun hmsErrorCount_ignoresNonFaultEntries() {
        val status = PrinterStatus(
            connected = true,
            rawState = "IDLE",
            fileName = null,
            progress = null,
            remainingTimeSeconds = null,
            nozzleTemp = null,
            bedTemp = null,
            hmsErrors = listOf(
                PrinterHmsError(code = "0300-0C00-0003-0001"),
                PrinterHmsError(code = "0300-0C00-0002-0001"),
                PrinterHmsError(code = "0300-0C00-0001-0007"),
            ),
        )
        assertEquals(1, status.hmsErrorCount)
    }
}
