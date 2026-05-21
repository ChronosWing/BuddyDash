package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.util.FilamentSourceGroup
import com.chronoswing.buddydash.util.SlotInventoryKey
import com.chronoswing.buddydash.util.groupByFilamentSource
import com.chronoswing.buddydash.util.isActiveSlot
import com.chronoswing.buddydash.util.normalizeFilamentType

private enum class ActiveEmphasis { Home, Detail }

/** Compact grouped filament row for home printer cards. */
@Composable
fun FilamentHomeGroupsRow(
    slots: List<FilamentSlot>,
    activeKey: SlotInventoryKey?,
    modifier: Modifier = Modifier,
) {
    if (slots.isEmpty()) return
    val groups = slots.groupByFilamentSource()

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        groups.forEach { group ->
            FilamentHomeSourceSection(
                group = group,
                activeKey = activeKey,
            )
        }
    }
}

@Composable
private fun FilamentHomeSourceSection(
    group: FilamentSourceGroup,
    activeKey: SlotInventoryKey?,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = group.sourceLabel,
            style = MaterialTheme.typography.labelSmall,
            color = muted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 1.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            group.slots.forEach { slot ->
                FilamentSlotChip(
                    slot = slot,
                    activeKey = activeKey,
                    compact = true,
                    isExternal = group.isExternal,
                )
            }
        }
    }
}

/** Grouped filament layout for the detail Filament tab. */
@Composable
fun FilamentDetailGroups(
    slots: List<FilamentSlot>,
    activeKey: SlotInventoryKey?,
    modifier: Modifier = Modifier,
) {
    if (slots.isEmpty()) return
    val groups = slots.groupByFilamentSource()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        groups.forEach { group ->
            FilamentSourceGroup(
                group = group,
                activeKey = activeKey,
            )
        }
    }
}

@Composable
fun FilamentSourceGroup(
    group: FilamentSourceGroup,
    activeKey: SlotInventoryKey?,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = if (group.isExternal) "External" else group.sourceLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = muted,
        )
        group.slots.forEach { slot ->
            FilamentSlotCard(
                slot = slot,
                activeKey = activeKey,
                isExternal = group.isExternal,
            )
        }
    }
}

@Composable
fun FilamentSlotChip(
    slot: FilamentSlot,
    activeKey: SlotInventoryKey?,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    isExternal: Boolean = slot.isExternal,
) {
    if (slot.isLoaded) {
        LoadedFilamentSlotChip(
            slot = slot,
            isActive = slot.isActiveSlot(activeKey),
            compact = compact,
            isExternal = isExternal,
            modifier = modifier,
        )
    } else {
        EmptyFilamentSlotChip(
            label = slot.label,
            compact = compact,
            isExternal = isExternal,
            modifier = modifier,
        )
    }
}

@Composable
fun FilamentSlotCard(
    slot: FilamentSlot,
    activeKey: SlotInventoryKey?,
    modifier: Modifier = Modifier,
    isExternal: Boolean = slot.isExternal,
) {
    val isActive = slot.isActiveSlot(activeKey)
    val shape = RoundedCornerShape(12.dp)
    val bgAlpha = if (slot.isLoaded) 1f else 0.72f

    Row(
        modifier = modifier
            .alpha(bgAlpha)
            .fillMaxWidth()
            .filamentChipSurface(
                shape = shape,
                isActive = isActive,
                isExternal = isExternal,
                emphasis = ActiveEmphasis.Detail,
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilamentColorSwatch(slot = slot, size = 40.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = slot.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isActive) {
                    ActiveFilamentDot(size = 7.dp)
                }
            }
            if (slot.isLoaded) {
                normalizeFilamentType(slot.filamentType)?.uppercase()?.let { type ->
                    Text(
                        text = type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            slot.remainPercent?.takeIf { slot.isLoaded }?.let { remain ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { remain / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Text(
                        text = "$remain%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
            }
            slot.metadata?.takeIf { slot.isLoaded }?.let { meta ->
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun LoadedFilamentSlotChip(
    slot: FilamentSlot,
    isActive: Boolean,
    compact: Boolean,
    isExternal: Boolean,
    modifier: Modifier = Modifier,
) {
    val typeText = normalizeFilamentType(slot.filamentType)?.uppercase()
    val shape = RoundedCornerShape(7.dp)
    val swatchSize = if (compact) 11.dp else 14.dp
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .filamentChipSurface(
                shape = shape,
                isActive = isActive,
                isExternal = isExternal,
                emphasis = if (compact) ActiveEmphasis.Home else ActiveEmphasis.Detail,
            )
            .padding(horizontal = 5.dp, vertical = if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        FilamentColorSwatch(slot = slot, size = swatchSize)
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = slot.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                if (typeText != null) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = muted.copy(alpha = 0.55f),
                    )
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
                if (isActive) {
                    Spacer(modifier = Modifier.width(2.dp))
                    ActiveFilamentDot(size = if (compact) 4.dp else 5.dp)
                }
            }
            if (compact) {
                slot.remainPercent?.let { remain ->
                    Text(
                        text = "$remain%",
                        style = MaterialTheme.typography.labelSmall,
                        color = muted.copy(alpha = 0.58f),
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFilamentSlotChip(
    label: String,
    compact: Boolean,
    isExternal: Boolean,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
    val shape = RoundedCornerShape(7.dp)
    val swatchSize = if (compact) 11.dp else 14.dp

    Row(
        modifier = modifier
            .alpha(0.72f)
            .filamentChipSurface(
                shape = shape,
                isActive = false,
                isExternal = isExternal,
                emphasis = if (compact) ActiveEmphasis.Home else ActiveEmphasis.Detail,
            )
            .padding(horizontal = 5.dp, vertical = if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        EmptySlotSwatch(size = swatchSize)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
    }
}

@Composable
private fun ActiveFilamentDot(size: androidx.compose.ui.unit.Dp) {
    Spacer(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)),
    )
}

@Composable
private fun Modifier.filamentChipSurface(
    shape: Shape,
    isActive: Boolean,
    isExternal: Boolean,
    emphasis: ActiveEmphasis,
): Modifier {
    val glowStrength = when (emphasis) {
        ActiveEmphasis.Home -> 0.85f
        ActiveEmphasis.Detail -> 1.25f
    }
    return this
        .then(
            if (isActive) {
                Modifier.subtleActiveGlow(strength = glowStrength)
            } else {
                Modifier
            },
        )
        .clip(shape)
        .background(chipBackgroundColor(isActive, isExternal, emphasis), shape)
        .border(chipBorderWidth(isActive), chipBorderColor(isActive, isExternal), shape)
}

@Composable
private fun Modifier.subtleActiveGlow(
    strength: Float,
): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    val corner = 7.dp
    return drawBehind {
        val layers = listOf(10f, 6f, 3f)
        layers.forEachIndexed { index, spread ->
            val alpha = (0.05f + index * 0.02f) * strength
            drawRoundRect(
                color = primary.copy(alpha = alpha.coerceAtMost(0.14f)),
                topLeft = Offset(-spread, -spread),
                size = Size(size.width + spread * 2, size.height + spread * 2),
                cornerRadius = CornerRadius(corner.toPx() + spread),
            )
        }
    }
}

@Composable
private fun chipBackgroundColor(
    isActive: Boolean,
    isExternal: Boolean,
    emphasis: ActiveEmphasis,
): androidx.compose.ui.graphics.Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        isActive && emphasis == ActiveEmphasis.Detail ->
            scheme.surfaceVariant.copy(alpha = 0.62f)
        isActive ->
            scheme.surfaceVariant.copy(alpha = 0.48f)
        isExternal ->
            scheme.surface.copy(alpha = 0.42f)
        else ->
            scheme.surface.copy(alpha = 0.88f)
    }
}

@Composable
private fun chipBorderColor(
    isActive: Boolean,
    isExternal: Boolean,
): androidx.compose.ui.graphics.Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        isActive -> scheme.primary.copy(alpha = 0.18f)
        isExternal -> scheme.outline.copy(alpha = 0.38f)
        else -> scheme.outline.copy(alpha = 0.22f)
    }
}

private fun chipBorderWidth(isActive: Boolean) = 1.dp
