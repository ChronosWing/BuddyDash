package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.ui.components.CompactLabelValue
import com.chronoswing.buddydash.ui.components.DetailInfoCard
import com.chronoswing.buddydash.ui.components.DetailStatusHeroImage
import com.chronoswing.buddydash.ui.components.FilamentUsageText
import com.chronoswing.buddydash.ui.components.HighlightValue
import com.chronoswing.buddydash.ui.components.MicroMotionProgressBar
import com.chronoswing.buddydash.ui.components.PrintFileHighlightWithCover
import com.chronoswing.buddydash.ui.components.PrintTempsRow
import com.chronoswing.buddydash.ui.components.PrinterErrorDetailsCard
import com.chronoswing.buddydash.ui.components.PrinterQuickStatusRow
import com.chronoswing.buddydash.ui.components.SecondaryNote
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.ui.components.SectionHeaderRow
import com.chronoswing.buddydash.util.HmsSeverity
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.buildPrintHeadline
import kotlin.math.roundToInt

private val HmsDetailAmber = androidx.compose.ui.graphics.Color(0xFFFBBF24)

@Composable
internal fun ActivePrintStatusTabExpanded(
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
    density: Density,
) {
    var cameraHeroActive by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(0.6f)) {
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
            }
            Column(modifier = Modifier.weight(0.4f)) {
                DetailInfoCard {
                    SectionHeaderRow(
                        title = stringResource(R.string.section_overview),
                        trailing = headerTrailing,
                    )
                    PrinterQuickStatusRow(
                        activityKind = labels.activityKind,
                        progressCompact = labels.progressCompact,
                        plateKind = labels.plateKind,
                        onErrorChipClick = onErrorChipClick,
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
                    if (labels.showPlateClearAction) {
                        PlateClearButton(
                            labels = labels,
                            isClearingPlate = isClearingPlate,
                            onClick = onMarkPlateClear,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                DetailInfoCard {
                    SectionHeader(stringResource(R.string.section_print))
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
                }
            }
            if (labels.tempsLine != null) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailInfoCard {
                        SectionHeader(stringResource(R.string.section_environment))
                        PrintTempsRow(
                            nozzleTemp = labels.nozzleTemp,
                            bedTemp = labels.bedTemp,
                            valueStyle = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun IdleStatusTabExpanded(
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
    density: Density,
) {
    var cameraHeroActive by remember { mutableStateOf(false) }
    val hasPrintSection = labels.showFile || labels.lastPrintResult != null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(0.6f)) {
                DetailStatusHeroImage(
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    printerId = printerId,
                    printerModel = printerModel,
                    status = printerStatus,
                    printingQueueJobId = printingQueueJobId,
                    onCameraHeroActive = { cameraHeroActive = it },
                )
            }
            Column(modifier = Modifier.weight(0.4f)) {
                DetailInfoCard {
                    SectionHeaderRow(
                        title = stringResource(R.string.section_overview),
                        trailing = headerTrailing,
                    )
                    PrinterQuickStatusRow(
                        activityKind = labels.activityKind,
                        progressCompact = labels.progressCompact,
                        plateKind = labels.plateKind,
                        onErrorChipClick = onErrorChipClick,
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
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (hasPrintSection) {
                Column(modifier = Modifier.weight(1f)) {
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
            }
            Column(modifier = Modifier.weight(1f)) {
                DetailInfoCard {
                    SectionHeader(stringResource(R.string.section_environment))
                    CompactLabelValue(
                        label = stringResource(R.string.nozzle_temp),
                        value = labels.nozzleTemp,
                    )
                    CompactLabelValue(
                        label = stringResource(R.string.bed_temp),
                        value = labels.bedTemp,
                    )
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
        }
    }
}
