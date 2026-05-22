package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.theme.CyanAccent
import com.chronoswing.buddydash.ui.theme.OnlineGreen
import com.chronoswing.buddydash.util.HomeDashboardChipKind
import com.chronoswing.buddydash.util.HomeDashboardChipModel
import com.chronoswing.buddydash.util.homeDashboardVisibleChips

@Composable
fun HomeDashboardActivityStrip(
    onlineCount: Int,
    printingCount: Int,
    loadedSpoolCount: Int?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isLoading) {
        HomeDashboardActivityStripSkeleton(modifier = modifier)
        return
    }
    val chips = homeDashboardVisibleChips(
        online = onlineCount,
        printing = printingCount,
        loadedSpools = loadedSpoolCount,
    )
    if (chips.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            HomeDashboardStatChip(model = chip)
        }
    }
}

@Composable
fun HomeDashboardActivityStripSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) {
            SkeletonBlock(
                modifier = Modifier
                    .width(88.dp)
                    .height(24.dp),
                cornerRadius = 8.dp,
            )
        }
    }
}

@Composable
private fun HomeDashboardStatChip(
    model: HomeDashboardChipModel,
    modifier: Modifier = Modifier,
) {
    val style = dashboardChipStyle(model.kind)
    val label = when (model.kind) {
        HomeDashboardChipKind.Online ->
            stringResource(R.string.home_dashboard_online, model.count)
        HomeDashboardChipKind.Printing ->
            stringResource(R.string.home_dashboard_printing, model.count)
        HomeDashboardChipKind.LoadedSpools ->
            stringResource(R.string.home_dashboard_loaded_spools, model.count)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = style.container,
        border = BorderStroke(1.dp, style.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = style.iconTint,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = style.content,
            )
        }
    }
}

private data class DashboardChipStyle(
    val icon: ImageVector,
    val iconTint: Color,
    val container: Color,
    val border: Color,
    val content: Color,
)

@Composable
private fun dashboardChipStyle(kind: HomeDashboardChipKind): DashboardChipStyle {
    val mutedVariant = MaterialTheme.colorScheme.onSurfaceVariant
    return when (kind) {
        HomeDashboardChipKind.Online -> DashboardChipStyle(
            icon = Icons.Outlined.Circle,
            iconTint = OnlineGreen.copy(alpha = 0.88f),
            container = OnlineGreen.copy(alpha = 0.1f),
            border = OnlineGreen.copy(alpha = 0.22f),
            content = mutedVariant.copy(alpha = 0.92f),
        )
        HomeDashboardChipKind.Printing -> DashboardChipStyle(
            icon = Icons.Filled.PlayArrow,
            iconTint = CyanAccent.copy(alpha = 0.9f),
            container = CyanAccent.copy(alpha = 0.1f),
            border = CyanAccent.copy(alpha = 0.22f),
            content = mutedVariant.copy(alpha = 0.92f),
        )
        HomeDashboardChipKind.LoadedSpools -> DashboardChipStyle(
            icon = Icons.Outlined.Palette,
            iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            content = mutedVariant.copy(alpha = 0.9f),
        )
    }
}
