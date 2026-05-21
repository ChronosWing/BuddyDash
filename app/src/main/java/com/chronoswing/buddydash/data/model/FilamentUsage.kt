package com.chronoswing.buddydash.data.model

/**
 * Filament consumed or estimated for a print.
 * Reusable for queue, live status, archives (filament_used_grams), and print history.
 */
data class FilamentUsage(
    val weightGrams: Double? = null,
    val lengthMeters: Double? = null,
)
