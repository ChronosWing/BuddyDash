package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.json.JSONObject

private const val TAG = "BuddyDash/PrinterResilience"

/** Minimal disconnected status for printers that fail enrichment or status parsing. */
fun disconnectedPrinterStatus(): PrinterStatus = PrinterStatus(
    connected = false,
    rawState = null,
    progress = null,
    fileName = null,
    remainingTimeSeconds = null,
    nozzleTemp = null,
    bedTemp = null,
)

/**
 * After enrichment, ensure every printer card has a confirmed offline shell when live status
 * could not be loaded. Preserves existing [Printer.liveStatus] when present.
 */
fun Printer.withEnrichFallback(): Printer =
    if (liveStatus != null) this else copy(liveStatus = disconnectedPrinterStatus())

fun List<Printer>.withEnrichFallbacks(): List<Printer> = map { it.withEnrichFallback() }

/** Best-effort offline status from partial/malformed status JSON. */
fun parseDegradedPrinterStatus(json: JSONObject): PrinterStatus = PrinterStatus(
    connected = json.optBoolean("connected", false),
    rawState = json.optString("state").takeIf { it.isNotBlank() },
    progress = json.optDouble("progress")
        .takeIf { json.has("progress") && !json.isNull("progress") && it.isFinite() }
        ?.toFloat(),
    fileName = null,
    remainingTimeSeconds = null,
    nozzleTemp = null,
    bedTemp = null,
)

fun JSONObject.optSafeInt(key: String): Int? =
    if (!has(key) || isNull(key)) {
        null
    } else {
        runCatching { getInt(key) }.getOrNull()
    }

fun logPrinterEnrichFailure(printerId: Int, stage: String, error: Throwable) {
    if (BuddyDashDebug.enabled) {
        Log.w(TAG, "printer enrich failed id=$printerId stage=$stage message=${error.message}")
    }
}
