package com.chronoswing.buddydash.util

fun normalizeTrayColor(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (trimmed.equals("null", ignoreCase = true)) return null
    val hex = trimmed.removePrefix("#")
    if (hex.all { it == '0' }) return null
    return trimmed
}

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
