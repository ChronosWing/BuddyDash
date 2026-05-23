package com.chronoswing.buddydash.util

/**
 * Header ambient only (gradient + wash). Logo glow uses [HomeLogoGlowTuning].
 *
 * 1× = preferred design target. 0.5× = more subtle. 2×/3× = visibly stronger debug states.
 * Caps are above the 3× value so the full debug range is always linear and unclipped.
 */
object HomeHeaderVisualTuning {
    // base=0.088; 0.5×=0.044  1×=0.088  2×=0.176  3×=0.264 — cap at 0.30
    fun effectiveHeaderWashAlpha(base: Float, multiplier: Float): Float =
        (base * multiplier).coerceIn(0f, 0.30f)

    // base=0.22; 0.5×=0.11  1×=0.22  2×=0.44  3×=0.66 — cap at 0.72
    fun effectiveGradientTopLift(base: Float, multiplier: Float): Float =
        (base * multiplier).coerceIn(0f, 0.72f)
}
