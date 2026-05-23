package com.chronoswing.buddydash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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

    val homeIdleGlowMultiplier: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[HOME_IDLE_GLOW_MULTIPLIER_KEY] ?: DEFAULT_VISUAL_MULTIPLIER
    }

    val homeHeaderAmbientMultiplier: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[HOME_HEADER_AMBIENT_MULTIPLIER_KEY] ?: DEFAULT_VISUAL_MULTIPLIER
    }

    val homePrintGlowMultiplier: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[HOME_PRINT_GLOW_MULTIPLIER_KEY] ?: DEFAULT_VISUAL_MULTIPLIER
    }

    val homeDebugForcePrintGlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[HOME_DEBUG_FORCE_PRINT_GLOW_KEY] ?: false
    }

    val homeDebugShowLogoGlowBounds: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[HOME_DEBUG_SHOW_LOGO_GLOW_BOUNDS_KEY] ?: false
    }

    suspend fun saveHomeDebugShowLogoGlowBounds(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_DEBUG_SHOW_LOGO_GLOW_BOUNDS_KEY] = enabled
        }
    }

    suspend fun saveHomePrintGlowMultiplier(multiplier: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_PRINT_GLOW_MULTIPLIER_KEY] = multiplier
        }
    }

    suspend fun saveHomeDebugForcePrintGlow(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_DEBUG_FORCE_PRINT_GLOW_KEY] = enabled
        }
    }

    suspend fun saveHomeIdleGlowMultiplier(multiplier: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_IDLE_GLOW_MULTIPLIER_KEY] = multiplier
        }
    }

    suspend fun saveHomeHeaderAmbientMultiplier(multiplier: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_HEADER_AMBIENT_MULTIPLIER_KEY] = multiplier
        }
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
        const val DEFAULT_VISUAL_MULTIPLIER = 1f
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val API_KEY_KEY = stringPreferencesKey("api_key")
        private val CAMERA_TOKEN_KEY = stringPreferencesKey("camera_token")
        private val HOME_IDLE_GLOW_MULTIPLIER_KEY = floatPreferencesKey("home_idle_glow_multiplier")
        private val HOME_HEADER_AMBIENT_MULTIPLIER_KEY = floatPreferencesKey("home_header_ambient_multiplier")
        private val HOME_PRINT_GLOW_MULTIPLIER_KEY = floatPreferencesKey("home_print_glow_multiplier")
        private val HOME_DEBUG_FORCE_PRINT_GLOW_KEY = booleanPreferencesKey("home_debug_force_print_glow")
        private val HOME_DEBUG_SHOW_LOGO_GLOW_BOUNDS_KEY = booleanPreferencesKey("home_debug_show_logo_glow_bounds")
    }
}
