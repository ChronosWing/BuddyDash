package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.Printer
import org.junit.Assert.assertEquals
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
    fun defaultArchiveReprintPrinterId_prefersArchivePrinter() {
        val archive = sampleArchive(slicedForModel = null, printerId = 2)
        val compatible = listOf(
            ArchiveReprintPrinter(1, "One", null),
            ArchiveReprintPrinter(2, "Two", null),
        )
        assertEquals(2, defaultArchiveReprintPrinterId(archive, compatible))
    }

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
