package com.chronoswing.buddydash.data.model

/** Queue rows for one printer: upcoming (pending) and optional in-progress job. */
data class PrinterQueueSnapshot(
    val upcoming: List<PrintQueueJob> = emptyList(),
    val printing: PrintQueueJob? = null,
)
