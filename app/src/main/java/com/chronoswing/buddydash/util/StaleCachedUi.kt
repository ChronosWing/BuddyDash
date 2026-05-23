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

/** Non-offline refresh failure while cached content is visible. */
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

/** Use refresh-failed banner copy instead of offline copy. */
fun staleBannerShowsRefreshFailed(
    hasCachedContent: Boolean,
    isStaleCachedData: Boolean,
    refreshError: String?,
): Boolean = showConnectionStaleInHeader(
    hasCachedContent = hasCachedContent,
    isStaleCachedData = isStaleCachedData,
    refreshError = refreshError,
)

/** True when cached content is shown but server actions should be blocked. */
fun isShowingStaleCachedContent(
    isStaleCachedData: Boolean,
    refreshError: String?,
): Boolean = isStaleCachedData || refreshError != null
