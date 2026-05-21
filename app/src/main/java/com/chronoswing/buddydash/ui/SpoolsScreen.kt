package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.SpoolsViewModel
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.components.SpoolInventoryRow
import com.chronoswing.buddydash.util.SpoolInventoryFilter

@Composable
fun SpoolsScreen(
    viewModel: SpoolsViewModel,
) {
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
        spools = uiState.filteredSpools,
        totalCount = uiState.spools.size,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        error = uiState.error,
        hasCredentials = uiState.hasCredentials,
        searchQuery = uiState.searchQuery,
        filter = uiState.filter,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onRefresh = { viewModel.loadSpools(showLoading = uiState.spools.isEmpty()) },
        onPullRefresh = { viewModel.loadSpools(showLoading = false, fromPull = true) },
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
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (SpoolInventoryFilter) -> Unit,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
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
                actions = {
                    IconButton(onClick = onRefresh, enabled = hasCredentials && !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
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
                            onSearchQueryChange = onSearchQueryChange,
                            onFilterChange = onFilterChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        if (spools.isEmpty()) {
                            EmptyContent(
                                message = when {
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
                                    SpoolInventoryRow(spool = spool)
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
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (SpoolInventoryFilter) -> Unit,
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
        androidx.compose.foundation.layout.Row(
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
