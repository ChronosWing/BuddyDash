package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.json.JSONArray
import org.json.JSONObject

const val PRINTER_ERROR_PREVIEW_COUNT = 2

private val debugHms: Boolean get() = BuddyDashDebug.enabled
private const val TAG_HMS_PARSE = "BuddyDash/HmsParse"

data class PrinterErrorDisplay(
    val showCard: Boolean,
    val lines: List<String>,
    val hasKnownDetails: Boolean,
)

fun PrinterStatus.resolvePrinterErrorDisplay(
    noDetailsFallback: String,
): PrinterErrorDisplay {
    val lines = buildPrinterErrorLines()
    val inErrorState = resolveActivityKind() == PrinterActivityKind.Error
    val showCard = inErrorState || lines.isNotEmpty()
    return PrinterErrorDisplay(
        showCard = showCard,
        lines = when {
            lines.isNotEmpty() -> lines
            inErrorState -> listOf(noDetailsFallback)
            else -> emptyList()
        },
        hasKnownDetails = lines.isNotEmpty(),
    )
}

fun PrinterStatus.buildPrinterErrorLines(): List<String> =
    (statusFaultMessages + activeFaultHmsErrors().map { it.toDisplayLine() })
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

fun PrinterHmsError.toDisplayLine(): String {
    detail?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    val codeText = code.trim().takeIf { it.isNotBlank() }
    return buildList {
        codeText?.let { add("Code: $it") }
        module?.let { add("Module $it") }
        severity?.let { add("Severity $it") }
        if (attr != 0) add("Attr $attr")
    }.joinToString(" · ").ifBlank { codeText ?: "HMS alert" }
}

fun parsePrinterHmsErrors(array: JSONArray?): List<PrinterHmsError> {
    if (debugHms) {
        Log.d(TAG_HMS_PARSE, "hms_errors array: ${if (array == null) "null" else "len=${array.length()}"}")
    }
    if (array == null || array.length() == 0) return emptyList()

    return buildList {
        for (i in 0 until array.length()) {
            val raw = array.opt(i)
            val entry = array.optJSONObject(i)
            if (debugHms) {
                Log.d(TAG_HMS_PARSE, "  hms[$i] rawType=${raw?.javaClass?.simpleName} entry=${entry?.toString()?.take(200)}")
            }
            if (entry == null) {
                // Array item is a non-object (e.g. plain string or number) — skip
                continue
            }
            val code = entry.optString("code").trim()
            val attr = entry.optInt("attr", 0)
            val module = entry.optInt("module").takeIf { entry.has("module") && !entry.isNull("module") }
            val severity = entry.optInt("severity")
                .takeIf { entry.has("severity") && !entry.isNull("severity") }
            val detail = parseHmsErrorDetail(entry)

            // Include entry if it has a code, or if it carries at least one meaningful field
            // so that no-code entries from BambuBuddy are not silently dropped.
            val hasUsefulData = code.isNotBlank() || severity != null || detail != null || attr != 0 || module != null
            if (!hasUsefulData) {
                if (debugHms) Log.d(TAG_HMS_PARSE, "  hms[$i] SKIPPED — no meaningful fields")
                continue
            }

            val parsed = PrinterHmsError(
                code = code,
                attr = attr,
                module = module,
                severity = severity,
                detail = detail,
            )
            if (debugHms) {
                Log.d(
                    TAG_HMS_PARSE,
                    "  hms[$i] KEPT code='$code' severity=$severity attr=$attr module=$module " +
                        "detail=${detail?.take(64)} alertLevel=${parsed.alertLevel()}",
                )
            }
            add(parsed)
        }
    }.also { result ->
        if (debugHms) {
            Log.d(TAG_HMS_PARSE, "parsePrinterHmsErrors result: ${result.size} entries kept from ${array.length()} raw")
        }
    }
}

private fun parseHmsErrorDetail(entry: JSONObject): String? {
    val keys = listOf(
        "message",
        "error_message",
        "description",
        "msg",
        "text",
        "title",
    )
    for (key in keys) {
        if (!entry.has(key) || entry.isNull(key)) continue
        entry.optString(key).trim().takeIf { it.isNotBlank() }?.let { return it }
    }
    return null
}

/** Explicit fault fields only — not warnings, notifications, or informational HMS lists. */
fun parsePrinterStatusFaultMessages(json: JSONObject): List<String> = buildList {
    fun addLabelled(key: String, label: String) {
        if (!json.has(key) || json.isNull(key)) return
        val value = json.opt(key)?.toString()?.trim().orEmpty()
        if (value.isBlank()) return
        add("$label: $value")
    }
    fun addRaw(key: String) {
        if (!json.has(key) || json.isNull(key)) return
        val value = json.optString(key).trim()
        if (value.isNotBlank()) add(value)
    }

    addLabelled("error_message", "Message")
    addRaw("printer_error")
    addLabelled("status_reason", "Reason")
    if (json.has("error_code") && !json.isNull("error_code")) {
        val code = json.opt("error_code")?.toString()?.trim()
        if (!code.isNullOrBlank()) add("Code: $code")
    }
    if (json.has("reason") && !json.isNull("reason")) {
        val reason = json.opt("reason")?.toString()?.trim()
        if (!reason.isNullOrBlank()) add(reason)
    }
    addAll(parseStringishJsonArray(json.optJSONArray("alarms"), prefix = "Alarm"))
}

private fun parseStringishJsonArray(array: JSONArray?, prefix: String): List<String> {
    if (array == null || array.length() == 0) return emptyList()
    return buildList {
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is String -> item.trim().takeIf { it.isNotBlank() }?.let { add("$prefix: $it") }
                is JSONObject -> {
                    parseHmsErrorDetail(item)?.let { add(it) }
                        ?: item.optString("code").trim().takeIf { it.isNotBlank() }?.let {
                            add("$prefix: $it")
                        }
                }
                else -> item?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let {
                    add("$prefix: $it")
                }
            }
        }
    }
}
