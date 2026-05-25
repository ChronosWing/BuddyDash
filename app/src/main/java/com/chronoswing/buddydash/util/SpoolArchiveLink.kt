package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import kotlin.math.sqrt
import org.json.JSONObject

/** Debug-only: spool ↔ archive linking diagnostics. */
val DEBUG_LOG_SPOOL_ARCHIVE_LINK: Boolean get() = BuddyDashDebug.enabled

const val TAG_SPOOL_ARCHIVE_LINK = "BuddyDash/SpoolArchiveLink"

const val SPOOL_ARCHIVE_MATCH_PREVIEW_LIMIT = 5

/** Max RGB distance (0–441) to treat two hex colors as near-equivalent. */
private const val COLOR_APPROX_MAX_DISTANCE = 85.0

private data class ColorFamily(
    val name: String,
    val keywords: Set<String>,
    val referenceRgb: Triple<Int, Int, Int>,
    val maxDistance: Double = COLOR_APPROX_MAX_DISTANCE,
)

private val COLOR_FAMILIES = listOf(
    ColorFamily("white", setOf("white", "ivory", "snow", "natural", "off-white", "off white"), Triple(245, 245, 245), 55.0),
    ColorFamily("black", setOf("black", "charcoal", "onyx", "ebony"), Triple(20, 20, 20), 55.0),
    ColorFamily("gray", setOf("gray", "grey", "silver", "slate", "ash"), Triple(128, 128, 128), 65.0),
    ColorFamily("red", setOf("red", "crimson", "scarlet", "maroon", "cherry", "ruby"), Triple(220, 40, 40)),
    ColorFamily("orange", setOf("orange", "copper", "amber", "peach"), Triple(230, 120, 40)),
    ColorFamily("yellow", setOf("yellow", "gold", "lemon"), Triple(240, 210, 50)),
    ColorFamily("green", setOf("green", "lime", "olive", "mint", "forest"), Triple(50, 160, 70)),
    ColorFamily("blue", setOf("blue", "navy", "azure", "cyan", "teal", "cobalt"), Triple(40, 90, 200)),
    ColorFamily("purple", setOf("purple", "violet", "magenta", "pink", "lavender", "lilac"), Triple(140, 60, 180)),
    ColorFamily("brown", setOf("brown", "tan", "beige", "wood", "bronze"), Triple(140, 90, 50)),
)

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
    if (archiveHexes.isEmpty()) {
        val raw = archive.filamentColor?.trim()?.takeIf { isMeaningfulArchiveField(it) }
        return if (raw != null && !isHexColorField(raw)) {
            colorNamesLooselyMatch(raw, spool.colorName) ||
                spoolMatchesColorSearchTerm(spool, raw.lowercase())
        } else {
            true
        }
    }

    if (spoolHexes.isNotEmpty() && colorHexesMatch(archiveHexes, spoolHexes)) return true
    if (spoolHexes.isNotEmpty() && colorHexesMatchByFamily(archiveHexes, spoolHexes)) return true
    val colorFilter = ArchiveSpoolLookupFilter(
        materialLabel = "",
        materialKey = "",
        colorHexes = archiveHexes,
        colorDisplayLabel = resolveArchiveColorDisplayLabel(archive, emptyList()),
    )
    return matchesArchiveLookupColor(spool, colorFilter)
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

    val navigation = when (matches.size) {
        1 -> ArchiveMaterialNavigation.SpoolDetail(matches.first().id)
        else -> ArchiveMaterialNavigation.SpoolsFiltered(lookupFilter)
    }
    if (DEBUG_LOG_SPOOL_ARCHIVE_LINK) {
        val route = when (navigation) {
            is ArchiveMaterialNavigation.SpoolDetail ->
                "SpoolDetail spoolId=${navigation.spoolId}"
            is ArchiveMaterialNavigation.SpoolsFiltered -> "SpoolsFilteredTopLevel"
            ArchiveMaterialNavigation.None -> "None"
        }
        Log.d(
            TAG_SPOOL_ARCHIVE_LINK,
            "archiveMaterialNavigation archiveId=${archive.id} matchCount=${matches.size} route=$route",
        )
    }
    return navigation
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
    return normalizedArchiveColorHexes(archive)
        .firstNotNullOfOrNull { deriveColorFamilyName(it) }
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
        if (colorHexesMatchByFamily(filter.colorHexes, spoolHexes)) return true
    }
    for (term in archiveLookupColorSearchTerms(filter)) {
        if (spoolMatchesColorSearchTerm(spool, term)) return true
    }
    return false
}

/** Display + matching terms derived from archive filter (hex → family name, label). */
private fun archiveLookupColorSearchTerms(filter: ArchiveSpoolLookupFilter): List<String> =
    buildList {
        filter.colorDisplayLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        filter.colorHexes.forEach { hex ->
            deriveColorFamilyName(hex)?.let { add(it) }
        }
    }.map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .distinct()

private fun spoolMatchesColorSearchTerm(spool: SpoolInventoryItem, term: String): Boolean {
    val normalizedTerm = term.trim().lowercase()
    if (normalizedTerm.isBlank()) return false
    if (colorNamesLooselyMatch(normalizedTerm, spool.colorName)) return true
    if (spoolTextContainsColorToken(spool, normalizedTerm)) return true
    for (hex in normalizedSpoolColorHexes(spool)) {
        if (deriveColorFamilyName(hex) == normalizedTerm) return true
        if (hexMatchesColorFamily(hex, normalizedTerm)) return true
    }
    if (isWhiteColorLabel(normalizedTerm)) {
        if (normalizedSpoolColorHexes(spool).any { isNearWhiteHex(it) }) return true
        if (spool.colorName?.let { isWhiteColorLabel(it) } == true) return true
    }
    return false
}

private fun spoolTextContainsColorToken(spool: SpoolInventoryItem, term: String): Boolean {
    val haystacks = listOfNotNull(
        spool.colorName,
        spool.displayName,
        formatSpoolCardTitle(spool),
        spool.brand,
    )
    return haystacks.any { text -> textContainsColorWord(text, term) }
}

private fun textContainsColorWord(text: String, term: String): Boolean {
    val words = text.lowercase()
        .replace('_', ' ')
        .split(Regex("""[\s\-.,]+"""))
        .filter { it.isNotBlank() }
    return words.any { word ->
        word == term || word.startsWith(term) && term.length >= 3
    }
}

private fun colorHexesMatchByFamily(archiveHexes: List<String>, spoolHexes: List<String>): Boolean {
    val archiveFamilies = archiveHexes.mapNotNull { deriveColorFamilyName(it) }.toSet()
    if (archiveFamilies.isEmpty()) return false
    return spoolHexes.any { spoolHex ->
        val spoolFamily = deriveColorFamilyName(spoolHex) ?: return@any false
        spoolFamily in archiveFamilies
    }
}

/** Map #rrggbb to a stable color family (red, blue, white, …) for labels and fallback matching. */
fun deriveColorFamilyName(hex: String): String? {
    val rgb = hexToRgb(expandShortHex(normalizeMatchColorHex(hex) ?: return null)) ?: return null
    if (isNearWhiteRgb(rgb)) return "white"
    if (isNearBlackRgb(rgb)) return "black"
    val (r, g, b) = rgb
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (max < 40) return "black"
    if (delta < 18 && max < 200) return "gray"
    val hue = rgbHueDegrees(r, g, b)
    return when {
        hue < 18 || hue >= 342 -> "red"
        hue < 45 -> "orange"
        hue < 68 -> "yellow"
        hue < 165 -> "green"
        hue < 200 -> "cyan"
        hue < 250 -> "blue"
        hue < 290 -> "purple"
        hue < 342 -> "pink"
        else -> "red"
    }
}

private fun hexMatchesColorFamily(hex: String, familyName: String): Boolean =
    deriveColorFamilyName(hex)?.equals(familyName, ignoreCase = true) == true

private fun rgbHueDegrees(r: Int, g: Int, b: Int): Int {
    val rf = r / 255.0
    val gf = g / 255.0
    val bf = b / 255.0
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min
    if (delta < 0.001) return 0
    val hue = when (max) {
        rf -> ((gf - bf) / delta + (if (gf < bf) 6 else 0)) * 60
        gf -> ((bf - rf) / delta + 2) * 60
        else -> ((rf - gf) / delta + 4) * 60
    }
    return hue.toInt().mod(360)
}

private fun isNearWhiteRgb(rgb: Triple<Int, Int, Int>): Boolean =
    rgb.first >= 235 && rgb.second >= 235 && rgb.third >= 235

private fun isNearBlackRgb(rgb: Triple<Int, Int, Int>): Boolean =
    rgb.first <= 45 && rgb.second <= 45 && rgb.third <= 45

private fun logArchiveSpoolLookupCandidates(
    archive: PrintArchive,
    filter: ArchiveSpoolLookupFilter,
    spools: List<SpoolInventoryItem>,
    matches: List<SpoolInventoryItem>,
) {
    if (!DEBUG_LOG_SPOOL_ARCHIVE_LINK) return
    val archiveMaterial = archive.filamentType?.trim().orEmpty()
    val archiveColorRaw = archive.filamentColor?.trim().orEmpty()
    val archiveNormHexes = filter.colorHexes
    val archiveDerivedNames = archiveLookupColorSearchTerms(filter)
    Log.d(
        TAG_SPOOL_ARCHIVE_LINK,
        "lookupArchiveFilter archiveId=${archive.id} " +
            "material='$archiveMaterial' materialKey='${filter.materialKey}' " +
            "colorRaw='$archiveColorRaw' colorNormHexes=$archiveNormHexes " +
            "colorDerivedNames=$archiveDerivedNames colorLabel='${filter.colorDisplayLabel}'",
    )
    spools.forEach { spool ->
        val materialMatch = filter.materialKey.isNotBlank() &&
            materialsMatchForLookupKey(filter.materialKey, spool.material)
        val colorMatch = matchesArchiveLookupColor(spool, filter)
        val included = spoolMatchesArchiveLookupFilter(spool, filter)
        val spoolNormHexes = normalizedSpoolColorHexes(spool)
        val spoolDerivedNames = spoolNormHexes.mapNotNull { deriveColorFamilyName(it) }
        Log.d(
            TAG_SPOOL_ARCHIVE_LINK,
            "lookupSpoolCandidate archiveId=${archive.id} spoolId=${spool.id} " +
                "spoolName='${spool.displayName}' spoolMaterial='${spool.material}' " +
                "spoolColorRaw='${spool.colorName}' spoolColorNormHexes=$spoolNormHexes " +
                "spoolDerivedColorNames=$spoolDerivedNames " +
                "materialMatch=$materialMatch colorMatch=$colorMatch included=$included",
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
    val archiveName = archiveColor?.trim()?.takeIf { isMeaningfulSpoolField(it) }
        ?.removePrefix("#")
        ?.lowercase()
    val spoolName = spoolColorName?.trim()?.takeIf { isMeaningfulSpoolField(it) }?.lowercase()
    if (archiveName == null || spoolName == null) return false
    if (archiveName == spoolName) return true
    if (archiveName.contains(spoolName) || spoolName.contains(archiveName)) return true
    val archiveFamily = COLOR_FAMILIES.find { archiveName in it.keywords || it.name == archiveName }
    val spoolFamily = COLOR_FAMILIES.find { spoolName in it.keywords || it.name == spoolName }
    return archiveFamily != null && archiveFamily.name == spoolFamily?.name
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
