package com.chronoswing.buddydash.ui.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/** 0..1 breathing phase for subtle chip/card pulses; static when reduced motion. */
@Composable
fun rememberAttentionPulse(
    enabled: Boolean,
    periodMillis: Int = 2_800,
): Float {
    if (!enabled) return 0f
    val reduced = rememberPrefersReducedMotion()
    if (reduced) return 0f
    val transition = rememberInfiniteTransition(label = "attentionPulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "attentionPulsePhase",
    )
    return phase
}
