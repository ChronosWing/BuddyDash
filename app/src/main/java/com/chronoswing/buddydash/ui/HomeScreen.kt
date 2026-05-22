package com.chronoswing.buddydash.ui

import com.chronoswing.buddydash.ui.motion.HomeTitleLogoSlot
import com.chronoswing.buddydash.ui.motion.buddyDashClickable
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
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.chronoswing.buddydash.ui.components.HomeDashboardActivityStrip
import com.chronoswing.buddydash.ui.components.HomeDashboardActivityStripSkeleton
import com.chronoswing.buddydash.ui.components.HomeCardMicroMotionFrame
import com.chronoswing.buddydash.ui.components.MicroMotionProgressBar
import com.chronoswing.buddydash.ui.components.MicroMotionThumbnailFrame
import com.chronoswing.buddydash.ui.components.OfflineStaleBanner
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.ui.components.PrintTempsRow
import com.chronoswing.buddydash.ui.components.PrinterCoverImage
import com.chronoswing.buddydash.ui.components.PrinterQuickStatusRow
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.components.StatusLastUpdatedIndicator
import com.chronoswing.buddydash.util.HOME_PRINTER_SEARCH_MIN_COUNT
import com.chronoswing.buddydash.util.HomePrintersLoadState
import com.chronoswing.buddydash.util.ListLoadUi
import com.chronoswing.buddydash.util.resolveHomePrintersLoadState
import com.chronoswing.buddydash.util.showHomeConnectionStaleInHeader
import com.chronoswing.buddydash.util.showHomeHeaderUpdating
import com.chronoswing.buddydash.util.showHomeOfflineInHeader
import com.chronoswing.buddydash.util.showHomeStaleDataBanner
import com.chronoswing.buddydash.util.PrinterCardLabels
import com.chronoswing.buddydash.util.HomePrinterSearchFilter
import com.chronoswing.buddydash.util.applyHomePrinterSearch
import com.chronoswing.buddydash.util.homeSearchEmptyMessageRes
import com.chronoswing.buddydash.util.homePrinterDashboardCounts
import com.chronoswing.buddydash.util.PrinterActivityKind
import com.chronoswing.buddydash.util.resolveActivityKind
import com.chronoswing.buddydash.util.toCardLabels

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
    val showDashboardStrip = settingsReady && hasCredentials
    val dashboardStripLoading = showDashboardStrip &&
        !hasCompletedLoad &&
        cachedCount == 0 &&
        loadedSpoolCount == null
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
    val preferOfflineInHeader = showHomeOfflineInHeader(
        printers = printers,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
    )
    val showConnectionStaleInHeader = showHomeConnectionStaleInHeader(
        printers = printers,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
        lastUpdatedAtMillis = lastUpdatedAtMillis,
    )
    val showHeaderUpdating = showHomeHeaderUpdating(
        isRefreshActive = isRefreshActive,
        printers = printers,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
        lastUpdatedAtMillis = lastUpdatedAtMillis,
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
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HomeTitleWordmark(
                            ambientPulseEnabled = hasAnyPrinterPrinting(printers) &&
                                !preferOfflineInHeader,
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = appNameContentDescription
                                },
                        )
                        StatusLastUpdatedIndicator(
                            lastUpdatedAtMillis = lastUpdatedAtMillis,
                            isRefreshing = showHeaderUpdating,
                            enabled = hasCredentials && !isLoading,
                            onRefresh = onRefresh,
                            preferConnectionStale = showConnectionStaleInHeader,
                            preferOffline = preferOfflineInHeader,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
                actions = {
                    if (showPrinterSearch) {
                        IconButton(
                            onClick = {
                                searchExpanded = !searchExpanded
                                if (!searchExpanded) {
                                    searchQuery = ""
                                    searchFilter = HomePrinterSearchFilter.All
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_printers),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (searchExpanded) 0.95f else 0.65f,
                                ),
                            )
                        }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (showDashboardStrip) {
                        HomeDashboardActivityStripSkeleton()
                    }
                    PrinterListSkeleton(Modifier.weight(1f))
                }
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
                    if (showDashboardStrip) {
                        HomeDashboardActivityStrip(
                            onlineCount = printerCounts.online,
                            printingCount = printerCounts.printing,
                            loadedSpoolCount = loadedSpoolCount,
                            isLoading = dashboardStripLoading,
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
                    if (showDashboardStrip) {
                        HomeDashboardActivityStrip(
                            onlineCount = printerCounts.online,
                            printingCount = printerCounts.printing,
                            loadedSpoolCount = loadedSpoolCount,
                            isLoading = dashboardStripLoading,
                        )
                    }
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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

private fun hasAnyPrinterPrinting(printers: List<Printer>): Boolean =
    printers.any { printer ->
        printer.liveStatus?.resolveActivityKind() == PrinterActivityKind.Printing
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
        modifier = modifier,
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
            overflow = TextOverflow.Ellipsis,
        )
    }
}
