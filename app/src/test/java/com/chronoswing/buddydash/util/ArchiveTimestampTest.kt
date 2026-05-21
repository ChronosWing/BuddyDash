package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ArchiveTimestampTest {

    @Test
    fun parseArchiveTimestamp_isoWithZ() {
        val instant = parseArchiveTimestamp("2024-01-15T12:00:00Z")
        assertEquals(Instant.parse("2024-01-15T12:00:00Z"), instant)
    }

    @Test
    fun parseArchiveTimestamp_localNaiveUsesSystemZone() {
        val instant = parseArchiveTimestamp("2024-01-15T14:30:00")
        assertNotNull(instant)
    }

    @Test
    fun parseArchiveTimestamp_epochSeconds() {
        val instant = parseArchiveTimestamp("1700000000")
        assertEquals(Instant.ofEpochSecond(1700000000), instant)
    }

    @Test
    fun parseArchiveTimestamp_epochMillis() {
        val instant = parseArchiveTimestamp("1700000000000")
        assertEquals(Instant.ofEpochMilli(1700000000000), instant)
    }

    @Test
    fun resolveArchiveStatsInstant_prefersStatsMillisOverRawStrings() {
        val archive = PrintArchive(
            id = 1,
            displayName = "Test",
            printerId = null,
            printerName = null,
            printerModel = null,
            statusRaw = "completed",
            resultKind = ArchiveResultKind.Success,
            startedAtIso = "2020-01-01T00:00:00Z",
            completedAtIso = "2020-01-01T00:00:00Z",
            statsCompletedAtMillis = Instant.parse("2024-01-10T10:00:00Z").toEpochMilli(),
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
        assertEquals(Instant.parse("2024-01-10T10:00:00Z"), resolveArchiveStatsInstant(archive))
    }

    @Test
    fun resolveArchiveStatsInstant_parsesNaiveCompletedAtWhenMillisAbsent() {
        val archive = PrintArchive(
            id = 1,
            displayName = "Test",
            printerId = null,
            printerName = null,
            printerModel = null,
            statusRaw = "completed",
            resultKind = ArchiveResultKind.Success,
            startedAtIso = null,
            completedAtIso = "2024-01-10T10:00:00Z",
            statsCompletedAtMillis = null,
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
        assertEquals(Instant.parse("2024-01-10T10:00:00Z"), resolveArchiveStatsInstant(archive))
    }

    @Test
    fun archivePassesStatsRange_includesTodayWithLocalCutoff() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val todayLocal = now.toLocalDate().atTime(10, 0).atZone(zone).toInstant()
        val archive = PrintArchive(
            id = 1,
            displayName = "Today",
            printerId = null,
            printerName = null,
            printerModel = null,
            statusRaw = "completed",
            resultKind = ArchiveResultKind.Success,
            startedAtIso = null,
            completedAtIso = null,
            statsCompletedAtMillis = todayLocal.toEpochMilli(),
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
        assertTrue(archivePassesStatsRange(archive, ArchiveStatsTimeRange.Last7Days, zone, now))
        assertTrue(archivePassesStatsRange(archive, ArchiveStatsTimeRange.Last30Days, zone, now))
    }

    @Test
    fun archivePassesStatsRange_excludesUndatedFromRangedFilters() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val archive = PrintArchive(
            id = 1,
            displayName = "No date",
            printerId = null,
            printerName = null,
            printerModel = null,
            statusRaw = "completed",
            resultKind = ArchiveResultKind.Success,
            startedAtIso = null,
            completedAtIso = null,
            statsCompletedAtMillis = null,
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
        assertFalse(archivePassesStatsRange(archive, ArchiveStatsTimeRange.Last7Days, zone, now))
        assertTrue(archivePassesStatsRange(archive, ArchiveStatsTimeRange.AllTime, zone, now))
    }
}
