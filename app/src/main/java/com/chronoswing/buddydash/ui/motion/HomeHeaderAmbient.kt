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

private const val HEADER_TEXTURE_DOT_ALPHA = 0.036f
private const val HEADER_LOGO_WASH_CENTER_ALPHA = 0.088f
private const val HEADER_GRADIENT_TOP_LIFT = 0.22f
private const val HEADER_GRADIENT_DEPTH = 0.75f

/**
 * Home header stack: solid base → ambient depth → foreground chrome.
 * Ambient sits above the base fill and below logo, pills, and refresh affordances.
 */
@Composable
fun HomeHeaderBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val topRich = lerp(surface, Slate800, HEADER_GRADIENT_TOP_LIFT)
    val depthBase = lerp(surface, Slate950, HEADER_GRADIENT_DEPTH)
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
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to topRich,
                                0.42f to surface,
                                1f to depthBase,
                            ),
                            startY = 0f,
                            endY = size.height,
                        ),
                    )
                    val washCenter = Offset(size.width * 0.11f, size.height * 0.46f)
                    val washRadius = size.maxDimension * 1.28f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to CyanAccentDim.copy(alpha = HEADER_LOGO_WASH_CENTER_ALPHA),
                                0.38f to CyanAccentDim.copy(alpha = HEADER_LOGO_WASH_CENTER_ALPHA * 0.42f),
                                0.72f to CyanAccentDim.copy(alpha = HEADER_LOGO_WASH_CENTER_ALPHA * 0.12f),
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
