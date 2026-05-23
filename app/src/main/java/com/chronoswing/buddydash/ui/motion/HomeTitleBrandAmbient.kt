package com.chronoswing.buddydash.ui.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp

/** Clips the logo image to [slotWidth]. Glow is [HomeLogoGlowLayer]. */
@Composable
fun HomeTitleLogoSlot(
    slotWidth: Dp,
    ambientDiameter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(width = slotWidth, height = ambientDiameter)
            .clip(RectangleShape),
        contentAlignment = Alignment.CenterEnd,
        content = content,
    )
}
