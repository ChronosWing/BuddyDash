package com.chronoswing.buddydash.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.SpoolDetailViewModel
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.ui.components.SpoolUsageThumbnail
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentColorSwatch
import com.chronoswing.buddydash.ui.components.FilamentRemainingBar
import com.chronoswing.buddydash.ui.components.FilamentUsageText
import com.chronoswing.buddydash.ui.components.PrinterDetailSkeleton
import com.chronoswing.buddydash.ui.components.LowSpoolChip
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.util.formatSpoolCardTitle
import com.chronoswing.buddydash.util.formatSpoolLastUsed
import com.chronoswing.buddydash.util.formatSpoolLocationLine
import com.chronoswing.buddydash.util.formatSpoolMaterialSubtitle
import com.chronoswing.buddydash.util.formatSpoolRemainingGrams
import com.chronoswing.buddydash.util.formatSpoolTagIndicator
import com.chronoswing.buddydash.util.SpoolUsageDisplayItem

@Composable
fun SpoolDetailScreen(
    spoolId: Int,
    viewModel: SpoolDetailViewModel,
    onBack: () -> Unit,
    onArchiveClick: (Int) -> Unit = {},
) {
    LaunchedEffect(spoolId) {
        viewModel.init(spoolId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SpoolDetailScreenContent(
        spool = uiState.spool,
        usageDisplayItems = uiState.usageDisplayItems,
        serverUrl = uiState.serverUrl,
        cameraToken = uiState.cameraToken,
        isLoading = uiState.isLoading,
        error = uiState.error,
        onBack = onBack,
        onRetry = { viewModel.load(force = true) },
        onArchiveClick = onArchiveClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoolDetailScreenContent(
    spool: SpoolInventoryItem?,
    usageDisplayItems: List<SpoolUsageDisplayItem>,
    serverUrl: String,
    cameraToken: String,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onArchiveClick: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = spool?.let { formatSpoolCardTitle(it) }
                            ?: stringResource(R.string.spool_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            isLoading && spool == null -> PrinterDetailSkeleton(Modifier.padding(innerPadding))
            error != null && spool == null -> ErrorContent(
                message = error,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )
            spool != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpoolDetailHero(spool = spool)
                    SpoolDetailFields(spool = spool)
                    SpoolUsageHistorySection(
                        items = usageDisplayItems,
                        serverUrl = serverUrl,
                        cameraToken = cameraToken,
                        onArchiveClick = onArchiveClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpoolDetailHero(spool: SpoolInventoryItem) {
    DetailInfoCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilamentColorSwatch(
                colorHexes = spool.swatch.colorHexes,
                isTranslucent = spool.swatch.isTranslucent,
                alpha = spool.swatch.alpha,
                size = 56.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatSpoolCardTitle(spool),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (spool.isLowStock) {
                        LowSpoolChip()
                    }
                }
                formatSpoolMaterialSubtitle(spool)?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatSpoolLocationLine(spool),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                spool.remainPercent?.let { percent ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilamentRemainingBar(
                            remainPercent = percent,
                            modifier = Modifier.weight(1f),
                            height = 5.dp,
                            barWidth = null,
                        )
                        Text(
                            text = stringResource(R.string.spool_remain_percent, percent),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpoolDetailFields(spool: SpoolInventoryItem) {
    DetailInfoCard {
        spool.brand?.let { brand ->
            CompactLabelValue(
                label = stringResource(R.string.spool_detail_brand),
                value = brand,
            )
        }
        formatSpoolMaterialSubtitle(spool)?.let { material ->
            CompactLabelValue(
                label = stringResource(R.string.spool_detail_material),
                value = material,
            )
        }
        spool.colorName?.let { color ->
            CompactLabelValue(
                label = stringResource(R.string.spool_detail_color),
                value = color,
            )
        }
        formatSpoolRemainingGrams(spool)?.let { remaining ->
            CompactLabelValue(
                label = stringResource(R.string.spool_detail_remaining),
                value = remaining,
            )
        }
        formatSpoolTagIndicator(spool.tagType, spool.dataOrigin)?.let { tag ->
            CompactLabelValue(
                label = stringResource(R.string.spool_detail_source),
                value = tag,
            )
        }
        formatSpoolLastUsed(spool.lastUsedIso)?.let { lastUsed ->
            CompactLabelValue(
                label = stringResource(R.string.spool_detail_last_used),
                value = lastUsed,
            )
        }
    }
}

@Composable
private fun SpoolUsageHistorySection(
    items: List<SpoolUsageDisplayItem>,
    serverUrl: String,
    cameraToken: String,
    onArchiveClick: (Int) -> Unit,
) {
    if (items.isEmpty()) return

    DetailInfoCard {
        SectionHeader(stringResource(R.string.spool_detail_used_in_prints))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item ->
                SpoolUsageHistoryRow(
                    item = item,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    onArchiveClick = onArchiveClick,
                )
            }
        }
    }
}

@Composable
private fun SpoolUsageHistoryRow(
    item: SpoolUsageDisplayItem,
    serverUrl: String,
    cameraToken: String,
    onArchiveClick: (Int) -> Unit,
) {
    val archiveId = item.archiveId
    val rowModifier = Modifier.fillMaxWidth().let { base ->
        if (item.isTappable && archiveId != null) {
            base.clickable { onArchiveClick(archiveId) }
        } else {
            base
        }
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpoolUsageThumbnail(
            archiveId = item.archiveId,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
            size = 44.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PrintFileNameText(
                fileName = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            FilamentUsageText(text = item.weightLine)
            item.printerLine?.let { printer ->
                Text(
                    text = printer,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.isTappable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(start = 2.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            )
        }
    }
}
