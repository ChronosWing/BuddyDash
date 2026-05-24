package com.chronoswing.buddydash.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.ui.theme.OfflineRed
import com.chronoswing.buddydash.util.BambuHmsLookup
import com.chronoswing.buddydash.util.HmsAlertLevel
import com.chronoswing.buddydash.util.HmsSeverity

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
                .padding(bottom = 24.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                val headerColor = if (hmsAlertSeverity == HmsSeverity.Error) OfflineRed else HmsAmberSheet
                Icon(
                    imageVector = if (hmsAlertSeverity == HmsSeverity.Error) {
                        Icons.Filled.Error
                    } else {
                        Icons.Outlined.Warning
                    },
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
    val context = LocalContext.current
    val lookupInfo = BambuHmsLookup.lookup(entry)

    // Prefer lookup-table level (authoritative); fall back to API-derived level
    val level: HmsAlertLevel? = lookupInfo?.alertLevel ?: when (entry.severity) {
        1 -> HmsAlertLevel.Error
        2 -> HmsAlertLevel.Warning
        3 -> HmsAlertLevel.Notification
        else -> null
    }

    val (levelLabel, levelColor) = when (level) {
        HmsAlertLevel.Error -> stringResource(R.string.hms_sheet_severity_error) to OfflineRed
        HmsAlertLevel.Warning -> stringResource(R.string.hms_sheet_severity_warning) to HmsAmberSheet
        HmsAlertLevel.Notification -> stringResource(R.string.hms_sheet_severity_notification) to
            MaterialTheme.colorScheme.onSurfaceVariant
        null -> stringResource(R.string.hms_sheet_severity_unknown) to HmsAmberSheet
    }

    val formattedCode = BambuHmsLookup.formatDisplayCode(entry)
    val message: String? = lookupInfo?.message
        ?: entry.detail?.trim()?.takeIf { it.isNotBlank() }
    val wikiUrl = BambuHmsLookup.wikiUrl(entry)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = levelColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Code + severity on one line
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (formattedCode != null) {
                    Text(
                        text = formattedCode,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
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

            // Message
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else if (formattedCode == null) {
                Text(
                    text = stringResource(R.string.hms_sheet_no_entries),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Wiki link
            if (wikiUrl != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(wikiUrl))
                        )
                    },
                ) {
                    Text(
                        text = stringResource(R.string.hms_sheet_wiki_link),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}
