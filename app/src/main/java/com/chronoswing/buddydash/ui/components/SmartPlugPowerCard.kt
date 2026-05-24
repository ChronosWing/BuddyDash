package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrinterSmartPlugState
import com.chronoswing.buddydash.data.model.SmartOutletPowerState
import com.chronoswing.buddydash.ui.motion.buddyDashButtonPress
import com.chronoswing.buddydash.ui.motion.rememberBuddyDashInteractionSource
import com.chronoswing.buddydash.ui.theme.OnlineGreen
import com.chronoswing.buddydash.util.formatHeroPowerWatts
import com.chronoswing.buddydash.util.formatSmartPlugPowerFactor
import com.chronoswing.buddydash.util.formatSmartPlugStatCurrent
import com.chronoswing.buddydash.util.formatSmartPlugStatVoltage
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun SmartPlugPowerCard(
    plug: PrinterSmartPlugState,
    powerHistory: List<Float>,
    actionsEnabled: Boolean,
    powerControlsEnabled: Boolean,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onRequiresConnectionTap: () -> Unit,
    modifier: Modifier = Modifier,
    dashboardCompact: Boolean = false,
) {
    val energy = plug.energy
    val powerState = plug.displayPowerState
    val primary = MaterialTheme.colorScheme.primary
    val heroWatts = formatHeroPowerWatts(energy)
    val voltage = formatSmartPlugStatVoltage(energy)
    val current = formatSmartPlugStatCurrent(energy)
    val powerFactor = formatSmartPlugPowerFactor(energy)
    val showGraph = powerHistory.size >= 2
    val cardAlpha = if (powerControlsEnabled) 1f else 0.88f
    val outletSize = if (dashboardCompact) 56.dp else 72.dp
    val heroFontSize = if (dashboardCompact) 26.sp else 32.sp
    val sparklineHeight = if (dashboardCompact) 48.dp else 72.dp

    DetailInfoCard(modifier = modifier.alpha(cardAlpha)) {
        PowerCardHeader(
            subtitle = plug.config.name.ifBlank { stringResource(R.string.machine_power_smart_outlet) },
            powerState = powerState,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PowerOutletGlowIcon(
                modifier = Modifier.size(outletSize),
                tint = primary,
                isOn = powerState == SmartOutletPowerState.On,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.machine_power_live_draw),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
                Text(
                    text = heroWatts ?: "—",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = heroFontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (voltage != null || current != null || powerFactor != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                voltage?.let {
                    PowerStatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.ShowChart,
                        label = stringResource(R.string.machine_power_voltage),
                        value = it,
                        unit = stringResource(R.string.machine_power_unit_ac),
                    )
                }
                current?.let {
                    PowerStatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt,
                        label = stringResource(R.string.machine_power_current),
                        value = it,
                        unit = stringResource(R.string.machine_power_unit_ac),
                    )
                }
                powerFactor?.let {
                    PowerStatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Speed,
                        label = stringResource(R.string.machine_power_factor),
                        value = it,
                        unit = stringResource(R.string.machine_power_unit_pf),
                    )
                }
            }
        }

        if (showGraph) {
            PowerSparkline(
                samples = powerHistory,
                lineColor = primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sparklineHeight),
            )
        }

        val interactionSource = rememberBuddyDashInteractionSource()
        val actionEnabled = actionsEnabled
        val actionLabel = when (powerState) {
            SmartOutletPowerState.On -> stringResource(R.string.machine_power_turn_off)
            SmartOutletPowerState.Off,
            SmartOutletPowerState.Unknown,
            -> stringResource(R.string.machine_power_turn_on)
        }
        val actionClick = {
            if (!powerControlsEnabled) {
                onRequiresConnectionTap()
            } else when (powerState) {
                SmartOutletPowerState.On -> onTurnOff()
                else -> onTurnOn()
            }
        }

        OutlinedButton(
            onClick = actionClick,
            enabled = actionEnabled,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .buddyDashButtonPress(actionEnabled, interactionSource)
                .machineActionShadow(actionEnabled),
            shape = RoundedCornerShape(12.dp),
            colors = machineActionButtonColors(actionEnabled && powerControlsEnabled),
            border = machineActionBorder(actionEnabled && powerControlsEnabled),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 10.dp,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (!powerControlsEnabled) {
            Text(
                text = stringResource(R.string.requires_connection),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun PowerCardHeader(
    subtitle: String,
    powerState: SmartOutletPowerState,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = primary.copy(alpha = 0.95f),
                )
                Text(
                    text = stringResource(R.string.machine_section_power),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primary.copy(alpha = 0.92f),
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
        PowerStatePill(powerState = powerState)
    }
}

@Composable
private fun PowerStatePill(powerState: SmartOutletPowerState) {
    val (dotColor, label, containerAlpha) = when (powerState) {
        SmartOutletPowerState.On -> Triple(OnlineGreen, stringResource(R.string.machine_power_state_on_upper), 0.18f)
        SmartOutletPowerState.Off -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            stringResource(R.string.machine_power_state_off_upper),
            0.12f,
        )
        SmartOutletPowerState.Unknown -> Triple(
            MaterialTheme.colorScheme.outline,
            stringResource(R.string.machine_power_state_unknown_upper),
            0.10f,
        )
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = dotColor.copy(alpha = containerAlpha),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = dotColor,
            ) {}
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            )
        }
    }
}

@Composable
private fun PowerStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            0.75.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = primary.copy(alpha = 0.75f),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun PowerOutletGlowIcon(
    tint: Color,
    isOn: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val glowAlpha = if (isOn) 0.28f else 0.12f
            listOf(0.95f, 0.72f, 0.48f).forEach { scale ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tint.copy(alpha = glowAlpha),
                            tint.copy(alpha = 0f),
                        ),
                        center = center,
                        radius = size.minDimension * scale * 0.5f,
                    ),
                    radius = size.minDimension * scale * 0.5f,
                    center = center,
                )
            }
        }
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = tint.copy(alpha = if (isOn) 0.95f else 0.55f),
        )
    }
}

@Composable
private fun PowerSparkline(
    samples: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxSample = samples.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val yMax = max(60f, ceil(maxSample / 30f) * 30f)
    val yMid = yMax / 2f

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${yMax.toInt()} W",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                    Text(
                        text = "${yMid.toInt()} W",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                    Text(
                        text = "0 W",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                }
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 4.dp),
                ) {
                    val chartLeft = 0f
                    val chartRight = size.width
                    val chartTop = 0f
                    val chartBottom = size.height
                    val chartHeight = chartBottom - chartTop

                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = chartBottom - chartHeight * fraction
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(chartLeft, y),
                            end = Offset(chartRight, y),
                            strokeWidth = 1f,
                        )
                    }

                    if (samples.size < 2) return@Canvas

                    val stepX = (chartRight - chartLeft) / (samples.size - 1).coerceAtLeast(1)
                    fun yFor(value: Float): Float {
                        val clamped = value.coerceIn(0f, yMax)
                        return chartBottom - (clamped / yMax) * chartHeight
                    }

                    val fillPath = Path().apply {
                        moveTo(chartLeft, yFor(samples.first()))
                        samples.forEachIndexed { index, sample ->
                            lineTo(chartLeft + stepX * index, yFor(sample))
                        }
                        lineTo(chartLeft + stepX * (samples.lastIndex), chartBottom)
                        lineTo(chartLeft, chartBottom)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.22f),
                                lineColor.copy(alpha = 0.02f),
                            ),
                            startY = chartTop,
                            endY = chartBottom,
                        ),
                    )

                    val linePath = Path().apply {
                        moveTo(chartLeft, yFor(samples.first()))
                        samples.forEachIndexed { index, sample ->
                            if (index == 0) return@forEachIndexed
                            lineTo(chartLeft + stepX * index, yFor(sample))
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = lineColor.copy(alpha = 0.9f),
                        style = Stroke(width = 2f, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.machine_power_graph_ago),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
            Text(
                text = stringResource(R.string.machine_power_graph_now),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}
