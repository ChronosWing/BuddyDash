package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chronoswing.buddydash.network.archiveThumbnailUrl

/**
 * Compact thumbnail for spool usage rows — archive cover, usage-provided URL, or file icon.
 */
@Composable
fun SpoolUsageThumbnail(
    archiveId: Int?,
    usageImageUrl: String?,
    serverUrl: String,
    cameraToken: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val imageUrl = remember(serverUrl, cameraToken, archiveId, usageImageUrl) {
        when {
            archiveId != null -> archiveThumbnailUrl(serverUrl, archiveId, cameraToken)
            !usageImageUrl.isNullOrBlank() -> usageImageUrl
            else -> null
        }
    }
    val shape = RoundedCornerShape(8.dp)
    val frameModifier = modifier
        .size(size)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))

    var showImage by remember(archiveId, usageImageUrl, imageUrl) { mutableStateOf(imageUrl != null) }

    if (!showImage || imageUrl == null) {
        Box(frameModifier, contentAlignment = Alignment.Center) {
            SpoolUsageFileIcon()
        }
        return
    }

    val context = LocalContext.current
    var lastPainter by remember { mutableStateOf<Painter?>(null) }
    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(200)
            .listener(onError = { _, _ -> showImage = false })
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = frameModifier,
        contentScale = ContentScale.Crop,
        loading = {
            lastPainter?.let { painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop,
                )
            } ?: Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                SpoolUsageFileIcon()
            }
        },
        error = {
            Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                SpoolUsageFileIcon()
            }
        },
        success = { state ->
            lastPainter = state.painter
            Image(
                painter = state.painter,
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
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
