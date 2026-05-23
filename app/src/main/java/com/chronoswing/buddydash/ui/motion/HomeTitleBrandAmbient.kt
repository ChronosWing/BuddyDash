package com.chronoswing.buddydash.ui.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.chronoswing.buddydash.ui.theme.CyanAccent

/** ~4.5s teal breathe behind the Home header logo while printing. */
private const val AMBIENT_PULSE_PERIOD_MS = 4_500
private const val PRINTING_AMBIENT_ALPHA_MIN = 0.1f
private const val PRINTING_AMBIENT_ALPHA_MAX = 0.15f
private const val PRINTING_AMBIENT_RADIUS_SCALE = 0.58f

/** Static idle halo — visible but clearly below [PRINTING_AMBIENT_ALPHA_MIN]. */
private const val IDLE_AMBIENT_ALPHA = 0.07f
private const val IDLE_AMBIENT_RADIUS_SCALE = 0.64f

/**
 * Clips ambient glow to [slotWidth] so it cannot bleed into the title row.
 * Layout/size of [content] is unchanged — pulse is drawn behind it only.
 */
@Composable
fun HomeTitleLogoSlot(
    ambientPulseEnabled: Boolean,
    slotWidth: Dp,
    ambientDiameter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val pulseActive = ambientPulseEnabled && isVisible && isResumed

    Box(
        modifier = modifier
            .size(width = slotWidth, height = ambientDiameter)
            .clip(RectangleShape)
            .onVisibilityChanged { visible -> isVisible = visible },
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (pulseActive) {
            HomeTitleLogoPrintingAmbientGlow(
                diameter = ambientDiameter,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        } else {
            HomeTitleLogoIdleAmbientGlow(
                diameter = ambientDiameter,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        content()
    }
}

@Composable
private fun HomeTitleLogoIdleAmbientGlow(
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(diameter)
            .drawBehind {
                val radius = size.minDimension * IDLE_AMBIENT_RADIUS_SCALE
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to CyanAccent.copy(alpha = IDLE_AMBIENT_ALPHA),
                            0.35f to CyanAccent.copy(alpha = IDLE_AMBIENT_ALPHA * 0.5f),
                            0.68f to CyanAccent.copy(alpha = IDLE_AMBIENT_ALPHA * 0.18f),
                            1f to Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            },
    )
}

@Composable
private fun HomeTitleLogoPrintingAmbientGlow(
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val phase = rememberAttentionPulse(enabled = true, periodMillis = AMBIENT_PULSE_PERIOD_MS)
    val ambientAlpha = PRINTING_AMBIENT_ALPHA_MIN +
        phase * (PRINTING_AMBIENT_ALPHA_MAX - PRINTING_AMBIENT_ALPHA_MIN)
    Box(
        modifier = modifier
            .size(diameter)
            .drawBehind {
                val radius = size.minDimension * PRINTING_AMBIENT_RADIUS_SCALE
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to CyanAccent.copy(alpha = ambientAlpha),
                            0.32f to CyanAccent.copy(alpha = ambientAlpha * 0.55f),
                            0.65f to CyanAccent.copy(alpha = ambientAlpha * 0.2f),
                            1f to Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            },
    )
}
