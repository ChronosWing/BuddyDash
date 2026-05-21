package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer

/** Show Home printer search when printer count is at or above this value. */
const val HOME_PRINTER_SEARCH_MIN_COUNT = 4

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
