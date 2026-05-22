package com.chronoswing.buddydash.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.spoolsCacheDataStore by preferencesDataStore(name = "buddydash_spools_cache")

class SpoolsCacheRepository(private val context: Context) {

    suspend fun load(serverUrl: String): SpoolsCacheSnapshot? {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return null
        val json = context.spoolsCacheDataStore.data.first()[CACHE_JSON_KEY] ?: return null
        val snapshot = SpoolsCacheCodec.decode(json) ?: return null
        return snapshot.takeIf { HomePrintersCacheRepository.cacheServerKey(it.serverUrl) == key }
    }

    suspend fun save(
        serverUrl: String,
        spools: List<com.chronoswing.buddydash.data.model.SpoolInventoryItem>,
        printerFilamentActivityById: Map<Int, com.chronoswing.buddydash.util.PrinterFilamentActivity>,
        lastUpdatedAtMillis: Long,
    ) {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return
        val snapshot = SpoolsCacheSnapshot(
            serverUrl = key,
            spools = spools,
            printerFilamentActivityById = printerFilamentActivityById,
            lastUpdatedAtMillis = lastUpdatedAtMillis,
        )
        context.spoolsCacheDataStore.edit { it[CACHE_JSON_KEY] = SpoolsCacheCodec.encode(snapshot) }
    }

    suspend fun findSpool(serverUrl: String, spoolId: Int): com.chronoswing.buddydash.data.model.SpoolInventoryItem? =
        load(serverUrl)?.spools?.find { it.id == spoolId }

    suspend fun clear(serverUrl: String) {
        val key = HomePrintersCacheRepository.cacheServerKey(serverUrl) ?: return
        context.spoolsCacheDataStore.edit { preferences ->
            val existing = preferences[CACHE_JSON_KEY] ?: return@edit
            val snapshot = SpoolsCacheCodec.decode(existing) ?: return@edit
            if (HomePrintersCacheRepository.cacheServerKey(snapshot.serverUrl) == key) {
                preferences.remove(CACHE_JSON_KEY)
            }
        }
    }

    companion object {
        private val CACHE_JSON_KEY = stringPreferencesKey("spools_snapshot")
    }
}
