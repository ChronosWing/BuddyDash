package com.chronoswing.buddydash.ui.motion

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import kotlin.math.roundToInt

// Material 3 standard easing: rapid initial movement, smooth deceleration into final position.
// Produces a more premium, intentional feel than FastOutSlowIn for navigation.
private val NavDetailEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/** Detail push: subtle depth shift + fade (~220ms). Slide is 12% of container width. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashDetailEnter(
    context: Context,
): EnterTransition {
    val duration = BuddyDashMotion.NAV_DETAIL_MS
    val fraction = BuddyDashMotion.NAV_DETAIL_SLIDE_FRACTION
    return if (context.prefersReducedMotion()) {
        fadeIn(tween(duration, easing = FastOutSlowInEasing))
    } else {
        fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(duration, easing = NavDetailEasing),
                initialOffset = { (it * fraction).roundToInt() },
            )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashDetailExit(
    context: Context,
): ExitTransition {
    val duration = BuddyDashMotion.NAV_DETAIL_MS
    val fraction = BuddyDashMotion.NAV_DETAIL_SLIDE_FRACTION
    return if (context.prefersReducedMotion()) {
        fadeOut(tween(duration, easing = FastOutSlowInEasing))
    } else {
        fadeOut(tween(duration, easing = FastOutSlowInEasing)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(duration, easing = NavDetailEasing),
                targetOffset = { (it * fraction).roundToInt() },
            )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashDetailPopEnter(
    context: Context,
): EnterTransition {
    val duration = BuddyDashMotion.NAV_DETAIL_MS
    val fraction = BuddyDashMotion.NAV_DETAIL_SLIDE_FRACTION
    return if (context.prefersReducedMotion()) {
        fadeIn(tween(duration, easing = FastOutSlowInEasing))
    } else {
        fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(duration, easing = NavDetailEasing),
                initialOffset = { (it * fraction).roundToInt() },
            )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashDetailPopExit(
    context: Context,
): ExitTransition {
    val duration = BuddyDashMotion.NAV_DETAIL_MS
    val fraction = BuddyDashMotion.NAV_DETAIL_SLIDE_FRACTION
    return if (context.prefersReducedMotion()) {
        fadeOut(tween(duration, easing = FastOutSlowInEasing))
    } else {
        fadeOut(tween(duration, easing = FastOutSlowInEasing)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(duration, easing = NavDetailEasing),
                targetOffset = { (it * fraction).roundToInt() },
            )
    }
}

/** Bottom-nav section change: soft fade + slight vertical shift. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashSectionEnter(
    context: Context,
): EnterTransition {
    val duration = BuddyDashMotion.NAV_SECTION_MS
    val easing = FastOutSlowInEasing
    return if (context.prefersReducedMotion()) {
        fadeIn(tween(duration, easing = easing))
    } else {
        fadeIn(tween(duration, easing = easing)) +
            slideInVertically(
                animationSpec = tween(duration, easing = easing),
                initialOffsetY = { fullHeight -> fullHeight / 24 },
            )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashSectionExit(
    context: Context,
): ExitTransition {
    val duration = BuddyDashMotion.NAV_SECTION_MS
    val easing = FastOutSlowInEasing
    return if (context.prefersReducedMotion()) {
        fadeOut(tween(duration, easing = easing))
    } else {
        fadeOut(tween(duration, easing = easing)) +
            slideOutVertically(
                animationSpec = tween(duration, easing = easing),
                targetOffsetY = { fullHeight -> -fullHeight / 28 },
            )
    }
}
