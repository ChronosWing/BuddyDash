package com.chronoswing.buddydash.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class HomeCardDensity(val index: Int) {
    Comfortable(0),
    Compact(1),
    Dense(2);

    companion object {
        fun fromIndex(index: Int): HomeCardDensity = entries.firstOrNull { it.index == index } ?: Comfortable
    }
}

data class HomeCardDensityValues(
    val cardPadding: Dp,
    val contentSpacing: Dp,
    val listItemSpacing: Dp,
    val thumbnailSize: Dp,
    val chipSpacing: Dp,
    val cameraSpacing: Dp,
)

fun HomeCardDensity.layoutValues(): HomeCardDensityValues = when (this) {
    HomeCardDensity.Comfortable -> HomeCardDensityValues(
        cardPadding = 12.dp,
        contentSpacing = 8.dp,
        listItemSpacing = 10.dp,
        thumbnailSize = 64.dp,
        chipSpacing = 6.dp,
        cameraSpacing = 12.dp,
    )
    HomeCardDensity.Compact -> HomeCardDensityValues(
        cardPadding = 9.dp,
        contentSpacing = 5.dp,
        listItemSpacing = 7.dp,
        thumbnailSize = 52.dp,
        chipSpacing = 4.dp,
        cameraSpacing = 8.dp,
    )
    HomeCardDensity.Dense -> HomeCardDensityValues(
        cardPadding = 7.dp,
        contentSpacing = 3.dp,
        listItemSpacing = 5.dp,
        thumbnailSize = 44.dp,
        chipSpacing = 3.dp,
        cameraSpacing = 6.dp,
    )
}
