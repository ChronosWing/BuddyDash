package com.chronoswing.buddydash.util

/**
 * Header ambient only (gradient + wash). Logo glow uses [HomeLogoGlowTuning].
 *
 * Caps are set so the full 1×→3× multiplier range is always linear and visible:
 * no stop is hit until above 3× (4.5× for topLift, 4.5× for wash).
 */
object HomeHeaderVisualTuning {
    fun effectiveHeaderWashAlpha(base: Float, multiplier: Float): Float =
        (base * multiplier).coerceIn(0f, 0.20f)

    fun effectiveGradientTopLift(base: Float, multiplier: Float): Float =
        (base * multiplier).coerceIn(0f, 0.55f)
}
