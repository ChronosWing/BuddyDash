package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolUsageDirectIds
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

/** Archives loaded for spool detail usage linking (may exceed list default). */
const val SPOOL_DETAIL_ARCHIVES_LIMIT = 500

/** Same-session completion vs usage created_at. */
private const val USAGE_ARCHIVE_TIME_STRONG_SECONDS = 300L

/** Max gap for title+date composite path (reprints days later). */
private const val USAGE_ARCHIVE_TIME_LOOSE_SECONDS = 7L * 24 * 3600

private const val USAGE_ARCHIVE_DURATION_TOLERANCE_SECONDS = 120L

const val TAG_SPOOL_USAGE_LINK = "BuddyDash/SpoolUsageLink"

enum class SpoolUsageArchiveLinkKind {
    /** Direct ID on usage row maps to [PrintArchive.id]. */
    ArchiveId,
    /** Normalized title + printer/time/duration composite (color/material not required). */
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
 *
 * Archive [filamentType]/[filamentColor] are sliced metadata only — never required to link.
 */
fun resolveSpoolUsageArchiveLink(
    entry: SpoolUsageEntry,
    archives: List<PrintArchive>,
    spoolMaterial: String? = null,
    spoolColorName: String? = null,
): SpoolUsageArchiveLinkResult {
    val archivesById = archives.associateBy { it.id }
    val archiveIdSet = archivesById.keys

    resolveSpoolUsageArchiveId(entry.directIds, archiveIdSet)?.let { (archiveId, idField) ->
        return linked(
            entry = entry,
            archiveId = archiveId,
            kind = SpoolUsageArchiveLinkKind.ArchiveId,
            reason = "direct_id field=$idField → archive id=$archiveId",
            candidates = listOf(archiveId),
        )
    }

    val usageTitleKey = entry.printName?.let { normalizePrintTitleForMatch(it) }
    if (usageTitleKey == null) {
        return failed(entry, "no_print_name")
    }

    val scored = archives.mapNotNull { archive ->
        scoreUsageArchiveCandidate(
            entry = entry,
            archive = archive,
            usageTitleKey = usageTitleKey,
            spoolMaterial = spoolMaterial,
            spoolColorName = spoolColorName,
        )
    }
    val qualified = scored.filter { it.qualified }
    val candidateIds = qualified.map { it.archive.id }

    when {
        qualified.isEmpty() -> {
            logSpoolUsageLinkFailure(entry, usageTitleKey, archives, scored, "no_qualified_match")
            return failed(entry, "no_deterministic_key_found", candidateIds)
        }
        qualified.size == 1 -> {
            val pick = qualified.first()
            logSpoolUsageLinkScoring(entry, listOf(pick), selected = pick, reason = "single_candidate")
            return linked(
                entry = entry,
                archiveId = pick.archive.id,
                kind = SpoolUsageArchiveLinkKind.CompositeKey,
                reason = pick.linkReason(),
                candidates = candidateIds,
            )
        }
        else -> {
            val best = qualified.minWith(usageArchiveDisambiguationComparator())
            val ties = qualified.filter {
                usageArchiveDisambiguationComparator().compare(it, best) == 0
            }
            if (ties.size == 1) {
                logSpoolUsageLinkScoring(entry, qualified, selected = best, reason = "best_time_plate_tiebreak")
                return linked(
                    entry = entry,
                    archiveId = best.archive.id,
                    kind = SpoolUsageArchiveLinkKind.CompositeKey,
                    reason = best.linkReason(),
                    candidates = candidateIds,
                )
            }
            logSpoolUsageLinkFailure(
                entry,
                usageTitleKey,
                archives,
                scored,
                "ambiguous_qualified count=${ties.size}",
            )
            return failed(
                entry,
                "ambiguous_composite_match count=${ties.size}",
                ties.map { it.archive.id },
            )
        }
    }
}

/** Try usage direct IDs against [PrintArchive.id] in priority order. */
fun resolveSpoolUsageArchiveId(
    directIds: SpoolUsageDirectIds,
    archiveIds: Set<Int>,
): Pair<Int, String>? {
    for ((field, id) in directIds.resolutionOrder()) {
        if (id in archiveIds) return id to field
    }
    return null
}

fun archiveTitleKeys(archive: PrintArchive): Set<String> =
    buildSet {
        normalizePrintTitleForMatch(archive.displayName)?.let { add(it) }
        archive.filename?.let { normalizePrintTitleForMatch(it) }?.let { add(it) }
    }

private data class UsageArchiveMatchCandidate(
    val archive: PrintArchive,
    val titleExact: Boolean,
    val titleFuzzy: Boolean,
    val printerMatch: Boolean,
    val printerReject: Boolean,
    val plateMatch: Boolean,
    val plateReject: Boolean,
    val timeDeltaSeconds: Long?,
    val timeCloseStrong: Boolean,
    val timeCloseLoose: Boolean,
    val durationMatch: Boolean,
    val materialColorTieBreaker: Int,
    val weightDeltaGrams: Double?,
) {
    val titleMatch: Boolean get() = titleExact || titleFuzzy

    /** Color/material are never required; archive slice metadata is ignored for qualification. */
    val qualified: Boolean
        get() {
            if (printerReject || plateReject) return false
            if (!titleMatch) return false
            if (titleExact && printerMatch) return true
            if (titleExact && (timeCloseStrong || timeCloseLoose || durationMatch)) return true
            if (titleFuzzy && printerMatch && (timeCloseStrong || timeCloseLoose || durationMatch)) return true
            return false
        }

    fun linkReason(): String = buildString {
        append("composite:")
        if (titleExact) append(" title_exact") else if (titleFuzzy) append(" title_fuzzy")
        if (printerMatch) append(" printer")
        if (timeCloseStrong) append(" time_strong") else if (timeCloseLoose) append(" time_loose")
        if (durationMatch) append(" duration")
        if (plateMatch) append(" plate")
        if (materialColorTieBreaker > 0) append(" material_tie=$materialColorTieBreaker")
        append(" (color_ignored)")
    }

    fun scoringLogLine(): String =
        "archiveId=${archive.id} titleExact=$titleExact titleFuzzy=$titleFuzzy " +
            "printerMatch=$printerMatch plateMatch=$plateMatch " +
            "timeDeltaSec=${timeDeltaSeconds ?: "n/a"} timeStrong=$timeCloseStrong timeLoose=$timeCloseLoose " +
            "durationMatch=$durationMatch materialTie=$materialColorTieBreaker " +
            "weightDelta=${weightDeltaGrams?.let { "%.1f".format(it) } ?: "n/a"} " +
            "qualified=$qualified colorIgnored=true"
}

private fun scoreUsageArchiveCandidate(
    entry: SpoolUsageEntry,
    archive: PrintArchive,
    usageTitleKey: String,
    spoolMaterial: String?,
    spoolColorName: String?,
): UsageArchiveMatchCandidate? {
    val archiveKeys = archiveTitleKeys(archive)
    val titleExact = archiveKeys.contains(usageTitleKey)
    val titleFuzzy = !titleExact && archiveKeys.any { key ->
        titlesContainMatchHighConfidence(usageTitleKey, key)
    }
    if (!titleExact && !titleFuzzy) return null

    val usagePrinterId = entry.printerId
    val archivePrinterId = archive.printerId
    val printerReject = usagePrinterId != null &&
        archivePrinterId != null &&
        usagePrinterId != archivePrinterId
    val printerMatch = usagePrinterId != null &&
        archivePrinterId != null &&
        usagePrinterId == archivePrinterId

    val usagePlate = entry.plateNumber
    val archivePlate = archive.plateNumber
    val plateReject = usagePlate != null &&
        archivePlate != null &&
        usagePlate != archivePlate
    val plateMatch = usagePlate != null &&
        archivePlate != null &&
        usagePlate == archivePlate

    val timeDeltaSeconds = usageArchiveTimeDeltaSeconds(entry.createdAtIso, archive)
    val timeCloseStrong = timeDeltaSeconds?.let { it <= USAGE_ARCHIVE_TIME_STRONG_SECONDS } == true
    val timeCloseLoose = timeDeltaSeconds?.let { it <= USAGE_ARCHIVE_TIME_LOOSE_SECONDS } == true
    val durationMatch = usageDurationMatchesArchive(entry, archive)

    val usageGrams = entry.weightUsedGrams
    val archiveGrams = archive.filamentUsage?.weightGrams
    val weightDeltaGrams = if (archiveGrams != null) {
        kotlin.math.abs(usageGrams - archiveGrams)
    } else {
        null
    }

    return UsageArchiveMatchCandidate(
        archive = archive,
        titleExact = titleExact,
        titleFuzzy = titleFuzzy,
        printerMatch = printerMatch,
        printerReject = printerReject,
        plateMatch = plateMatch,
        plateReject = plateReject,
        timeDeltaSeconds = timeDeltaSeconds,
        timeCloseStrong = timeCloseStrong,
        timeCloseLoose = timeCloseLoose,
        durationMatch = durationMatch,
        materialColorTieBreaker = materialColorTieBreaker(archive, spoolMaterial, spoolColorName),
        weightDeltaGrams = weightDeltaGrams,
    )
}

private fun usageArchiveDisambiguationComparator(): Comparator<UsageArchiveMatchCandidate> =
    compareBy<UsageArchiveMatchCandidate> { it.timeDeltaSeconds ?: Long.MAX_VALUE }
        .thenBy { if (it.plateMatch) 0 else 1 }
        .thenByDescending { it.materialColorTieBreaker }
        .thenBy { it.weightDeltaGrams ?: Double.MAX_VALUE }

/** Weak tie-breaker only — never used to qualify or reject a link. */
private fun materialColorTieBreaker(
    archive: PrintArchive,
    spoolMaterial: String?,
    spoolColorName: String?,
): Int {
    var score = 0
    val archiveMat = archive.filamentType?.trim()?.takeIf { it.isNotBlank() }
    val spoolMat = spoolMaterial?.trim()?.takeIf { it.isNotBlank() }
    if (archiveMat != null && spoolMat != null) {
        if (normalizeArchiveMaterialKey(archiveMat) ==
            normalizeArchiveMaterialKey(normalizeFilamentType(spoolMat) ?: spoolMat)
        ) {
            score += 1
        }
    }
    val archiveColor = archive.filamentColor?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val spoolColor = spoolColorName?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    if (archiveColor != null && spoolColor != null) {
        if (archiveColor == spoolColor || archiveColor in spoolColor || spoolColor in archiveColor) {
            score += 1
        }
    }
    return score
}

private fun usageArchiveTimeDeltaSeconds(usageCreatedAtIso: String, archive: PrintArchive): Long? {
    val usageInstant = parseSpoolUsageInstantForLink(usageCreatedAtIso) ?: return null
    val archiveInstant = resolveArchiveStatsInstant(archive) ?: return null
    return kotlin.math.abs(Duration.between(usageInstant, archiveInstant).seconds)
}

private fun usageDurationMatchesArchive(entry: SpoolUsageEntry, archive: PrintArchive): Boolean {
    val usageDuration = resolveUsageDurationSeconds(entry) ?: return false
    val archiveDuration = archive.durationSeconds?.takeIf { it > 0 } ?: return false
    return kotlin.math.abs(usageDuration - archiveDuration) <= USAGE_ARCHIVE_DURATION_TOLERANCE_SECONDS
}

private fun resolveUsageDurationSeconds(entry: SpoolUsageEntry): Int? {
    if (entry.rawJson.isBlank()) return null
    return try {
        val json = JSONObject(entry.rawJson)
        usageJsonPositiveInt(json, "duration_seconds", "duration", "print_duration")
            ?: json.optJSONObject("extra_data")?.let { extra ->
                usageJsonPositiveInt(extra, "duration_seconds", "duration", "print_duration")
            }
    } catch (_: Exception) {
        null
    }
}

private fun usageJsonPositiveInt(json: JSONObject, vararg keys: String): Int? {
    for (key in keys) {
        if (!json.has(key) || json.isNull(key)) continue
        val value = json.optInt(key, -1)
        if (value >= 0) return value
        json.optString(key).trim().toIntOrNull()?.takeIf { it >= 0 }?.let { return it }
    }
    return null
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

private fun logSpoolUsageLinkScoring(
    entry: SpoolUsageEntry,
    candidates: List<UsageArchiveMatchCandidate>,
    selected: UsageArchiveMatchCandidate,
    reason: String,
) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    Log.d(
        TAG_SPOOL_USAGE_LINK,
        "matchScoring usageHistoryId=${entry.id} selection=$reason selectedArchiveId=${selected.archive.id}",
    )
    candidates.forEach { Log.d(TAG_SPOOL_USAGE_LINK, "  ${it.scoringLogLine()}") }
}

private fun logSpoolUsageLinkFailure(
    entry: SpoolUsageEntry,
    usageTitleKey: String,
    archives: List<PrintArchive>,
    scored: List<UsageArchiveMatchCandidate>,
    reason: String,
) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    val usageRaw = entry.printName
    Log.d(
        TAG_SPOOL_USAGE_LINK,
        "matchScoringFailed usageHistoryId=${entry.id} reason=$reason " +
            "rawUsageTitle=$usageRaw normalizedUsage=$usageTitleKey " +
            "printerId=${entry.printerId} weight=${entry.weightUsedGrams}g " +
            "createdAt=${entry.createdAtIso} plate=${entry.plateNumber} colorMatchIgnored=true",
    )
    val scoredById = scored.associateBy { it.archive.id }
    archives.take(12).forEach { archive ->
        val candidate = scoredById[archive.id]
        if (candidate != null) {
            Log.d(TAG_SPOOL_USAGE_LINK, "  ${candidate.scoringLogLine()}")
        } else {
            val archiveKeys = archiveTitleKeys(archive)
            Log.d(
                TAG_SPOOL_USAGE_LINK,
                "  archiveId=${archive.id} titleExact=false titleFuzzy=false " +
                    "rawArchiveTitle=${archive.displayName} normalizedArchive=${archiveKeys.joinToString("|")} " +
                    "archiveMaterial=${archive.filamentType} archiveColor=${archive.filamentColor} " +
                    "qualified=false colorIgnored=true rejected=title_mismatch",
            )
        }
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
