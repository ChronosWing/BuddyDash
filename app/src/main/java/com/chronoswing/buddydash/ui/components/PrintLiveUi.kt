package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SingleBed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.network.printerCoverUrl
import com.chronoswing.buddydash.util.formatFilenameForDisplay

@Composable
fun PrintFileNameText(
    fileName: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val displayName = formatFilenameForDisplay(fileName)
    Text(
        text = displayName,
        modifier = modifier.semantics {
            contentDescription = fileName
        },
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun PrintFileHighlight(
    label: String,
    fileName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrintFileNameText(
            fileName = fileName.ifBlank { "—" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PrintFileHighlightWithCover(
    label: String,
    fileName: String,
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    showCoverThumbnail: Boolean,
    modifier: Modifier = Modifier,
) {
    val coverAvailable = remember(serverUrl, printerId, cameraToken) {
        printerCoverUrl(serverUrl, printerId, cameraToken) != null
    }
    if (showCoverThumbnail && coverAvailable) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PrinterCoverImage(
                serverUrl = serverUrl,
                cameraToken = cameraToken,
                printerId = printerId,
                size = 52.dp,
                cornerRadius = 8.dp,
            )
            PrintFileHighlight(
                label = label,
                fileName = fileName,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        PrintFileHighlight(label = label, fileName = fileName, modifier = modifier)
    }
}

@Composable
fun PrintTempsRow(
    nozzleTemp: String,
    bedTemp: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TempWithIcon(
            icon = Icons.Filled.LocalFireDepartment,
            value = compactTempForDisplay(nozzleTemp),
            description = stringResource(R.string.nozzle_temp_value, nozzleTemp),
            valueStyle = valueStyle,
        )
        TempWithIcon(
            icon = Icons.Filled.SingleBed,
            value = compactTempForDisplay(bedTemp),
            description = stringResource(R.string.bed_temp_value, bedTemp),
            valueStyle = valueStyle,
        )
    }
}

@Composable
private fun TempWithIcon(
    icon: ImageVector,
    value: String,
    description: String,
    valueStyle: TextStyle,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = valueStyle,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun compactTempForDisplay(formatted: String): String =
    formatted.replace("°C", "°")
