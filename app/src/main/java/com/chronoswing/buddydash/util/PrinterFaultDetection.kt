package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.json.JSONArray
import org.json.JSONObject

/** Temporary: trace raw Bambuddy status → BuddyDash activity mapping. Set false before release. */
const val DEBUG_LOG_STATUS_MAP = true

const val TAG_STATUS_MAP = "BuddyDash/StatusMap"

/** Delay before re-reading status after motion commands (home/jog). */
const val PRINTER_STATUS_SETTLE_MS = 1_500L

private val TRANSIENT_PRINTER_STATES = setOf(
    "HOMING", "HOME", "AUTO_HOME", "AUTOHOMING", "G28", "G28ING",
    "PREPARE", "PREPARING", "INITIALIZING", "BUSY",
)

fun PrinterStatus.isTransientState(): Boolean {
    val raw = rawState?.uppercase() ?: return false
    return raw in TRANSIENT_PRINTER_STATES
}

/** @deprecated Use [PrinterStatus.isTransientState]. */
fun isTransientPrinterState(rawState: String?): Boolean =
    rawState?.uppercase() in TRANSIENT_PRINTER_STATES

/**
 * Bambu HMS alert level encoded in the 3rd segment of codes like `0300-0C00-0001-0007`.
 * See Bambu Lab HMS docs: 0001=error, 0002=warning, 0003=notification.
 */
enum class HmsAlertLevel {
    Error,
    Warning,
    Notification,
}

fun parseHmsCodeAlertLevel(code: String): HmsAlertLevel? {
    val parts = code.split("-", "_").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size < 3) return null
    return when (parts[2].uppercase()) {
        "0001" -> HmsAlertLevel.Error
        "0002" -> HmsAlertLevel.Warning
        "0003" -> HmsAlertLevel.Notification
        else -> null
    }
}

/** True when this HMS entry is a fault/alarm, not a warning or notification. */
fun PrinterHmsError.isFault(): Boolean {
    parseHmsCodeAlertLevel(code)?.let { level ->
        return level == HmsAlertLevel.Error
    }
    // Bambuddy OpenAPI `severity` fallback when code segments are unavailable.
    return when (severity) {
        1 -> true
        2, 3 -> false
        else -> false
    }
}

fun PrinterStatus.hasExplicitStatusFault(): Boolean = statusFaultMessages.isNotEmpty()

fun PrinterStatus.hasActiveFault(): Boolean =
    hasExplicitStatusFault() || hmsErrors.any { it.isFault() }

fun PrinterStatus.activeFaultHmsErrors(): List<PrinterHmsError> =
    hmsErrors.filter { it.isFault() }

fun logPrinterStatusMapping(
    json: JSONObject,
    status: PrinterStatus,
    context: String = "",
) {
    if (!DEBUG_LOG_STATUS_MAP) return
    val kind = status.resolveActivityKind()
    val raw = status.rawState
    val hmsTotal = status.hmsErrors.size
    val hmsFaults = status.hmsErrorCount
    val explicit = status.hasExplicitStatusFault()
    val reason = buildString {
        if (explicit) append("explicitFault ")
        append("hmsTotal=$hmsTotal hmsFaults=$hmsFaults ")
        if (hmsTotal > hmsFaults) append("(ignoredNonFaultHms=${hmsTotal - hmsFaults}) ")
        if (kind == PrinterActivityKind.Error && !status.hasActiveFault() && raw != null) {
            append("unexpectedErrorMapping ")
        }
    }.trim()
    Log.d(
        TAG_STATUS_MAP,
        "${context.ifBlank { "status" }} rawState=$raw mapped=$kind errorReason=$reason " +
            "fields=${statusMappingFieldSample(json)}",
    )
}

private fun statusMappingFieldSample(json: JSONObject): String = buildString {
    fun field(key: String) {
        if (!json.has(key) || json.isNull(key)) return
        when (val value = json.opt(key)) {
            is JSONArray -> append("$key[len=${value.length()}] ")
            is JSONObject -> append("$key{…} ")
            else -> append("$key=${value.toString().take(48)} ")
        }
    }
    listOf(
        "state",
        "stage",
        "hms_errors",
        "warnings",
        "printer_error",
        "error_code",
        "error_message",
        "reason",
        "message",
        "task_state",
        "gcode_state",
        "work_state",
    ).forEach(::field)
}
