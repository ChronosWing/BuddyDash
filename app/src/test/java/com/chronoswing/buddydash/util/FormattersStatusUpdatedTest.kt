package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersStatusUpdatedTest {

    @Test
    fun formatStatusUpdatedAgo_nullWhenNeverUpdated() {
        assertNull(formatStatusUpdatedAgo(null))
    }

    @Test
    fun formatStatusUpdatedAgo_relativeLabels() {
        val now = 1_000_000L
        assertEquals("Just now", formatStatusUpdatedAgo(now - 2_000L, now))
        assertEquals("4s ago", formatStatusUpdatedAgo(now - 4_000L, now))
        assertEquals("12s ago", formatStatusUpdatedAgo(now - 12_000L, now))
        assertEquals("1m ago", formatStatusUpdatedAgo(now - 60_000L, now))
    }

    @Test
    fun resolveStatusRefreshFreshness_tiers() {
        val now = 100_000L
        assertEquals(
            StatusRefreshFreshness.Live,
            resolveStatusRefreshFreshness(now - 5_000L, now),
        )
        assertEquals(
            StatusRefreshFreshness.Aging,
            resolveStatusRefreshFreshness(now - 15_000L, now),
        )
        assertEquals(
            StatusRefreshFreshness.Stale,
            resolveStatusRefreshFreshness(now - 45_000L, now),
        )
        assertEquals(
            StatusRefreshFreshness.ConnectionStale,
            resolveStatusRefreshFreshness(now - 90_000L, now),
        )
    }
}
