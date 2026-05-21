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
        filamentType: String? = null,
        filamentColor: String? = null,
    ) = PrintArchive(
        id = 1,
        displayName = "Test",
        printerId = 1,
        printerName = "Printer",
        printerModel = null,
        statusRaw = "completed",
        resultKind = ArchiveResultKind.Success,
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
