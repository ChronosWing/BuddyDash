package com.chronoswing.buddydash.util

import android.net.Uri
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus

/** Parsed NFC / deep link: `buddydash://printer/{idOrName}/clear-plate` */
data class ClearPlateDeepLink(
    val printerKey: String,
)

sealed class ClearPlateActionOutcome {
    data object Debounced : ClearPlateActionOutcome()
    data object InvalidLink : ClearPlateActionOutcome()
    data object MissingCredentials : ClearPlateActionOutcome()
    data object ConnectionRequired : ClearPlateActionOutcome()
    data object PrinterActive : ClearPlateActionOutcome()
    data object PrinterNotFound : ClearPlateActionOutcome()
    data object ApiFailed : ClearPlateActionOutcome()
    data object AlreadyCleared : ClearPlateActionOutcome()
    data class Success(val printerName: String) : ClearPlateActionOutcome()
}

/** Matches `buddydash://printer/{idOrName}/clear-plate` (case-insensitive scheme/host/action). */
private val CLEAR_PLATE_URI_PATTERN = Regex(
    """buddydash://printer/([^/]+)/clear-plate/?""",
    RegexOption.IGNORE_CASE,
)

fun parseClearPlateDeepLink(uri: Uri?): ClearPlateDeepLink? {
    if (uri == null) return null
    parseFromUriString(uri.toString())?.let { return it }
    val host = uri.host ?: return null
    if (!host.equals("printer", ignoreCase = true)) return null
    val pathSegments = uri.pathSegments
    if (pathSegments.size == 2 && pathSegments[1].equals("clear-plate", ignoreCase = true)) {
        return printerKeyFromSegment(pathSegments[0])
    }
    val pathParts = uri.path
        ?.trim('/')
        ?.split('/')
        ?.filter { it.isNotEmpty() }
        .orEmpty()
    if (pathParts.size == 2 && pathParts[1].equals("clear-plate", ignoreCase = true)) {
        return printerKeyFromSegment(pathParts[0])
    }
    return null
}

internal fun parseFromUriString(raw: String): ClearPlateDeepLink? {
    val match = CLEAR_PLATE_URI_PATTERN.matchEntire(raw.trim()) ?: return null
    return printerKeyFromSegment(match.groupValues[1])
}

private fun printerKeyFromSegment(segment: String): ClearPlateDeepLink? {
    val printerKey = (Uri.decode(segment) ?: segment).trim()
    if (printerKey.isEmpty()) return null
    return ClearPlateDeepLink(printerKey = printerKey)
}

fun resolvePrinterByKey(printers: List<Printer>, printerKey: String): Printer? {
    printerKey.toIntOrNull()?.let { id ->
        printers.find { it.id == id }?.let { return it }
    }
    return printers.find { it.name.equals(printerKey, ignoreCase = true) }
}

/** True when NFC clear-plate should be blocked (printing, paused, or heating). */
fun blocksNfcPlateClear(status: PrinterStatus): Boolean {
    val raw = status.rawState?.uppercase() ?: return false
    if (raw == "RUNNING" || raw == "PAUSE") return true
    if (raw.contains("HEAT")) return true
    return false
}

/**
 * True when the plate is known to be cleared (`awaiting_plate_clear == false`).
 * Returns false when state is unknown (`null`) or the printer is actively printing.
 */
fun isPlateKnownCleared(status: PrinterStatus): Boolean {
    if (!status.connected || blocksNfcPlateClear(status)) return false
    return status.awaitingPlateClear == false
}

/** Detects API success/error text that indicates clear-plate was already acknowledged. */
fun isClearPlateAlreadyAcknowledged(message: String): Boolean {
    val normalized = message.lowercase()
    return normalized.contains("already") && normalized.contains("clear") ||
        normalized.contains("already cleared") ||
        normalized.contains("already clear") ||
        normalized.contains("not awaiting") ||
        normalized.contains("no-op") ||
        normalized.contains("noop") ||
        normalized.contains("nothing to do")
}

/**
 * NFC tags should only store a BuddyDash deep link — never API keys or server URLs.
 * Example: `buddydash://printer/MECHABROBOT/clear-plate` or `buddydash://printer/42/clear-plate`
 */
const val NFC_CLEAR_PLATE_EXAMPLE_URI = "buddydash://printer/{printerId}/clear-plate"

object NfcClearPlateDebounce {
    private const val DEBOUNCE_MS = 2_000L

    @Volatile
    private var lastActionKey: String? = null

    @Volatile
    private var lastActionAtMillis: Long = 0L

    @Synchronized
    fun shouldProcess(actionKey: String): Boolean {
        val now = System.currentTimeMillis()
        if (lastActionKey == actionKey && now - lastActionAtMillis < DEBOUNCE_MS) {
            return false
        }
        lastActionKey = actionKey
        lastActionAtMillis = now
        return true
    }
}
