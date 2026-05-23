package com.chronoswing.buddydash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R

@Composable
fun OfflineStaleBanner(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    limited: Boolean = false,
    refreshFailed: Boolean = false,
) {
    val messageRes = when {
        limited -> R.string.offline_limited_cached_banner
        refreshFailed -> R.string.home_stale_banner
        else -> R.string.offline_stale_banner
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
            expandVertically(
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top,
            ),
        exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
            shrinkVertically(
                animationSpec = tween(160, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top,
            ),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        ) {
            Text(
                text = stringResource(messageRes),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            )
        }
    }
}
