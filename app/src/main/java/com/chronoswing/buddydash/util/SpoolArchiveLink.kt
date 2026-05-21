package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import kotlin.math.sqrt
import org.json.JSONObject

/** Temporary: spool ↔ archive linking diagnostics. Set false before release. */
const val DEBUG_LOG_SPOOL_ARCHIVE_LINK = true // set false before release

const val TAG_SPOOL_ARCHIVE_LINK = "BuddyDash/SpoolArchiveLink"

const val SPOOL_ARCHIVE_MATCH_PREVIEW_LIMIT = 5

/** Max RGB distance (0–441) to treat two hex colors as the same filament color. */
private const val COLOR_APPROX_MAX_DISTANCE = 48.0

private val MATERIAL_COSMETIC_SUFFIX = Regex(
    pattern = """\s+(basic|hf|pro|plus|lite|matte|silk|transparent|translucent|cf|gf|tpu\+?)$""",
    option = RegexOption.IGNORE_CASE,
)

data class SpoolArchiveMatches(
    val isExactSpoolId: Boolean,
    val archives: List<PrintArchive>,
)

/** Structured archive→spool lookup; never put raw hex in the visible search field. */
data class ArchiveSpoolLookupFilter(
    val materialLabel: String,
    val materialKey: String,
    val colorHexes: List<String>,
    val colorDisplayLabel: String?,
)

sealed class ArchiveMaterialNavigation {
    data class SpoolDetail(val spoolId: Int) : ArchiveMaterialNavigation()
    data class SpoolsFiltered(val lookupFilter: ArchiveSpoolLookupFilter) : ArchiveMaterialNavigation()
    data object None : ArchiveMaterialNavigation()
}

/** Read optional spool id from archive JSON or nested [extra_data] (runtime fields only). */
fun resolveArchiveSpoolId(json: JSONObject): Int? {
    json.optInt("spool_id", -1).takeIf { it >= 0 }?.let { return it }
    val extra = json.optJSONObject("extra_data") ?: return null
    for (key in listOf("spool_id", "inventory_spool_id", "spoolId")) {
        extra.optInt(key, -1).takeIf { it >= 0 }?.let { return it }
    }
    return null
}

/** Lowercase material key with cosmetic suffixes stripped (e.g. "PETG Basic" → "petg"). */
fun normalizeArchiveMaterialKey(material: String): String {
    var key = material.trim().lowercase().replace(Regex("""\s+"""), " ")
    while (true) {
        val stripped = MATERIAL_COSMETIC_SUFFIX.replace(key, "").trim()
        if (stripped == key) break
        key = stripped
    }
    return key
}

/** Normalized #rrggbb for comparison; accepts with or without leading #. */
fun normalizeMatchColorHex(raw: String?): String? =
    normalizeInventoryColor(raw)?.lowercase()

fun matchArchivesForSpool(
    spool: SpoolInventoryItem,
    archives: List<PrintArchive>,
): SpoolArchiveMatches {
    val exact = archives.filter { it.spoolId == spool.id }
        .sortedByDescending { it.statsCompletedAtMillis ?: 0L }
    if (exact.isNotEmpty()) {
        logSpoolArchiveMatch(spool, exact, isExact = true)
        return SpoolArchiveMatches(isExactSpoolId = true, archives = exact)
    }
    val material = archives.filter { archiveMatchesSpoolMaterial(spool, it) }
        .sortedByDescending { it.statsCompletedAtMillis ?: 0L }
    logSpoolArchiveMatch(spool, material, isExact = false)
    return SpoolArchiveMatches(isExactSpoolId = false, archives = material)
}

fun archiveMatchesSpoolMaterial(spool: SpoolInventoryItem, archive: PrintArchive): Boolean {
    val archiveType = archive.filamentType?.trim()?.takeIf { isMeaningfulArchiveField(it) }
        ?: return false
    if (!materialsMatchForLookup(archiveType, spool.material)) return false

    val archiveHexes = normalizedArchiveColorHexes(archive)
    val spoolHexes = normalizedSpoolColorHexes(spool)
    if (archiveHexes.isEmpty()) return true

    if (spoolHexes.isNotEmpty()) {
        return colorHexesMatch(archiveHexes, spoolHexes)
    }
    return colorNamesLooselyMatch(archive.filamentColor, spool.colorName)
}

fun buildArchiveSpoolLookupFilter(
    archive: PrintArchive,
    matchesForColorLabel: List<SpoolInventoryItem> = emptyList(),
): ArchiveSpoolLookupFilter {
    val materialLabel = formatArchiveDetailMaterialType(archive)
        ?: archive.filamentType?.trim().orEmpty()
    val materialKey = archive.filamentType?.trim()
        ?.takeIf { isMeaningfulArchiveField(it) }
        ?.let { normalizeArchiveMaterialKey(it) }
        .orEmpty()
    return ArchiveSpoolLookupFilter(
        materialLabel = materialLabel,
        materialKey = materialKey,
        colorHexes = normalizedArchiveColorHexes(archive).distinct(),
        colorDisplayLabel = resolveArchiveColorDisplayLabel(archive, matchesForColorLabel),
    )
}

/** Decode comma-separated hex tokens from navigation (no # prefix required). */
fun parseArchiveLookupColorHexesArg(arg: String): List<String> =
    if (arg.isBlank()) {
        emptyList()
    } else {
        arg.split(',')
            .mapNotNull { normalizeMatchColorHex(it.trim()) }
            .distinct()
    }

/** Banner line, e.g. "PLA • White". */
fun archiveLookupFilterSummary(filter: ArchiveSpoolLookupFilter): String {
    val material = filter.materialLabel.ifBlank { filter.materialKey.uppercase() }
    val color = filter.colorDisplayLabel?.trim()?.takeIf { it.isNotEmpty() }
    return if (color != null) "$material • $color" else material
}

/** Structured archive lookup: material required; color required when archive has color data. */
fun spoolMatchesArchiveLookupFilter(
    spool: SpoolInventoryItem,
    filter: ArchiveSpoolLookupFilter,
): Boolean {
    if (filter.materialKey.isBlank()) return false
    if (!materialsMatchForLookupKey(filter.materialKey, spool.material)) return false
    val requiresColor = filter.colorHexes.isNotEmpty() ||
        !filter.colorDisplayLabel.isNullOrBlank()
    if (!requiresColor) return true
    return matchesArchiveLookupColor(spool, filter)
}

/** Spools matching archive material + color (lookup only, not identity). */
fun matchingSpoolsForArchive(
    archive: PrintArchive,
    spools: List<SpoolInventoryItem>,
): List<SpoolInventoryItem> {
    val filter = buildArchiveSpoolLookupFilter(archive)
    val matches = spools.filter { spoolMatchesArchiveLookupFilter(it, filter) }
    logArchiveSpoolLookupCandidates(archive, filter, spools, matches)
    val printerId = archive.printerId ?: return matches
    return matches.sortedByDescending { it.assignment?.printerId == printerId }
}

fun resolveArchiveMaterialNavigation(
    archive: PrintArchive,
    spools: List<SpoolInventoryItem>,
): ArchiveMaterialNavigation {
    if (!archiveHasMaterialDisplay(archive)) return ArchiveMaterialNavigation.None

    val archiveMaterialRaw = archive.filamentType?.trim().orEmpty()
    val archiveColorRaw = archive.filamentColor?.trim().orEmpty()
    val normalizedMaterial = archiveMaterialRaw
        .takeIf { isMeaningfulArchiveField(it) }
        ?.let { normalizeArchiveMaterialKey(it) }
    val normalizedHexes = normalizedArchiveColorHexes(archive)

    val matches = matchingSpoolsForArchive(archive, spools)
    val lookupFilter = buildArchiveSpoolLookupFilter(archive, matches)

    if (DEBUG_LOG_SPOOL_ARCHIVE_LINK) {
        Log.d(
            TAG_SPOOL_ARCHIVE_LINK,
            "archiveMaterialLookup archiveId=${archive.id} " +
                "materialRaw='$archiveMaterialRaw' materialNorm='$normalizedMaterial' " +
                "colorRaw='$archiveColorRaw' colorNormHexes=${lookupFilter.colorHexes} " +
                "colorLabel='${lookupFilter.colorDisplayLabel}' " +
                "inventoryCount=${spools.size} matchCount=${matches.size} " +
                "matchIds=${matches.map { it.id }}",
        )
    }

    return when (matches.size) {
        1 -> ArchiveMaterialNavigation.SpoolDetail(matches.first().id)
        else -> ArchiveMaterialNavigation.SpoolsFiltered(lookupFilter)
    }
}

private fun materialsMatchForLookup(archiveType: String, spoolMaterial: String): Boolean =
    materialsMatchForLookupKey(
        normalizeArchiveMaterialKey(archiveType),
        spoolMaterial,
    )

private fun materialsMatchForLookupKey(materialKey: String, spoolMaterial: String): Boolean {
    val spoolKey = normalizeArchiveMaterialKey(
        normalizeFilamentType(spoolMaterial) ?: spoolMaterial,
    )
    if (materialKey == spoolKey) return true
    if (materialKey.length >= 3 && spoolKey.length >= 3) {
        if (materialKey.startsWith(spoolKey) || spoolKey.startsWith(materialKey)) return true
    }
    val archiveBase = materialKey.takeWhile { it.isLetterOrDigit() }
    val spoolBase = spoolKey.takeWhile { it.isLetterOrDigit() }
    if (archiveBase.length >= 3 && archiveBase == spoolBase) return true
    val archiveToken = materialKey.split('-', ' ', '_').firstOrNull().orEmpty()
    val spoolToken = spoolKey.split('-', ' ', '_').firstOrNull().orEmpty()
    return archiveToken.length >= 3 && archiveToken == spoolToken
}

private fun normalizedArchiveColorHexes(archive: PrintArchive): List<String> {
    val fromSwatch = parseArchiveFilamentSwatch(archive)?.colorHexes
        ?.mapNotNull { normalizeMatchColorHex(it) }
        .orEmpty()
    val fromRaw = archive.filamentColor?.trim()
        ?.takeIf { isMeaningfulArchiveField(it) && isHexColorField(it) }
        ?.let { listOfNotNull(normalizeMatchColorHex(it)) }
        .orEmpty()
    return (fromSwatch + fromRaw).distinct()
}

private fun resolveArchiveColorDisplayLabel(
    archive: PrintArchive,
    matches: List<SpoolInventoryItem>,
): String? {
    matches.firstNotNullOfOrNull { spool ->
        spool.colorName?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    }?.let { return it }
    val raw = archive.filamentColor?.trim()?.takeIf { isMeaningfulArchiveField(it) } ?: return null
    if (!isHexColorField(raw)) {
        return raw.removePrefix("#").trim()
    }
    normalizedArchiveColorHexes(archive).firstOrNull { isNearWhiteHex(it) }?.let { return "white" }
    return null
}

private fun isHexColorField(raw: String): Boolean {
    val body = raw.removePrefix("#").filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    return body.length in 3..8
}

private fun matchesArchiveLookupColor(
    spool: SpoolInventoryItem,
    filter: ArchiveSpoolLookupFilter,
): Boolean {
    val spoolHexes = normalizedSpoolColorHexes(spool)
    if (filter.colorHexes.isNotEmpty() && spoolHexes.isNotEmpty()) {
        if (colorHexesMatch(filter.colorHexes, spoolHexes)) return true
    }
    filter.colorDisplayLabel?.let { label ->
        if (colorNamesLooselyMatch(label, spool.colorName)) return true
        if (isWhiteColorLabel(label) && spoolHexes.any { isNearWhiteHex(it) }) return true
        if (isWhiteColorLabel(label) && spool.colorName?.let { isWhiteColorLabel(it) } == true) {
            return true
        }
    }
    if (filter.colorHexes.any { isNearWhiteHex(it) }) {
        if (spoolHexes.any { isNearWhiteHex(it) }) return true
        if (spool.colorName?.let { isWhiteColorLabel(it) } == true) return true
    }
    return false
}

private fun logArchiveSpoolLookupCandidates(
    archive: PrintArchive,
    filter: ArchiveSpoolLookupFilter,
    spools: List<SpoolInventoryItem>,
    matches: List<SpoolInventoryItem>,
) {
    if (!DEBUG_LOG_SPOOL_ARCHIVE_LINK) return
    spools.forEach { spool ->
        val matched = spoolMatchesArchiveLookupFilter(spool, filter)
        Log.d(
            TAG_SPOOL_ARCHIVE_LINK,
            "lookupCandidate archiveId=${archive.id} spoolId=${spool.id} " +
                "material=${spool.material} colorName=${spool.colorName} " +
                "hexes=${normalizedSpoolColorHexes(spool)} matched=$matched",
        )
    }
    Log.d(
        TAG_SPOOL_ARCHIVE_LINK,
        "lookupResult archiveId=${archive.id} matchCount=${matches.size} " +
            "ids=${matches.map { it.id }}",
    )
}

private fun isNearWhiteHex(hex: String): Boolean {
    val rgb = hexToRgb(expandShortHex(normalizeMatchColorHex(hex) ?: return false)) ?: return false
    return rgb.first >= 235 && rgb.second >= 235 && rgb.third >= 235
}

private fun isWhiteColorLabel(label: String): Boolean {
    val normalized = label.trim().lowercase()
    return normalized in setOf("white", "ivory", "snow", "natural white", "off white", "off-white")
}

private fun normalizedSpoolColorHexes(spool: SpoolInventoryItem): List<String> =
    spool.swatch.colorHexes.mapNotNull { normalizeMatchColorHex(it) }

private fun colorHexesMatch(archiveHexes: List<String>, spoolHexes: List<String>): Boolean =
    archiveHexes.any { archiveHex ->
        spoolHexes.any { spoolHex ->
            hexColorsEquivalent(archiveHex, spoolHex)
        }
    }

private fun hexColorsEquivalent(a: String, b: String): Boolean {
    val na = expandShortHex(normalizeMatchColorHex(a) ?: return false)
    val nb = expandShortHex(normalizeMatchColorHex(b) ?: return false)
    if (na.equals(nb, ignoreCase = true)) return true
    val ra = hexToRgb(na) ?: return false
    val rb = hexToRgb(nb) ?: return false
    val distance = sqrt(
        (ra.first - rb.first).toDouble().let { it * it } +
            (ra.second - rb.second).toDouble().let { it * it } +
            (ra.third - rb.third).toDouble().let { it * it },
    )
    return distance <= COLOR_APPROX_MAX_DISTANCE
}

private fun expandShortHex(hex: String): String {
    val body = hex.removePrefix("#").lowercase()
    if (body.length == 3) {
        return "#${body[0]}${body[0]}${body[1]}${body[1]}${body[2]}${body[2]}"
    }
    return if (hex.startsWith("#")) hex.lowercase() else "#$body"
}

private fun hexToRgb(hex: String): Triple<Int, Int, Int>? {
    val body = hex.removePrefix("#")
    if (body.length != 6) return null
    return try {
        Triple(
            body.substring(0, 2).toInt(16),
            body.substring(2, 4).toInt(16),
            body.substring(4, 6).toInt(16),
        )
    } catch (_: NumberFormatException) {
        null
    }
}

private fun colorNamesLooselyMatch(archiveColor: String?, spoolColorName: String?): Boolean {
    val archiveName = archiveColor?.trim()?.takeIf { isMeaningfulArchiveField(it) }
        ?.removePrefix("#")
        ?.lowercase()
    val spoolName = spoolColorName?.trim()?.takeIf { isMeaningfulSpoolField(it) }?.lowercase()
    if (archiveName == null || spoolName == null) return false
    return archiveName == spoolName ||
        archiveName.contains(spoolName) ||
        spoolName.contains(archiveName)
}

private fun logSpoolArchiveMatch(
    spool: SpoolInventoryItem,
    matches: List<PrintArchive>,
    isExact: Boolean,
) {
    if (!DEBUG_LOG_SPOOL_ARCHIVE_LINK) return
    Log.d(
        TAG_SPOOL_ARCHIVE_LINK,
        "spoolMatch spoolId=${spool.id} material=${spool.material} color=${spool.colorName} " +
            "hexes=${spool.swatch.colorHexes} exact=$isExact matched=${matches.size}",
    )
}
