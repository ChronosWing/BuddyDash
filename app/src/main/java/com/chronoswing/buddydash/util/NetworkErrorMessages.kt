package com.chronoswing.buddydash.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Maps transport/API failures to short user-facing copy. */
fun Throwable.toUserNetworkMessage(fallback: String): String {
    when (this) {
        is IllegalArgumentException -> return message?.takeIf { it.isNotBlank() } ?: fallback
        is UnknownHostException -> return "Can't reach server. Check the URL and network."
        is SocketTimeoutException -> return "Request timed out. Try again."
        is SSLException -> return "Secure connection failed. Check the server URL."
        is IOException -> {
            val msg = message.orEmpty()
            when {
                msg.contains("Canceled", ignoreCase = true) -> return fallback
                msg.contains("timeout", ignoreCase = true) ->
                    return "Request timed out. Try again."
                msg.contains("Failed to connect", ignoreCase = true) ||
                    msg.contains("Connection refused", ignoreCase = true) ||
                    msg.contains("Network is unreachable", ignoreCase = true) ->
                    return "Can't reach server. Check the URL and network."
            }
        }
    }
    val msg = message.orEmpty()
    return when {
        msg.contains("401", ignoreCase = true) ||
            msg.contains("Unauthorized", ignoreCase = true) ->
            "Invalid API key."
        msg.contains("403", ignoreCase = true) -> "Access denied."
        msg.contains("404", ignoreCase = true) -> "Not found on server."
        msg.contains("500", ignoreCase = true) ||
            msg.contains("502", ignoreCase = true) ||
            msg.contains("503", ignoreCase = true) ->
            "Server error. Try again later."
        msg.isNotBlank() -> msg
        else -> fallback
    }
}
