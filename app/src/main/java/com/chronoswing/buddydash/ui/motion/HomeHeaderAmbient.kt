package com.chronoswing.buddydash.ui.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.ui.theme.CyanAccentDim
import com.chronoswing.buddydash.ui.theme.Slate800
import com.chronoswing.buddydash.ui.theme.Slate950
import com.chronoswing.buddydash.util.HomeHeaderVisualTuning

// Reduced from 0.036 — adds depth without looking speckled.
private const val HEADER_TEXTURE_DOT_ALPHA = 0.020f
// Was 0.22 — lerp(Slate900,Slate800,0.22) ≈ #1C2536, barely visible.
// 0.48 gives #1E283D — clearly richer without crossing into Slate800 territory.
private const val HEADER_GRADIENT_TOP_LIFT = 0.48f
// Was 0.75 — lerp(Slate900,Slate950,0.75) ≈ #14202C, too dark vs surface #1A2332.
// 0.20 gives #18212F — subtly darker, grounds cards without a hard border.
private const val HEADER_GRADIENT_DEPTH = 0.20f
// Slightly reduced: logo glow is now the primary illumination source.
private const val HEADER_LOGO_WASH_ALPHA_1X = 0.034f

/**
 * Static header ambience: base → gradient → subtle logo wash → texture.
 * No idle/print glow — see [HomeLogoGlowLayer].
 */
@Composable
fun HomeHeaderBackground(
    modifier: Modifier = Modifier,
    ambientMultiplier: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val topLift = HomeHeaderVisualTuning.effectiveGradientTopLift(HEADER_GRADIENT_TOP_LIFT, ambientMultiplier)
    val depth = HomeHeaderVisualTuning.effectiveGradientDepth(HEADER_GRADIENT_DEPTH, ambientMultiplier)
    val washAlpha = HomeHeaderVisualTuning.effectiveHeaderWashAlpha(HEADER_LOGO_WASH_ALPHA_1X, ambientMultiplier)
    val topRich = lerp(surface, Slate800, topLift)
    val depthBase = lerp(surface, Slate950, depth)
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .background(surface),
        )
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    // S-curve rolloff: hold the rich navy briefly at top, smooth fade to
                    // surface, then hold surface so the header/content seam is invisible.
                    // The very bottom has a barely-perceptible darkening that grounds the cards.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to topRich,
                                0.20f to lerp(topRich, surface, 0.40f),
                                0.58f to surface,
                                0.82f to surface,
                                1f to depthBase,
                            ),
                            startY = 0f,
                            endY = size.height,
                        ),
                    )
                    val washCenter = Offset(size.width * 0.10f, size.height * 0.44f)
                    val washRadius = size.maxDimension * 0.72f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to CyanAccentDim.copy(alpha = washAlpha),
                                0.35f to CyanAccentDim.copy(alpha = washAlpha * 0.35f),
                                0.70f to CyanAccentDim.copy(alpha = washAlpha * 0.08f),
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeaderDotTexture(alpha: Float) {
    // 14 dp spacing: slightly sparser than before so the lower alpha still reads as texture
    // rather than disappearing entirely.
    val spacing = 14.dp.toPx()
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
