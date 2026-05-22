package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.util.BuddyDashDebug

enum class RefreshSource {
    APP_RESUME,
    RETURN_FROM_DETAIL,
    BOTTOM_NAV_RESELECT,
    MANUAL,
    POLL,
}

object RefreshIntervals {
    const val HOME_MS = 15_000L
    const val SPOOLS_MS = 30_000L
    const val ARCHIVES_MS = 60_000L
}

/** True when data is older than [intervalMs] or never loaded. */
fun isDataStale(
    lastUpdatedAtMillis: Long?,
    intervalMs: Long,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean {
    if (lastUpdatedAtMillis == null) return true
    return nowMillis - lastUpdatedAtMillis >= intervalMs
}

/** UI stale threshold (30s+) used by status ticker. */
fun isConnectionDisplayStale(
    lastUpdatedAtMillis: Long?,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean {
    val freshness = resolveStatusRefreshFreshness(lastUpdatedAtMillis, nowMillis) ?: return true
    return freshness >= StatusRefreshFreshness.Stale
}

fun RefreshSource.forcesHomeRefresh(force: Boolean): Boolean =
    force ||
        this == RefreshSource.APP_RESUME ||
        this == RefreshSource.RETURN_FROM_DETAIL ||
        this == RefreshSource.BOTTOM_NAV_RESELECT ||
        this == RefreshSource.MANUAL

fun RefreshSource.cancelsInFlightRefresh(): Boolean =
    this == RefreshSource.RETURN_FROM_DETAIL ||
        this == RefreshSource.BOTTOM_NAV_RESELECT ||
        this == RefreshSource.MANUAL ||
        this == RefreshSource.APP_RESUME

private const val TAG_REFRESH = "BuddyDash/Refresh"

fun logRefreshDecision(
    screen: String,
    source: RefreshSource,
    currentRoute: String?,
    lastUpdatedAtMillis: Long?,
    intervalMs: Long,
    stale: Boolean,
    refreshTriggered: Boolean,
    extra: String = "",
) {
    if (!BuddyDashDebug.enabled) return
    val ageSec = statusRefreshAgeSeconds(lastUpdatedAtMillis)
    Log.d(
        TAG_REFRESH,
        "screen=$screen lifecycleOrNav event source=$source route=$currentRoute " +
            "lastRefreshAgeSec=${ageSec ?: "never"} stale=$stale refreshTriggered=$refreshTriggered $extra",
    )
}
