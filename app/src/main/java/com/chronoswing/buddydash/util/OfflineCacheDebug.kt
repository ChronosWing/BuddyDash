package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.util.BuddyDashDebug

private const val TAG = "BuddyDash/OfflineCache"

enum class PrinterDetailCacheSource {
    FullDetail,
    HomeCard,
    None,
}

enum class SpoolDetailCacheSource {
    FullDetail,
    ListFallback,
    None,
}

enum class OfflineUiResolution {
    LoadedFresh,
    StaleWithCache,
    LimitedFromCache,
    NoCacheOffline,
}

fun logPrinterDetailCacheRead(printerId: Int, hit: Boolean, source: PrinterDetailCacheSource) {
    if (!BuddyDashDebug.enabled) return
    Log.d(TAG, "PrinterDetail cacheRead printerId=$printerId hit=$hit source=$source")
}

fun logPrinterDetailCacheWrite(printerId: Int, success: Boolean) {
    if (!BuddyDashDebug.enabled) return
    Log.d(TAG, "PrinterDetail cacheWrite printerId=$printerId success=$success")
}

fun logSpoolDetailCacheRead(spoolId: Int, source: SpoolDetailCacheSource) {
    if (!BuddyDashDebug.enabled) return
    Log.d(TAG, "SpoolDetail cacheRead spoolId=$spoolId source=$source")
}

fun logSpoolDetailCacheWrite(spoolId: Int, success: Boolean) {
    if (!BuddyDashDebug.enabled) return
    Log.d(TAG, "SpoolDetail cacheWrite spoolId=$spoolId success=$success")
}

fun logArchivesListCacheRead(hit: Boolean, count: Int, printerFilterId: Int?) {
    if (!BuddyDashDebug.enabled) return
    Log.d(
        TAG,
        "Archives listCacheRead hit=$hit count=$count printerFilterId=$printerFilterId",
    )
}

fun logArchivesListCacheWrite(count: Int, success: Boolean, printerFilterId: Int?) {
    if (!BuddyDashDebug.enabled) return
    Log.d(
        TAG,
        "Archives listCacheWrite count=$count success=$success printerFilterId=$printerFilterId",
    )
}

fun logArchiveDetailCacheRead(archiveId: Int, hit: Boolean, fromListFallback: Boolean) {
    if (!BuddyDashDebug.enabled) return
    Log.d(
        TAG,
        "ArchiveDetail cacheRead archiveId=$archiveId hit=$hit listFallback=$fromListFallback",
    )
}

fun logOfflineLoadState(
    screen: String,
    onlineAttempt: Boolean,
    cacheResult: String,
    finalState: OfflineUiResolution,
) {
    if (!BuddyDashDebug.enabled) return
    Log.d(
        TAG,
        "LoadState screen=$screen onlineAttempt=$onlineAttempt cache=$cacheResult final=$finalState",
    )
}
