package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentUsage
import org.json.JSONObject
import kotlin.math.roundToInt

/** Temporary: log filament usage field discovery. Set false before release. */
val DEBUG_LOG_FILAMENT_USAGE: Boolean get() = BuddyDashDebug.enabled

const val TAG_FILAMENT_USAGE = "BuddyDash/FilamentUsage"

/**
 * OpenAPI-backed keys (PrintQueueItemResponse, ArchiveResponse, FileListResponse, PrintLogEntrySchema).
 * Additional keys are scanned for discovery only when present in JSON.
 */
val FILAMENT_USAGE_WEIGHT_GRAMS_KEYS = listOf(
    "filament_used_grams",
    "filament_weight",
    "filament_weight_g",
    "weight_g",
    "filament_g",
    "grams",
    "filament_used",
    "total_filament_grams",
)

val FILAMENT_USAGE_LENGTH_METERS_KEYS = listOf(
    "filament_length",
    "filament_length_m",
    "filament_used_m",
    "length_m",
    "meters",
)

fun filamentUsageWeightFieldCandidates(json: JSONObject): Map<String, String?> =
    FILAMENT_USAGE_WEIGHT_GRAMS_KEYS.associateWith { key ->
        jsonPositiveNumberString(json, key)
    }

fun filamentUsageLengthFieldCandidates(json: JSONObject): Map<String, String?> =
    FILAMENT_USAGE_LENGTH_METERS_KEYS.associateWith { key ->
        jsonPositiveNumberString(json, key)
    }

/** Keys in [json] that look filament/usage-related (debug discovery). */
fun discoverFilamentUsageKeys(json: JSONObject): List<String> {
    val names = json.names() ?: return emptyList()
    val patterns = listOf("filament", "weight", "length", "gram", "meter", "usage", "spool")
    return buildList {
        for (i in 0 until names.length()) {
            val key = names.getString(i)
            val lower = key.lowercase()
            if (patterns.any { lower.contains(it) }) add(key)
        }
    }.sorted()
}

fun resolveFilamentUsageFromJson(json: JSONObject): FilamentUsage? {
    val weight = resolveWeightGrams(json)
    val length = resolveLengthMeters(json)
    if (weight == null && length == null) return null
    return FilamentUsage(weightGrams = weight, lengthMeters = length)
}

private fun resolveWeightGrams(json: JSONObject): Double? {
    for (key in FILAMENT_USAGE_WEIGHT_GRAMS_KEYS) {
        jsonPositiveDouble(json, key)?.let { return it }
    }
    return null
}

private fun resolveLengthMeters(json: JSONObject): Double? {
    for (key in FILAMENT_USAGE_LENGTH_METERS_KEYS) {
        jsonPositiveDouble(json, key)?.let { return it }
    }
    return null
}

private fun jsonPositiveDouble(json: JSONObject, key: String): Double? {
    if (!json.has(key) || json.isNull(key)) return null
    return when (val value = json.opt(key)) {
        is Number -> value.toDouble().takeIf { it > 0.0 }
        is String -> value.toDoubleOrNull()?.takeIf { it > 0.0 }
        else -> null
    }
}

private fun jsonPositiveNumberString(json: JSONObject, key: String): String? =
    jsonPositiveDouble(json, key)?.let { formatRawNumber(it) }

private fun formatRawNumber(value: Double): String =
    if (value == value.roundToInt().toDouble()) {
        value.roundToInt().toString()
    } else {
        value.toString()
    }

/** Compact display, e.g. "🧵 86g • 28.4m" or "🧵 86g". Returns null when empty. */
fun formatFilamentUsageCompact(usage: FilamentUsage?): String? {
    val weight = usage?.weightGrams?.let { formatFilamentWeightGrams(it) }
    val length = usage?.lengthMeters?.let { formatFilamentLengthMeters(it) }
    return when {
        weight != null && length != null -> "🧵 $weight • $length"
        weight != null -> "🧵 $weight"
        length != null -> "🧵 $length"
        else -> null
    }
}

/** Archives / queue / detail — integer grams (86g, not 86.000g). */
fun formatFilamentWeightGrams(grams: Double): String {
    val rounded = grams.roundToInt().coerceAtLeast(1)
    return "${rounded}g"
}

/** Meters with at most one decimal when under 100m (2.4m, 28.1m). */
fun formatFilamentLengthMeters(meters: Double): String {
    val text = if (meters >= 100) {
        meters.roundToInt().toString()
    } else {
        val tenths = (meters * 10).roundToInt() / 10.0
        if (tenths == tenths.roundToInt().toDouble()) {
            tenths.roundToInt().toString()
        } else {
            tenths.toString()
        }
    }
    return "${text}m"
}

fun logFilamentUsageDiscovery(
    tag: String,
    context: String,
    json: JSONObject,
    resolved: FilamentUsage?,
) {
    if (!DEBUG_LOG_FILAMENT_USAGE) return
    val discovered = discoverFilamentUsageKeys(json)
    android.util.Log.d(
        tag,
        "$context discoveredKeys=$discovered " +
            "weightCandidates=${filamentUsageWeightFieldCandidates(json)} " +
            "lengthCandidates=${filamentUsageLengthFieldCandidates(json)} " +
            "resolvedWeight=${resolved?.weightGrams} resolvedLength=${resolved?.lengthMeters} " +
            "display=${formatFilamentUsageCompact(resolved)}",
    )
}
