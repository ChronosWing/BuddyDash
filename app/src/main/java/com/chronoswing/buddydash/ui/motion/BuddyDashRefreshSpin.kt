package com.chronoswing.buddydash.ui.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/** Continuous rotation while [active]; static when reduced motion. */
fun Modifier.refreshSpinning(active: Boolean): Modifier = composed {
    if (!active || rememberPrefersReducedMotion()) return@composed this
    val transition = rememberInfiniteTransition(label = "refreshSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
        ),
        label = "refreshSpinAngle",
    )
    graphicsLayer { rotationZ = angle }
}
