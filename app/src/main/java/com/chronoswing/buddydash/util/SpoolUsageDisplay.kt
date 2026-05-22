package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolUsageDirectIds
import com.chronoswing.buddydash.data.model.SpoolUsageEntry
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.network.tokenAuthenticatedImageUrl
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** Temporary: spool usage history diagnostics. Set false before release. */
const val DEBUG_LOG_SPOOL_USAGE = true // set false before release

const val TAG_SPOOL_USAGE = "BuddyDash/SpoolUsage"

private val PLATE_SUFFIX_PATTERN = Regex(
    """\s+(?:-\s+)?Plate\s+\d+$""",
    RegexOption.IGNORE_CASE,
)

fun parseSpoolUsageHistoryList(body: String): List<SpoolUsageEntry> {
    val array = JSONArray(body)
    return buildList {
        for (i in 0 until array.length()) {
            parseSpoolUsageEntry(array.optJSONObject(i) ?: continue)?.let { add(it) }
        }
    }.sortedByDescending { it.createdAtIso }
}

private fun parseSpoolUsageEntry(json: JSONObject): SpoolUsageEntry? {
    val id = json.optInt("id", -1)
    val spoolId = json.optInt("spool_id", -1)
    if (id < 0 || spoolId < 0) return null
    val weightUsed = jsonPositiveDouble(json, "weight_used") ?: return null
    val createdAt = spoolJsonOptionalString(json, "created_at") ?: return null
    return SpoolUsageEntry(
        id = id,
        spoolId = spoolId,
        printerId = json.optInt("printer_id", -1).takeIf { it >= 0 },
        printName = spoolJsonOptionalString(json, "print_name"),
        weightUsedGrams = weightUsed,
        percentUsed = json.optInt("percent_used", -1).takeIf { it >= 0 },
        status = spoolJsonOptionalString(json, "status") ?: "unknown",
        createdAtIso = createdAt,
        directIds = resolveSpoolUsageDirectIds(json),
        usageThumbnailPath = resolveSpoolUsageThumbnailPath(json),
        libraryFileId = spoolJsonPositiveInt(json, "library_file_id")
            ?: spoolJsonPositiveInt(json, "file_id"),
        materialType = spoolJsonOptionalString(json, "filament_type")
            ?: spoolJsonOptionalString(json, "material"),
        filamentColor = spoolJsonOptionalString(json, "filament_color")
            ?: spoolJsonOptionalString(json, "color"),
        durationSeconds = spoolJsonPositiveInt(json, "duration_seconds")
            ?: spoolJsonPositiveInt(json, "print_time_seconds")
            ?: spoolJsonPositiveInt(json, "actual_time_seconds"),
    )
}

/** Read optional direct IDs from usage JSON or nested [extra_data] (runtime only). */
fun resolveSpoolUsageDirectIds(json: JSONObject): SpoolUsageDirectIds {
    fun intField(vararg keys: String): Int? {
        for (key in keys) {
            spoolJsonPositiveInt(json, key)?.let { return it }
        }
        return null
    }
    val fromRoot = SpoolUsageDirectIds(
        archiveId = intField("archive_id", "archiveId", "print_archive_id", "printArchiveId"),
        historyId = intField("history_id", "historyId"),
        jobId = intField("job_id", "jobId"),
        taskId = intField("task_id", "taskId"),
        fileId = intField("file_id", "fileId"),
        printId = intField("print_id", "printId"),
    )
    val extra = json.optJSONObject("extra_data") ?: return fromRoot
    return SpoolUsageDirectIds(
        archiveId = fromRoot.archiveId
            ?: spoolJsonPositiveInt(extra, "archive_id", "archiveId", "print_archive_id"),
        historyId = fromRoot.historyId
            ?: spoolJsonPositiveInt(extra, "history_id", "historyId"),
        jobId = fromRoot.jobId ?: spoolJsonPositiveInt(extra, "job_id", "jobId"),
        taskId = fromRoot.taskId ?: spoolJsonPositiveInt(extra, "task_id", "taskId"),
        fileId = fromRoot.fileId ?: spoolJsonPositiveInt(extra, "file_id", "fileId"),
        printId = fromRoot.printId ?: spoolJsonPositiveInt(extra, "print_id", "printId"),
    )
}

private fun resolveSpoolUsageThumbnailPath(json: JSONObject): String? {
    for (key in listOf(
        "thumbnail_url",
        "thumbnailUrl",
        "cover_url",
        "coverUrl",
        "image_url",
        "imageUrl",
        "thumbnail_path",
        "thumbnail",
        "cover_image",
    )) {
        spoolJsonOptionalString(json, key)?.let { return it }
    }
    val extra = json.optJSONObject("extra_data") ?: return null
    for (key in listOf("thumbnail_url", "thumbnail_path", "cover_url")) {
        spoolJsonOptionalString(extra, key)?.let { return it }
    }
    return null
}

private fun spoolJsonPositiveInt(json: JSONObject, vararg keys: String): Int? {
    for (key in keys) {
        if (!json.has(key) || json.isNull(key)) continue
        val value = json.optInt(key, -1)
        if (value >= 0) return value
    }
    return null
}

enum class SpoolUsageArchiveMatchKind {
    DirectId,
    NameOnly,
    NamePrinter,
    NameMaterial,
    NameDuration,
    NameDate,
    None,
}

enum class SpoolUsageThumbnailSource {
    Archive,
    UsageUrl,
    FileIcon,
}

data class SpoolUsageMatchResult(
    val archiveId: Int?,
    val kind: SpoolUsageArchiveMatchKind,
    val reason: String,
    val candidateArchiveIds: List<Int>,
    val directIdsFound: SpoolUsageDirectIds,
    val normalizedUsageName: String?,
)

data class SpoolUsageDisplayItem(
    val entry: SpoolUsageEntry,
    val archiveId: Int?,
    val archiveMatchKind: SpoolUsageArchiveMatchKind,
    val thumbnailSource: SpoolUsageThumbnailSource,
    val usageImageUrl: String?,
    val isTappable: Boolean,
    val displayName: String,
    val weightLine: String,
    val printerLine: String?,
    val matchResult: SpoolUsageMatchResult,
)

fun buildSpoolUsageDisplayItems(
    entries: List<SpoolUsageEntry>,
    archives: List<PrintArchive>,
    printerNamesById: Map<Int, String>,
    spoolMaterial: String? = null,
    spoolColorName: String? = null,
    serverUrl: String = "",
    cameraToken: String = "",
): List<SpoolUsageDisplayItem> =
    entries.map { entry ->
        val match = resolveSpoolUsageArchiveMatch(
            entry = entry,
            archives = archives,
            printerNamesById = printerNamesById,
            spoolMaterial = spoolMaterial,
            spoolColorName = spoolColorName,
        )
        val archiveId = match.archiveId
        val usageImageUrl = resolveSpoolUsageRowImageUrl(
            entry = entry,
            archiveId = archiveId,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
        )
        val thumbnailSource = when {
            archiveId != null -> SpoolUsageThumbnailSource.Archive
            usageImageUrl != null -> SpoolUsageThumbnailSource.UsageUrl
            else -> SpoolUsageThumbnailSource.FileIcon
        }
        SpoolUsageDisplayItem(
            entry = entry,
            archiveId = archiveId,
            archiveMatchKind = match.kind,
            thumbnailSource = thumbnailSource,
            usageImageUrl = usageImageUrl,
            isTappable = archiveId != null,
            displayName = formatSpoolUsagePrintName(entry),
            weightLine = formatSpoolUsageWeightUsedLine(entry),
            printerLine = formatSpoolUsagePrinterLine(entry, printerNamesById),
            matchResult = match,
        )
    }

fun resolveSpoolUsageArchiveMatch(
    entry: SpoolUsageEntry,
    archives: List<PrintArchive>,
    printerNamesById: Map<Int, String>,
    spoolMaterial: String? = null,
    spoolColorName: String? = null,
): SpoolUsageMatchResult {
    val normalizedName = entry.printName?.let { normalizePrintNameForArchiveMatch(it) }
    val archiveIds = archives.map { it.id }.toSet()

    resolveDirectArchiveId(entry.directIds, archiveIds)?.let { archiveId ->
        return SpoolUsageMatchResult(
            archiveId = archiveId,
            kind = SpoolUsageArchiveMatchKind.DirectId,
            reason = "direct_id_matched_archive",
            candidateArchiveIds = listOf(archiveId),
            directIdsFound = entry.directIds,
            normalizedUsageName = normalizedName,
        )
    }

    val printName = entry.printName?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    if (printName == null) {
        return noMatch(entry, normalizedName, emptyList(), "no_print_name")
    }
    val usageNameKey = normalizedName ?: return noMatch(entry, normalizedName, emptyList(), "name_not_normalizable")

    val nameCandidates = archives.filter { archive ->
        normalizePrintNameForArchiveMatch(archive.displayName) == usageNameKey
    }
    val candidateIds = nameCandidates.map { it.id }

    if (nameCandidates.isEmpty()) {
        return noMatch(entry, normalizedName, emptyList(), "no_name_match")
    }
    if (nameCandidates.size == 1) {
        return SpoolUsageMatchResult(
            archiveId = nameCandidates.first().id,
            kind = SpoolUsageArchiveMatchKind.NameOnly,
            reason = "unique_normalized_name",
            candidateArchiveIds = candidateIds,
            directIdsFound = entry.directIds,
            normalizedUsageName = normalizedName,
        )
    }

    if (entry.printerId != null) {
        val byPrinter = nameCandidates.filter { archive ->
            archiveMatchesSpoolUsagePrinter(archive, entry, printerNamesById)
        }
        if (byPrinter.size == 1) {
            return matched(entry, normalizedName, byPrinter.first().id, candidateIds, SpoolUsageArchiveMatchKind.NamePrinter, "name+printer")
        }
    }

    if (hasSpoolUsageMaterialSignal(entry, spoolMaterial, spoolColorName)) {
        val byMaterial = nameCandidates.filter { archive ->
            archiveMatchesSpoolUsageMaterial(archive, entry, spoolMaterial, spoolColorName)
        }
        if (byMaterial.size == 1) {
            return matched(entry, normalizedName, byMaterial.first().id, candidateIds, SpoolUsageArchiveMatchKind.NameMaterial, "name+material")
        }
    }

    if (entry.durationSeconds != null || parseSpoolUsageInstant(entry.createdAtIso) != null) {
        val byDuration = nameCandidates.filter { archive ->
            archiveMatchesSpoolUsageDuration(entry, archive)
        }
        if (byDuration.size == 1) {
            return matched(entry, normalizedName, byDuration.first().id, candidateIds, SpoolUsageArchiveMatchKind.NameDuration, "name+duration")
        }
    }

    if (parseSpoolUsageInstant(entry.createdAtIso) != null) {
        val byDate = nameCandidates.filter { archive ->
            archiveMatchesSpoolUsageDate(entry, archive)
        }
        if (byDate.size == 1) {
            return matched(entry, normalizedName, byDate.first().id, candidateIds, SpoolUsageArchiveMatchKind.NameDate, "name+date")
        }
    }

    return noMatch(
        entry = entry,
        normalizedName = normalizedName,
        candidateIds = candidateIds,
        reason = "ambiguous_name_match count=${nameCandidates.size}",
    )
}

fun resolveDirectArchiveId(directIds: SpoolUsageDirectIds, archiveIds: Set<Int>): Int? {
    for ((_, id) in directIds.resolutionOrder()) {
        if (id in archiveIds) return id
    }
    return null
}

/** @deprecated Use [resolveSpoolUsageArchiveMatch]; kept for unit tests. */
fun matchArchiveForSpoolUsage(
    entry: SpoolUsageEntry,
    archives: List<PrintArchive>,
    printerNamesById: Map<Int, String> = emptyMap(),
    spoolMaterial: String? = null,
    spoolColorName: String? = null,
): Int? = resolveSpoolUsageArchiveMatch(
    entry,
    archives,
    printerNamesById,
    spoolMaterial,
    spoolColorName,
).archiveId

fun normalizePrintNameForArchiveMatch(raw: String): String? {
    val trimmed = raw.trim()
    if (!isMeaningfulSpoolField(trimmed)) return null
    val display = formatFilenameForDisplay(trimmed)
    val unicode = Normalizer.normalize(display, Normalizer.Form.NFKC)
    val noPlate = stripPlateSuffixForArchiveMatch(unicode)
    val withoutExt = noPlate.replace(Regex("\\.[^.]+$"), "").trim()
    val key = (withoutExt.ifBlank { noPlate }).lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
    return key.takeIf { it.isNotBlank() }
}

fun stripPlateSuffixForArchiveMatch(name: String): String =
    name.replace(PLATE_SUFFIX_PATTERN, "").trim()

private fun archiveMatchesSpoolUsagePrinter(
    archive: PrintArchive,
    entry: SpoolUsageEntry,
    printerNamesById: Map<Int, String>,
): Boolean {
    val usagePrinterId = entry.printerId ?: return false
    if (archive.printerId != null && archive.printerId == usagePrinterId) return true
    val usagePrinterName = normalizePrinterNameForMatch(printerNamesById[usagePrinterId])
    val archivePrinterName = normalizePrinterNameForMatch(archive.printerName)
    return usagePrinterName != null &&
        archivePrinterName != null &&
        usagePrinterName == archivePrinterName
}

private fun normalizePrinterNameForMatch(name: String?): String? =
    name?.trim()?.takeIf { isMeaningfulSpoolField(it) }?.lowercase()

private fun hasSpoolUsageMaterialSignal(
    entry: SpoolUsageEntry,
    spoolMaterial: String?,
    spoolColorName: String?,
): Boolean {
    val usageType = entry.materialType?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val usageColor = entry.filamentColor?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val spoolMat = spoolMaterial?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val spoolColor = spoolColorName?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    return usageType != null || usageColor != null || spoolMat != null || spoolColor != null
}

private fun archiveMatchesSpoolUsageMaterial(
    archive: PrintArchive,
    entry: SpoolUsageEntry,
    spoolMaterial: String?,
    spoolColorName: String?,
): Boolean {
    val archiveType = archive.filamentType?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val usageType = entry.materialType?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val spoolMat = spoolMaterial?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val materialToMatch = usageType ?: spoolMat
    if (archiveType != null && materialToMatch != null) {
        if (!spoolUsageMaterialsMatch(archiveType, materialToMatch)) return false
    }
    val archiveColor = archive.filamentColor?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val usageColor = entry.filamentColor?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val spoolColor = spoolColorName?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val colorToMatch = usageColor ?: spoolColor
    if (archiveColor != null && colorToMatch != null) {
        if (!spoolUsageColorsMatch(archiveColor, colorToMatch)) return false
    }
    return archiveType != null || archiveColor != null
}

private fun spoolUsageMaterialsMatch(archiveType: String, otherMaterial: String): Boolean {
    val archiveKey = normalizeFilamentType(archiveType)?.lowercase()
        ?: archiveType.lowercase()
    val otherKey = normalizeFilamentType(otherMaterial)?.lowercase()
        ?: otherMaterial.lowercase()
    if (archiveKey == otherKey) return true
    if (archiveKey.length >= 3 && otherKey.length >= 3) {
        if (archiveKey.startsWith(otherKey) || otherKey.startsWith(archiveKey)) return true
    }
    val archiveToken = archiveKey.split('-', ' ', '_').firstOrNull().orEmpty()
    val otherToken = otherKey.split('-', ' ', '_').firstOrNull().orEmpty()
    return archiveToken.length >= 3 && archiveToken == otherToken
}

private fun spoolUsageColorsMatch(archiveColor: String, otherColor: String): Boolean {
    val a = archiveColor.lowercase()
    val b = otherColor.lowercase()
    return a == b || a.contains(b) || b.contains(a)
}

private fun archiveMatchesSpoolUsageDuration(
    entry: SpoolUsageEntry,
    archive: PrintArchive,
): Boolean {
    val usageDur = entry.durationSeconds
    val archiveDur = archive.durationSeconds
    if (usageDur != null && usageDur > 0 && archiveDur != null && archiveDur > 0) {
        val diff = kotlin.math.abs(usageDur - archiveDur)
        val tolerance = maxOf(60, (archiveDur * 0.15).toInt())
        return diff <= tolerance
    }
    return archiveMatchesSpoolUsageDate(entry, archive)
}

private fun archiveMatchesSpoolUsageDate(
    entry: SpoolUsageEntry,
    archive: PrintArchive,
): Boolean {
    val usageInstant = parseSpoolUsageInstant(entry.createdAtIso) ?: return false
    val archiveInstant = resolveArchiveStatsInstant(archive) ?: return false
    return spoolUsageDatesAlign(usageInstant, archiveInstant, ZoneId.systemDefault())
}

fun resolveSpoolUsageRowImageUrl(
    entry: SpoolUsageEntry,
    archiveId: Int?,
    serverUrl: String,
    cameraToken: String,
): String? {
    if (archiveId != null) return null
    entry.usageThumbnailPath?.let { path ->
        tokenAuthenticatedImageUrl(serverUrl, path, cameraToken)?.let { return it }
    }
    val fileId = entry.libraryFileId ?: entry.directIds.fileId
    if (fileId != null) {
        return tokenAuthenticatedImageUrl(
            serverUrl,
            BambuddyApi.libraryFileThumbnailPath(fileId),
            cameraToken,
        )
    }
    return null
}

private fun parseSpoolUsageInstant(iso: String): Instant? =
    try {
        Instant.parse(iso)
    } catch (_: DateTimeParseException) {
        parseArchiveTimestamp(iso)
    }

private fun spoolUsageDatesAlign(
    usageInstant: Instant,
    archiveInstant: Instant,
    zone: ZoneId,
): Boolean {
    val usageDay = usageInstant.atZone(zone).toLocalDate()
    val archiveDay = archiveInstant.atZone(zone).toLocalDate()
    if (usageDay == archiveDay) return true
    val diffSeconds = kotlin.math.abs(Duration.between(usageInstant, archiveInstant).seconds)
    return diffSeconds <= 2 * 3600
}

private fun matched(
    entry: SpoolUsageEntry,
    normalizedName: String?,
    archiveId: Int,
    candidateIds: List<Int>,
    kind: SpoolUsageArchiveMatchKind,
    reason: String,
) = SpoolUsageMatchResult(
    archiveId = archiveId,
    kind = kind,
    reason = reason,
    candidateArchiveIds = candidateIds,
    directIdsFound = entry.directIds,
    normalizedUsageName = normalizedName,
)

private fun noMatch(
    entry: SpoolUsageEntry,
    normalizedName: String?,
    candidateIds: List<Int>,
    reason: String,
) = SpoolUsageMatchResult(
    archiveId = null,
    kind = SpoolUsageArchiveMatchKind.None,
    reason = reason,
    candidateArchiveIds = candidateIds,
    directIdsFound = entry.directIds,
    normalizedUsageName = normalizedName,
)

fun formatSpoolUsagePrintName(entry: SpoolUsageEntry): String {
    val raw = entry.printName?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    return if (raw != null) formatFilenameForDisplay(raw) else "Print #${entry.id}"
}

fun formatSpoolUsageWeight(entry: SpoolUsageEntry): String =
    formatFilamentWeightGrams(entry.weightUsedGrams)

fun formatSpoolUsageWeightUsedLine(entry: SpoolUsageEntry): String =
    "${formatFilamentWeightGrams(entry.weightUsedGrams)} used"

fun formatSpoolUsageDate(entry: SpoolUsageEntry): String? =
    formatSpoolLastUsed(entry.createdAtIso)

fun formatSpoolUsagePrinterLine(
    entry: SpoolUsageEntry,
    printerNamesById: Map<Int, String>,
): String? {
    val printerId = entry.printerId ?: return null
    val name = printerNamesById[printerId] ?: "Printer $printerId"
    return name
}

fun logSpoolUsageFetch(
    spoolId: Int,
    path: String,
    rawBodyPreview: String?,
    entries: List<SpoolUsageEntry>,
    error: Throwable? = null,
) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    if (error != null) {
        Log.e(
            TAG_SPOOL_USAGE,
            "spoolUsage spoolId=$spoolId endpoint=$path failed: ${error.message}",
            error,
        )
        return
    }
    Log.d(
        TAG_SPOOL_USAGE,
        "spoolUsage spoolId=$spoolId endpoint=$path count=${entries.size} " +
            "preview=${rawBodyPreview?.take(200)}",
    )
    entries.take(8).forEach { entry ->
        Log.d(
            TAG_SPOOL_USAGE,
            "spoolUsageRaw usageId=${entry.id} print=${entry.printName} " +
                "weight=${entry.weightUsedGrams}g printerId=${entry.printerId} " +
                "directIds=${entry.directIds} material=${entry.materialType} " +
                "color=${entry.filamentColor} duration=${entry.durationSeconds} " +
                "thumbPath=${entry.usageThumbnailPath} libraryFile=${entry.libraryFileId} " +
                "at=${entry.createdAtIso}",
        )
    }
}

fun logSpoolUsageDisplayItems(spoolId: Int, items: List<SpoolUsageDisplayItem>) {
    if (!DEBUG_LOG_SPOOL_USAGE) return
    items.forEach { item ->
        val entry = item.entry
        val match = item.matchResult
        Log.d(
            TAG_SPOOL_USAGE,
            "spoolUsageRow spoolId=$spoolId usageId=${entry.id} " +
                "rawPrint=${entry.printName} normalizedName=${match.normalizedUsageName} " +
                "usageAmount=${item.weightLine} directIds=${match.directIdsFound} " +
                "candidates=${match.candidateArchiveIds} " +
                "selectedArchiveId=${match.archiveId} reason=${match.reason} " +
                "matchKind=${item.archiveMatchKind} thumbnailSource=${item.thumbnailSource} " +
                "usageImageUrl=${item.usageImageUrl?.take(80)} tappable=${item.isTappable}",
        )
    }
}

/** @deprecated Use [resolveSpoolUsageDirectIds]. */
fun resolveSpoolUsageArchiveId(json: JSONObject): Int? =
    resolveSpoolUsageDirectIds(json).archiveId

private fun jsonPositiveDouble(json: JSONObject, key: String): Double? {
    if (!json.has(key) || json.isNull(key)) return null
    val parsed = json.optDouble(key, Double.NaN)
    if (!parsed.isNaN() && parsed >= 0.0) return parsed
    return json.optString(key).trim().toDoubleOrNull()?.takeIf { it >= 0.0 }
}
