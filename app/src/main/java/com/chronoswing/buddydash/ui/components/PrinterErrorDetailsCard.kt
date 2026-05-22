package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.PRINTER_ERROR_PREVIEW_COUNT
import com.chronoswing.buddydash.util.PrinterErrorDisplay

@Composable
fun PrinterErrorDetailsCard(
    display: PrinterErrorDisplay,
    expanded: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!display.showCard || display.lines.isEmpty()) return

    val previewCount = if (expanded) display.lines.size else PRINTER_ERROR_PREVIEW_COUNT
    val visibleLines = display.lines.take(previewCount)
    val hiddenCount = display.lines.size - visibleLines.size

    DetailInfoCard(modifier = modifier) {
        SectionHeader(stringResource(R.string.printer_error_details_title))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            visibleLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (hiddenCount > 0) {
                TextButton(
                    onClick = onExpand,
                    modifier = Modifier.padding(top = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.printer_error_show_more, hiddenCount),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
