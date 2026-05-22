package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.ArchiveDetailViewModel
import com.chronoswing.buddydash.ArchiveReprintSheetState
import com.chronoswing.buddydash.ArchiveReprintSnackbar
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.ui.components.ArchiveDetailHeroImage
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.EmptyContent
import com.chronoswing.buddydash.ui.components.OfflineStaleBanner
import com.chronoswing.buddydash.util.isShowingStaleCachedContent
import com.chronoswing.buddydash.ui.components.ArchiveMaterialRow
import com.chronoswing.buddydash.ui.components.FilamentUsageText
import com.chronoswing.buddydash.ui.components.BuddyDashEmptyIcon
import com.chronoswing.buddydash.ui.components.PrinterDetailSkeleton
import com.chronoswing.buddydash.ui.components.asImageVector
import com.chronoswing.buddydash.ui.motion.buddyDashButtonPress
import com.chronoswing.buddydash.ui.motion.rememberBuddyDashInteractionSource
import com.chronoswing.buddydash.ui.motion.successPulseOn
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.util.ARCHIVE_DISPLAY_NAME_FALLBACK
import com.chronoswing.buddydash.util.ArchiveMaterialNavigation
import com.chronoswing.buddydash.util.archiveHasMaterialDisplay
import com.chronoswing.buddydash.util.formatArchiveDetailMaterialType
import com.chronoswing.buddydash.util.formatArchiveDuration
import com.chronoswing.buddydash.util.formatArchivePlateLine
import com.chronoswing.buddydash.util.formatArchivePrinterLine
import com.chronoswing.buddydash.util.formatArchiveStatusLabel
import com.chronoswing.buddydash.util.formatFilamentUsageCompact
import com.chronoswing.buddydash.util.isMeaningfulArchiveField
import com.chronoswing.buddydash.util.shouldShowArchiveFailureReason

@Composable
fun ArchiveDetailScreen(
    archiveId: Int,
    viewModel: ArchiveDetailViewModel,
    onBack: () -> Unit,
    onViewQueue: (printerId: Int, printerName: String, printerModel: String?) -> Unit,
    onMaterialNavigation: (ArchiveMaterialNavigation) -> Unit,
) {
    LaunchedEffect(archiveId) {
        viewModel.init(archiveId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ArchiveDetailScreenContent(
        archive = uiState.archive,
        isLoading = uiState.isLoading,
        hasCompletedLoad = uiState.hasCompletedLoad,
        settingsReady = uiState.settingsReady,
        error = uiState.error,
        isStaleCachedData = uiState.isStaleCachedData,
        isLimitedFromListCache = uiState.isLimitedFromListCache,
        serverUrl = uiState.serverUrl,
        cameraToken = uiState.cameraToken,
        hasCredentials = uiState.hasCredentials,
        reprintSheet = uiState.reprintSheet,
        reprintSnackbar = uiState.reprintSnackbar,
        onBack = onBack,
        onRetry = viewModel::loadArchive,
        onQueueAgain = viewModel::onQueueAgainClick,
        onDismissReprintSheet = viewModel::onDismissReprintSheet,
        onReprintPrinterSelected = viewModel::onReprintPrinterSelected,
        onReprintQuantityChange = viewModel::onReprintQuantityChange,
        onConfirmQueueOnly = viewModel::onConfirmQueueOnly,
        onConfirmQueueAndStart = viewModel::onConfirmQueueAndStart,
        onReprintSnackbarShown = viewModel::onReprintSnackbarShown,
        onViewQueue = {
            val id = uiState.queuedPrinterId ?: return@ArchiveDetailScreenContent
            val name = uiState.queuedPrinterName ?: return@ArchiveDetailScreenContent
            onViewQueue(id, name, uiState.queuedPrinterModel)
        },
        onMaterialTap = viewModel::onMaterialTap,
        onMaterialNavigation = onMaterialNavigation,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveDetailScreenContent(
    archive: PrintArchive?,
    isLoading: Boolean,
    hasCompletedLoad: Boolean,
    settingsReady: Boolean,
    error: String?,
    isStaleCachedData: Boolean,
    isLimitedFromListCache: Boolean,
    serverUrl: String,
    cameraToken: String,
    hasCredentials: Boolean,
    reprintSheet: ArchiveReprintSheetState,
    reprintSnackbar: ArchiveReprintSnackbar?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onQueueAgain: () -> Unit,
    onDismissReprintSheet: () -> Unit,
    onReprintPrinterSelected: (Int) -> Unit,
    onReprintQuantityChange: (Int) -> Unit,
    onConfirmQueueOnly: () -> Unit,
    onConfirmQueueAndStart: () -> Unit,
    onReprintSnackbarShown: () -> Unit,
    onViewQueue: () -> Unit,
    onMaterialTap: (onNavigate: (ArchiveMaterialNavigation) -> Unit) -> Unit,
    onMaterialNavigation: (ArchiveMaterialNavigation) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var queueSuccessPulse by remember { mutableIntStateOf(0) }
    val queuedMessage = stringResource(R.string.archive_reprint_success)
    val startedMessage = stringResource(R.string.archive_reprint_started)
    val queuedStartFailedMessage = stringResource(R.string.archive_reprint_queued_start_failed)
    val failedMessage = stringResource(R.string.archive_reprint_failed)
    val viewQueueAction = stringResource(R.string.archive_reprint_view_queue)

    LaunchedEffect(reprintSnackbar) {
        when (reprintSnackbar) {
            ArchiveReprintSnackbar.Queued,
            ArchiveReprintSnackbar.Started,
            ArchiveReprintSnackbar.QueuedStartFailed,
            -> {
                queueSuccessPulse++
                val message = when (reprintSnackbar) {
                    ArchiveReprintSnackbar.Queued -> queuedMessage
                    ArchiveReprintSnackbar.Started -> startedMessage
                    ArchiveReprintSnackbar.QueuedStartFailed -> queuedStartFailedMessage
                    else -> queuedMessage
                }
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = viewQueueAction,
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onViewQueue()
                }
                onReprintSnackbarShown()
            }
            ArchiveReprintSnackbar.Failed -> {
                snackbarHostState.showSnackbar(failedMessage)
                onReprintSnackbarShown()
            }
            null -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archive_detail_title)) },
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
        bottomBar = {
            if (archive != null && hasCredentials) {
                val queueAgainInteraction = rememberBuddyDashInteractionSource()
                val queueAgainEnabled = !isLoading && !reprintSheet.isSubmitting &&
                    !isShowingStaleCachedContent(isStaleCachedData, null)
                Button(
                    onClick = onQueueAgain,
                    enabled = queueAgainEnabled,
                    interactionSource = queueAgainInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .successPulseOn(queueSuccessPulse)
                        .buddyDashButtonPress(queueAgainEnabled, queueAgainInteraction),
                ) {
                    Text(stringResource(R.string.archive_reprint_queue_again))
                }
            }
        },
    ) { innerPadding ->
        val showInitialLoading = !settingsReady || !hasCompletedLoad
        val showOfflineEmpty = hasCompletedLoad && archive == null && error != null
        when {
            showInitialLoading -> {
                PrinterDetailSkeleton(Modifier.padding(innerPadding))
            }
            settingsReady && !hasCredentials && archive == null -> {
                EmptyContent(
                    message = stringResource(R.string.configure_settings_hint),
                    icon = BuddyDashEmptyIcon.Settings.asImageVector(),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            showOfflineEmpty -> {
                EmptyContent(
                    message = stringResource(R.string.offline_empty_archive_detail_title),
                    subtitle = stringResource(R.string.offline_empty_archive_detail_subtitle),
                    icon = BuddyDashEmptyIcon.Archives.asImageVector(),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            archive != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isStaleCachedData) {
                        OfflineStaleBanner(limited = isLimitedFromListCache)
                    }
                    ArchiveDetailBody(
                        archive = archive,
                        serverUrl = serverUrl,
                        cameraToken = cameraToken,
                        hasCredentials = hasCredentials,
                        onMaterialTap = onMaterialTap,
                        onMaterialNavigation = onMaterialNavigation,
                    )
                }
                ArchiveReprintSheet(
                    archive = archive,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    sheetState = reprintSheet,
                    onDismiss = onDismissReprintSheet,
                    onPrinterSelected = onReprintPrinterSelected,
                    onQuantityChange = onReprintQuantityChange,
                    onQueueOnly = onConfirmQueueOnly,
                    onQueueAndStart = onConfirmQueueAndStart,
                )
            }
        }
    }
}

@Composable
private fun ArchiveDetailBody(
    archive: PrintArchive,
    serverUrl: String,
    cameraToken: String,
    hasCredentials: Boolean,
    onMaterialTap: (onNavigate: (ArchiveMaterialNavigation) -> Unit) -> Unit,
    onMaterialNavigation: (ArchiveMaterialNavigation) -> Unit,
) {
    val displayName = if (archive.displayName == ARCHIVE_DISPLAY_NAME_FALLBACK) {
        stringResource(R.string.archive_unnamed_print)
    } else {
        archive.displayName
    }
    val statusLabel = formatArchiveStatusLabel(archive.resultKind, archive.statusRaw)
    val showMaterial = archiveHasMaterialDisplay(archive)

    DetailInfoCard {
        ArchiveDetailHeroImage(
            archiveId = archive.id,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
        )
        PrintFileNameText(
            fileName = displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        ArchiveResultBadge(
            label = statusLabel,
            resultKind = archive.resultKind,
        )
        formatArchivePrinterLine(archive)?.let { printer ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_printer),
                value = printer,
            )
        }
        formatArchiveDuration(archive.durationSeconds)?.let { duration ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_duration),
                value = duration,
            )
        }
        formatFilamentUsageCompact(archive.filamentUsage)?.let { usage ->
            FilamentUsageText(
                text = usage,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (showMaterial) {
            ArchiveDetailMaterialRow(
                archive = archive,
                tappable = hasCredentials,
                onMaterialClick = {
                    onMaterialTap(onMaterialNavigation)
                },
            )
        }
        formatArchivePlateLine(archive)?.let { plate ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_plate),
                value = plate,
            )
        }
        if (shouldShowArchiveFailureReason(archive)) {
            CompactLabelValue(
                label = stringResource(R.string.archive_label_failure),
                value = archive.failureReason.orEmpty(),
            )
        }
        archive.projectName?.takeIf { isMeaningfulArchiveField(it) }?.let { project ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_project),
                value = project,
            )
        }
        archive.notes?.takeIf { isMeaningfulArchiveField(it) }?.let { notes ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_notes),
                value = notes,
            )
        }
    }
}

@Composable
private fun ArchiveResultBadge(
    label: String,
    resultKind: ArchiveResultKind,
) {
    val containerColor = when (resultKind) {
        ArchiveResultKind.Success ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        ArchiveResultKind.Failed ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        ArchiveResultKind.Cancelled ->
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        ArchiveResultKind.Other ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val contentColor = when (resultKind) {
        ArchiveResultKind.Success -> MaterialTheme.colorScheme.primary
        ArchiveResultKind.Failed -> MaterialTheme.colorScheme.error
        ArchiveResultKind.Cancelled -> MaterialTheme.colorScheme.tertiary
        ArchiveResultKind.Other -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

@Composable
private fun ArchiveDetailMaterialRow(
    archive: PrintArchive,
    tappable: Boolean,
    onMaterialClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.archive_label_material),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
        ArchiveMaterialRow(
            archive = archive,
            swatchSize = 22.dp,
            textStyle = MaterialTheme.typography.bodyMedium,
            textColor = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            tappable = tappable,
            onClick = onMaterialClick,
        )
        if (tappable) {
            Text(
                text = stringResource(R.string.archive_material_lookup_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
    }
}
