package com.chronoswing.buddydash.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.network.queueJobThumbnailUrl
import com.chronoswing.buddydash.util.QUEUE_DISPLAY_NAME_FALLBACK
import com.chronoswing.buddydash.util.formatQueueDuration

private const val DEBUG_LOG_QUEUE_THUMB = true
private const val TAG_QUEUE_THUMB = "BuddyDash/Queue"

@Composable
fun PrintQueueItemRow(
    job: PrintQueueJob,
    rowLabel: String,
    serverUrl: String,
    cameraToken: String,
    modifier: Modifier = Modifier,
) {
    val duration = formatQueueDuration(job.estimatedDurationSeconds)
    val thumbResult = remember(serverUrl, cameraToken, job) {
        queueJobThumbnailUrl(serverUrl, cameraToken, job)
    }
    val thumbUrl = thumbResult.url
    val displayName = if (job.displayName == QUEUE_DISPLAY_NAME_FALLBACK) {
        stringResource(R.string.queue_unnamed_print)
    } else {
        job.displayName
    }
    var showThumbnail by remember(job.id, thumbResult) { mutableStateOf(thumbUrl != null) }

    if (DEBUG_LOG_QUEUE_THUMB) {
        LaunchedEffect(job.id, thumbResult, displayName) {
            Log.d(
                TAG_QUEUE_THUMB,
                "jobId=${job.id} name=$displayName duration=$duration " +
                    "source=${thumbResult.source} finalUrl=${redactToken(thumbUrl.orEmpty())}",
            )
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (thumbUrl != null && showThumbnail) {
            QueueJobThumbnail(
                imageUrl = thumbUrl,
                jobId = job.id,
                onLoadFailed = { showThumbnail = false },
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = rowLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
            PrintFileNameText(
                fileName = displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            duration?.let { dur ->
                Text(
                    text = dur,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
internal fun QueueJobThumbnail(
    imageUrl: String,
    jobId: Int,
    onLoadFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(8.dp)
    var lastPainter by remember { mutableStateOf<Painter?>(null) }

    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(200)
            .listener(
                onError = { _, result ->
                    if (DEBUG_LOG_QUEUE_THUMB) {
                        Log.d(
                            TAG_QUEUE_THUMB,
                            "thumb failed jobId=$jobId error=${result.throwable.message}",
                        )
                    }
                    onLoadFailed()
                },
            )
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier
            .size(44.dp)
            .clip(shape),
        contentScale = ContentScale.Crop,
        loading = {
            lastPainter?.let { painter ->
                QueueThumbnailImage(painter = painter, shape = shape)
            }
        },
        error = {
            lastPainter?.let { painter ->
                QueueThumbnailImage(painter = painter, shape = shape)
            }
        },
        success = { state ->
            lastPainter = state.painter
            QueueThumbnailImage(painter = state.painter, shape = shape)
        },
    )
}

@Composable
private fun QueueThumbnailImage(
    painter: Painter,
    shape: RoundedCornerShape,
) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .size(44.dp)
            .clip(shape),
        contentScale = ContentScale.Crop,
    )
}

private fun redactToken(url: String): String =
    url.replace(Regex("token=[^&]+"), "token=***")
