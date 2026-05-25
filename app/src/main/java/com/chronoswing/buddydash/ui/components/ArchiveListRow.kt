package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.util.ARCHIVE_DISPLAY_NAME_FALLBACK
import com.chronoswing.buddydash.util.archiveHasMaterialDisplay
import com.chronoswing.buddydash.util.formatArchiveListMetaLine
import com.chronoswing.buddydash.util.formatArchiveStatusPrinterLine

@Composable
fun ArchiveListRow(
    archive: PrintArchive,
    serverUrl: String,
    cameraToken: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val displayName = if (archive.displayName == ARCHIVE_DISPLAY_NAME_FALLBACK) {
        stringResource(R.string.archive_unnamed_print)
    } else {
        archive.displayName
    }
    val statusLine = formatArchiveStatusPrinterLine(archive)
    val metaLine = formatArchiveListMetaLine(archive)
    val showMaterial = archiveHasMaterialDisplay(archive)
    val verticalPadding = if (compact) 8.dp else 10.dp
    val lineSpacing = if (compact) 2.dp else 3.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ArchiveThumbnail(
                archiveId = archive.id,
                serverUrl = serverUrl,
                cameraToken = cameraToken,
                size = 52.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(lineSpacing),
            ) {
                PrintFileNameText(
                    fileName = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metaLine != null || compact) {
                    Text(
                        text = metaLine.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (compact) {
                            Modifier.defaultMinSize(minHeight = 14.dp)
                        } else {
                            Modifier
                        },
                    )
                }
                if (showMaterial) {
                    ArchiveMaterialRow(archive = archive)
                }
            }
        }
    }
}
