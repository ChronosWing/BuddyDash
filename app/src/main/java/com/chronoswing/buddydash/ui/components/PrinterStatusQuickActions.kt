package com.chronoswing.buddydash.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.PrinterActivityKind
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.StartNextQueuedPrintReadiness

@Composable
fun PrinterStatusQuickActions(
    labels: PrinterDetailLabels,
    printerName: String,
    showStartNextPrint: Boolean,
    startReadiness: StartNextQueuedPrintReadiness,
    isStartingQueuedPrint: Boolean,
    isControlBusy: Boolean,
    isClearingPlate: Boolean,
    onToggleLight: () -> Unit,
    onPausePrint: () -> Unit,
    onResumePrint: () -> Unit,
    onStopPrint: () -> Unit,
    onStartNextQueuedPrint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!shouldShowStatusQuickActions(labels)) return

    var showStopConfirm by remember { mutableStateOf(false) }
    var showStartConfirm by remember { mutableStateOf(false) }
    val actionsEnabled = !isClearingPlate && !isControlBusy

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text(stringResource(R.string.stop_print_confirm_title)) },
            text = { Text(stringResource(R.string.stop_print_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirm = false
                        onStopPrint()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.stop_print_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showStartConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isStartingQueuedPrint) showStartConfirm = false },
            title = { Text(stringResource(R.string.start_next_print_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.start_next_print_confirm_message,
                        printerName,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartConfirm = false
                        onStartNextQueuedPrint()
                    },
                    enabled = !isStartingQueuedPrint,
                ) {
                    Text(stringResource(R.string.start_next_print_confirm_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStartConfirm = false },
                    enabled = !isStartingQueuedPrint,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    val showLight = labels.canToggleLight
    val showPause = labels.canPause
    val showResume = labels.canResume
    val showStop = labels.canStop
    val showStart = showStartNextPrint &&
        !labels.isActivePrint &&
        labels.activityKind == PrinterActivityKind.Idle

    if (!showLight && !showPause && !showResume && !showStop && !showStart) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLight) {
            ChamberLightIconButton(
                isOn = labels.chamberLightOn == true,
                enabled = actionsEnabled,
                onClick = onToggleLight,
                contentDescription = stringResource(
                    if (labels.chamberLightOn == true) {
                        R.string.light_on
                    } else {
                        R.string.light_off
                    },
                ),
            )
        }
        if (showStart) {
            StartNextQueueChip(
                enabled = startReadiness.canStart && !isStartingQueuedPrint && actionsEnabled,
                isSubmitting = isStartingQueuedPrint,
                onClick = { showStartConfirm = true },
            )
        }
        if (showResume) {
            StatusIconButton(
                onClick = onResumePrint,
                enabled = actionsEnabled,
                tint = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.resume_print),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (showPause) {
            StatusIconButton(
                onClick = onPausePrint,
                enabled = actionsEnabled,
                tint = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = stringResource(R.string.pause_print),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (showStop) {
            StatusIconButton(
                onClick = { if (actionsEnabled) showStopConfirm = true },
                enabled = actionsEnabled,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = stringResource(R.string.stop_print),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(36.dp),
    ) {
        if (tint == Color.Unspecified) {
            icon()
        } else {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides tint.copy(
                    alpha = if (enabled) 1f else 0.35f,
                ),
            ) {
                icon()
            }
        }
    }
}

private fun shouldShowStatusQuickActions(labels: PrinterDetailLabels): Boolean =
    labels.activityKind != PrinterActivityKind.Offline

/** Compact ▶ Queue chip — clearer than a lone play icon for starting the next queued job. */
@Composable
private fun StartNextQueueChip(
    enabled: Boolean,
    isSubmitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(8.dp)
    val chipAlpha = if (enabled) 1f else 0.42f

    val actionLabel = stringResource(R.string.start_next_print_action)

    Surface(
        modifier = modifier
            .height(28.dp)
            .semantics { contentDescription = actionLabel }
            .clickable(enabled = enabled && !isSubmitting) { onClick() },
        shape = shape,
        color = primary.copy(alpha = 0.14f * chipAlpha),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.38f * chipAlpha)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = primary.copy(alpha = chipAlpha),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = primary.copy(alpha = chipAlpha),
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = stringResource(R.string.start_next_queue_chip),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = primary.copy(alpha = chipAlpha),
            )
        }
    }
}

private const val CHAMBER_LIGHT_GLOW_CYCLE_MS = 2_800

/**
 * Chamber light toggle with breathing bloom when ON (primary cyan).
 * Glow is drawn in a non-clipping layer so the pulse stays visible inside the icon button.
 */
@Composable
private fun ChamberLightIconButton(
    isOn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val glowBreath = rememberChamberLightGlowBreath(isOn)
    val litColor = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
    val innerGlowAlpha = if (isOn) 0.22f + glowBreath * 0.14f else 0f
    val outerGlowAlpha = if (isOn) 0.1f + glowBreath * 0.08f else 0f
    val iconAlpha = when {
        !enabled -> 0.35f
        isOn -> 0.94f + glowBreath * 0.06f
        else -> 0.42f
    }
    val iconScale = if (isOn) 1f + glowBreath * 0.04f else 1f

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(36.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer { clip = false },
            contentAlignment = Alignment.Center,
        ) {
            if (isOn) {
                Box(
                    Modifier
                        .size(40.dp)
                        .graphicsLayer { clip = false }
                        .drawBehind {
                            val center = this.center
                            val outerRadius = size.minDimension * 0.62f
                            val innerRadius = size.minDimension * 0.38f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        litColor.copy(alpha = outerGlowAlpha),
                                        litColor.copy(alpha = outerGlowAlpha * 0.4f),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = outerRadius,
                                ),
                                radius = outerRadius,
                                center = center,
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        litColor.copy(alpha = innerGlowAlpha),
                                        litColor.copy(alpha = innerGlowAlpha * 0.55f),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = innerRadius,
                                ),
                                radius = innerRadius,
                                center = center,
                            )
                        },
                )
            }
            Icon(
                imageVector = if (isOn) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                contentDescription = contentDescription,
                tint = if (isOn) litColor.copy(alpha = iconAlpha) else muted,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        clip = false
                    },
            )
        }
    }
}

@Composable
private fun rememberChamberLightGlowBreath(active: Boolean): Float {
    if (!active) return 0f
    val transition = rememberInfiniteTransition(label = "chamberLightGlow")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CHAMBER_LIGHT_GLOW_CYCLE_MS,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chamberLightGlowBreath",
    )
    return breath
}
