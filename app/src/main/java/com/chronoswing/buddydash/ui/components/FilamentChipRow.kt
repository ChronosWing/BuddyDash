package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.chronoswing.buddydash.util.normalizeFilamentType

@Composable
fun FilamentChipRow(
    slots: List<FilamentSlot>,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    if (slots.isEmpty()) return

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        slots.forEach { slot ->
            if (slot.isLoaded) {
                LoadedFilamentChip(slot = slot, compact = compact)
            } else {
                EmptyFilamentChip(label = slot.label, compact = compact)
            }
        }
    }
}

@Composable
private fun LoadedFilamentChip(
    slot: FilamentSlot,
    compact: Boolean,
) {
    val typeText = normalizeFilamentType(slot.filamentType)?.uppercase()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val swatchSize = if (compact) 14.dp else 18.dp

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!compact) {
            Text(
                text = slot.label,
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
        FilamentColorSwatch(slot = slot, size = swatchSize)
        if (typeText != null) {
            Text(
                text = typeText,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EmptyFilamentChip(
    label: String,
    compact: Boolean,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val swatchSize = if (compact) 14.dp else 18.dp

    Row(
        modifier = Modifier
            .alpha(0.72f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
fun FilamentSlotDetailHeader(
    slot: FilamentSlot,
    typeLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilamentColorSwatch(slot = slot, size = 48.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = slot.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
