package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.theme.CyanAccent
import com.chronoswing.buddydash.ui.theme.OnlineGreen

private val LoadedFilamentTint = Color(0xFFF5B84A)

@Composable
fun HomeActivityGhostPillsRow(
    onlineCount: Int,
    printingCount: Int,
    loadedSpoolCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GhostActivityPill(
            count = onlineCount,
            kind = HomeActivityPillKind.Online,
        )
        GhostActivityPill(
            count = printingCount,
            kind = HomeActivityPillKind.Printing,
            emphasize = printingCount > 0,
        )
        GhostActivityPill(
            count = loadedSpoolCount,
            kind = HomeActivityPillKind.Loaded,
        )
    }
}

private enum class HomeActivityPillKind {
    Online,
    Printing,
    Loaded,
}

@Composable
private fun GhostActivityPill(
    count: Int,
    kind: HomeActivityPillKind,
    emphasize: Boolean = false,
) {
    val style = ghostPillStyle(kind = kind, emphasize = emphasize)
    val label = when (kind) {
        HomeActivityPillKind.Online ->
            stringResource(R.string.home_header_meta_online, count)
        HomeActivityPillKind.Printing ->
            stringResource(R.string.home_header_meta_printing, count)
        HomeActivityPillKind.Loaded ->
            stringResource(R.string.home_header_meta_loaded, count)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (emphasize) MaterialTheme.colorScheme.surface.copy(alpha = 0.03f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (emphasize) style.borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = style.iconTint,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = style.textTint,
                maxLines = 1,
            )
        }
    }
}

private data class GhostPillStyle(
    val icon: ImageVector,
    val iconTint: Color,
    val textTint: Color,
    val borderColor: Color,
)

@Composable
private fun ghostPillStyle(
    kind: HomeActivityPillKind,
    emphasize: Boolean,
): GhostPillStyle {
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
    return when (kind) {
        HomeActivityPillKind.Online -> GhostPillStyle(
            icon = Icons.Outlined.Circle,
            iconTint = OnlineGreen.copy(alpha = 0.68f),
            textTint = mutedText,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        )
        HomeActivityPillKind.Printing -> {
            val tint = if (emphasize) CyanAccent.copy(alpha = 0.78f) else CyanAccent.copy(alpha = 0.55f)
            GhostPillStyle(
                icon = Icons.Outlined.Print,
                iconTint = tint,
                textTint = if (emphasize) tint else mutedText,
                borderColor = CyanAccent.copy(alpha = 0.2f),
            )
        }
        HomeActivityPillKind.Loaded -> {
            val tint = LoadedFilamentTint.copy(alpha = 0.68f)
            GhostPillStyle(
                icon = Icons.Outlined.DonutLarge,
                iconTint = tint,
                textTint = mutedText,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
            )
        }
    }
}
