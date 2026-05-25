package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R

data class NfcSettingsExamplePrinter(
    val name: String,
    val id: Int,
)

@Composable
fun NfcStickersSettingsCard(
    examplePrinter: NfcSettingsExamplePrinter?,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    DetailInfoCard(modifier = modifier) {
        SectionHeader(stringResource(R.string.nfc_actions_title))

        Text(
            text = stringResource(R.string.nfc_actions_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )

        NfcInfoRow(
            icon = {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = muted.copy(alpha = 0.72f),
                )
            },
            text = stringResource(R.string.nfc_actions_security_compact),
        )

        NfcInfoRow(
            icon = {
                Icon(
                    Icons.Outlined.Print,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = muted.copy(alpha = 0.72f),
                )
            },
            text = stringResource(R.string.nfc_actions_find_id_hint),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.nfc_actions_supported),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = muted,
            )

            val exampleId = examplePrinter?.id?.toString() ?: "{printerId}"
            NfcActionRow("clear-plate", "buddydash://printer/$exampleId/clear-plate")
            NfcActionRow("toggle-power", "buddydash://printer/$exampleId/toggle-power")
            NfcActionRow("finish", "buddydash://printer/$exampleId/finish")
        }

        NfcInfoRow(
            icon = {
                Icon(
                    Icons.Outlined.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = muted.copy(alpha = 0.72f),
                )
            },
            text = stringResource(R.string.nfc_actions_power_note),
        )

        Text(
            text = stringResource(R.string.nfc_actions_writer_hint),
            style = MaterialTheme.typography.labelSmall,
            color = muted.copy(alpha = 0.62f),
        )
    }
}

@Composable
private fun NfcInfoRow(
    icon: @Composable () -> Unit,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NfcActionRow(
    actionSlug: String,
    exampleUri: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = actionSlug,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = exampleUri,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
