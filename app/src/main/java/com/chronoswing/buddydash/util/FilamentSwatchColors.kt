package com.chronoswing.buddydash.util

import org.json.JSONObject

/** Display colors for a filament swatch (inventory or tray fallback). */
data class FilamentSwatchColors(
    val colorHexes: List<String>,
    val isTranslucent: Boolean = false,
    val alpha: Float = 1f,
) {
    val isMultiColor: Boolean
        get() = colorHexes.size > 1

    companion object {
        fun fromTrayColor(trayColor: String?): FilamentSwatchColors {
            val primary = normalizeTrayColor(trayColor)
            val alpha = parseRgbaAlpha(trayColor)
            return FilamentSwatchColors(
                colorHexes = listOfNotNull(primary),
                isTranslucent = isTranslucentEffect(null, alpha),
                alpha = alpha,
            )
        }
    }
}

/** Full opacity for 6-digit RGB; reads AA from 8-digit RRGGBBAA only. */
fun parseRgbaAlpha(raw: String?): Float {
    val hex = raw?.trim()?.removePrefix("#")
        ?.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        ?: return 1f
    if (hex.length < 8) return 1f
    return try {
        (hex.substring(6, 8).toInt(16) / 255f).coerceIn(0f, 1f)
    } catch (_: Exception) {
        1f
    }
}

fun isTranslucentEffect(effectType: String?, alpha: Float): Boolean {
    if (effectType.equals("translucent", ignoreCase = true)) return true
    if (effectType.equals("transparent", ignoreCase = true)) return true
    return alpha < 255f / 255f - 1e-3f
}

fun parseExtraColorStops(extraColors: String?): List<String> {
    if (extraColors.isNullOrBlank()) return emptyList()
    return extraColors
        .split(',')
        .mapNotNull { normalizeInventoryColor(it.trim()) }
}

fun mergeFilamentColorHexes(primary: String?, extraStops: List<String>): List<String> =
    buildList {
        primary?.let { add(it) }
        for (stop in extraStops) {
            if (stop !in this) add(stop)
        }
    }

fun parseFilamentSwatchFromSpool(spool: JSONObject): FilamentSwatchColors {
    val rgbaRaw = spool.optString("rgba")
    val primary = normalizeInventoryColor(rgbaRaw)
    val stops = parseExtraColorStops(spool.optString("extra_colors"))
    val alpha = parseRgbaAlpha(rgbaRaw)
    val effectType = spool.optString("effect_type").takeIf { it.isNotBlank() }
    return FilamentSwatchColors(
        colorHexes = mergeFilamentColorHexes(primary, stops),
        isTranslucent = isTranslucentEffect(effectType, alpha),
        alpha = alpha,
    )
}
