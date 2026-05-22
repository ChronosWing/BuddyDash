package com.chronoswing.buddydash.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Fades tab content on switch without changing layout structure.
 * Uses [Column] (not Box/AnimatedContent) so multi-child tab composables stack vertically.
 */
@Composable
fun BuddyDashTabFadeContainer(
    selectedTab: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduced = rememberPrefersReducedMotion()
    val alpha = remember { Animatable(1f) }
    var lastTab by rememberSaveable { mutableIntStateOf(selectedTab) }

    LaunchedEffect(selectedTab) {
        if (reduced || selectedTab == lastTab) {
            lastTab = selectedTab
            alpha.snapTo(1f)
            return@LaunchedEffect
        }
        lastTab = selectedTab
        alpha.snapTo(0.94f)
        alpha.animateTo(
            1f,
            animationSpec = tween(BuddyDashMotion.NAV_TAB_MS, easing = FastOutSlowInEasing),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha.value },
    ) {
        content()
    }
}
