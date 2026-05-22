package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoolArchiveLinkTest {

    @Test
    fun normalizeArchiveMaterialKey_stripsCosmeticSuffix() {
        assertEquals("petg", normalizeArchiveMaterialKey("PETG Basic"))
        assertEquals("pla", normalizeArchiveMaterialKey("  PLA Matte  "))
    }

    @Test
    fun normalizeMatchColorHex_handlesHashAndCase() {
        assertEquals("#000000", normalizeMatchColorHex("000000"))
        assertEquals("#ffffff", normalizeMatchColorHex("#FFFFFF"))
    }

    @Test
    fun parseArchiveLookupColorHexesArg_dedupes() {
        assertEquals(
            listOf("#ffffff"),
            parseArchiveLookupColorHexesArg("ffffff,FFFFFF"),
        )
    }

    @Test
    fun archiveLookupFilterSummary_formatsMaterialAndColor() {
        val filter = ArchiveSpoolLookupFilter(
            materialLabel = "PETG",
            materialKey = "petg",
            colorHexes = listOf("#ffffff"),
            colorDisplayLabel = "white",
        )
        assertEquals("PETG • white", archiveLookupFilterSummary(filter))
    }

    @Test
    fun archiveMatchesSpoolMaterial_petgBasicMatchesPetgSpool() {
        val archive = sampleArchive(id = 1, spoolId = null, filamentType = "PETG Basic", filamentColor = "#0000FF")
        val spool = sampleSpool(id = 2, material = "PETG")
        assertTrue(archiveMatchesSpoolMaterial(spool, archive))
    }

    @Test
    fun spoolMatchesArchiveLookupFilter_requiresMaterialAndColor() {
        val filter = ArchiveSpoolLookupFilter(
            materialLabel = "PLA",
            materialKey = "pla",
            colorHexes = listOf("#ffffff"),
            colorDisplayLabel = "white",
        )
        val whitePla = sampleSpool(id = 1, material = "PLA", colorName = "White", colorHexes = listOf("#FFFFFF"))
        val bluePla = sampleSpool(id = 2, material = "PLA", colorName = "Blue", colorHexes = listOf("#0000FF"))
        assertTrue(spoolMatchesArchiveLookupFilter(whitePla, filter))
        assertFalse(spoolMatchesArchiveLookupFilter(bluePla, filter))
    }

    @Test
    fun spoolMatchesArchiveLookupFilter_materialOnlyWhenArchiveHasNoColor() {
        val filter = ArchiveSpoolLookupFilter(
            materialLabel = "PLA",
            materialKey = "pla",
            colorHexes = emptyList(),
            colorDisplayLabel = null,
        )
        val bluePla = sampleSpool(id = 2, material = "PLA", colorHexes = listOf("#0000FF"))
        assertTrue(spoolMatchesArchiveLookupFilter(bluePla, filter))
    }

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
    fun matchingSpoolsForArchive_returnsAllMaterialMatchesSortedByPrinter() {
        val archive = sampleArchive(
            id = 1,
            spoolId = 99,
            printerId = 10,
            filamentType = "PETG",
            filamentColor = "#0000FF",
        )
        val onPrinter = sampleSpool(
            id = 1,
            assignment = SpoolSlotAssignment(printerId = 10, printerName = "X1", slotLabel = "A1"),
        )
        val otherMatch = sampleSpool(id = 2)
        val matches = matchingSpoolsForArchive(archive, listOf(otherMatch, onPrinter))
        assertEquals(2, matches.size)
        assertEquals(1, matches.first().id)
    }

    @Test
    fun resolveArchiveMaterialNavigation_singleMatchOpensDetail() {
        val archive = sampleArchive(id = 1, spoolId = 5, filamentType = "PETG", filamentColor = "#0000FF")
        val spools = listOf(
            sampleSpool(id = 5),
            sampleSpool(id = 6, material = "PLA"),
        )
        val nav = resolveArchiveMaterialNavigation(archive, spools)
        assertTrue(nav is ArchiveMaterialNavigation.SpoolDetail)
        assertEquals(5, (nav as ArchiveMaterialNavigation.SpoolDetail).spoolId)
    }

    @Test
    fun resolveArchiveMaterialNavigation_multipleMatchesOpensFilteredLookup() {
        val archive = sampleArchive(id = 1, spoolId = 5, filamentType = "PETG", filamentColor = "#0000FF")
        val spools = listOf(
            sampleSpool(id = 5),
            sampleSpool(id = 6),
        )
        val nav = resolveArchiveMaterialNavigation(archive, spools)
        assertTrue(nav is ArchiveMaterialNavigation.SpoolsFiltered)
        val filtered = nav as ArchiveMaterialNavigation.SpoolsFiltered
        assertEquals("PETG", filtered.lookupFilter.materialLabel)
        assertEquals("petg", filtered.lookupFilter.materialKey)
        assertEquals(listOf("#0000ff"), filtered.lookupFilter.colorHexes)
    }

    @Test
    fun deriveColorFamilyName_mapsRedHex() {
        assertEquals("red", deriveColorFamilyName("#FF0000"))
        assertEquals("red", deriveColorFamilyName("#FF3300"))
        assertEquals("red", deriveColorFamilyName("#E53935"))
    }

    @Test
    fun spoolMatchesArchiveLookupFilter_petgRedArchiveMatchesKingroonPetgRedSpool() {
        val filter = ArchiveSpoolLookupFilter(
            materialLabel = "PETG",
            materialKey = "petg",
            colorHexes = listOf("#ff0000"),
            colorDisplayLabel = "red",
        )
        val spool = sampleSpool(
            id = 3,
            material = "PETG",
            colorName = "Red",
            colorHexes = emptyList(),
            displayNameOverride = "KINGROON PETG Red",
        )
        assertTrue(spoolMatchesArchiveLookupFilter(spool, filter))
    }

    @Test
    fun spoolMatchesArchiveLookupFilter_nearRedHexesMatch() {
        val filter = ArchiveSpoolLookupFilter(
            materialLabel = "PETG",
            materialKey = "petg",
            colorHexes = listOf("#ff0000"),
            colorDisplayLabel = "red",
        )
        val spool = sampleSpool(
            id = 4,
            material = "PETG",
            colorName = "Red",
            colorHexes = listOf("#E53935"),
        )
        assertTrue(spoolMatchesArchiveLookupFilter(spool, filter))
    }

    @Test
    fun buildArchiveSpoolLookupFilter_derivesRedLabelFromHex() {
        val archive = sampleArchive(id = 1, spoolId = null, filamentType = "PETG", filamentColor = "#FF0000")
        val filter = buildArchiveSpoolLookupFilter(archive)
        assertEquals("red", filter.colorDisplayLabel)
        assertEquals(listOf("#ff0000"), filter.colorHexes)
    }

    @Test
    fun resolveArchiveMaterialNavigation_noMatchStillOpensFilteredLookup() {
        val archive = sampleArchive(id = 1, spoolId = 99, filamentType = "PETG", filamentColor = "#0000FF")
        val spools = listOf(sampleSpool(id = 1, material = "PLA"))
        val nav = resolveArchiveMaterialNavigation(archive, spools)
        assertTrue(nav is ArchiveMaterialNavigation.SpoolsFiltered)
        val filtered = nav as ArchiveMaterialNavigation.SpoolsFiltered
        assertEquals("petg", filtered.lookupFilter.materialKey)
        assertTrue(filtered.lookupFilter.colorHexes.isNotEmpty())
    }

    private fun sampleSpool(
        id: Int,
        material: String = "PETG",
        colorName: String = "Blue",
        colorHexes: List<String> = listOf("#0000FF"),
        assignment: SpoolSlotAssignment? = null,
        displayNameOverride: String? = null,
    ) = SpoolInventoryItem(
        id = id,
        material = material,
        colorName = colorName,
        swatch = FilamentSwatchColors(colorHexes = colorHexes),
        remainPercent = null,
        lowStockThresholdPct = null,
        isLowStock = false,
        displayName = displayNameOverride ?: "$material $colorName",
        assignment = assignment,
    )

    private fun sampleArchive(
        id: Int,
        spoolId: Int?,
        filamentType: String,
        filamentColor: String? = null,
        printerId: Int? = 1,
    ) = PrintArchive(
        id = id,
        displayName = "Print $id",
        printerId = printerId,
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
