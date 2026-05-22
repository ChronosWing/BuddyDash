package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.PrintArchive
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
        assertTrue(entries.first().rawJson.contains("Benchy"))
    }

    @Test
    fun parseSpoolUsageHistoryList_readsArchiveIdWhenPresentInRawJson() {
        val body =
            """[{"id":1,"spool_id":1,"weight_used":1,"status":"ok","created_at":"2024-01-01T00:00:00Z","archive_id":99}]"""
        assertEquals(99, parseSpoolUsageHistoryList(body).first().directIds.archiveId)
    }

    @Test
    fun resolveSpoolUsageArchiveLink_archiveIdDirectMatch() {
        val entry = parseSpoolUsageHistoryList(
            """[{"id":5,"spool_id":1,"printer_id":2,"print_name":"Part.3mf","weight_used":12,"status":"ok","created_at":"2024-06-01T12:00:00Z","archive_id":42}]""",
        ).first()
        val archives = listOf(printArchive(id = 42, name = "Part.3mf", printerId = 2, grams = 12.0))
        val link = resolveSpoolUsageArchiveLink(entry, archives)
        assertEquals(42, link.archiveId)
        assertEquals(SpoolUsageArchiveLinkKind.ArchiveId, link.kind)
    }

    @Test
    fun resolveSpoolUsageArchiveLink_compositeMatchByTitlePrinterTime() {
        val entry = SpoolUsageEntry(
            id = 7,
            spoolId = 1,
            printerId = 2,
            printName = "MECHA_ROBOT.3mf",
            weightUsedGrams = 46.0,
            percentUsed = null,
            status = "completed",
            createdAtIso = "2024-06-01T14:00:00Z",
            rawJson = "{}",
        )
        val archives = listOf(
            printArchive(
                id = 100,
                name = "Mecha Robot.3mf",
                filename = "Mecha Robot.3mf",
                printerId = 2,
                grams = 46.0,
                completedAt = "2024-06-01T14:00:05Z",
            ),
            printArchive(id = 101, name = "Other", printerId = 2, grams = 10.0, completedAt = "2024-01-01T00:00:00Z"),
        )
        val link = resolveSpoolUsageArchiveLink(entry, archives)
        assertEquals(100, link.archiveId)
        assertEquals(SpoolUsageArchiveLinkKind.CompositeKey, link.kind)
    }

    @Test
    fun resolveSpoolUsageArchiveLink_compositeMatchChineseTitlePipeVsSpaces() {
        val title =
            "万能表探针固定夹 | 单手操作表笔辅助工具 | 电子维修防滑神器 | 提升测量效率"
        val usageTitle =
            "万能表探针固定夹 单手操作表笔辅助工具 电子维修防滑神器 提升测量效率"
        val entry = SpoolUsageEntry(
            id = 8,
            spoolId = 1,
            printerId = 3,
            printName = usageTitle,
            weightUsedGrams = 22.0,
            percentUsed = null,
            status = "completed",
            createdAtIso = "2024-07-01T10:00:00Z",
            rawJson = "{}",
        )
        val archives = listOf(
            printArchive(
                id = 200,
                name = title,
                printerId = 3,
                grams = 22.0,
                completedAt = "2024-07-01T10:00:02Z",
            ),
        )
        val link = resolveSpoolUsageArchiveLink(entry, archives)
        assertEquals(200, link.archiveId)
        assertEquals(SpoolUsageArchiveLinkKind.CompositeKey, link.kind)
    }

    @Test
    fun resolveSpoolUsageArchiveLink_ambiguousCompositeReturnsNull() {
        val entry = SpoolUsageEntry(
            id = 1,
            spoolId = 5,
            printerId = 2,
            printName = "Duplicate.3mf",
            weightUsedGrams = 10.0,
            percentUsed = null,
            status = "completed",
            createdAtIso = "2024-06-01T14:00:00Z",
            rawJson = "{}",
        )
        val sameCompletedAt = "2024-06-01T14:00:00Z"
        val archives = listOf(
            printArchive(id = 100, name = "Duplicate.3mf", printerId = 2, grams = 10.0, completedAt = sameCompletedAt),
            printArchive(id = 101, name = "Duplicate.3mf", printerId = 2, grams = 10.0, completedAt = sameCompletedAt),
        )
        assertNull(resolveSpoolUsageArchiveLink(entry, archives).archiveId)
    }

    @Test
    fun buildSpoolUsageDisplayItems_marksTappableWhenLinked() {
        val entry = parseSpoolUsageHistoryList(
            """[{"id":1,"spool_id":1,"printer_id":2,"print_name":"Benchy.3mf","weight_used":12,"status":"ok","created_at":"2024-06-01T12:00:00Z","archive_id":7}]""",
        ).first()
        val archives = listOf(printArchive(id = 7, name = "Benchy.3mf", printerId = 2, grams = 12.0))
        val items = buildSpoolUsageDisplayItems(listOf(entry), archives, mapOf(2 to "P1"))
        assertTrue(items.first().isTappable)
        assertEquals(7, items.first().archiveId)
        assertEquals(SpoolUsageThumbnailSource.Archive, items.first().thumbnailSource)
    }

    @Test
    fun resolveSpoolUsageArchiveLink_titleAndPrinterIgnoresArchiveFilamentColor() {
        val entry = SpoolUsageEntry(
            id = 9,
            spoolId = 1,
            printerId = 2,
            printName = "Widget.3mf",
            weightUsedGrams = 30.0,
            percentUsed = null,
            status = "completed",
            createdAtIso = "2024-09-15T10:00:00Z",
            rawJson = "{}",
        )
        val archives = listOf(
            printArchive(
                id = 300,
                name = "Widget.3mf",
                printerId = 2,
                grams = 50.0,
                completedAt = "2024-06-01T12:00:00Z",
                filamentType = "PETG",
                filamentColor = "red",
            ),
        )
        val link = resolveSpoolUsageArchiveLink(
            entry = entry,
            archives = archives,
            spoolMaterial = "PLA",
            spoolColorName = "blue",
        )
        assertEquals(300, link.archiveId)
        assertEquals(SpoolUsageArchiveLinkKind.CompositeKey, link.kind)
    }

    @Test
    fun resolveSpoolUsageArchiveLink_picksClosestTimeAmongDuplicateTitles() {
        val entry = SpoolUsageEntry(
            id = 10,
            spoolId = 1,
            printerId = 2,
            printName = "Dup.3mf",
            weightUsedGrams = 10.0,
            percentUsed = null,
            status = "completed",
            createdAtIso = "2024-06-01T14:00:10Z",
            rawJson = "{}",
        )
        val archives = listOf(
            printArchive(
                id = 401,
                name = "Dup.3mf",
                printerId = 2,
                grams = 10.0,
                completedAt = "2024-06-01T14:00:00Z",
            ),
            printArchive(
                id = 402,
                name = "Dup.3mf",
                printerId = 2,
                grams = 10.0,
                completedAt = "2024-01-01T00:00:00Z",
            ),
        )
        val link = resolveSpoolUsageArchiveLink(entry, archives)
        assertEquals(401, link.archiveId)
    }

    private fun printArchive(
        id: Int,
        name: String,
        printerId: Int,
        grams: Double,
        filename: String? = name,
        completedAt: String? = "2024-06-01T12:00:00Z",
        filamentType: String? = null,
        filamentColor: String? = null,
    ): PrintArchive = PrintArchive(
        id = id,
        displayName = name,
        filename = filename,
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
        filamentUsage = FilamentUsage(weightGrams = grams),
        filamentType = filamentType,
        filamentColor = filamentColor,
        spoolId = null,
        plateNumber = null,
        contentHash = null,
        failureReason = null,
        totalLayers = null,
        quantity = null,
        projectName = null,
        slicedForModel = null,
        notes = null,
    )
}
