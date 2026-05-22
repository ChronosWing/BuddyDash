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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.animateScrollBy
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.util.ControlAction
import com.chronoswing.buddydash.util.ControlFeedback
import com.chronoswing.buddydash.MaintenanceResetSnackbar
import com.chronoswing.buddydash.PlateClearSnackbar
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.StartQueuedPrintSnackbar
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.ui.components.DetailConnectivityCard
import com.chronoswing.buddydash.ui.components.DetailFansCard
import com.chronoswing.buddydash.ui.components.DetailMaintenanceCard
import com.chronoswing.buddydash.ui.components.DetailPrintSpeedCard
import com.chronoswing.buddydash.ui.components.FilamentAmsEnvironmentSection
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentDetailGroups
import com.chronoswing.buddydash.ui.components.FilamentUsageText
import com.chronoswing.buddydash.ui.components.MicroMotionProgressBar
import com.chronoswing.buddydash.ui.components.DetailPrintQueueSection
import com.chronoswing.buddydash.ui.components.PrinterStatusQuickActions
import com.chronoswing.buddydash.ui.components.SectionHeaderRow
import com.chronoswing.buddydash.ui.components.PrinterErrorDetailsCard
import com.chronoswing.buddydash.ui.components.DetailStatusHeroImage
import com.chronoswing.buddydash.ui.components.HighlightValue
import com.chronoswing.buddydash.ui.components.LifecyclePollingEffect
import com.chronoswing.buddydash.ui.components.PrintFileHighlightWithCover
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.ui.components.PrintTempsRow
import com.chronoswing.buddydash.ui.components.PrinterQuickStatusRow
import com.chronoswing.buddydash.network.printerCoverUrl
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.motion.BuddyDashTabFadeContainer
import com.chronoswing.buddydash.ui.components.SecondaryNote
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.ui.components.StatusLastUpdatedIndicator
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.buildPrintHeadline
import com.chronoswing.buddydash.util.StartNextQueuedPrintReadiness
import com.chronoswing.buddydash.util.toDetailLabels

private val detailTabs = listOf("Status", "Filament", "Machine")

@Composable
fun PrinterDetailScreen(
    printerId: Int,
    printerName: String,
    printerModel: String? = null,
    viewModel: PrinterDetailViewModel,
    onBack: () -> Unit,
    onViewFullQueue: () -> Unit,
    onOpenPrinterArchives: () -> Unit = {},
) {
    LaunchedEffect(printerId, printerName, printerModel) {
        viewModel.init(printerId, printerName, printerModel)
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

    val printerErrorNoDetails = stringResource(R.string.printer_error_no_details)
    val labels = uiState.status?.toDetailLabels(
        maintenanceItems = uiState.maintenanceItems,
        totalPrintHours = uiState.totalPrintHours,
        printerModel = uiState.printerModel ?: printerModel,
        activePrintFilamentUsage = uiState.activePrintFilamentUsage,
        printerErrorNoDetailsFallback = printerErrorNoDetails,
    )

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
        controlFeedback = uiState.controlFeedback,
        isMaintenanceResetBusy = uiState.isMaintenanceResetBusy,
        maintenanceResetSnackbar = uiState.maintenanceResetSnackbar,
        lastStatusUpdatedAtMillis = uiState.lastStatusUpdatedAtMillis,
        queueUpcoming = uiState.queueUpcoming,
        startNextQueuedPrintReadiness = uiState.startNextQueuedPrintReadiness,
        isStartingQueuedPrint = uiState.isStartingQueuedPrint,
        startQueuedPrintSnackbar = uiState.startQueuedPrintSnackbar,
        onBack = onBack,
        onRetry = viewModel::loadStatus,
        onRefresh = { viewModel.loadStatus(showLoading = false) },
        onPullRefresh = { viewModel.loadStatus(showLoading = false, fromPull = true) },
        onMarkPlateClear = viewModel::markPlateClear,
        onPlateClearSnackbarShown = viewModel::onPlateClearSnackbarShown,
        onControlFeedbackShown = viewModel::onControlFeedbackShown,
        onSetPrintSpeed = viewModel::setPrintSpeed,
        onPausePrint = viewModel::pausePrint,
        onResumePrint = viewModel::resumePrint,
        onStopPrint = viewModel::stopPrint,
        onToggleLight = viewModel::toggleChamberLight,
        bedJogStepMm = uiState.bedJogStepMm,
        machineInfo = uiState.machineInfo,
        printerModel = uiState.printerModel ?: printerModel,
        onBedJogStepChange = viewModel::setBedJogStepMm,
        onJogBedUp = viewModel::jogBedUp,
        onJogBedDown = viewModel::jogBedDown,
        onHomePrinter = viewModel::homePrinter,
        onPerformMaintenanceReset = viewModel::performMaintenanceReset,
        onMaintenanceResetSnackbarShown = viewModel::onMaintenanceResetSnackbarShown,
        onViewFullQueue = onViewFullQueue,
        onStartNextQueuedPrint = viewModel::startNextQueuedPrint,
        onStartQueuedPrintSnackbarShown = viewModel::onStartQueuedPrintSnackbarShown,
        onOpenPrinterArchives = onOpenPrinterArchives,
        onStopCameraStream = viewModel::stopCameraStream,
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
    controlFeedback: ControlFeedback?,
    isMaintenanceResetBusy: Boolean,
    maintenanceResetSnackbar: MaintenanceResetSnackbar?,
    lastStatusUpdatedAtMillis: Long?,
    queueUpcoming: List<PrintQueueJob>,
    startNextQueuedPrintReadiness: StartNextQueuedPrintReadiness,
    isStartingQueuedPrint: Boolean,
    startQueuedPrintSnackbar: StartQueuedPrintSnackbar?,
    onBack: () -> Unit,
    onViewFullQueue: () -> Unit,
    onOpenPrinterArchives: () -> Unit,
    onStopCameraStream: () -> Unit,
    onStartNextQueuedPrint: () -> Unit,
    onStartQueuedPrintSnackbarShown: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    onMarkPlateClear: () -> Unit,
    onPlateClearSnackbarShown: () -> Unit,
    onControlFeedbackShown: () -> Unit,
    onSetPrintSpeed: (Int) -> Unit,
    onPausePrint: () -> Unit,
    onResumePrint: () -> Unit,
    onStopPrint: () -> Unit,
    onToggleLight: () -> Unit,
    bedJogStepMm: Float,
    machineInfo: com.chronoswing.buddydash.data.model.PrinterMachineInfo?,
    printerModel: String?,
    onBedJogStepChange: (Float) -> Unit,
    onJogBedUp: () -> Unit,
    onJogBedDown: () -> Unit,
    onHomePrinter: () -> Unit,
    onPerformMaintenanceReset: (Int) -> Unit,
    onMaintenanceResetSnackbarShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val plateClearSuccessMessage = stringResource(R.string.plate_clear_success)
    val plateClearFailedMessage = stringResource(R.string.plate_clear_failed)
    val controlSuccessMessage = stringResource(R.string.control_success)
    val controlFailedMessage = stringResource(R.string.control_failed)
    val homeFailedMessage = stringResource(R.string.machine_home_failed)
    val bedJogFailedMessage = stringResource(R.string.machine_bed_jog_failed)
    val chamberLightFailedMessage = stringResource(R.string.machine_chamber_light_failed)
    val printStoppedSuccessMessage = stringResource(R.string.print_stopped_success)
    val printStoppedFailedMessage = stringResource(R.string.print_stopped_failed)
    val maintenanceResetSuccessMessage = stringResource(R.string.maintenance_reset_success)
    val maintenanceResetFailedMessage = stringResource(R.string.maintenance_reset_failed)
    val printStartedMessage = stringResource(R.string.archive_reprint_started)
    val startNextPrintFailedMessage = stringResource(R.string.start_next_print_failed)
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    var errorDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    val errorCardScrollOffset = remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(errorDetailsExpanded) {
        if (errorDetailsExpanded) {
            val target = errorCardScrollOffset.intValue.toFloat()
            val delta = target - scrollState.value
            if (delta > 0f) scrollState.animateScrollBy(delta)
        }
    }

    LaunchedEffect(plateClearSnackbar) {
        val message = when (plateClearSnackbar) {
            PlateClearSnackbar.Success -> plateClearSuccessMessage
            PlateClearSnackbar.Failed -> plateClearFailedMessage
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        onPlateClearSnackbarShown()
    }

    LaunchedEffect(controlFeedback) {
        val feedback = controlFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            controlFeedbackMessage(
                feedback = feedback,
                genericSuccessMessage = controlSuccessMessage,
                genericFailedMessage = controlFailedMessage,
                homeFailedMessage = homeFailedMessage,
                bedJogFailedMessage = bedJogFailedMessage,
                chamberLightFailedMessage = chamberLightFailedMessage,
                printStoppedSuccessMessage = printStoppedSuccessMessage,
                printStoppedFailedMessage = printStoppedFailedMessage,
            ),
        )
        onControlFeedbackShown()
    }

    LaunchedEffect(maintenanceResetSnackbar) {
        val message = when (maintenanceResetSnackbar) {
            MaintenanceResetSnackbar.Success -> maintenanceResetSuccessMessage
            MaintenanceResetSnackbar.Failed -> maintenanceResetFailedMessage
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        onMaintenanceResetSnackbarShown()
    }

    LaunchedEffect(startQueuedPrintSnackbar) {
        val message = when (startQueuedPrintSnackbar) {
            StartQueuedPrintSnackbar.Started -> printStartedMessage
            StartQueuedPrintSnackbar.Failed -> startNextPrintFailedMessage
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        onStartQueuedPrintSnackbarShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        StatusLastUpdatedIndicator(
                            lastUpdatedAtMillis = lastStatusUpdatedAtMillis,
                            isRefreshing = isRefreshing,
                            enabled = !isClearingPlate && !isControlBusy,
                            onRefresh = onRefresh,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
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
                                .verticalScroll(scrollState)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BuddyDashTabFadeContainer(selectedTab = selectedTab) {
                            when (selectedTab) {
                                0 -> StatusTab(
                                    labels = labels,
                                    errorDetailsExpanded = errorDetailsExpanded,
                                    onExpandErrorDetails = { errorDetailsExpanded = true },
                                    onErrorChipClick = {
                                        if (labels.printerErrorDisplay.hasKnownDetails ||
                                            labels.printerErrorDisplay.showCard
                                        ) {
                                            errorDetailsExpanded = true
                                        }
                                    },
                                    errorCardScrollOffset = errorCardScrollOffset,
                                    density = density,
                                    printerName = title,
                                    printerId = printerId,
                                    serverUrl = serverUrl,
                                    cameraToken = cameraToken,
                                    queueUpcoming = queueUpcoming,
                                    showStartNextPrint = queueUpcoming.isNotEmpty() &&
                                        !labels.isActivePrint &&
                                        BambuddyApi.hasQueueStartEndpoint,
                                    startNextQueuedPrintReadiness = startNextQueuedPrintReadiness,
                                    isStartingQueuedPrint = isStartingQueuedPrint,
                                    isClearingPlate = isClearingPlate,
                                    isControlBusy = isControlBusy,
                                    isMaintenanceResetBusy = isMaintenanceResetBusy,
                                    onMarkPlateClear = onMarkPlateClear,
                                    onPerformMaintenanceReset = onPerformMaintenanceReset,
                                    onViewFullQueue = onViewFullQueue,
                                    onStartNextQueuedPrint = onStartNextQueuedPrint,
                                    onToggleLight = onToggleLight,
                                    onPausePrint = onPausePrint,
                                    onResumePrint = onResumePrint,
                                    onStopPrint = onStopPrint,
                                )
                                1 -> FilamentTab(labels = labels)
                                2 -> MachineTab(
                                    labels = labels,
                                    printerModel = printerModel,
                                    machineInfo = machineInfo,
                                    cameraToken = cameraToken,
                                    serverUrl = serverUrl,
                                    printerId = printerId,
                                    bedJogStepMm = bedJogStepMm,
                                    isControlBusy = isControlBusy,
                                    statusUpdatedAtMillis = lastStatusUpdatedAtMillis,
                                    onBedJogStepChange = onBedJogStepChange,
                                    onJogBedUp = onJogBedUp,
                                    onJogBedDown = onJogBedDown,
                                    onHomePrinter = onHomePrinter,
                                    onToggleLight = onToggleLight,
                                    onOpenPrinterArchives = onOpenPrinterArchives,
                                    onStopCameraStream = onStopCameraStream,
                                )
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Status tab sections — keep as a vertical [Column]; never wrap in Box/AnimatedContent.
 * Order: camera → overview/print → queue → connectivity → fans → speed → maintenance.
 */
@Composable
private fun StatusTab(
    labels: PrinterDetailLabels,
    printerName: String,
    printerId: Int,
    serverUrl: String,
    cameraToken: String,
    queueUpcoming: List<PrintQueueJob>,
    showStartNextPrint: Boolean,
    startNextQueuedPrintReadiness: StartNextQueuedPrintReadiness,
    isStartingQueuedPrint: Boolean,
    isClearingPlate: Boolean,
    isMaintenanceResetBusy: Boolean,
    errorDetailsExpanded: Boolean,
    onExpandErrorDetails: () -> Unit,
    onErrorChipClick: () -> Unit,
    errorCardScrollOffset: androidx.compose.runtime.MutableIntState,
    density: androidx.compose.ui.unit.Density,
    onMarkPlateClear: () -> Unit,
    onPerformMaintenanceReset: (Int) -> Unit,
    onViewFullQueue: () -> Unit,
    onStartNextQueuedPrint: () -> Unit,
    onToggleLight: () -> Unit,
    onPausePrint: () -> Unit,
    onResumePrint: () -> Unit,
    onStopPrint: () -> Unit,
    isControlBusy: Boolean,
) {
    val quickActions: @Composable () -> Unit = {
        PrinterStatusQuickActions(
            labels = labels,
            printerName = printerName,
            showStartNextPrint = showStartNextPrint,
            startReadiness = startNextQueuedPrintReadiness,
            isStartingQueuedPrint = isStartingQueuedPrint,
            isControlBusy = isControlBusy,
            isClearingPlate = isClearingPlate,
            onToggleLight = onToggleLight,
            onPausePrint = onPausePrint,
            onResumePrint = onResumePrint,
            onStopPrint = onStopPrint,
            onStartNextQueuedPrint = onStartNextQueuedPrint,
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (labels.isActivePrint) {
            ActivePrintStatusTab(
                labels = labels,
                printerId = printerId,
                serverUrl = serverUrl,
                cameraToken = cameraToken,
                isClearingPlate = isClearingPlate,
                onMarkPlateClear = onMarkPlateClear,
                headerTrailing = quickActions,
                errorDetailsExpanded = errorDetailsExpanded,
                onExpandErrorDetails = onExpandErrorDetails,
                onErrorChipClick = onErrorChipClick,
                errorCardScrollOffset = errorCardScrollOffset,
                density = density,
            )
        } else {
            IdleStatusTab(
                labels = labels,
                printerId = printerId,
                serverUrl = serverUrl,
                cameraToken = cameraToken,
                isClearingPlate = isClearingPlate,
                onMarkPlateClear = onMarkPlateClear,
                headerTrailing = quickActions,
                errorDetailsExpanded = errorDetailsExpanded,
                onExpandErrorDetails = onExpandErrorDetails,
                onErrorChipClick = onErrorChipClick,
                errorCardScrollOffset = errorCardScrollOffset,
                density = density,
            )
        }
        if (queueUpcoming.isNotEmpty()) {
            DetailPrintQueueSection(
                jobs = queueUpcoming,
                serverUrl = serverUrl,
                cameraToken = cameraToken,
                onViewFullQueue = onViewFullQueue,
            )
        }
        DetailOperationalStats(
            labels = labels,
            isMaintenanceResetBusy = isMaintenanceResetBusy,
            onPerformMaintenanceReset = onPerformMaintenanceReset,
        )
    }
}

@Composable
private fun DetailOperationalStats(
    labels: PrinterDetailLabels,
    isMaintenanceResetBusy: Boolean,
    onPerformMaintenanceReset: (Int) -> Unit,
) {
    DetailConnectivityCard(labels)
    DetailFansCard(labels)
    DetailPrintSpeedCard(labels)
    DetailMaintenanceCard(
        labels = labels,
        resetBusy = isMaintenanceResetBusy,
        onPerformReset = onPerformMaintenanceReset,
    )
}

@Composable
private fun ActivePrintStatusTab(
    labels: PrinterDetailLabels,
    printerId: Int,
    serverUrl: String,
    cameraToken: String,
    isClearingPlate: Boolean,
    onMarkPlateClear: () -> Unit,
    headerTrailing: @Composable () -> Unit,
    errorDetailsExpanded: Boolean,
    onExpandErrorDetails: () -> Unit,
    onErrorChipClick: () -> Unit,
    errorCardScrollOffset: androidx.compose.runtime.MutableIntState,
    density: androidx.compose.ui.unit.Density,
) {
    var cameraHeroActive by remember { mutableStateOf(false) }
    DetailStatusHeroImage(
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        motion = labels.cardMicroMotion,
        onCameraHeroActive = { cameraHeroActive = it },
    )
    DetailInfoCard {
        SectionHeaderRow(
            title = stringResource(R.string.section_print),
            trailing = headerTrailing,
        )
        PrinterQuickStatusRow(
            activityKind = labels.activityKind,
            progressCompact = labels.progressCompact,
            plateKind = labels.plateKind,
            onErrorChipClick = onErrorChipClick.takeIf {
                labels.printerErrorDisplay.showCard
            },
        )
        PrinterErrorDetailsCard(
            display = labels.printerErrorDisplay,
            expanded = errorDetailsExpanded,
            onExpand = onExpandErrorDetails,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                errorCardScrollOffset.intValue = with(density) {
                    coordinates.positionInParent().y.toDp().roundToPx()
                }
            },
        )
        HighlightValue(
            label = labels.progressTitle,
            value = buildPrintHeadline(labels.currentActivity, labels.progressValue),
        )
        labels.progressFraction?.let { fraction ->
            MicroMotionProgressBar(
                progress = { fraction.coerceIn(0f, 1f) },
                motion = labels.cardMicroMotion,
                modifier = Modifier.height(3.dp),
            )
        }
        if (labels.showFile) {
            PrintFileHighlightWithCover(
                label = labels.fileLabel,
                fileName = labels.fileName,
                serverUrl = serverUrl,
                cameraToken = cameraToken,
                printerId = printerId,
                showCoverThumbnail = cameraHeroActive,
            )
        }
        if (labels.showEta) {
            CompactLabelValue(label = stringResource(R.string.eta), value = labels.eta)
        }
        labels.filamentUsageCompact?.let { usage ->
            FilamentUsageText(
                text = usage,
                modifier = Modifier.padding(top = 2.dp),
            )
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
    headerTrailing: @Composable () -> Unit,
    errorDetailsExpanded: Boolean,
    onExpandErrorDetails: () -> Unit,
    onErrorChipClick: () -> Unit,
    errorCardScrollOffset: androidx.compose.runtime.MutableIntState,
    density: androidx.compose.ui.unit.Density,
) {
    var cameraHeroActive by remember { mutableStateOf(false) }
    DetailStatusHeroImage(
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        onCameraHeroActive = { cameraHeroActive = it },
    )
    DetailInfoCard {
        SectionHeaderRow(
            title = stringResource(R.string.section_overview),
            trailing = headerTrailing,
        )
        PrinterQuickStatusRow(
            activityKind = labels.activityKind,
            progressCompact = labels.progressCompact,
            plateKind = labels.plateKind,
            onErrorChipClick = onErrorChipClick.takeIf {
                labels.printerErrorDisplay.showCard
            },
        )
        PrinterErrorDetailsCard(
            display = labels.printerErrorDisplay,
            expanded = errorDetailsExpanded,
            onExpand = onExpandErrorDetails,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                errorCardScrollOffset.intValue = with(density) {
                    coordinates.positionInParent().y.toDp().roundToPx()
                }
            },
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
                if (cameraHeroActive && printerCoverUrl(serverUrl, printerId, cameraToken) != null) {
                    PrintFileHighlightWithCover(
                        label = labels.fileLabel,
                        fileName = labels.fileName,
                        serverUrl = serverUrl,
                        cameraToken = cameraToken,
                        printerId = printerId,
                        showCoverThumbnail = true,
                    )
                } else {
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

private fun controlFeedbackMessage(
    feedback: ControlFeedback,
    genericSuccessMessage: String,
    genericFailedMessage: String,
    homeFailedMessage: String,
    bedJogFailedMessage: String,
    chamberLightFailedMessage: String,
    printStoppedSuccessMessage: String,
    printStoppedFailedMessage: String,
): String {
    if (feedback.success) {
        return when (feedback.action) {
            ControlAction.Stop -> printStoppedSuccessMessage
            else -> genericSuccessMessage
        }
    }
    return when (feedback.action) {
        ControlAction.HomeAxes -> homeFailedMessage
        ControlAction.BedJog -> bedJogFailedMessage
        ControlAction.ChamberLight -> chamberLightFailedMessage
        ControlAction.Stop -> printStoppedFailedMessage
        else -> genericFailedMessage
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
    FilamentDetailGroups(
        slots = slots,
        activeKey = labels.activeFilamentSlot,
        cardMicroMotion = labels.cardMicroMotion,
        modifier = Modifier.fillMaxWidth(),
    )
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
