package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fill color for remaining-filament progress by percentage tier (Home, Spools, Detail). */
fun remainingProgressColor(percent: Int): Color {
    val green = Color(0xFF5CB85C)
    val amber = Color(0xFFFFB74D)
    val orange = Color(0xFFFF8A50)
    val red = Color(0xFFEF5350)
    val p = percent.coerceIn(0, 100)
    return when {
        p >= 70 -> green
        p >= 30 -> lerp(amber, green, (p - 30) / 40f)
        p >= 10 -> lerp(orange, amber, (p - 10) / 20f)
        else -> lerp(red, orange, p / 10f)
    }
}

/**
 * Thin horizontal bar for filament remaining %.
 * @param barWidth fixed width when set; omit (null) to use parent width (e.g. [Modifier.weight]).
 */
@Composable
fun FilamentRemainingBar(
    remainPercent: Int,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    barWidth: Dp? = 44.dp,
) {
    val fraction = remainPercent.coerceIn(0, 100) / 100f
    val fillColor = remainingProgressColor(remainPercent)
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val shape = RoundedCornerShape(height / 2)

    Box(
        modifier = modifier
            .then(if (barWidth != null) Modifier.width(barWidth) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(shape)
                .background(fillColor),
        )
    }
}
