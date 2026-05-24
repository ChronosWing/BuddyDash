package com.chronoswing.buddydash.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Minimum screen width (dp) before Home printer cards use a multi-column grid. */
const val HOME_PRINTER_GRID_MIN_WIDTH_DP = 600

fun homePrinterGridColumnCount(screenWidthDp: Int): Int =
    if (screenWidthDp >= HOME_PRINTER_GRID_MIN_WIDTH_DP) 2 else 1

@Composable
fun rememberHomePrinterGridColumnCount(): Int {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return homePrinterGridColumnCount(screenWidthDp)
}
