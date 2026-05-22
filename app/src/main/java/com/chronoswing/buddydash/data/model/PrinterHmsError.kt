package com.chronoswing.buddydash.data.model

/** One HMS alert from Bambuddy `hms_errors` (OpenAPI: code, attr, module, severity). */
data class PrinterHmsError(
    val code: String,
    val attr: Int = 0,
    val module: Int? = null,
    val severity: Int? = null,
    /** Optional text when the API includes a message-like field on the HMS object. */
    val detail: String? = null,
)
