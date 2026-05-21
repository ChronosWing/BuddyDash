package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrintArchive
import com.chronoswing.buddydash.data.model.Printer

/** Temporary: log archive queue/reprint API calls. Set false before release. */
const val DEBUG_LOG_ARCHIVE_REPRINT = true

const val TAG_ARCHIVE_REPRINT = "BuddyDash/ArchiveReprint"

data class ArchiveReprintPrinter(
    val id: Int,
    val name: String,
    val model: String?,
)

data class ArchiveReprintPrinterOptions(
    val compatible: List<ArchiveReprintPrinter>,
    val hiddenIncompatibleCount: Int,
)

/** Filter printers compatible with archive [sliced_for_model] when known. */
fun resolveArchiveReprintPrinters(
    printers: List<Printer>,
    archive: PrintArchive,
): ArchiveReprintPrinterOptions {
    val slicedFor = archive.slicedForModel?.trim()?.takeIf { it.isNotBlank() }
    if (slicedFor == null) {
        return ArchiveReprintPrinterOptions(
            compatible = printers.map { it.toReprintPrinter() },
            hiddenIncompatibleCount = 0,
        )
    }
    val compatible = printers.filter { printer ->
        printer.model?.let { model -> archiveModelsCompatible(slicedFor, model) } == true
    }
    val hidden = (printers.size - compatible.size).coerceAtLeast(0)
    return ArchiveReprintPrinterOptions(
        compatible = compatible.map { it.toReprintPrinter() },
        hiddenIncompatibleCount = hidden,
    )
}

fun defaultArchiveReprintPrinterId(
    archive: PrintArchive,
    compatible: List<ArchiveReprintPrinter>,
): Int? {
    if (compatible.isEmpty()) return null
    archive.printerId?.let { id ->
        if (compatible.any { it.id == id }) return id
    }
    return compatible.first().id
}

fun defaultArchiveReprintQuantity(archive: PrintArchive): Int =
    archive.quantity?.coerceIn(1, 99) ?: 1

private fun Printer.toReprintPrinter() = ArchiveReprintPrinter(
    id = id,
    name = name,
    model = model,
)

private fun archiveModelsCompatible(slicedFor: String, printerModel: String): Boolean {
    val a = normalizePrinterModelToken(slicedFor)
    val b = normalizePrinterModelToken(printerModel)
    if (a.isEmpty() || b.isEmpty()) return false
    return a == b || a.contains(b) || b.contains(a)
}

private fun normalizePrinterModelToken(value: String): String =
    value.lowercase().replace(Regex("[^a-z0-9]"), "")
