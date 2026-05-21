package com.chronoswing.buddydash.network

/** Normalizes saved server URL to origin (no trailing `/api/v1`). */
fun normalizeBambuddyBaseUrl(serverUrl: String): String? {
    var trimmed = serverUrl.trim().trimEnd('/')
    if (trimmed.endsWith("/api/v1")) {
        trimmed = trimmed.removeSuffix("/api/v1")
    }
    return trimmed.takeIf { it.isNotEmpty() }
}

fun printerCoverUrl(serverUrl: String, printerId: Int): String? {
    val base = normalizeBambuddyBaseUrl(serverUrl) ?: return null
    return "$base${BambuddyApi.printerCoverPath(printerId)}"
}
