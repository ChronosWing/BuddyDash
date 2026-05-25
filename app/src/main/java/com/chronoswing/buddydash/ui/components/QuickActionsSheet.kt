package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.util.PrinterActivityKind
import com.chronoswing.buddydash.util.resolveActivityKind

enum class QuickAction {
    ClearPlate,
    TogglePower,
    Finish,
    ToggleLight,
    PauseResume,
    OpenDetail,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsSheet(
    printer: Printer,
    hasSmartOutlet: Boolean,
    hasLight: Boolean,
    onAction: (QuickAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activityKind = printer.liveStatus?.resolveActivityKind() ?: PrinterActivityKind.Idle

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = printer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            when (activityKind) {
                PrinterActivityKind.Idle -> {
                    QuickActionRow(
                        icon = Icons.Default.CleaningServices,
                        label = stringResource(R.string.quick_action_clear_plate),
                        onClick = { onAction(QuickAction.ClearPlate) },
                    )
                    if (hasSmartOutlet) {
                        QuickActionRow(
                            icon = Icons.Default.PowerSettingsNew,
                            label = stringResource(R.string.quick_action_toggle_power),
                            onClick = { onAction(QuickAction.TogglePower) },
                        )
                    }
                    QuickActionRow(
                        icon = Icons.Default.TaskAlt,
                        label = stringResource(R.string.quick_action_finish),
                        onClick = { onAction(QuickAction.Finish) },
                    )
                }
                PrinterActivityKind.Printing -> {
                    if (hasLight) {
                        QuickActionRow(
                            icon = Icons.Default.Lightbulb,
                            label = stringResource(R.string.quick_action_toggle_light),
                            onClick = { onAction(QuickAction.ToggleLight) },
                        )
                    }
                }
                PrinterActivityKind.Paused -> {
                    if (hasLight) {
                        QuickActionRow(
                            icon = Icons.Default.Lightbulb,
                            label = stringResource(R.string.quick_action_toggle_light),
                            onClick = { onAction(QuickAction.ToggleLight) },
                        )
                    }
                }
                PrinterActivityKind.Busy -> Unit
                PrinterActivityKind.Error -> Unit
                PrinterActivityKind.Offline -> Unit
            }

            QuickActionRow(
                icon = Icons.Default.Info,
                label = stringResource(R.string.quick_action_open_detail),
                onClick = { onAction(QuickAction.OpenDetail) },
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
