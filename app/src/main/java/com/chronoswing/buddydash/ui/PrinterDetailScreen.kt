package com.chronoswing.buddydash.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
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
import com.chronoswing.buddydash.FilamentAssignSnackbar
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.StartQueuedPrintSnackbar
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.util.FilamentSlotDisplay
import com.chronoswing.buddydash.util.evaluateFilamentAssignAvailability
import com.chronoswing.buddydash.util.buildFilamentSlotDisplays
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
import com.chronoswing.buddydash.ui.components.BuddyDashEmptyIcon
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.asImageVector
import com.chronoswing.buddydash.ui.components.FilamentAssignSpoolDialog
import com.chronoswing.buddydash.ui.components.FilamentClearAssignmentDialog
import com.chronoswing.buddydash.ui.components.FilamentDetailGroups
import com.chronoswing.buddydash.ui.components.FilamentSlotDetailSheet
import com.chronoswing.buddydash.ui.components.FilamentSpoolPickerSheet
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
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.motion.BuddyDashTabFadeContainer
import com.chronoswing.buddydash.ui.motion.HomeAtmosphericFade
import com.chronoswing.buddydash.ui.motion.SecondaryScreenHeader
import com.chronoswing.buddydash.ui.layout.BuddyDashExpandedDetailContainer
import com.chronoswing.buddydash.ui.layout.rememberBuddyDashExpandedGridColumnCount
import com.chronoswing.buddydash.ui.layout.rememberIsBuddyDashExpandedWidth
import com.chronoswing.buddydash.ui.components.SecondaryNote
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.ui.components.OfflineStaleBanner
import com.chronoswing.buddydash.ui.components.StatusLastUpdatedIndicator
import com.chronoswing.buddydash.util.showStaleDataBanner
import com.chronoswing.buddydash.util.staleBannerShowsRefreshFailed
import com.chronoswing.buddydash.util.ListLoadUi
import com.chronoswing.buddydash.util.HmsSeverity
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.buildPrintHeadline
import com.chronoswing.buddydash.util.StartNextQueuedPrintReadiness
import com.chronoswing.buddydash.util.toDetailLabels

private val detailTabs = listOf("Status", "Filament", "Machine")
private val HmsDetailAmber = androidx.compose.ui.graphics.Color(0xFFFBBF24)

@Composable
fun PrinterDetailScreen(
    printerId: Int,
    printerName: String,
    printerModel: String? = null,
    viewModel: PrinterDetailViewModel,
    onBack: () -> Unit,
    onViewFullQueue: () -> Unit,
    onOpenPrinterArchives: () -> Unit = {},
    onOpenSpoolDetail: (Int) -> Unit = {},
) {
    LaunchedEffect(printerId, printerName, printerModel) {
        viewModel.init(printerId, printerName, printerModel)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        hasCredentials = uiState.hasCredentials,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        hasAttemptedNetworkLoad = uiState.hasAttemptedNetworkLoad,
        error = uiState.error,
        refreshError = uiState.refreshError,
        isStaleCachedData = uiState.isStaleCachedData,
        isLimitedFromHomeCache = uiState.isLimitedFromHomeCache,
        hasCompletedLoad = uiState.hasCompletedLoad,
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
        onRetry = { viewModel.loadStatus() },
        onRefresh = { viewModel.loadStatus(showLoading = false, fromUser = true) },
        onPullRefresh = { viewModel.loadStatus(showLoading = false, fromPull = true) },
        onPollStatus = { showLoading ->
            viewModel.loadStatus(showLoading = showLoading)
        },
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
        smartPlugState = uiState.smartPlugState,
        smartPlugPowerHistory = uiState.smartPlugPowerHistory,
        powerControlsEnabled = !viewModel.requiresConnection,
        printerModel = uiState.printerModel ?: printerModel,
        onBedJogStepChange = viewModel::setBedJogStepMm,
        onJogBedUp = viewModel::jogBedUp,
        onJogBedDown = viewModel::jogBedDown,
        onHomePrinter = viewModel::homePrinter,
        onPowerOnSmartPlug = viewModel::powerOnSmartPlug,
        onPowerOffSmartPlug = viewModel::powerOffSmartPlug,
        onPerformMaintenanceReset = viewModel::performMaintenanceReset,
        onMaintenanceResetSnackbarShown = viewModel::onMaintenanceResetSnackbarShown,
        onViewFullQueue = onViewFullQueue,
        onStartNextQueuedPrint = viewModel::startNextQueuedPrint,
        onStartQueuedPrintSnackbarShown = viewModel::onStartQueuedPrintSnackbarShown,
        onOpenPrinterArchives = onOpenPrinterArchives,
        onStopCameraStream = viewModel::stopCameraStream,
        onOpenSpoolDetail = onOpenSpoolDetail,
        pendingSpoolDetailNavigationId = uiState.pendingSpoolDetailNavigationId,
        onConsumePendingSpoolDetailNavigation = viewModel::consumePendingSpoolDetailNavigation,
        onViewSpoolFromSlot = viewModel::viewSpoolFromSlot,
        onDismissFilamentTransientUi = viewModel::dismissFilamentTransientUi,
        filamentSlotDisplays = uiState.filamentSlotDisplays,
        printerStatus = uiState.status,
        printingQueueJobId = uiState.printingQueueJobId,
        filamentSlotSheet = uiState.filamentSlotSheet,
        filamentSpoolPickerOpen = uiState.filamentSpoolPickerOpen,
        spoolPickerSearchQuery = uiState.spoolPickerSearchQuery,
        assignSpoolConfirm = uiState.assignSpoolConfirm,
        clearAssignmentConfirm = uiState.clearAssignmentConfirm,
        isFilamentAssignBusy = uiState.isFilamentAssignBusy,
        filamentAssignSnackbar = uiState.filamentAssignSnackbar,
        pickerSpools = viewModel.spoolsForPicker(),
        assignConfirmSpoolTitle = viewModel.assignConfirmSpoolTitle(),
        onOpenFilamentSlot = viewModel::openFilamentSlotSheet,
        onDismissFilamentSlotSheet = viewModel::dismissFilamentSlotSheet,
        onOpenFilamentSpoolPicker = viewModel::openFilamentSpoolPicker,
        onDismissFilamentSpoolPicker = viewModel::dismissFilamentSpoolPicker,
        onSpoolPickerSearchChange = viewModel::onSpoolPickerSearchChange,
        onRequestAssignSpool = viewModel::requestAssignSpool,
        onDismissAssignSpoolConfirm = viewModel::dismissAssignSpoolConfirm,
        onConfirmAssignSpool = viewModel::confirmAssignSpool,
        onRequestClearAssignment = viewModel::requestClearSlotAssignment,
        onDismissClearAssignmentConfirm = viewModel::dismissClearAssignmentConfirm,
        onConfirmClearAssignment = viewModel::confirmClearSlotAssignment,
        onFilamentAssignSnackbarShown = viewModel::onFilamentAssignSnackbarShown,
        pickerAssignmentConflict = viewModel::pickerAssignmentConflict,
        pickerCardUsageFor = viewModel::pickerCardUsageFor,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterDetailScreenContent(
    title: String,
    printerId: Int,
    serverUrl: String,
    cameraToken: String,
    hasCredentials: Boolean,
    isLoading: Boolean,
    isRefreshing: Boolean,
    hasAttemptedNetworkLoad: Boolean,
    error: String?,
    refreshError: String?,
    isStaleCachedData: Boolean,
    isLimitedFromHomeCache: Boolean,
    hasCompletedLoad: Boolean,
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
    onOpenSpoolDetail: (Int) -> Unit,
    pendingSpoolDetailNavigationId: Int?,
    onConsumePendingSpoolDetailNavigation: () -> Int?,
    onViewSpoolFromSlot: (Int) -> Unit,
    onDismissFilamentTransientUi: () -> Unit,
    onStopCameraStream: () -> Unit,
    filamentSlotDisplays: List<FilamentSlotDisplay>,
    printerStatus: PrinterStatus?,
    printingQueueJobId: Int?,
    filamentSlotSheet: FilamentSlotDisplay?,
    filamentSpoolPickerOpen: Boolean,
    spoolPickerSearchQuery: String,
    assignSpoolConfirm: com.chronoswing.buddydash.AssignSpoolConfirm?,
    clearAssignmentConfirm: FilamentSlotDisplay?,
    isFilamentAssignBusy: Boolean,
    filamentAssignSnackbar: FilamentAssignSnackbar?,
    pickerSpools: List<SpoolInventoryItem>,
    assignConfirmSpoolTitle: String,
    onOpenFilamentSlot: (FilamentSlotDisplay) -> Unit,
    onDismissFilamentSlotSheet: () -> Unit,
    onOpenFilamentSpoolPicker: () -> Unit,
    onDismissFilamentSpoolPicker: () -> Unit,
    onSpoolPickerSearchChange: (String) -> Unit,
    onRequestAssignSpool: (SpoolInventoryItem) -> Unit,
    onDismissAssignSpoolConfirm: () -> Unit,
    onConfirmAssignSpool: () -> Unit,
    onRequestClearAssignment: () -> Unit,
    onDismissClearAssignmentConfirm: () -> Unit,
    onConfirmClearAssignment: () -> Unit,
    onFilamentAssignSnackbarShown: () -> Unit,
    pickerAssignmentConflict: (com.chronoswing.buddydash.data.model.SpoolInventoryItem) ->
        com.chronoswing.buddydash.util.SpoolAssignmentTargetConflict,
    pickerCardUsageFor: (com.chronoswing.buddydash.data.model.SpoolInventoryItem) ->
        com.chronoswing.buddydash.util.SpoolInventoryCardUsage,
    onStartNextQueuedPrint: () -> Unit,
    onStartQueuedPrintSnackbarShown: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    onPollStatus: (showLoading: Boolean) -> Unit,
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
    smartPlugState: com.chronoswing.buddydash.data.model.PrinterSmartPlugState?,
    smartPlugPowerHistory: List<Float>,
    powerControlsEnabled: Boolean,
    printerModel: String?,
    onBedJogStepChange: (Float) -> Unit,
    onJogBedUp: () -> Unit,
    onJogBedDown: () -> Unit,
    onHomePrinter: () -> Unit,
    onPowerOnSmartPlug: () -> Unit,
    onPowerOffSmartPlug: () -> Unit,
    onPerformMaintenanceReset: (Int) -> Unit,
    onMaintenanceResetSnackbarShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val hasCachedData = labels != null
    val showStaleBanner = hasAttemptedNetworkLoad && showStaleDataBanner(
        hasCachedContent = hasCachedData,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
        lastUpdatedAtMillis = lastStatusUpdatedAtMillis,
    )
    val staleBannerRefreshFailed = hasAttemptedNetworkLoad && staleBannerShowsRefreshFailed(
        hasCachedContent = hasCachedData,
        isStaleCachedData = isStaleCachedData,
        refreshError = refreshError,
    )
    // Keep skeleton visible until the first network attempt completes when there is no cached data.
    val showInitialLoading = !hasCompletedLoad || (!hasAttemptedNetworkLoad && labels == null && error == null)
    val showOfflineEmpty = hasCompletedLoad && hasAttemptedNetworkLoad && labels == null && error != null
    val showPullRefreshIndicator = ListLoadUi.showPullRefreshIndicator(
        isRefreshing = isRefreshing,
        cachedItemCount = if (hasCachedData) 1 else 0,
    )
    val plateClearSuccessMessage = stringResource(R.string.plate_clear_success)
    val plateClearFailedMessage = stringResource(R.string.plate_clear_failed)
    val controlSuccessMessage = stringResource(R.string.control_success)
    val homeStartedMessage = stringResource(R.string.machine_home_started)
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
    val requiresConnectionMessage = stringResource(R.string.requires_connection)
    val powerOnSuccessMessage = stringResource(R.string.machine_power_on_success)
    val powerOffSuccessMessage = stringResource(R.string.machine_power_off_success)
    val powerOnFailedMessage = stringResource(R.string.machine_power_on_failed)
    val powerOffFailedMessage = stringResource(R.string.machine_power_off_failed)
    val snackbarScope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val statusTabIndex = 0
    val pollIntervalMs = if (selectedTab == statusTabIndex) 5_000L else 15_000L
    LifecyclePollingEffect(
        enabled = hasCredentials && printerId >= 0,
        intervalMs = pollIntervalMs,
        initialDelayMs = pollIntervalMs,
        pollImmediately = false,
        onPoll = {
            val showLoading = labels == null && error == null && !isClearingPlate
            onPollStatus(showLoading)
        },
    )
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
                homeSuccessMessage = homeStartedMessage,
                genericFailedMessage = controlFailedMessage,
                homeFailedMessage = homeFailedMessage,
                bedJogFailedMessage = bedJogFailedMessage,
                chamberLightFailedMessage = chamberLightFailedMessage,
                printStoppedSuccessMessage = printStoppedSuccessMessage,
                printStoppedFailedMessage = printStoppedFailedMessage,
                powerOnSuccessMessage = powerOnSuccessMessage,
                powerOffSuccessMessage = powerOffSuccessMessage,
                powerOnFailedMessage = powerOnFailedMessage,
                powerOffFailedMessage = powerOffFailedMessage,
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

    val filamentAssignSuccessMessage = stringResource(R.string.filament_assign_success)
    val filamentAssignFailedMessage = stringResource(R.string.filament_assign_failed)
    val filamentClearSuccessMessage = stringResource(R.string.filament_clear_success)
    val filamentClearFailedMessage = stringResource(R.string.filament_clear_failed)

    LaunchedEffect(filamentAssignSnackbar) {
        val message = when (filamentAssignSnackbar) {
            FilamentAssignSnackbar.Assigned -> filamentAssignSuccessMessage
            FilamentAssignSnackbar.AssignFailed -> filamentAssignFailedMessage
            FilamentAssignSnackbar.Cleared -> filamentClearSuccessMessage
            FilamentAssignSnackbar.ClearFailed -> filamentClearFailedMessage
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        onFilamentAssignSnackbarShown()
    }

    val assignAvailability = evaluateFilamentAssignAvailability(printerStatus)
    val filamentTabIndex = 1

    LaunchedEffect(pendingSpoolDetailNavigationId) {
        val spoolId = onConsumePendingSpoolDetailNavigation() ?: return@LaunchedEffect
        onOpenSpoolDetail(spoolId)
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != filamentTabIndex) {
            onDismissFilamentTransientUi()
        }
    }

    filamentSlotSheet?.let { sheet ->
        FilamentSlotDetailSheet(
            display = sheet,
            assignAvailability = assignAvailability,
            isBusy = isFilamentAssignBusy,
            onDismiss = onDismissFilamentSlotSheet,
            onChangeSpool = onOpenFilamentSpoolPicker,
            onClearAssignment = onRequestClearAssignment,
            onViewSpool = onViewSpoolFromSlot,
        )
    }

    if (filamentSpoolPickerOpen) {
        val slotLabel = filamentSlotSheet?.slot?.label ?: ""
        FilamentSpoolPickerSheet(
            spools = pickerSpools,
            searchQuery = spoolPickerSearchQuery,
            slotLabel = slotLabel,
            onSearchQueryChange = onSpoolPickerSearchChange,
            onDismiss = onDismissFilamentSpoolPicker,
            onSpoolSelected = onRequestAssignSpool,
            assignmentConflictForSpool = pickerAssignmentConflict,
            cardUsageForSpool = pickerCardUsageFor,
        )
    }

    assignSpoolConfirm?.let { confirm ->
        FilamentAssignSpoolDialog(
            spoolTitle = assignConfirmSpoolTitle,
            slotLabel = confirm.slotDisplay.slot.label,
            printerName = title,
            conflict = confirm.conflict,
            isBusy = isFilamentAssignBusy,
            onConfirm = onConfirmAssignSpool,
            onDismiss = onDismissAssignSpoolConfirm,
        )
    }

    clearAssignmentConfirm?.let { slot ->
        FilamentClearAssignmentDialog(
            slotLabel = slot.slot.label,
            isBusy = isFilamentAssignBusy,
            onConfirm = onConfirmClearAssignment,
            onDismiss = onDismissClearAssignmentConfirm,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Box {
                SecondaryScreenHeader(Modifier.matchParentSize())
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            HomeAtmosphericFade(Modifier.padding(top = innerPadding.calculateTopPadding()))
            val contentPhase = when {
                showInitialLoading -> "skeleton"
                showOfflineEmpty -> "offline"
                labels == null -> "skeleton"
                else -> "content"
            }
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = {
                    fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(110, easing = FastOutSlowInEasing))
                },
                modifier = Modifier.fillMaxSize(),
                label = "printerDetailContent",
            ) { phase ->
        when (phase) {
            "skeleton" -> LoadingContent(Modifier.padding(innerPadding))
            "offline" -> EmptyContent(
                message = stringResource(R.string.offline_empty_printer_detail_title),
                subtitle = stringResource(R.string.offline_empty_printer_detail_subtitle),
                icon = BuddyDashEmptyIcon.Printers.asImageVector(),
                modifier = Modifier.padding(innerPadding),
            )
            else -> {
                val labels = labels ?: return@AnimatedContent
                val isExpandedWidth = rememberIsBuddyDashExpandedWidth()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    OfflineStaleBanner(
                        visible = showStaleBanner,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        limited = isLimitedFromHomeCache,
                        refreshFailed = staleBannerRefreshFailed,
                    )
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
                        isRefreshing = showPullRefreshIndicator,
                        onRefresh = onPullRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        BuddyDashExpandedDetailContainer(
                            isExpandedWidth = isExpandedWidth,
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
                                    printerModel = printerModel,
                                    printerStatus = printerStatus,
                                    printingQueueJobId = printingQueueJobId,
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
                                1 -> FilamentTab(
                                    labels = labels,
                                    printerId = printerId,
                                    filamentSlotDisplays = filamentSlotDisplays,
                                    onOpenFilamentSlot = onOpenFilamentSlot,
                                )
                                2 -> MachineTab(
                                    labels = labels,
                                    printerModel = printerModel,
                                    machineInfo = machineInfo,
                                    smartPlugState = smartPlugState,
                                    smartPlugPowerHistory = smartPlugPowerHistory,
                                    printerStatus = printerStatus,
                                    powerControlsEnabled = powerControlsEnabled,
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
                                    onPowerOn = onPowerOnSmartPlug,
                                    onPowerOff = onPowerOffSmartPlug,
                                    onRequiresConnectionTap = {
                                        snackbarScope.launch {
                                            snackbarHostState.showSnackbar(requiresConnectionMessage)
                                        }
                                    },
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
        } // AnimatedContent
        } // Box
    }
}

/**
 * Status tab sections — keep as a vertical [Column]; never wrap in Box/AnimatedContent.
 * Order: camera → overview/print → queue → connectivity → fans → speed → maintenance.
 */
@Composable
private fun StatusTab(
    labels: PrinterDetailLabels,
    printerModel: String?,
    printerStatus: PrinterStatus?,
    printingQueueJobId: Int?,
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
    val isExpandedWidth = rememberIsBuddyDashExpandedWidth()
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
                printerModel = printerModel,
                printerStatus = printerStatus,
                printingQueueJobId = printingQueueJobId,
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
                isExpandedWidth = isExpandedWidth,
            )
        } else {
            IdleStatusTab(
                labels = labels,
                printerModel = printerModel,
                printerStatus = printerStatus,
                printingQueueJobId = printingQueueJobId,
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
                isExpandedWidth = isExpandedWidth,
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
            isExpandedWidth = isExpandedWidth,
        )
    }
}

@Composable
private fun DetailOperationalStats(
    labels: PrinterDetailLabels,
    isMaintenanceResetBusy: Boolean,
    onPerformMaintenanceReset: (Int) -> Unit,
    isExpandedWidth: Boolean,
) {
    if (!isExpandedWidth) {
        DetailConnectivityCard(labels)
        DetailFansCard(labels)
        DetailPrintSpeedCard(labels)
        DetailMaintenanceCard(
            labels = labels,
            resetBusy = isMaintenanceResetBusy,
            onPerformReset = onPerformMaintenanceReset,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                DetailConnectivityCard(labels)
            }
            Column(modifier = Modifier.weight(1f)) {
                DetailFansCard(labels)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                DetailPrintSpeedCard(labels)
            }
            Column(modifier = Modifier.weight(1f)) {
                DetailMaintenanceCard(
                    labels = labels,
                    resetBusy = isMaintenanceResetBusy,
                    onPerformReset = onPerformMaintenanceReset,
                )
            }
        }
    }
}

@Composable
private fun ActivePrintStatusTab(
    labels: PrinterDetailLabels,
    printerModel: String?,
    printerStatus: PrinterStatus?,
    printingQueueJobId: Int?,
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
    isExpandedWidth: Boolean,
) {
    if (isExpandedWidth) {
        ActivePrintStatusTabExpanded(
            labels = labels,
            printerModel = printerModel,
            printerStatus = printerStatus,
            printingQueueJobId = printingQueueJobId,
            printerId = printerId,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
            isClearingPlate = isClearingPlate,
            onMarkPlateClear = onMarkPlateClear,
            headerTrailing = headerTrailing,
            errorDetailsExpanded = errorDetailsExpanded,
            onExpandErrorDetails = onExpandErrorDetails,
            onErrorChipClick = onErrorChipClick,
            errorCardScrollOffset = errorCardScrollOffset,
            density = density,
        )
        return
    }
    var cameraHeroActive by remember { mutableStateOf(false) }
    DetailStatusHeroImage(
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        printerModel = printerModel,
        status = printerStatus,
        printingQueueJobId = printingQueueJobId,
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
                status = printerStatus,
                printingQueueJobId = printingQueueJobId,
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
    printerModel: String?,
    printerStatus: PrinterStatus?,
    printingQueueJobId: Int?,
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
    isExpandedWidth: Boolean,
) {
    if (isExpandedWidth) {
        IdleStatusTabExpanded(
            labels = labels,
            printerModel = printerModel,
            printerStatus = printerStatus,
            printingQueueJobId = printingQueueJobId,
            printerId = printerId,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
            isClearingPlate = isClearingPlate,
            onMarkPlateClear = onMarkPlateClear,
            headerTrailing = headerTrailing,
            errorDetailsExpanded = errorDetailsExpanded,
            onExpandErrorDetails = onExpandErrorDetails,
            onErrorChipClick = onErrorChipClick,
            errorCardScrollOffset = errorCardScrollOffset,
            density = density,
        )
        return
    }
    var cameraHeroActive by remember { mutableStateOf(false) }
    DetailStatusHeroImage(
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        printerModel = printerModel,
        status = printerStatus,
        printingQueueJobId = printingQueueJobId,
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
                PrintFileHighlightWithCover(
                    label = labels.fileLabel,
                    fileName = labels.fileName,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    printerId = printerId,
                    showCoverThumbnail = cameraHeroActive,
                    status = printerStatus,
                    printingQueueJobId = printingQueueJobId,
                )
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
            valueColor = when (labels.hmsAlertSeverity) {
                HmsSeverity.Error -> MaterialTheme.colorScheme.error
                HmsSeverity.Warning, HmsSeverity.Unknown -> HmsDetailAmber
                HmsSeverity.Ok -> MaterialTheme.colorScheme.primary
            },
        )
    }
}

private fun controlFeedbackMessage(
    feedback: ControlFeedback,
    genericSuccessMessage: String,
    homeSuccessMessage: String,
    genericFailedMessage: String,
    homeFailedMessage: String,
    bedJogFailedMessage: String,
    chamberLightFailedMessage: String,
    printStoppedSuccessMessage: String,
    printStoppedFailedMessage: String,
    powerOnSuccessMessage: String,
    powerOffSuccessMessage: String,
    powerOnFailedMessage: String,
    powerOffFailedMessage: String,
): String {
    if (feedback.success) {
        return when (feedback.action) {
            ControlAction.Stop -> printStoppedSuccessMessage
            ControlAction.HomeAxes -> homeSuccessMessage
            ControlAction.SmartPlugOn -> powerOnSuccessMessage
            ControlAction.SmartPlugOff -> powerOffSuccessMessage
            else -> genericSuccessMessage
        }
    }
    return when (feedback.action) {
        ControlAction.HomeAxes -> homeFailedMessage
        ControlAction.BedJog -> bedJogFailedMessage
        ControlAction.ChamberLight -> chamberLightFailedMessage
        ControlAction.Stop -> printStoppedFailedMessage
        ControlAction.SmartPlugOn -> powerOnFailedMessage
        ControlAction.SmartPlugOff -> powerOffFailedMessage
        else -> genericFailedMessage
    }
}

@Composable
private fun FilamentTab(
    labels: PrinterDetailLabels,
    printerId: Int,
    filamentSlotDisplays: List<FilamentSlotDisplay>,
    onOpenFilamentSlot: (FilamentSlotDisplay) -> Unit,
) {
    val slots = labels.filamentSlots
    if (slots.isEmpty()) {
        Text(
            text = stringResource(R.string.no_filament_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val displays = filamentSlotDisplays.ifEmpty {
        buildFilamentSlotDisplays(
            slots = slots,
            activeKey = labels.activeFilamentSlot,
            printerId = printerId,
            inventoryBySlot = emptyMap(),
            spoolsById = emptyMap(),
            spoolsAssignedToPrinter = emptyList(),
        )
    }
    FilamentAmsEnvironmentSection(labels)
    val filamentGridColumns = rememberBuddyDashExpandedGridColumnCount()
    FilamentDetailGroups(
        slotDisplays = displays,
        cardMicroMotion = labels.cardMicroMotion,
        onSlotClick = onOpenFilamentSlot,
        modifier = Modifier.fillMaxWidth(),
        gridColumns = filamentGridColumns,
    )
}

@Composable
internal fun PlateClearButton(
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
