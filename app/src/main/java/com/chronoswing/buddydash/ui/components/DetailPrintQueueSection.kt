package com.chronoswing.buddydash.ui.components

import com.chronoswing.buddydash.ui.motion.buddyDashClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.util.DETAIL_QUEUE_VISIBLE_LIMIT

@Composable
fun DetailPrintQueueSection(
    jobs: List<PrintQueueJob>,
    serverUrl: String,
    cameraToken: String,
    onViewFullQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (jobs.isEmpty()) return
    val totalCount = jobs.size
    val visibleJobs = jobs.take(DETAIL_QUEUE_VISIBLE_LIMIT)
    val moreCount = totalCount - visibleJobs.size

    DetailInfoCard(
        modifier = modifier.buddyDashClickable(onClick = onViewFullQueue),
    ) {
        SectionHeader(stringResource(R.string.queue_section_title, totalCount))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visibleJobs.forEachIndexed { index, job ->
                val label = when (index) {
                    0 -> stringResource(R.string.queue_next)
                    1 -> stringResource(R.string.queue_then)
                    else -> stringResource(R.string.queue_item_number, index + 1)
                }
                PrintQueueItemRow(
                    job = job,
                    rowLabel = label,
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                )
            }
            if (moreCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.queue_more_count, moreCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                    Text(
                        text = stringResource(R.string.queue_view_full),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
                    )
                }
            }
        }
    }
}
