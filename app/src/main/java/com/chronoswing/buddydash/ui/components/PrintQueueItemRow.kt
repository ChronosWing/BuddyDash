package com.chronoswing.buddydash.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.network.queueJobThumbnailUrl
import com.chronoswing.buddydash.util.QUEUE_DISPLAY_NAME_FALLBACK
import com.chronoswing.buddydash.util.formatQueueDurationAndFilament

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
    val metaLine = formatQueueDurationAndFilament(job.estimatedDurationSeconds, job.filamentUsage)
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
                "jobId=${job.id} name=$displayName meta=$metaLine " +
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
            BuddyDashFadeInThumbnail(
                imageUrl = thumbUrl,
                size = 44.dp,
                shape = RoundedCornerShape(8.dp),
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
            metaLine?.let { line ->
                FilamentUsageText(text = line)
            }
        }
    }
}

private fun redactToken(url: String): String =
    url.replace(Regex("token=[^&]+"), "token=***")
