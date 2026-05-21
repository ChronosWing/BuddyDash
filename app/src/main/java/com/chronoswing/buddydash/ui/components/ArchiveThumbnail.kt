package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun ArchiveThumbnail(
    archiveId: Int,
    serverUrl: String,
    cameraToken: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val imageUrl = remember(serverUrl, cameraToken, archiveId) {
        archiveThumbnailUrl(serverUrl, archiveId, cameraToken)
    }
    var showImage by remember(archiveId, imageUrl) { mutableStateOf(imageUrl != null) }

    if (!showImage || imageUrl == null) return

    val context = LocalContext.current
    val shape = RoundedCornerShape(8.dp)
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
        modifier = modifier
            .size(size)
            .clip(shape),
        contentScale = ContentScale.Crop,
        loading = {
            lastPainter?.let { painter ->
                ArchiveThumbImage(painter = painter, shape = shape, size = size)
            }
        },
        error = { Box(Modifier.size(size)) },
        success = { state ->
            lastPainter = state.painter
            ArchiveThumbImage(painter = state.painter, shape = shape, size = size)
        },
    )
}

@Composable
private fun ArchiveThumbImage(
    painter: Painter,
    shape: RoundedCornerShape,
    size: Dp,
) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(shape),
        contentScale = ContentScale.Crop,
    )
}
