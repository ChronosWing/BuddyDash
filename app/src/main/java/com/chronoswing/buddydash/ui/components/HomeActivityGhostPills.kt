package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.theme.CyanAccent

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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GhostActivityPill(
            label = stringResource(R.string.home_header_meta_online, onlineCount),
        )
        GhostActivityPill(
            label = stringResource(R.string.home_header_meta_printing, printingCount),
            accent = printingCount > 0,
        )
        GhostActivityPill(
            label = stringResource(R.string.home_header_meta_loaded, loadedSpoolCount),
        )
    }
}

@Composable
private fun GhostActivityPill(
    label: String,
    accent: Boolean = false,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.03f)
    val textColor = when {
        accent -> CyanAccent.copy(alpha = 0.72f)
        else -> muted.copy(alpha = 0.52f)
    }
    val accentBorder = CyanAccent.copy(alpha = 0.2f)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (accent) containerColor else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (accent) accentBorder else borderColor,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = textColor,
            maxLines = 1,
        )
    }
}
