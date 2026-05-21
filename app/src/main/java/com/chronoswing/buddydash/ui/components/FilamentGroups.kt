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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.util.FilamentSourceGroup
import com.chronoswing.buddydash.util.SlotInventoryKey
import com.chronoswing.buddydash.util.groupByFilamentSource
import com.chronoswing.buddydash.util.isActiveSlot
import com.chronoswing.buddydash.util.normalizeFilamentType

/** Compact grouped filament row for home printer cards. */
@Composable
fun FilamentHomeGroupsRow(
    slots: List<FilamentSlot>,
    activeKey: SlotInventoryKey?,
    modifier: Modifier = Modifier,
) {
    if (slots.isEmpty()) return
    val groups = slots.groupByFilamentSource()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        groups.forEachIndexed { index, group ->
            if (index > 0) {
                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(width = 1.dp, height = 18.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                )
            }
            Text(
                text = "${group.sourceLabel}:",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                fontWeight = FontWeight.Medium,
            )
            group.slots.forEach { slot ->
                FilamentSlotChip(
                    slot = slot,
                    activeKey = activeKey,
                    compact = true,
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = group.sourceLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        group.slots.forEach { slot ->
            FilamentSlotCard(slot = slot, activeKey = activeKey)
        }
    }
}

@Composable
fun FilamentSlotChip(
    slot: FilamentSlot,
    activeKey: SlotInventoryKey?,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    if (slot.isLoaded) {
        LoadedFilamentSlotChip(
            slot = slot,
            isActive = slot.isActiveSlot(activeKey),
            compact = compact,
            modifier = modifier,
        )
    } else {
        EmptyFilamentSlotChip(
            label = slot.label,
            compact = compact,
            modifier = modifier,
        )
    }
}

@Composable
fun FilamentSlotCard(
    slot: FilamentSlot,
    activeKey: SlotInventoryKey?,
    modifier: Modifier = Modifier,
) {
    val isActive = slot.isActiveSlot(activeKey)
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val borderWidth = if (isActive) 2.dp else 1.dp
    val bgAlpha = if (slot.isLoaded) 1f else 0.72f

    Row(
        modifier = modifier
            .alpha(bgAlpha)
            .fillMaxWidth()
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    ActiveFilamentIndicator()
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    modifier: Modifier = Modifier,
) {
    val typeText = normalizeFilamentType(slot.filamentType)?.uppercase()
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val swatchSize = if (compact) 12.dp else 16.dp

    Row(
        modifier = modifier
            .clip(shape)
            .border(if (isActive) 2.dp else 1.dp, borderColor, shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = slot.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
        FilamentColorSwatch(slot = slot, size = swatchSize)
        if (typeText != null) {
            Text(
                text = typeText,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (isActive) {
            ActiveFilamentIndicator(compact = true)
        }
        if (compact) {
            slot.remainPercent?.let { remain ->
                Text(
                    text = "$remain%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyFilamentSlotChip(
    label: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val shape = RoundedCornerShape(8.dp)
    val swatchSize = if (compact) 12.dp else 16.dp

    Row(
        modifier = modifier
            .alpha(0.7f)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                shape = shape,
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
            .padding(horizontal = 6.dp, vertical = if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
        EmptySlotSwatch(size = swatchSize)
    }
}

@Composable
private fun ActiveFilamentIndicator(compact: Boolean = false) {
    val tint = MaterialTheme.colorScheme.primary
    if (compact) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp),
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxWithPrimaryDot()
            Text(
                text = "Active",
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun BoxWithPrimaryDot() {
    Spacer(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}
