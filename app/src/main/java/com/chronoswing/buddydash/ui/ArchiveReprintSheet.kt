package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.ArchiveReprintSheetState
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.ui.components.ArchiveThumbnail
import com.chronoswing.buddydash.ui.components.PrintFileNameText
import com.chronoswing.buddydash.util.ARCHIVE_DISPLAY_NAME_FALLBACK
import com.chronoswing.buddydash.util.ArchiveReprintPrinter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveReprintSheet(
    archive: PrintArchive,
    serverUrl: String,
    cameraToken: String,
    sheetState: ArchiveReprintSheetState,
    onDismiss: () -> Unit,
    onPrinterSelected: (Int) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onConfirm: () -> Unit,
) {
    if (!sheetState.isOpen) return

    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val displayName = if (archive.displayName == ARCHIVE_DISPLAY_NAME_FALLBACK) {
        stringResource(R.string.archive_unnamed_print)
    } else {
        archive.displayName
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!sheetState.isSubmitting) onDismiss()
        },
        sheetState = modalState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArchiveThumbnail(
                    archiveId = archive.id,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    size = 52.dp,
                )
                PrintFileNameText(
                    fileName = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }

            when {
                sheetState.isLoadingPrinters -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
                sheetState.compatiblePrinters.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.archive_reprint_no_printers),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    ReprintPrinterField(
                        printers = sheetState.compatiblePrinters,
                        selectedPrinterId = sheetState.selectedPrinterId,
                        enabled = !sheetState.isSubmitting,
                        onPrinterSelected = onPrinterSelected,
                    )
                    if (sheetState.hiddenIncompatibleCount > 0) {
                        Text(
                            text = stringResource(
                                R.string.archive_reprint_incompatible_hidden,
                                sheetState.hiddenIncompatibleCount,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        )
                    }
                    ReprintQuantityRow(
                        quantity = sheetState.quantity,
                        enabled = !sheetState.isSubmitting,
                        onQuantityChange = onQuantityChange,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !sheetState.isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = sheetState.canSubmit && !sheetState.isSubmitting && !sheetState.isLoadingPrinters,
                    modifier = Modifier.weight(1f),
                ) {
                    if (sheetState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.archive_reprint_queue_print))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReprintPrinterField(
    printers: List<ArchiveReprintPrinter>,
    selectedPrinterId: Int?,
    enabled: Boolean,
    onPrinterSelected: (Int) -> Unit,
) {
    val selected = printers.find { it.id == selectedPrinterId } ?: printers.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.archive_label_printer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
        if (printers.size == 1 && selected != null) {
            Text(
                text = selected.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            var expanded by remember { mutableStateOf(false) }
            Column {
                TextButton(
                    onClick = { if (enabled) expanded = true },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = selected?.name ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    printers.forEach { printer ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = printer.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = {
                                onPrinterSelected(printer.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReprintQuantityRow(
    quantity: Int,
    enabled: Boolean,
    onQuantityChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.archive_reprint_quantity),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = { onQuantityChange(-1) },
                enabled = enabled && quantity > 1,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.archive_reprint_decrease_quantity),
                )
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(
                onClick = { onQuantityChange(1) },
                enabled = enabled && quantity < 99,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.archive_reprint_increase_quantity),
                )
            }
        }
    }
}
