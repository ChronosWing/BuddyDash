package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.HomeUiState
import com.chronoswing.buddydash.data.model.Printer

/** Display state for Home / Printers list loading and errors. */
enum class HomePrintersLoadState {
    InitialLoading,
    Loaded,
    Refreshing,
    StaleWithCachedData,
    ErrorNoCachedData,
    EmptyLoadedSuccessfully,
}

fun HomeUiState.resolveHomePrintersLoadState(): HomePrintersLoadState =
    resolveHomePrintersLoadState(
        printers = printers,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        isEnriching = isEnriching,
        hasCompletedLoad = hasCompletedLoad,
        error = error,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
        lastUpdatedAtMillis = lastUpdatedAtMillis,
    )

fun resolveHomePrintersLoadState(
    printers: List<Printer>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isEnriching: Boolean = false,
    hasCompletedLoad: Boolean,
    error: String?,
    isStaleCachedData: Boolean = false,
    refreshError: String?,
    lastUpdatedAtMillis: Long?,
): HomePrintersLoadState {
    val hasCachedPrinters = printers.isNotEmpty()
    val showingStaleCachedData = showHomeStaleDataBanner(
        printers = printers,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
        lastUpdatedAtMillis = lastUpdatedAtMillis,
    )
    return when {
        !hasCompletedLoad && isLoading -> HomePrintersLoadState.InitialLoading
        !hasCachedPrinters && error != null -> HomePrintersLoadState.ErrorNoCachedData
        !hasCachedPrinters && hasCompletedLoad -> HomePrintersLoadState.EmptyLoadedSuccessfully
        hasCachedPrinters && showingStaleCachedData -> HomePrintersLoadState.StaleWithCachedData
        hasCachedPrinters && (isRefreshing || isEnriching) -> HomePrintersLoadState.Refreshing
        hasCachedPrinters -> HomePrintersLoadState.Loaded
        else -> HomePrintersLoadState.InitialLoading
    }
}

/** Persistent inline banner while last-known printer cards are shown after failure or age-based staleness. */
fun showHomeStaleDataBanner(
    printers: List<Printer>,
    isStaleCachedData: Boolean,
    refreshError: String? = null,
    lastUpdatedAtMillis: Long?,
): Boolean = showStaleDataBanner(
    hasCachedContent = printers.isNotEmpty(),
    isStaleCachedData = isStaleCachedData,
    refreshError = refreshError,
    lastUpdatedAtMillis = lastUpdatedAtMillis,
)

fun showHomeOfflineInHeader(
    printers: List<Printer>,
    isStaleCachedData: Boolean,
    refreshError: String?,
): Boolean = showOfflineInHeader(
    hasCachedContent = printers.isNotEmpty(),
    isStaleCachedData = isStaleCachedData,
    refreshError = refreshError,
)

fun showHomeConnectionStaleInHeader(
    printers: List<Printer>,
    isStaleCachedData: Boolean = false,
    refreshError: String?,
    lastUpdatedAtMillis: Long?,
): Boolean = showConnectionStaleInHeader(
    hasCachedContent = printers.isNotEmpty(),
    isStaleCachedData = isStaleCachedData,
    refreshError = refreshError,
    lastUpdatedAtMillis = lastUpdatedAtMillis,
)

fun resolveHomeHeaderStatusAttention(
    isRefreshActive: Boolean,
    printers: List<Printer>,
    isStaleCachedData: Boolean,
    refreshError: String?,
): HeaderStatusAttention = resolveHeaderStatusAttention(
    isRefreshActive = isRefreshActive,
    hasCachedContent = printers.isNotEmpty(),
    isStaleCachedData = isStaleCachedData,
    refreshError = refreshError,
)
