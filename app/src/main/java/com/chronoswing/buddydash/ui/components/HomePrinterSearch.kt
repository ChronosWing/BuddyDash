package com.chronoswing.buddydash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.util.HomePrinterSearchFilter

@Composable
fun HomePrinterSearchField(
    expanded: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_printers_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            ),
        )
    }
}

@Composable
fun HomePrinterSearchFilterChips(
    expanded: Boolean,
    selectedFilter: HomePrinterSearchFilter,
    onFilterSelected: (HomePrinterSearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val scrollState = rememberScrollState()
        val surface = MaterialTheme.colorScheme.surface
        val fadeColor = surface.copy(alpha = 0.92f)
        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HomePrinterSearchFilter.entries.forEach { filter ->
                    val label = when (filter) {
                        HomePrinterSearchFilter.All -> stringResource(R.string.filter_all)
                        HomePrinterSearchFilter.Active -> stringResource(R.string.filter_active)
                        HomePrinterSearchFilter.NeedsAttention ->
                            stringResource(R.string.filter_needs_attention)
                    }
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = chipColors,
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
                            selectedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = 0.38f),
                        ),
                    )
                }
            }
            if (scrollState.value > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(16.dp)
                        .height(36.dp)
                        .background(
                            Brush.horizontalGradient(
                                0f to fadeColor,
                                1f to Color.Transparent,
                            ),
                        ),
                )
            }
            if (scrollState.value < scrollState.maxValue) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(16.dp)
                        .height(36.dp)
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                1f to fadeColor,
                            ),
                        ),
                )
            }
        }
    }
}
