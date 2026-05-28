package com.chronoswing.buddydash.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chronoswing.buddydash.R

/** Confirmation when powering off a smart outlet while the printer appears busy. */
@Composable
fun SmartPlugBusyPowerOffDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.machine_power_off_active_confirm_title)) },
        text = { Text(stringResource(R.string.machine_power_off_active_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.machine_power_off_active_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
