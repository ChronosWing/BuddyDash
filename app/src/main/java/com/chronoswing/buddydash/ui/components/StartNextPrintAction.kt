package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.StartNextQueuedPrintBlockReason
import com.chronoswing.buddydash.util.StartNextQueuedPrintReadiness

@Composable
fun StartNextPrintAction(
    printerName: String,
    readiness: StartNextQueuedPrintReadiness,
    isSubmitting: Boolean,
    onConfirmStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirm by remember { mutableStateOf(false) }
    val enabled = readiness.canStart && !isSubmitting
    val disabledReason = when {
        readiness.canStart || isSubmitting -> null
        readiness.blockReason == StartNextQueuedPrintBlockReason.PlateNotClear ->
            stringResource(R.string.start_next_print_blocked_plate)
        else -> stringResource(R.string.start_next_print_blocked_busy)
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showConfirm = false },
            title = { Text(stringResource(R.string.start_next_print_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.start_next_print_confirm_message,
                        printerName,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onConfirmStart()
                    },
                    enabled = !isSubmitting,
                ) {
                    Text(stringResource(R.string.start_next_print_confirm_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirm = false },
                    enabled = !isSubmitting,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { showConfirm = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(vertical = 2.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.start_next_print_action))
            }
        }
        disabledReason?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
