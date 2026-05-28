package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.chronoswing.buddydash.ui.components.DetailOperationalStatsDashboard
import com.chronoswing.buddydash.ui.components.DetailStatusHeroImage
import com.chronoswing.buddydash.ui.components.FilamentUsageText
import com.chronoswing.buddydash.ui.components.HighlightValue
import com.chronoswing.buddydash.ui.components.MicroMotionProgressBar
import com.chronoswing.buddydash.ui.components.PrintFileHighlightWithCover
import com.chronoswing.buddydash.ui.components.PrinterErrorDetailsCard
import com.chronoswing.buddydash.ui.components.PrinterQuickStatusRow
import com.chronoswing.buddydash.ui.components.SecondaryNote
import com.chronoswing.buddydash.ui.components.SectionHeader
import com.chronoswing.buddydash.ui.components.SectionHeaderRow
import com.chronoswing.buddydash.util.PrinterDetailLabels
import com.chronoswing.buddydash.util.buildPrintHeadline
import com.chronoswing.buddydash.util.clampFinite
import kotlin.math.roundToInt

private val DashboardGutter = 8.dp
private val ExpandedCameraCornerRadius = 8.dp
/** Stable 16:9 preview tile — avoids ultra-wide crop on expanded-width layouts. */
private const val ExpandedCameraAspectRatio = 16f / 9f

@Composable
private fun StatusDashboardTopRow(
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
    onCameraHeroActive: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(DashboardGutter),
    ) {
        DetailInfoCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                DetailStatusHeroImage(
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    printerId = printerId,
                    printerModel = printerModel,
                    status = printerStatus,
                    printingQueueJobId = printingQueueJobId,
                    motion = labels.cardMicroMotion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ExpandedCameraAspectRatio)
                        .clip(RoundedCornerShape(ExpandedCameraCornerRadius)),
                    fillContainer = true,
                    onCameraHeroActive = onCameraHeroActive,
                )
            }
        }
        StatusOverviewDashboardCard(
            labels = labels,
            headerTrailing = headerTrailing,
            errorDetailsExpanded = errorDetailsExpanded,
            onExpandErrorDetails = onExpandErrorDetails,
            onErrorChipClick = onErrorChipClick,
            errorCardScrollOffset = errorCardScrollOffset,
            density = density,
            isClearingPlate = isClearingPlate,
            onMarkPlateClear = onMarkPlateClear,
            showConnection = false,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

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
    isMaintenanceResetBusy: Boolean,
    onPerformMaintenanceReset: (Int) -> Unit,
) {
    var cameraHeroActive by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DashboardGutter),
    ) {
        StatusDashboardTopRow(
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
            onCameraHeroActive = { cameraHeroActive = it },
        )
        ActivePrintProgressDashboardCard(
            labels = labels,
            printerId = printerId,
            serverUrl = serverUrl,
            cameraToken = cameraToken,
            printerStatus = printerStatus,
            printingQueueJobId = printingQueueJobId,
            cameraHeroActive = cameraHeroActive,
        )
        DetailOperationalStatsDashboard(
            labels = labels,
            resetBusy = isMaintenanceResetBusy,
            onPerformReset = onPerformMaintenanceReset,
            includeHealthMetrics = false,
        )
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
    isMaintenanceResetBusy: Boolean,
    onPerformMaintenanceReset: (Int) -> Unit,
) {
    var cameraHeroActive by remember { mutableStateOf(false) }
    val hasPrintSection = labels.showFile || labels.lastPrintResult != null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DashboardGutter),
    ) {
        StatusDashboardTopRow(
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
            onCameraHeroActive = { cameraHeroActive = it },
        )
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
        DetailOperationalStatsDashboard(
            labels = labels,
            resetBusy = isMaintenanceResetBusy,
            onPerformReset = onPerformMaintenanceReset,
            includeHealthMetrics = true,
        )
    }
}

@Composable
private fun StatusOverviewDashboardCard(
    labels: PrinterDetailLabels,
    headerTrailing: @Composable () -> Unit,
    errorDetailsExpanded: Boolean,
    onExpandErrorDetails: () -> Unit,
    onErrorChipClick: () -> Unit,
    errorCardScrollOffset: androidx.compose.runtime.MutableIntState,
    density: Density,
    isClearingPlate: Boolean,
    onMarkPlateClear: () -> Unit,
    showConnection: Boolean,
    modifier: Modifier = Modifier,
) {
    DetailInfoCard(modifier = modifier) {
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
        if (showConnection) {
            CompactLabelValue(
                label = stringResource(R.string.connection),
                value = labels.connection,
            )
        }
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

@Composable
private fun ActivePrintProgressDashboardCard(
    labels: PrinterDetailLabels,
    printerId: Int,
    serverUrl: String,
    cameraToken: String,
    printerStatus: PrinterStatus?,
    printingQueueJobId: Int?,
    cameraHeroActive: Boolean,
) {
    DetailInfoCard {
        SectionHeader(stringResource(R.string.section_print))
        HighlightValue(
            label = labels.progressTitle,
            value = buildPrintHeadline(labels.currentActivity, labels.progressValue),
        )
        labels.progressFraction?.let { fraction ->
            MicroMotionProgressBar(
                progress = { fraction.clampFinite(0f, 1f) },
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
