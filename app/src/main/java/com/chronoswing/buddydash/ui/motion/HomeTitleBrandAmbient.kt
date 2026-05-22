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

/** ~4s teal opacity breathe behind the Home header logo (printing + online only). */
private const val AMBIENT_PULSE_PERIOD_MS = 4_000
private const val AMBIENT_ALPHA_MIN = 0.02f
private const val AMBIENT_ALPHA_MAX = 0.055f

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
            HomeTitleLogoAmbientGlow(
                diameter = ambientDiameter,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        content()
    }
}

@Composable
private fun HomeTitleLogoAmbientGlow(
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val phase = rememberAttentionPulse(enabled = true, periodMillis = AMBIENT_PULSE_PERIOD_MS)
    val ambientAlpha = AMBIENT_ALPHA_MIN + phase * (AMBIENT_ALPHA_MAX - AMBIENT_ALPHA_MIN)
    Box(
        modifier = modifier
            .size(diameter)
            .drawBehind {
                val radius = size.minDimension * 0.52f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanAccent.copy(alpha = ambientAlpha),
                            Color.Transparent,
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
