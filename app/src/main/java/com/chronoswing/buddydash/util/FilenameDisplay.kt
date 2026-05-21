package com.chronoswing.buddydash.util

/**
 * Formats a raw print filename for on-screen display only. Does not change stored/API values.
 */
fun formatFilenameForDisplay(raw: String): String {
    if (raw.isBlank() || raw == "—") return raw
    return raw
        .replace('_', ' ')
        .replace(Regex("[\\s]+"), " ")
        .trim()
}
