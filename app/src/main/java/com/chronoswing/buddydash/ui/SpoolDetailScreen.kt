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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.ui.components.ArchiveThumbnail
import com.chronoswing.buddydash.ui.components.FilamentUsageText
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentColorSwatch
import com.chronoswing.buddydash.ui.components.FilamentRemainingBar
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.components.LowSpoolChip
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.util.ARCHIVE_DISPLAY_NAME_FALLBACK
import com.chronoswing.buddydash.util.SPOOL_ARCHIVE_MATCH_PREVIEW_LIMIT
import com.chronoswing.buddydash.util.SpoolArchiveMatches
import com.chronoswing.buddydash.util.formatArchiveDuration
import com.chronoswing.buddydash.util.formatArchiveStatusLabel
import com.chronoswing.buddydash.util.formatFilamentUsageCompact
import com.chronoswing.buddydash.util.formatSpoolCardTitle
import com.chronoswing.buddydash.util.formatSpoolLastUsed
import com.chronoswing.buddydash.util.formatSpoolLocationLine
import com.chronoswing.buddydash.util.formatSpoolMaterialSubtitle
import com.chronoswing.buddydash.util.formatSpoolRemainingGrams
import com.chronoswing.buddydash.util.formatSpoolTagIndicator

@Composable
fun SpoolDetailScreen(
    spoolId: Int,
    viewModel: SpoolDetailViewModel,
    onBack: () -> Unit,
    onArchiveClick: (PrintArchive) -> Unit,
    onViewAllArchives: (String) -> Unit,
) {
    LaunchedEffect(spoolId) {
        viewModel.init(spoolId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SpoolDetailScreenContent(
        spool = uiState.spool,
        archiveMatches = uiState.archiveMatches,
        isLoading = uiState.isLoading,
        error = uiState.error,
        serverUrl = uiState.serverUrl,
        cameraToken = uiState.cameraToken,
        onBack = onBack,
        onRetry = { viewModel.load(force = true) },
        onArchiveClick = onArchiveClick,
        onViewAllArchives = onViewAllArchives,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoolDetailScreenContent(
    spool: SpoolInventoryItem?,
    archiveMatches: SpoolArchiveMatches,
    isLoading: Boolean,
    error: String?,
    serverUrl: String,
    cameraToken: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onArchiveClick: (PrintArchive) -> Unit,
    onViewAllArchives: (String) -> Unit,
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
            isLoading && spool == null -> LoadingContent(Modifier.padding(innerPadding))
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
                    SpoolArchiveUsageSection(
                        spool = spool,
                        matches = archiveMatches,
                        serverUrl = serverUrl,
                        cameraToken = cameraToken,
                        onArchiveClick = onArchiveClick,
                        onViewAllArchives = onViewAllArchives,
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
private fun SpoolArchiveUsageSection(
    spool: SpoolInventoryItem,
    matches: SpoolArchiveMatches,
    serverUrl: String,
    cameraToken: String,
    onArchiveClick: (PrintArchive) -> Unit,
    onViewAllArchives: (String) -> Unit,
) {
    if (matches.archives.isEmpty()) return

    val sectionTitle = if (matches.isExactSpoolId) {
        stringResource(R.string.spool_detail_used_in_prints)
    } else {
        stringResource(R.string.spool_detail_matching_archives)
    }
    val preview = matches.archives.take(SPOOL_ARCHIVE_MATCH_PREVIEW_LIMIT)
    val moreCount = matches.archives.size - preview.size
    val viewAllQuery = buildList {
        add(formatSpoolCardTitle(spool))
        spool.colorName?.let { add(it) }
        spool.swatch.colorHexes.forEach { add(it) }
    }.joinToString(" ")

    DetailInfoCard {
        SectionHeader(sectionTitle)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            preview.forEach { archive ->
                val displayName = if (archive.displayName == ARCHIVE_DISPLAY_NAME_FALLBACK) {
                    stringResource(R.string.archive_unnamed_print)
                } else {
                    archive.displayName
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArchiveClick(archive) },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArchiveThumbnail(
                            archiveId = archive.id,
                            serverUrl = serverUrl,
                            cameraToken = cameraToken,
                            size = 44.dp,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatArchiveStatusLabel(archive.resultKind, archive.statusRaw),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            formatArchiveDuration(archive.durationSeconds)?.let { duration ->
                                Text(
                                    text = duration,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                )
                            }
                            formatFilamentUsageCompact(archive.filamentUsage)?.let { usage ->
                                FilamentUsageText(text = usage)
                            }
                        }
                    }
                }
            }
            if (moreCount > 0) {
                Text(
                    text = stringResource(R.string.spool_detail_view_all_archives, moreCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onViewAllArchives(viewAllQuery) }
                        .padding(top = 4.dp),
                )
            }
        }
    }
}
