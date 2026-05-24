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

    // ---- resolveHmsAlertSeverity tests ----

    @Test
    fun resolveHmsAlertSeverity_emptyList_isOk() {
        val status = PrinterStatus(
            connected = true, rawState = "IDLE",
            fileName = null, progress = null, remainingTimeSeconds = null,
            nozzleTemp = null, bedTemp = null,
        )
        assertEquals(HmsSeverity.Ok, status.resolveHmsAlertSeverity())
    }

    @Test
    fun resolveHmsAlertSeverity_notificationOnly_isUnknown_neverOk() {
        // Notification-level entries exist → must NOT return Ok.
        // Ok is reserved for an empty hms_errors list.
        val status = PrinterStatus(
            connected = true, rawState = "IDLE",
            fileName = null, progress = null, remainingTimeSeconds = null,
            nozzleTemp = null, bedTemp = null,
            hmsErrors = listOf(PrinterHmsError(code = "0300-0C00-0003-0001")),
        )
        assertEquals(HmsSeverity.Unknown, status.resolveHmsAlertSeverity())
    }

    @Test
    fun resolveHmsAlertSeverity_warningEntry_isWarning() {
        val status = PrinterStatus(
            connected = true, rawState = "IDLE",
            fileName = null, progress = null, remainingTimeSeconds = null,
            nozzleTemp = null, bedTemp = null,
            hmsErrors = listOf(PrinterHmsError(code = "0300-0C00-0002-0001")),
        )
        assertEquals(HmsSeverity.Warning, status.resolveHmsAlertSeverity())
    }

    @Test
    fun resolveHmsAlertSeverity_errorEntry_isError() {
        val status = PrinterStatus(
            connected = true, rawState = "IDLE",
            fileName = null, progress = null, remainingTimeSeconds = null,
            nozzleTemp = null, bedTemp = null,
            hmsErrors = listOf(PrinterHmsError(code = "0300-0C00-0001-0007")),
        )
        assertEquals(HmsSeverity.Error, status.resolveHmsAlertSeverity())
    }

    @Test
    fun resolveHmsAlertSeverity_mixedEntries_errorWins() {
        val status = PrinterStatus(
            connected = true, rawState = "IDLE",
            fileName = null, progress = null, remainingTimeSeconds = null,
            nozzleTemp = null, bedTemp = null,
            hmsErrors = listOf(
                PrinterHmsError(code = "0300-0C00-0003-0001"),  // notification
                PrinterHmsError(code = "0300-0C00-0002-0001"),  // warning
                PrinterHmsError(code = "0300-0C00-0001-0007"),  // error
            ),
        )
        assertEquals(HmsSeverity.Error, status.resolveHmsAlertSeverity())
    }

    @Test
    fun resolveHmsAlertSeverity_unknownCode_noSeverity_isUnknown() {
        // Entry with unparseable code and null severity → Unknown, never Ok.
        val status = PrinterStatus(
            connected = true, rawState = "IDLE",
            fileName = null, progress = null, remainingTimeSeconds = null,
            nozzleTemp = null, bedTemp = null,
            hmsErrors = listOf(PrinterHmsError(code = "SHORT", severity = null)),
        )
        assertEquals(HmsSeverity.Unknown, status.resolveHmsAlertSeverity())
    }

    @Test
    fun resolveHmsAlertSeverity_severityFallback_3_isUnknown() {
        // severity=3 (Notification via int fallback) → Unknown (not Ok).
        val status = PrinterStatus(
            connected = true, rawState = "IDLE",
            fileName = null, progress = null, remainingTimeSeconds = null,
            nozzleTemp = null, bedTemp = null,
            hmsErrors = listOf(PrinterHmsError(code = "SHORT", severity = 3)),
        )
        assertEquals(HmsSeverity.Unknown, status.resolveHmsAlertSeverity())
    }

    // ── BambuHmsLookup ──────────────────────────────────────────────────────

    @Test
    fun bambuHmsLookup_formatDisplayCode_module5_code0xc010() {
        // module=5 → "0500", code="0xc010" → "C010"
        assertEquals("[0500-C010]", BambuHmsLookup.formatDisplayCode(5, "0xc010"))
    }

    @Test
    fun bambuHmsLookup_formatDisplayCode_uppercasesInput() {
        assertEquals("[0500-C010]", BambuHmsLookup.formatDisplayCode(5, "0XC010"))
    }

    @Test
    fun bambuHmsLookup_formatDisplayCode_module3() {
        assertEquals("[0300-4000]", BambuHmsLookup.formatDisplayCode(3, "0x4000"))
    }

    @Test
    fun bambuHmsLookup_lookup_microSdCard_returnsWarning() {
        val info = BambuHmsLookup.lookup(5, "0xc010")
        assertEquals(HmsAlertLevel.Warning, info?.alertLevel)
    }

    @Test
    fun bambuHmsLookup_lookup_microSdCard_hasMessage() {
        val info = BambuHmsLookup.lookup(5, "0xc010")
        assertTrue(info?.message?.contains("MicroSD") == true)
    }

    @Test
    fun resolveHmsAlertSeverity_lookupOverridesApiSeverity() {
        // API sends severity=3 (which would map to Notification/Unknown),
        // but lookup table classifies [0500-C010] as Warning.
        val status = PrinterStatus(
            connected = true, rawState = "IDLE",
            fileName = null, progress = null, remainingTimeSeconds = null,
            nozzleTemp = null, bedTemp = null,
            hmsErrors = listOf(PrinterHmsError(code = "0xc010", module = 5, severity = 3)),
        )
        assertEquals(HmsSeverity.Warning, status.resolveHmsAlertSeverity())
    }
}
