package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.util.archiveHasMaterialDisplay
import com.chronoswing.buddydash.util.formatArchiveDetailMaterialType
import com.chronoswing.buddydash.util.parseArchiveFilamentSwatch

@Composable
fun ArchiveMaterialRow(
    archive: PrintArchive,
    modifier: Modifier = Modifier,
    swatchSize: Dp = 14.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
    fontWeight: FontWeight? = null,
    tappable: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    if (!archiveHasMaterialDisplay(archive)) return

    val typeLabel = formatArchiveDetailMaterialType(archive)
    val swatch = parseArchiveFilamentSwatch(archive)
    val rowModifier = if (tappable && onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        typeLabel?.let { type ->
            Text(
                text = type,
                style = textStyle,
                fontWeight = fontWeight,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        swatch?.takeIf { it.colorHexes.isNotEmpty() }?.let { colors ->
            FilamentColorSwatch(
                colorHexes = colors.colorHexes,
                isTranslucent = colors.isTranslucent,
                alpha = colors.alpha,
                size = swatchSize,
            )
        }
        if (tappable && onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            )
        }
    }
}
