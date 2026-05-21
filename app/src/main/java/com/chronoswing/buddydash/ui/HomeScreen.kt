package com.chronoswing.buddydash.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.HomeViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentChipRow
import com.chronoswing.buddydash.ui.components.PrinterCoverImage
import com.chronoswing.buddydash.ui.components.PrinterQuickStatusRow
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.util.PrinterCardLabels
import com.chronoswing.buddydash.util.toCardLabels

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPrinterClick: (Printer) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecyclePollingEffect(
        enabled = uiState.hasCredentials,
        intervalMs = 15_000L,
        onPoll = {
            val showLoading = uiState.printers.isEmpty() && uiState.error == null
            viewModel.loadPrinters(showLoading = showLoading)
        },
    )

    HomeScreenContent(
        printers = uiState.printers,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        error = uiState.error,
        hasCredentials = uiState.hasCredentials,
        serverUrl = uiState.serverUrl,
        apiKey = uiState.apiKey,
        onRefresh = {
            viewModel.loadPrinters(showLoading = uiState.printers.isEmpty())
        },
        onPullRefresh = {
            viewModel.loadPrinters(showLoading = false, fromPull = true)
        },
        onPrinterClick = onPrinterClick,
        onSettingsClick = onSettingsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    printers: List<Printer>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    hasCredentials: Boolean,
    serverUrl: String,
    apiKey: String,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    onPrinterClick: (Printer) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = hasCredentials && !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
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
            isLoading && printers.isEmpty() -> {
                LoadingContent(Modifier.padding(innerPadding))
            }
            error != null && printers.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onPullRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    ErrorContent(
                        message = error,
                        onRetry = onRefresh,
                    )
                }
            }
            printers.isEmpty() -> {
                EmptyContent(
                    message = stringResource(R.string.no_printers),
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (error != null) {
                            item {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        items(printers, key = { it.id }) { printer ->
                            GlancePrinterCard(
                                labels = printer.toCardLabels(),
                                printerId = printer.id,
                                serverUrl = serverUrl,
                                apiKey = apiKey,
                                onClick = { onPrinterClick(printer) },
                            )
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
    serverUrl: String,
    apiKey: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            )

            if (labels.isActivePrint) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        labels.progressFraction?.let { fraction ->
                            LinearProgressIndicator(
                                progress = { fraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                            )
                        }
                        labels.fileLine?.let { file ->
                            Text(
                                text = file,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
                    PrinterCoverImage(
                        serverUrl = serverUrl,
                        apiKey = apiKey,
                        printerId = printerId,
                        size = 56.dp,
                    )
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
                    Text(
                        text = file,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            labels.tempsLine?.let { temps ->
                Text(
                    text = temps,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            FilamentChipRow(slots = labels.filamentSlots, compact = true)
        }
    }
}
