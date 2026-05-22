package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.chronoswing.buddydash.ui.motion.buddyDashClickable
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.util.FilamentAssignAvailability
import com.chronoswing.buddydash.util.FilamentSlotDisplay
import com.chronoswing.buddydash.util.SpoolAssignmentTargetConflict
import com.chronoswing.buddydash.util.SpoolInventoryCardUsage
import com.chronoswing.buddydash.util.formatSpoolAssignmentLocationBrief
import com.chronoswing.buddydash.util.formatSpoolInventoryCardLocationLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilamentSlotDetailSheet(
    display: FilamentSlotDisplay,
    assignAvailability: FilamentAssignAvailability,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onChangeSpool: () -> Unit,
    onClearAssignment: () -> Unit,
    onViewSpool: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val slot = display.slot
    val titleText = when {
        display.isEmpty -> stringResource(R.string.filament_empty)
        display.primaryTitle.isNotBlank() -> display.primaryTitle
        else -> stringResource(R.string.filament_unknown)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.filament_slot_sheet_title, slot.label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (display.isEmpty) {
                    EmptySlotSwatch(size = 44.dp)
                } else {
                    FilamentColorSwatch(slot = slot, size = 44.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    display.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    slot.remainPercent?.let { remain ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilamentRemainingBar(
                                remainPercent = remain,
                                modifier = Modifier.weight(1f),
                                height = 4.dp,
                                barWidth = 120.dp,
                            )
                            Text(
                                text = stringResource(R.string.filament_remaining_percent, remain),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (display.canAssign) {
                HorizontalDivider()
                val changeLabel = if (display.assignedSpoolId != null || !display.isEmpty) {
                    stringResource(R.string.filament_action_change_spool)
                } else {
                    stringResource(R.string.filament_action_assign_spool)
                }
                Button(
                    onClick = onChangeSpool,
                    enabled = assignAvailability.allowed && !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(changeLabel)
                }
                if (display.assignedSpoolId != null && com.chronoswing.buddydash.network.BambuddyApi.hasInventoryUnassignEndpoint) {
                    TextButton(
                        onClick = onClearAssignment,
                        enabled = assignAvailability.allowed && !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.filament_action_clear_assignment))
                    }
                }
                display.spoolId?.let { spoolId ->
                    TextButton(
                        onClick = { onViewSpool(spoolId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.filament_view_spool_detail))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilamentSpoolPickerSheet(
    spools: List<SpoolInventoryItem>,
    searchQuery: String,
    slotLabel: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSpoolSelected: (SpoolInventoryItem) -> Unit,
    assignmentConflictForSpool: (SpoolInventoryItem) -> SpoolAssignmentTargetConflict,
    cardUsageForSpool: (SpoolInventoryItem) -> SpoolInventoryCardUsage,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.filament_picker_title, slotLabel),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.filament_picker_search_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
            )
            if (spools.isEmpty()) {
                Text(
                    text = stringResource(R.string.filament_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(spools, key = { it.id }) { spool ->
                        val conflict = assignmentConflictForSpool(spool)
                        FilamentPickerSpoolRow(
                            spool = spool,
                            conflict = conflict,
                            cardUsage = cardUsageForSpool(spool),
                            onClick = {
                                if (conflict !is SpoolAssignmentTargetConflict.AlreadyOnTarget) {
                                    onSpoolSelected(spool)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilamentPickerSpoolRow(
    spool: SpoolInventoryItem,
    conflict: SpoolAssignmentTargetConflict,
    cardUsage: SpoolInventoryCardUsage,
    onClick: () -> Unit,
) {
    val enabled = conflict !is SpoolAssignmentTargetConflict.AlreadyOnTarget
    val locationLine = when (conflict) {
        is SpoolAssignmentTargetConflict.AlreadyOnTarget ->
            stringResource(R.string.filament_picker_already_here)
        else -> formatSpoolInventoryCardLocationLine(spool)
    }
    val displayUsage = when {
        conflict is SpoolAssignmentTargetConflict.AlreadyOnTarget -> SpoolInventoryCardUsage.Normal
        else -> cardUsage
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .then(
                if (enabled) {
                    Modifier.buddyDashClickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        SpoolInventoryCardContent(
            spool = spool,
            locationLine = locationLine,
            cardUsage = displayUsage,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            locationMaxLines = 2,
        )
    }
}

@Composable
fun FilamentAssignSpoolDialog(
    spoolTitle: String,
    slotLabel: String,
    printerName: String,
    conflict: SpoolAssignmentTargetConflict,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val elsewhere = conflict as? SpoolAssignmentTargetConflict.AssignedElsewhere
    val titleRes = if (elsewhere != null) {
        R.string.filament_confirm_assign_elsewhere_title
    } else {
        R.string.filament_confirm_assign_title
    }
    val targetLocation = "$printerName • $slotLabel"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            if (elsewhere != null) {
                Text(
                    stringResource(
                        R.string.filament_confirm_assign_elsewhere_message,
                        spoolTitle,
                        formatSpoolAssignmentLocationBrief(elsewhere.assignment),
                        targetLocation,
                    ),
                )
            } else {
                Text(stringResource(R.string.filament_confirm_assign_message, spoolTitle, slotLabel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBusy) {
                Text(
                    stringResource(
                        if (elsewhere != null) {
                            R.string.filament_action_assign_anyway
                        } else {
                            R.string.filament_action_assign
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun FilamentClearAssignmentDialog(
    slotLabel: String,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filament_confirm_clear_title)) },
        text = {
            Text(stringResource(R.string.filament_confirm_clear_message, slotLabel))
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBusy) {
                Text(stringResource(R.string.filament_action_clear_assignment))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
