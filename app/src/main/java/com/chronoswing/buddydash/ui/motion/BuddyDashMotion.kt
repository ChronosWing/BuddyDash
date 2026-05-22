package com.chronoswing.buddydash.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp

object BuddyDashMotion {
    const val PROGRESS_DURATION_MS = 400
    const val CAMERA_CROSSFADE_MS = 250
    const val PRESS_SCALE = 0.975f
    const val NAV_DETAIL_MS = 220
    const val NAV_SECTION_MS = 200
    const val NAV_TAB_MS = 180
    const val THUMBNAIL_FADE_MS = 220
}

fun Context.prefersReducedMotion(): Boolean = isReducedMotionEnabled()

@Composable
fun rememberPrefersReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isReducedMotionEnabled() }
}

private fun Context.isReducedMotionEnabled(): Boolean =
    try {
        Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    } catch (_: Exception) {
        false
    }

/** Smooth progress fill for bars (print, filament, maintenance). */
@Composable
fun rememberAnimatedProgressFraction(target: Float): Float {
    val reduced = rememberPrefersReducedMotion()
    val clamped = target.coerceIn(0f, 1f)
    val animatable = remember { Animatable(clamped) }
    LaunchedEffect(clamped, reduced) {
        if (reduced) {
            animatable.snapTo(clamped)
        } else {
            animatable.animateTo(
                clamped,
                animationSpec = tween(
                    durationMillis = BuddyDashMotion.PROGRESS_DURATION_MS,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }
    return animatable.value
}

@Composable
fun AnimatedLinearProgressIndicator(
    targetFraction: Float,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    trackColor: androidx.compose.ui.graphics.Color =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
) {
    val animated = rememberAnimatedProgressFraction(targetFraction)
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier.then(
            if (height != null) Modifier.fillMaxWidth().height(height) else Modifier.fillMaxWidth(),
        ),
        color = color,
        trackColor = trackColor,
    )
}
