package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.data.model.SpoolSlotAssignment
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Non-empty text that is not the literal string "null". */
fun isMeaningfulSpoolField(value: String?): Boolean {
    val trimmed = value?.trim() ?: return false
    return trimmed.isNotBlank() && !trimmed.equals("null", ignoreCase = true)
}

fun spoolJsonOptionalString(json: JSONObject, key: String): String? =
    json.optString(key).trim().takeIf { isMeaningfulSpoolField(it) }

/** Primary list/detail title: brand + material + color name (no null literals). */
fun formatSpoolCardTitle(spool: SpoolInventoryItem): String {
    val parts = buildList {
        spool.brand?.let { add(it) }
        val material = normalizeFilamentType(spool.material)?.uppercase() ?: spool.material
        add(material)
        spool.colorName?.let { add(it) }
    }.filter { isMeaningfulSpoolField(it) }
    return parts.joinToString(" ").ifBlank { "Spool #${spool.id}" }
}

fun formatSpoolMaterialSubtitle(spool: SpoolInventoryItem): String? {
    val material = normalizeFilamentType(spool.material)?.uppercase() ?: spool.material
    val subtype = spool.subtype?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    return when {
        subtype != null -> "$material · $subtype"
        isMeaningfulSpoolField(material) -> material
        else -> null
    }
}

/** Loaded • Printer • Slot, or Storage. */
fun formatSpoolLocationLine(spool: SpoolInventoryItem): String =
    spool.assignment?.let { formatSpoolAssignmentLine(it) } ?: "Storage"

/** Compact Printer • Slot (or Storage) for inventory list and assignment picker cards. */
fun formatSpoolInventoryCardLocationLine(spool: SpoolInventoryItem): String =
    spool.assignment?.let { formatSpoolAssignmentLocationBrief(it) } ?: "Storage"

fun formatSpoolAssignmentLine(assignment: SpoolSlotAssignment): String {
    val slot = assignment.slotLabel.trim().takeIf { isMeaningfulSpoolField(it) }
    return if (slot != null) {
        "Loaded • ${assignment.printerName} • $slot"
    } else {
        "Loaded • ${assignment.printerName}"
    }
}

fun formatSpoolRemainingGrams(spool: SpoolInventoryItem): String? {
    val remaining = spool.remainingGrams ?: return null
    if (remaining <= 0.0) return null
    return formatFilamentWeightGrams(remaining)
}

fun formatSpoolLabelWeight(spool: SpoolInventoryItem): String? =
    spool.labelWeightGrams?.takeIf { it > 0 }?.let { formatFilamentWeightGrams(it.toDouble()) }

fun formatSpoolTagIndicator(tagType: String?, dataOrigin: String?): String? {
    val tag = tagType?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    val origin = dataOrigin?.trim()?.takeIf { isMeaningfulSpoolField(it) }
    return when {
        tag != null && origin != null -> "$tag · $origin"
        tag != null -> tag
        origin != null -> origin
        else -> null
    }
}

fun formatSpoolLastUsed(isoTimestamp: String?): String? {
    val instant = parseSpoolIsoInstant(isoTimestamp) ?: return null
    return DateTimeFormatter.ofPattern("MMM d, yyyy")
        .withZone(java.time.ZoneId.systemDefault())
        .format(instant)
}

private fun parseSpoolIsoInstant(iso: String?): Instant? {
    if (!isMeaningfulSpoolField(iso)) return null
    return try {
        Instant.parse(iso)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun computeSpoolRemainingGrams(labelWeightGrams: Int?, weightUsedGrams: Double?): Double? {
    if (labelWeightGrams == null || labelWeightGrams <= 0) return null
    if (weightUsedGrams == null) return null
    return (labelWeightGrams - weightUsedGrams).coerceAtLeast(0.0)
}
