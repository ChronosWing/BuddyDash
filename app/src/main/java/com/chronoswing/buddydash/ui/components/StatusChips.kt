package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Circle
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
import com.chronoswing.buddydash.ui.theme.OfflineRed
import com.chronoswing.buddydash.ui.theme.OnlineGreen
import com.chronoswing.buddydash.ui.theme.TextSecondary
import com.chronoswing.buddydash.util.PlateIndicatorKind
import com.chronoswing.buddydash.util.PrinterActivityKind

@Composable
fun PrinterQuickStatusRow(
    activityKind: PrinterActivityKind,
    progressCompact: String?,
    plateKind: PlateIndicatorKind?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityStatusChip(
            kind = activityKind,
            progressCompact = progressCompact,
        )
        plateKind?.let { PlateStatusChip(kind = it) }
    }
}

@Composable
fun ActivityStatusChip(
    kind: PrinterActivityKind,
    progressCompact: String?,
    modifier: Modifier = Modifier,
) {
    val style = activityChipStyle(kind = kind)
    val label = activityChipLabel(kind, progressCompact)
    StatusPill(
        label = label,
        icon = style.icon,
        iconTint = style.accent,
        containerColor = style.container,
        contentColor = style.content,
        borderColor = style.accent.copy(alpha = 0.55f),
        modifier = modifier,
    )
}

@Composable
fun PlateStatusChip(
    kind: PlateIndicatorKind,
    modifier: Modifier = Modifier,
) {
    val style = when (kind) {
        PlateIndicatorKind.InUse -> PlateChipStyle(
            label = stringResource(R.string.plate_in_use_chip),
            icon = Icons.Filled.Layers,
            accent = CyanAccent,
            container = CyanAccent.copy(alpha = 0.14f),
            content = CyanAccent,
        )
        PlateIndicatorKind.Clear -> PlateChipStyle(
            label = stringResource(R.string.plate_clear_chip),
            icon = Icons.Filled.CheckCircle,
            accent = OnlineGreen,
            container = OnlineGreen.copy(alpha = 0.12f),
            content = OnlineGreen,
        )
        PlateIndicatorKind.NotClear -> PlateChipStyle(
            label = stringResource(R.string.plate_not_clear_chip),
            icon = Icons.Filled.Warning,
            accent = Color(0xFFFBBF24),
            container = Color(0xFFFBBF24).copy(alpha = 0.14f),
            content = Color(0xFFFBBF24),
        )
    }
    StatusPill(
        label = style.label,
        icon = style.icon,
        iconTint = style.accent,
        containerColor = style.container,
        contentColor = style.content,
        borderColor = style.accent.copy(alpha = 0.55f),
        modifier = modifier,
    )
}

@Composable
private fun StatusPill(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = iconTint,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun activityChipLabel(kind: PrinterActivityKind, progressCompact: String?): String {
    val base = when (kind) {
        PrinterActivityKind.Idle -> stringResource(R.string.status_idle)
        PrinterActivityKind.Printing -> stringResource(R.string.status_printing)
        PrinterActivityKind.Paused -> stringResource(R.string.status_paused)
        PrinterActivityKind.Error -> stringResource(R.string.status_error)
        PrinterActivityKind.Offline -> stringResource(R.string.status_offline)
        PrinterActivityKind.Busy -> stringResource(R.string.status_busy)
    }
    return if (kind == PrinterActivityKind.Printing && progressCompact != null) {
        "$base · $progressCompact"
    } else {
        base
    }
}

private data class ActivityChipStyle(
    val icon: ImageVector,
    val accent: Color,
    val container: Color,
    val content: Color,
)

private data class PlateChipStyle(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val container: Color,
    val content: Color,
)

@Composable
private fun activityChipStyle(kind: PrinterActivityKind): ActivityChipStyle = when (kind) {
    PrinterActivityKind.Idle -> ActivityChipStyle(
        icon = Icons.Outlined.Circle,
        accent = OnlineGreen,
        container = OnlineGreen.copy(alpha = 0.12f),
        content = OnlineGreen,
    )
    PrinterActivityKind.Printing -> ActivityChipStyle(
        icon = Icons.Filled.PlayArrow,
        accent = CyanAccent,
        container = CyanAccent.copy(alpha = 0.14f),
        content = CyanAccent,
    )
    PrinterActivityKind.Paused -> ActivityChipStyle(
        icon = Icons.Filled.Pause,
        accent = Color(0xFFFBBF24),
        container = Color(0xFFFBBF24).copy(alpha = 0.14f),
        content = Color(0xFFFBBF24),
    )
    PrinterActivityKind.Error -> ActivityChipStyle(
        icon = Icons.Filled.Error,
        accent = OfflineRed,
        container = OfflineRed.copy(alpha = 0.14f),
        content = OfflineRed,
    )
    PrinterActivityKind.Offline -> ActivityChipStyle(
        icon = Icons.Filled.CloudOff,
        accent = TextSecondary,
        container = TextSecondary.copy(alpha = 0.12f),
        content = TextSecondary,
    )
    PrinterActivityKind.Busy -> ActivityChipStyle(
        icon = Icons.Filled.HourglassEmpty,
        accent = MaterialTheme.colorScheme.primary,
        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        content = MaterialTheme.colorScheme.primary,
    )
}
