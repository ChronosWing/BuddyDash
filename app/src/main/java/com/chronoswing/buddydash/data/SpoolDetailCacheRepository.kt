package com.chronoswing.buddydash.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chronoswing.buddydash.util.BuddyDashDebug
import kotlinx.coroutines.flow.first

private const val TAG_SPOOL_DETAIL_CACHE = "BuddyDash/OfflineCache"
private val Context.spoolDetailCacheDataStore by preferencesDataStore(
    name = "buddydash_spool_detail_cache",
)

class SpoolDetailCacheRepository(private val context: Context) {

    suspend fun load(serverUrl: String, spoolId: Int): SpoolDetailCacheSnapshot? {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return null
        val json = context.spoolDetailCacheDataStore.data.first()[cacheKey(spoolId)] ?: return null
        val snapshot = SpoolDetailCacheCodec.decode(json) ?: return null
        if (HomePrintersCacheRepository.cacheServerKey(snapshot.serverUrl) != key) return null
        if (snapshot.spoolId != spoolId) return null
        return snapshot
    }

    suspend fun save(snapshot: SpoolDetailCacheSnapshot): Boolean {
        val key = HomePrintersCacheRepository.cacheServerKey(snapshot.serverUrl)
        if (key == null) {
            if (BuddyDashDebug.enabled) {
                Log.w(TAG_SPOOL_DETAIL_CACHE, "cacheWrite skip spoolId=${snapshot.spoolId} bad url")
            }
            return false
        }
        return try {
            val normalized = snapshot.copy(serverUrl = key)
            val encoded = SpoolDetailCacheCodec.encode(normalized)
            context.spoolDetailCacheDataStore.edit {
                it[cacheKey(snapshot.spoolId)] = encoded
            }
            if (BuddyDashDebug.enabled) {
                Log.d(
                    TAG_SPOOL_DETAIL_CACHE,
                    "cacheWrite ok spoolId=${snapshot.spoolId} bytes=${encoded.length}",
                )
            }
            true
        } catch (e: Exception) {
            if (BuddyDashDebug.enabled) {
                Log.e(TAG_SPOOL_DETAIL_CACHE, "cacheWrite fail spoolId=${snapshot.spoolId}", e)
            }
            false
        }
    }

    companion object {
        private fun cacheKey(spoolId: Int) = stringPreferencesKey("spool_detail_$spoolId")
    }
}
