package com.chronoswing.buddydash.ui.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme

/** Subtle shimmer for skeleton blocks; null when reduced motion (static fill). */
@Composable
fun skeletonShimmerBrush(reducedMotion: Boolean): Brush? {
    if (reducedMotion) return null
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonShimmerPhase",
    )
    return Brush.linearGradient(
        0f to base,
        0.45f to highlight,
        0.55f to highlight,
        1f to base,
        start = Offset(phase * 800f - 200f, 0f),
        end = Offset(phase * 800f + 200f, 120f),
    )
}
