package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePrintersLoadStateTest {

    @Test
    fun showInitialSkeleton_falseWhileSettingsLoading() {
        assertFalse(
            ListLoadUi.showInitialSkeleton(
                settingsReady = false,
                hasCredentials = false,
                cachedItemCount = 0,
                isInitialLoading = false,
                hasCompletedLoad = false,
            ),
        )
    }

    @Test
    fun resolve_staleWithCachedData_whenRefreshFailsWithPrinters() {
        val state = resolveHomePrintersLoadState(
            printers = listOf(samplePrinter()),
            isLoading = false,
            isRefreshing = false,
            hasCompletedLoad = true,
            error = null,
            isStaleCachedData = true,
            refreshError = "Could not refresh printers",
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
        assertEquals(HomePrintersLoadState.StaleWithCachedData, state)
    }

    @Test
    fun showStaleBanner_persistsWithStaleFlagWithoutRefreshError() {
        assertTrue(
            showHomeStaleDataBanner(
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = null,
                lastUpdatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    @Test
    fun showStaleBanner_hiddenAfterSuccessfulRefresh() {
        assertFalse(
            showHomeStaleDataBanner(
                printers = listOf(samplePrinter()),
                isStaleCachedData = false,
                refreshError = null,
                lastUpdatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    @Test
    fun resolve_errorNoCachedData_onlyWithoutPrinters() {
        val state = resolveHomePrintersLoadState(
            printers = emptyList(),
            isLoading = false,
            isRefreshing = false,
            hasCompletedLoad = true,
            error = "Request timed out. Try again.",
            refreshError = null,
            lastUpdatedAtMillis = null,
        )
        assertEquals(HomePrintersLoadState.ErrorNoCachedData, state)
    }

    @Test
    fun resolve_refreshing_keepsCachedVisible() {
        val state = resolveHomePrintersLoadState(
            printers = listOf(samplePrinter()),
            isLoading = false,
            isRefreshing = true,
            hasCompletedLoad = true,
            error = null,
            refreshError = null,
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
        assertEquals(HomePrintersLoadState.Refreshing, state)
    }

    @Test
    fun showOfflineInHeader_whenStaleCacheBeforeFirstRefresh() {
        assertTrue(
            showHomeOfflineInHeader(
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = null,
            ),
        )
    }

    @Test
    fun showOfflineInHeader_whenUnreachableRefreshFails() {
        assertTrue(
            showHomeOfflineInHeader(
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = "Can't reach server. Check the URL and network.",
            ),
        )
    }

    @Test
    fun resolveHeaderStatusAttention_refreshingWhileHealthy() {
        assertEquals(
            HeaderStatusAttention.Refreshing,
            resolveHomeHeaderStatusAttention(
                isRefreshActive = true,
                printers = listOf(samplePrinter()),
                isStaleCachedData = false,
                refreshError = null,
            ),
        )
    }

    @Test
    fun resolveHeaderStatusAttention_refreshingDuringOfflineStale() {
        assertEquals(
            HeaderStatusAttention.Refreshing,
            resolveHomeHeaderStatusAttention(
                isRefreshActive = true,
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = null,
            ),
        )
    }

    @Test
    fun resolveHeaderStatusAttention_noneWhenHealthy() {
        assertEquals(
            HeaderStatusAttention.None,
            resolveHomeHeaderStatusAttention(
                isRefreshActive = false,
                printers = listOf(samplePrinter()),
                isStaleCachedData = false,
                refreshError = null,
            ),
        )
    }

    @Test
    fun showConnectionStaleInHeader_whenNonOfflineRefreshError() {
        assertTrue(
            showHomeConnectionStaleInHeader(
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = "Invalid API key.",
                lastUpdatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    @Test
    fun showConnectionStaleInHeader_falseWhenOfflineStale() {
        assertFalse(
            showHomeConnectionStaleInHeader(
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = "Can't reach server. Check the URL and network.",
                lastUpdatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    @Test
    fun showConnectionStaleInHeader_falseWhenNoCache() {
        assertFalse(
            showHomeConnectionStaleInHeader(
                printers = emptyList(),
                refreshError = "Could not refresh printers",
                lastUpdatedAtMillis = null,
            ),
        )
    }

    @Test
    fun showConnectionStaleInHeader_falseWhenOnlyConnectionAgeStale() {
        assertFalse(
            showHomeConnectionStaleInHeader(
                printers = listOf(samplePrinter()),
                isStaleCachedData = false,
                refreshError = null,
                lastUpdatedAtMillis = System.currentTimeMillis() - 120_000L,
            ),
        )
    }

    private fun samplePrinter() = Printer(id = 1, name = "Test", model = "P1S")
}
