package com.chronoswing.buddydash.ui.motion

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.chronoswing.buddydash.ui.theme.CyanAccent
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.HomeLogoGlowTuning

private const val TAG_LOGO_GLOW = "BuddyDash/LogoGlow"

/** Logo glow state — printing fully replaces idle (never blended). */
enum class HomeLogoGlowState {
    Idle,
    Printing,
}

private const val PRINT_PULSE_PERIOD_MS = 4_500

private const val GLOW_ELLIPSE_SCALE_X = 0.72f
private const val GLOW_ELLIPSE_SCALE_Y = 0.82f

private val DebugBoundsFill = Color.Magenta.copy(alpha = 0.18f)
private val DebugBoundsStroke = Color.Magenta.copy(alpha = 0.72f)

/**
 * Draws localized logo illumination on [modifier] — typically [Modifier.matchParentSize]
 * over the full brand row. Must NOT use a logo-sized box; parent must not clip (see [graphicsLayer]).
 */
@Composable
fun HomeLogoGlowLayer(
    state: HomeLogoGlowState,
    logoImageSize: Dp,
    logoSlotWidth: Dp,
    textPullLeft: Dp,
    idleGlowMultiplier: Float,
    printGlowMultiplier: Float,
    showDebugBounds: Boolean,
    modifier: Modifier = Modifier,
) {
    val printPhase = rememberAttentionPulse(
        enabled = state == HomeLogoGlowState.Printing,
        periodMillis = PRINT_PULSE_PERIOD_MS,
    )
    val density = LocalDensity.current
    val logoBasePx = with(density) { logoImageSize.toPx() }
    val logoSlotWidthPx = with(density) { logoSlotWidth.toPx() }
    val textPullLeftPx = with(density) { textPullLeft.toPx() }

    LaunchedEffect(state, idleGlowMultiplier, printGlowMultiplier, showDebugBounds, printPhase) {
        if (!BuddyDashDebug.enabled || !showDebugBounds) return@LaunchedEffect
        val coreAlpha = when (state) {
            HomeLogoGlowState.Idle -> HomeLogoGlowTuning.idleCoreAlpha(idleGlowMultiplier)
            HomeLogoGlowState.Printing ->
                HomeLogoGlowTuning.printCoreAlpha(printPhase, printGlowMultiplier)
        }
        val featherRadius = when (state) {
            HomeLogoGlowState.Idle -> HomeLogoGlowTuning.idleFeatherRadiusPx(logoBasePx)
            HomeLogoGlowState.Printing ->
                HomeLogoGlowTuning.printFeatherRadiusPx(logoBasePx, printPhase)
        }
        Log.d(
            TAG_LOGO_GLOW,
            "state=$state idleMult=$idleGlowMultiplier printMult=$printGlowMultiplier " +
                "phase=$printPhase coreAlpha=$coreAlpha featherRadiusPx=$featherRadius " +
                "logoBasePx=$logoBasePx slotPx=$logoSlotWidthPx",
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer { clip = false }
            .drawBehind {
                val center = brandGlowCenter(
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    logoSlotWidthPx = logoSlotWidthPx,
                    logoBasePx = logoBasePx,
                    textPullLeftPx = textPullLeftPx,
                )
                when (state) {
                    HomeLogoGlowState.Idle -> drawIdleLogoGlow(
                        center = center,
                        logoBasePx = logoBasePx,
                        idleGlowMultiplier = idleGlowMultiplier,
                    )
                    HomeLogoGlowState.Printing -> drawPrintingLogoGlow(
                        center = center,
                        logoBasePx = logoBasePx,
                        printGlowMultiplier = printGlowMultiplier,
                        phase = printPhase,
                    )
                }
                if (showDebugBounds) {
                    drawLogoGlowDebugOverlay(
                        center = center,
                        logoBasePx = logoBasePx,
                        state = state,
                        idleGlowMultiplier = idleGlowMultiplier,
                        printGlowMultiplier = printGlowMultiplier,
                        phase = printPhase,
                    )
                }
            },
    )
}

/** Logo icon center, nudged toward wordmark start. Coordinates in brand-row space. */
private fun brandGlowCenter(
    canvasWidth: Float,
    canvasHeight: Float,
    logoSlotWidthPx: Float,
    logoBasePx: Float,
    textPullLeftPx: Float,
): Offset {
    val logoCenterX = logoSlotWidthPx - logoBasePx * 0.5f
    val centerX = (logoCenterX + textPullLeftPx * 0.42f).coerceIn(0f, canvasWidth)
    return Offset(centerX, canvasHeight * 0.5f)
}

private fun DrawScope.drawLogoGlowDebugOverlay(
    center: Offset,
    logoBasePx: Float,
    state: HomeLogoGlowState,
    idleGlowMultiplier: Float,
    printGlowMultiplier: Float,
    phase: Float,
) {
    drawRect(color = DebugBoundsFill, size = size)
    drawRect(color = DebugBoundsStroke, size = size, style = Stroke(width = 2f))
    val featherRadius = when (state) {
        HomeLogoGlowState.Idle -> HomeLogoGlowTuning.idleFeatherRadiusPx(logoBasePx)
        HomeLogoGlowState.Printing -> HomeLogoGlowTuning.printFeatherRadiusPx(logoBasePx, phase)
    }
    drawCircle(
        color = DebugBoundsStroke,
        radius = featherRadius * GLOW_ELLIPSE_SCALE_X,
        center = center,
        style = Stroke(width = 2.5f),
    )
    drawCircle(color = Color.Red.copy(alpha = 0.85f), radius = 6f, center = center)
}

private fun DrawScope.drawIdleLogoGlow(
    center: Offset,
    logoBasePx: Float,
    idleGlowMultiplier: Float,
) {
    val coreAlpha = HomeLogoGlowTuning.idleCoreAlpha(idleGlowMultiplier).coerceIn(0f, 1f)
    val featherAlpha = HomeLogoGlowTuning.idleFeatherAlpha(idleGlowMultiplier).coerceIn(0f, 1f)
    val coreRadius = HomeLogoGlowTuning.idleCoreRadiusPx(logoBasePx)
    val featherRadius = HomeLogoGlowTuning.idleFeatherRadiusPx(logoBasePx)
    drawLocalizedGlow(
        center = center,
        tint = CyanAccent,
        coreAlpha = coreAlpha,
        featherAlpha = featherAlpha,
        coreRadius = coreRadius,
        featherRadius = featherRadius,
        hotCore = false,
    )
}

private fun DrawScope.drawPrintingLogoGlow(
    center: Offset,
    logoBasePx: Float,
    printGlowMultiplier: Float,
    phase: Float,
) {
    val coreAlpha = HomeLogoGlowTuning.printCoreAlpha(phase, printGlowMultiplier).coerceIn(0f, 1f)
    val featherAlpha = HomeLogoGlowTuning.printFeatherAlpha(phase, printGlowMultiplier).coerceIn(0f, 1f)
    val coreRadius = HomeLogoGlowTuning.printCoreRadiusPx(logoBasePx, phase)
    val featherRadius = HomeLogoGlowTuning.printFeatherRadiusPx(logoBasePx, phase)
    drawLocalizedGlow(
        center = center,
        tint = CyanAccent,
        coreAlpha = coreAlpha,
        featherAlpha = featherAlpha,
        coreRadius = coreRadius,
        featherRadius = featherRadius,
        hotCore = true,
    )
}

private fun DrawScope.drawLocalizedGlow(
    center: Offset,
    tint: Color,
    coreAlpha: Float,
    featherAlpha: Float,
    coreRadius: Float,
    featherRadius: Float,
    hotCore: Boolean,
) {
    if (coreAlpha <= 0f && featherAlpha <= 0f) return

    scale(scaleX = GLOW_ELLIPSE_SCALE_X, scaleY = GLOW_ELLIPSE_SCALE_Y, pivot = center) {
        if (featherAlpha > 0f) {
            // Gentle falloff: keeps ~50% brightness at 40% radius so the halo is visible
            // at the logo edge (which sits ~35-40% into the feather radius).
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to tint.copy(alpha = featherAlpha),
                        0.25f to tint.copy(alpha = featherAlpha * 0.72f),
                        0.50f to tint.copy(alpha = featherAlpha * 0.38f),
                        0.75f to tint.copy(alpha = featherAlpha * 0.10f),
                        1f to Color.Transparent,
                    ),
                    center = center,
                    radius = featherRadius,
                ),
                radius = featherRadius,
                center = center,
            )
        }
        if (coreAlpha > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to tint.copy(alpha = coreAlpha),
                        0.30f to tint.copy(alpha = coreAlpha * 0.72f),
                        0.60f to tint.copy(alpha = coreAlpha * 0.22f),
                        1f to Color.Transparent,
                    ),
                    center = center,
                    radius = coreRadius,
                ),
                radius = coreRadius,
                center = center,
            )
            if (hotCore) {
                val hotRadius = coreRadius * 0.45f
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = coreAlpha * 0.50f),
                            0.40f to tint.copy(alpha = coreAlpha * 0.45f),
                            1f to Color.Transparent,
                        ),
                        center = center,
                        radius = hotRadius,
                    ),
                    radius = hotRadius,
                    center = center,
                )
            }
        }
    }
}
