package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.json.JSONArray
import org.json.JSONObject

/** Temporary: trace raw Bambuddy status → BuddyDash activity mapping. Set false before release. */
val DEBUG_LOG_STATUS_MAP: Boolean get() = BuddyDashDebug.enabled

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

/**
 * Aggregate HMS health level for a printer.
 * Error wins over Warning wins over Unknown wins over Ok.
 */
enum class HmsSeverity {
    /** No active HMS entries. */
    Ok,
    /** Entries exist but alert level cannot be determined from code or severity field. */
    Unknown,
    /** One or more active HMS warning entries. */
    Warning,
    /** One or more active HMS error/fault entries (takes priority over Warning and Unknown). */
    Error,
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

/**
 * Alert level for a single HMS entry.
 *
 * Returns null when neither the code segment nor the severity field can determine the level.
 * Callers must treat null as "unknown" — never silently treat as OK.
 */
fun PrinterHmsError.alertLevel(): HmsAlertLevel? {
    parseHmsCodeAlertLevel(code)?.let { return it }
    return when (severity) {
        1 -> HmsAlertLevel.Error
        2 -> HmsAlertLevel.Warning
        3 -> HmsAlertLevel.Notification
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

/**
 * Aggregate HMS health severity across all HMS entries.
 *
 * Rule: Ok only when hmsErrors is empty.
 * Error > Warning > Unknown > Ok.
 *
 * Notification entries are treated as Unknown, not Ok.
 * Rationale: BambuBuddy surfaces every HMS entry in its own UI regardless of level.
 * Any entry that exists should be visible in BuddyDash — only an empty list is Ok.
 * Notification entries that are genuinely benign will read as "HMS" (amber Unknown chip)
 * which the user can tap to inspect; the detail sheet shows the actual severity.
 */
fun PrinterStatus.resolveHmsAlertSeverity(): HmsSeverity {
    if (hmsErrors.isEmpty()) return HmsSeverity.Ok

    var hasUnknown = false
    var hasWarning = false

    for (entry in hmsErrors) {
        val level = entry.alertLevel()
        if (DEBUG_LOG_STATUS_MAP) {
            Log.d(
                TAG_STATUS_MAP,
                "resolveHmsAlertSeverity: entry code='${entry.code}' severity=${entry.severity} " +
                    "module=${entry.module} attr=${entry.attr} " +
                    "→ alertLevel=$level reason=${hmsAlertLevelReason(entry, level)}",
            )
        }
        when (level) {
            HmsAlertLevel.Error -> {
                if (DEBUG_LOG_STATUS_MAP) Log.d(TAG_STATUS_MAP, "  → resolved Error (short-circuit)")
                return HmsSeverity.Error
            }
            HmsAlertLevel.Warning -> hasWarning = true
            // Notification exists but is still an active HMS entry — treat as Unknown,
            // not Ok. Ok is reserved for an empty hms_errors list.
            HmsAlertLevel.Notification -> hasUnknown = true
            // Level undetectable (no parseable code segment, no recognized severity) → Unknown.
            null -> hasUnknown = true
        }
    }

    return when {
        hasWarning -> HmsSeverity.Warning
        hasUnknown -> HmsSeverity.Unknown
        else -> HmsSeverity.Ok     // only reachable if hmsErrors is empty (already guarded above)
    }.also { result ->
        if (DEBUG_LOG_STATUS_MAP) {
            Log.d(TAG_STATUS_MAP, "resolveHmsAlertSeverity → $result (hasWarning=$hasWarning hasUnknown=$hasUnknown)")
        }
    }
}

private fun hmsAlertLevelReason(entry: PrinterHmsError, level: HmsAlertLevel?): String {
    val codeLevel = parseHmsCodeAlertLevel(entry.code)
    return when {
        codeLevel != null -> "code-segment-3 parsed '$codeLevel'"
        entry.severity != null -> "severity-fallback=${entry.severity} → $level"
        else -> "no-code-no-severity → Unknown"
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
    val hmsSeverity = status.resolveHmsAlertSeverity()
    val raw = status.rawState
    val hmsTotal = status.hmsErrors.size
    val hmsFaults = status.hmsErrorCount
    val explicit = status.hasExplicitStatusFault()
    val reason = buildString {
        if (explicit) append("explicitFault ")
        append("hmsTotal=$hmsTotal hmsFaults=$hmsFaults hmsSeverity=$hmsSeverity ")
        if (hmsTotal > hmsFaults) append("(nonFaultHms=${hmsTotal - hmsFaults}) ")
        if (kind == PrinterActivityKind.Error && !status.hasActiveFault() && raw != null) {
            append("unexpectedErrorMapping ")
        }
    }.trim()
    Log.d(
        TAG_STATUS_MAP,
        "${context.ifBlank { "status" }} rawState=$raw mapped=$kind errorReason=$reason " +
            "fields=${statusMappingFieldSample(json)}",
    )
    // Log each HMS entry with resolved level
    status.hmsErrors.forEachIndexed { i, entry ->
        Log.d(
            TAG_STATUS_MAP,
            "  hms[$i] code=${entry.code} alertLevel=${entry.alertLevel()} " +
                "severity=${entry.severity} module=${entry.module} " +
                "detail=${entry.detail?.take(64)}",
        )
    }
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
