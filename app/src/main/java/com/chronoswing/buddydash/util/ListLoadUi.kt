package com.chronoswing.buddydash.util

/**
 * UI rules for list/detail screens: skeletons and pull-to-refresh overlays apply only
 * when there is no cached content to show.
 */
object ListLoadUi {
    fun hasCachedData(cachedItemCount: Int): Boolean = cachedItemCount > 0

    fun showInitialSkeleton(
        settingsReady: Boolean = true,
        hasCredentials: Boolean,
        cachedItemCount: Int,
        isInitialLoading: Boolean,
        hasCompletedLoad: Boolean,
    ): Boolean =
        settingsReady &&
            hasCredentials &&
            cachedItemCount == 0 &&
            (isInitialLoading || !hasCompletedLoad)

    /** Pull-to-refresh spinner overlays content; hide when cached rows are visible. */
    fun showPullRefreshIndicator(isRefreshing: Boolean, cachedItemCount: Int): Boolean =
        isRefreshing && cachedItemCount == 0
}
