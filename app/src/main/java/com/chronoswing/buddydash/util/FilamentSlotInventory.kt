package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.SpoolInventoryItem

/** How a live tray row links to inventory (for navigation and labels). */
enum class FilamentSpoolMatchKind {
    /** GET /inventory/assignments spool id for this AMS/tray key. */
    Assignment,
    /** Unique material+color match among spools assigned to this printer. */
    Heuristic,
    None,
}

data class FilamentSlotDisplay(
    val slot: FilamentSlot,
    val spoolId: Int?,
    val spool: SpoolInventoryItem?,
    val matchKind: FilamentSpoolMatchKind,
    val primaryTitle: String,
    val subtitle: String?,
    /** Bambuddy tray_id query for POST …/ams/load (ams_id*4+slot_id or 254 external). */
    val trayGlobalId: Int,
    val isActive: Boolean,
    val isTappable: Boolean,
)

/** Encode tray id for POST /api/v1/printers/{id}/ams/load per Bambuddy convention. */
fun encodeBambuddyTrayId(slot: FilamentSlot): Int? {
    val amsId = slot.amsId ?: return null
    val trayId = slot.trayId ?: return null
    if (slot.isExternal || amsId == EXTERNAL_AMS_ID) return 254
    return amsId * 4 + trayId
}

fun buildFilamentSlotDisplays(
    slots: List<FilamentSlot>,
    activeKey: SlotInventoryKey?,
    printerId: Int,
    inventoryBySlot: Map<SlotInventoryKey, SlotInventoryInfo>,
    spoolsById: Map<Int, SpoolInventoryItem>,
    spoolsAssignedToPrinter: List<SpoolInventoryItem>,
): List<FilamentSlotDisplay> =
    slots.map { slot ->
        val key = slot.inventoryKey
        val inventory = key?.let { inventoryBySlot[it] }
        val assignmentSpoolId = slot.inventorySpoolId
            ?: inventory?.spoolId?.takeIf { it >= 0 }
        var matchKind = FilamentSpoolMatchKind.None
        var spool: SpoolInventoryItem? = null

        if (assignmentSpoolId != null) {
            spool = spoolsById[assignmentSpoolId]
            if (spool != null) {
                matchKind = FilamentSpoolMatchKind.Assignment
            }
        }

        if (spool == null && slot.isLoaded) {
            val heuristic = findUniqueHeuristicSpoolMatch(slot, spoolsAssignedToPrinter)
            if (heuristic != null) {
                spool = heuristic
                matchKind = FilamentSpoolMatchKind.Heuristic
            }
        }

        val (primaryTitle, subtitle) = formatFilamentSlotTitles(slot, spool, inventory)
        val trayGlobalId = encodeBambuddyTrayId(slot) ?: -1
        val isActive = slot.isActiveSlot(activeKey)
        val isTappable = spool != null && matchKind != FilamentSpoolMatchKind.None

        FilamentSlotDisplay(
            slot = slot,
            spoolId = spool?.id,
            spool = spool,
            matchKind = matchKind,
            primaryTitle = primaryTitle,
            subtitle = subtitle,
            trayGlobalId = trayGlobalId,
            isActive = isActive,
            isTappable = isTappable,
        )
    }

private fun formatFilamentSlotTitles(
    slot: FilamentSlot,
    spool: SpoolInventoryItem?,
    inventory: SlotInventoryInfo?,
): Pair<String, String?> {
    if (spool != null) {
        return formatSpoolCardTitle(spool) to formatSpoolMaterialSubtitle(spool)
    }
    inventory?.spoolName?.takeIf { isMeaningfulSpoolField(it) }?.let { name ->
        val type = normalizeFilamentType(slot.filamentType)?.uppercase()
        val subtitle = when {
            type != null && inventory.spoolName.contains(type, ignoreCase = true) -> null
            type != null -> type
            else -> null
        }
        return name to subtitle
    }
    val type = normalizeFilamentType(slot.filamentType)?.uppercase()
    return (type ?: slot.metadata ?: "—") to null
}

/**
 * Fallback match only when exactly one assigned spool matches material and color on this printer.
 */
private fun findUniqueHeuristicSpoolMatch(
    slot: FilamentSlot,
    assignedSpools: List<SpoolInventoryItem>,
): SpoolInventoryItem? {
    val material = normalizeFilamentType(slot.filamentType) ?: return null
    val slotHexes = slot.swatchColorHexes.mapNotNull { normalizeMatchColorHex(it) }
    if (slotHexes.isEmpty()) return null

    val matches = assignedSpools.filter { spool ->
        materialsMatchHeuristic(material, spool.material) &&
            spoolColorMatchesSlot(spool, slotHexes)
    }
    return matches.singleOrNull()
}

private fun materialsMatchHeuristic(slotMaterial: String, spoolMaterial: String): Boolean {
    val slotKey = normalizeArchiveMaterialKey(slotMaterial)
    val spoolKey = normalizeArchiveMaterialKey(spoolMaterial)
    return slotKey == spoolKey
}

private fun spoolColorMatchesSlot(
    spool: SpoolInventoryItem,
    slotHexes: List<String>,
): Boolean {
    val spoolHexes = spool.swatch.colorHexes.mapNotNull { normalizeMatchColorHex(it) }
    if (spoolHexes.isEmpty() || slotHexes.isEmpty()) return false
    return slotHexes.any { slotHex ->
        spoolHexes.any { spoolHex ->
            colorsApproxMatch(spoolHex, slotHex)
        }
    }
}

private fun colorsApproxMatch(a: String, b: String): Boolean {
    val rgbA = parseHexRgb(a) ?: return false
    val rgbB = parseHexRgb(b) ?: return false
    val dr = rgbA.first - rgbB.first
    val dg = rgbA.second - rgbB.second
    val db = rgbA.third - rgbB.third
    return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble()) <= COLOR_APPROX_MAX_DISTANCE
}

private fun parseHexRgb(hex: String): Triple<Int, Int, Int>? {
    val normalized = hex.removePrefix("#")
    if (normalized.length != 6) return null
    return try {
        Triple(
            normalized.substring(0, 2).toInt(16),
            normalized.substring(2, 4).toInt(16),
            normalized.substring(4, 6).toInt(16),
        )
    } catch (_: NumberFormatException) {
        null
    }
}

private const val COLOR_APPROX_MAX_DISTANCE = 85.0
