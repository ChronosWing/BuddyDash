package com.chronoswing.buddydash.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.chronoswing.buddydash.ui.motion.buddyDashClickable
import com.chronoswing.buddydash.ui.motion.HomeAtmosphericFade
import com.chronoswing.buddydash.ui.motion.SecondaryScreenHeader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.ArchivesViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.ui.components.ArchiveListRow
import com.chronoswing.buddydash.ui.components.ArchiveListSkeleton
import com.chronoswing.buddydash.ui.components.BuddyDashEmptyIcon
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.components.OfflineStaleBanner
import com.chronoswing.buddydash.util.showStaleDataBanner
import com.chronoswing.buddydash.util.staleBannerShowsRefreshFailed
import com.chronoswing.buddydash.ui.components.asImageVector
import com.chronoswing.buddydash.ui.layout.BUDDYDASH_GRID_GUTTER_DP
import com.chronoswing.buddydash.ui.layout.rememberBuddyDashExpandedGridColumnCount
import com.chronoswing.buddydash.util.ArchivePrinterFilter
import com.chronoswing.buddydash.util.ArchiveResultFilter
import com.chronoswing.buddydash.util.ArchiveStatsSnapshot
import com.chronoswing.buddydash.util.ArchiveStatsTimeRange
import com.chronoswing.buddydash.util.ArchivesSection
import com.chronoswing.buddydash.util.ListLoadUi

@Composable
fun ArchivesScreen(
    viewModel: ArchivesViewModel,
    onArchiveClick: (PrintArchive) -> Unit,
    onClearPrinterFilter: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecyclePollingEffect(
        enabled = uiState.settingsReady && uiState.hasCredentials,
        intervalMs = 60_000L,
        initialDelayMs = 60_000L,
        pollImmediately = false,
        onPoll = {
            val showLoading = uiState.archives.isEmpty() && uiState.error == null
            viewModel.loadArchives(showLoading = showLoading)
        },
    )

    ArchivesScreenContent(
        archives = uiState.filteredArchives,
        totalCount = uiState.archives.size,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        hasAttemptedNetworkLoad = uiState.hasAttemptedNetworkLoad,
        error = uiState.error,
        refreshError = uiState.refreshError,
        isStaleCachedData = uiState.isStaleCachedData,
        hasCompletedLoad = uiState.hasCompletedLoad,
        lastUpdatedAtMillis = uiState.lastUpdatedAtMillis,
        settingsReady = uiState.settingsReady,
        hasCredentials = uiState.hasCredentials,
        serverUrl = uiState.serverUrl,
        cameraToken = uiState.cameraToken,
        searchQuery = uiState.searchQuery,
        filter = uiState.filter,
        printerFilter = uiState.printerFilter,
        section = uiState.section,
        statsTimeRange = uiState.statsTimeRange,
        statsSnapshot = uiState.statsSnapshot,
        onSectionChange = viewModel::onSectionChange,
        onStatsTimeRangeChange = viewModel::onStatsTimeRangeChange,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onRefresh = {
            viewModel.loadArchives(
                showLoading = uiState.archives.isEmpty(),
                fromUser = true,
            )
        },
        onPullRefresh = { viewModel.loadArchives(showLoading = false, fromPull = true) },
        onArchiveClick = onArchiveClick,
        onClearPrinterFilter = onClearPrinterFilter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivesScreenContent(
    archives: List<PrintArchive>,
    totalCount: Int,
    isLoading: Boolean,
    isRefreshing: Boolean,
    hasAttemptedNetworkLoad: Boolean,
    error: String?,
    refreshError: String?,
    isStaleCachedData: Boolean,
    hasCompletedLoad: Boolean,
    lastUpdatedAtMillis: Long?,
    settingsReady: Boolean,
    hasCredentials: Boolean,
    serverUrl: String,
    cameraToken: String,
    searchQuery: String,
    filter: ArchiveResultFilter,
    printerFilter: ArchivePrinterFilter?,
    section: ArchivesSection,
    statsTimeRange: ArchiveStatsTimeRange,
    statsSnapshot: ArchiveStatsSnapshot,
    onSectionChange: (ArchivesSection) -> Unit,
    onStatsTimeRangeChange: (ArchiveStatsTimeRange) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ArchiveResultFilter) -> Unit,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    onArchiveClick: (PrintArchive) -> Unit,
    onClearPrinterFilter: () -> Unit,
) {
    val cachedCount = totalCount
    val showStaleBanner = hasAttemptedNetworkLoad && showStaleDataBanner(
        hasCachedContent = cachedCount > 0,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
        lastUpdatedAtMillis = lastUpdatedAtMillis,
    )
    val staleBannerRefreshFailed = hasAttemptedNetworkLoad && staleBannerShowsRefreshFailed(
        hasCachedContent = cachedCount > 0,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
    )
    val showInitialSkeleton = ListLoadUi.showInitialSkeleton(
        hasCredentials = hasCredentials,
        cachedItemCount = cachedCount,
        isInitialLoading = isLoading,
        hasCompletedLoad = hasCompletedLoad,
    )
    val showPullRefreshIndicator = ListLoadUi.showPullRefreshIndicator(
        isRefreshing = isRefreshing,
        cachedItemCount = cachedCount,
    )

    val selectedTabIndex = when (section) {
        ArchivesSection.History -> 0
        ArchivesSection.Stats -> 1
    }

    Scaffold(
        topBar = {
            Box {
                SecondaryScreenHeader(Modifier.matchParentSize())
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.archives_title))
                            if (section == ArchivesSection.History && totalCount > 0) {
                                Text(
                                    text = stringResource(R.string.archives_count, totalCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            HomeAtmosphericFade(Modifier.padding(top = innerPadding.calculateTopPadding()))
            val contentPhase = when {
                !settingsReady || showInitialSkeleton -> "skeleton"
                !hasCredentials -> "no_creds"
                error != null && totalCount == 0 && hasCompletedLoad && hasAttemptedNetworkLoad -> "error"
                else -> "content"
            }
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = {
                    fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(110, easing = FastOutSlowInEasing))
                },
                modifier = Modifier.fillMaxSize(),
                label = "archivesContent",
            ) { phase ->
        when (phase) {
            "skeleton" -> {
                ArchiveListSkeleton(Modifier.padding(innerPadding))
            }
            "no_creds" -> {
                EmptyContent(
                    message = stringResource(R.string.configure_settings_hint),
                    icon = BuddyDashEmptyIcon.Settings.asImageVector(),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            "error" -> {
                EmptyContent(
                    message = stringResource(R.string.offline_empty_archives_title),
                    subtitle = stringResource(R.string.offline_empty_archives_subtitle),
                    icon = BuddyDashEmptyIcon.Archives.asImageVector(),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    OfflineStaleBanner(
                        visible = showStaleBanner,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        refreshFailed = staleBannerRefreshFailed,
                    )
                    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = section == ArchivesSection.History,
                            onClick = { onSectionChange(ArchivesSection.History) },
                            text = { Text(stringResource(R.string.archives_tab_history)) },
                        )
                        Tab(
                            selected = section == ArchivesSection.Stats,
                            onClick = { onSectionChange(ArchivesSection.Stats) },
                            text = { Text(stringResource(R.string.archives_tab_stats)) },
                        )
                    }
                    PullToRefreshBox(
                        isRefreshing = showPullRefreshIndicator,
                        onRefresh = onPullRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        when (section) {
                            ArchivesSection.History -> ArchivesHistoryContent(
                                archives = archives,
                                totalCount = totalCount,
                                serverUrl = serverUrl,
                                cameraToken = cameraToken,
                                searchQuery = searchQuery,
                                filter = filter,
                                printerFilter = printerFilter,
                                onSearchQueryChange = onSearchQueryChange,
                                onFilterChange = onFilterChange,
                                onArchiveClick = onArchiveClick,
                                onClearPrinterFilter = onClearPrinterFilter,
                            )
                            ArchivesSection.Stats -> ArchiveStatsContent(
                                stats = statsSnapshot,
                                timeRange = statsTimeRange,
                                onTimeRangeChange = onStatsTimeRangeChange,
                            )
                        }
                    }
                }
            }
        }
        } // AnimatedContent
        } // Box
    }
}

@Composable
private fun ArchivesHistoryContent(
    archives: List<PrintArchive>,
    totalCount: Int,
    serverUrl: String,
    cameraToken: String,
    searchQuery: String,
    filter: ArchiveResultFilter,
    printerFilter: ArchivePrinterFilter?,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ArchiveResultFilter) -> Unit,
    onArchiveClick: (PrintArchive) -> Unit,
    onClearPrinterFilter: () -> Unit,
) {
    val archiveGridColumns = rememberBuddyDashExpandedGridColumnCount()
    Column(modifier = Modifier.fillMaxSize()) {
        printerFilter?.let { activePrinterFilter ->
            ArchivesPrinterFilterBanner(
                printerName = activePrinterFilter.printerName,
                onClear = onClearPrinterFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        ArchiveSearchAndFilters(
            searchQuery = searchQuery,
            filter = filter,
            onSearchQueryChange = onSearchQueryChange,
            onFilterChange = onFilterChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        if (archives.isEmpty()) {
            EmptyContent(
                message = when {
                    totalCount == 0 -> stringResource(R.string.archives_empty)
                    else -> stringResource(R.string.archives_no_match)
                },
                subtitle = when {
                    totalCount == 0 -> stringResource(R.string.empty_hint_archives)
                    else -> stringResource(R.string.empty_hint_search)
                },
                icon = if (totalCount == 0) {
                    BuddyDashEmptyIcon.Archives.asImageVector()
                } else {
                    BuddyDashEmptyIcon.Search.asImageVector()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        } else {
            val listState = rememberLazyListState()
            val gridState = rememberLazyGridState()
            ArchiveHistoryList(
                archives = archives,
                gridColumns = archiveGridColumns,
                listState = listState,
                gridState = gridState,
                serverUrl = serverUrl,
                cameraToken = cameraToken,
                onArchiveClick = onArchiveClick,
            )
        }
    }
}

@Composable
private fun ArchiveHistoryList(
    archives: List<PrintArchive>,
    gridColumns: Int,
    listState: LazyListState,
    gridState: LazyGridState,
    serverUrl: String,
    cameraToken: String,
    onArchiveClick: (PrintArchive) -> Unit,
) {
    val contentPadding = PaddingValues(
        start = 12.dp,
        end = 12.dp,
        bottom = 12.dp,
    )
    if (gridColumns <= 1) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = archives,
                key = { it.id },
            ) { archive ->
                ArchiveHistoryListItem(
                    archive = archive,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    onArchiveClick = onArchiveClick,
                )
            }
        }
    } else {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(BUDDYDASH_GRID_GUTTER_DP.dp),
            verticalArrangement = Arrangement.spacedBy(BUDDYDASH_GRID_GUTTER_DP.dp),
        ) {
            items(
                items = archives,
                key = { it.id },
            ) { archive ->
                ArchiveHistoryListItem(
                    archive = archive,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    onArchiveClick = onArchiveClick,
                )
            }
        }
    }
}

@Composable
private fun ArchiveHistoryListItem(
    archive: PrintArchive,
    serverUrl: String,
    cameraToken: String,
    onArchiveClick: (PrintArchive) -> Unit,
) {
    ArchiveListRow(
        archive = archive,
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        modifier = Modifier.buddyDashClickable { onArchiveClick(archive) },
    )
}

@Composable
private fun ArchiveSearchAndFilters(
    searchQuery: String,
    filter: ArchiveResultFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ArchiveResultFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ArchiveSearchAndFiltersContent(
            searchQuery = searchQuery,
            filter = filter,
            onSearchQueryChange = onSearchQueryChange,
            onFilterChange = onFilterChange,
        )
    }
}

@Composable
private fun ArchiveSearchAndFiltersContent(
    searchQuery: String,
    filter: ArchiveResultFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ArchiveResultFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.archives_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ArchiveFilterChip(
                label = stringResource(R.string.archive_filter_all),
                selected = filter == ArchiveResultFilter.All,
                onClick = { onFilterChange(ArchiveResultFilter.All) },
            )
            ArchiveFilterChip(
                label = stringResource(R.string.archive_filter_success),
                selected = filter == ArchiveResultFilter.Success,
                onClick = { onFilterChange(ArchiveResultFilter.Success) },
            )
            ArchiveFilterChip(
                label = stringResource(R.string.archive_filter_failed),
                selected = filter == ArchiveResultFilter.Failed,
                onClick = { onFilterChange(ArchiveResultFilter.Failed) },
            )
            ArchiveFilterChip(
                label = stringResource(R.string.archive_filter_cancelled),
                selected = filter == ArchiveResultFilter.Cancelled,
                onClick = { onFilterChange(ArchiveResultFilter.Cancelled) },
            )
        }
    }
}

@Composable
private fun ArchiveFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}

@Composable
private fun ArchivesPrinterFilterBanner(
    printerName: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.archives_printer_filter_chip, printerName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.archives_clear_printer_filter),
                    )
                }
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.archives_clear_printer_filter))
            }
        }
    }
}
