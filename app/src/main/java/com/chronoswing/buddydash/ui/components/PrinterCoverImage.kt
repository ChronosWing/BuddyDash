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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.network.printerCoverUrl

/** Temporary: log cover URL and load failures. Set false before release. */
import com.chronoswing.buddydash.util.BuddyDashDebug
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
    printerId: Int,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    height: Dp? = null,
    cornerRadius: Dp = 10.dp,
) {
    if (size == null && height == null) return
    val imageUrl = remember(serverUrl, printerId, cameraToken) {
        printerCoverUrl(serverUrl, printerId, cameraToken)
    }
    if (imageUrl == null || printerId < 0) return

    val context = LocalContext.current
    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(false)
            .listener(
                onStart = {
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_COVER, "Cover load start printerId=$printerId")
                    }
                },
                onSuccess = { _, _ ->
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_COVER, "Cover load ok printerId=$printerId")
                    }
                },
                onError = { _, result ->
                    if (BuddyDashDebug.enabled) {
                        Log.d(
                            TAG_COVER,
                            "Cover load failed printerId=$printerId url=${redactCoverToken(imageUrl)} " +
                                "error=${result.throwable.message}",
                        )
                    }
                },
            )
            .build()
    }
    val shape = RoundedCornerShape(cornerRadius)

    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier,
        loading = { },
        error = { },
        success = { state ->
            val frameModifier = when {
                height != null -> Modifier
                    .fillMaxWidth()
                    .height(height)
                else -> Modifier.size(size!!)
            }
            Box(modifier = frameModifier.clip(shape)) {
                Image(
                    painter = state.painter,
                    contentDescription = stringResource(R.string.print_cover),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CoverScrimGradient),
                )
            }
        },
    )
}

private fun redactCoverToken(url: String): String =
    url.replace(Regex("token=[^&]+"), "token=***")
