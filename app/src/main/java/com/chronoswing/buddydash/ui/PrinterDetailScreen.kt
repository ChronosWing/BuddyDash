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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.toDetailLabels

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
        onBack = onBack,
        onRetry = viewModel::loadStatus,
        onMarkPlateClear = viewModel::markPlateClear,
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
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onMarkPlateClear: () -> Unit,
) {
    Scaffold(
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
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatusCard(
                        label = stringResource(R.string.connection),
                        value = labels.connection,
                    )
                    StatusCard(
                        label = stringResource(R.string.current_activity),
                        value = labels.currentActivity,
                    )
                    labels.lastPrintResult?.let { result ->
                        StatusCard(
                            label = stringResource(R.string.last_print_result),
                            value = result,
                        )
                    }
                    if (labels.plateStatus != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusCard(
                                label = stringResource(R.string.plate_status),
                                value = labels.plateStatus,
                            )
                            if (labels.showPlateClearAction) {
                                val endpointMissing = !labels.plateClearEndpointAvailable
                                Button(
                                    onClick = onMarkPlateClear,
                                    enabled = !isClearingPlate && !endpointMissing,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        when {
                                            endpointMissing ->
                                                stringResource(R.string.plate_clear_endpoint_not_found)
                                            isClearingPlate ->
                                                stringResource(R.string.marking_plate_clear)
                                            else ->
                                                stringResource(R.string.mark_plate_clear)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (labels.showProgress) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.progress_label,
                                    labels.progressTitle,
                                    labels.progressValue,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            labels.progressFraction?.let { fraction ->
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    if (labels.showFile) {
                        StatusCard(
                            label = labels.fileLabel,
                            value = labels.fileName.ifBlank { "—" },
                        )
                    }
                    if (labels.showEta) {
                        StatusCard(
                            label = stringResource(R.string.eta),
                            value = labels.eta,
                        )
                    }
                    StatusCard(
                        label = stringResource(R.string.nozzle_temp),
                        value = labels.nozzleTemp,
                    )
                    StatusCard(
                        label = stringResource(R.string.bed_temp),
                        value = labels.bedTemp,
                    )
                    StatusCard(
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
        }
    }
}

@Composable
private fun StatusCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor,
            )
        }
    }
}
