package com.chronoswing.buddydash.network

import com.chronoswing.buddydash.data.model.PrintQueueJob
import com.chronoswing.buddydash.util.QueueThumbnailSource
import com.chronoswing.buddydash.util.resolveQueueThumbnailSource
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

/** Bambuddy image endpoints require `?token=` stream token (same as web `withStreamToken`). */
fun tokenAuthenticatedImageUrl(
    serverUrl: String,
    path: String,
    cameraToken: String,
    cacheBust: Long? = null,
): String? {
    val base = normalizeBambuddyBaseUrl(serverUrl) ?: return null
    val token = cameraToken.trim()
    if (token.isEmpty()) return null
    val encoded = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
    val pathPart = when {
        path.startsWith("http://", ignoreCase = true) ||
            path.startsWith("https://", ignoreCase = true) -> {
            if (path.contains("token=")) return path
            if (path.startsWith(base)) path.removePrefix(base) else return null
        }
        else -> path
    }
    val normalizedPath = when {
        pathPart.startsWith("/") -> pathPart
        else -> "/$pathPart"
    }
    val query = buildList {
        cacheBust?.let { add("v=$it") }
        add("token=$encoded")
    }
    val separator = if (normalizedPath.contains('?')) "&" else "?"
    return "$base$normalizedPath$separator${query.joinToString("&")}"
}

/** Cover image requires `?token=` query param; API key / Bearer auth are not accepted. */
fun printerCoverUrl(serverUrl: String, printerId: Int, cameraToken: String): String? =
    tokenAuthenticatedImageUrl(serverUrl, BambuddyApi.printerCoverPath(printerId), cameraToken)

/** Live camera snapshot (OpenAPI: GET …/camera/snapshot?token=). */
fun printerCameraSnapshotUrl(
    serverUrl: String,
    printerId: Int,
    cameraToken: String,
    cacheBust: Long? = null,
): String? =
    tokenAuthenticatedImageUrl(
        serverUrl,
        BambuddyApi.cameraSnapshotPath(printerId),
        cameraToken,
        cacheBust = cacheBust,
    )

data class QueueJobThumbnailUrl(
    val source: QueueThumbnailSource,
    val url: String?,
)

/**
 * Resolves queue thumbnail URL using the same API routes as Bambuddy web QueuePage.
 * Never uses filesystem `archive_thumbnail` paths or the printer's live cover image.
 */
fun queueJobThumbnailUrl(
    serverUrl: String,
    cameraToken: String,
    job: PrintQueueJob,
): QueueJobThumbnailUrl {
    val resolution = resolveQueueThumbnailSource(job)
    val apiPath = resolution.apiPath
    if (apiPath == null) {
        return QueueJobThumbnailUrl(resolution.source, null)
    }
    val cacheBust = if (resolution.source == QueueThumbnailSource.ARCHIVE) {
        System.currentTimeMillis()
    } else {
        null
    }
    val url = tokenAuthenticatedImageUrl(serverUrl, apiPath, cameraToken, cacheBust = cacheBust)
    return QueueJobThumbnailUrl(resolution.source, url)
}

/** Archive thumbnail (GET /api/v1/archives/{id}/thumbnail?token=). */
fun archiveThumbnailUrl(
    serverUrl: String,
    archiveId: Int,
    cameraToken: String,
): String? =
    tokenAuthenticatedImageUrl(
        serverUrl,
        BambuddyApi.archiveThumbnailPath(archiveId),
        cameraToken,
        cacheBust = System.currentTimeMillis(),
    )
