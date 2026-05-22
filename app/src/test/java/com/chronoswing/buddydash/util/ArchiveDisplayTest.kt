package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.PrintArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveDisplayTest {

    @Test
    fun parseArchiveResultKind_mapsKnownStatuses() {
        assertEquals(ArchiveResultKind.Success, parseArchiveResultKind("completed"))
        assertEquals(ArchiveResultKind.Success, parseArchiveResultKind("success"))
        assertEquals(ArchiveResultKind.Failed, parseArchiveResultKind("failed"))
        assertEquals(ArchiveResultKind.Cancelled, parseArchiveResultKind("cancelled"))
    }

    @Test
    fun formatArchiveListMetaLine_combinesDurationFilamentAndTime() {
        val archive = com.chronoswing.buddydash.data.model.PrintArchive(
            id = 1,
            displayName = "Test",
            printerId = 1,
            printerName = "X1C",
            printerModel = null,
            statusRaw = "completed",
            resultKind = ArchiveResultKind.Success,
            startedAtIso = null,
            completedAtIso = "2024-01-15T12:00:00Z",
            durationSeconds = 11_520,
            filamentUsage = FilamentUsage(weightGrams = 184.0),
            filamentType = null,
            filamentColor = null,
            failureReason = null,
            totalLayers = null,
            quantity = null,
            projectName = null,
            slicedForModel = null,
            notes = null,
        )
        val now = java.time.Instant.parse("2024-01-17T12:00:00Z").toEpochMilli()
        val meta = formatArchiveListMetaLine(archive, now)
        assertEquals("3h 12m • 184g • 2d ago", meta)
    }

    @Test
    fun formatArchiveMaterialLine_omitsRawHex() {
        val archive = sampleArchive(filamentType = "PETG", filamentColor = "#FFFFFF")
        assertEquals("PETG", formatArchiveMaterialLine(archive))
    }

    @Test
    fun parseArchiveFilamentSwatch_parsesSingleAndMultiColor() {
        val single = sampleArchive(filamentColor = "#0000FF")
        assertEquals(listOf("#0000FF"), parseArchiveFilamentSwatch(single)?.colorHexes)

        val multi = sampleArchive(filamentColor = "#FFFFFF,#000000")
        assertEquals(listOf("#FFFFFF", "#000000"), parseArchiveFilamentSwatch(multi)?.colorHexes)
    }

    @Test
    fun archiveHasMaterialDisplay_falseForNullColorLiteral() {
        val archive = sampleArchive(filamentType = null, filamentColor = "null")
        assertFalse(archiveHasMaterialDisplay(archive))
    }

    @Test
    fun archiveHasMaterialDisplay_trueForTypeOrColor() {
        assertTrue(archiveHasMaterialDisplay(sampleArchive(filamentType = "PLA", filamentColor = null)))
        assertTrue(archiveHasMaterialDisplay(sampleArchive(filamentType = null, filamentColor = "#FF0000")))
    }

    @Test
    fun applyArchiveListFilters_combinesPrinterSearchAndStatus() {
        val babyBro = sampleArchive(id = 1, printerId = 10, printerName = "BabyBroBot", displayName = "clip A")
        val other = sampleArchive(id = 2, printerId = 20, printerName = "Other", displayName = "clip B")
        val babyFailed = sampleArchive(
            id = 3,
            printerId = 10,
            printerName = "BabyBroBot",
            displayName = "widget",
            resultKind = ArchiveResultKind.Failed,
            statusRaw = "failed",
        )
        val archives = listOf(babyBro, other, babyFailed)

        val filtered = applyArchiveListFilters(
            archives = archives,
            query = "clip",
            filter = ArchiveResultFilter.Success,
            printerId = 10,
        )

        assertEquals(1, filtered.size)
        assertEquals(1, filtered.first().id)
    }

    @Test
    fun formatArchiveListMetaLine_nullWhenNoFields() {
        val archive = com.chronoswing.buddydash.data.model.PrintArchive(
            id = 1,
            displayName = "Test",
            printerId = null,
            printerName = null,
            printerModel = null,
            statusRaw = "",
            resultKind = ArchiveResultKind.Other,
            startedAtIso = null,
            completedAtIso = null,
            durationSeconds = null,
            filamentUsage = null,
            filamentType = null,
            filamentColor = null,
            failureReason = null,
            totalLayers = null,
            quantity = null,
            projectName = null,
            slicedForModel = null,
            notes = null,
        )
        assertNull(formatArchiveListMetaLine(archive))
    }

    private fun sampleArchive(
        id: Int = 1,
        displayName: String = "Test",
        printerId: Int = 1,
        printerName: String = "Printer",
        filamentType: String? = null,
        filamentColor: String? = null,
        resultKind: ArchiveResultKind = ArchiveResultKind.Success,
        statusRaw: String = "completed",
    ) = PrintArchive(
        id = id,
        displayName = displayName,
        printerId = printerId,
        printerName = printerName,
        printerModel = null,
        statusRaw = statusRaw,
        resultKind = resultKind,
        startedAtIso = null,
        completedAtIso = null,
        durationSeconds = null,
        filamentUsage = null,
        filamentType = filamentType,
        filamentColor = filamentColor,
        failureReason = null,
        totalLayers = null,
        quantity = null,
        projectName = null,
        slicedForModel = null,
        notes = null,
    )
}
