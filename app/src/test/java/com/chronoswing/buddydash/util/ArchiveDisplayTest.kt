package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.FilamentUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
