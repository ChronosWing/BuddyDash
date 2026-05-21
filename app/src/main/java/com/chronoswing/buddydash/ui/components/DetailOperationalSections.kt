package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.util.MaintenanceLine
import com.chronoswing.buddydash.util.MaintenanceLineKind
import com.chronoswing.buddydash.util.PrintSpeedMode
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.formatAmsHumidityCompact
import com.chronoswing.buddydash.util.formatAmsTempCompact
import com.chronoswing.buddydash.util.PrinterMotionLayout
import com.chronoswing.buddydash.util.formatFanPercentCompact
import com.chronoswing.buddydash.util.maintenanceDisplayLines

@Composable
fun DetailConnectivityCard(labels: PrinterDetailLabels) {
    if (!labels.showConnectivitySection) return
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_connectivity))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactIconStatsFlowRow {
                labels.wifiCompact?.let { value ->
                    CompactIconStat(
                        icon = Icons.Default.Wifi,
                        value = value,
                        contentDescription = stringResource(R.string.cd_wifi, value),
                    )
                }
                labels.doorLine?.let { value ->
                    CompactIconStat(
                        icon = Icons.Default.DoorFront,
                        value = value,
                        contentDescription = stringResource(R.string.cd_door, value),
                    )
                }
                labels.chamberTempCompact?.let { value ->
                    CompactIconStat(
                        icon = Icons.Default.Thermostat,
                        value = value,
                        contentDescription = stringResource(R.string.cd_chamber_temp, value),
                    )
                }
                labels.totalPrintTimeCompact?.let { value ->
                    CompactIconStat(
                        icon = Icons.Default.Schedule,
                        value = value,
                        contentDescription = stringResource(R.string.cd_total_print_time, value),
                    )
                }
                labels.nozzleDiameterCompact?.let { value ->
                    CompactIconStat(
                        icon = Icons.Outlined.Circle,
                        value = value,
                        contentDescription = stringResource(R.string.cd_nozzle_diameter, value),
                    )
                }
            }
            labels.firmwareLine?.let { value ->
                Text(
                    text = stringResource(R.string.firmware_compact, value),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
fun DetailFansCard(labels: PrinterDetailLabels) {
    if (!labels.showFansSection) return
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_fans))
        CompactIconStatsFlowRow {
            labels.partFanPercent?.let { percent ->
                formatFanPercentCompact(percent)?.let { value ->
                    FanCompactStat(
                        microLabel = stringResource(R.string.fan_tag_part),
                        value = value,
                        contentDescription = stringResource(R.string.cd_part_fan, value),
                    )
                }
            }
            labels.auxFanPercent?.let { percent ->
                formatFanPercentCompact(percent)?.let { value ->
                    FanCompactStat(
                        microLabel = stringResource(R.string.fan_tag_aux),
                        value = value,
                        contentDescription = stringResource(R.string.cd_aux_fan, value),
                    )
                }
            }
            labels.chamberFanPercent?.let { percent ->
                formatFanPercentCompact(percent)?.let { value ->
                    FanCompactStat(
                        microLabel = stringResource(R.string.fan_tag_chamber),
                        value = value,
                        contentDescription = stringResource(R.string.cd_chamber_fan, value),
                    )
                }
            }
        }
    }
}

@Composable
fun DetailPrintSpeedCard(labels: PrinterDetailLabels) {
    val speed = labels.printSpeedLabel ?: return
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_print_speed))
        CompactIconStat(
            icon = Icons.Default.Speed,
            value = speed,
            contentDescription = stringResource(R.string.cd_print_speed, speed),
        )
    }
}

@Composable
private fun FanCompactStat(
    microLabel: String,
    value: String,
    contentDescription: String,
) {
    CompactIconStat(
        icon = Icons.Default.Air,
        microLabel = microLabel,
        value = value,
        contentDescription = contentDescription,
    )
}

@Composable
private fun MaintenanceStatusRow(
    line: MaintenanceLine,
    resetBusy: Boolean,
    onResetClick: (() -> Unit)?,
) {
    val dueSoonTint = Color(0xFFFBBF24)
    val icon: ImageVector
    val accentColor: Color
    val nameColor: Color
    val remainingColor: Color
    when (line.kind) {
        MaintenanceLineKind.Ok -> {
            icon = Icons.Default.CheckCircle
            accentColor = MaterialTheme.colorScheme.primary
            nameColor = MaterialTheme.colorScheme.onSurface
            remainingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        }
        MaintenanceLineKind.DueSoon -> {
            icon = Icons.Default.Warning
            accentColor = dueSoonTint
            nameColor = MaterialTheme.colorScheme.onSurface
            remainingColor = dueSoonTint
        }
        MaintenanceLineKind.Due -> {
            icon = Icons.Default.Build
            accentColor = MaterialTheme.colorScheme.error
            nameColor = MaterialTheme.colorScheme.onSurface
            remainingColor = MaterialTheme.colorScheme.error
        }
    }
    val progressTrack = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val progressColor = when (line.kind) {
        MaintenanceLineKind.Ok -> accentColor.copy(alpha = 0.45f)
        MaintenanceLineKind.DueSoon -> accentColor.copy(alpha = 0.75f)
        MaintenanceLineKind.Due -> accentColor
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.cd_maintenance_item, line.name),
                modifier = Modifier.size(16.dp),
                tint = accentColor,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = line.name,
                style = MaterialTheme.typography.bodySmall,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 6.dp),
            )
            line.remainingText?.let { remaining ->
                Text(
                    text = remaining,
                    style = MaterialTheme.typography.labelSmall,
                    color = remainingColor,
                    maxLines = 1,
                    modifier = Modifier.padding(end = if (onResetClick != null) 4.dp else 0.dp),
                )
            }
            if (onResetClick != null) {
                TextButton(
                    onClick = onResetClick,
                    enabled = !resetBusy,
                    modifier = Modifier.heightIn(max = 32.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.maintenance_reset),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        line.progressFraction?.let { fraction ->
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp)
                    .height(2.dp),
                color = progressColor,
                trackColor = progressTrack,
            )
        }
    }
}

@Composable
fun DetailMaintenanceCard(
    labels: PrinterDetailLabels,
    resetBusy: Boolean,
    onPerformReset: (itemId: Int) -> Unit,
) {
    val lines = maintenanceDisplayLines(labels.maintenanceItems)
    if (lines.isEmpty()) return
    var pendingReset by remember { mutableStateOf<MaintenanceLine?>(null) }

    pendingReset?.let { line ->
        AlertDialog(
            onDismissRequest = { pendingReset = null },
            title = { Text(stringResource(R.string.maintenance_reset_confirm_title)) },
            text = { Text(stringResource(R.string.maintenance_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingReset = null
                        onPerformReset(line.itemId)
                    },
                ) {
                    Text(stringResource(R.string.maintenance_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReset = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_maintenance))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            lines.forEach { line ->
                MaintenanceStatusRow(
                    line = line,
                    resetBusy = resetBusy,
                    onResetClick = if (line.canReset) {
                        { pendingReset = line }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun AmsEnvironmentUnitRow(unit: AmsUnitInfo) {
    val temp = formatAmsTempCompact(unit.tempC)
    val humidity = formatAmsHumidityCompact(unit.humidityPercent)
    if (temp == null && humidity == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = unit.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CompactIconStatsFlowRow {
            temp?.let { value ->
                CompactIconStat(
                    icon = Icons.Default.Thermostat,
                    value = value,
                    contentDescription = stringResource(R.string.cd_ams_temp, unit.label, value),
                )
            }
            humidity?.let { value ->
                CompactIconStat(
                    icon = Icons.Default.WaterDrop,
                    value = value,
                    contentDescription = stringResource(R.string.cd_ams_humidity, unit.label, value),
                )
            }
        }
    }
}

@Composable
fun FilamentAmsEnvironmentSection(labels: PrinterDetailLabels) {
    val unitsWithEnv = labels.amsUnits.filter { unit ->
        formatAmsTempCompact(unit.tempC) != null || formatAmsHumidityCompact(unit.humidityPercent) != null
    }
    if (unitsWithEnv.isEmpty()) return
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_ams_environment))
        unitsWithEnv.forEach { unit ->
            AmsEnvironmentUnitRow(unit)
        }
    }
}

@Composable
fun MotionControlsSection(
    layout: PrinterMotionLayout,
    canUseMotion: Boolean,
    actionsEnabled: Boolean,
    stepMm: Float,
    onJogUp: () -> Unit,
    onJogDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (layout == PrinterMotionLayout.Hidden) return
    val stepLabel = stepMm.toInt().toString()
    val (upLabel, downLabel) = when (layout) {
        PrinterMotionLayout.BedSlingerZ -> stringResource(R.string.toolhead_up) to
            stringResource(R.string.toolhead_down)
        PrinterMotionLayout.ZBedJog -> stringResource(R.string.bed_up) to
            stringResource(R.string.bed_down)
        PrinterMotionLayout.Hidden -> return
    }
    val controlsEnabled = canUseMotion && actionsEnabled

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MotionJogButton(
                label = upLabel,
                icon = Icons.Default.KeyboardArrowUp,
                enabled = controlsEnabled,
                onClick = onJogUp,
                modifier = Modifier.weight(1f),
            )
            MotionJogButton(
                label = downLabel,
                icon = Icons.Default.KeyboardArrowDown,
                enabled = controlsEnabled,
                onClick = onJogDown,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.motion_step_hint, stepLabel),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!canUseMotion) {
            Text(
                text = stringResource(R.string.motion_controls_unavailable),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun MotionJogButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        ),
        border = BorderStroke(
            width = if (enabled) 1.5.dp else 1.dp,
            color = if (enabled) {
                accent.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
            },
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun ChamberLightControlChip(
    isOn: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = isOn,
        onClick = { if (enabled) onToggle() },
        enabled = enabled,
        modifier = modifier,
        label = {
            Text(
                stringResource(
                    if (isOn) R.string.light_on else R.string.light_off,
                ),
            )
        },
        leadingIcon = if (isOn) {
            {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrintSpeedControlChips(
    currentLevel: Int?,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrintSpeedMode.entries.forEach { mode ->
            FilterChip(
                selected = currentLevel == mode.apiValue,
                onClick = { if (enabled) onSelect(mode.apiValue) },
                enabled = enabled,
                label = { Text(mode.label) },
                leadingIcon = if (currentLevel == mode.apiValue) {
                    {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
