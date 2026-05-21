package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import org.json.JSONObject

/** Temporary: spool ↔ archive linking diagnostics. Set false before release. */
const val DEBUG_LOG_SPOOL_ARCHIVE_LINK = true // set false before release

const val TAG_SPOOL_ARCHIVE_LINK = "BuddyDash/SpoolArchiveLink"

const val SPOOL_ARCHIVE_MATCH_PREVIEW_LIMIT = 5

data class SpoolArchiveMatches(
    val isExactSpoolId: Boolean,
    val archives: List<PrintArchive>,
)

sealed class ArchiveMaterialNavigation {
    data class SpoolDetail(val spoolId: Int) : ArchiveMaterialNavigation()
    data class SpoolsFiltered(val searchQuery: String) : ArchiveMaterialNavigation()
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
    val archiveType = formatArchiveDetailMaterialType(archive)?.lowercase() ?: return false
    val spoolMaterial = (normalizeFilamentType(spool.material) ?: spool.material).lowercase()
    val typeMatches = archiveType == spoolMaterial ||
        archiveType.contains(spoolMaterial) ||
        spoolMaterial.contains(archiveType)
    if (!typeMatches) return false

    val archiveSwatch = parseArchiveFilamentSwatch(archive)
    val archiveHexes = archiveSwatch?.colorHexes.orEmpty()
    val spoolHexes = spool.swatch.colorHexes
    if (archiveHexes.isNotEmpty() && spoolHexes.isNotEmpty()) {
        return archiveHexes.zip(spoolHexes).all { (a, b) ->
            a.equals(b, ignoreCase = true)
        } || archiveHexes.first().equals(spoolHexes.first(), ignoreCase = true)
    }
    val archiveColorName = archive.filamentColor?.trim()?.takeIf { isMeaningfulArchiveField(it) }
        ?.removePrefix("#")
    val spoolColor = spool.colorName?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    if (archiveColorName != null && spoolColor != null) {
        return archiveColorName.equals(spoolColor, ignoreCase = true) ||
            archiveColorName.contains(spoolColor, ignoreCase = true) ||
            spoolColor.contains(archiveColorName, ignoreCase = true)
    }
    return archiveHexes.isEmpty() && spoolHexes.isEmpty()
}

fun buildArchiveMaterialSearchQuery(archive: PrintArchive): String? {
    val parts = buildList {
        formatArchiveDetailMaterialType(archive)?.let { add(it) }
        archive.filamentColor?.trim()?.takeIf { isMeaningfulArchiveField(it) }?.let { add(it) }
        parseArchiveFilamentSwatch(archive)?.colorHexes?.forEach { add(it) }
    }
    return parts.joinToString(" ").trim().takeIf { it.isNotBlank() }
}

fun resolveArchiveMaterialNavigation(
    archive: PrintArchive,
    spools: List<SpoolInventoryItem>,
): ArchiveMaterialNavigation {
    archive.spoolId?.let { spoolId ->
        if (spools.any { it.id == spoolId }) {
            if (DEBUG_LOG_SPOOL_ARCHIVE_LINK) {
                Log.d(
                    TAG_SPOOL_ARCHIVE_LINK,
                    "archiveMaterialNav archiveId=${archive.id} exactSpoolId=$spoolId",
                )
            }
            return ArchiveMaterialNavigation.SpoolDetail(spoolId)
        }
    }
    val query = buildArchiveMaterialSearchQuery(archive) ?: return ArchiveMaterialNavigation.None
    val hasMatch = spools.any { archiveMatchesSpoolMaterial(it, archive) }
    if (!hasMatch) return ArchiveMaterialNavigation.None
    if (DEBUG_LOG_SPOOL_ARCHIVE_LINK) {
        Log.d(
            TAG_SPOOL_ARCHIVE_LINK,
            "archiveMaterialNav archiveId=${archive.id} materialFilter=$query " +
                "archiveType=${archive.filamentType} archiveColor=${archive.filamentColor}",
        )
    }
    return ArchiveMaterialNavigation.SpoolsFiltered(query)
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
