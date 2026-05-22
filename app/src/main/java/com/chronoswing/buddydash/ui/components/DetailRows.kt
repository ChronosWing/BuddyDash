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
import com.chronoswing.buddydash.ui.motion.AnimatedLinearProgressIndicator
import com.chronoswing.buddydash.ui.motion.FadeValueText
import com.chronoswing.buddydash.ui.motion.buddyDashClickable
import com.chronoswing.buddydash.ui.motion.refreshSpinning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.HeaderStatusAttention
import com.chronoswing.buddydash.util.resolveHeaderStatusAttention
import androidx.compose.ui.graphics.Color
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
        FadeValueText(
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
        FadeValueText(
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

@Composable
fun SectionHeaderRow(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * Calm header status: hidden when healthy, spinner-only while refreshing,
 * persistent muted text only for offline or refresh-failure attention states.
 */
@Composable
fun StatusLastUpdatedIndicator(
    @Suppress("UNUSED_PARAMETER") lastUpdatedAtMillis: Long?,
    isRefreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    hasCachedContent: Boolean = true,
    isStaleCachedData: Boolean = false,
    refreshError: String? = null,
) {
    val display = resolveHeaderStatusAttention(
        isRefreshActive = isRefreshing,
        hasCachedContent = hasCachedContent,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
    )
    if (display == HeaderStatusAttention.None) return

    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
    val attentionColor = MaterialTheme.colorScheme.error.copy(alpha = 0.82f)
    val message = when (display) {
        HeaderStatusAttention.Refreshing -> null
        HeaderStatusAttention.Offline -> stringResource(R.string.status_header_offline)
        HeaderStatusAttention.RefreshFailed -> stringResource(R.string.status_header_refresh_failed)
        HeaderStatusAttention.None -> return
    }
    val iconTint = when (display) {
        HeaderStatusAttention.Refreshing -> muted
        else -> attentionColor.copy(alpha = 0.75f)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        message?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = attentionColor,
                maxLines = 2,
            )
        }
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.cd_refresh_status),
            modifier = Modifier
                .size(if (display == HeaderStatusAttention.Refreshing) 14.dp else 12.dp)
                .refreshSpinning(display == HeaderStatusAttention.Refreshing)
                .buddyDashClickable(
                    enabled = enabled && display != HeaderStatusAttention.Refreshing,
                    onClick = onRefresh,
                ),
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
        FadeValueText(
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
        AnimatedLinearProgressIndicator(
            targetFraction = fraction.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
