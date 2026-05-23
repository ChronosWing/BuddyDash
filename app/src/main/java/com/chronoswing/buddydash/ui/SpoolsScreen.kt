package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import com.chronoswing.buddydash.ui.motion.HomeAtmosphericFade
import com.chronoswing.buddydash.ui.motion.SecondaryScreenHeader
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.SpoolsViewModel
import com.chronoswing.buddydash.ui.components.BuddyDashEmptyIcon
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentColorSwatch
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.components.OfflineStaleBanner
import com.chronoswing.buddydash.util.showStaleDataBanner
import com.chronoswing.buddydash.util.staleBannerShowsRefreshFailed
import com.chronoswing.buddydash.ui.components.SpoolInventoryRow
import com.chronoswing.buddydash.ui.components.SpoolListSkeleton
import com.chronoswing.buddydash.ui.components.asImageVector
import com.chronoswing.buddydash.util.ArchiveSpoolLookupFilter
import com.chronoswing.buddydash.util.ListLoadUi
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.util.SpoolInventoryCardUsage
import com.chronoswing.buddydash.util.SpoolInventoryFilter
import com.chronoswing.buddydash.util.archiveLookupFilterSummary

@Composable
fun SpoolsScreen(
    viewModel: SpoolsViewModel,
    initialSearchQuery: String = "",
    initialArchiveLookupFilter: ArchiveSpoolLookupFilter? = null,
    onBack: (() -> Unit)? = null,
    onSpoolClick: (Int) -> Unit,
    onClearArchiveLookup: () -> Unit = {},
) {
    LaunchedEffect(initialArchiveLookupFilter, initialSearchQuery) {
        when {
            initialArchiveLookupFilter != null -> {
                viewModel.applyArchiveMaterialLookup(initialArchiveLookupFilter)
            }
            initialSearchQuery.isNotBlank() -> {
                viewModel.applyInitialSearchQuery(initialSearchQuery)
            }
            else -> {
                viewModel.applySectionRootFromNavigation()
            }
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecyclePollingEffect(
        enabled = uiState.hasCredentials && uiState.hasCompletedLoad,
        intervalMs = 30_000L,
        initialDelayMs = 30_000L,
        pollImmediately = false,
        onPoll = { viewModel.loadSpools(showLoading = false) },
    )

    SpoolsScreenContent(
        spools = uiState.filteredSpools(),
        totalCount = uiState.spools.size,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        hasCompletedLoad = uiState.hasCompletedLoad,
        hasAttemptedNetworkLoad = uiState.hasAttemptedNetworkLoad,
        error = uiState.error,
        refreshError = uiState.refreshError,
        isStaleCachedData = uiState.isStaleCachedData,
        lastUpdatedAtMillis = uiState.lastUpdatedAtMillis,
        hasCredentials = uiState.hasCredentials,
        searchQuery = uiState.searchQuery,
        filter = uiState.filter,
        archiveLookupFilter = uiState.archiveLookupFilter,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onClearArchiveLookup = onClearArchiveLookup,
        onRefresh = {
            viewModel.loadSpools(
                showLoading = uiState.spools.isEmpty(),
                fromUser = true,
            )
        },
        onPullRefresh = { viewModel.loadSpools(showLoading = false, fromPull = true) },
        onSpoolClick = onSpoolClick,
        onBack = onBack,
        cardUsageFor = uiState::cardUsageFor,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoolsScreenContent(
    spools: List<com.chronoswing.buddydash.data.model.SpoolInventoryItem>,
    totalCount: Int,
    isLoading: Boolean,
    isRefreshing: Boolean,
    hasCompletedLoad: Boolean,
    hasAttemptedNetworkLoad: Boolean,
    error: String?,
    refreshError: String?,
    isStaleCachedData: Boolean,
    lastUpdatedAtMillis: Long?,
    hasCredentials: Boolean,
    searchQuery: String,
    filter: SpoolInventoryFilter,
    archiveLookupFilter: ArchiveSpoolLookupFilter?,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (SpoolInventoryFilter) -> Unit,
    onClearArchiveLookup: () -> Unit,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    onSpoolClick: (Int) -> Unit,
    onBack: (() -> Unit)?,
    cardUsageFor: (SpoolInventoryItem) -> SpoolInventoryCardUsage,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val noMatchMessage = stringResource(R.string.snackbar_no_matching_filament)
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

    LaunchedEffect(archiveLookupFilter, spools.isEmpty(), isLoading, hasCompletedLoad) {
        if (
            archiveLookupFilter != null &&
            spools.isEmpty() &&
            !isLoading &&
            hasCompletedLoad &&
            totalCount > 0
        ) {
            snackbarHostState.showSnackbar(noMatchMessage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box {
                SecondaryScreenHeader(Modifier.matchParentSize())
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.spools_title))
                            if (totalCount > 0) {
                                Text(
                                    text = stringResource(R.string.spools_count, totalCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
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
        when {
            !hasCredentials -> {
                EmptyContent(
                    message = stringResource(R.string.configure_settings_hint),
                    icon = BuddyDashEmptyIcon.Settings.asImageVector(),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            showInitialSkeleton -> {
                SpoolListSkeleton(Modifier.padding(innerPadding))
            }
            error != null && totalCount == 0 && hasCompletedLoad && hasAttemptedNetworkLoad -> {
                EmptyContent(
                    message = stringResource(R.string.offline_empty_spools_title),
                    subtitle = stringResource(R.string.offline_empty_spools_subtitle),
                    icon = BuddyDashEmptyIcon.Spools.asImageVector(),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = showPullRefreshIndicator,
                    onRefresh = onPullRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (showStaleBanner) {
                            OfflineStaleBanner(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                refreshFailed = staleBannerRefreshFailed,
                            )
                        }
                        SpoolSearchAndFilters(
                            searchQuery = searchQuery,
                            filter = filter,
                            archiveLookupFilter = archiveLookupFilter,
                            onSearchQueryChange = onSearchQueryChange,
                            onFilterChange = onFilterChange,
                            onClearArchiveLookup = onClearArchiveLookup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        if (spools.isEmpty() && hasCompletedLoad) {
                            EmptyContent(
                                message = when {
                                    archiveLookupFilter != null ->
                                        stringResource(R.string.spools_archive_no_matching_filament)
                                    totalCount == 0 -> stringResource(R.string.spools_empty)
                                    else -> stringResource(R.string.spools_no_match)
                                },
                                subtitle = when {
                                    archiveLookupFilter != null ->
                                        stringResource(R.string.empty_hint_no_matching_filament)
                                    totalCount > 0 -> stringResource(R.string.empty_hint_search)
                                    else -> stringResource(R.string.empty_hint_spools)
                                },
                                icon = when {
                                    archiveLookupFilter != null || totalCount > 0 ->
                                        BuddyDashEmptyIcon.Search.asImageVector()
                                    else -> BuddyDashEmptyIcon.Spools.asImageVector()
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                            )
                        } else {
                            val listState = rememberLazyListState()
                            val visibleSpoolIds by remember {
                                derivedStateOf {
                                    listState.layoutInfo.visibleItemsInfo
                                        .mapNotNull { item ->
                                            (item.key as? Int) ?: (item.key as? String)?.toIntOrNull()
                                        }
                                        .toSet()
                                }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    bottom = 12.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(
                                    items = spools,
                                    key = { it.id },
                                ) { spool ->
                                    SpoolInventoryRow(
                                        spool = spool,
                                        cardUsage = cardUsageFor(spool),
                                        glowAnimationEnabled = spool.id in visibleSpoolIds,
                                        onClick = { onSpoolClick(spool.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        } // Box
    }
}

@Composable
private fun SpoolSearchAndFilters(
    searchQuery: String,
    filter: SpoolInventoryFilter,
    archiveLookupFilter: ArchiveSpoolLookupFilter?,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (SpoolInventoryFilter) -> Unit,
    onClearArchiveLookup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        archiveLookupFilter?.let { lookup ->
            ArchiveMatchingFilamentBanner(
                lookupFilter = lookup,
                onClear = onClearArchiveLookup,
            )
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.spools_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SpoolFilterChip(
                label = stringResource(R.string.spool_filter_all),
                selected = filter == SpoolInventoryFilter.All,
                onClick = { onFilterChange(SpoolInventoryFilter.All) },
            )
            SpoolFilterChip(
                label = stringResource(R.string.spool_filter_low),
                selected = filter == SpoolInventoryFilter.Low,
                onClick = { onFilterChange(SpoolInventoryFilter.Low) },
            )
            SpoolFilterChip(
                label = stringResource(R.string.spool_filter_loaded),
                selected = filter == SpoolInventoryFilter.Loaded,
                onClick = { onFilterChange(SpoolInventoryFilter.Loaded) },
            )
            SpoolFilterChip(
                label = stringResource(R.string.spool_filter_storage),
                selected = filter == SpoolInventoryFilter.Unloaded,
                onClick = { onFilterChange(SpoolInventoryFilter.Unloaded) },
            )
        }
    }
}

@Composable
private fun ArchiveMatchingFilamentBanner(
    lookupFilter: ArchiveSpoolLookupFilter,
    onClear: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.spools_matching_filament_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = archiveLookupFilterSummary(lookupFilter),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    lookupFilter.colorHexes.firstOrNull()?.let { hex ->
                        FilamentColorSwatch(
                            colorHexes = listOf(hex),
                            size = 14.dp,
                        )
                    }
                }
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.spools_clear_archive_filter))
            }
        }
    }
}

@Composable
private fun SpoolFilterChip(
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
