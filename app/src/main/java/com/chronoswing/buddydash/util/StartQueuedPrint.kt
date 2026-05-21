package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi

enum class StartNextQueuedPrintBlockReason {
    None,
    PlateNotClear,
    PrinterBusy,
}

data class StartNextQueuedPrintReadiness(
    val canStart: Boolean,
    val blockReason: StartNextQueuedPrintBlockReason = StartNextQueuedPrintBlockReason.PrinterBusy,
)

/** Whether POST /api/v1/queue/{item_id}/start can be used for the next queued job. */
fun evaluateStartNextQueuedPrintReadiness(
    status: PrinterStatus?,
    queuedItemCount: Int,
    hasStartEndpoint: Boolean = BambuddyApi.hasQueueStartEndpoint,
): StartNextQueuedPrintReadiness {
    if (queuedItemCount <= 0 || !hasStartEndpoint) {
        return StartNextQueuedPrintReadiness(
            canStart = false,
            blockReason = StartNextQueuedPrintBlockReason.PrinterBusy,
        )
    }
    val printerReady = evaluateQueueAndStartReadiness(status, hasStartEndpoint)
    return when {
        printerReady.canQueueAndStart -> StartNextQueuedPrintReadiness(
            canStart = true,
            blockReason = StartNextQueuedPrintBlockReason.None,
        )
        printerReady.blockReason == QueueAndStartBlockReason.PlateNotClear -> {
            StartNextQueuedPrintReadiness(
                canStart = false,
                blockReason = StartNextQueuedPrintBlockReason.PlateNotClear,
            )
        }
        else -> StartNextQueuedPrintReadiness(
            canStart = false,
            blockReason = StartNextQueuedPrintBlockReason.PrinterBusy,
        )
    }
}
