package com.chronoswing.buddydash.ui

import com.chronoswing.buddydash.ui.motion.HomeTitleLogoSlot
import com.chronoswing.buddydash.ui.motion.HomeHeaderBackground
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.ui.components.PrintTempsRow
import com.chronoswing.buddydash.ui.components.PrinterCoverImage
import com.chronoswing.buddydash.ui.components.PrinterQuickStatusRow
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
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
import com.chronoswing.buddydash.util.PrinterActivityKind
import com.chronoswing.buddydash.util.resolveActivityKind
import com.chronoswing.buddydash.util.toCardLabels
import com.chronoswing.buddydash.ui.theme.OfflineRed

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPrinterClick: (Printer) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        onPrinterClick = onPrinterClick,
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
    onPrinterClick: (Printer) -> Unit,
) {
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
    val showStaleBanner = showHomeStaleDataBanner(
        printers = printers,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
        lastUpdatedAtMillis = lastUpdatedAtMillis,
    )
    val staleBannerRefreshFailed = showHomeStaleBannerRefreshFailed(
        printers = printers,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
    )
    val showPrinterSearch = printers.size >= HOME_PRINTER_SEARCH_MIN_COUNT
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchFilter by rememberSaveable { mutableStateOf(HomePrinterSearchFilter.All) }
    val listState = rememberLazyListState()

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
        // topBar height is only the branded header; content top padding matches that height.
        topBar = {
            HomeCompactTopBar(
                ambientPulseEnabled = hasAnyPrinterPrinting(printers) && !showStaleBanner,
                appNameContentDescription = appNameContentDescription,
                isRefreshActive = isRefreshActive,
                hasCredentials = hasCredentials,
                isLoading = isLoading,
                onRefresh = onRefresh,
                refreshFailed = staleBannerRefreshFailed,
                offlineStale = showStaleBanner && !staleBannerRefreshFailed,
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
        when {
            !settingsReady -> {
                PrinterListSkeleton(Modifier.padding(innerPadding))
            }
            !hasCredentials && cachedCount == 0 -> {
                EmptyContent(
                    message = stringResource(R.string.configure_settings_hint),
                    icon = BuddyDashEmptyIcon.Settings.asImageVector(),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            showInitialSkeleton -> {
                PrinterListSkeleton(Modifier.padding(innerPadding))
            }
            loadState == HomePrintersLoadState.ErrorNoCachedData -> {
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
            loadState == HomePrintersLoadState.EmptyLoadedSuccessfully -> {
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
                    if (showStaleBanner) {
                        OfflineStaleBanner(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            refreshFailed = staleBannerRefreshFailed,
                        )
                    }
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = HomePrinterListTopPadding,
                                bottom = 8.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (searchExpanded && filteredPrinters.isEmpty()) {
                                item(key = "search_empty") {
                                    EmptyContent(
                                        message = stringResource(
                                            homeSearchEmptyMessageRes(searchQuery, searchFilter),
                                        ),
                                        subtitle = stringResource(R.string.empty_hint_search),
                                        icon = BuddyDashEmptyIcon.Search.asImageVector(),
                                        modifier = Modifier.padding(vertical = 12.dp),
                                    )
                                }
                            }
                            items(filteredPrinters, key = { it.id }) { printer ->
                                GlancePrinterCard(
                                    labels = printer.toCardLabels(),
                                    printerId = printer.id,
                                    liveStatus = printer.liveStatus,
                                    serverUrl = serverUrl,
                                    cameraToken = cameraToken,
                                    onClick = { onPrinterClick(printer) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlancePrinterCard(
    labels: PrinterCardLabels,
    printerId: Int,
    liveStatus: PrinterStatus?,
    serverUrl: String,
    cameraToken: String,
    onClick: () -> Unit,
) {
    val printThumbnailIdentity = rememberCurrentPrintThumbnailIdentity(
        printerId = printerId,
        status = liveStatus,
        fileName = labels.fileLine,
    )
    HomeCardMicroMotionFrame(
        animateIdleBreath = false,
        motion = labels.cardMicroMotion,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .buddyDashClickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = labels.title,
                    style = MaterialTheme.typography.titleMedium,
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

            PrinterQuickStatusRow(
                activityKind = labels.activityKind,
                progressCompact = labels.progressCompact,
                plateKind = labels.plateKind,
                maintenanceIndicator = labels.maintenanceIndicator,
                pendingQueueCount = labels.pendingQueueCount,
            )

            if (labels.isActivePrint) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        labels.progressFraction?.let { fraction ->
                            MicroMotionProgressBar(
                                progress = { fraction.coerceIn(0f, 1f) },
                                motion = labels.cardMicroMotion,
                                modifier = Modifier.height(3.dp),
                            )
                        }
                        labels.fileLine?.let { file ->
                            PrintFileNameText(
                                fileName = file,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        labels.etaLine?.let { eta ->
                            Text(
                                text = eta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    MicroMotionThumbnailFrame(
                        motion = labels.cardMicroMotion,
                        modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                    ) {
                        PrinterCoverImage(
                            serverUrl = serverUrl,
                            cameraToken = cameraToken,
                            thumbnailIdentity = printThumbnailIdentity,
                            size = 64.dp,
                        )
                    }
                }
            } else {
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

            if (labels.tempsLine != null) {
                PrintTempsRow(
                    nozzleTemp = labels.nozzleTemp,
                    bedTemp = labels.bedTemp,
                )
            }

            FilamentHomeGroupsRow(
                slots = labels.filamentSlots,
                activeKey = labels.activeFilamentSlot,
                cardMicroMotion = labels.cardMicroMotion,
                modifier = Modifier.padding(top = 2.dp),
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
    showPrinterSearch: Boolean,
    searchExpanded: Boolean,
    onSearchToggle: () -> Unit,
) {
    HomeHeaderBackground(modifier = Modifier.fillMaxWidth()) {
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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeTitleLogoImageSize),
    ) {
        HomeTitleWordmark(
            ambientPulseEnabled = ambientPulseEnabled,
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
    modifier: Modifier = Modifier,
) {
    val wordmarkTextStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Row(
        modifier = modifier.wrapContentWidth(Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        HomeTitleLogoSlot(
            ambientPulseEnabled = ambientPulseEnabled,
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
