package com.chronoswing.buddydash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.network.normalizeBambuddyBaseUrl
import kotlinx.coroutines.flow.first

private val Context.homePrintersCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "buddydash_home_printers_cache",
)

class HomePrintersCacheRepository(private val context: Context) {

    suspend fun load(serverUrl: String): HomePrintersCacheSnapshot? {
        val key = cacheServerKey(serverUrl) ?: return null
        val json = context.homePrintersCacheDataStore.data.first()[CACHE_JSON_KEY] ?: return null
        val snapshot = HomePrintersCacheCodec.decode(json) ?: return null
        return snapshot.takeIf { cacheServerKey(it.serverUrl) == key }
    }

    suspend fun save(serverUrl: String, printers: List<Printer>, lastUpdatedAtMillis: Long) {
        val key = cacheServerKey(serverUrl) ?: return
        val snapshot = HomePrintersCacheSnapshot(
            serverUrl = key,
            printers = printers,
            lastUpdatedAtMillis = lastUpdatedAtMillis,
        )
        context.homePrintersCacheDataStore.edit { preferences ->
            preferences[CACHE_JSON_KEY] = HomePrintersCacheCodec.encode(snapshot)
        }
    }

    suspend fun findPrinter(serverUrl: String, printerId: Int): Printer? =
        load(serverUrl)?.printers?.find { it.id == printerId }

    suspend fun clear(serverUrl: String) {
        val key = cacheServerKey(serverUrl) ?: return
        context.homePrintersCacheDataStore.edit { preferences ->
            val existing = preferences[CACHE_JSON_KEY] ?: return@edit
            val snapshot = HomePrintersCacheCodec.decode(existing) ?: return@edit
            if (cacheServerKey(snapshot.serverUrl) == key) {
                preferences.remove(CACHE_JSON_KEY)
            }
        }
    }

    companion object {
        private val CACHE_JSON_KEY = stringPreferencesKey("home_printers_snapshot")

        fun cacheServerKey(serverUrl: String): String? =
            normalizeBambuddyBaseUrl(serverUrl)
    }
}
