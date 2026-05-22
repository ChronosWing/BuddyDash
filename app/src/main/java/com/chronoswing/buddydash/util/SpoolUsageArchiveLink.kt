package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolUsageDirectIds
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Archives loaded for spool detail usage linking (may exceed list default). */
const val SPOOL_DETAIL_ARCHIVES_LIMIT = 500

/** Max age gap between usage [SpoolUsageEntry.createdAtIso] and archive completion. */
private const val USAGE_ARCHIVE_TIME_TOLERANCE_SECONDS = 300L

const val TAG_SPOOL_USAGE_LINK = "BuddyDash/SpoolUsageLink"

enum class SpoolUsageArchiveLinkKind {
    /** [SpoolUsageHistory.archive_id] → [PrintArchive.id] (Bambuddy DB FK). */
    ArchiveId,
    /** Exact normalized title + printer + weight + time composite. */
    CompositeKey,
    None,
}

data class SpoolUsageArchiveLinkResult(
    val archiveId: Int?,
    val kind: SpoolUsageArchiveLinkKind,
    val reason: String,
    val usageHistoryId: Int,
    val candidateArchiveIds: List<Int> = emptyList(),
)

/**
 * Bambuddy stores [SpoolUsageHistory.archive_id] as FK to print_archives.id.
 * The published API schema may omit archive_id; parse it from raw JSON when present.
 */
fun resolveSpoolUsageArchiveLink(
    entry: SpoolUsageEntry,
    archives: List<PrintArchive>,
): SpoolUsageArchiveLinkResult {
    val archivesById = archives.associateBy { it.id }
    val archiveIdSet = archivesById.keys

    resolveSpoolUsageArchiveId(entry.directIds, archiveIdSet)?.let { archiveId ->
        return linked(
            entry = entry,
            archiveId = archiveId,
            kind = SpoolUsageArchiveLinkKind.ArchiveId,
            reason = "shared archive_id (Bambuddy spool_usage_history.archive_id → print_archives.id)",
            candidates = listOf(archiveId),
        )
    }

    val usageTitleKey = entry.printName?.let { normalizePrintTitleForMatch(it) }
    if (usageTitleKey == null) {
        return failed(entry, "no_print_name")
    }

    val compositeCandidates = archives.filter { archive ->
        archiveMatchesSpoolUsageComposite(entry, archive, usageTitleKey)
    }
    val candidateIds = compositeCandidates.map { it.id }

    when (compositeCandidates.size) {
        1 -> {
            val archive = compositeCandidates.first()
            return linked(
                entry = entry,
                archiveId = archive.id,
                kind = SpoolUsageArchiveLinkKind.CompositeKey,
                reason = "composite: normalized_title+printer+plate+time",
                candidates = candidateIds,
            )
        }
        0 -> {
            logSpoolUsageLinkFailure(entry, usageTitleKey, archives, "no_composite_match")
            return failed(entry, "no_deterministic_key_found", candidateIds)
        }
        else -> {
            logSpoolUsageLinkFailure(
                entry,
                usageTitleKey,
                compositeCandidates,
                "ambiguous_composite count=${compositeCandidates.size}",
            )
            return failed(entry, "ambiguous_composite_match count=${compositeCandidates.size}", candidateIds)
        }
    }
}

/** Only [archive_id] / [print_archive_id] are true archive FKs in Bambuddy. */
fun resolveSpoolUsageArchiveId(
    directIds: SpoolUsageDirectIds,
    archiveIds: Set<Int>,
): Int? {
    val candidates = listOfNotNull(
        directIds.archiveId?.let { "archive_id" to it },
    )
    for ((_, id) in candidates) {
        if (id in archiveIds) return id
    }
    return null
}

fun archiveTitleKeys(archive: PrintArchive): Set<String> =
    buildSet {
        normalizePrintTitleForMatch(archive.displayName)?.let { add(it) }
        archive.filename?.let { normalizePrintTitleForMatch(it) }?.let { add(it) }
    }

private fun archiveMatchesSpoolUsageComposite(
    entry: SpoolUsageEntry,
    archive: PrintArchive,
    usageTitleKey: String,
): Boolean {
    if (!archiveTitleKeys(archive).contains(usageTitleKey)) return false

    val usagePrinterId = entry.printerId
    val archivePrinterId = archive.printerId
    if (usagePrinterId != null && archivePrinterId != null && usagePrinterId != archivePrinterId) {
        return false
    }
    if (usagePrinterId != null && archivePrinterId == null) return false

    if (!spoolUsageTimestampsAlign(entry.createdAtIso, archive)) {
        return false
    }

    val usagePlate = entry.plateNumber
    val archivePlate = archive.plateNumber
    if (usagePlate != null && archivePlate != null && usagePlate != archivePlate) {
        return false
    }

    return true
}

private fun spoolUsageTimestampsAlign(usageCreatedAtIso: String, archive: PrintArchive): Boolean {
    val usageInstant = parseSpoolUsageInstantForLink(usageCreatedAtIso) ?: return true
    val archiveInstant = resolveArchiveStatsInstant(archive) ?: return true
    val diffSeconds = kotlin.math.abs(Duration.between(usageInstant, archiveInstant).seconds)
    return diffSeconds <= USAGE_ARCHIVE_TIME_TOLERANCE_SECONDS
}

private fun parseSpoolUsageInstantForLink(iso: String): Instant? =
    try {
        Instant.parse(iso)
    } catch (_: Exception) {
        parseArchiveTimestamp(iso)
    }

private fun linked(
    entry: SpoolUsageEntry,
    archiveId: Int,
    kind: SpoolUsageArchiveLinkKind,
    reason: String,
    candidates: List<Int>,
): SpoolUsageArchiveLinkResult {
    logSpoolUsageLinkMatched(entry, archiveId, reason)
    return SpoolUsageArchiveLinkResult(
        archiveId = archiveId,
        kind = kind,
        reason = reason,
        usageHistoryId = entry.id,
        candidateArchiveIds = candidates,
    )
}

private fun failed(
    entry: SpoolUsageEntry,
    reason: String,
    candidates: List<Int> = emptyList(),
): SpoolUsageArchiveLinkResult {
    logSpoolUsageLinkUnmatched(entry, reason)
    return SpoolUsageArchiveLinkResult(
        archiveId = null,
        kind = SpoolUsageArchiveLinkKind.None,
        reason = reason,
        usageHistoryId = entry.id,
        candidateArchiveIds = candidates,
    )
}

fun logSpoolUsageLinkMatched(entry: SpoolUsageEntry, archiveId: Int, reason: String) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    Log.d(
        TAG_SPOOL_USAGE_LINK,
        "MATCHED: usage history id=${entry.id} → archive id=$archiveId\nreason: $reason",
    )
}

fun logSpoolUsageLinkUnmatched(entry: SpoolUsageEntry, reason: String) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    Log.d(
        TAG_SPOOL_USAGE_LINK,
        "MATCH FAILED: usage history id=${entry.id}\nreason: $reason",
    )
}

fun logSpoolUsageLinkFailure(
    entry: SpoolUsageEntry,
    usageTitleKey: String,
    archives: List<PrintArchive>,
    reason: String,
) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    val usageRaw = entry.printName
    Log.d(
        TAG_SPOOL_USAGE_LINK,
        "titleCompareFailed usageHistoryId=${entry.id} reason=$reason " +
            "rawUsageTitle=$usageRaw normalizedUsage=$usageTitleKey " +
            "printerId=${entry.printerId} weight=${entry.weightUsedGrams}g " +
            "createdAt=${entry.createdAtIso} plate=${entry.plateNumber}",
    )
    archives.take(8).forEach { archive ->
        val archiveNorm = archiveTitleKeys(archive).joinToString("|")
        val printerMatch = entry.printerId != null && archive.printerId == entry.printerId
        val timeMatch = spoolUsageTimestampsAlign(entry.createdAtIso, archive)
        Log.d(
            TAG_SPOOL_USAGE_LINK,
            "titleCompare archiveId=${archive.id} rawArchiveTitle=${archive.displayName} " +
                "filename=${archive.filename} normalizedArchive=$archiveNorm " +
                "printerMatch=$printerMatch timeMatch=$timeMatch " +
                "usageGrams=${entry.weightUsedGrams} archiveGrams=${archive.filamentUsage?.weightGrams}",
        )
    }
}

/** Log full JSON without truncation (split across log lines). */
fun logFullJsonPayload(tag: String, label: String, json: String) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    val maxChunk = 3500
    if (json.length <= maxChunk) {
        Log.d(tag, "$label $json")
        return
    }
    var offset = 0
    var part = 0
    while (offset < json.length) {
        val end = minOf(offset + maxChunk, json.length)
        Log.d(tag, "$label part=$part ${json.substring(offset, end)}")
        offset = end
        part++
    }
}

fun logSpoolUsageArchiveDiscovery(
    spoolId: Int,
    usageEntries: List<SpoolUsageEntry>,
    archivesRawJson: String?,
) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    Log.d(TAG_SPOOL_USAGE_LINK, "=== spool usage archive discovery spoolId=$spoolId ===")
    usageEntries.forEach { entry ->
        Log.d(TAG_SPOOL_USAGE_LINK, "--- spool usage history item usageHistoryId=${entry.id} ---")
        logFullJsonPayload(TAG_SPOOL_USAGE_LINK, "usageRaw", entry.rawJson)
    }
    if (archivesRawJson != null) {
        Log.d(TAG_SPOOL_USAGE_LINK, "--- archive list (for same spool context) ---")
        logFullJsonPayload(TAG_SPOOL_USAGE_LINK, "archivesListRaw", archivesRawJson)
    }
}
