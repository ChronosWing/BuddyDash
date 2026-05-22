package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.chronoswing.buddydash.util.CardMicroMotion
import com.chronoswing.buddydash.util.PlateIndicatorKind
import com.chronoswing.buddydash.util.MaintenanceHomeIndicator
import com.chronoswing.buddydash.util.PrinterActivityKind

@Composable
fun PrinterQuickStatusRow(
    activityKind: PrinterActivityKind,
    progressCompact: String?,
    plateKind: PlateIndicatorKind?,
    modifier: Modifier = Modifier,
    cardMicroMotion: CardMicroMotion = CardMicroMotion.None,
    maintenanceIndicator: MaintenanceHomeIndicator = MaintenanceHomeIndicator.None,
    pendingQueueCount: Int = 0,
    onErrorChipClick: (() -> Unit)? = null,
) {
    val chipBreath = rememberPrintingChipBreath(cardMicroMotion == CardMicroMotion.Printing)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityStatusChip(
            kind = activityKind,
            progressCompact = progressCompact,
            breathPhase = chipBreath,
            onClick = if (activityKind == PrinterActivityKind.Error && onErrorChipClick != null) {
                onErrorChipClick
            } else {
                null
            },
        )
        plateKind?.let { PlateStatusChip(kind = it) }
        MaintenanceHomeIndicatorIcon(indicator = maintenanceIndicator)
        QueueCountChip(count = pendingQueueCount)
    }
}

@Composable
fun QueueCountChip(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
        ),
    ) {
        Text(
            text = stringResource(R.string.queue_count_chip, count),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}

@Composable
fun MaintenanceHomeIndicatorIcon(
    indicator: MaintenanceHomeIndicator,
    modifier: Modifier = Modifier,
) {
    when (indicator) {
        MaintenanceHomeIndicator.None -> Unit
        MaintenanceHomeIndicator.DueSoon -> {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.cd_maintenance_due_soon_home),
                modifier = modifier.size(16.dp),
                tint = Color(0xFFFBBF24).copy(alpha = 0.9f),
            )
        }
        MaintenanceHomeIndicator.Due -> {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.cd_maintenance_due_home),
                modifier = modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.92f),
            )
        }
    }
}

@Composable
fun ActivityStatusChip(
    kind: PrinterActivityKind,
    progressCompact: String?,
    modifier: Modifier = Modifier,
    breathPhase: Float = 0f,
    onClick: (() -> Unit)? = null,
) {
    val style = activityChipStyle(kind = kind)
    val label = activityChipLabel(kind, progressCompact)
    val breathe = kind == PrinterActivityKind.Printing
    val containerAlpha = if (breathe) 0.12f + breathPhase * 0.05f else null
    val borderAlpha = if (breathe) 0.48f + breathPhase * 0.12f else 0.55f
    StatusPill(
        label = label,
        icon = style.icon,
        iconTint = style.accent.copy(alpha = if (breathe) 0.88f + breathPhase * 0.1f else 1f),
        containerColor = style.container.copy(
            alpha = containerAlpha ?: style.container.alpha,
        ),
        contentColor = style.content,
        borderColor = style.accent.copy(alpha = borderAlpha),
        modifier = modifier,
        onClick = onClick,
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
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
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
