package com.chronoswing.buddydash.util

import android.os.SystemClock
import android.util.Log

private const val TAG = "BuddyDash/HomeLoad"

/** Debug timing markers for Home printer list startup (debug builds only). */
object HomeLoadTiming {
    private val sessionStartMs = SystemClock.elapsedRealtime()

    fun log(event: String) {
        if (!BuddyDashDebug.enabled) return
        val elapsed = SystemClock.elapsedRealtime() - sessionStartMs
        Log.d(TAG, "$event at ${elapsed}ms")
    }
}
