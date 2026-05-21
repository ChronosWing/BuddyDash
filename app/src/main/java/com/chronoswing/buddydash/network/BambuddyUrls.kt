package com.chronoswing.buddydash.network

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Normalizes saved server URL to origin (no trailing `/api/v1`). */
fun normalizeBambuddyBaseUrl(serverUrl: String): String? {
    var trimmed = serverUrl.trim().trimEnd('/')
    if (trimmed.endsWith("/api/v1")) {
        trimmed = trimmed.removeSuffix("/api/v1")
    }
    return trimmed.takeIf { it.isNotEmpty() }
}

/** Cover image requires `?token=` query param; API key / Bearer auth are not accepted. */
fun printerCoverUrl(serverUrl: String, printerId: Int, cameraToken: String): String? {
    val base = normalizeBambuddyBaseUrl(serverUrl) ?: return null
    val token = cameraToken.trim()
    if (token.isEmpty() || printerId < 0) return null
    val encoded = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
    return "$base${BambuddyApi.printerCoverPath(printerId)}?token=$encoded"
}
