package com.chronoswing.buddydash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "buddydash_settings",
)

class SettingsRepository(private val context: Context) {

    val serverUrl: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[SERVER_URL_KEY].orEmpty()
    }

    val apiKey: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[API_KEY_KEY].orEmpty()
    }

    val cameraToken: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[CAMERA_TOKEN_KEY].orEmpty()
    }

    suspend fun saveServerUrl(url: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url
        }
    }

    suspend fun saveApiKey(key: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[API_KEY_KEY] = key
        }
    }

    suspend fun saveCameraToken(token: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[CAMERA_TOKEN_KEY] = token
        }
    }

    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val API_KEY_KEY = stringPreferencesKey("api_key")
        private val CAMERA_TOKEN_KEY = stringPreferencesKey("camera_token")
    }
}
