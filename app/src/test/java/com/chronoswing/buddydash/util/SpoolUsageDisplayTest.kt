package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolUsageDirectIds
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoolUsageDisplayTest {

    @Test
    fun parseSpoolUsageHistoryList_parsesOpenApiShape() {
        val body =
            """[{"id":10,"spool_id":5,"printer_id":2,"print_name":"Benchy.3mf","weight_used":12.5,"percent_used":3,"status":"completed","created_at":"2024-06-01T12:00:00Z"}]"""
        val entries = parseSpoolUsageHistoryList(body)
        assertEquals(1, entries.size)
        assertEquals(10, entries.first().id)
        assertEquals(5, entries.first().spoolId)
        assertEquals(2, entries.first().printerId)
        assertEquals("Benchy.3mf", entries.first().printName)
        assertEquals(12.5, entries.first().weightUsedGrams, 0.001)
    }

    @Test
    fun parseSpoolUsageHistoryList_emptyArray() {
        assertTrue(parseSpoolUsageHistoryList("[]").isEmpty())
    }

    @Test
    fun formatSpoolUsageWeight_formatsGrams() {
        val entry = parseSpoolUsageHistoryList(
            """[{"id":1,"spool_id":1,"weight_used":5,"status":"ok","created_at":"2024-01-01T00:00:00Z"}]""",
        ).first()
        assertEquals("5g", formatSpoolUsageWeight(entry))
    }

    @Test
    fun formatSpoolUsageWeightUsedLine_appendsUsed() {
        val entry = parseSpoolUsageHistoryList(
            """[{"id":1,"spool_id":1,"weight_used":46,"status":"ok","created_at":"2024-01-01T00:00:00Z"}]""",
        ).first()
        assertEquals("46g used", formatSpoolUsageWeightUsedLine(entry))
    }

    @Test
    fun parseSpoolUsageHistoryList_readsOptionalArchiveId() {
        val body =
            """[{"id":1,"spool_id":1,"weight_used":1,"status":"ok","created_at":"2024-01-01T00:00:00Z","archive_id":99}]"""
        assertEquals(99, parseSpoolUsageHistoryList(body).first().directIds.archiveId)
    }

    @Test
    fun resolveSpoolUsageDirectIds_readsJobAndHistoryIds() {
        val json = JSONObject(
            """{"archive_id":1,"history_id":2,"job_id":3,"task_id":4,"file_id":5,"print_id":6}""",
        )
        val ids = resolveSpoolUsageDirectIds(json)
        assertEquals(1, ids.archiveId)
        assertEquals(2, ids.historyId)
        assertEquals(3, ids.jobId)
        assertEquals(4, ids.taskId)
        assertEquals(5, ids.fileId)
        assertEquals(6, ids.printId)
    }

    @Test
    fun resolveDirectArchiveId_triesIdsInPriorityOrder() {
        val archives = setOf(42)
        assertEquals(
            42,
            resolveDirectArchiveId(SpoolUsageDirectIds(jobId = 42, archiveId = 99), archives),
        )
        assertEquals(
            42,
            resolveDirectArchiveId(SpoolUsageDirectIds(archiveId = 42), archives),
        )
    }

    @Test
    fun normalizePrintNameForArchiveMatch_underscoresExtensionAndPlate() {
        assertEquals("mecha robot", normalizePrintNameForArchiveMatch("MECHA_ROBOT.3mf"))
        assertEquals(
            "mecha robot",
            normalizePrintNameForArchiveMatch("Mecha Robot - Plate 6"),
        )
    }

    @Test
    fun matchArchiveForSpoolUsage_singleHighConfidenceMatchWithoutDate() {
        val entry = SpoolUsageEntry(
            id = 1,
            spoolId = 5,
            printerId = 2,
            printName = "MECHA_ROBOT.3mf",
            weightUsedGrams = 46.0,
            percentUsed = null,
            status = "completed",
            createdAtIso = "2024-06-01T14:00:00Z",
        )
        val archives = listOf(
            printArchive(id = 100, name = "Mecha Robot.3mf", printerId = 2, completedAt = null),
            printArchive(id = 101, name = "Other Part", printerId = 2, completedAt = null),
        )
        assertEquals(100, matchArchiveForSpoolUsage(entry, archives))
    }

    @Test
    fun matchArchiveForSpoolUsage_disambiguatesByPrinter() {
        val entry = SpoolUsageEntry(
            id = 1,
            spoolId = 5,
            printerId = 2,
            printName = "Duplicate.3mf",
            weightUsedGrams = 10.0,
            percentUsed = null,
            status = "completed",
            createdAtIso = "2024-06-01T14:00:00Z",
        )
        val archives = listOf(
            printArchive(id = 100, name = "Duplicate.3mf", printerId = 2, completedAt = "2024-06-01T13:00:00Z"),
            printArchive(id = 101, name = "Duplicate.3mf", printerId = 3, completedAt = "2024-06-01T15:00:00Z"),
        )
        assertEquals(
            100,
            matchArchiveForSpoolUsage(entry, archives, mapOf(2 to "MECHABROBOT", 3 to "OTHER")),
        )
    }

    @Test
    fun matchArchiveForSpoolUsage_ambiguousSamePrinterReturnsNull() {
        val entry = SpoolUsageEntry(
            id = 1,
            spoolId = 5,
            printerId = 2,
            printName = "Duplicate.3mf",
            weightUsedGrams = 10.0,
            percentUsed = null,
            status = "completed",
            createdAtIso = "2024-06-01T14:00:00Z",
        )
        val archives = listOf(
            printArchive(id = 100, name = "Duplicate.3mf", printerId = 2, completedAt = "2024-06-01T13:00:00Z"),
            printArchive(id = 101, name = "Duplicate.3mf", printerId = 2, completedAt = "2024-06-01T15:00:00Z"),
        )
        assertNull(matchArchiveForSpoolUsage(entry, archives))
    }

    @Test
    fun buildSpoolUsageDisplayItems_marksTappableWhenArchiveResolved() {
        val entry = parseSpoolUsageHistoryList(
            """[{"id":1,"spool_id":1,"printer_id":2,"print_name":"Benchy.3mf","weight_used":12,"status":"ok","created_at":"2024-06-01T12:00:00Z","archive_id":7}]""",
        ).first()
        val archives = listOf(printArchive(id = 7, name = "Benchy.3mf", printerId = 2, completedAt = "2024-06-01T12:00:00Z"))
        val items = buildSpoolUsageDisplayItems(
            entries = listOf(entry),
            archives = archives,
            printerNamesById = mapOf(2 to "MECHABROBOT"),
        )
        assertTrue(items.first().isTappable)
        assertEquals(7, items.first().archiveId)
        assertEquals(SpoolUsageArchiveMatchKind.DirectId, items.first().archiveMatchKind)
        assertEquals("MECHABROBOT", items.first().printerLine)
    }

    private fun printArchive(
        id: Int,
        name: String,
        printerId: Int,
        completedAt: String?,
    ): PrintArchive = PrintArchive(
        id = id,
        displayName = name,
        printerId = printerId,
        printerName = null,
        printerModel = null,
        statusRaw = "completed",
        resultKind = ArchiveResultKind.Success,
        startedAtIso = null,
        completedAtIso = completedAt,
        createdAtIso = completedAt,
        statsCompletedAtMillis = completedAt?.let { parseArchiveTimestamp(it)?.toEpochMilli() },
        durationSeconds = null,
        filamentUsage = null,
        filamentType = null,
        filamentColor = null,
        spoolId = null,
        failureReason = null,
        totalLayers = null,
        quantity = null,
        projectName = null,
        slicedForModel = null,
        notes = null,
    )
}
