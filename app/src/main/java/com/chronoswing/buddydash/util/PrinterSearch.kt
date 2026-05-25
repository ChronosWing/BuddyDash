package com.chronoswing.buddydash.util

import androidx.annotation.StringRes
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus

/** Show Home printer search when printer count is at or above this value. */
const val HOME_PRINTER_SEARCH_MIN_COUNT = 4

enum class HomePrinterSearchFilter {
    All,
    Active,
    NeedsAttention,
    Printing,
    Offline,
}

fun applyHomePrinterSearch(
    printers: List<Printer>,
    query: String,
    filter: HomePrinterSearchFilter,
): List<Printer> {
    val searched = filterPrintersForSearch(printers, query)
    return when (filter) {
        HomePrinterSearchFilter.All -> searched
        HomePrinterSearchFilter.Active -> searched.filter { printerMatchesActiveFilter(it) }
        HomePrinterSearchFilter.NeedsAttention -> searched.filter { printerMatchesNeedsAttentionFilter(it) }
        HomePrinterSearchFilter.Printing -> searched.filter { printerMatchesPrintingFilter(it) }
        HomePrinterSearchFilter.Offline -> searched.filter { printerMatchesOfflineFilter(it) }
    }
}

fun filterPrintersForSearch(printers: List<Printer>, query: String): List<Printer> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return printers
    return printers.filter { printerMatchesSearchQuery(it, trimmed) }
}

fun printerMatchesSearchQuery(printer: Printer, query: String): Boolean {
    if (query.isEmpty()) return true
    if (printer.name.contains(query, ignoreCase = true)) return true
    val model = printer.model?.trim()
    if (!model.isNullOrEmpty() && model.contains(query, ignoreCase = true)) return true
    return false
}

/** Non-idle work: printing, paused, heating, calibrating, preparing, homing, etc. */
fun printerMatchesActiveFilter(printer: Printer): Boolean {
    val status = printer.liveStatus ?: return false
    if (!status.connected) return false
    return when (status.resolveActivityKind()) {
        PrinterActivityKind.Printing,
        PrinterActivityKind.Paused,
        PrinterActivityKind.Busy,
        -> true
        PrinterActivityKind.Idle,
        PrinterActivityKind.Offline,
        PrinterActivityKind.Error,
        -> false
    }
}

/** Actionable/warning states; excludes maintenance due-soon (`DueSoon`). */
fun printerMatchesNeedsAttentionFilter(printer: Printer): Boolean {
    if (printer.maintenanceIndicator == MaintenanceHomeIndicator.Due) return true
    val status = printer.liveStatus ?: return true
    if (!status.connected) return true
    if (status.resolveHmsAlertSeverity() != HmsSeverity.Ok) return true
    if (status.hasFilamentOrAmsIssue()) return true
    if (status.awaitingPlateClear == true) return true
    val raw = status.rawState?.uppercase()
    if (raw == "FAILED") return true
    return when (status.resolveActivityKind()) {
        PrinterActivityKind.Error,
        PrinterActivityKind.Offline,
        -> true
        else -> false
    }
}

fun printerMatchesPrintingFilter(printer: Printer): Boolean {
    val status = printer.liveStatus ?: return false
    return status.resolveActivityKind() == PrinterActivityKind.Printing
}

fun printerMatchesOfflineFilter(printer: Printer): Boolean {
    val status = printer.liveStatus ?: return true
    return !status.connected
}

private fun PrinterStatus.hasFilamentOrAmsIssue(): Boolean =
    filamentSlots.any { !it.isLoaded }

/** Empty-state copy when search UI is open and the combined filter yields no printers. */
@StringRes
fun homeSearchEmptyMessageRes(
    query: String,
    filter: HomePrinterSearchFilter,
): Int {
    if (query.trim().isNotEmpty()) return R.string.no_printers_match_search
    return when (filter) {
        HomePrinterSearchFilter.All -> R.string.no_printers_match_search
        HomePrinterSearchFilter.Active -> R.string.no_active_printers
        HomePrinterSearchFilter.NeedsAttention -> R.string.no_printers_need_attention
        HomePrinterSearchFilter.Printing -> R.string.no_printers_printing
        HomePrinterSearchFilter.Offline -> R.string.no_printers_offline
    }
}
