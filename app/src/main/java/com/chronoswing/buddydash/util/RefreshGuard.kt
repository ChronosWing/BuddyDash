package com.chronoswing.buddydash.util

/** Debounces rapid manual refresh taps without affecting passive polling. */
class RefreshGuard(private val minIntervalMs: Long = 800L) {
    private var lastRequestAt = 0L

    fun shouldSkipManualRefresh(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRequestAt < minIntervalMs) return true
        lastRequestAt = now
        return false
    }
}
