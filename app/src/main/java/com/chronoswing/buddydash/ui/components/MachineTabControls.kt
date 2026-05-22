package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val MachineButtonShape = RoundedCornerShape(12.dp)

@Composable
fun machineActionButtonColors(enabled: Boolean): ButtonColors {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    return if (enabled) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = primary.copy(alpha = 0.16f),
            contentColor = onSurface.copy(alpha = 0.96f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            disabledContentColor = onSurface.copy(alpha = 0.38f),
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
            contentColor = onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
            disabledContentColor = onSurface.copy(alpha = 0.38f),
        )
    }
}

@Composable
fun machineActionBorder(enabled: Boolean): BorderStroke {
    val primary = MaterialTheme.colorScheme.primary
    return BorderStroke(
        width = if (enabled) 2.dp else 1.dp,
        color = if (enabled) {
            primary.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        },
    )
}

@Composable
fun Modifier.machineActionShadow(enabled: Boolean): Modifier =
    if (enabled) {
        shadow(elevation = 2.dp, shape = MachineButtonShape, spotColor = Color.Black.copy(alpha = 0.35f))
    } else {
        this
    }

@Composable
fun MachineStepFilterChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected && enabled) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            labelColor = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (enabled) 0.72f else 0.38f,
            ),
            selectedContainerColor = primary.copy(alpha = 0.22f),
            selectedLabelColor = primary.copy(alpha = if (enabled) 1f else 0.5f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            selectedBorderColor = primary.copy(alpha = 0.75f),
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}
