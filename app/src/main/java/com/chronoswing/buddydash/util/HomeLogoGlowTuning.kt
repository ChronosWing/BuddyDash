package com.chronoswing.buddydash.util

/**
 * Debug-calibration values for [com.chronoswing.buddydash.ui.motion.HomeLogoGlowLayer] only.
 * Intentionally exaggerated to prove idle vs print hierarchy; tune down after validation.
 */
object HomeLogoGlowTuning {
    val multiplierPresets: List<Float> = listOf(0.5f, 1f, 2f, 3f)

    // --- Idle (static) — scaled only by idleGlowMultiplier ---
    private const val IDLE_CORE_ALPHA_1X = 0.22f
    // Feather must be high enough that the outer halo (which gradient dims heavily) is visible
    private const val IDLE_FEATHER_ALPHA_1X = 0.18f
    // Radii must exceed logoBasePx so glow escapes the logo image bounds and blooms around it.
    // Previous values (0.18 core / 0.32 feather) produced a disk fully contained within the
    // 84 dp logo slot, hidden behind opaque logo pixels — only a faint dot showed through the B.
    private const val IDLE_CORE_RADIUS_FRAC = 0.52f
    private const val IDLE_FEATHER_RADIUS_FRAC = 1.20f

    // --- Print (pulsing) — scaled only by printGlowMultiplier ---
    private const val PRINT_CORE_ALPHA_MIN_1X = 0.58f
    private const val PRINT_CORE_ALPHA_MAX_1X = 0.78f
    private const val PRINT_FEATHER_ALPHA_MIN_1X = 0.28f
    private const val PRINT_FEATHER_ALPHA_MAX_1X = 0.45f
    private const val PRINT_CORE_RADIUS_FRAC = 0.65f
    private const val PRINT_FEATHER_RADIUS_FRAC = 1.80f
    private const val PRINT_RADIUS_BREATHE_FRAC = 0.10f

    fun idleCoreAlpha(multiplier: Float): Float = IDLE_CORE_ALPHA_1X * multiplier

    fun idleFeatherAlpha(multiplier: Float): Float = IDLE_FEATHER_ALPHA_1X * multiplier

    fun idleCoreRadiusPx(logoBasePx: Float): Float = logoBasePx * IDLE_CORE_RADIUS_FRAC

    fun idleFeatherRadiusPx(logoBasePx: Float): Float = logoBasePx * IDLE_FEATHER_RADIUS_FRAC

    fun printCoreAlpha(phase: Float, multiplier: Float): Float {
        val min = PRINT_CORE_ALPHA_MIN_1X * multiplier
        val max = PRINT_CORE_ALPHA_MAX_1X * multiplier
        return min + phase * (max - min)
    }

    fun printFeatherAlpha(phase: Float, multiplier: Float): Float {
        val min = PRINT_FEATHER_ALPHA_MIN_1X * multiplier
        val max = PRINT_FEATHER_ALPHA_MAX_1X * multiplier
        return min + phase * (max - min)
    }

    fun printCoreRadiusPx(logoBasePx: Float, phase: Float): Float {
        val breathe = 1f + phase * PRINT_RADIUS_BREATHE_FRAC
        return logoBasePx * PRINT_CORE_RADIUS_FRAC * breathe
    }

    fun printFeatherRadiusPx(logoBasePx: Float, phase: Float): Float {
        val breathe = 1f + phase * PRINT_RADIUS_BREATHE_FRAC
        return logoBasePx * PRINT_FEATHER_RADIUS_FRAC * breathe
    }
}
