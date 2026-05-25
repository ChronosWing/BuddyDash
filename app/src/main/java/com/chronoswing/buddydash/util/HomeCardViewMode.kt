package com.chronoswing.buddydash.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class HomeCardViewMode(val index: Int) {
    Minimal(0),
    Standard(1),
    Detailed(2);

    companion object {
        fun fromIndex(index: Int): HomeCardViewMode =
            entries.firstOrNull { it.index == index } ?: Standard
    }
}

data class HomeCardLayoutValues(
    val cardPadding: Dp,
    val contentSpacing: Dp,
    val listItemSpacing: Dp,
    val thumbnailSize: Dp,
)

fun HomeCardViewMode.layoutValues(): HomeCardLayoutValues = when (this) {
    HomeCardViewMode.Minimal -> HomeCardLayoutValues(
        cardPadding = 8.dp,
        contentSpacing = 4.dp,
        listItemSpacing = 6.dp,
        thumbnailSize = 44.dp,
    )
    HomeCardViewMode.Standard -> HomeCardLayoutValues(
        cardPadding = 12.dp,
        contentSpacing = 8.dp,
        listItemSpacing = 10.dp,
        thumbnailSize = 64.dp,
    )
    HomeCardViewMode.Detailed -> HomeCardLayoutValues(
        cardPadding = 14.dp,
        contentSpacing = 10.dp,
        listItemSpacing = 12.dp,
        thumbnailSize = 72.dp,
    )
}
