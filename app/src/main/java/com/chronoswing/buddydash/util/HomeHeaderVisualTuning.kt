package com.chronoswing.buddydash.util

/** Header ambient only (gradient + wash). Logo glow uses [HomeLogoGlowTuning]. */
object HomeHeaderVisualTuning {
    fun effectiveHeaderWashAlpha(base: Float, multiplier: Float): Float =
        (base * multiplier).coerceIn(0f, 0.20f)

    fun effectiveGradientTopLift(base: Float, multiplier: Float): Float =
        (base * multiplier).coerceIn(0f, 0.45f)

    fun effectiveGradientDepth(base: Float, multiplier: Float): Float =
        (base * multiplier).coerceIn(0f, 0.92f)
}
