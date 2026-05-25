package com.chronoswing.buddydash.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val TAG = "BuddyDash/EncCreds"
private const val PREFS_NAME = "buddydash_secure_credentials"
private const val KEY_SERVER_URL = "server_url"
private const val KEY_API_KEY = "api_key"
private const val KEY_CAMERA_TOKEN = "camera_token"
private const val KEY_MIGRATED = "migrated_from_datastore"

/**
 * Encrypted credential storage using AndroidX Security Crypto.
 *
 * Stores server URL, API key, and camera token in [EncryptedSharedPreferences]
 * backed by Android Keystore AES256-SIV keys.
 *
 * On first access after migration, legacy plain-text values are copied from
 * DataStore and then cleared from unencrypted storage.
 */
class EncryptedCredentialStore(private val context: Context) {

    private val prefs: SharedPreferences? by lazy { createEncryptedPrefs() }

    @Suppress("DEPRECATION")
    private fun createEncryptedPrefs(): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to create encrypted prefs", e)
        null
    }

    val isAvailable: Boolean get() = prefs != null

    fun readServerUrl(): String = prefs?.getString(KEY_SERVER_URL, "") ?: ""
    fun readApiKey(): String = prefs?.getString(KEY_API_KEY, "") ?: ""
    fun readCameraToken(): String = prefs?.getString(KEY_CAMERA_TOKEN, "") ?: ""
    fun hasMigrated(): Boolean = prefs?.getBoolean(KEY_MIGRATED, false) ?: false

    fun saveCredentials(serverUrl: String, apiKey: String, cameraToken: String): Boolean = try {
        prefs?.edit()
            ?.putString(KEY_SERVER_URL, serverUrl)
            ?.putString(KEY_API_KEY, apiKey)
            ?.putString(KEY_CAMERA_TOKEN, cameraToken)
            ?.apply()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to save encrypted credentials", e)
        false
    }

    fun markMigrated(): Boolean = try {
        prefs?.edit()?.putBoolean(KEY_MIGRATED, true)?.apply()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to mark migration complete", e)
        false
    }

    fun hasCredentials(): Boolean {
        val p = prefs ?: return false
        return p.getString(KEY_SERVER_URL, "")?.isNotBlank() == true ||
            p.getString(KEY_API_KEY, "")?.isNotBlank() == true
    }
}
