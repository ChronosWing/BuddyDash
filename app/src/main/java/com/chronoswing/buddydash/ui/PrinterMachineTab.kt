package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrinterMachineInfo
import com.chronoswing.buddydash.data.model.PrinterSmartPlugState
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.data.model.SmartOutletPowerState
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.MachineStepFilterChip
import com.chronoswing.buddydash.ui.components.MachineUtilityButton
import com.chronoswing.buddydash.ui.components.MotionControlsSection
import com.chronoswing.buddydash.ui.components.PrinterCameraFullscreenDialog
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.util.BED_JOG_STEP_OPTIONS_MM
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.buildMachineInfoRows
import com.chronoswing.buddydash.util.formatSmartPlugCurrent
import com.chronoswing.buddydash.util.formatSmartPlugPowerWatts
import com.chronoswing.buddydash.util.formatSmartPlugVoltage
import com.chronoswing.buddydash.util.formatStatusUpdatedAgo
import com.chronoswing.buddydash.util.machineTabCapabilities
import com.chronoswing.buddydash.util.requiresActivePowerOffConfirmation

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MachineTab(
    labels: PrinterDetailLabels,
    printerModel: String?,
    machineInfo: PrinterMachineInfo?,
    smartPlugState: PrinterSmartPlugState?,
    printerStatus: PrinterStatus?,
    powerControlsEnabled: Boolean,
    cameraToken: String,
    serverUrl: String,
    printerId: Int,
    bedJogStepMm: Float,
    isControlBusy: Boolean,
    statusUpdatedAtMillis: Long?,
    onBedJogStepChange: (Float) -> Unit,
    onJogBedUp: () -> Unit,
    onJogBedDown: () -> Unit,
    onHomePrinter: () -> Unit,
    onPowerOn: () -> Unit,
    onPowerOff: () -> Unit,
    onRequiresConnectionTap: () -> Unit,
    onToggleLight: (() -> Unit)? = null,
    onOpenPrinterArchives: () -> Unit = {},
    onStopCameraStream: () -> Unit = {},
) {
    val caps = labels.machineTabCapabilities(cameraTokenConfigured = cameraToken.isNotBlank())
    var showCameraFullscreen by rememberSaveable { mutableStateOf(false) }
    var showHomeConfirm by remember { mutableStateOf(false) }
    var showPowerOffConfirm by remember { mutableStateOf(false) }

    PrinterCameraFullscreenDialog(
        visible = showCameraFullscreen,
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        printerModel = printerModel,
        onDismiss = { showCameraFullscreen = false },
        onStopCameraStream = onStopCameraStream,
        chamberLightOn = labels.chamberLightOn,
        canToggleLight = labels.canToggleLight && caps.utilitiesEnabled,
        onToggleLight = onToggleLight,
    )

    if (showHomeConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHomeConfirm = false },
            title = { Text(stringResource(R.string.machine_home_confirm_title)) },
            text = { Text(stringResource(R.string.machine_home_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHomeConfirm = false
                        onHomePrinter()
                    },
                ) {
                    Text(stringResource(R.string.machine_home))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHomeConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showPowerOffConfirm) {
        val activeConfirm = printerStatus.requiresActivePowerOffConfirmation()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPowerOffConfirm = false },
            title = {
                Text(
                    stringResource(
                        if (activeConfirm) {
                            R.string.machine_power_off_active_confirm_title
                        } else {
                            R.string.machine_power_off_confirm_title
                        },
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (activeConfirm) {
                            R.string.machine_power_off_active_confirm_message
                        } else {
                            R.string.machine_power_off_confirm_message
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPowerOffConfirm = false
                        onPowerOff()
                    },
                ) {
                    Text(stringResource(R.string.machine_power_off))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPowerOffConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (caps.showMotionSection) {
            DetailInfoCard {
                SectionHeader(stringResource(R.string.machine_section_motion))
                BedJogStepSelector(
                    selectedStepMm = bedJogStepMm,
                    enabled = !isControlBusy,
                    onSelect = onBedJogStepChange,
                )
                MotionControlsSection(
                    layout = labels.motionLayout,
                    canUseMotion = caps.motionEnabled,
                    actionsEnabled = !isControlBusy && caps.motionEnabled,
                    stepMm = bedJogStepMm,
                    onJogUp = onJogBedUp,
                    onJogDown = onJogBedDown,
                    compactButtons = true,
                )
                caps.motionDisabledReason?.let { reason ->
                    MachineDisabledHint(reasonCode = reason)
                }
            }
        }

        if (caps.showUtilitiesSection) {
            DetailInfoCard {
                SectionHeader(stringResource(R.string.machine_section_utilities))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (caps.showCamera) {
                        MachineUtilityButton(
                            label = stringResource(R.string.camera_view),
                            icon = Icons.Default.Videocam,
                            enabled = caps.cameraEnabled && !isControlBusy,
                            onClick = { showCameraFullscreen = true },
                        )
                    }
                    if (caps.showHome) {
                        MachineUtilityButton(
                            label = stringResource(R.string.machine_home),
                            icon = Icons.Default.Home,
                            enabled = caps.homeEnabled && !isControlBusy,
                            onClick = { showHomeConfirm = true },
                        )
                    }
                    if (caps.showFiles) {
                        MachineUtilityButton(
                            label = stringResource(R.string.machine_files),
                            icon = Icons.Default.Folder,
                            enabled = caps.filesEnabled,
                            onClick = onOpenPrinterArchives,
                        )
                    }
                }
                if (!caps.utilitiesEnabled) {
                    caps.utilitiesDisabledReason?.let { reason ->
                        MachineDisabledHint(reasonCode = reason)
                    }
                }
            }
        }

        smartPlugState?.let { plug ->
            SmartPlugPowerSection(
                plug = plug,
                actionsEnabled = !isControlBusy,
                powerControlsEnabled = powerControlsEnabled,
                onPowerOn = onPowerOn,
                onPowerOff = { showPowerOffConfirm = true },
                onRequiresConnectionTap = onRequiresConnectionTap,
            )
        }

        MachinePrinterInfoCard(
            labels = labels,
            machineInfo = machineInfo,
            printerModel = printerModel,
            statusUpdatedAtMillis = statusUpdatedAtMillis,
        )
    }
}

@Composable
private fun SmartPlugPowerSection(
    plug: PrinterSmartPlugState,
    actionsEnabled: Boolean,
    powerControlsEnabled: Boolean,
    onPowerOn: () -> Unit,
    onPowerOff: () -> Unit,
    onRequiresConnectionTap: () -> Unit,
) {
    val energy = plug.energy
    val powerStateLabel = when (plug.displayPowerState) {
        SmartOutletPowerState.On -> stringResource(R.string.machine_power_state_on)
        SmartOutletPowerState.Off -> stringResource(R.string.machine_power_state_off)
        SmartOutletPowerState.Unknown -> stringResource(R.string.machine_power_state_unknown)
    }
    val lastUpdated = formatStatusUpdatedAgo(plug.lastUpdatedAtMillis)

    DetailInfoCard {
        SectionHeader(stringResource(R.string.machine_section_power))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactLabelValue(
                label = stringResource(R.string.machine_power_state),
                value = powerStateLabel,
            )
            formatSmartPlugPowerWatts(energy)?.let { watts ->
                CompactLabelValue(label = stringResource(R.string.machine_power_draw), value = watts)
            }
            formatSmartPlugVoltage(energy)?.let { volts ->
                CompactLabelValue(label = stringResource(R.string.machine_power_voltage), value = volts)
            }
            formatSmartPlugCurrent(energy)?.let { amps ->
                CompactLabelValue(label = stringResource(R.string.machine_power_current), value = amps)
            }
            lastUpdated?.let { updated ->
                CompactLabelValue(
                    label = stringResource(R.string.machine_info_last_updated),
                    value = updated,
                )
            }
        }
        Column(
            modifier = Modifier.alpha(if (powerControlsEnabled) 1f else 0.55f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MachineUtilityButton(
                label = stringResource(R.string.machine_power_on),
                icon = Icons.Default.PowerSettingsNew,
                enabled = actionsEnabled,
                onClick = {
                    if (powerControlsEnabled) onPowerOn() else onRequiresConnectionTap()
                },
            )
            MachineUtilityButton(
                label = stringResource(R.string.machine_power_off),
                icon = Icons.Default.PowerSettingsNew,
                enabled = actionsEnabled,
                onClick = {
                    if (powerControlsEnabled) onPowerOff() else onRequiresConnectionTap()
                },
            )
        }
        if (!powerControlsEnabled) {
            MachineDisabledHint(reasonCode = "requires_connection")
        }
    }
}

@Composable
private fun BedJogStepSelector(
    selectedStepMm: Float,
    enabled: Boolean,
    onSelect: (Float) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BED_JOG_STEP_OPTIONS_MM.forEach { step ->
            MachineStepFilterChip(
                label = stringResource(R.string.machine_step_mm, step.toInt()),
                selected = selectedStepMm == step,
                enabled = enabled,
                onClick = { onSelect(step) },
            )
        }
    }
}

@Composable
private fun MachineDisabledHint(reasonCode: String) {
    val text = when (reasonCode) {
        "offline" -> stringResource(R.string.machine_disabled_offline)
        "requires_connection" -> stringResource(R.string.requires_connection)
        "busy" -> stringResource(R.string.machine_disabled_busy)
        "not_supported" -> stringResource(R.string.machine_disabled_unsupported)
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun MachinePrinterInfoCard(
    labels: PrinterDetailLabels,
    machineInfo: PrinterMachineInfo?,
    printerModel: String?,
    statusUpdatedAtMillis: Long?,
) {
    val rows = buildMachineInfoRows(labels, machineInfo, printerModel, statusUpdatedAtMillis)
    if (rows.isEmpty()) return
    DetailInfoCard {
        SectionHeader(stringResource(R.string.machine_section_printer_info))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { (key, value) ->
                val label = when (key) {
                    "connection" -> stringResource(R.string.connection)
                    "state" -> stringResource(R.string.machine_info_state)
                    "model" -> stringResource(R.string.machine_info_model)
                    "firmware" -> stringResource(R.string.firmware)
                    "ip" -> stringResource(R.string.machine_info_ip)
                    "wifi_signal" -> stringResource(R.string.machine_info_wifi_signal)
                    "lan_mode" -> stringResource(R.string.machine_info_lan_mode)
                    "serial" -> stringResource(R.string.machine_info_serial)
                    "nozzle_count" -> stringResource(R.string.machine_info_nozzle_count)
                    "developer_mode" -> stringResource(R.string.machine_info_developer_mode)
                    "print_hours" -> stringResource(R.string.machine_info_print_hours)
                    "auto_archive" -> stringResource(R.string.machine_info_auto_archive)
                    else -> key
                }
                CompactLabelValue(label = label, value = value)
            }
        }
    }
}
