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
import com.chronoswing.buddydash.util.formatConnection
import com.chronoswing.buddydash.util.formatEta
import com.chronoswing.buddydash.util.formatHmsHealth
import com.chronoswing.buddydash.util.formatProgress
import com.chronoswing.buddydash.util.formatTemp

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

    PrinterDetailScreenContent(
        title = uiState.printerName.ifBlank { printerName },
        isLoading = uiState.isLoading,
        error = uiState.error,
        connected = uiState.status?.connected ?: false,
        state = uiState.status?.state,
        progress = uiState.status?.progress,
        fileName = uiState.status?.fileName,
        eta = formatEta(uiState.status?.remainingTimeSeconds),
        nozzleTemp = formatTemp(uiState.status?.nozzleTemp),
        bedTemp = formatTemp(uiState.status?.bedTemp),
        hmsHealth = formatHmsHealth(uiState.status?.hmsErrorCount ?: 0),
        hmsHasErrors = (uiState.status?.hmsErrorCount ?: 0) > 0,
        onBack = onBack,
        onRetry = viewModel::loadStatus,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterDetailScreenContent(
    title: String,
    isLoading: Boolean,
    error: String?,
    connected: Boolean,
    state: String?,
    progress: Float?,
    fileName: String?,
    eta: String,
    nozzleTemp: String,
    bedTemp: String,
    hmsHealth: String,
    hmsHasErrors: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
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
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val progressFraction = (progress ?: 0f).coerceIn(0f, 100f) / 100f
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.print_progress, formatProgress(progress)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    StatusCard(
                        label = stringResource(R.string.connection),
                        value = formatConnection(connected),
                    )
                    StatusCard(
                        label = stringResource(R.string.print_state),
                        value = state ?: "—",
                    )
                    StatusCard(
                        label = stringResource(R.string.hms_health),
                        value = hmsHealth,
                        valueColor = if (hmsHasErrors) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    StatusCard(
                        label = stringResource(R.string.current_file),
                        value = fileName ?: "—",
                    )
                    StatusCard(
                        label = stringResource(R.string.eta),
                        value = eta,
                    )
                    StatusCard(
                        label = stringResource(R.string.nozzle_temp),
                        value = nozzleTemp,
                    )
                    StatusCard(
                        label = stringResource(R.string.bed_temp),
                        value = bedTemp,
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
