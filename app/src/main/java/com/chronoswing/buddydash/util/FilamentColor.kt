package com.chronoswing.buddydash.util

import androidx.compose.ui.graphics.Color

fun parseFilamentColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val cleaned = hex.removePrefix("#").filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    return try {
        when (cleaned.length) {
            6 -> Color(0xFF000000L or cleaned.toLong(16))
            8 -> {
                // Bambu often sends AARRGGBB or RRGGBBAA; use last 6 as RGB for the dot
                Color(0xFF000000L or cleaned.takeLast(6).toLong(16))
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
