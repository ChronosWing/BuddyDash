package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

enum class ArchivesSection {
    History,
    Stats,
}

enum class ArchiveStatsTimeRange {
    Last7Days,
    Last30Days,
    AllTime,
}

data class ArchiveRecentFailure(
    val archiveId: Int,
    val displayName: String,
    val subtitle: String?,
)

/** Aggregated archive stats for a time range; null fields are hidden in UI. */
data class ArchiveStatsSnapshot(
    val totalPrints: Int? = null,
    val successfulPrints: Int? = null,
    val failedPrints: Int? = null,
    val successRatePercent: Int? = null,
    val totalPrintTimeFormatted: String? = null,
    val totalFilamentFormatted: String? = null,
    val mostUsedPrinter: String? = null,
    val mostUsedMaterial: String? = null,
    val longestPrintFormatted: String? = null,
    val recentFailures: List<ArchiveRecentFailure> = emptyList(),
)

fun filterArchivesForStatsRange(
    archives: List<PrintArchive>,
    range: ArchiveStatsTimeRange,
    zone: ZoneId = ZoneId.systemDefault(),
    now: java.time.ZonedDateTime = java.time.ZonedDateTime.now(zone),
): List<PrintArchive> {
    if (range == ArchiveStatsTimeRange.AllTime) return archives
    return archives.filter { archivePassesStatsRange(it, range, zone, now) }
}

fun computeArchiveStats(
    archives: List<PrintArchive>,
    nowMillis: Long = System.currentTimeMillis(),
): ArchiveStatsSnapshot {
    if (archives.isEmpty()) return ArchiveStatsSnapshot(totalPrints = 0)

    val total = archives.size
    val successful = archives.count { it.resultKind == ArchiveResultKind.Success }
    val failed = archives.count { it.resultKind == ArchiveResultKind.Failed }

    val successRate = (successful + failed).takeIf { it > 0 }?.let { denom ->
        ((successful * 100.0) / denom).roundToInt().coerceIn(0, 100)
    }

    val durationSumSeconds = archives.mapNotNull { it.durationSeconds?.takeIf { s -> s > 0 } }.sum()
    val totalPrintTimeFormatted = durationSumSeconds.takeIf { it > 0 }?.let { seconds ->
        formatTotalPrintTimeCompact(seconds / 3600.0)
    }

    val filamentSumGrams = archives.mapNotNull { it.filamentUsage?.weightGrams }.sum()
    val totalFilamentFormatted = filamentSumGrams.takeIf { it > 0.0 }?.let {
        formatArchiveStatsFilamentTotal(it)
    }

    val mostUsedPrinter = archives
        .mapNotNull { archivePrinterStatsKey(it) }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    val mostUsedMaterial = archives
        .mapNotNull { archiveMaterialStatsKey(it) }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    val longestSeconds = archives.mapNotNull { it.durationSeconds?.takeIf { s -> s > 0 } }.maxOrNull()
    val longestPrintFormatted = longestSeconds?.let { formatQueueDuration(it) }

    val recentFailures = archives
        .filter { it.resultKind == ArchiveResultKind.Failed }
        .sortedByDescending { resolveArchiveStatsInstant(it)?.toEpochMilli() ?: Long.MIN_VALUE }
        .take(5)
        .map { archive ->
            ArchiveRecentFailure(
                archiveId = archive.id,
                displayName = archive.displayName,
                subtitle = buildRecentFailureSubtitle(archive, nowMillis),
            )
        }

    return ArchiveStatsSnapshot(
        totalPrints = total,
        successfulPrints = successful.takeIf { successful > 0 },
        failedPrints = failed.takeIf { failed > 0 },
        successRatePercent = successRate,
        totalPrintTimeFormatted = totalPrintTimeFormatted,
        totalFilamentFormatted = totalFilamentFormatted,
        mostUsedPrinter = mostUsedPrinter,
        mostUsedMaterial = mostUsedMaterial,
        longestPrintFormatted = longestPrintFormatted,
        recentFailures = recentFailures,
    )
}

/** Total filament across archives, e.g. 2.4kg or 184g. */
fun formatArchiveStatsFilamentTotal(grams: Double): String {
    if (grams >= 1000.0) {
        val kg = (grams / 1000.0 * 10).roundToInt() / 10.0
        val text = if (kg == kg.roundToInt().toDouble()) {
            kg.roundToInt().toString()
        } else {
            kg.toString()
        }
        return "${text}kg"
    }
    return formatFilamentWeightGrams(grams)
}

private fun archivePrinterStatsKey(archive: PrintArchive): String? {
    val name = archive.printerName?.trim()?.takeIf { it.isNotBlank() }
    if (name != null) return name
    val model = archive.printerModel?.trim()?.takeIf { it.isNotBlank() }
    if (model != null) return model
    return archive.printerId?.let { "Printer $it" }
}

private fun archiveMaterialStatsKey(archive: PrintArchive): String? {
    val type = archive.filamentType?.trim()?.takeIf { it.isNotBlank() }
    if (type != null) return type
    return archive.filamentColor?.trim()?.takeIf { it.isNotBlank() }
}

private fun buildRecentFailureSubtitle(archive: PrintArchive, nowMillis: Long): String? {
    val parts = buildList {
        formatArchivePrinterLine(archive)?.let { add(it) }
        archive.failureReason?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
        formatArchiveCompletedAgo(
            archive.completedAtIso ?: archive.createdAtIso ?: archive.startedAtIso,
            nowMillis,
        )?.let { add(it) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}
