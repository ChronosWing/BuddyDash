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

fun showConnectionStaleInHeader(
    hasCachedContent: Boolean,
    isStaleCachedData: Boolean,
    refreshError: String?,
    lastUpdatedAtMillis: Long?,
): Boolean {
    if (!hasCachedContent) return false
    if (showOfflineInHeader(hasCachedContent, isStaleCachedData, refreshError)) return false
    return isConnectionDisplayStale(lastUpdatedAtMillis) ||
        (refreshError != null && !isHomeRefreshOfflineError(refreshError))
}

fun showHeaderUpdating(
    isRefreshActive: Boolean,
    hasCachedContent: Boolean,
    isStaleCachedData: Boolean,
    refreshError: String?,
    lastUpdatedAtMillis: Long?,
): Boolean =
    isRefreshActive &&
        hasCachedContent &&
        !showOfflineInHeader(hasCachedContent, isStaleCachedData, refreshError) &&
        !showConnectionStaleInHeader(
            hasCachedContent = hasCachedContent,
            isStaleCachedData = isStaleCachedData,
            refreshError = refreshError,
            lastUpdatedAtMillis = lastUpdatedAtMillis,
        )

/** True when cached content is shown but server actions should be blocked. */
fun isShowingStaleCachedContent(
    isStaleCachedData: Boolean,
    refreshError: String?,
): Boolean = isStaleCachedData || refreshError != null
