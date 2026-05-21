package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.PlateClearSnackbar
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentChipRow
import com.chronoswing.buddydash.ui.components.HighlightValue
import com.chronoswing.buddydash.ui.components.InlineProgress
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.components.SecondaryNote
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.normalizeFilamentType
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
    val labels = uiState.status?.toDetailLabels()

    PrinterDetailScreenContent(
        title = uiState.printerName.ifBlank { printerName },
        isLoading = uiState.isLoading,
        error = uiState.error,
        labels = labels,
        isClearingPlate = uiState.isClearingPlate,
        plateClearSnackbar = uiState.plateClearSnackbar,
        onBack = onBack,
        onRetry = viewModel::loadStatus,
        onRefresh = { viewModel.loadStatus(showLoading = false) },
        onMarkPlateClear = viewModel::markPlateClear,
        onPlateClearSnackbarShown = viewModel::onPlateClearSnackbarShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterDetailScreenContent(
    title: String,
    isLoading: Boolean,
    error: String?,
    labels: PrinterDetailLabels?,
    isClearingPlate: Boolean,
    plateClearSnackbar: PlateClearSnackbar?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onMarkPlateClear: () -> Unit,
    onPlateClearSnackbarShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val plateClearSuccessMessage = stringResource(R.string.plate_clear_success)
    val plateClearFailedMessage = stringResource(R.string.plate_clear_failed)
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
                                isClearingPlate = isClearingPlate,
                                onMarkPlateClear = onMarkPlateClear,
                            )
                            1 -> FilamentTab(slots = labels.filamentSlots)
                            2 -> ControlsTab(
                                labels = labels,
                                isClearingPlate = isClearingPlate,
                                onRefresh = onRefresh,
                                onMarkPlateClear = onMarkPlateClear,
                            )
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
    isClearingPlate: Boolean,
    onMarkPlateClear: () -> Unit,
) {
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_overview))
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

    val hasPrintSection = labels.showProgress ||
        labels.showFile ||
        labels.showEta ||
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
            if (labels.showProgress) {
                labels.progressFraction?.let { fraction ->
                    InlineProgress(
                        label = labels.progressTitle,
                        value = labels.progressValue,
                        fraction = fraction,
                    )
                }
            }
            if (labels.showFile) {
                CompactLabelValue(
                    label = labels.fileLabel,
                    value = labels.fileName.ifBlank { "—" },
                )
            }
            if (labels.showEta) {
                CompactLabelValue(label = stringResource(R.string.eta), value = labels.eta)
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
private fun FilamentTab(slots: List<FilamentSlot>) {
    if (slots.isEmpty()) {
        Text(
            text = stringResource(R.string.no_filament_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FilamentChipRow(slots = slots, compact = false, modifier = Modifier.fillMaxWidth())
    slots.forEach { slot ->
        DetailInfoCard {
            CompactLabelValue(label = stringResource(R.string.slot_label), value = slot.label)
            if (slot.isLoaded) {
                CompactLabelValue(
                    label = stringResource(R.string.filament_type),
                    value = normalizeFilamentType(slot.filamentType)?.uppercase()
                        ?: stringResource(R.string.filament_unknown),
                )
                slot.remainPercent?.let { remain ->
                    CompactLabelValue(
                        label = stringResource(R.string.filament_remaining),
                        value = "$remain%",
                    )
                }
                slot.metadata?.let { meta ->
                    CompactLabelValue(label = stringResource(R.string.filament_metadata), value = meta)
                }
            } else {
                CompactLabelValue(
                    label = stringResource(R.string.filament_type),
                    value = stringResource(R.string.filament_empty),
                )
            }
        }
    }
}

@Composable
private fun ControlsTab(
    labels: PrinterDetailLabels,
    isClearingPlate: Boolean,
    onRefresh: () -> Unit,
    onMarkPlateClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.refresh_status))
        }
        if (labels.showPlateClearAction) {
            PlateClearButton(
                labels = labels,
                isClearingPlate = isClearingPlate,
                onClick = onMarkPlateClear,
            )
        }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pause_print_placeholder))
        }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel_print_placeholder))
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
