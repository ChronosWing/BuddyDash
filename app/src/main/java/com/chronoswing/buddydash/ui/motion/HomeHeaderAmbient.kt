package com.chronoswing.buddydash.ui.motion

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
import com.chronoswing.buddydash.ui.theme.Slate950

private const val HEADER_TEXTURE_DOT_ALPHA = 0.016f
private const val HEADER_TEAL_WASH_ALPHA = 0.035f

/**
 * Faint navy/teal depth for the Home header — gradient plus barely-there dot texture.
 */
@Composable
fun Modifier.homeHeaderAmbientBackground(): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val depthBase = lerp(surface, Slate950, 0.38f)
    return drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    surface,
                    depthBase,
                ),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    CyanAccentDim.copy(alpha = HEADER_TEAL_WASH_ALPHA),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.12f, size.height * 0.08f),
                radius = size.maxDimension * 0.95f,
            ),
        )
        drawHeaderDotTexture(alpha = HEADER_TEXTURE_DOT_ALPHA)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeaderDotTexture(alpha: Float) {
    val spacing = 13.dp.toPx()
    val dotColor = Color.White.copy(alpha = alpha)
    var y = spacing * 0.5f
    while (y < size.height) {
        var x = spacing * 0.5f
        while (x < size.width) {
            drawCircle(color = dotColor, radius = 0.55f, center = Offset(x, y))
            x += spacing
        }
        y += spacing
    }
}
