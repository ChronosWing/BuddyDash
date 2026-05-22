package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePrintersLoadStateTest {

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
    fun showHeaderUpdating_falseWhileOfflineStaleRefreshInFlight() {
        assertFalse(
            showHomeHeaderUpdating(
                isRefreshActive = true,
                printers = listOf(samplePrinter()),
                isStaleCachedData = true,
                refreshError = null,
                lastUpdatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    @Test
    fun showHeaderUpdating_trueForFreshDataRefresh() {
        val now = System.currentTimeMillis()
        assertTrue(
            showHomeHeaderUpdating(
                isRefreshActive = true,
                printers = listOf(samplePrinter()),
                isStaleCachedData = false,
                refreshError = null,
                lastUpdatedAtMillis = now,
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

    private fun samplePrinter() = Printer(id = 1, name = "Test", model = "P1S")
}
