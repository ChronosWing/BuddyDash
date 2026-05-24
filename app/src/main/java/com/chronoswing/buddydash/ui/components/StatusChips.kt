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
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.theme.CyanAccent
import com.chronoswing.buddydash.ui.theme.OfflineRed
import com.chronoswing.buddydash.ui.theme.OnlineGreen
import com.chronoswing.buddydash.ui.theme.TextSecondary
import com.chronoswing.buddydash.ui.motion.FadeValueText
import com.chronoswing.buddydash.ui.motion.rememberAttentionPulse
import com.chronoswing.buddydash.ui.motion.buddyDashClickable
import com.chronoswing.buddydash.util.HmsSeverity
import com.chronoswing.buddydash.util.PlateIndicatorKind
import com.chronoswing.buddydash.util.MaintenanceHomeIndicator
import com.chronoswing.buddydash.util.PrinterActivityKind

@Composable
fun PrinterQuickStatusRow(
    activityKind: PrinterActivityKind,
    progressCompact: String?,
    plateKind: PlateIndicatorKind?,
    modifier: Modifier = Modifier,
    maintenanceIndicator: MaintenanceHomeIndicator = MaintenanceHomeIndicator.None,
    pendingQueueCount: Int = 0,
    hmsAlertSeverity: HmsSeverity = HmsSeverity.Ok,
    onErrorChipClick: (() -> Unit)? = null,
    onHmsChipClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityStatusChip(
            kind = activityKind,
            progressCompact = progressCompact,
            onClick = if (activityKind == PrinterActivityKind.Error && onErrorChipClick != null) {
                onErrorChipClick
            } else {
                null
            },
        )
        HmsAlertChip(severity = hmsAlertSeverity, onClick = onHmsChipClick)
        plateKind?.let { PlateStatusChip(kind = it) }
        MaintenanceHomeIndicatorIcon(indicator = maintenanceIndicator)
        QueueCountChip(count = pendingQueueCount)
    }
}

private val HmsAmber = Color(0xFFFBBF24)

/**
 * Compact tappable chip shown when a printer has active HMS warnings or errors.
 * Visually distinct from the [MaintenanceHomeIndicatorIcon] (bare icon) — this is a full
 * bordered chip with text, using a different icon shape.
 */
@Composable
fun HmsAlertChip(
    severity: HmsSeverity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (severity == HmsSeverity.Ok) return

    val (accent, label, cd) = when (severity) {
        HmsSeverity.Error -> Triple(
            OfflineRed,
            stringResource(R.string.hms_chip_error),
            stringResource(R.string.cd_hms_chip_error),
        )
        HmsSeverity.Warning -> Triple(
            HmsAmber,
            stringResource(R.string.hms_chip_warning),
            stringResource(R.string.cd_hms_chip_warning),
        )
        HmsSeverity.Unknown -> Triple(
            HmsAmber,
            stringResource(R.string.hms_chip_unknown),
            stringResource(R.string.cd_hms_chip_unknown),
        )
        HmsSeverity.Ok -> return
    }

    val pulse = rememberAttentionPulse(
        enabled = severity == HmsSeverity.Error,
        periodMillis = 4_000,
    )
    val borderAlpha = if (severity == HmsSeverity.Error) 0.5f + pulse * 0.16f else 0.55f
    val containerAlpha = if (severity == HmsSeverity.Error) 0.14f + pulse * 0.05f else 0.14f

    StatusPill(
        label = label,
        icon = Icons.Outlined.Warning,
        iconTint = accent.copy(alpha = if (severity == HmsSeverity.Error) 0.88f + pulse * 0.1f else 1f),
        containerColor = accent.copy(alpha = containerAlpha),
        contentColor = accent,
        borderColor = accent.copy(alpha = borderAlpha),
        contentDescription = cd,
        modifier = modifier,
        onClick = onClick,
    )
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
            val pulse = rememberAttentionPulse(enabled = true, periodMillis = 2_800)
            val amber = Color(0xFFFBBF24)
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.cd_maintenance_due_soon_home),
                modifier = modifier.size(16.dp),
                tint = amber.copy(alpha = 0.82f + pulse * 0.14f),
            )
        }
        MaintenanceHomeIndicator.Due -> {
            val pulse = rememberAttentionPulse(enabled = true, periodMillis = 4_000)
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.cd_maintenance_due_home),
                modifier = modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.82f + pulse * 0.12f),
            )
        }
    }
}

@Composable
fun ActivityStatusChip(
    kind: PrinterActivityKind,
    progressCompact: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val style = activityChipStyle(kind = kind)
    val label = activityChipLabel(kind, progressCompact)
    val printingPulse = rememberAttentionPulse(kind == PrinterActivityKind.Printing, periodMillis = 3_000)
    val pausedPulse = rememberAttentionPulse(kind == PrinterActivityKind.Paused, periodMillis = 3_000)
    val errorPulse = rememberAttentionPulse(kind == PrinterActivityKind.Error, periodMillis = 4_500)
    val containerAlpha = when (kind) {
        PrinterActivityKind.Printing -> 0.12f + printingPulse * 0.05f
        PrinterActivityKind.Paused -> 0.14f + pausedPulse * 0.06f
        PrinterActivityKind.Error -> 0.14f + errorPulse * 0.05f
        else -> null
    }
    val borderAlpha = when (kind) {
        PrinterActivityKind.Printing -> 0.48f + printingPulse * 0.12f
        PrinterActivityKind.Paused -> 0.52f + pausedPulse * 0.14f
        PrinterActivityKind.Error -> 0.5f + errorPulse * 0.16f
        else -> 0.55f
    }
    val iconTintAlpha = when (kind) {
        PrinterActivityKind.Printing -> 0.88f + printingPulse * 0.1f
        PrinterActivityKind.Paused -> 0.9f + pausedPulse * 0.08f
        PrinterActivityKind.Error -> 0.88f + errorPulse * 0.1f
        else -> 1f
    }
    StatusPill(
        label = label,
        icon = style.icon,
        iconTint = style.accent.copy(alpha = iconTintAlpha),
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
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.buddyDashClickable(onClick = onClick) else Modifier,
        ),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .then(
                    if (contentDescription != null) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    }
                ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = iconTint,
            )
            FadeValueText(
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
