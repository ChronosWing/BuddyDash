package com.chronoswing.buddydash.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chronoswing.buddydash.util.ValidatedConnectionSettings
import com.chronoswing.buddydash.util.validateConnectionSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "buddydash_settings",
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() },
    ),
)

class SettingsRepository(private val context: Context) {

    private val recoveryMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val settingsRecoveryMessage: SharedFlow<String> = recoveryMessages.asSharedFlow()

    private val safePreferences: Flow<Preferences> = context.settingsDataStore.data
        .catch { error ->
            Log.w(TAG, "Settings DataStore read failed; resetting to defaults", error)
            recoveryMessages.tryEmit(RECOVERY_MESSAGE)
            runCatching { clearAllSettings() }
            emit(emptyPreferences())
        }

    val serverUrl: Flow<String> = safePreferences.map { preferences ->
        preferences.safeString(SERVER_URL_KEY)
    }

    val apiKey: Flow<String> = safePreferences.map { preferences ->
        preferences.safeString(API_KEY_KEY)
    }

    val cameraToken: Flow<String> = safePreferences.map { preferences ->
        preferences.safeString(CAMERA_TOKEN_KEY)
    }

    val homeIdleGlowMultiplier: Flow<Float> = safePreferences.map { preferences ->
        preferences.safeFloat(HOME_IDLE_GLOW_MULTIPLIER_KEY, DEFAULT_VISUAL_MULTIPLIER)
    }

    val homeHeaderAmbientMultiplier: Flow<Float> = safePreferences.map { preferences ->
        preferences.safeFloat(HOME_HEADER_AMBIENT_MULTIPLIER_KEY, DEFAULT_VISUAL_MULTIPLIER)
    }

    val homePrintGlowMultiplier: Flow<Float> = safePreferences.map { preferences ->
        preferences.safeFloat(HOME_PRINT_GLOW_MULTIPLIER_KEY, DEFAULT_VISUAL_MULTIPLIER)
    }

    val homeDebugForcePrintGlow: Flow<Boolean> = safePreferences.map { preferences ->
        preferences.safeBoolean(HOME_DEBUG_FORCE_PRINT_GLOW_KEY, false)
    }

    val homeDebugShowLogoGlowBounds: Flow<Boolean> = safePreferences.map { preferences ->
        preferences.safeBoolean(HOME_DEBUG_SHOW_LOGO_GLOW_BOUNDS_KEY, false)
    }

    suspend fun saveConnectionSettings(
        serverUrl: String,
        apiKey: String,
        cameraToken: String,
    ): Result<Unit> {
        val validated = validateConnectionSettings(serverUrl, apiKey, cameraToken)
        if (validated.isFailure) {
            return Result.failure(validated.exceptionOrNull() ?: IllegalArgumentException("Invalid settings"))
        }
        return runCatching {
            persistConnectionSettings(validated.getOrThrow())
        }
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

    private suspend fun persistConnectionSettings(settings: ValidatedConnectionSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = settings.serverUrl
            preferences[API_KEY_KEY] = settings.apiKey
            preferences[CAMERA_TOKEN_KEY] = settings.cameraToken
        }
    }

    private suspend fun clearAllSettings() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        const val DEFAULT_VISUAL_MULTIPLIER = 1f
        const val RECOVERY_MESSAGE =
            "Saved settings were invalid and have been reset. Please enter your server URL and API key again."

        private const val TAG = "BuddyDash/Settings"
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

private fun Preferences.safeString(key: Preferences.Key<String>): String =
    runCatching { this[key].orEmpty() }.getOrElse { "" }

private fun Preferences.safeFloat(key: Preferences.Key<Float>, default: Float): Float =
    runCatching { this[key] ?: default }.getOrElse { default }

private fun Preferences.safeBoolean(key: Preferences.Key<Boolean>, default: Boolean): Boolean =
    runCatching { this[key] ?: default }.getOrElse { default }
