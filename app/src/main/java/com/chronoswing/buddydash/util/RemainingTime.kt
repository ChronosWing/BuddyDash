package com.chronoswing.buddydash.util

import org.json.JSONObject

/**
 * Bambuddy GET /printers/{id}/status sets [remaining_time] from MQTT mc_remaining_time in **minutes**
 * (see bambuddy `printer_manager.py` / `bambu_mqtt.py`). Convert to seconds for display.
 */
fun remainingMinutesToSeconds(minutes: Int?): Int? {
    if (minutes == null || minutes <= 0) return null
    return minutes * 60
}

fun parseRemainingTimeSeconds(json: JSONObject): Int? {
    if (!json.has("remaining_time") || json.isNull("remaining_time")) return null
    return remainingMinutesToSeconds(json.optInt("remaining_time"))
}

/** Message for temporary debug logging from the API client layer. */
fun etaDebugLogLine(json: JSONObject, rawState: String?, parsedSeconds: Int?): String? {
    val state = rawState?.uppercase() ?: return null
    if (state != "RUNNING" && state != "PAUSE") return null
    val remaining = if (json.has("remaining_time") && !json.isNull("remaining_time")) {
        json.opt("remaining_time")
    } else {
        "missing"
    }
    val minutes = if (parsedSeconds != null) parsedSeconds / 60 else null
    return "state=$state remaining_time=$remaining (api minutes) " +
        "mc_remaining_time=${json.opt("mc_remaining_time")} parsedMinutes=$minutes parsedSeconds=$parsedSeconds"
}
