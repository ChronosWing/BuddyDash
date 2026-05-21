package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.StatusRefreshFreshness
import com.chronoswing.buddydash.util.formatStatusUpdatedAgo
import com.chronoswing.buddydash.util.resolveStatusRefreshFreshness
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CompactIconStat(
    icon: ImageVector,
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    microLabel: String? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = iconTint,
        )
        microLabel?.let { tag ->
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactIconStatsFlowRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = { content() },
    )
}

@Composable
fun CompactLabelValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}

@Composable
fun DetailInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
fun ComingSoonActionButton(
    label: String,
    modifier: Modifier = Modifier,
    helperText: String,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label)
        }
        Text(
            text = helperText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Passive last-updated label with optional subtle manual refresh tap. */
@Composable
fun StatusLastUpdatedIndicator(
    lastUpdatedAtMillis: Long?,
    isRefreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(lastUpdatedAtMillis) {
        while (true) {
            delay(1_000L)
            tick = System.currentTimeMillis()
        }
    }
    val now = if (tick == 0L) System.currentTimeMillis() else tick
    val freshness = resolveStatusRefreshFreshness(lastUpdatedAtMillis, now) ?: return
    val ago = formatStatusUpdatedAgo(lastUpdatedAtMillis, now)
    val label = when {
        isRefreshing -> stringResource(R.string.status_updating)
        freshness == StatusRefreshFreshness.ConnectionStale ->
            stringResource(R.string.status_connection_stale)
        ago != null -> stringResource(R.string.status_updated_ago, ago)
        else -> return
    }

    val dueSoonTint = Color(0xFFFBBF24)
    val (textColor, iconTint) = when {
        isRefreshing -> {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            muted to muted
        }
        freshness == StatusRefreshFreshness.Live ->
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f) to
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
        freshness == StatusRefreshFreshness.Aging ->
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f) to
                dueSoonTint.copy(alpha = 0.7f)
        freshness == StatusRefreshFreshness.Stale ->
            dueSoonTint to dueSoonTint.copy(alpha = 0.9f)
        else ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.88f) to
                MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.cd_refresh_status),
            modifier = Modifier
                .size(12.dp)
                .clickable(enabled = enabled && !isRefreshing) { onRefresh() },
            tint = iconTint,
        )
    }
}

@Composable
fun HighlightValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SecondaryNote(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun InlineProgress(
    label: String,
    value: String,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
