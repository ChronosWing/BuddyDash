package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.ui.motion.AnimatedLinearProgressIndicator
import com.chronoswing.buddydash.ui.theme.OfflineRed
import com.chronoswing.buddydash.util.MaintenanceHomeIndicator
import com.chronoswing.buddydash.util.clampFinite
import com.chronoswing.buddydash.util.MaintenanceLine
import com.chronoswing.buddydash.util.MaintenanceLineKind
import com.chronoswing.buddydash.util.formatMaintenanceDetailMeta
import com.chronoswing.buddydash.util.maintenanceAttentionLines
import com.chronoswing.buddydash.util.maintenanceDisplayLines

private val MaintenanceAmber = Color(0xFFFBBF24)

enum class MaintenanceSnoozeDuration {
    LaterToday,
    Tomorrow,
    Dismiss,
}

fun MaintenanceSnoozeDuration.toMillis(): Long = when (this) {
    MaintenanceSnoozeDuration.LaterToday -> 6L * 60 * 60 * 1000
    MaintenanceSnoozeDuration.Tomorrow -> 24L * 60 * 60 * 1000
    MaintenanceSnoozeDuration.Dismiss -> 72L * 60 * 60 * 1000
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceDetailSheet(
    printerName: String,
    maintenanceItems: List<MaintenanceItem>,
    maintenanceIndicator: MaintenanceHomeIndicator,
    totalPrintHours: Double?,
    onSnooze: ((Int, MaintenanceSnoozeDuration) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        MaintenanceAlertsContent(
            printerName = printerName,
            maintenanceItems = maintenanceItems,
            maintenanceIndicator = maintenanceIndicator,
            totalPrintHours = totalPrintHours,
            showHeader = true,
            onSnooze = onSnooze,
        )
    }
}

@Composable
fun MaintenanceAlertsContent(
    printerName: String,
    maintenanceItems: List<MaintenanceItem>,
    maintenanceIndicator: MaintenanceHomeIndicator,
    totalPrintHours: Double?,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
    standaloneSheet: Boolean = true,
    onSnooze: ((Int, MaintenanceSnoozeDuration) -> Unit)? = null,
) {
    val attentionLines = maintenanceAttentionLines(maintenanceItems)
    val fallbackNeeded = attentionLines.isEmpty() &&
        maintenanceIndicator != MaintenanceHomeIndicator.None

  val bodyModifier = if (standaloneSheet) {
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Column(modifier = bodyModifier) {
        if (showHeader) {
            MaintenanceSheetHeader(
                printerName = printerName,
                maintenanceIndicator = maintenanceIndicator,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            )
        }

        totalPrintHours?.let { hours ->
            Text(
                text = stringResource(R.string.maintenance_sheet_total_print_hours, hours),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        when {
            attentionLines.isNotEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    attentionLines.forEach { line ->
                        val item = maintenanceItems.firstOrNull { it.id == line.itemId }
                        MaintenanceAlertEntryRow(
                            line = line,
                            item = item,
                            onSnooze = if (onSnooze != null) {
                                { duration -> onSnooze(line.itemId, duration) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
            fallbackNeeded -> {
                MaintenanceFallbackCard(
                    maintenanceItems = maintenanceItems,
                    maintenanceIndicator = maintenanceIndicator,
                )
            }
            else -> {
                Text(
                    text = stringResource(R.string.maintenance_sheet_no_entries),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MaintenanceSheetHeader(
    printerName: String,
    maintenanceIndicator: MaintenanceHomeIndicator,
) {
    val headerColor = when (maintenanceIndicator) {
        MaintenanceHomeIndicator.Due -> OfflineRed
        MaintenanceHomeIndicator.DueSoon -> MaintenanceAmber
        MaintenanceHomeIndicator.None -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        Icon(
            imageVector = if (maintenanceIndicator == MaintenanceHomeIndicator.Due) {
                Icons.Filled.Build
            } else {
                Icons.Filled.Warning
            },
            contentDescription = null,
            tint = headerColor,
            modifier = Modifier.size(22.dp),
        )
        Column {
            Text(
                text = stringResource(R.string.maintenance_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = printerName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MaintenanceAlertEntryRow(
    line: MaintenanceLine,
    item: MaintenanceItem?,
    onSnooze: ((MaintenanceSnoozeDuration) -> Unit)? = null,
) {
    val (levelLabel, levelColor) = when (line.kind) {
        MaintenanceLineKind.Due ->
            stringResource(R.string.maintenance_sheet_status_due) to OfflineRed
        MaintenanceLineKind.DueSoon ->
            stringResource(R.string.maintenance_sheet_status_due_soon) to MaintenanceAmber
        MaintenanceLineKind.Ok ->
            stringResource(R.string.maintenance_sheet_status_ok) to MaterialTheme.colorScheme.primary
    }
    val meta = item?.let { formatMaintenanceDetailMeta(it, line.kind) }
    val progressTrack = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val progressColor = when (line.kind) {
        MaintenanceLineKind.Ok -> levelColor.copy(alpha = 0.45f)
        MaintenanceLineKind.DueSoon -> levelColor.copy(alpha = 0.75f)
        MaintenanceLineKind.Due -> levelColor
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = levelColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = line.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = levelColor.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = levelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = levelColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            line.remainingText?.let { remaining ->
                Text(
                    text = remaining,
                    style = MaterialTheme.typography.bodySmall,
                    color = levelColor,
                )
            }
            meta?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (line.canReset) {
                Text(
                    text = stringResource(R.string.maintenance_sheet_reset_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
            line.progressFraction?.let { fraction ->
                AnimatedLinearProgressIndicator(
                    targetFraction = fraction.clampFinite(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .height(2.dp),
                    color = progressColor,
                    trackColor = progressTrack,
                )
            }
            if (onSnooze != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.maintenance_snooze_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    TextButton(
                        onClick = { onSnooze(MaintenanceSnoozeDuration.LaterToday) },
                    ) {
                        Text(
                            text = stringResource(R.string.maintenance_snooze_later_today),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TextButton(
                        onClick = { onSnooze(MaintenanceSnoozeDuration.Tomorrow) },
                    ) {
                        Text(
                            text = stringResource(R.string.maintenance_snooze_tomorrow),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TextButton(
                        onClick = { onSnooze(MaintenanceSnoozeDuration.Dismiss) },
                    ) {
                        Text(
                            text = stringResource(R.string.maintenance_snooze_dismiss),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceFallbackCard(
    maintenanceItems: List<MaintenanceItem>,
    maintenanceIndicator: MaintenanceHomeIndicator,
) {
    val accent = when (maintenanceIndicator) {
        MaintenanceHomeIndicator.Due -> OfflineRed
        MaintenanceHomeIndicator.DueSoon -> MaintenanceAmber
        MaintenanceHomeIndicator.None -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.maintenance_sheet_fallback),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val rawNames = maintenanceItems
                .filter { it.enabled }
                .map { it.name.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            if (rawNames.isNotEmpty()) {
                Text(
                    text = rawNames.joinToString(separator = "\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                maintenanceDisplayLines(maintenanceItems)
                    .take(3)
                    .forEach { line ->
                        Text(
                            text = line.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
            }
        }
    }
}
