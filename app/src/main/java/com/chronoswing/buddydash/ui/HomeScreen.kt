package com.chronoswing.buddydash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.chronoswing.buddydash.ui.components.InlineProgress
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.theme.OnlineGreen
import com.chronoswing.buddydash.ui.theme.OfflineRed
import com.chronoswing.buddydash.util.PrinterCardLabels
import com.chronoswing.buddydash.util.toCardLabels

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPrinterClick: (Printer) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.hasCredentials) {
        if (uiState.hasCredentials) {
            viewModel.loadPrinters()
        }
    }

    HomeScreenContent(
        printers = uiState.printers,
        isLoading = uiState.isLoading,
        error = uiState.error,
        hasCredentials = uiState.hasCredentials,
        onRefresh = viewModel::loadPrinters,
        onPrinterClick = onPrinterClick,
        onSettingsClick = onSettingsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    printers: List<Printer>,
    isLoading: Boolean,
    error: String?,
    hasCredentials: Boolean,
    onRefresh: () -> Unit,
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
                ErrorContent(
                    message = error,
                    onRetry = onRefresh,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            printers.isEmpty() -> {
                EmptyContent(
                    message = stringResource(R.string.no_printers),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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
                            onClick = { onPrinterClick(printer) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlancePrinterCard(
    labels: PrinterCardLabels,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val dotColor = if (labels.isConnected) OnlineGreen else OfflineRed
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Text(
                        text = labels.connection,
                        style = MaterialTheme.typography.labelMedium,
                        color = dotColor,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = labels.currentActivity,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                labels.plateStatus?.let { plate ->
                    Text(
                        text = plate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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

            if (labels.progressText != null && labels.progressFraction != null) {
                InlineProgress(
                    label = stringResource(R.string.printing),
                    value = labels.progressText,
                    fraction = labels.progressFraction,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.nozzle_short, labels.nozzleTemp),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(R.string.bed_short, labels.bedTemp),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(R.string.hms_short, labels.hmsHealth),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (labels.hmsHasErrors) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            FilamentChipRow(slots = labels.filamentSlots, compact = true)
        }
    }
}
