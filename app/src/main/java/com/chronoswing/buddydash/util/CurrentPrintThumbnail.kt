package com.chronoswing.buddydash.util

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.network.printerCoverUrl

private const val TAG_PRINT_THUMB = "BuddyDash/PrintThumb"

/** Identifies the current (or last) print job for cover thumbnail cache invalidation. */
data class CurrentPrintThumbnailIdentity(
    val printerId: Int,
    /** Normalized file/job name key; blank when unknown. */
    val fileKey: String,
    /** Queue item id when status=printing, else null. */
    val taskJobId: Int?,
    /** API cover path or status `cover_url` hint used in cache key. */
    val thumbnailPath: String,
    /** Human-readable current file for debug logs. */
    val currentFile: String?,
    /** Previous file captured on identity change (debug only). */
    val previousFile: String? = null,
) {
    fun cacheKey(): String {
        val fileSegment = fileKey.ifBlank { "unknown" }
        val jobSegment = taskJobId?.toString() ?: "none"
        val pathSegment = thumbnailPath.hashCode().toUInt().toString(16)
        return "print-cover-$printerId-$fileSegment-$jobSegment-$pathSegment"
    }

    /** Stable bust param so Coil refetches when print identity changes, not on every poll. */
    fun urlCacheBust(): Long {
        var hash = printerId.toLong()
        hash = 31L * hash + fileKey.hashCode()
        hash = 31L * hash + (taskJobId ?: -1)
        hash = 31L * hash + thumbnailPath.hashCode()
        return hash
    }

    fun samePrintAs(other: CurrentPrintThumbnailIdentity?): Boolean {
        if (other == null) return false
        return printerId == other.printerId &&
            fileKey == other.fileKey &&
            taskJobId == other.taskJobId &&
            thumbnailPath == other.thumbnailPath
    }
}

fun PrinterStatus.toCurrentPrintThumbnailIdentity(
    printerId: Int,
    queueJobId: Int? = null,
    previousFile: String? = null,
): CurrentPrintThumbnailIdentity {
    val fileKey = resolveCurrentPrintFileKey(
        fileName = fileName,
        currentPrint = currentPrint,
        subtaskName = subtaskName,
        gcodeFile = gcodeFile,
    )
    val currentFile = displayCurrentPrintFile(
        fileName = fileName,
        currentPrint = currentPrint,
        subtaskName = subtaskName,
        gcodeFile = gcodeFile,
    )
    return CurrentPrintThumbnailIdentity(
        printerId = printerId,
        fileKey = fileKey,
        taskJobId = queueJobId,
        thumbnailPath = normalizePrintThumbnailPath(coverUrl, printerId),
        currentFile = currentFile,
        previousFile = previousFile,
    )
}

fun currentPrintThumbnailIdentityForFile(
    printerId: Int,
    fileName: String?,
    queueJobId: Int? = null,
): CurrentPrintThumbnailIdentity =
    CurrentPrintThumbnailIdentity(
        printerId = printerId,
        fileKey = fileName?.trim()?.takeIf { it.isNotBlank() } ?: "",
        taskJobId = queueJobId,
        thumbnailPath = BambuddyApi.printerCoverPath(printerId),
        currentFile = fileName?.trim()?.takeIf { it.isNotBlank() },
    )

fun resolveCurrentPrintThumbnailUrl(
    serverUrl: String,
    cameraToken: String,
    identity: CurrentPrintThumbnailIdentity,
): String? =
    printerCoverUrl(
        serverUrl = serverUrl,
        printerId = identity.printerId,
        cameraToken = cameraToken,
        cacheBust = identity.urlCacheBust(),
    )

fun resolveCurrentPrintFileKey(
    fileName: String?,
    currentPrint: String?,
    subtaskName: String?,
    gcodeFile: String?,
): String {
    val name = sequenceOf(currentPrint, subtaskName, fileName)
        .mapNotNull { it?.trim()?.takeIf { s -> s.isNotBlank() } }
        .firstOrNull()
    val gcode = gcodeFile?.trim()?.takeIf { it.isNotBlank() }
    return when {
        name != null && gcode != null -> "$name|${gcode.substringAfterLast('/')}"
        name != null -> name
        gcode != null -> gcode.substringAfterLast('/')
        else -> ""
    }
}

private fun displayCurrentPrintFile(
    fileName: String?,
    currentPrint: String?,
    subtaskName: String?,
    gcodeFile: String?,
): String? =
    sequenceOf(currentPrint, subtaskName, fileName, gcodeFile?.substringAfterLast('/'))
        .mapNotNull { it?.trim()?.takeIf { s -> s.isNotBlank() } }
        .firstOrNull()

fun normalizePrintThumbnailPath(coverUrl: String?, printerId: Int): String {
    val raw = coverUrl?.trim()?.takeIf { it.isNotBlank() } ?: return BambuddyApi.printerCoverPath(printerId)
    val pathOnly = when {
        raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true) -> {
            raw.substringAfter("://").substringAfter('/').let { p ->
                if (p.startsWith("/")) p else "/$p"
            }
        }
        raw.startsWith("/") -> raw
        else -> "/$raw"
    }
    return pathOnly
}

fun logCurrentPrintThumbnailIdentity(
    identity: CurrentPrintThumbnailIdentity,
    imageUrl: String?,
) {
    if (!BuddyDashDebug.enabled) return
    Log.d(
        TAG_PRINT_THUMB,
        "currentFile=${identity.currentFile ?: "—"} " +
            "previousFile=${identity.previousFile ?: "—"} " +
            "currentTaskJobId=${identity.taskJobId ?: "—"} " +
            "thumbnailPath=${identity.thumbnailPath} " +
            "imageCacheKey=${identity.cacheKey()} " +
            "thumbnailUrl=${redactPrintThumbToken(imageUrl)}",
    )
}

private fun redactPrintThumbToken(url: String?): String =
    url?.replace(Regex("token=[^&]+"), "token=***") ?: "—"

@Composable
fun rememberCurrentPrintThumbnailIdentity(
    printerId: Int,
    status: PrinterStatus?,
    fileName: String? = null,
    queueJobId: Int? = null,
): CurrentPrintThumbnailIdentity {
    var previousFile by remember(printerId) { mutableStateOf<String?>(null) }
    val identity = remember(status, fileName, queueJobId, printerId, previousFile) {
        when {
            status != null -> status.toCurrentPrintThumbnailIdentity(
                printerId = printerId,
                queueJobId = queueJobId,
                previousFile = previousFile,
            )
            else -> currentPrintThumbnailIdentityForFile(
                printerId = printerId,
                fileName = fileName,
                queueJobId = queueJobId,
            )
        }
    }
    LaunchedEffect(identity.fileKey, identity.taskJobId, identity.thumbnailPath) {
        previousFile = identity.currentFile
    }
    return identity
}
