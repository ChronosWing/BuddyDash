package com.chronoswing.buddydash.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.CurrentPrintThumbnailIdentity
import com.chronoswing.buddydash.util.logCurrentPrintThumbnailIdentity
import com.chronoswing.buddydash.util.resolveCurrentPrintThumbnailUrl

private const val TAG_COVER = "BuddyDash/Cover"

private val CoverScrimGradient = Brush.verticalGradient(
    0f to Color.Transparent,
    0.5f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.38f),
)

@Composable
fun PrinterCoverImage(
    serverUrl: String,
    cameraToken: String,
    thumbnailIdentity: CurrentPrintThumbnailIdentity,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    height: Dp? = null,
    cornerRadius: Dp = 10.dp,
    fillContainer: Boolean = false,
) {
    if (size == null && height == null && !fillContainer) return
    val imageUrl = remember(serverUrl, cameraToken, thumbnailIdentity) {
        resolveCurrentPrintThumbnailUrl(serverUrl, cameraToken, thumbnailIdentity)
    }
    if (imageUrl == null || thumbnailIdentity.printerId < 0) return

    val imageCacheKey = remember(thumbnailIdentity) { thumbnailIdentity.cacheKey() }
    var lastPainter by remember(imageCacheKey) { mutableStateOf<Painter?>(null) }
    var loadedForKey by remember(imageCacheKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(thumbnailIdentity, imageUrl, imageCacheKey) {
        logCurrentPrintThumbnailIdentity(thumbnailIdentity, imageUrl)
    }

    val context = LocalContext.current
    val request = remember(imageUrl, imageCacheKey) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(imageCacheKey)
            .diskCacheKey(imageCacheKey)
            .crossfade(false)
            .listener(
                onStart = {
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_COVER, "Cover load start cacheKey=$imageCacheKey")
                    }
                },
                onSuccess = { _, _ ->
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_COVER, "Cover load ok cacheKey=$imageCacheKey")
                    }
                },
                onError = { _, result ->
                    if (BuddyDashDebug.enabled) {
                        Log.d(
                            TAG_COVER,
                            "Cover load failed cacheKey=$imageCacheKey " +
                                "error=${result.throwable.message}",
                        )
                    }
                },
            )
            .build()
    }
    val shape = RoundedCornerShape(cornerRadius)
    val canRetainPrevious = loadedForKey == imageCacheKey

    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = if (fillContainer) modifier.fillMaxSize() else modifier,
        loading = {
            if (canRetainPrevious && lastPainter != null) {
                CoverImageFrame(
                    size = size,
                    height = height,
                    shape = shape,
                    painter = lastPainter!!,
                    fillContainer = fillContainer,
                    modifier = modifier,
                )
            } else {
                CoverPlaceholderFrame(size, height, shape, fillContainer, modifier)
            }
        },
        error = {
            if (canRetainPrevious && lastPainter != null) {
                CoverImageFrame(
                    size = size,
                    height = height,
                    shape = shape,
                    painter = lastPainter!!,
                    fillContainer = fillContainer,
                    modifier = modifier,
                )
            } else {
                CoverPlaceholderFrame(size, height, shape, fillContainer, modifier)
            }
        },
        success = { state ->
            lastPainter = state.painter
            loadedForKey = imageCacheKey
            CoverImageFrame(
                size = size,
                height = height,
                shape = shape,
                painter = state.painter,
                showScrim = true,
                fillContainer = fillContainer,
                modifier = modifier,
            )
        },
    )
}

@Composable
private fun CoverPlaceholderFrame(
    size: Dp?,
    height: Dp?,
    shape: RoundedCornerShape,
    fillContainer: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val frameModifier = coverFrameModifier(size, height, fillContainer, modifier)
    ThumbnailPlaceholder(modifier = frameModifier.clip(shape))
}

@Composable
private fun CoverImageFrame(
    size: Dp?,
    height: Dp?,
    shape: RoundedCornerShape,
    painter: Painter,
    showScrim: Boolean = false,
    fillContainer: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val frameModifier = coverFrameModifier(size, height, fillContainer, modifier)
    Box(modifier = frameModifier.clip(shape)) {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.print_cover),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (showScrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CoverScrimGradient),
            )
        }
    }
}

private fun coverFrameModifier(
    size: Dp?,
    height: Dp?,
    fillContainer: Boolean = false,
    modifier: Modifier = Modifier,
): Modifier = when {
    fillContainer -> modifier.fillMaxSize()
    height != null -> modifier.fillMaxWidth().height(height)
    else -> modifier.size(size!!)
}
