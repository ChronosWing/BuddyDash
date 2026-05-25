package com.chronoswing.buddydash.util

import android.net.Uri
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcClearPlateTest {

    @Test
    fun parseClearPlateDeepLink_acceptsPrinterId() {
        val link = parseFromUriString("buddydash://printer/42/clear-plate")
        assertNotNull(link)
        assertEquals("42", link?.printerKey)
    }

    @Test
    fun parseClearPlateDeepLink_acceptsPrinterName() {
        val link = parseFromUriString("buddydash://printer/MECHABROBOT/clear-plate")
        assertNotNull(link)
        assertEquals("MECHABROBOT", link?.printerKey)
    }

    @Test
    fun parseClearPlateDeepLink_rejectsUnknownHost() {
        assertNull(parseFromUriString("buddydash://home/42/clear-plate"))
    }

    @Test
    fun parseClearPlateDeepLink_rejectsMissingAction() {
        assertNull(parseFromUriString("buddydash://printer/42/status"))
    }

    @Test
    fun resolvePrinterByKey_prefersIdThenName() {
        val printers = listOf(
            Printer(id = 7, name = "MECHABROBOT", model = null),
            Printer(id = 8, name = "Other", model = null),
        )
        assertEquals(7, resolvePrinterByKey(printers, "7")?.id)
        assertEquals(7, resolvePrinterByKey(printers, "mechabrobot")?.id)
        assertEquals(8, resolvePrinterByKey(printers, "Other")?.id)
    }

    @Test
    fun blocksNfcPlateClear_blocksPrintingPausedAndHeating() {
        assertTrue(
            blocksNfcPlateClear(
                PrinterStatus(connected = true, rawState = "RUNNING", progress = null, fileName = null, remainingTimeSeconds = null, nozzleTemp = null, bedTemp = null),
            ),
        )
        assertTrue(
            blocksNfcPlateClear(
                PrinterStatus(connected = true, rawState = "PAUSE", progress = null, fileName = null, remainingTimeSeconds = null, nozzleTemp = null, bedTemp = null),
            ),
        )
        assertTrue(
            blocksNfcPlateClear(
                PrinterStatus(connected = true, rawState = "HEATING", progress = null, fileName = null, remainingTimeSeconds = null, nozzleTemp = null, bedTemp = null),
            ),
        )
        assertFalse(
            blocksNfcPlateClear(
                PrinterStatus(connected = true, rawState = "IDLE", progress = null, fileName = null, remainingTimeSeconds = null, nozzleTemp = null, bedTemp = null),
            ),
        )
    }

    @Test
    fun debounce_ignoresDuplicateWithinWindow() {
        assertTrue(NfcClearPlateDebounce.shouldProcess("clear-plate:42"))
        assertFalse(NfcClearPlateDebounce.shouldProcess("clear-plate:42"))
    }

    @Test
    fun isPlateKnownCleared_trueWhenAwaitingPlateClearFalse() {
        val status = PrinterStatus(
            connected = true,
            rawState = "IDLE",
            progress = null,
            fileName = null,
            remainingTimeSeconds = null,
            nozzleTemp = null,
            bedTemp = null,
            awaitingPlateClear = false,
        )
        assertTrue(isPlateKnownCleared(status))
    }

    @Test
    fun isPlateKnownCleared_falseWhenUnknownOrNeedsClear() {
        val unknown = PrinterStatus(
            connected = true,
            rawState = "IDLE",
            progress = null,
            fileName = null,
            remainingTimeSeconds = null,
            nozzleTemp = null,
            bedTemp = null,
            awaitingPlateClear = null,
        )
        val needsClear = unknown.copy(awaitingPlateClear = true)
        assertFalse(isPlateKnownCleared(unknown))
        assertFalse(isPlateKnownCleared(needsClear))
    }

    @Test
    fun isClearPlateAlreadyAcknowledged_detectsNoOpMessages() {
        assertTrue(isClearPlateAlreadyAcknowledged("Plate already cleared"))
        assertTrue(isClearPlateAlreadyAcknowledged("Already clear — no action needed"))
        assertFalse(isClearPlateAlreadyAcknowledged("Plate marked clear"))
    }
}
