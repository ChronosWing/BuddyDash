package com.chronoswing.buddydash.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.util.Log
import com.chronoswing.buddydash.util.BuddyDashDebug
import kotlinx.coroutines.flow.first

private const val TAG_PRINTER_DETAIL_CACHE = "BuddyDash/OfflineCache"
private val Context.printerDetailCacheDataStore by preferencesDataStore(
    name = "buddydash_printer_detail_cache",
)

class PrinterDetailCacheRepository(private val context: Context) {

    suspend fun load(serverUrl: String, printerId: Int): PrinterDetailCacheSnapshot? {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return null
        val json = context.printerDetailCacheDataStore.data.first()[cacheKey(printerId)] ?: return null
        val snapshot = PrinterDetailCacheCodec.decode(json) ?: return null
        if (HomePrintersCacheRepository.cacheServerKey(snapshot.serverUrl) != key) return null
        if (snapshot.printerId != printerId) return null
        return snapshot
    }

    suspend fun save(snapshot: PrinterDetailCacheSnapshot): Boolean {
        val key = HomePrintersCacheRepository.cacheServerKey(snapshot.serverUrl)
        if (key == null) {
            if (BuddyDashDebug.enabled) {
                Log.w(TAG_PRINTER_DETAIL_CACHE, "cacheWrite skip printerId=${snapshot.printerId} bad url")
            }
            return false
        }
        return try {
            val normalized = snapshot.copy(serverUrl = key)
            val encoded = PrinterDetailCacheCodec.encode(normalized)
            context.printerDetailCacheDataStore.edit {
                it[cacheKey(snapshot.printerId)] = encoded
            }
            if (BuddyDashDebug.enabled) {
                Log.d(
                    TAG_PRINTER_DETAIL_CACHE,
                    "cacheWrite ok printerId=${snapshot.printerId} bytes=${encoded.length}",
                )
            }
            true
        } catch (e: Exception) {
            if (BuddyDashDebug.enabled) {
                Log.e(TAG_PRINTER_DETAIL_CACHE, "cacheWrite fail printerId=${snapshot.printerId}", e)
            }
            false
        }
    }

    suspend fun clear(serverUrl: String, printerId: Int) {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return
        context.printerDetailCacheDataStore.edit { preferences ->
            val existing = preferences[cacheKey(printerId)] ?: return@edit
            val snapshot = PrinterDetailCacheCodec.decode(existing) ?: return@edit
            if (HomePrintersCacheRepository.cacheServerKey(snapshot.serverUrl) == key) {
                preferences.remove(cacheKey(printerId))
            }
        }
    }

    companion object {
        private fun cacheKey(printerId: Int) = stringPreferencesKey("printer_detail_$printerId")
    }
}
