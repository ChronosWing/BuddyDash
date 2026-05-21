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

private fun tokenImageUrl(
    serverUrl: String,
    printerId: Int,
    cameraToken: String,
    path: String,
    cacheBust: Long? = null,
): String? {
    val base = normalizeBambuddyBaseUrl(serverUrl) ?: return null
    val token = cameraToken.trim()
    if (token.isEmpty() || printerId < 0) return null
    val encoded = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
    val bust = cacheBust?.let { "&t=$it" }.orEmpty()
    return "$base$path?token=$encoded$bust"
}

/** Cover image requires `?token=` query param; API key / Bearer auth are not accepted. */
fun printerCoverUrl(serverUrl: String, printerId: Int, cameraToken: String): String? =
    tokenImageUrl(serverUrl, printerId, cameraToken, BambuddyApi.printerCoverPath(printerId))

/** Live camera snapshot (OpenAPI: GET …/camera/snapshot?token=). */
fun printerCameraSnapshotUrl(
    serverUrl: String,
    printerId: Int,
    cameraToken: String,
    cacheBust: Long? = null,
): String? =
    tokenImageUrl(
        serverUrl,
        printerId,
        cameraToken,
        BambuddyApi.cameraSnapshotPath(printerId),
        cacheBust = cacheBust,
    )
