package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.PrintArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ArchiveStatsTest {

    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.ofInstant(Instant.parse("2024-02-01T12:00:00Z"), zone)
    private val nowMillis = now.toInstant().toEpochMilli()

    @Test
    fun computeArchiveStats_aggregatesFromRealFields() {
        val archives = listOf(
            sampleArchive(
                id = 1,
                kind = ArchiveResultKind.Success,
                completedAt = "2024-01-20T10:00:00Z",
                durationSeconds = 3600,
                grams = 100.0,
                printerName = "X1C",
                filamentType = "PLA",
            ),
            sampleArchive(
                id = 2,
                kind = ArchiveResultKind.Success,
                completedAt = "2024-01-25T10:00:00Z",
                durationSeconds = 7200,
                grams = 200.0,
                printerName = "X1C",
                filamentType = "PETG",
            ),
            sampleArchive(
                id = 3,
                kind = ArchiveResultKind.Failed,
                completedAt = "2024-01-28T10:00:00Z",
                durationSeconds = 1800,
                grams = null,
                printerName = "A1",
                filamentType = "PLA",
                failureReason = "Bed adhesion",
            ),
        )
        val stats = computeArchiveStats(archives, nowMillis)
        assertEquals(3, stats.totalPrints)
        assertEquals(2, stats.successfulPrints)
        assertEquals(1, stats.failedPrints)
        assertEquals(67, stats.successRatePercent)
        assertEquals("3h 30m", stats.totalPrintTimeFormatted)
        assertEquals("300g", stats.totalFilamentFormatted)
        assertEquals("X1C", stats.mostUsedPrinter)
        assertEquals("PLA", stats.mostUsedMaterial)
        assertEquals("2h 0m", stats.longestPrintFormatted)
        assertEquals(1, stats.recentFailures.size)
        assertTrue(stats.recentFailures.first().subtitle!!.contains("Bed adhesion"))
    }

    @Test
    fun computeArchiveStats_hidesZeroOptionalCounts() {
        val stats = computeArchiveStats(
            listOf(
                sampleArchive(
                    id = 1,
                    kind = ArchiveResultKind.Cancelled,
                    completedAt = "2024-01-20T10:00:00Z",
                ),
            ),
            nowMillis,
        )
        assertEquals(1, stats.totalPrints)
        assertNull(stats.successfulPrints)
        assertNull(stats.failedPrints)
        assertNull(stats.successRatePercent)
    }

    @Test
    fun computeArchiveStats_emptyRangeReportsZeroTotal() {
        val stats = computeArchiveStats(emptyList(), nowMillis)
        assertEquals(0, stats.totalPrints)
        assertNull(stats.successRatePercent)
    }

    @Test
    fun formatArchiveStatsFilamentTotal_usesKgAboveThreshold() {
        assertEquals("2.4kg", formatArchiveStatsFilamentTotal(2400.0))
        assertEquals("184g", formatArchiveStatsFilamentTotal(184.0))
    }

    @Test
    fun filterArchivesForStatsRange_last30Days() {
        val archives = listOf(
            sampleArchive(id = 1, kind = ArchiveResultKind.Success, completedAt = "2023-12-01T10:00:00Z"),
            sampleArchive(id = 2, kind = ArchiveResultKind.Success, completedAt = "2024-01-20T10:00:00Z"),
        )
        val filtered = filterArchivesForStatsRange(
            archives,
            ArchiveStatsTimeRange.Last30Days,
            zone,
            now,
        )
        assertEquals(1, filtered.size)
        assertEquals(2, filtered.first().id)
    }

    private fun sampleArchive(
        id: Int,
        kind: ArchiveResultKind,
        completedAt: String? = null,
        durationSeconds: Int? = null,
        grams: Double? = null,
        printerName: String? = null,
        filamentType: String? = null,
        failureReason: String? = null,
    ) = PrintArchive(
        id = id,
        displayName = "Print $id",
        printerId = 1,
        printerName = printerName,
        printerModel = null,
        statusRaw = kind.name.lowercase(),
        resultKind = kind,
        startedAtIso = null,
        completedAtIso = completedAt,
        statsCompletedAtMillis = completedAt?.let { parseArchiveTimestamp(it)?.toEpochMilli() },
        durationSeconds = durationSeconds,
        filamentUsage = grams?.let { FilamentUsage(weightGrams = it) },
        filamentType = filamentType,
        filamentColor = null,
        failureReason = failureReason,
        totalLayers = null,
        quantity = null,
        projectName = null,
        slicedForModel = null,
        notes = null,
    )
}
