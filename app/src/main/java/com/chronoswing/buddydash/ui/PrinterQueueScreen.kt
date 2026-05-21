package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.StartQueuedPrintSnackbar
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.ui.components.PrintQueueItemRow
import com.chronoswing.buddydash.ui.components.StartNextPrintAction
import com.chronoswing.buddydash.util.StartNextQueuedPrintReadiness

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterQueueScreen(
    printerName: String,
    jobs: List<PrintQueueJob>,
    serverUrl: String,
    cameraToken: String,
    showStartNextPrint: Boolean,
    startNextQueuedPrintReadiness: StartNextQueuedPrintReadiness,
    isStartingQueuedPrint: Boolean,
    startQueuedPrintSnackbar: StartQueuedPrintSnackbar?,
    onBack: () -> Unit,
    onStartNextQueuedPrint: () -> Unit,
    onStartQueuedPrintSnackbarShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val printStartedMessage = stringResource(R.string.archive_reprint_started)
    val startNextPrintFailedMessage = stringResource(R.string.start_next_print_failed)

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = printerName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.queue_section_title, jobs.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
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
        if (jobs.isEmpty()) {
            Text(
                text = stringResource(R.string.queue_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showStartNextPrint) {
                    item(key = "start_next_print") {
                        StartNextPrintAction(
                            printerName = printerName,
                            readiness = startNextQueuedPrintReadiness,
                            isSubmitting = isStartingQueuedPrint,
                            onConfirmStart = onStartNextQueuedPrint,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                itemsIndexed(
                    items = jobs,
                    key = { _, job -> job.id },
                ) { index, job ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        PrintQueueItemRow(
                            job = job,
                            rowLabel = (index + 1).toString(),
                            serverUrl = serverUrl,
                            cameraToken = cameraToken,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}
