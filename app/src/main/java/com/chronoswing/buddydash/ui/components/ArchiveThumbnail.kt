package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.network.archiveThumbnailUrl
import com.chronoswing.buddydash.ui.motion.BuddyDashMotion
import com.chronoswing.buddydash.ui.motion.rememberPrefersReducedMotion
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween

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

    BuddyDashFadeInThumbnail(
        imageUrl = imageUrl,
        cacheKey = "archive-thumb-$archiveId",
        modifier = modifier,
        size = size,
        shape = RoundedCornerShape(8.dp),
        onLoadFailed = { showImage = false },
    )
}

/** Centered hero thumbnail for Archive Detail — fit aspect ratio, placeholder when missing. */
@Composable
fun ArchiveDetailHeroImage(
    archiveId: Int,
    serverUrl: String,
    cameraToken: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 220.dp,
    maxHeight: Dp = 180.dp,
) {
    val imageUrl = remember(serverUrl, cameraToken, archiveId) {
        archiveThumbnailUrl(serverUrl, archiveId, cameraToken)
    }
    var showImage by remember(archiveId, imageUrl) { mutableStateOf(imageUrl != null) }
    val shape = RoundedCornerShape(12.dp)
    val frameModifier = Modifier
        .width(maxWidth)
        .height(maxHeight)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = frameModifier,
            contentAlignment = Alignment.Center,
        ) {
            if (!showImage || imageUrl == null) {
                ArchiveThumbnailPlaceholder()
            } else {
                val reduced = rememberPrefersReducedMotion()
                val fadeMs = if (reduced) 0 else BuddyDashMotion.THUMBNAIL_FADE_MS
                val context = LocalContext.current
                val request = remember(imageUrl) {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(false)
                        .listener(onError = { _, _ -> showImage = false })
                        .build()
                }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = stringResource(R.string.print_cover),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    loading = {
                        ArchiveThumbnailPlaceholder(showLabel = false)
                    },
                    error = { ArchiveThumbnailPlaceholder() },
                    success = { state ->
                        if (fadeMs <= 0) {
                            Image(
                                painter = state.painter,
                                contentDescription = stringResource(R.string.print_cover),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Crossfade(
                                targetState = state.painter,
                                animationSpec = tween(fadeMs),
                                label = "archiveHeroFade",
                            ) { painter ->
                                Image(
                                    painter = painter,
                                    contentDescription = stringResource(R.string.print_cover),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ArchiveThumbnailPlaceholder(
    showLabel: Boolean = true,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            )
            if (showLabel) {
                Text(
                    text = stringResource(R.string.archive_thumbnail_placeholder),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
