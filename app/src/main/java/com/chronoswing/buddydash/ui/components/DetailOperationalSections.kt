package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.util.MaintenanceLine
import com.chronoswing.buddydash.util.MaintenanceLineKind
import com.chronoswing.buddydash.util.PrintSpeedMode
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.formatAmsHumidityCompact
import com.chronoswing.buddydash.util.formatAmsTempCompact
import com.chronoswing.buddydash.util.formatFanPercentCompact
import com.chronoswing.buddydash.util.maintenanceDisplayLines

@Composable
fun DetailConnectivityCard(labels: PrinterDetailLabels) {
    if (!labels.showConnectivitySection) return
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_connectivity))
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
            labels.firmwareLine?.let { value ->
                CompactIconStat(
                    icon = Icons.Default.Memory,
                    value = value,
                    contentDescription = stringResource(R.string.cd_firmware, value),
                )
            }
            labels.chamberTempCompact?.let { value ->
                CompactIconStat(
                    icon = Icons.Default.Thermostat,
                    value = value,
                    contentDescription = stringResource(R.string.cd_chamber_temp, value),
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
                    CompactIconStat(
                        icon = Icons.Default.Air,
                        value = value,
                        contentDescription = stringResource(R.string.cd_part_fan, value),
                    )
                }
            }
            labels.auxFanPercent?.let { percent ->
                formatFanPercentCompact(percent)?.let { value ->
                    CompactIconStat(
                        icon = Icons.Default.Air,
                        value = value,
                        contentDescription = stringResource(R.string.cd_aux_fan, value),
                    )
                }
            }
            labels.chamberFanPercent?.let { percent ->
                formatFanPercentCompact(percent)?.let { value ->
                    CompactIconStat(
                        icon = Icons.Default.Air,
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
private fun MaintenanceStatusRow(line: MaintenanceLine) {
    val (icon, tint, suffix) = when (line.kind) {
        MaintenanceLineKind.Healthy -> Triple(
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primary,
            stringResource(R.string.maintenance_ok_suffix),
        )
        MaintenanceLineKind.Warning -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.error,
            stringResource(R.string.maintenance_soon_suffix),
        )
        MaintenanceLineKind.Due -> Triple(
            Icons.Default.Build,
            MaterialTheme.colorScheme.error,
            stringResource(R.string.maintenance_due_suffix),
        )
    }
    val label = "${line.name} $suffix".trim()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.cd_maintenance_item, label),
            modifier = Modifier.size(18.dp),
            tint = tint,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (line.kind == MaintenanceLineKind.Healthy) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
fun DetailMaintenanceCard(labels: PrinterDetailLabels) {
    val lines = maintenanceDisplayLines(labels.maintenanceItems)
    if (lines.isEmpty()) return
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_maintenance))
        lines.forEach { line ->
            MaintenanceStatusRow(line)
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
