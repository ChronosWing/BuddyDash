package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.util.normalizeFilamentType
import com.chronoswing.buddydash.util.parseFilamentColor

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
    val dotColor = parseFilamentColor(slot.colorHex)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!compact) {
            Text(
                text = slot.label,
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
        if (typeText != null) {
            Text(
                text = typeText,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        // Remaining % hidden on chips — status remain is not reliable inventory (see FilamentParsing).
    }
}

@Composable
private fun EmptyFilamentChip(
    label: String,
    compact: Boolean,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
        Text(
            text = "—",
            style = MaterialTheme.typography.labelMedium,
            color = muted,
        )
    }
}

@Composable
fun FilamentColorSwatch(
    colorHex: String?,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val swatchColor = if (!isEmpty) parseFilamentColor(colorHex) else null
    val shape = RoundedCornerShape(10.dp)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (swatchColor != null) {
                    Modifier
                        .background(swatchColor)
                        .border(1.dp, outline, shape)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEmpty) 0.5f else 1f))
                        .border(1.dp, outline, shape)
                },
            ),
    )
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
        FilamentColorSwatch(
            colorHex = slot.colorHex,
            isEmpty = !slot.isLoaded,
            size = 48.dp,
        )
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
