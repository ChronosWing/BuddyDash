package com.chronoswing.buddydash.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.ripple

/** Tap scale + optional ripple for list rows and chips. */
@Composable
fun Modifier.buddyDashClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = rememberPrefersReducedMotion()
    val targetScale = if (enabled && pressed && !reduced) BuddyDashMotion.PRESS_SCALE else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "buddyDashClickScale",
    )
    val brightness = if (enabled && pressed && !reduced) 1.04f else 1f
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = brightness.coerceIn(0.92f, 1f)
        }
        .clickable(
            enabled = enabled,
            onClick = onClick,
            role = Role.Button,
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
        )
}

/** Press scale for Material buttons — pass the same [interactionSource] into the button. */
@Composable
fun Modifier.buddyDashButtonPress(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = rememberPrefersReducedMotion()
    val targetScale = if (enabled && pressed && !reduced) BuddyDashMotion.PRESS_SCALE else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "buddyDashButtonPressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun rememberBuddyDashInteractionSource(): MutableInteractionSource =
    remember { MutableInteractionSource() }
