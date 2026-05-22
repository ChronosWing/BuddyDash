package com.chronoswing.buddydash.ui.motion

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/** Crossfades text when [text] changes — fixed layout, no size animation. */
@Composable
fun FadeValueText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val duration = if (rememberPrefersReducedMotion()) 0 else BuddyDashMotion.VALUE_FADE_MS
    if (duration <= 0) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
        )
    } else {
        Crossfade(
            targetState = text,
            animationSpec = tween(duration),
            label = "fadeValueText",
            modifier = modifier,
        ) { value ->
            Text(
                text = value,
                style = style,
                color = color,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
            )
        }
    }
}
