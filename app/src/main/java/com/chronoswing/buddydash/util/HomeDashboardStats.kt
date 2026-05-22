package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer

data class HomePrinterDashboardCounts(
    val online: Int,
    val printing: Int,
)

fun List<Printer>.homePrinterDashboardCounts(): HomePrinterDashboardCounts {
    var online = 0
    var printing = 0
    for (printer in this) {
        val status = printer.liveStatus ?: continue
        if (status.connected) online++
        if (status.resolveActivityKind() == PrinterActivityKind.Printing) printing++
    }
    return HomePrinterDashboardCounts(online = online, printing = printing)
}

enum class HomeDashboardChipKind {
    Online,
    Printing,
    LoadedSpools,
}

data class HomeDashboardChipModel(
    val kind: HomeDashboardChipKind,
    val count: Int,
)

/**
 * Hide zero-value chips when another chip is positive (less clutter).
 * If everything is zero after load, show all three so the strip stays informative.
 */
fun homeDashboardVisibleChips(
    online: Int,
    printing: Int,
    loadedSpools: Int?,
): List<HomeDashboardChipModel> {
    val all = buildList {
        add(HomeDashboardChipModel(HomeDashboardChipKind.Online, online))
        add(HomeDashboardChipModel(HomeDashboardChipKind.Printing, printing))
        if (loadedSpools != null) {
            add(HomeDashboardChipModel(HomeDashboardChipKind.LoadedSpools, loadedSpools))
        }
    }
    val anyPositive = all.any { it.count > 0 }
    return if (anyPositive) {
        all.filter { it.count > 0 }
    } else {
        all
    }
}
