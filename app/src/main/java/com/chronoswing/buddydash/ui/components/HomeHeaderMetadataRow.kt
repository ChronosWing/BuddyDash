package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.theme.CyanAccent

@Composable
fun HomeHeaderMetadataRow(
    onlineCount: Int,
    printingCount: Int,
    loadedSpoolCount: Int,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    val separatorColor = muted.copy(alpha = 0.42f)
    val printingColor = if (printingCount > 0) {
        CyanAccent.copy(alpha = 0.8f)
    } else {
        muted.copy(alpha = 0.46f)
    }
    val segmentStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.5.sp,
        lineHeight = 14.sp,
    )

    Row(
        modifier = modifier.wrapContentWidth(Alignment.End),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataSegment(
            text = stringResource(R.string.home_header_meta_online, onlineCount),
            style = segmentStyle,
            color = muted,
        )
        MetadataSeparator(color = separatorColor, style = segmentStyle)
        MetadataSegment(
            text = stringResource(R.string.home_header_meta_printing, printingCount),
            style = segmentStyle,
            color = printingColor,
        )
        MetadataSeparator(color = separatorColor, style = segmentStyle)
        MetadataSegment(
            text = stringResource(R.string.home_header_meta_loaded, loadedSpoolCount),
            style = segmentStyle,
            color = muted,
        )
    }
}

@Composable
private fun MetadataSegment(
    text: String,
    style: TextStyle,
    color: Color,
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

@Composable
private fun MetadataSeparator(
    color: Color,
    style: TextStyle,
) {
    Text(
        text = stringResource(R.string.home_header_meta_separator),
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}
