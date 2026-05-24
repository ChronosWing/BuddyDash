package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.ui.theme.OfflineRed
import com.chronoswing.buddydash.util.HmsAlertLevel
import com.chronoswing.buddydash.util.HmsSeverity
import com.chronoswing.buddydash.util.alertLevel

private val HmsAmberSheet = Color(0xFFFBBF24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HmsDetailSheet(
    printerName: String,
    hmsErrors: List<PrinterHmsError>,
    hmsAlertSeverity: HmsSeverity,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                val headerIcon = if (hmsAlertSeverity == HmsSeverity.Error) {
                    Icons.Filled.Error
                } else {
                    Icons.Outlined.Warning
                }
                val headerColor = if (hmsAlertSeverity == HmsSeverity.Error) OfflineRed else HmsAmberSheet
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(22.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.hms_sheet_title),
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

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            )

            if (hmsErrors.isEmpty()) {
                Text(
                    text = stringResource(R.string.hms_sheet_no_entries),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    hmsErrors.forEachIndexed { index, entry ->
                        HmsEntryRow(entry = entry, index = index, total = hmsErrors.size)
                    }
                }
            }
        }
    }
}

@Composable
private fun HmsEntryRow(
    entry: PrinterHmsError,
    index: Int,
    total: Int,
) {
    val level = entry.alertLevel()
    val (levelLabel, levelColor) = when (level) {
        HmsAlertLevel.Error -> stringResource(R.string.hms_sheet_severity_error) to OfflineRed
        HmsAlertLevel.Warning -> stringResource(R.string.hms_sheet_severity_warning) to HmsAmberSheet
        HmsAlertLevel.Notification -> stringResource(R.string.hms_sheet_severity_notification) to
            MaterialTheme.colorScheme.onSurfaceVariant
        null -> stringResource(R.string.hms_sheet_severity_unknown) to HmsAmberSheet
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = levelColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Severity badge + optional counter
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                if (total > 1) {
                    Text(
                        text = "${index + 1} / $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Human-readable detail if available
            val detail = entry.detail?.trim()?.takeIf { it.isNotBlank() }
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Code / module / attr metadata
            val metaLine = buildString {
                val code = entry.code.trim().takeIf { it.isNotBlank() }
                if (code != null) append("${stringResource(R.string.hms_sheet_code)}: $code")
                entry.module?.let {
                    if (isNotEmpty()) append("  ·  ")
                    append("${stringResource(R.string.hms_sheet_module)}: $it")
                }
            }
            if (metaLine.isNotBlank()) {
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Fallback when no detail and no meaningful metadata
            if (detail == null && metaLine.isBlank()) {
                Text(
                    text = stringResource(R.string.hms_sheet_no_entries),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
