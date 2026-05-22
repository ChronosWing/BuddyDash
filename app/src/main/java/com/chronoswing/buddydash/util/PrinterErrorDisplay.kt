package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.json.JSONArray
import org.json.JSONObject

const val PRINTER_ERROR_PREVIEW_COUNT = 2

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
    if (array == null || array.length() == 0) return emptyList()
    return buildList {
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val code = entry.optString("code").trim()
            if (code.isBlank()) continue
            add(
                PrinterHmsError(
                    code = code,
                    attr = entry.optInt("attr", 0),
                    module = entry.optInt("module").takeIf { entry.has("module") && !entry.isNull("module") },
                    severity = entry.optInt("severity")
                        .takeIf { entry.has("severity") && !entry.isNull("severity") },
                    detail = parseHmsErrorDetail(entry),
                ),
            )
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
