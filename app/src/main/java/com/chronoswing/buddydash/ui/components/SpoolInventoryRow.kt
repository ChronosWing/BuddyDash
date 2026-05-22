package com.chronoswing.buddydash.ui.components

import com.chronoswing.buddydash.ui.motion.FadeValueText
import com.chronoswing.buddydash.ui.motion.buddyDashClickable
import com.chronoswing.buddydash.ui.motion.rememberAttentionPulse
import com.chronoswing.buddydash.ui.motion.rememberPrefersReducedMotion
import com.chronoswing.buddydash.ui.theme.CyanAccent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.SpoolInventoryItem
import com.chronoswing.buddydash.util.SpoolInventoryCardUsage
import com.chronoswing.buddydash.util.formatSpoolCardTitle
import com.chronoswing.buddydash.util.formatSpoolInventoryCardLocationLine
import com.chronoswing.buddydash.util.formatSpoolMaterialSubtitle

private val SpoolCardCorner = 12.dp

@Composable
fun SpoolInventoryRow(
    spool: SpoolInventoryItem,
    cardUsage: SpoolInventoryCardUsage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowAnimationEnabled: Boolean = true,
) {
    SpoolPrintingGlowFrame(
        enabled = cardUsage == SpoolInventoryCardUsage.Printing,
        animateGlow = glowAnimationEnabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .buddyDashClickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            SpoolInventoryCardContent(
                spool = spool,
                locationLine = formatSpoolInventoryCardLocationLine(spool),
                cardUsage = cardUsage,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
fun SpoolPrintingGlowFrame(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    animateGlow: Boolean = true,
    content: @Composable () -> Unit,
) {
    val pulse = rememberAttentionPulse(
        enabled = enabled && animateGlow && !rememberPrefersReducedMotion(),
        periodMillis = 3_400,
    )
    Box(modifier = modifier) {
        if (enabled) {
            val glowAlpha = 0.045f + pulse * 0.055f
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRoundRect(
                            color = CyanAccent.copy(alpha = glowAlpha),
                            size = size,
                            cornerRadius = CornerRadius(SpoolCardCorner.toPx()),
                        )
                    },
            )
        }
        content()
    }
}

/** Shared title, badges, material, location, and remaining bar for inventory and picker rows. */
@Composable
fun SpoolInventoryCardContent(
    spool: SpoolInventoryItem,
    locationLine: String,
    cardUsage: SpoolInventoryCardUsage,
    modifier: Modifier = Modifier,
    locationMaxLines: Int = 1,
) {
    val materialLine = formatSpoolMaterialSubtitle(spool)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilamentColorSwatch(
            colorHexes = spool.swatch.colorHexes,
            isTranslucent = spool.swatch.isTranslucent,
            alpha = spool.swatch.alpha,
            size = 40.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSpoolCardTitle(spool),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                when (cardUsage) {
                    SpoolInventoryCardUsage.Printing -> SpoolPrintingBadge()
                    SpoolInventoryCardUsage.InUse -> SpoolInUseBadge()
                    SpoolInventoryCardUsage.Normal -> Unit
                }
                if (spool.isLowStock) {
                    LowSpoolChip()
                }
            }
            materialLine?.let { material ->
                Text(
                    text = material,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = locationLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                maxLines = locationMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            spool.remainPercent?.let { percent ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilamentRemainingBar(
                        remainPercent = percent,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp),
                        height = 4.dp,
                        barWidth = null,
                    )
                    FadeValueText(
                        text = stringResource(R.string.spool_remain_percent, percent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier.width(36.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SpoolPrintingBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = CyanAccent.copy(alpha = 0.2f),
    ) {
        Text(
            text = stringResource(R.string.spool_printing),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = CyanAccent.copy(alpha = 0.95f),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SpoolInUseBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
    ) {
        Text(
            text = stringResource(R.string.spool_in_use),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun LowSpoolChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
    ) {
        Text(
            text = stringResource(R.string.spool_low_stock),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
