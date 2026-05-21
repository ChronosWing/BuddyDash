package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.ControlSnackbar
import com.chronoswing.buddydash.PlateClearSnackbar
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.ui.components.ComingSoonActionButton
import com.chronoswing.buddydash.ui.components.DetailConnectivityCard
import com.chronoswing.buddydash.ui.components.DetailFansCard
import com.chronoswing.buddydash.ui.components.DetailMaintenanceCard
import com.chronoswing.buddydash.ui.components.DetailPrintSpeedCard
import com.chronoswing.buddydash.ui.components.FilamentAmsEnvironmentSection
import com.chronoswing.buddydash.ui.components.BedAdjustControl
import com.chronoswing.buddydash.ui.components.BedAdjustOptionsDialog
import com.chronoswing.buddydash.ui.components.ChamberLightControlChip
import com.chronoswing.buddydash.ui.components.PrintSpeedControlChips
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentChipRow
import com.chronoswing.buddydash.ui.components.FilamentSlotDetailHeader
import com.chronoswing.buddydash.ui.components.HighlightValue
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.components.PrintFileHighlight
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.ui.components.PrintTempsRow
import com.chronoswing.buddydash.ui.components.PrinterCoverImage
import com.chronoswing.buddydash.ui.components.PrinterQuickStatusRow
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.components.SecondaryNote
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.buildPrintHeadline
import com.chronoswing.buddydash.util.normalizeFilamentType
import com.chronoswing.buddydash.util.BED_JOG_STEP_MM
import com.chronoswing.buddydash.util.toDetailLabels

private val detailTabs = listOf("Status", "Filament", "Controls")

@Composable
fun PrinterDetailScreen(
    printerId: Int,
    printerName: String,
    viewModel: PrinterDetailViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(printerId, printerName) {
        viewModel.init(printerId, printerName)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecyclePollingEffect(
        enabled = uiState.hasCredentials && printerId >= 0,
        intervalMs = 5_000L,
        onPoll = {
            val showLoading = uiState.status == null && uiState.error == null && !uiState.isClearingPlate
            viewModel.loadStatus(showLoading = showLoading)
        },
    )

    val labels = uiState.status?.toDetailLabels(uiState.maintenanceItems)

    PrinterDetailScreenContent(
        title = uiState.printerName.ifBlank { printerName },
        printerId = printerId,
        serverUrl = uiState.serverUrl,
        cameraToken = uiState.cameraToken,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        error = uiState.error,
        labels = labels,
        isClearingPlate = uiState.isClearingPlate,
        isControlBusy = uiState.isControlBusy,
        plateClearSnackbar = uiState.plateClearSnackbar,
        controlSnackbar = uiState.controlSnackbar,
        onBack = onBack,
        onRetry = viewModel::loadStatus,
        onRefresh = { viewModel.loadStatus(showLoading = false) },
        onPullRefresh = { viewModel.loadStatus(showLoading = false, fromPull = true) },
        onMarkPlateClear = viewModel::markPlateClear,
        onPlateClearSnackbarShown = viewModel::onPlateClearSnackbarShown,
        onControlSnackbarShown = viewModel::onControlSnackbarShown,
        onSetPrintSpeed = viewModel::setPrintSpeed,
        onPausePrint = viewModel::pausePrint,
        onResumePrint = viewModel::resumePrint,
        onStopPrint = viewModel::stopPrint,
        onToggleLight = viewModel::toggleChamberLight,
        onJogBedUp = viewModel::jogBedUp,
        onJogBedDown = viewModel::jogBedDown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterDetailScreenContent(
    title: String,
    printerId: Int,
    serverUrl: String,
    cameraToken: String,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    labels: PrinterDetailLabels?,
    isClearingPlate: Boolean,
    isControlBusy: Boolean,
    plateClearSnackbar: PlateClearSnackbar?,
    controlSnackbar: ControlSnackbar?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    onMarkPlateClear: () -> Unit,
    onPlateClearSnackbarShown: () -> Unit,
    onControlSnackbarShown: () -> Unit,
    onSetPrintSpeed: (Int) -> Unit,
    onPausePrint: () -> Unit,
    onResumePrint: () -> Unit,
    onStopPrint: () -> Unit,
    onToggleLight: () -> Unit,
    onJogBedUp: () -> Unit,
    onJogBedDown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val plateClearSuccessMessage = stringResource(R.string.plate_clear_success)
    val plateClearFailedMessage = stringResource(R.string.plate_clear_failed)
    val controlSuccessMessage = stringResource(R.string.control_success)
    val controlFailedMessage = stringResource(R.string.control_failed)
    val printStoppedSuccessMessage = stringResource(R.string.print_stopped_success)
    val printStoppedFailedMessage = stringResource(R.string.print_stopped_failed)
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(plateClearSnackbar) {
        val message = when (plateClearSnackbar) {
            PlateClearSnackbar.Success -> plateClearSuccessMessage
            PlateClearSnackbar.Failed -> plateClearFailedMessage
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        onPlateClearSnackbarShown()
    }

    LaunchedEffect(controlSnackbar) {
        val message = when (controlSnackbar) {
            ControlSnackbar.Success -> controlSuccessMessage
            ControlSnackbar.Failed -> controlFailedMessage
            ControlSnackbar.StopSuccess -> printStoppedSuccessMessage
            ControlSnackbar.StopFailed -> printStoppedFailedMessage
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        onControlSnackbarShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            isLoading -> LoadingContent(Modifier.padding(innerPadding))
            error != null -> ErrorContent(
                message = error,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )
            labels == null -> Unit
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        detailTabs.forEachIndexed { index, tabTitle ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(tabTitle) },
                            )
                        }
                    }
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onPullRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            when (selectedTab) {
                                0 -> StatusTab(
                                    labels = labels,
                                    printerId = printerId,
                                    serverUrl = serverUrl,
                                    cameraToken = cameraToken,
                                    isClearingPlate = isClearingPlate,
                                    onMarkPlateClear = onMarkPlateClear,
                                )
                                1 -> FilamentTab(labels = labels)
                                2 -> ControlsTab(
                                    labels = labels,
                                    isClearingPlate = isClearingPlate,
                                    isControlBusy = isControlBusy,
                                    onRefresh = onRefresh,
                                    onMarkPlateClear = onMarkPlateClear,
                                    onSetPrintSpeed = onSetPrintSpeed,
                                    onPausePrint = onPausePrint,
                                    onResumePrint = onResumePrint,
                                    onStopPrint = onStopPrint,
                                    onToggleLight = onToggleLight,
                                    onJogBedUp = onJogBedUp,
                                    onJogBedDown = onJogBedDown,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTab(
    labels: PrinterDetailLabels,
    printerId: Int,
    serverUrl: String,
    cameraToken: String,
    isClearingPlate: Boolean,
    onMarkPlateClear: () -> Unit,
) {
    if (labels.isActivePrint) {
        ActivePrintStatusTab(
            labels = labels,
            printerId = printerId,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
            isClearingPlate = isClearingPlate,
            onMarkPlateClear = onMarkPlateClear,
        )
    } else {
        IdleStatusTab(
            labels = labels,
            printerId = printerId,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
            isClearingPlate = isClearingPlate,
            onMarkPlateClear = onMarkPlateClear,
        )
    }
    DetailOperationalStats(labels)
}

@Composable
private fun DetailOperationalStats(labels: PrinterDetailLabels) {
    DetailConnectivityCard(labels)
    DetailFansCard(labels)
    DetailPrintSpeedCard(labels)
    DetailMaintenanceCard(labels)
}

@Composable
private fun ActivePrintStatusTab(
    labels: PrinterDetailLabels,
    printerId: Int,
    serverUrl: String,
    cameraToken: String,
    isClearingPlate: Boolean,
    onMarkPlateClear: () -> Unit,
) {
    PrinterCoverImage(
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        height = 160.dp,
    )
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_print))
        PrinterQuickStatusRow(
            activityKind = labels.activityKind,
            progressCompact = labels.progressCompact,
            plateKind = labels.plateKind,
        )
        HighlightValue(
            label = labels.progressTitle,
            value = buildPrintHeadline(labels.currentActivity, labels.progressValue),
        )
        labels.progressFraction?.let { fraction ->
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
            )
        }
        if (labels.showFile) {
            PrintFileHighlight(
                label = labels.fileLabel,
                fileName = labels.fileName,
            )
        }
        if (labels.showEta) {
            CompactLabelValue(label = stringResource(R.string.eta), value = labels.eta)
        }
        if (labels.tempsLine != null) {
            PrintTempsRow(
                nozzleTemp = labels.nozzleTemp,
                bedTemp = labels.bedTemp,
                valueStyle = MaterialTheme.typography.titleMedium,
            )
        }
        CompactLabelValue(
            label = stringResource(R.string.current_activity),
            value = labels.currentActivity,
        )
        CompactLabelValue(
            label = stringResource(R.string.connection),
            value = labels.connection,
        )
        labels.plateStatus?.let { plate ->
            SecondaryNote(
                label = stringResource(R.string.plate_status),
                value = plate,
            )
        }
        if (labels.showPlateClearAction) {
            PlateClearButton(
                labels = labels,
                isClearingPlate = isClearingPlate,
                onClick = onMarkPlateClear,
            )
        }
    }
}

@Composable
private fun IdleStatusTab(
    labels: PrinterDetailLabels,
    printerId: Int,
    serverUrl: String,
    cameraToken: String,
    isClearingPlate: Boolean,
    onMarkPlateClear: () -> Unit,
) {
    PrinterCoverImage(
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        height = 160.dp,
    )
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_overview))
        PrinterQuickStatusRow(
            activityKind = labels.activityKind,
            progressCompact = labels.progressCompact,
            plateKind = labels.plateKind,
        )
        HighlightValue(
            label = stringResource(R.string.current_activity),
            value = labels.currentActivity,
        )
        CompactLabelValue(
            label = stringResource(R.string.connection),
            value = labels.connection,
        )
        labels.plateStatus?.let { plate ->
            CompactLabelValue(
                label = stringResource(R.string.plate_status),
                value = plate,
            )
        }
        if (labels.showPlateClearAction) {
            PlateClearButton(
                labels = labels,
                isClearingPlate = isClearingPlate,
                onClick = onMarkPlateClear,
            )
        }
    }

    val hasPrintSection = labels.showFile ||
        labels.lastPrintResult != null

    if (hasPrintSection) {
        DetailInfoCard {
            SectionHeader(stringResource(R.string.section_print))
            labels.lastPrintResult?.let { result ->
                SecondaryNote(
                    label = stringResource(R.string.last_print_result_short),
                    value = result,
                )
            }
            if (labels.showFile) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = labels.fileLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrintFileNameText(
                        fileName = labels.fileName.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_environment))
        CompactLabelValue(label = stringResource(R.string.nozzle_temp), value = labels.nozzleTemp)
        CompactLabelValue(label = stringResource(R.string.bed_temp), value = labels.bedTemp)
        CompactLabelValue(
            label = stringResource(R.string.hms_health),
            value = labels.hmsHealth,
            valueColor = if (labels.hmsHasErrors) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun FilamentTab(labels: PrinterDetailLabels) {
    val slots = labels.filamentSlots
    if (slots.isEmpty()) {
        Text(
            text = stringResource(R.string.no_filament_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FilamentAmsEnvironmentSection(labels)
    FilamentChipRow(slots = slots, compact = false, modifier = Modifier.fillMaxWidth())
    slots.forEach { slot ->
        val typeLabel = if (slot.isLoaded) {
            normalizeFilamentType(slot.filamentType)?.uppercase()
                ?: stringResource(R.string.filament_unknown)
        } else {
            stringResource(R.string.filament_empty)
        }
        DetailInfoCard {
            FilamentSlotDetailHeader(slot = slot, typeLabel = typeLabel)
            if (slot.isLoaded) {
                slot.remainPercent?.let { remain ->
                    CompactLabelValue(
                        label = stringResource(R.string.filament_remaining),
                        value = "$remain%",
                    )
                }
                slot.metadata?.let { meta ->
                    CompactLabelValue(label = stringResource(R.string.filament_metadata), value = meta)
                }
            }
        }
    }
}

@Composable
private fun ControlsTab(
    labels: PrinterDetailLabels,
    isClearingPlate: Boolean,
    isControlBusy: Boolean,
    onRefresh: () -> Unit,
    onMarkPlateClear: () -> Unit,
    onSetPrintSpeed: (Int) -> Unit,
    onPausePrint: () -> Unit,
    onResumePrint: () -> Unit,
    onStopPrint: () -> Unit,
    onToggleLight: () -> Unit,
    onJogBedUp: () -> Unit,
    onJogBedDown: () -> Unit,
) {
    val comingSoon = stringResource(R.string.coming_soon)
    var showStopConfirm by remember { mutableStateOf(false) }
    var showBedAdjustDialog by remember { mutableStateOf(false) }
    val actionsEnabled = !isClearingPlate && !isControlBusy

    BedAdjustOptionsDialog(
        visible = showBedAdjustDialog,
        stepMm = BED_JOG_STEP_MM,
        onDismiss = { showBedAdjustDialog = false },
        onRaiseBed = onJogBedUp,
        onLowerBed = onJogBedDown,
    )

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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(stringResource(R.string.controls_section_status))
        DetailInfoCard {
            Button(
                onClick = onRefresh,
                enabled = actionsEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.refresh_status))
            }
            if (labels.showPlateClearAction) {
                PlateClearButton(
                    labels = labels,
                    isClearingPlate = isClearingPlate,
                    onClick = onMarkPlateClear,
                )
            }
        }

        if (labels.canControlPrint) {
            SectionHeader(stringResource(R.string.controls_section_print))
            DetailInfoCard {
                PrintSpeedControlChips(
                    currentLevel = labels.speedLevel,
                    enabled = actionsEnabled,
                    onSelect = onSetPrintSpeed,
                )
            }
        }

        val hasQuickActions = labels.canPause || labels.canResume || labels.canStop ||
            labels.showBedAdjust || labels.canToggleLight
        if (hasQuickActions) {
            SectionHeader(stringResource(R.string.controls_section_quick_actions))
            DetailInfoCard {
                if (labels.canResume) {
                    Button(
                        onClick = onResumePrint,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.resume_print))
                    }
                } else if (labels.canPause) {
                    Button(
                        onClick = onPausePrint,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.pause_print))
                    }
                }
                if (labels.canStop) {
                    Button(
                        onClick = { showStopConfirm = true },
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(stringResource(R.string.stop_print))
                    }
                }
                if (labels.showBedAdjust) {
                    BedAdjustControl(
                        enabled = labels.canAdjustBed,
                        actionsEnabled = actionsEnabled,
                        onOpenOptions = { showBedAdjustDialog = true },
                    )
                }
                if (labels.canToggleLight) {
                    ChamberLightControlChip(
                        isOn = labels.chamberLightOn == true,
                        enabled = actionsEnabled,
                        onToggle = onToggleLight,
                    )
                }
            }
        }

        SectionHeader(stringResource(R.string.controls_section_more))
        DetailInfoCard {
            ComingSoonActionButton(
                label = stringResource(R.string.camera_view),
                helperText = comingSoon,
            )
            ComingSoonActionButton(
                label = stringResource(R.string.printer_files),
                helperText = comingSoon,
            )
        }
    }
}

@Composable
private fun PlateClearButton(
    labels: PrinterDetailLabels,
    isClearingPlate: Boolean,
    onClick: () -> Unit,
) {
    val endpointMissing = !labels.plateClearEndpointAvailable
    Button(
        onClick = onClick,
        enabled = !isClearingPlate && !endpointMissing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            when {
                endpointMissing -> stringResource(R.string.plate_clear_endpoint_not_found)
                isClearingPlate -> stringResource(R.string.marking_plate_clear)
                else -> stringResource(R.string.mark_plate_clear)
            },
        )
    }
}
