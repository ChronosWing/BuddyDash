package com.chronoswing.buddydash.ui.motion

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation.NavBackStackEntry

/** Detail push: gentle slide + fade (~220ms). */
fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashDetailEnter(
    context: Context,
): EnterTransition {
    val duration = BuddyDashMotion.NAV_DETAIL_MS
    val easing = FastOutSlowInEasing
    return if (context.prefersReducedMotion()) {
        fadeIn(tween(duration, easing = easing))
    } else {
        fadeIn(tween(duration, easing = easing)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(duration, easing = easing),
            )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashDetailExit(
    context: Context,
): ExitTransition {
    val duration = BuddyDashMotion.NAV_DETAIL_MS
    val easing = FastOutSlowInEasing
    return if (context.prefersReducedMotion()) {
        fadeOut(tween(duration, easing = easing))
    } else {
        fadeOut(tween(duration, easing = easing)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(duration, easing = easing),
            )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashDetailPopEnter(
    context: Context,
): EnterTransition {
    val duration = BuddyDashMotion.NAV_DETAIL_MS
    val easing = FastOutSlowInEasing
    return if (context.prefersReducedMotion()) {
        fadeIn(tween(duration, easing = easing))
    } else {
        fadeIn(tween(duration, easing = easing)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(duration, easing = easing),
            )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.buddyDashDetailPopExit(
    context: Context,
): ExitTransition {
    val duration = BuddyDashMotion.NAV_DETAIL_MS
    val easing = FastOutSlowInEasing
    return if (context.prefersReducedMotion()) {
        fadeOut(tween(duration, easing = easing))
    } else {
        fadeOut(tween(duration, easing = easing)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(duration, easing = easing),
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

/** Printer detail tab content cross-fade. */
fun buddyDashTabContentTransform(reducedMotion: Boolean): ContentTransform {
    val duration = BuddyDashMotion.NAV_TAB_MS
    val easing = FastOutSlowInEasing
    return if (reducedMotion) {
        fadeIn(tween(duration, easing = easing)) togetherWith
            fadeOut(tween(duration, easing = easing))
    } else {
        (fadeIn(tween(duration, easing = easing)) +
            slideInVertically(
                animationSpec = tween(duration, easing = easing),
                initialOffsetY = { height -> height / 18 },
            )) togetherWith
            (fadeOut(tween(duration, easing = easing)) +
                slideOutVertically(
                    animationSpec = tween(duration, easing = easing),
                    targetOffsetY = { height -> -height / 22 },
                ))
    }
}
