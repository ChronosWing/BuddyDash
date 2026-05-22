package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.network.archiveThumbnailUrl

/**
 * Compact thumbnail for spool usage rows — archive cover, usage-provided URL, or file icon.
 */
@Composable
fun SpoolUsageThumbnail(
    archiveId: Int?,
    serverUrl: String,
    cameraToken: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val imageUrl = remember(serverUrl, cameraToken, archiveId) {
        archiveId?.let { archiveThumbnailUrl(serverUrl, it, cameraToken) }
    }
    val shape = RoundedCornerShape(8.dp)

    var showImage by remember(archiveId, imageUrl) { mutableStateOf(imageUrl != null) }

    if (!showImage || imageUrl == null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape),
            contentAlignment = Alignment.Center,
        ) {
            SpoolUsageFileIcon()
        }
        return
    }

    BuddyDashFadeInThumbnail(
        imageUrl = imageUrl,
        modifier = modifier,
        size = size,
        shape = shape,
        onLoadFailed = { showImage = false },
        placeholder = {
            ThumbnailPlaceholder(
                modifier = Modifier
                    .size(size)
                    .clip(shape),
            )
        },
    )
}

@Composable
private fun SpoolUsageFileIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    )
}
