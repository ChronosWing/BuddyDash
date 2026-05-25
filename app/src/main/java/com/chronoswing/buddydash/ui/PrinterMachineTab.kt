package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrinterMachineInfo
import com.chronoswing.buddydash.data.model.PrinterSmartPlugState
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.ui.components.CopyableCompactLabelValue
import com.chronoswing.buddydash.ui.components.CompactInfoGrid
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.MachineStepFilterChip
import com.chronoswing.buddydash.ui.components.MachineUtilityButton
import com.chronoswing.buddydash.ui.components.MotionControlsSection
import com.chronoswing.buddydash.ui.components.PrinterCameraFullscreenDialog
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.ui.components.SmartPlugPowerCard
import com.chronoswing.buddydash.ui.layout.rememberIsBuddyDashExpandedWidth
import com.chronoswing.buddydash.util.BED_JOG_STEP_OPTIONS_MM
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.buildMachineInfoRows
import com.chronoswing.buddydash.util.machineTabCapabilities
import com.chronoswing.buddydash.util.MachineTabCapabilities
import com.chronoswing.buddydash.util.requiresActivePowerOffConfirmation
import androidx.compose.ui.Alignment

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MachineTab(
    labels: PrinterDetailLabels,
    printerModel: String?,
    machineInfo: PrinterMachineInfo?,
    smartPlugState: PrinterSmartPlugState?,
    smartPlugPowerHistory: List<Float>,
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
    onCopyPrinterId: () -> Unit = {},
    onCopyNfcClearPlateLink: () -> Unit = {},
) {
    val caps = labels.machineTabCapabilities(cameraTokenConfigured = cameraToken.isNotBlank())
    val isExpandedWidth = rememberIsBuddyDashExpandedWidth()
    val useDashboardRow = isExpandedWidth &&
        (caps.showMotionSection || caps.showUtilitiesSection)
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
                    Text(stringResource(R.string.machine_power_turn_off))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPowerOffConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (useDashboardRow) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (caps.showMotionSection) {
                    MachineMotionCard(
                        labels = labels,
                        caps = caps,
                        bedJogStepMm = bedJogStepMm,
                        isControlBusy = isControlBusy,
                        onBedJogStepChange = onBedJogStepChange,
                        onJogBedUp = onJogBedUp,
                        onJogBedDown = onJogBedDown,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (caps.showUtilitiesSection) {
                    MachineUtilitiesCard(
                        caps = caps,
                        isControlBusy = isControlBusy,
                        onShowCamera = { showCameraFullscreen = true },
                        onShowHomeConfirm = { showHomeConfirm = true },
                        onOpenPrinterArchives = onOpenPrinterArchives,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        } else {
            if (caps.showMotionSection) {
                MachineMotionCard(
                    labels = labels,
                    caps = caps,
                    bedJogStepMm = bedJogStepMm,
                    isControlBusy = isControlBusy,
                    onBedJogStepChange = onBedJogStepChange,
                    onJogBedUp = onJogBedUp,
                    onJogBedDown = onJogBedDown,
                )
            }

            if (caps.showUtilitiesSection) {
                MachineUtilitiesCard(
                    caps = caps,
                    isControlBusy = isControlBusy,
                    onShowCamera = { showCameraFullscreen = true },
                    onShowHomeConfirm = { showHomeConfirm = true },
                    onOpenPrinterArchives = onOpenPrinterArchives,
                )
            }
        }

        val infoRows = buildMachineInfoRows(
            labels,
            machineInfo,
            printerModel,
            printerId,
            statusUpdatedAtMillis,
        )
        if (isExpandedWidth) {
            when {
                smartPlugState != null && infoRows.isNotEmpty() -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SmartPlugPowerCard(
                            plug = smartPlugState,
                            powerHistory = smartPlugPowerHistory,
                            actionsEnabled = !isControlBusy,
                            powerControlsEnabled = powerControlsEnabled,
                            onTurnOn = onPowerOn,
                            onTurnOff = { showPowerOffConfirm = true },
                            onRequiresConnectionTap = onRequiresConnectionTap,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            dashboardCompact = true,
                        )
                        MachinePrinterInfoCard(
                            labels = labels,
                            machineInfo = machineInfo,
                            printerModel = printerModel,
                            printerId = printerId,
                            statusUpdatedAtMillis = statusUpdatedAtMillis,
                            useInfoGrid = true,
                            onCopyPrinterId = onCopyPrinterId,
                            onCopyNfcClearPlateLink = onCopyNfcClearPlateLink,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                smartPlugState != null -> {
                    SmartPlugPowerCard(
                        plug = smartPlugState,
                        powerHistory = smartPlugPowerHistory,
                        actionsEnabled = !isControlBusy,
                        powerControlsEnabled = powerControlsEnabled,
                        onTurnOn = onPowerOn,
                        onTurnOff = { showPowerOffConfirm = true },
                        onRequiresConnectionTap = onRequiresConnectionTap,
                        dashboardCompact = true,
                    )
                }
                infoRows.isNotEmpty() -> {
                    MachinePrinterInfoCard(
                        labels = labels,
                        machineInfo = machineInfo,
                        printerModel = printerModel,
                        printerId = printerId,
                        statusUpdatedAtMillis = statusUpdatedAtMillis,
                        useInfoGrid = true,
                        onCopyPrinterId = onCopyPrinterId,
                        onCopyNfcClearPlateLink = onCopyNfcClearPlateLink,
                    )
                }
            }
        } else {
            smartPlugState?.let { plug ->
                SmartPlugPowerCard(
                    plug = plug,
                    powerHistory = smartPlugPowerHistory,
                    actionsEnabled = !isControlBusy,
                    powerControlsEnabled = powerControlsEnabled,
                    onTurnOn = onPowerOn,
                    onTurnOff = { showPowerOffConfirm = true },
                    onRequiresConnectionTap = onRequiresConnectionTap,
                )
            }

            MachinePrinterInfoCard(
                labels = labels,
                machineInfo = machineInfo,
                printerModel = printerModel,
                printerId = printerId,
                statusUpdatedAtMillis = statusUpdatedAtMillis,
                useInfoGrid = false,
                onCopyPrinterId = onCopyPrinterId,
                onCopyNfcClearPlateLink = onCopyNfcClearPlateLink,
            )
        }
    }
}

@Composable
private fun MachineMotionCard(
    labels: PrinterDetailLabels,
    caps: MachineTabCapabilities,
    bedJogStepMm: Float,
    isControlBusy: Boolean,
    onBedJogStepChange: (Float) -> Unit,
    onJogBedUp: () -> Unit,
    onJogBedDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailInfoCard(modifier = modifier) {
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

@Composable
private fun MachineUtilitiesCard(
    caps: MachineTabCapabilities,
    isControlBusy: Boolean,
    onShowCamera: () -> Unit,
    onShowHomeConfirm: () -> Unit,
    onOpenPrinterArchives: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailInfoCard(modifier = modifier) {
        SectionHeader(stringResource(R.string.machine_section_utilities))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (caps.showCamera) {
                MachineUtilityButton(
                    label = stringResource(R.string.camera_view),
                    icon = Icons.Default.Videocam,
                    enabled = caps.cameraEnabled && !isControlBusy,
                    onClick = onShowCamera,
                )
            }
            if (caps.showHome) {
                MachineUtilityButton(
                    label = stringResource(R.string.machine_home),
                    icon = Icons.Default.Home,
                    enabled = caps.homeEnabled && !isControlBusy,
                    onClick = onShowHomeConfirm,
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
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun MachinePrinterInfoCard(
    labels: PrinterDetailLabels,
    machineInfo: PrinterMachineInfo?,
    printerModel: String?,
    printerId: Int,
    statusUpdatedAtMillis: Long?,
    useInfoGrid: Boolean,
    onCopyPrinterId: () -> Unit,
    onCopyNfcClearPlateLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = buildMachineInfoRows(
        labels,
        machineInfo,
        printerModel,
        printerId,
        statusUpdatedAtMillis,
    )
    if (rows.isEmpty()) return
    val printerIdValue = printerId.toString()
    val copyPrinterIdLabel = stringResource(R.string.machine_copy_printer_id)
    val labeledRows = rows.map { (key, value) ->
        val label = when (key) {
            "connection" -> stringResource(R.string.connection)
            "state" -> stringResource(R.string.machine_info_state)
            "printer_id" -> stringResource(R.string.machine_info_printer_id)
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
        label to value
    }
    DetailInfoCard(modifier = modifier) {
        SectionHeader(stringResource(R.string.machine_section_printer_info))
        if (useInfoGrid) {
            CompactInfoGrid(
                rows = labeledRows,
                copyableValues = setOf(printerIdValue),
                onCopyValue = { onCopyPrinterId() },
                copyContentDescription = copyPrinterIdLabel,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                labeledRows.forEach { (label, value) ->
                    if (value == printerIdValue) {
                        CopyableCompactLabelValue(
                            label = label,
                            value = value,
                            onCopy = onCopyPrinterId,
                            copyContentDescription = copyPrinterIdLabel,
                        )
                    } else {
                        CompactLabelValue(label = label, value = value)
                    }
                }
            }
        }
        MachineUtilityButton(
            label = stringResource(R.string.machine_copy_nfc_link),
            icon = Icons.Default.ContentCopy,
            enabled = true,
            onClick = onCopyNfcClearPlateLink,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
