package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.network.normalizeBambuddyBaseUrl
import java.net.URI
import java.net.URISyntaxException

data class ValidatedConnectionSettings(
    val serverUrl: String,
    val apiKey: String,
    val cameraToken: String,
)

/**
 * Validates and normalizes connection settings before persistence or network calls.
 * Never logs or includes secrets in error messages.
 */
fun validateConnectionSettings(
    serverUrl: String,
    apiKey: String,
    cameraToken: String = "",
): Result<ValidatedConnectionSettings> {
    val trimmedKey = apiKey.trim()
    if (trimmedKey.isEmpty()) {
        return Result.failure(IllegalArgumentException("API key is required"))
    }

    val normalizedUrl = normalizeServerUrlForStorage(serverUrl)
        ?: return Result.failure(IllegalArgumentException("Server URL is invalid"))

    return Result.success(
        ValidatedConnectionSettings(
            serverUrl = normalizedUrl,
            apiKey = trimmedKey,
            cameraToken = cameraToken.trim(),
        ),
    )
}

/** Normalizes a user-entered server URL for storage and API calls. Returns null when invalid. */
fun normalizeServerUrlForStorage(serverUrl: String): String? {
    val trimmed = serverUrl.trim()
    if (trimmed.isEmpty()) return null

    val withScheme = when {
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        else -> "http://$trimmed"
    }

    val normalized = normalizeBambuddyBaseUrl(withScheme) ?: return null
    return try {
        val uri = URI(normalized)
        val host = uri.host
        if (host.isNullOrBlank()) {
            null
        } else {
            normalized.trimEnd('/')
        }
    } catch (_: URISyntaxException) {
        null
    }
}
