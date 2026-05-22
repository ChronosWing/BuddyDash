package com.chronoswing.buddydash.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/** Brief scale pulse when [trigger] increments (e.g. action succeeded). */
@Composable
fun Modifier.successPulseOn(trigger: Int): Modifier = composed {
    val reduced = rememberPrefersReducedMotion()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger <= 0 || reduced) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        scale.snapTo(1f)
        scale.animateTo(0.94f, tween(BuddyDashMotion.SUCCESS_PULSE_MS / 2, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(BuddyDashMotion.SUCCESS_PULSE_MS / 2, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
