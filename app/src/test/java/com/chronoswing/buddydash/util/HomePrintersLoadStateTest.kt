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
    fun showStaleBannerRefreshFailed_whenNonOfflineRefreshError() {
        assertTrue(
            showHomeStaleBannerRefreshFailed(
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = "Invalid API key.",
            ),
        )
    }

    @Test
    fun showStaleBannerRefreshFailed_falseWhenOfflineStale() {
        assertFalse(
            showHomeStaleBannerRefreshFailed(
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = "Can't reach server. Check the URL and network.",
            ),
        )
    }

    @Test
    fun showStaleBannerRefreshFailed_falseWhenNoCache() {
        assertFalse(
            showHomeStaleBannerRefreshFailed(
                printers = emptyList(),
                isStaleCachedData = true,
                refreshError = "Could not refresh printers",
            ),
        )
    }

    @Test
    fun showStaleBannerRefreshFailed_falseWhenOnlyStaleFlagWithoutError() {
        assertFalse(
            showHomeStaleBannerRefreshFailed(
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = null,
            ),
        )
    }

    private fun samplePrinter() = Printer(id = 1, name = "Test", model = "P1S")
}
