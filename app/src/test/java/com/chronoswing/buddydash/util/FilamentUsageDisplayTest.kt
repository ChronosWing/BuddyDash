package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.FilamentUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class FilamentUsageDisplayTest {

    @Test
    fun formatFilamentWeightGrams_roundsToInteger() {
        assertEquals("86g", formatFilamentWeightGrams(86.2))
        assertEquals("12g", formatFilamentWeightGrams(12.0))
        assertEquals("185g", formatFilamentWeightGrams(184.7))
    }

    @Test
    fun formatFilamentLengthMeters_oneDecimalWhenNeeded() {
        assertEquals("2.4m", formatFilamentLengthMeters(2.44))
        assertEquals("28.1m", formatFilamentLengthMeters(28.12))
        assertEquals("120m", formatFilamentLengthMeters(120.4))
    }

    @Test
    fun formatFilamentUsageCompact_weightAndLength() {
        val line = formatFilamentUsageCompact(
            FilamentUsage(weightGrams = 86.0, lengthMeters = 28.4),
        )
        assertEquals("🧵 86g • 28.4m", line)
    }

    @Test
    fun formatFilamentUsageCompact_weightOnly() {
        assertEquals("🧵 12g", formatFilamentUsageCompact(FilamentUsage(weightGrams = 12.0)))
    }

    @Test
    fun formatQueueDurationAndFilament_combinesWhenBothPresent() {
        val line = formatQueueDurationAndFilament(
            durationSeconds = 44 * 60,
            usage = FilamentUsage(weightGrams = 12.0),
        )
        assertEquals("44m • 🧵 12g", line)
    }
}
