package com.chronoswing.buddydash.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.chronoswing.buddydash.ui.motion.HomeTitleLogoSlot
import com.chronoswing.buddydash.ui.motion.HomeLogoGlowLayer
import com.chronoswing.buddydash.ui.motion.HomeLogoGlowState
import com.chronoswing.buddydash.ui.motion.HomeAtmosphericFade
import com.chronoswing.buddydash.ui.motion.HomeHeaderBackground
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalHapticFeedback
import com.chronoswing.buddydash.ui.motion.buddyDashClickable
import com.chronoswing.buddydash.ui.motion.refreshSpinning
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SingleBed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.chronoswing.buddydash.ui.components.HmsDetailSheet
import com.chronoswing.buddydash.ui.components.MaintenanceDetailSheet
import com.chronoswing.buddydash.ui.components.PrinterAlertsSheet
import com.chronoswing.buddydash.util.HmsSeverity
import com.chronoswing.buddydash.util.clampFinite
import com.chronoswing.buddydash.util.MaintenanceHomeIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.HomeViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.util.rememberCurrentPrintThumbnailIdentity
import com.chronoswing.buddydash.ui.components.BuddyDashEmptyIcon
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.PrinterListSkeleton
import com.chronoswing.buddydash.ui.components.asImageVector
import com.chronoswing.buddydash.ui.components.FilamentHomeGroupsRow
import com.chronoswing.buddydash.ui.components.HomePrinterSearchField
import com.chronoswing.buddydash.ui.components.HomePrinterSearchFilterChips
import com.chronoswing.buddydash.ui.components.HomeActivityGhostPillsRow
import com.chronoswing.buddydash.ui.components.HomeCardMicroMotionFrame
import com.chronoswing.buddydash.ui.components.MicroMotionProgressBar
import com.chronoswing.buddydash.ui.components.MicroMotionThumbnailFrame
import com.chronoswing.buddydash.ui.components.OfflineStaleBanner
import com.chronoswing.buddydash.ui.components.rememberDelayedOfflineBannerVisible
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.ui.components.PrintTempsRow
import com.chronoswing.buddydash.ui.components.PrinterCoverImage
import com.chronoswing.buddydash.ui.components.PrinterQuickStatusRow
import com.chronoswing.buddydash.ui.components.QuickAction
import com.chronoswing.buddydash.ui.components.QuickActionsSheet
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.layout.rememberHomePrinterGridColumnCount
import com.chronoswing.buddydash.util.HOME_PRINTER_SEARCH_MIN_COUNT
import com.chronoswing.buddydash.util.HomePrintersLoadState
import com.chronoswing.buddydash.util.ListLoadUi
import com.chronoswing.buddydash.util.resolveHomePrintersLoadState
import com.chronoswing.buddydash.util.showHomeStaleBannerRefreshFailed
import com.chronoswing.buddydash.util.showHomeStaleDataBanner
import com.chronoswing.buddydash.util.PrinterCardLabels
import com.chronoswing.buddydash.util.HomePrinterSearchFilter
import com.chronoswing.buddydash.util.applyHomePrinterSearch
import com.chronoswing.buddydash.util.homeSearchEmptyMessageRes
import com.chronoswing.buddydash.util.homePrinterDashboardCounts
import com.chronoswing.buddydash.data.PrinterCardVisibility
import com.chronoswing.buddydash.util.HomeCardLayoutValues
import com.chronoswing.buddydash.util.HomeCardViewMode
import com.chronoswing.buddydash.util.layoutValues
import com.chronoswing.buddydash.util.PrinterActivityKind
import com.chronoswing.buddydash.util.resolveActivityKind
import com.chronoswing.buddydash.util.toCardLabels
import com.chronoswing.buddydash.ui.theme.OfflineRed
import com.chronoswing.buddydash.data.model.PrinterSmartPlugState
import com.chronoswing.buddydash.util.performLongPressHaptic
import com.chronoswing.buddydash.util.performNfcOutcomeHaptic
import com.chronoswing.buddydash.util.resolveNfcActionOutcomeMessage

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPrinterClick: (Printer) -> Unit,
    onQuickAction: (Printer, QuickAction) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(uiState.powerToggleOutcome) {
        val outcome = uiState.powerToggleOutcome ?: return@LaunchedEffect
        performNfcOutcomeHaptic(haptic, outcome.tier)
        resolveNfcActionOutcomeMessage(context, outcome)?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
        viewModel.consumePowerToggleOutcome()
    }

    LifecyclePollingEffect(
        enabled = uiState.settingsReady && uiState.hasCredentials && uiState.hasCompletedLoad,
        intervalMs = 15_000L,
        initialDelayMs = 15_000L,
        pollImmediately = false,
        onPoll = { viewModel.loadPrinters(showLoading = false) },
    )

    HomeScreenContent(
        printers = uiState.printers,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        isEnriching = uiState.isEnriching,
        hasCompletedLoad = uiState.hasCompletedLoad,
        hasAttemptedNetworkLoad = uiState.hasAttemptedNetworkLoad,
        error = uiState.error,
        refreshError = uiState.refreshError,
        isStaleCachedData = uiState.isStaleCachedData,
        hasCredentials = uiState.hasCredentials,
        settingsReady = uiState.settingsReady,
        serverUrl = uiState.serverUrl,
        cameraToken = uiState.cameraToken,
        onRefresh = { viewModel.refreshManual() },
        onPullRefresh = { viewModel.refreshManual() },
        lastUpdatedAtMillis = uiState.lastUpdatedAtMillis,
        loadedSpoolCount = uiState.loadedSpoolCount,
        idleGlowMultiplier = uiState.idleGlowMultiplier,
        headerAmbientMultiplier = uiState.headerAmbientMultiplier,
        printGlowMultiplier = uiState.printGlowMultiplier,
        debugForcePrintGlow = uiState.debugForcePrintGlow,
        debugShowLogoGlowBounds = uiState.debugShowLogoGlowBounds,
        homeCardDensity = uiState.homeCardDensity,
        cardVisibility = uiState.cardVisibility,
        showQuickActionHint = !uiState.hasUsedQuickActions,
        onQuickActionHintDismissed = { viewModel.markQuickActionsUsed() },
        onPrinterClick = onPrinterClick,
        onClearPrinterHms = viewModel::clearPrinterHmsErrors,
        onQuickAction = onQuickAction,
        snackbarHostState = snackbarHostState,
        powerToggleInFlightIds = uiState.powerToggleInFlightIds,
        onToggleSmartPlugPower = viewModel::toggleSmartPlugPower,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    printers: List<Printer>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isEnriching: Boolean,
    hasCompletedLoad: Boolean,
    hasAttemptedNetworkLoad: Boolean,
    error: String?,
    refreshError: String?,
    isStaleCachedData: Boolean,
    hasCredentials: Boolean,
    settingsReady: Boolean,
    serverUrl: String,
    cameraToken: String,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    lastUpdatedAtMillis: Long?,
    loadedSpoolCount: Int?,
    idleGlowMultiplier: Float,
    headerAmbientMultiplier: Float,
    printGlowMultiplier: Float,
    debugForcePrintGlow: Boolean,
    debugShowLogoGlowBounds: Boolean,
    homeCardDensity: Int = 0,
    cardVisibility: Map<Int, PrinterCardVisibility> = emptyMap(),
    showQuickActionHint: Boolean = false,
    onQuickActionHintDismissed: () -> Unit = {},
    onPrinterClick: (Printer) -> Unit,
    onClearPrinterHms: (Int, (Result<Unit>) -> Unit) -> Unit,
    onQuickAction: (Printer, QuickAction) -> Unit = { _, _ -> },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    powerToggleInFlightIds: Set<Int> = emptySet(),
    onToggleSmartPlugPower: (Int) -> Unit = {},
) {
    val viewMode = HomeCardViewMode.fromIndex(homeCardDensity)
    val layoutValues = viewMode.layoutValues()
    val cachedCount = printers.size
    val printerCounts = printers.homePrinterDashboardCounts()
    val showHeaderMetadata = settingsReady && hasCredentials
    val showInitialSkeleton = ListLoadUi.showInitialSkeleton(
        settingsReady = settingsReady,
        hasCredentials = hasCredentials,
        cachedItemCount = cachedCount,
        isInitialLoading = isLoading,
        hasCompletedLoad = hasCompletedLoad,
    )
    val isRefreshActive = isRefreshing || isEnriching
    val showPullRefreshIndicator = ListLoadUi.showPullRefreshIndicator(
        isRefreshing = isRefreshActive,
        cachedItemCount = cachedCount,
    )
    val appNameContentDescription = stringResource(R.string.app_name)
    val loadState = resolveHomePrintersLoadState(
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
    val showStaleBanner = hasAttemptedNetworkLoad && showHomeStaleDataBanner(
        printers = printers,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
        lastUpdatedAtMillis = lastUpdatedAtMillis,
    )
    val staleBannerRefreshFailed = hasAttemptedNetworkLoad && showHomeStaleBannerRefreshFailed(
        printers = printers,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
    )
    val showStaleBannerDelayed = rememberDelayedOfflineBannerVisible(showStaleBanner)
    val staleBannerRefreshFailedDelayed = rememberDelayedOfflineBannerVisible(staleBannerRefreshFailed)
    val showPrinterSearch = printers.size >= HOME_PRINTER_SEARCH_MIN_COUNT
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchFilter by rememberSaveable { mutableStateOf(HomePrinterSearchFilter.All) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val printerGridColumns = rememberHomePrinterGridColumnCount()

    if (searchExpanded) {
        BackHandler {
            searchExpanded = false
            searchQuery = ""
            searchFilter = HomePrinterSearchFilter.All
        }
    }

    LaunchedEffect(showPrinterSearch) {
        if (!showPrinterSearch) {
            searchExpanded = false
            searchQuery = ""
            searchFilter = HomePrinterSearchFilter.All
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // topBar height is only the branded header; content top padding matches that height.
        topBar = {
            HomeCompactTopBar(
                ambientPulseEnabled = hasAnyPrinterPrinting(printers) && !showStaleBanner,
                appNameContentDescription = appNameContentDescription,
                isRefreshActive = isRefreshActive,
                hasCredentials = hasCredentials,
                isLoading = isLoading,
                onRefresh = onRefresh,
                refreshFailed = staleBannerRefreshFailedDelayed,
                offlineStale = showStaleBannerDelayed && !staleBannerRefreshFailedDelayed,
                idleGlowMultiplier = idleGlowMultiplier,
                headerAmbientMultiplier = headerAmbientMultiplier,
                printGlowMultiplier = printGlowMultiplier,
                debugForcePrintGlow = debugForcePrintGlow,
                debugShowLogoGlowBounds = debugShowLogoGlowBounds,
                showPrinterSearch = showPrinterSearch,
                searchExpanded = searchExpanded,
                onSearchToggle = {
                    searchExpanded = !searchExpanded
                    if (!searchExpanded) {
                        searchQuery = ""
                        searchFilter = HomePrinterSearchFilter.All
                    }
                },
            )
        },
    ) { innerPadding ->
        // Box wraps all content branches so HomeAtmosphericFade can be placed behind
        // everything (first child = lowest z-order) while each branch retains its own
        // innerPadding. The fade is offset to the content-area top — not behind the topBar.
        Box(modifier = Modifier.fillMaxSize()) {
            HomeAtmosphericFade(
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            )
            val contentPhase = when {
                !settingsReady || showInitialSkeleton -> "skeleton"
                !hasCredentials && cachedCount == 0 -> "no_creds"
                loadState == HomePrintersLoadState.ErrorNoCachedData -> "error"
                loadState == HomePrintersLoadState.EmptyLoadedSuccessfully -> "empty"
                else -> "content"
            }
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = {
                    fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(110, easing = FastOutSlowInEasing))
                },
                modifier = Modifier.fillMaxSize(),
                label = "homeContent",
            ) { phase ->
            when (phase) {
            "skeleton" -> {
                PrinterListSkeleton(Modifier.padding(innerPadding))
            }
            "no_creds" -> {
                EmptyContent(
                    message = stringResource(R.string.configure_settings_hint),
                    icon = BuddyDashEmptyIcon.Settings.asImageVector(),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            "error" -> {
                PullToRefreshBox(
                    isRefreshing = showPullRefreshIndicator,
                    onRefresh = onPullRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    ErrorContent(
                        message = stringResource(R.string.home_error_no_connection),
                        subtitle = stringResource(R.string.home_error_no_connection_hint),
                        onRetry = onRefresh,
                    )
                }
            }
            "empty" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (showHeaderMetadata) {
                        HomeActivityGhostPillsRow(
                            onlineCount = printerCounts.online,
                            printingCount = printerCounts.printing,
                            loadedSpoolCount = loadedSpoolCount ?: 0,
                        )
                    }
                    EmptyContent(
                        message = stringResource(R.string.no_printers),
                        subtitle = stringResource(R.string.empty_hint_printers),
                        icon = BuddyDashEmptyIcon.Printers.asImageVector(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            else -> {
                val filteredPrinters = if (searchExpanded) {
                    applyHomePrinterSearch(printers, searchQuery, searchFilter)
                } else {
                    printers
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    HomePrinterSearchField(
                        expanded = searchExpanded,
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                    )
                    HomePrinterSearchFilterChips(
                        expanded = searchExpanded,
                        selectedFilter = searchFilter,
                        onFilterSelected = { searchFilter = it },
                    )
                    OfflineStaleBanner(
                        visible = showStaleBanner,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        refreshFailed = staleBannerRefreshFailed,
                    )
                    if (showHeaderMetadata) {
                        HomeActivityGhostPillsRow(
                            onlineCount = printerCounts.online,
                            printingCount = printerCounts.printing,
                            loadedSpoolCount = loadedSpoolCount ?: 0,
                        )
                    }
                    PullToRefreshBox(
                        isRefreshing = showPullRefreshIndicator,
                        onRefresh = onPullRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        HomePrinterCardsList(
                            filteredPrinters = filteredPrinters,
                            gridColumns = printerGridColumns,
                            listState = listState,
                            gridState = gridState,
                            searchExpanded = searchExpanded,
                            searchQuery = searchQuery,
                            searchFilter = searchFilter,
                            serverUrl = serverUrl,
                            cameraToken = cameraToken,
                            viewMode = viewMode,
                            layoutValues = layoutValues,
                            cardVisibility = cardVisibility,
                            showQuickActionHint = showQuickActionHint,
                            onQuickActionHintDismissed = onQuickActionHintDismissed,
                            onPrinterClick = onPrinterClick,
                            onClearPrinterHms = onClearPrinterHms,
                            onQuickAction = onQuickAction,
                            hasCredentials = hasCredentials,
                            powerToggleInFlightIds = powerToggleInFlightIds,
                            onToggleSmartPlugPower = onToggleSmartPlugPower,
                        )
                    }
                }
            }
        } // when
        } // AnimatedContent
        } // Box
    }
}

@Composable
private fun HomePrinterCardsList(
    filteredPrinters: List<Printer>,
    gridColumns: Int,
    listState: LazyListState,
    gridState: LazyGridState,
    searchExpanded: Boolean,
    searchQuery: String,
    searchFilter: HomePrinterSearchFilter,
    serverUrl: String,
    cameraToken: String,
    viewMode: HomeCardViewMode,
    layoutValues: HomeCardLayoutValues,
    cardVisibility: Map<Int, PrinterCardVisibility>,
    showQuickActionHint: Boolean,
    onQuickActionHintDismissed: () -> Unit,
    onPrinterClick: (Printer) -> Unit,
    onClearPrinterHms: (Int, (Result<Unit>) -> Unit) -> Unit,
    onQuickAction: (Printer, QuickAction) -> Unit,
    hasCredentials: Boolean,
    powerToggleInFlightIds: Set<Int>,
    onToggleSmartPlugPower: (Int) -> Unit,
) {
    val contentPadding = PaddingValues(
        start = 12.dp,
        end = 12.dp,
        top = HomePrinterListTopPadding,
        bottom = 8.dp,
    )
    val showSearchEmpty = searchExpanded && filteredPrinters.isEmpty()

    if (gridColumns <= 1) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(layoutValues.listItemSpacing),
        ) {
            if (showSearchEmpty) {
                item(key = "search_empty") {
                    HomePrinterSearchEmptyContent(
                        searchQuery = searchQuery,
                        searchFilter = searchFilter,
                    )
                }
            }
            itemsIndexed(filteredPrinters, key = { _, p -> p.id }) { index, printer ->
                HomePrinterCardItem(
                    printer = printer,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    viewMode = viewMode,
                    layoutValues = layoutValues,
                    visibility = cardVisibility[printer.id] ?: PrinterCardVisibility(),
                    showQuickActionHint = showQuickActionHint && index == 0,
                    onQuickActionHintDismissed = onQuickActionHintDismissed,
                    onPrinterClick = onPrinterClick,
                    onClearPrinterHms = onClearPrinterHms,
                    onQuickAction = onQuickAction,
                    hasCredentials = hasCredentials,
                    powerToggleInFlight = printer.id in powerToggleInFlightIds,
                    onToggleSmartPlugPower = { onToggleSmartPlugPower(printer.id) },
                )
            }
        }
    } else {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(layoutValues.listItemSpacing),
            verticalArrangement = Arrangement.spacedBy(layoutValues.listItemSpacing),
        ) {
            if (showSearchEmpty) {
                item(
                    key = "search_empty",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    HomePrinterSearchEmptyContent(
                        searchQuery = searchQuery,
                        searchFilter = searchFilter,
                    )
                }
            }
            itemsIndexed(filteredPrinters, key = { _, p -> p.id }) { index, printer ->
                HomePrinterCardItem(
                    printer = printer,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    viewMode = viewMode,
                    layoutValues = layoutValues,
                    visibility = cardVisibility[printer.id] ?: PrinterCardVisibility(),
                    showQuickActionHint = showQuickActionHint && index == 0,
                    onQuickActionHintDismissed = onQuickActionHintDismissed,
                    onPrinterClick = onPrinterClick,
                    onClearPrinterHms = onClearPrinterHms,
                    onQuickAction = onQuickAction,
                    hasCredentials = hasCredentials,
                    powerToggleInFlight = printer.id in powerToggleInFlightIds,
                    onToggleSmartPlugPower = { onToggleSmartPlugPower(printer.id) },
                )
            }
        }
    }
}

@Composable
private fun HomePrinterSearchEmptyContent(
    searchQuery: String,
    searchFilter: HomePrinterSearchFilter,
) {
    EmptyContent(
        message = stringResource(homeSearchEmptyMessageRes(searchQuery, searchFilter)),
        subtitle = stringResource(R.string.empty_hint_search),
        icon = BuddyDashEmptyIcon.Search.asImageVector(),
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun HomePrinterCardItem(
    printer: Printer,
    serverUrl: String,
    cameraToken: String,
    viewMode: HomeCardViewMode,
    layoutValues: HomeCardLayoutValues,
    visibility: PrinterCardVisibility,
    showQuickActionHint: Boolean = false,
    onQuickActionHintDismissed: () -> Unit = {},
    onPrinterClick: (Printer) -> Unit,
    onClearPrinterHms: (Int, (Result<Unit>) -> Unit) -> Unit,
    onQuickAction: (Printer, QuickAction) -> Unit,
    hasCredentials: Boolean,
    powerToggleInFlight: Boolean,
    onToggleSmartPlugPower: () -> Unit,
) {
    GlancePrinterCard(
        printer = printer,
        labels = printer.toCardLabels(),
        printerId = printer.id,
        liveStatus = printer.liveStatus,
        smartPlugState = printer.smartPlugState,
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        viewMode = viewMode,
        layoutValues = layoutValues,
        visibility = visibility,
        showQuickActionHint = showQuickActionHint,
        onQuickActionHintDismissed = onQuickActionHintDismissed,
        onClick = { onPrinterClick(printer) },
        onClearPrinterHms = onClearPrinterHms,
        onQuickAction = { action -> onQuickAction(printer, action) },
        hasCredentials = hasCredentials,
        smartOutletPowerLoading = powerToggleInFlight,
        onToggleSmartPlugPower = onToggleSmartPlugPower,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlancePrinterCard(
    printer: Printer,
    labels: PrinterCardLabels,
    printerId: Int,
    liveStatus: PrinterStatus?,
    smartPlugState: PrinterSmartPlugState?,
    serverUrl: String,
    cameraToken: String,
    viewMode: HomeCardViewMode,
    layoutValues: HomeCardLayoutValues,
    visibility: PrinterCardVisibility = PrinterCardVisibility(),
    showQuickActionHint: Boolean = false,
    onQuickActionHintDismissed: () -> Unit = {},
    onClick: () -> Unit,
    onClearPrinterHms: (Int, (Result<Unit>) -> Unit) -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    hasCredentials: Boolean,
    smartOutletPowerLoading: Boolean,
    onToggleSmartPlugPower: () -> Unit,
) {
    val printThumbnailIdentity = rememberCurrentPrintThumbnailIdentity(
        printerId = printerId,
        status = liveStatus,
        fileName = labels.fileLine,
    )
    var alertSheet by remember { mutableStateOf<HomePrinterAlertSheet?>(null) }
    var hmsClearInProgress by remember(printerId) { mutableStateOf(false) }
    var hmsClearError by remember(printerId) { mutableStateOf<String?>(null) }
    val hmsClearFailedMessage = stringResource(R.string.hms_clear_failed)
    val hasHms = labels.hmsAlertSeverity != HmsSeverity.Ok
    val hasMaintenance = labels.maintenanceIndicator != MaintenanceHomeIndicator.None
    val showClearHmsAction = hasHms

    LaunchedEffect(hasHms, hasMaintenance) {
        when (alertSheet) {
            HomePrinterAlertSheet.Hms -> if (!hasHms) alertSheet = null
            HomePrinterAlertSheet.Unified -> when {
                !hasHms && !hasMaintenance -> alertSheet = null
                !hasHms && hasMaintenance -> alertSheet = HomePrinterAlertSheet.Maintenance
            }
            else -> Unit
        }
    }

    val onClearHms: () -> Unit = {
        if (!hmsClearInProgress) {
            hmsClearInProgress = true
            hmsClearError = null
            onClearPrinterHms(printerId) { result ->
                hmsClearInProgress = false
                result.fold(
                    onSuccess = { hmsClearError = null },
                    onFailure = { hmsClearError = hmsClearFailedMessage },
                )
            }
        }
    }

    when (val sheet = alertSheet) {
        HomePrinterAlertSheet.Hms -> if (hasHms) {
            HmsDetailSheet(
                printerName = labels.title,
                hmsErrors = labels.hmsErrors,
                hmsAlertSeverity = labels.hmsAlertSeverity,
                showClearAction = showClearHmsAction,
                isClearingHms = hmsClearInProgress,
                clearHmsError = hmsClearError,
                onClearHms = onClearHms,
                onDismiss = { alertSheet = null },
            )
        }
        HomePrinterAlertSheet.Maintenance -> if (hasMaintenance) {
            MaintenanceDetailSheet(
                printerName = labels.title,
                maintenanceItems = labels.maintenanceItems,
                maintenanceIndicator = labels.maintenanceIndicator,
                totalPrintHours = labels.maintenanceTotalPrintHours,
                onDismiss = { alertSheet = null },
            )
        }
        HomePrinterAlertSheet.Unified -> if (hasHms && hasMaintenance) {
            PrinterAlertsSheet(
                printerName = labels.title,
                hmsErrors = labels.hmsErrors,
                hmsAlertSeverity = labels.hmsAlertSeverity,
                maintenanceItems = labels.maintenanceItems,
                maintenanceIndicator = labels.maintenanceIndicator,
                maintenanceTotalPrintHours = labels.maintenanceTotalPrintHours,
                showClearHmsAction = showClearHmsAction,
                isClearingHms = hmsClearInProgress,
                clearHmsError = hmsClearError,
                onClearHms = onClearHms,
                onDismiss = { alertSheet = null },
            )
        }
        null -> Unit
    }

    var showQuickActions by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    if (showQuickActions) {
        QuickActionsSheet(
            printer = printer,
            hasSmartOutlet = smartPlugState != null,
            hasLight = liveStatus?.chamberLightOn != null,
            onAction = { action ->
                showQuickActions = false
                onQuickAction(action)
            },
            onDismiss = { showQuickActions = false },
        )
    }

    val effectiveShowMaintenanceChip = visibility.showMaintenanceChip
    val effectiveShowHmsChip = visibility.showHmsChip
    val effectiveShowTemps = visibility.showTemperatures &&
        (viewMode != HomeCardViewMode.Minimal)
    val effectiveShowThumbnail = visibility.showPrintThumbnail
    val showSmartOutletPower = visibility.showPowerChip && smartPlugState != null
    val smartOutletPowerState = smartPlugState?.displayPowerState

    HomeCardMicroMotionFrame(
        animateIdleBreath = false,
        motion = labels.cardMicroMotion,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        performLongPressHaptic(haptic)
                        showQuickActions = true
                        onQuickActionHintDismissed()
                    },
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Column(
                modifier = Modifier.padding(layoutValues.cardPadding),
                verticalArrangement = Arrangement.spacedBy(layoutValues.contentSpacing),
            ) {
                // -- Header: printer name + model --
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = labels.title,
                        style = if (viewMode == HomeCardViewMode.Minimal)
                            MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    labels.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // -- Status chips --
                PrinterQuickStatusRow(
                    activityKind = labels.activityKind,
                    progressCompact = labels.progressCompact,
                    plateKind = labels.plateKind,
                    maintenanceIndicator = if (effectiveShowMaintenanceChip) labels.maintenanceIndicator else MaintenanceHomeIndicator.None,
                    pendingQueueCount = labels.pendingQueueCount,
                    hmsAlertSeverity = if (effectiveShowHmsChip) labels.hmsAlertSeverity else HmsSeverity.Ok,
                    onHmsChipClick = if (hasHms && !hasMaintenance) {
                        { alertSheet = HomePrinterAlertSheet.Hms }
                    } else {
                        null
                    },
                    onMaintenanceChipClick = if (hasMaintenance && !hasHms) {
                        { alertSheet = HomePrinterAlertSheet.Maintenance }
                    } else {
                        null
                    },
                    onUnifiedAlertsClick = if (hasHms && hasMaintenance) {
                        { alertSheet = HomePrinterAlertSheet.Unified }
                    } else {
                        null
                    },
                    showSmartOutletPower = showSmartOutletPower,
                    smartOutletPowerState = smartOutletPowerState,
                    smartOutletPowerLoading = smartOutletPowerLoading,
                    smartOutletPowerEnabled = hasCredentials,
                    onSmartOutletPowerClick = if (showSmartOutletPower && hasCredentials) {
                        onToggleSmartPlugPower
                    } else {
                        null
                    },
                )

                // -- Active print section --
                if (labels.isActivePrint) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = layoutValues.contentSpacing / 2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(layoutValues.contentSpacing),
                        ) {
                            labels.progressFraction?.let { fraction ->
                                MicroMotionProgressBar(
                                    progress = { fraction.clampFinite(0f, 1f) },
                                    motion = labels.cardMicroMotion,
                                    modifier = Modifier.height(3.dp),
                                )
                            }
                            if (viewMode != HomeCardViewMode.Minimal) {
                                labels.fileLine?.let { file ->
                                    PrintFileNameText(
                                        fileName = file,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            labels.etaLine?.let { eta ->
                                Text(
                                    text = eta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (effectiveShowThumbnail) {
                            MicroMotionThumbnailFrame(
                                motion = labels.cardMicroMotion,
                                modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                            ) {
                                PrinterCoverImage(
                                    serverUrl = serverUrl,
                                    cameraToken = cameraToken,
                                    thumbnailIdentity = printThumbnailIdentity,
                                    size = layoutValues.thumbnailSize,
                                )
                            }
                        }
                    }
                } else if (viewMode != HomeCardViewMode.Minimal) {
                    // -- Idle section (Standard + Detailed) --
                    if (labels.showLastPrint && labels.lastPrintResult != null) {
                        Text(
                            text = stringResource(
                                R.string.last_print_line,
                                labels.lastPrintResult,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    labels.fileLine?.let { file ->
                        PrintFileNameText(
                            fileName = file,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // -- Temperatures (Standard only — Detailed folds them into metadata strip) --
                if (labels.tempsLine != null && effectiveShowTemps &&
                    viewMode != HomeCardViewMode.Detailed
                ) {
                    PrintTempsRow(
                        nozzleTemp = labels.nozzleTemp,
                        bedTemp = labels.bedTemp,
                    )
                }

                // -- Detailed mode: icon-driven metadata strip (includes temps) --
                if (viewMode == HomeCardViewMode.Detailed) {
                    GlanceCardDetailedExtras(
                        liveStatus = liveStatus,
                        maintenanceTotalPrintHours = labels.maintenanceTotalPrintHours,
                        nozzleTemp = if (effectiveShowTemps) labels.nozzleTemp else null,
                        bedTemp = if (effectiveShowTemps) labels.bedTemp else null,
                    )
                }

                // -- Filament slots (Standard + Detailed) --
                if (viewMode != HomeCardViewMode.Minimal) {
                    FilamentHomeGroupsRow(
                        slots = labels.filamentSlots,
                        activeKey = labels.activeFilamentSlot,
                        cardMicroMotion = labels.cardMicroMotion,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                if (showQuickActionHint) {
                    Text(
                        text = stringResource(R.string.quick_action_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlanceCardDetailedExtras(
    liveStatus: PrinterStatus?,
    maintenanceTotalPrintHours: Double?,
    nozzleTemp: String? = null,
    bedTemp: String? = null,
) {
    data class MetaChip(val icon: ImageVector, val value: String)

    val chips = buildList {
        nozzleTemp?.let {
            add(MetaChip(Icons.Filled.LocalFireDepartment, it.replace("°C", "°")))
        }
        bedTemp?.let {
            add(MetaChip(Icons.Filled.SingleBed, it.replace("°C", "°")))
        }
        liveStatus?.nozzleDiameterDisplay?.let { raw ->
            val short = raw.replace(Regex("\\s*mm$", RegexOption.IGNORE_CASE), "")
            add(MetaChip(Icons.Filled.Build, short))
        }
        liveStatus?.wifiSignalDbm?.let {
            add(MetaChip(Icons.Filled.Wifi, "$it"))
        }
        maintenanceTotalPrintHours?.let {
            add(MetaChip(Icons.Filled.Schedule, "${it.toInt()}h"))
        }
        liveStatus?.firmwareVersion?.let { fw ->
            val short = fw.trimStart('0').split(".").take(2)
                .joinToString(".") { it.trimStart('0').ifEmpty { "0" } }
            add(MetaChip(Icons.Filled.Memory, short))
        }
    }
    if (chips.isEmpty()) return
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        chips.forEach { chip ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = chip.icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = metaColor,
                )
                Text(
                    text = chip.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = metaColor,
                )
            }
        }
    }
}

private val HomeTitleLogoImageSize = 104.dp
private val HomeTitleLogoSlotWidth = 84.dp
private val HomeTitleTextPullLeft = 14.dp
private val HomeTitleStatusStartPadding = 16.dp
private val HomeTopBarHorizontalPadding = 16.dp
private val HomeTopBarActionsEndPadding = 4.dp
/** Below status bar; outer NavHost no longer applies top safe-area inset. */
private val HomeTopBarContentTopPadding = 1.dp
private val HomeTopBarContentBottomPadding = 2.dp
private val HomePrinterListTopPadding = 1.dp
private val HomeHeaderRefreshIdleAlpha = 0.48f
private val HomeHeaderRefreshActiveAlpha = 0.62f
private val HomeHeaderRefreshWarningTint = Color(0xFFF59E0B)

private enum class HomePrinterAlertSheet {
    Hms,
    Maintenance,
    Unified,
}

private fun hasAnyPrinterPrinting(printers: List<Printer>): Boolean =
    printers.any { printer ->
        printer.liveStatus?.resolveActivityKind() == PrinterActivityKind.Printing
    }

@Composable
private fun HomeCompactTopBar(
    ambientPulseEnabled: Boolean,
    appNameContentDescription: String,
    isRefreshActive: Boolean,
    hasCredentials: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    refreshFailed: Boolean,
    offlineStale: Boolean,
    idleGlowMultiplier: Float,
    headerAmbientMultiplier: Float,
    printGlowMultiplier: Float,
    debugForcePrintGlow: Boolean,
    debugShowLogoGlowBounds: Boolean,
    showPrinterSearch: Boolean,
    searchExpanded: Boolean,
    onSearchToggle: () -> Unit,
) {
    HomeHeaderBackground(
        modifier = Modifier.fillMaxWidth(),
        ambientMultiplier = headerAmbientMultiplier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = HomeTopBarHorizontalPadding,
                    end = HomeTopBarActionsEndPadding,
                    top = HomeTopBarContentTopPadding,
                    bottom = HomeTopBarContentBottomPadding,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                HomeTopBarTitle(
                    ambientPulseEnabled = ambientPulseEnabled,
                    appNameContentDescription = appNameContentDescription,
                    isRefreshActive = isRefreshActive,
                    hasCredentials = hasCredentials,
                    isLoading = isLoading,
                    onRefresh = onRefresh,
                    refreshFailed = refreshFailed,
                    offlineStale = offlineStale,
                    idleGlowMultiplier = idleGlowMultiplier,
                    printGlowMultiplier = printGlowMultiplier,
                    debugForcePrintGlow = debugForcePrintGlow,
                    debugShowLogoGlowBounds = debugShowLogoGlowBounds,
                    modifier = Modifier.weight(1f),
                )
                if (showPrinterSearch) {
                    IconButton(onClick = onSearchToggle) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_printers),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (searchExpanded) 0.95f else 0.65f,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBarTitle(
    ambientPulseEnabled: Boolean,
    appNameContentDescription: String,
    isRefreshActive: Boolean,
    hasCredentials: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    refreshFailed: Boolean,
    offlineStale: Boolean,
    idleGlowMultiplier: Float,
    printGlowMultiplier: Float,
    debugForcePrintGlow: Boolean,
    debugShowLogoGlowBounds: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeTitleLogoImageSize)
            .graphicsLayer { clip = false },
    ) {
        HomeTitleWordmark(
            ambientPulseEnabled = ambientPulseEnabled,
            idleGlowMultiplier = idleGlowMultiplier,
            printGlowMultiplier = printGlowMultiplier,
            debugForcePrintGlow = debugForcePrintGlow,
            debugShowLogoGlowBounds = debugShowLogoGlowBounds,
            modifier = Modifier
                .align(Alignment.TopStart)
                .semantics {
                    contentDescription = appNameContentDescription
                },
        )
        HomeHeaderRefreshAffordance(
            isRefreshing = isRefreshActive,
            refreshFailed = refreshFailed,
            offlineStale = offlineStale,
            enabled = hasCredentials && !isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, start = HomeTitleStatusStartPadding),
        )
    }
}

@Composable
private fun HomeHeaderRefreshAffordance(
    isRefreshing: Boolean,
    refreshFailed: Boolean,
    offlineStale: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tint = when {
        isRefreshing -> muted.copy(alpha = HomeHeaderRefreshActiveAlpha)
        refreshFailed -> HomeHeaderRefreshWarningTint.copy(alpha = 0.72f)
        offlineStale -> OfflineRed.copy(alpha = 0.68f)
        else -> muted.copy(alpha = HomeHeaderRefreshIdleAlpha)
    }
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = stringResource(R.string.cd_refresh_status),
        modifier = modifier
            .size(18.dp)
            .refreshSpinning(isRefreshing)
            .buddyDashClickable(enabled = enabled, onClick = onRefresh),
        tint = tint,
    )
}

@Composable
private fun HomeTitleWordmark(
    ambientPulseEnabled: Boolean,
    idleGlowMultiplier: Float,
    printGlowMultiplier: Float,
    debugForcePrintGlow: Boolean,
    debugShowLogoGlowBounds: Boolean,
    modifier: Modifier = Modifier,
) {
    val wordmarkTextStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val glowState = when {
        ambientPulseEnabled || debugForcePrintGlow -> HomeLogoGlowState.Printing
        else -> HomeLogoGlowState.Idle
    }
    Box(
        modifier = modifier
            .wrapContentWidth(Alignment.Start)
            .height(HomeTitleLogoImageSize)
            .graphicsLayer { clip = false },
    ) {
        HomeLogoGlowLayer(
            state = glowState,
            logoImageSize = HomeTitleLogoImageSize,
            logoSlotWidth = HomeTitleLogoSlotWidth,
            textPullLeft = HomeTitleTextPullLeft,
            idleGlowMultiplier = idleGlowMultiplier,
            printGlowMultiplier = printGlowMultiplier,
            showDebugBounds = debugShowLogoGlowBounds,
            modifier = Modifier.matchParentSize(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            HomeTitleLogoSlot(
                slotWidth = HomeTitleLogoSlotWidth,
                ambientDiameter = HomeTitleLogoImageSize,
            ) {
                Image(
                    painter = painterResource(R.drawable.buddydash_logo_white),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(HomeTitleLogoImageSize),
                )
            }
            Text(
                text = stringResource(R.string.home_title_suffix),
                modifier = Modifier.offset(x = -HomeTitleTextPullLeft),
                style = wordmarkTextStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
        }
    }
}
