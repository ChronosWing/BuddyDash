package com.chronoswing.buddydash.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box

/** Minimum screen width (dp) before adaptive two-column grids activate. */
const val BUDDYDASH_EXPANDED_WIDTH_MIN_DP = 600

/** Max width for search/forms on expanded layouts so controls do not stretch edge-to-edge. */
const val BUDDYDASH_EXPANDED_FORM_MAX_WIDTH_DP = 720

/** @see BUDDYDASH_EXPANDED_WIDTH_MIN_DP */
const val HOME_PRINTER_GRID_MIN_WIDTH_DP = BUDDYDASH_EXPANDED_WIDTH_MIN_DP

fun buddyDashExpandedGridColumnCount(screenWidthDp: Int): Int =
    if (screenWidthDp >= BUDDYDASH_EXPANDED_WIDTH_MIN_DP) 2 else 1

fun homePrinterGridColumnCount(screenWidthDp: Int): Int =
    buddyDashExpandedGridColumnCount(screenWidthDp)

@Composable
fun rememberBuddyDashExpandedGridColumnCount(): Int {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return buddyDashExpandedGridColumnCount(screenWidthDp)
}

@Composable
fun rememberHomePrinterGridColumnCount(): Int = rememberBuddyDashExpandedGridColumnCount()

@Composable
fun Modifier.buddyDashExpandedFormWidth(gridColumns: Int): Modifier =
    if (gridColumns > 1) {
        widthIn(max = BUDDYDASH_EXPANDED_FORM_MAX_WIDTH_DP.dp).fillMaxWidth()
    } else {
        fillMaxWidth()
    }

/** Centers expanded-width form/search rows while leaving compact layout unchanged. */
@Composable
fun BuddyDashExpandedFormContainer(
    gridColumns: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (gridColumns <= 1) {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    } else {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.buddyDashExpandedFormWidth(gridColumns)) {
                content()
            }
        }
    }
}

@Composable
fun rememberIsBuddyDashExpandedWidth(): Boolean =
    rememberBuddyDashExpandedGridColumnCount() > 1
