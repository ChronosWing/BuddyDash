package com.chronoswing.buddydash.ui.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.ui.theme.CyanAccentDim
import com.chronoswing.buddydash.ui.theme.Slate800
import com.chronoswing.buddydash.util.HomeHeaderVisualTuning

private const val HEADER_TEXTURE_DOT_ALPHA = 0.036f

// 1× IS the design target — the preferred richer blended look at default multiplier.
// topLift = 0.22 → topRich = lerp(Slate900, Slate800, 0.22) ≈ #1C2636
private const val HEADER_GRADIENT_TOP_LIFT = 0.22f
// Large-radius wash is the primary ambient integration effect — keeps the header
// from reading as a gray rectangle. Radius 1.28× maxDimension covers the full header.
private const val HEADER_LOGO_WASH_CENTER_ALPHA = 0.088f

// Height of the atmospheric fade drawn in the Scaffold content area (behind pills/cards).
// Long enough to read as atmosphere rather than a visible gradient band.
internal const val HEADER_ATMOSPHERE_DP = 200f

/**
 * Static header ambience: base → gradient → subtle logo wash → texture.
 * No idle/print glow — see [HomeLogoGlowLayer].
 *
 * The draw box uses [graphicsLayer] with clip disabled so the wash circle can
 * extend naturally below the header boundary. The atmospheric surface fade is
 * drawn separately via [HomeAtmosphericFade] in the Scaffold content area
 * (behind pills and cards) to maintain the correct draw order.
 */
@Composable
fun HomeHeaderBackground(
    modifier: Modifier = Modifier,
    ambientMultiplier: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val topLift = HomeHeaderVisualTuning.effectiveGradientTopLift(HEADER_GRADIENT_TOP_LIFT, ambientMultiplier)
    val washAlpha = HomeHeaderVisualTuning.effectiveHeaderWashAlpha(HEADER_LOGO_WASH_CENTER_ALPHA, ambientMultiplier)
    val topRich = lerp(surface, Slate800, topLift)
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .background(surface),
        )
        Box(
            Modifier
                .matchParentSize()
                // clip=false lets the wash circle extend below the header boundary,
                // carrying the teal ambient tint into the content area with a soft falloff.
                .graphicsLayer { clip = false }
                .drawBehind {
                    // Top-down fade: richer navy at top, decays continuously to surface
                    // at the very bottom edge — no flat plateau, no tonal shelf.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to topRich,
                                0.28f to lerp(topRich, surface, 0.28f),
                                0.62f to lerp(topRich, surface, 0.68f),
                                1f to surface,
                            ),
                            startY = 0f,
                            endY = size.height,
                        ),
                    )

                    // Wash circle: large radius covers the full header with teal tint.
                    // With clip=false it extends naturally below the boundary.
                    val washCenter = Offset(size.width * 0.11f, size.height * 0.46f)
                    val washRadius = size.maxDimension * 1.28f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to CyanAccentDim.copy(alpha = washAlpha),
                                0.38f to CyanAccentDim.copy(alpha = washAlpha * 0.42f),
                                0.72f to CyanAccentDim.copy(alpha = washAlpha * 0.12f),
                                1f to Color.Transparent,
                            ),
                            center = washCenter,
                            radius = washRadius,
                        ),
                        radius = washRadius,
                        center = washCenter,
                    )

                    drawHeaderDotTexture(alpha = HEADER_TEXTURE_DOT_ALPHA)
                },
        )
        content()
    }
}

/**
 * Atmospheric fade placed at the TOP of the Scaffold content area, behind all content.
 * Dissolves the header surface colour (Slate900) into the Slate950 body background
 * over [HEADER_ATMOSPHERE_DP] dp.
 *
 * Must be placed as the FIRST child of the Scaffold content Box so it draws behind
 * pills and cards. Non-interactive — no pointer input is consumed.
 *
 * Position via [modifier]: use `Modifier.padding(top = innerPadding.calculateTopPadding())`
 * so the fade starts at the content-area top (right below the header).
 */
@Composable
fun HomeAtmosphericFade(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HEADER_ATMOSPHERE_DP.dp)
            .drawBehind {
                // Five stops spaced to avoid visible banding within the 200 dp range.
                // surface.copy(alpha) over surface card backgrounds = no visible change;
                // only the raw Slate950 body gaps are tinted — the mist dissolve effect.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            surface.copy(alpha = 0.90f),
                            surface.copy(alpha = 0.65f),
                            surface.copy(alpha = 0.32f),
                            surface.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height,
                    ),
                )
            },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeaderDotTexture(alpha: Float) {
    val spacing = 12.dp.toPx()
    val dotColor = Color.White.copy(alpha = alpha)
    var y = spacing * 0.5f
    while (y < size.height) {
        var x = spacing * 0.5f
        while (x < size.width) {
            drawCircle(color = dotColor, radius = 0.65f, center = Offset(x, y))
            x += spacing
        }
        y += spacing
    }
}
