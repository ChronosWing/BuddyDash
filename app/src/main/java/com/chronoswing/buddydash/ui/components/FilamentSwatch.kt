package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.util.parseFilamentColor

@Composable
fun EmptySlotSwatch(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val shape = RoundedCornerShape(10.dp)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(1.dp, outline, shape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = outline,
                start = Offset(0f, this.size.height),
                end = Offset(this.size.width, 0f),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}

@Composable
fun FilamentColorSwatch(
    slot: FilamentSlot,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    if (!slot.isLoaded) {
        EmptySlotSwatch(modifier = modifier, size = size)
        return
    }
    FilamentColorSwatch(
        colorHexes = slot.swatchColorHexes,
        isTranslucent = slot.isTranslucent,
        alpha = slot.colorAlpha,
        modifier = modifier,
        size = size,
    )
}

@Composable
fun FilamentColorSwatch(
    colorHexes: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isTranslucent: Boolean = false,
    alpha: Float = 1f,
) {
    val shape = RoundedCornerShape(10.dp)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val colors = colorHexes.mapNotNull { parseFilamentColor(it) }
    val fillAlpha = if (isTranslucent) alpha.coerceIn(0.25f, 0.9f) else 1f

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(1.dp, outline, shape),
    ) {
        if (isTranslucent || colors.isEmpty()) {
            SwatchCheckerboard(modifier = Modifier.fillMaxSize())
        }
        when (colors.size) {
            0 -> Unit
            1 -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors[0].copy(alpha = fillAlpha)),
            )
            2 -> DualColorSwatchFill(
                primary = colors[0],
                secondary = colors[1],
                alpha = fillAlpha,
                modifier = Modifier.fillMaxSize(),
            )
            else -> MultiColorPieSwatchFill(
                colors = colors,
                alpha = fillAlpha,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SwatchCheckerboard(modifier: Modifier = Modifier) {
    val light = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    val dark = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)

    Canvas(modifier = modifier) {
        val tile = 5.dp.toPx()
        val cols = (size.width / tile).toInt() + 1
        val rows = (size.height / tile).toInt() + 1
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                drawRect(
                    color = if ((row + col) % 2 == 0) light else dark,
                    topLeft = Offset(col * tile, row * tile),
                    size = Size(tile, tile),
                )
            }
        }
    }
}

/** Two-color filament: diagonal split. */
@Composable
private fun DualColorSwatchFill(
    primary: Color,
    secondary: Color,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val a = primary.copy(alpha = alpha)
        val b = secondary.copy(alpha = alpha)

        val lowerLeft = Path().apply {
            moveTo(0f, h)
            lineTo(0f, 0f)
            lineTo(w, h)
            close()
        }
        drawPath(lowerLeft, a)

        val upperRight = Path().apply {
            moveTo(w, 0f)
            lineTo(w, h)
            lineTo(0f, 0f)
            close()
        }
        drawPath(upperRight, b)
    }
}

/** Three or more colors: equal pie segments from center (tri-color, multicolor). */
@Composable
private fun MultiColorPieSwatchFill(
    colors: List<Color>,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val count = colors.size
        val sweep = 360f / count
        val paints = colors.map { it.copy(alpha = alpha) }
        paints.forEachIndexed { index, paint ->
            drawArc(
                color = paint,
                startAngle = -90f + sweep * index,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                style = Fill,
            )
        }
    }
}
