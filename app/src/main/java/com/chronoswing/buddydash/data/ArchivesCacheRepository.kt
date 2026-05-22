package com.chronoswing.buddydash.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chronoswing.buddydash.data.model.PrintArchive
import android.util.Log
import com.chronoswing.buddydash.util.BuddyDashDebug
import kotlinx.coroutines.flow.first

private const val TAG_ARCHIVES_CACHE = "BuddyDash/OfflineCache"

private val Context.archivesCacheDataStore by preferencesDataStore(name = "buddydash_archives_cache")

class ArchivesCacheRepository(private val context: Context) {

    suspend fun load(serverUrl: String, printerFilterId: Int?): ArchivesCacheSnapshot? {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return null
        val json = context.archivesCacheDataStore.data.first()[cacheKey(printerFilterId)] ?: return null
        val snapshot = ArchivesCacheCodec.decode(json) ?: return null
        if (HomePrintersCacheRepository.cacheServerKey(snapshot.serverUrl) != key) return null
        if (snapshot.printerFilterId != printerFilterId) return null
        return snapshot
    }

    suspend fun save(
        serverUrl: String,
        printerFilterId: Int?,
        archives: List<PrintArchive>,
        lastUpdatedAtMillis: Long,
    ): Boolean {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl)
        if (key == null) {
            if (BuddyDashDebug.enabled) {
                Log.w(TAG_ARCHIVES_CACHE, "listCacheWrite skip bad url filter=$printerFilterId")
            }
            return false
        }
        return try {
            val snapshot = ArchivesCacheSnapshot(
                serverUrl = key,
                printerFilterId = printerFilterId,
                archives = archives,
                lastUpdatedAtMillis = lastUpdatedAtMillis,
            )
            context.archivesCacheDataStore.edit {
                it[cacheKey(printerFilterId)] = ArchivesCacheCodec.encode(snapshot)
            }
            if (BuddyDashDebug.enabled) {
                Log.d(
                    TAG_ARCHIVES_CACHE,
                    "listCacheWrite ok count=${archives.size} filter=$printerFilterId",
                )
            }
            true
        } catch (e: Exception) {
            if (BuddyDashDebug.enabled) {
                Log.e(TAG_ARCHIVES_CACHE, "listCacheWrite fail filter=$printerFilterId", e)
            }
            false
        }
    }

    suspend fun findArchive(serverUrl: String, archiveId: Int): PrintArchive? {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return null
        val prefs = context.archivesCacheDataStore.data.first()
        for ((prefKey, value) in prefs.asMap()) {
            if (!prefKey.name.startsWith(PREFIX)) continue
            val json = value as? String ?: continue
            val snapshot = ArchivesCacheCodec.decode(json) ?: continue
            if (HomePrintersCacheRepository.cacheServerKey(snapshot.serverUrl) != key) continue
            snapshot.archives.find { it.id == archiveId }?.let { return it }
        }
        return null
    }

    suspend fun saveArchiveDetail(serverUrl: String, archive: PrintArchive) {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return
        val payload = org.json.JSONObject()
            .put("server_url", key)
            .put("archive", ArchivesCacheCodec.encodeArchive(archive))
            .toString()
        context.archivesCacheDataStore.edit {
            it[detailCacheKey(archive.id)] = payload
        }
    }

    suspend fun loadArchiveDetail(serverUrl: String, archiveId: Int): PrintArchive? {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return null
        val json = context.archivesCacheDataStore.data.first()[detailCacheKey(archiveId)] ?: return null
        return try {
            val root = org.json.JSONObject(json)
            if (root.optString("server_url") != key) return findArchive(serverUrl, archiveId)
            root.optJSONObject("archive")?.let { ArchivesCacheCodec.decodeArchive(it) }
        } catch (_: Exception) {
            findArchive(serverUrl, archiveId)
        }
    }

    companion object {
        private const val PREFIX = "archives_"
        private fun cacheKey(printerFilterId: Int?) =
            stringPreferencesKey("${PREFIX}list_${printerFilterId ?: "all"}")
        private fun detailCacheKey(archiveId: Int) =
            stringPreferencesKey("${PREFIX}detail_$archiveId")
    }
}
