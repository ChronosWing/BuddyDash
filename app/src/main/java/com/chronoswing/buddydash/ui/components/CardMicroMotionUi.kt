package com.chronoswing.buddydash.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.ui.theme.CyanAccent
import com.chronoswing.buddydash.ui.theme.OnlineGreen
import com.chronoswing.buddydash.util.CardMicroMotion

private const val COMPLETED_GLOW_MS = 2_600

/** Optional aura behind the home card (idle ambient + finish flash). */
@Composable
fun HomeCardMicroMotionFrame(
    motion: CardMicroMotion,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val finishGlow = remember { Animatable(0f) }

    LaunchedEffect(motion) {
        if (motion == CardMicroMotion.CompletedFlash) {
            finishGlow.snapTo(0f)
            finishGlow.animateTo(0.42f, tween(700, easing = FastOutSlowInEasing))
            finishGlow.animateTo(0f, tween(COMPLETED_GLOW_MS - 700, easing = FastOutSlowInEasing))
        } else {
            finishGlow.snapTo(0f)
        }
    }

    val idleBreath = rememberIdleBreathPhase(enabled = motion == CardMicroMotion.IdleAmbient)

    Box(modifier = modifier) {
        if (motion == CardMicroMotion.IdleAmbient) {
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val alpha = 0.018f + idleBreath * 0.014f
                        drawRoundRect(
                            color = scheme.onSurface.copy(alpha = alpha),
                            size = size,
                            cornerRadius = CornerRadius(12.dp.toPx()),
                        )
                    },
            )
        }
        if (finishGlow.value > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val alpha = finishGlow.value * 0.22f
                        drawRoundRect(
                            color = OnlineGreen.copy(alpha = alpha),
                            size = size,
                            cornerRadius = CornerRadius(12.dp.toPx()),
                        )
                    },
            )
        }
        content()
    }
}

@Composable
fun MicroMotionProgressBar(
    progress: () -> Float,
    motion: CardMicroMotion,
    modifier: Modifier = Modifier,
) {
    val trackShape = RoundedCornerShape(2.dp)
    val sheenEnabled = motion == CardMicroMotion.Printing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape),
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        )
        if (sheenEnabled) {
            val transition = rememberInfiniteTransition(label = "progressSheen")
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 4_200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "progressSheenPhase",
            )
            val accent = CyanAccent
            Canvas(modifier = Modifier.matchParentSize()) {
                val band = size.width * 0.28f
                val travel = size.width + band
                val startX = phase * travel - band
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.35f to accent.copy(alpha = 0.07f),
                        0.5f to Color.White.copy(alpha = 0.11f),
                        0.65f to accent.copy(alpha = 0.06f),
                        1f to Color.Transparent,
                        startX = startX,
                        endX = startX + band,
                    ),
                    size = Size(band, size.height),
                    topLeft = Offset(startX, 0f),
                )
            }
        }
    }
}

@Composable
fun MicroMotionThumbnailFrame(
    motion: CardMicroMotion,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sheenEnabled = motion == CardMicroMotion.Printing
    val cornerPx = 10.dp

    Box(modifier = modifier) {
        if (sheenEnabled) {
            val transition = rememberInfiniteTransition(label = "thumbAmbient")
            val breath by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3_600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "thumbAmbientBreath",
            )
            val sheenPhase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(5_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "thumbSheenPhase",
            )
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val glowAlpha = 0.06f + breath * 0.05f
                        val spread = 4f + breath * 2f
                        drawRoundRect(
                            color = CyanAccent.copy(alpha = glowAlpha),
                            topLeft = Offset(-spread, -spread),
                            size = Size(size.width + spread * 2, size.height + spread * 2),
                            cornerRadius = CornerRadius(cornerPx.toPx() + spread),
                        )
                    },
            )
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(cornerPx)),
            ) {
                val band = size.width * 0.35f
                val travel = size.width + band
                val startX = sheenPhase * travel - band
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.06f),
                        1f to Color.Transparent,
                        startX = startX,
                        endX = startX + band,
                    ),
                    size = Size(band, size.height),
                    topLeft = Offset(startX, 0f),
                )
            }
        }
        content()
    }
}

@Composable
fun rememberPrintingChipBreath(enabled: Boolean): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "printingChipBreath")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "printingChipBreathPhase",
    )
    return breath
}

@Composable
private fun rememberIdleBreathPhase(enabled: Boolean): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "idleCardBreath")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idleCardBreathPhase",
    )
    return breath
}
