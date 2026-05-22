package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveReprintTest {

    @Test
    fun resolveArchiveReprintPrinters_filtersBySlicedModel() {
        val archive = sampleArchive(slicedForModel = "Bambu Lab X1 Carbon")
        val printers = listOf(
            Printer(1, "X1C", "Bambu Lab X1 Carbon"),
            Printer(2, "A1", "Bambu Lab A1 mini"),
        )
        val options = resolveArchiveReprintPrinters(printers, archive)
        assertEquals(1, options.compatible.size)
        assertEquals(1, options.compatible.first().id)
        assertEquals(1, options.hiddenIncompatibleCount)
    }

    @Test
    fun resolveArchiveReprintPrinters_allWhenSlicedUnknown() {
        val archive = sampleArchive(slicedForModel = null)
        val printers = listOf(
            Printer(1, "X1C", "X1 Carbon"),
            Printer(2, "A1", "A1 mini"),
        )
        val options = resolveArchiveReprintPrinters(printers, archive)
        assertEquals(2, options.compatible.size)
        assertEquals(0, options.hiddenIncompatibleCount)
    }

    @Test
    fun evaluateQueueAndStartReadiness_readyWhenIdleAndPlateClear() {
        val status = reprintStatus(connected = true, rawState = "IDLE", awaitingPlateClear = false)
        val readiness = evaluateQueueAndStartReadiness(status, hasStartEndpoint = true)
        assertTrue(readiness.canQueueAndStart)
        assertEquals(QueueAndStartBlockReason.None, readiness.blockReason)
    }

    @Test
    fun evaluateQueueAndStartReadiness_blockedWhenPlateNotClear() {
        val status = reprintStatus(connected = true, rawState = "IDLE", awaitingPlateClear = true)
        val readiness = evaluateQueueAndStartReadiness(status, hasStartEndpoint = true)
        assertFalse(readiness.canQueueAndStart)
        assertEquals(QueueAndStartBlockReason.PlateNotClear, readiness.blockReason)
    }

    @Test
    fun evaluateQueueAndStartReadiness_blockedWhenPrinting() {
        val status = reprintStatus(connected = true, rawState = "RUNNING", awaitingPlateClear = false)
        val readiness = evaluateQueueAndStartReadiness(status, hasStartEndpoint = true)
        assertFalse(readiness.canQueueAndStart)
        assertEquals(QueueAndStartBlockReason.PrinterNotReady, readiness.blockReason)
    }

    @Test
    fun evaluateQueueAndStartReadiness_blockedWithoutStartEndpoint() {
        val status = reprintStatus(connected = true, rawState = "IDLE", awaitingPlateClear = false)
        val readiness = evaluateQueueAndStartReadiness(status, hasStartEndpoint = false)
        assertFalse(readiness.canQueueAndStart)
    }

    @Test
    fun defaultArchiveReprintPrinterId_prefersArchivePrinter() {
        val archive = sampleArchive(slicedForModel = null, printerId = 2)
        val compatible = listOf(
            ArchiveReprintPrinter(1, "One", null),
            ArchiveReprintPrinter(2, "Two", null),
        )
        assertEquals(2, defaultArchiveReprintPrinterId(archive, compatible))
    }

    private fun reprintStatus(
        connected: Boolean,
        rawState: String,
        awaitingPlateClear: Boolean?,
    ) = PrinterStatus(
        connected = connected,
        rawState = rawState,
        progress = null,
        fileName = null,
        remainingTimeSeconds = null,
        nozzleTemp = null,
        bedTemp = null,
        awaitingPlateClear = awaitingPlateClear,
    )

    private fun sampleArchive(
        slicedForModel: String?,
        printerId: Int = 1,
    ) = PrintArchive(
        id = 10,
        displayName = "Test",
        printerId = printerId,
        printerName = "Printer",
        printerModel = null,
        statusRaw = "completed",
        resultKind = ArchiveResultKind.Success,
        startedAtIso = null,
        completedAtIso = null,
        durationSeconds = null,
        filamentUsage = null,
        filamentType = null,
        filamentColor = null,
        failureReason = null,
        totalLayers = null,
        quantity = 2,
        projectName = null,
        slicedForModel = slicedForModel,
        notes = null,
    )
}
