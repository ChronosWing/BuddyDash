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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import kotlinx.coroutines.delay

/** Presentation-only delay before showing offline/stale banner (avoids startup/resume flash). */
const val OFFLINE_STALE_BANNER_SHOW_DELAY_MS = 2_000L

/**
 * Debounces banner visibility: hides immediately when [pendingVisible] is false;
 * shows only after [delayMs] if still pending. Does not affect cache or refresh logic.
 */
@Composable
fun rememberDelayedOfflineBannerVisible(
    pendingVisible: Boolean,
    delayMs: Long = OFFLINE_STALE_BANNER_SHOW_DELAY_MS,
): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(pendingVisible, delayMs) {
        if (!pendingVisible) {
            visible = false
            return@LaunchedEffect
        }
        delay(delayMs)
        visible = pendingVisible
    }
    return visible
}

@Composable
fun OfflineStaleBanner(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    limited: Boolean = false,
    refreshFailed: Boolean = false,
    debounceShow: Boolean = true,
) {
    val showBanner = if (debounceShow) {
        rememberDelayedOfflineBannerVisible(visible)
    } else {
        visible
    }
    val messageRes = when {
        limited -> R.string.offline_limited_cached_banner
        refreshFailed -> R.string.home_stale_banner
        else -> R.string.offline_stale_banner
    }
    AnimatedVisibility(
        visible = showBanner,
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
