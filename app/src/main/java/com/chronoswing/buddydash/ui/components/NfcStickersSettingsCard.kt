package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.NFC_CLEAR_PLATE_EXAMPLE_URI
import com.chronoswing.buddydash.util.buildNfcClearPlateUri
import com.chronoswing.buddydash.util.buildNfcClearPlateUriByName

data class NfcSettingsExamplePrinter(
    val name: String,
    val id: Int,
)

@Composable
fun NfcStickersSettingsCard(
    examplePrinter: NfcSettingsExamplePrinter?,
    onNfcLinkCopied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    DetailInfoCard(modifier = modifier) {
        SectionHeader(stringResource(R.string.nfc_stickers_title))

        Text(
            text = stringResource(R.string.nfc_stickers_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )

        NfcInfoRow(
            icon = { Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = muted.copy(alpha = 0.72f)) },
            text = stringResource(R.string.nfc_stickers_security_compact),
        )

        NfcInfoRow(
            icon = { Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = muted.copy(alpha = 0.72f)) },
            text = stringResource(R.string.nfc_stickers_find_id_hint),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val preferredUri = if (examplePrinter != null) {
                buildNfcClearPlateUri(examplePrinter.id)
            } else {
                NFC_CLEAR_PLATE_EXAMPLE_URI
            }
            val preferredLabel = if (examplePrinter != null) {
                stringResource(R.string.nfc_format_preferred_for, examplePrinter.name)
            } else {
                stringResource(R.string.nfc_format_id_preferred)
            }

            NfcLinkChip(
                label = preferredLabel,
                uri = preferredUri,
                onCopy = {
                    clipboard.setText(AnnotatedString(preferredUri))
                    onNfcLinkCopied()
                },
            )

            val fallbackUri = if (examplePrinter != null) {
                buildNfcClearPlateUriByName(examplePrinter.name)
            } else {
                stringResource(R.string.nfc_uri_placeholder_name)
            }

            NfcLinkChip(
                label = stringResource(R.string.nfc_format_name_fallback),
                uri = fallbackUri,
                onCopy = {
                    clipboard.setText(AnnotatedString(fallbackUri))
                    onNfcLinkCopied()
                },
            )
        }

        Text(
            text = stringResource(R.string.nfc_stickers_writer_hint_compact),
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
private fun NfcLinkChip(
    label: String,
    uri: String,
    onCopy: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uri,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.nfc_copy_link_cd),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}
