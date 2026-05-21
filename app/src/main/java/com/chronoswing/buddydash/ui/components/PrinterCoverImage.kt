package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.network.printerCoverUrl

@Composable
fun PrinterCoverImage(
    serverUrl: String,
    apiKey: String,
    printerId: Int,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    height: Dp? = null,
    cornerRadius: Dp = 10.dp,
) {
    if (size == null && height == null) return
    val imageUrl = remember(serverUrl, printerId) { printerCoverUrl(serverUrl, printerId) }
    val trimmedKey = apiKey.trim()
    if (imageUrl == null || trimmedKey.isEmpty() || printerId < 0) return

    val context = LocalContext.current
    val request = remember(imageUrl, trimmedKey) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .addHeader("X-API-Key", trimmedKey)
            .crossfade(false)
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
            val imageModifier = when {
                height != null -> Modifier
                    .fillMaxWidth()
                    .height(height)
                else -> Modifier.size(size!!)
            }
            Image(
                painter = state.painter,
                contentDescription = stringResource(R.string.print_cover),
                modifier = imageModifier.clip(shape),
                contentScale = ContentScale.Crop,
            )
        },
    )
}
