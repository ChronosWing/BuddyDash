package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.SpoolsViewModel
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentColorSwatch
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.components.SpoolInventoryRow
import com.chronoswing.buddydash.util.ArchiveSpoolLookupFilter
import com.chronoswing.buddydash.util.SpoolInventoryFilter
import com.chronoswing.buddydash.util.archiveLookupFilterSummary

@Composable
fun SpoolsScreen(
    viewModel: SpoolsViewModel,
    initialSearchQuery: String = "",
    initialArchiveLookupFilter: ArchiveSpoolLookupFilter? = null,
    onSpoolClick: (Int) -> Unit,
) {
    LaunchedEffect(initialArchiveLookupFilter) {
        if (initialArchiveLookupFilter != null) {
            viewModel.applyArchiveMaterialLookup(initialArchiveLookupFilter)
        } else if (initialSearchQuery.isNotBlank()) {
            viewModel.applyInitialSearchQuery(initialSearchQuery)
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecyclePollingEffect(
        enabled = uiState.hasCredentials,
        intervalMs = 30_000L,
        onPoll = {
            val showLoading = uiState.spools.isEmpty() && uiState.error == null
            viewModel.loadSpools(showLoading = showLoading)
        },
    )

    SpoolsScreenContent(
        spools = uiState.filteredSpools(),
        totalCount = uiState.spools.size,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        error = uiState.error,
        hasCredentials = uiState.hasCredentials,
        searchQuery = uiState.searchQuery,
        filter = uiState.filter,
        archiveLookupFilter = uiState.archiveLookupFilter,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onClearArchiveLookup = viewModel::clearArchiveLookupFilter,
        onRefresh = { viewModel.loadSpools(showLoading = uiState.spools.isEmpty()) },
        onPullRefresh = { viewModel.loadSpools(showLoading = false, fromPull = true) },
        onSpoolClick = onSpoolClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoolsScreenContent(
    spools: List<com.chronoswing.buddydash.data.model.SpoolInventoryItem>,
    totalCount: Int,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
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
) {
    Scaffold(
        topBar = {
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
            )
        },
    ) { innerPadding ->
        when {
            !hasCredentials -> {
                EmptyContent(
                    message = stringResource(R.string.configure_settings_hint),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            isLoading && totalCount == 0 -> {
                LoadingContent(Modifier.padding(innerPadding))
            }
            error != null && totalCount == 0 -> {
                ErrorContent(
                    message = error,
                    onRetry = onRefresh,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onPullRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
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
                        if (spools.isEmpty()) {
                            EmptyContent(
                                message = when {
                                    archiveLookupFilter != null ->
                                        stringResource(R.string.spools_archive_no_matching_filament)
                                    totalCount == 0 -> stringResource(R.string.spools_empty)
                                    else -> stringResource(R.string.spools_no_match)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                            )
                        } else {
                            LazyColumn(
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
                                        onClick = { onSpoolClick(spool.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
