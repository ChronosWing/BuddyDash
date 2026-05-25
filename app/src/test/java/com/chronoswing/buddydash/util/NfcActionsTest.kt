package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcActionsTest {

    // ── parseNfcDeepLink ──────────────────────────────────────────

    @Test
    fun parseNfcFromUriString_clearPlate() {
        val link = parseNfcFromUriString("buddydash://printer/42/clear-plate")
        assertNotNull(link)
        assertEquals("42", link!!.printerKey)
        assertEquals(NfcActionKind.ClearPlate, link.action)
    }

    @Test
    fun parseNfcFromUriString_togglePower() {
        val link = parseNfcFromUriString("buddydash://printer/7/toggle-power")
        assertNotNull(link)
        assertEquals("7", link!!.printerKey)
        assertEquals(NfcActionKind.TogglePower, link.action)
    }

    @Test
    fun parseNfcFromUriString_finish() {
        val link = parseNfcFromUriString("buddydash://printer/MECHABROBOT/finish")
        assertNotNull(link)
        assertEquals("MECHABROBOT", link!!.printerKey)
        assertEquals(NfcActionKind.Finish, link.action)
    }

    @Test
    fun parseNfcFromUriString_caseInsensitive() {
        val link = parseNfcFromUriString("BUDDYDASH://Printer/1/Toggle-Power")
        assertNotNull(link)
        assertEquals(NfcActionKind.TogglePower, link!!.action)
    }

    @Test
    fun parseNfcFromUriString_rejectsUnknownAction() {
        assertNull(parseNfcFromUriString("buddydash://printer/1/status"))
        assertNull(parseNfcFromUriString("buddydash://printer/1/prep"))
        assertNull(parseNfcFromUriString("buddydash://printer/1/power-on"))
    }

    @Test
    fun parseNfcFromUriString_rejectsUnknownHost() {
        assertNull(parseNfcFromUriString("buddydash://home/1/clear-plate"))
    }

    @Test
    fun parseNfcFromUriString_acceptsTrailingSlash() {
        val link = parseNfcFromUriString("buddydash://printer/42/finish/")
        assertNotNull(link)
        assertEquals(NfcActionKind.Finish, link!!.action)
    }

    // ── isPrinterSafeToPowerOff ───────────────────────────────────

    private fun status(rawState: String?, connected: Boolean = true) = PrinterStatus(
        connected = connected,
        rawState = rawState,
        progress = null,
        fileName = null,
        remainingTimeSeconds = null,
        nozzleTemp = null,
        bedTemp = null,
    )

    @Test
    fun safeToPowerOff_idle() {
        assertTrue(isPrinterSafeToPowerOff(status("IDLE")))
    }

    @Test
    fun safeToPowerOff_finish() {
        assertTrue(isPrinterSafeToPowerOff(status("FINISH")))
    }

    @Test
    fun safeToPowerOff_failed() {
        assertTrue(isPrinterSafeToPowerOff(status("FAILED")))
    }

    @Test
    fun unsafeToPowerOff_running() {
        assertFalse(isPrinterSafeToPowerOff(status("RUNNING")))
    }

    @Test
    fun unsafeToPowerOff_paused() {
        assertFalse(isPrinterSafeToPowerOff(status("PAUSE")))
    }

    @Test
    fun unsafeToPowerOff_heating() {
        assertFalse(isPrinterSafeToPowerOff(status("HEATING")))
    }

    @Test
    fun unsafeToPowerOff_calibrating() {
        assertFalse(isPrinterSafeToPowerOff(status("CALIBRATING")))
    }

    @Test
    fun unsafeToPowerOff_homing() {
        assertFalse(isPrinterSafeToPowerOff(status("HOMING")))
    }

    @Test
    fun unsafeToPowerOff_offline() {
        assertFalse(isPrinterSafeToPowerOff(status("IDLE", connected = false)))
    }

    @Test
    fun unsafeToPowerOff_null() {
        assertFalse(isPrinterSafeToPowerOff(null))
    }

    @Test
    fun unsafeToPowerOff_nullRawState() {
        assertFalse(isPrinterSafeToPowerOff(status(null)))
    }

    @Test
    fun unsafeToPowerOff_firmwareUpdate() {
        assertFalse(isPrinterSafeToPowerOff(status("FIRMWARE_UPDATE")))
    }

    @Test
    fun unsafeToPowerOff_loading() {
        assertFalse(isPrinterSafeToPowerOff(status("FILAMENT_LOADING")))
    }

    // ── buildNfcActionUri ─────────────────────────────────────────

    @Test
    fun buildNfcActionUri_clearPlate() {
        assertEquals(
            "buddydash://printer/7/clear-plate",
            buildNfcActionUri(7, NfcActionKind.ClearPlate),
        )
    }

    @Test
    fun buildNfcActionUri_togglePower() {
        assertEquals(
            "buddydash://printer/42/toggle-power",
            buildNfcActionUri(42, NfcActionKind.TogglePower),
        )
    }

    @Test
    fun buildNfcActionUri_finish() {
        assertEquals(
            "buddydash://printer/1/finish",
            buildNfcActionUri(1, NfcActionKind.Finish),
        )
    }

    // ── NfcActionDebounce ─────────────────────────────────────────

    @Test
    fun debounce_ignoresDuplicateWithinWindow() {
        assertTrue(NfcActionDebounce.shouldProcess("test-unique-key"))
        assertFalse(NfcActionDebounce.shouldProcess("test-unique-key"))
    }

    @Test
    fun debounce_allowsDifferentKeys() {
        assertTrue(NfcActionDebounce.shouldProcess("a-unique"))
        assertTrue(NfcActionDebounce.shouldProcess("b-unique"))
    }

    // ── NfcActionOutcome tiers ────────────────────────────────────

    @Test
    fun outcomeTiers_correct() {
        assertEquals(NfcActionOutcome.Tier.Success, NfcActionOutcome.PlateCleared("X").tier)
        assertEquals(NfcActionOutcome.Tier.Noop, NfcActionOutcome.PlateAlreadyClear.tier)
        assertEquals(NfcActionOutcome.Tier.Warning, NfcActionOutcome.PrinterBusyPlateUnchanged.tier)
        assertEquals(NfcActionOutcome.Tier.Failure, NfcActionOutcome.ApiFailed.tier)
        assertEquals(NfcActionOutcome.Tier.Success, NfcActionOutcome.PowerOn("X").tier)
        assertEquals(NfcActionOutcome.Tier.Success, NfcActionOutcome.PowerOff("X").tier)
        assertEquals(NfcActionOutcome.Tier.Warning, NfcActionOutcome.SmartOutletUnavailable.tier)
        assertEquals(NfcActionOutcome.Tier.Success, NfcActionOutcome.FinishedWithPowerOff.tier)
        assertEquals(NfcActionOutcome.Tier.Success, NfcActionOutcome.FinishedPlateClear.tier)
        assertEquals(NfcActionOutcome.Tier.Warning, NfcActionOutcome.PrinterBusyFinishSkipped.tier)
    }
}
