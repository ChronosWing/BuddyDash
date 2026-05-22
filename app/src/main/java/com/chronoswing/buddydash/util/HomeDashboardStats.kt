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

