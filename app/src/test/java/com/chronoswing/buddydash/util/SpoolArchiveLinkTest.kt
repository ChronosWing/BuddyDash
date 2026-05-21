package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoolArchiveLinkTest {

    @Test
    fun matchArchivesForSpool_prefersExactSpoolId() {
        val spool = sampleSpool(id = 5)
        val archives = listOf(
            sampleArchive(id = 1, spoolId = 5, filamentType = "PLA"),
            sampleArchive(id = 2, spoolId = null, filamentType = "PETG", filamentColor = "#0000FF"),
        )
        val matches = matchArchivesForSpool(spool, archives)
        assertTrue(matches.isExactSpoolId)
        assertEquals(1, matches.archives.size)
        assertEquals(1, matches.archives.first().id)
    }

    @Test
    fun matchArchivesForSpool_fallsBackToMaterialColor() {
        val spool = sampleSpool(id = 5)
        val archives = listOf(
            sampleArchive(id = 2, spoolId = null, filamentType = "PETG", filamentColor = "#0000FF"),
        )
        val matches = matchArchivesForSpool(spool, archives)
        assertFalse(matches.isExactSpoolId)
        assertEquals(1, matches.archives.size)
    }

    @Test
    fun resolveArchiveMaterialNavigation_spoolDetailWhenIdMatches() {
        val archive = sampleArchive(id = 1, spoolId = 5, filamentType = "PETG", filamentColor = "#0000FF")
        val spools = listOf(sampleSpool(id = 5))
        val nav = resolveArchiveMaterialNavigation(archive, spools)
        assertTrue(nav is ArchiveMaterialNavigation.SpoolDetail)
        assertEquals(5, (nav as ArchiveMaterialNavigation.SpoolDetail).spoolId)
    }

    private fun sampleSpool(id: Int) = SpoolInventoryItem(
        id = id,
        material = "PETG",
        colorName = "Blue",
        swatch = FilamentSwatchColors(colorHexes = listOf("#0000FF")),
        remainPercent = null,
        lowStockThresholdPct = null,
        isLowStock = false,
        displayName = "PETG Blue",
    )

    private fun sampleArchive(
        id: Int,
        spoolId: Int?,
        filamentType: String,
        filamentColor: String? = null,
    ) = PrintArchive(
        id = id,
        displayName = "Print $id",
        printerId = 1,
        printerName = "Printer",
        printerModel = null,
        statusRaw = "completed",
        resultKind = ArchiveResultKind.Success,
        startedAtIso = null,
        completedAtIso = "2024-01-01T00:00:00Z",
        statsCompletedAtMillis = 1L,
        durationSeconds = 100,
        filamentUsage = null,
        filamentType = filamentType,
        filamentColor = filamentColor,
        spoolId = spoolId,
        failureReason = null,
        totalLayers = null,
        quantity = null,
        projectName = null,
        slicedForModel = null,
        notes = null,
    )
}
