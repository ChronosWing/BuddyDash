package com.chronoswing.buddydash.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import com.chronoswing.buddydash.ui.motion.buddyDashClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.FilamentSlotDisplay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.ui.layout.BUDDYDASH_GRID_GUTTER_DP
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.ui.motion.rememberPrefersReducedMotion
import com.chronoswing.buddydash.util.CardMicroMotion
import com.chronoswing.buddydash.util.FilamentGlowMotion
import com.chronoswing.buddydash.util.FilamentSourceGroup
import com.chronoswing.buddydash.util.SlotInventoryKey
import com.chronoswing.buddydash.util.groupByFilamentSource
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.util.isActiveSlot
import com.chronoswing.buddydash.util.normalizeFilamentType
import com.chronoswing.buddydash.util.toFilamentGlowMotion

/** Set true locally to verify breathing; keep false for production subtlety. */
private const val DEBUG_FILAMENT_GLOW_EXAGGERATED = false

private enum class ActiveEmphasis { Home, Detail }

private data class ActiveFilamentBreath(
    val glowAlpha: Float,
    val backgroundLift: Float,
    val drawGlow: Boolean,
) {
    companion object {
        val Inactive = ActiveFilamentBreath(0f, 0f, drawGlow = false)
    }
}

private data class FilamentGlowConfig(
    val minGlow: Float,
    val maxGlow: Float,
    val minBgLift: Float,
    val maxBgLift: Float,
    val cycleMs: Int,
)

/** Compact grouped filament row for home printer cards. */
@Composable
fun FilamentHomeGroupsRow(
    slots: List<FilamentSlot>,
    activeKey: SlotInventoryKey?,
    cardMicroMotion: CardMicroMotion,
    modifier: Modifier = Modifier,
) {
    if (slots.isEmpty()) return
    val groups = slots.groupByFilamentSource()
    val glowMotion = remember(cardMicroMotion) { cardMicroMotion.toFilamentGlowMotion() }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        groups.forEach { group ->
            FilamentHomeSourceSection(
                group = group,
                activeKey = activeKey,
                glowMotion = glowMotion,
            )
        }
    }
}

@Composable
private fun FilamentHomeSourceSection(
    group: FilamentSourceGroup,
    activeKey: SlotInventoryKey?,
    glowMotion: FilamentGlowMotion,
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
                    glowMotion = glowMotion,
                )
            }
        }
    }
}

/** Grouped filament layout for the detail Filament tab. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilamentDetailGroups(
    slotDisplays: List<FilamentSlotDisplay>,
    cardMicroMotion: CardMicroMotion,
    onSlotClick: (FilamentSlotDisplay) -> Unit,
    modifier: Modifier = Modifier,
    gridColumns: Int = 1,
) {
    if (slotDisplays.isEmpty()) return
    val groups = slotDisplays.map { it.slot }.groupByFilamentSource()
    val displayBySlot = slotDisplays.associateBy { it.slot }
    val glowMotion = remember(cardMicroMotion) { cardMicroMotion.toFilamentGlowMotion() }
    val columns = gridColumns.coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        groups.forEach { group ->
            val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (group.isExternal) "External" else group.sourceLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = muted,
                )
                if (columns <= 1) {
                    group.slots.forEach { slot ->
                        val display = displayBySlot[slot] ?: return@forEach
                        FilamentDetailSlotCard(
                            display = display,
                            isExternal = group.isExternal,
                            glowMotion = glowMotion,
                            onSlotClick = onSlotClick,
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BUDDYDASH_GRID_GUTTER_DP.dp),
                        verticalArrangement = Arrangement.spacedBy(BUDDYDASH_GRID_GUTTER_DP.dp),
                        maxItemsInEachRow = columns,
                    ) {
                        group.slots.forEach { slot ->
                            val display = displayBySlot[slot] ?: return@forEach
                            FilamentDetailSlotCard(
                                modifier = Modifier.fillMaxWidth(1f / columns),
                                display = display,
                                isExternal = group.isExternal,
                                glowMotion = glowMotion,
                                onSlotClick = onSlotClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilamentSourceGroup(
    group: FilamentSourceGroup,
    activeKey: SlotInventoryKey?,
    glowMotion: FilamentGlowMotion,
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
                glowMotion = glowMotion,
            )
        }
    }
}

@Composable
fun FilamentSlotChip(
    slot: FilamentSlot,
    activeKey: SlotInventoryKey?,
    glowMotion: FilamentGlowMotion,
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
            glowMotion = glowMotion,
            modifier = modifier,
        )
    } else {
        EmptyFilamentSlotChip(
            label = slot.label,
            compact = compact,
            isExternal = isExternal,
            glowMotion = glowMotion,
            modifier = modifier,
        )
    }
}

@Composable
fun FilamentDetailSlotCard(
    display: FilamentSlotDisplay,
    glowMotion: FilamentGlowMotion,
    isExternal: Boolean,
    onSlotClick: (FilamentSlotDisplay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slot = display.slot
    val isActive = display.isActive
    val shape = RoundedCornerShape(12.dp)
    val bgAlpha = if (display.isEmpty) 0.72f else 1f
    val breath = rememberActiveFilamentBreath(isActive, glowMotion, ActiveEmphasis.Detail)
    val titleText = when {
        display.isEmpty -> stringResource(R.string.filament_empty)
        display.primaryTitle.isNotBlank() -> display.primaryTitle
        else -> stringResource(R.string.filament_unknown)
    }
    val mutedTitle = display.isEmpty

    val rowModifier = modifier
        .alpha(bgAlpha)
        .fillMaxWidth()
        .then(
            if (display.isTappable) {
                Modifier.buddyDashClickable { onSlotClick(display) }
            } else {
                Modifier
            },
        )

    FilamentChipSurface(
        modifier = rowModifier,
        shape = shape,
        isActive = isActive,
        isExternal = isExternal,
        emphasis = ActiveEmphasis.Detail,
        breath = breath,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (display.isEmpty) {
                    EmptySlotSwatch(size = 40.dp)
                } else {
                    FilamentColorSwatch(slot = slot, size = 40.dp)
                }
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
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        )
                        if (isActive) {
                            ActiveFilamentDot(
                                size = 7.dp,
                                glowAlpha = breath.glowAlpha,
                            )
                        }
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (mutedTitle) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                    )
                    display.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    slot.remainPercent?.takeIf { !display.isEmpty }?.let { remain ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilamentRemainingBar(
                                remainPercent = remain,
                                modifier = Modifier.weight(1f),
                                height = 4.dp,
                                barWidth = null,
                            )
                            Text(
                                text = "$remain%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
                if (display.isTappable && BambuddyApi.hasInventoryAssignEndpoint) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FilamentSlotCard(
    slot: FilamentSlot,
    activeKey: SlotInventoryKey?,
    glowMotion: FilamentGlowMotion,
    modifier: Modifier = Modifier,
    isExternal: Boolean = slot.isExternal,
) {
    val isActive = slot.isActiveSlot(activeKey)
    val shape = RoundedCornerShape(12.dp)
    val bgAlpha = if (slot.isLoaded) 1f else 0.72f
    val breath = rememberActiveFilamentBreath(isActive, glowMotion, ActiveEmphasis.Detail)

    FilamentChipSurface(
        modifier = modifier.alpha(bgAlpha).fillMaxWidth(),
        shape = shape,
        isActive = isActive,
        isExternal = isExternal,
        emphasis = ActiveEmphasis.Detail,
        breath = breath,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                        ActiveFilamentDot(
                            size = 7.dp,
                            glowAlpha = breath.glowAlpha,
                        )
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
                        FilamentRemainingBar(
                            remainPercent = remain,
                            modifier = Modifier.weight(1f),
                            height = 4.dp,
                            barWidth = null,
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
}

@Composable
private fun LoadedFilamentSlotChip(
    slot: FilamentSlot,
    isActive: Boolean,
    compact: Boolean,
    isExternal: Boolean,
    glowMotion: FilamentGlowMotion,
    modifier: Modifier = Modifier,
) {
    val typeText = normalizeFilamentType(slot.filamentType)?.uppercase()
    val shape = RoundedCornerShape(7.dp)
    val swatchSize = if (compact) 11.dp else 14.dp
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val emphasis = if (compact) ActiveEmphasis.Home else ActiveEmphasis.Detail
    val breath = rememberActiveFilamentBreath(isActive, glowMotion, emphasis)

    FilamentChipSurface(
        modifier = modifier,
        shape = shape,
        isActive = isActive,
        isExternal = isExternal,
        emphasis = emphasis,
        breath = breath,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = if (compact) 2.dp else 4.dp),
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
                        ActiveFilamentDot(
                            size = if (compact) 4.dp else 5.dp,
                            glowAlpha = breath.glowAlpha,
                        )
                    }
                }
                if (compact) {
                    slot.remainPercent?.takeIf { slot.isLoaded }?.let { remain ->
                        FilamentRemainingBar(
                            remainPercent = remain,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
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
    glowMotion: FilamentGlowMotion,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
    val shape = RoundedCornerShape(7.dp)
    val swatchSize = if (compact) 11.dp else 14.dp
    val emphasis = if (compact) ActiveEmphasis.Home else ActiveEmphasis.Detail

    FilamentChipSurface(
        modifier = modifier.alpha(0.72f),
        shape = shape,
        isActive = false,
        isExternal = isExternal,
        emphasis = emphasis,
        breath = ActiveFilamentBreath.Inactive,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = if (compact) 2.dp else 4.dp),
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
}

@Composable
private fun FilamentChipSurface(
    modifier: Modifier,
    shape: Shape,
    isActive: Boolean,
    isExternal: Boolean,
    emphasis: ActiveEmphasis,
    breath: ActiveFilamentBreath,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val background = chipBackgroundColor(
        scheme = scheme,
        isActive = isActive,
        isExternal = isExternal,
        emphasis = emphasis,
        breath = breath,
    )
    val borderColor = chipBorderColor(scheme, isActive, isExternal)

    Box(modifier = modifier) {
        if (isActive && breath.drawGlow) {
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawActiveFilamentGlow(
                            primary = scheme.primary,
                            glowAlpha = breath.glowAlpha,
                            cornerPx = 7.dp.toPx(),
                        )
                    },
            )
        }
        Box(
            Modifier
                .clip(shape)
                .background(background, shape)
                .border(1.dp, borderColor, shape),
        ) {
            content()
        }
    }
}

@Composable
private fun ActiveFilamentDot(size: Dp, glowAlpha: Float) {
    val scheme = MaterialTheme.colorScheme
    Spacer(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                scheme.primary.copy(alpha = (0.55f + glowAlpha * 0.8f).coerceIn(0.45f, 1f)),
            ),
    )
}

@Composable
private fun rememberActiveFilamentBreath(
    isActive: Boolean,
    motion: FilamentGlowMotion,
    emphasis: ActiveEmphasis,
): ActiveFilamentBreath {
    if (!isActive) return ActiveFilamentBreath.Inactive

    val config = filamentGlowConfig(emphasis, motion)

    return when (motion) {
        FilamentGlowMotion.None -> ActiveFilamentBreath(
            glowAlpha = config.minGlow,
            backgroundLift = config.minBgLift,
            drawGlow = true,
        )
        FilamentGlowMotion.Frozen -> ActiveFilamentBreath(
            glowAlpha = (config.minGlow + config.maxGlow) * 0.5f,
            backgroundLift = (config.minBgLift + config.maxBgLift) * 0.5f,
            drawGlow = true,
        )
        FilamentGlowMotion.SoftIdle, FilamentGlowMotion.Breathing -> {
            if (rememberPrefersReducedMotion()) {
                return ActiveFilamentBreath(
                    glowAlpha = (config.minGlow + config.maxGlow) * 0.5f,
                    backgroundLift = (config.minBgLift + config.maxBgLift) * 0.5f,
                    drawGlow = motion == FilamentGlowMotion.Breathing,
                )
            }
            val transition = rememberInfiniteTransition(label = "filamentActiveGlow")
            val breath by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = config.cycleMs, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "filamentActiveGlowBreath",
            )
            ActiveFilamentBreath(
                glowAlpha = config.minGlow + (config.maxGlow - config.minGlow) * breath,
                backgroundLift = config.minBgLift + (config.maxBgLift - config.minBgLift) * breath,
                drawGlow = true,
            )
        }
    }
}

private fun filamentGlowConfig(
    emphasis: ActiveEmphasis,
    motion: FilamentGlowMotion,
): FilamentGlowConfig {
    val boost = if (DEBUG_FILAMENT_GLOW_EXAGGERATED) 1.5f else 1f
    return when (emphasis) {
        ActiveEmphasis.Home -> when (motion) {
            FilamentGlowMotion.Breathing -> FilamentGlowConfig(
                minGlow = 0.15f * boost,
                maxGlow = 0.32f * boost,
                minBgLift = 0.3f,
                maxBgLift = 1f,
                cycleMs = 3200,
            )
            FilamentGlowMotion.SoftIdle -> FilamentGlowConfig(
                minGlow = 0.10f * boost,
                maxGlow = 0.22f * boost,
                minBgLift = 0.2f,
                maxBgLift = 0.7f,
                cycleMs = 3800,
            )
            FilamentGlowMotion.Frozen -> FilamentGlowConfig(
                minGlow = 0.18f * boost,
                maxGlow = 0.24f * boost,
                minBgLift = 0.45f,
                maxBgLift = 0.55f,
                cycleMs = 0,
            )
            FilamentGlowMotion.None -> FilamentGlowConfig(
                minGlow = 0.14f * boost,
                maxGlow = 0.14f * boost,
                minBgLift = 0.35f,
                maxBgLift = 0.35f,
                cycleMs = 0,
            )
        }
        ActiveEmphasis.Detail -> when (motion) {
            FilamentGlowMotion.Breathing -> FilamentGlowConfig(
                minGlow = 0.17f * boost,
                maxGlow = 0.38f * boost,
                minBgLift = 0.35f,
                maxBgLift = 1f,
                cycleMs = 2800,
            )
            FilamentGlowMotion.SoftIdle -> FilamentGlowConfig(
                minGlow = 0.12f * boost,
                maxGlow = 0.26f * boost,
                minBgLift = 0.25f,
                maxBgLift = 0.75f,
                cycleMs = 3400,
            )
            FilamentGlowMotion.Frozen -> FilamentGlowConfig(
                minGlow = 0.20f * boost,
                maxGlow = 0.28f * boost,
                minBgLift = 0.45f,
                maxBgLift = 0.55f,
                cycleMs = 0,
            )
            FilamentGlowMotion.None -> FilamentGlowConfig(
                minGlow = 0.16f * boost,
                maxGlow = 0.16f * boost,
                minBgLift = 0.4f,
                maxBgLift = 0.4f,
                cycleMs = 0,
            )
        }
    }
}

private fun DrawScope.drawActiveFilamentGlow(
    primary: Color,
    glowAlpha: Float,
    cornerPx: Float,
) {
    val layers = listOf(
        Triple(14f, 1f, glowAlpha),
        Triple(8f, 0.65f, glowAlpha * 0.75f),
        Triple(4f, 0.35f, glowAlpha * 0.5f),
    )
    layers.forEach { (spread, spreadWeight, alpha) ->
        val spreadPx = spread * spreadWeight
        drawRoundRect(
            color = primary.copy(alpha = alpha.coerceIn(0f, 0.55f)),
            topLeft = Offset(-spreadPx, -spreadPx),
            size = Size(size.width + spreadPx * 2, size.height + spreadPx * 2),
            cornerRadius = CornerRadius(cornerPx + spreadPx),
        )
    }
}

private fun chipBackgroundColor(
    scheme: androidx.compose.material3.ColorScheme,
    isActive: Boolean,
    isExternal: Boolean,
    emphasis: ActiveEmphasis,
    breath: ActiveFilamentBreath,
): Color {
    val base = when {
        isActive && emphasis == ActiveEmphasis.Detail ->
            scheme.surfaceVariant.copy(alpha = 0.58f)
        isActive ->
            scheme.surfaceVariant.copy(alpha = 0.44f)
        isExternal ->
            scheme.surface.copy(alpha = 0.42f)
        else ->
            scheme.surface.copy(alpha = 0.88f)
    }
    if (!isActive || breath.backgroundLift <= 0f) return base
    val liftTint = scheme.primary.copy(alpha = 0.09f + breath.backgroundLift * 0.06f)
    return lerp(base, liftTint, breath.backgroundLift * 0.42f)
}

private fun chipBorderColor(
    scheme: androidx.compose.material3.ColorScheme,
    isActive: Boolean,
    isExternal: Boolean,
): Color {
    return when {
        isActive -> scheme.primary.copy(alpha = 0.16f)
        isExternal -> scheme.outline.copy(alpha = 0.38f)
        else -> scheme.outline.copy(alpha = 0.22f)
    }
}
