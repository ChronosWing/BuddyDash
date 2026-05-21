package com.chronoswing.buddydash.util

/** Bambu / Bambuddy colors are RRGGBB or RRGGBBAA; use the first six hex digits as RGB. */
fun normalizeTrayColor(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (trimmed.equals("null", ignoreCase = true)) return null
    val hex = trimmed.removePrefix("#")
    val rgb = when {
        hex.length >= 8 -> hex.substring(0, 6)
        hex.length >= 6 -> hex.substring(0, 6)
        else -> return null
    }
    if (rgb.all { it == '0' }) return null
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
