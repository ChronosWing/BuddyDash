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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.ArchiveDetailViewModel
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.ui.components.ArchiveThumbnail
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.ErrorContent
import com.chronoswing.buddydash.ui.components.FilamentUsageText
import com.chronoswing.buddydash.ui.components.LoadingContent
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.util.ARCHIVE_DISPLAY_NAME_FALLBACK
import com.chronoswing.buddydash.util.formatArchiveDate
import com.chronoswing.buddydash.util.formatArchiveDuration
import com.chronoswing.buddydash.util.formatArchiveMaterialLine
import com.chronoswing.buddydash.util.formatArchivePlateLine
import com.chronoswing.buddydash.util.formatArchivePrinterLine
import com.chronoswing.buddydash.util.formatArchiveStatusLabel
import com.chronoswing.buddydash.util.formatFilamentUsageCompact

@Composable
fun ArchiveDetailScreen(
    archiveId: Int,
    viewModel: ArchiveDetailViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(archiveId) {
        viewModel.init(archiveId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ArchiveDetailScreenContent(
        archive = uiState.archive,
        isLoading = uiState.isLoading,
        error = uiState.error,
        serverUrl = uiState.serverUrl,
        cameraToken = uiState.cameraToken,
        onBack = onBack,
        onRetry = viewModel::loadArchive,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveDetailScreenContent(
    archive: PrintArchive?,
    isLoading: Boolean,
    error: String?,
    serverUrl: String,
    cameraToken: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
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
    ) { innerPadding ->
        when {
            isLoading && archive == null -> {
                LoadingContent(Modifier.padding(innerPadding))
            }
            error != null && archive == null -> {
                ErrorContent(
                    message = error,
                    onRetry = onRetry,
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
                    ArchiveDetailBody(
                        archive = archive,
                        serverUrl = serverUrl,
                        cameraToken = cameraToken,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveDetailBody(
    archive: PrintArchive,
    serverUrl: String,
    cameraToken: String,
) {
    val displayName = if (archive.displayName == ARCHIVE_DISPLAY_NAME_FALLBACK) {
        stringResource(R.string.archive_unnamed_print)
    } else {
        archive.displayName
    }

    DetailInfoCard {
        ArchiveThumbnail(
            archiveId = archive.id,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
            size = 200.dp,
        )
        PrintFileNameText(
            fileName = displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        CompactLabelValue(
            label = stringResource(R.string.archive_label_result),
            value = formatArchiveStatusLabel(archive.resultKind, archive.statusRaw),
        )
        formatArchivePrinterLine(archive)?.let { printer ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_printer),
                value = printer,
            )
        }
        archive.startedAtIso?.let { started ->
            formatArchiveDate(started)?.let { formatted ->
                CompactLabelValue(
                    label = stringResource(R.string.archive_label_started),
                    value = formatted,
                )
            }
        }
        archive.completedAtIso?.let { completed ->
            formatArchiveDate(completed)?.let { formatted ->
                CompactLabelValue(
                    label = stringResource(R.string.archive_label_completed),
                    value = formatted,
                )
            }
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
        formatArchiveMaterialLine(archive)?.let { material ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_material),
                value = material,
            )
        }
        formatArchivePlateLine(archive)?.let { plate ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_plate),
                value = plate,
            )
        }
        archive.failureReason?.let { reason ->
            SectionHeader(stringResource(R.string.archive_label_failure))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        archive.projectName?.let { project ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_project),
                value = project,
            )
        }
        archive.slicedForModel?.let { model ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_model),
                value = model,
            )
        }
        archive.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            CompactLabelValue(
                label = stringResource(R.string.archive_label_notes),
                value = notes,
            )
        }
    }
}
