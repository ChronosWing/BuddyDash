package com.chronoswing.buddydash.util

/**
 * Bambuddy filament colors: 6 hex digits = opaque RRGGBB (alpha FF), 8 = RRGGBBAA.
 * Returns #RRGGBB for display; alpha/translucency is handled separately via [parseRgbaAlpha].
 */
fun normalizeTrayColor(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (trimmed.equals("null", ignoreCase = true)) return null
    val hex = trimmed.removePrefix("#").filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    val rgb = when {
        hex.length >= 8 -> hex.substring(0, 6)
        hex.length == 6 -> hex
        else -> return null
    }
    return "#$rgb"
}

/** Spool inventory [rgba] field (same RRGGBBAA layout as Bambuddy catalog). */
fun normalizeInventoryColor(rgba: String?): String? = normalizeTrayColor(rgba)

fun isTrayLoaded(
    filamentType: String?,
    colorHex: String?,
    remainPercent: Int?,
    trayIdName: String? = null,
): Boolean {
    if (filamentType != null) return true
    if (colorHex != null) return true
    if (remainPercent != null) return true
    if (trayIdName != null && trayIdName.isNotBlank() && !trayIdName.equals("null", ignoreCase = true)) {
        return true
    }
    return false
}
