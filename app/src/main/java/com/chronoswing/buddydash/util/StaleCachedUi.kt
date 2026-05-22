package com.chronoswing.buddydash.util

/**
 * Shared offline/stale UI rules (Home is the reference). Used by list and detail screens.
 */
fun showStaleDataBanner(
    hasCachedContent: Boolean,
    isStaleCachedData: Boolean,
    refreshError: String? = null,
    lastUpdatedAtMillis: Long?,
): Boolean =
    hasCachedContent &&
        (isStaleCachedData ||
            refreshError != null ||
            isConnectionDisplayStale(lastUpdatedAtMillis))

fun showOfflineInHeader(
    hasCachedContent: Boolean,
    isStaleCachedData: Boolean,
    refreshError: String?,
): Boolean =
    hasCachedContent &&
        isStaleCachedData &&
        (refreshError == null || isHomeRefreshOfflineError(refreshError))

/** Non-offline refresh failure while cached content is visible (header attention only). */
fun showConnectionStaleInHeader(
    hasCachedContent: Boolean,
    isStaleCachedData: Boolean,
    refreshError: String?,
    lastUpdatedAtMillis: Long? = null,
): Boolean {
    if (!hasCachedContent) return false
    if (showOfflineInHeader(hasCachedContent, isStaleCachedData, refreshError)) return false
    return refreshError != null && !isHomeRefreshOfflineError(refreshError)
}

enum class HeaderStatusAttention {
    /** Healthy / live — no header status UI. */
    None,
    /** Active network request — spinner only. */
    Refreshing,
    /** Cached data, offline or not yet refreshed. */
    Offline,
    /** Refresh failed while showing cached data. */
    RefreshFailed,
}

fun resolveHeaderStatusAttention(
    isRefreshActive: Boolean,
    hasCachedContent: Boolean,
    isStaleCachedData: Boolean,
    refreshError: String?,
): HeaderStatusAttention {
    if (isRefreshActive) return HeaderStatusAttention.Refreshing
    if (!hasCachedContent) return HeaderStatusAttention.None
    if (showOfflineInHeader(hasCachedContent, isStaleCachedData, refreshError)) {
        return HeaderStatusAttention.Offline
    }
    if (showConnectionStaleInHeader(hasCachedContent, isStaleCachedData, refreshError)) {
        return HeaderStatusAttention.RefreshFailed
    }
    return HeaderStatusAttention.None
}

/** True when cached content is shown but server actions should be blocked. */
fun isShowingStaleCachedContent(
    isStaleCachedData: Boolean,
    refreshError: String?,
): Boolean = isStaleCachedData || refreshError != null
