package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.components.ArchiveStatCard
import com.chronoswing.buddydash.ui.components.BuddyDashEmptyIcon
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.asImageVector
import com.chronoswing.buddydash.util.ARCHIVE_DISPLAY_NAME_FALLBACK
import com.chronoswing.buddydash.util.ArchiveRecentFailure
import com.chronoswing.buddydash.util.ArchiveStatsSnapshot
import com.chronoswing.buddydash.util.ArchiveStatsTimeRange

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArchiveStatsContent(
    stats: ArchiveStatsSnapshot,
    timeRange: ArchiveStatsTimeRange,
    onTimeRangeChange: (ArchiveStatsTimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ArchiveStatsTimeRangeChips(
            selected = timeRange,
            onSelected = onTimeRangeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        if (stats.totalPrints == 0) {
            EmptyContent(
                message = stringResource(R.string.archive_stats_empty_range),
                subtitle = stringResource(R.string.empty_hint_archives),
                icon = BuddyDashEmptyIcon.Archives.asImageVector(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        } else {
            val cards = archiveStatCards(stats)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (cards.isNotEmpty()) {
                    item(key = "stat_cards") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 2,
                        ) {
                            cards.forEach { (label, value) ->
                                ArchiveStatCard(
                                    label = label,
                                    value = value,
                                    modifier = Modifier.fillMaxWidth(0.48f),
                                )
                            }
                        }
                    }
                }
                if (stats.recentFailures.isNotEmpty()) {
                    item(key = "recent_failures_header") {
                        Text(
                            text = stringResource(R.string.archive_stats_recent_failures),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(
                        items = stats.recentFailures,
                        key = { it.archiveId },
                    ) { failure ->
                        ArchiveRecentFailureRow(failure = failure)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveStatsTimeRangeChips(
    selected: ArchiveStatsTimeRange,
    onSelected: (ArchiveStatsTimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ArchiveStatsRangeChip(
            label = stringResource(R.string.archive_stats_range_7d),
            selected = selected == ArchiveStatsTimeRange.Last7Days,
            onClick = { onSelected(ArchiveStatsTimeRange.Last7Days) },
        )
        ArchiveStatsRangeChip(
            label = stringResource(R.string.archive_stats_range_30d),
            selected = selected == ArchiveStatsTimeRange.Last30Days,
            onClick = { onSelected(ArchiveStatsTimeRange.Last30Days) },
        )
        ArchiveStatsRangeChip(
            label = stringResource(R.string.archive_stats_range_all),
            selected = selected == ArchiveStatsTimeRange.AllTime,
            onClick = { onSelected(ArchiveStatsTimeRange.AllTime) },
        )
    }
}

@Composable
private fun ArchiveStatsRangeChip(
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
private fun ArchiveRecentFailureRow(
    failure: ArchiveRecentFailure,
    modifier: Modifier = Modifier,
) {
    val displayName = if (failure.displayName == ARCHIVE_DISPLAY_NAME_FALLBACK) {
        stringResource(R.string.archive_unnamed_print)
    } else {
        failure.displayName
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            failure.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun archiveStatCards(stats: ArchiveStatsSnapshot): List<Pair<String, String>> =
    buildList {
        stats.totalPrints?.let { add(stringResource(R.string.archive_stats_total_prints) to it.toString()) }
        stats.successfulPrints?.let {
            add(stringResource(R.string.archive_stats_successful_prints) to it.toString())
        }
        stats.failedPrints?.let {
            add(stringResource(R.string.archive_stats_failed_prints) to it.toString())
        }
        stats.successRatePercent?.let {
            add(stringResource(R.string.archive_stats_success_rate) to "$it%")
        }
        stats.totalPrintTimeFormatted?.let {
            add(stringResource(R.string.archive_stats_total_print_time) to it)
        }
        stats.totalFilamentFormatted?.let {
            add(stringResource(R.string.archive_stats_total_filament) to it)
        }
        stats.mostUsedPrinter?.let {
            add(stringResource(R.string.archive_stats_most_used_printer) to it)
        }
        stats.mostUsedMaterial?.let {
            add(stringResource(R.string.archive_stats_most_used_material) to it)
        }
        stats.longestPrintFormatted?.let {
            add(stringResource(R.string.archive_stats_longest_print) to it)
        }
    }
