package com.chronoswing.buddydash.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chronoswing.buddydash.ui.motion.BuddyDashMotion
import com.chronoswing.buddydash.ui.motion.rememberPrefersReducedMotion

/**
 * Thumbnail with neutral placeholder while loading and a soft fade-in on success.
 */
@Composable
fun BuddyDashFadeInThumbnail(
    imageUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Crop,
    onLoadFailed: () -> Unit = {},
    placeholder: @Composable () -> Unit = {
        ThumbnailPlaceholder(
            modifier = Modifier
                .size(size)
                .clip(shape),
        )
    },
) {
    val context = LocalContext.current
    val reduced = rememberPrefersReducedMotion()
    val fadeMs = if (reduced) 0 else BuddyDashMotion.THUMBNAIL_FADE_MS

    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(false)
            .listener(onError = { _, _ -> onLoadFailed() })
            .build()
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
            contentScale = contentScale,
            loading = { placeholder() },
            error = { placeholder() },
            success = { state ->
                if (fadeMs <= 0) {
                    Image(
                        painter = state.painter,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale,
                    )
                } else {
                    Crossfade(
                        targetState = state.painter,
                        animationSpec = tween(fadeMs),
                        label = "thumbnailFade",
                    ) { painter ->
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = contentScale,
                        )
                    }
                }
            },
        )
    }
}

@Composable
fun ThumbnailPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        ),
        contentAlignment = Alignment.Center,
    ) {}
}
