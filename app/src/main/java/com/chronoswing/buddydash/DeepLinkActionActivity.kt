package com.chronoswing.buddydash

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import com.chronoswing.buddydash.util.performNfcOutcomeHaptic
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.nfc.NfcActionExecutor
import com.chronoswing.buddydash.util.NfcActionOutcome
import com.chronoswing.buddydash.util.resolveNfcActionOutcomeMessage
import kotlinx.coroutines.launch

/**
 * Headless handler for NFC / deep-link automations.
 * Transparent theme — shows only a result toast and finishes without opening Home.
 *
 * Handles both:
 * - `NDEF_DISCOVERED` from physical NFC tag scans
 * - `ACTION_VIEW` from deep links / adb testing
 *
 * Supported actions:
 * - `buddydash://printer/{id}/clear-plate`
 * - `buddydash://printer/{id}/toggle-power`
 * - `buddydash://printer/{id}/finish`
 */
class DeepLinkActionActivity : ComponentActivity() {

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val homePrintersCacheRepository by lazy { HomePrintersCacheRepository(applicationContext) }
    private val apiClient by lazy { BambuddyApiClient() }
    private val executor by lazy {
        NfcActionExecutor(
            settingsRepository = settingsRepository,
            apiClient = apiClient,
            homePrintersCacheRepository = homePrintersCacheRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val uri = resolveUri(intent)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Intent received — action=$action data=${intent?.data} resolved=$uri")
        }

        if (uri == null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "No URI resolved, finishing")
            finish()
            return
        }

        lifecycleScope.launch {
            val outcome = executor.execute(uri)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "NFC result — uri=$uri outcome=$outcome")
            }
            performNfcOutcomeHaptic(window.decorView, outcome.tier)
            presentOutcome(outcome)
            finish()
        }
    }

    // ── Outcome → Toast / Navigation ──────────────────────────────

    private fun presentOutcome(outcome: NfcActionOutcome) {
        when (outcome) {
            NfcActionOutcome.MissingCredentials ->
                openSettingsWithMessage(getString(R.string.nfc_configure_first))
            else -> resolveNfcActionOutcomeMessage(this, outcome)?.let { showToast(it) }
        }
    }

    // ── Haptics ───────────────────────────────────────────────────

    // ── URI resolution ────────────────────────────────────────────

    private fun resolveUri(intent: Intent?): Uri? {
        intent ?: return null
        intent.data?.let { return it }

        @Suppress("DEPRECATION")
        val ndefMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (ndefMessages != null) {
            for (raw in ndefMessages) {
                val msg = raw as? NdefMessage ?: continue
                for (record in msg.records) {
                    val payload = record.toUri()
                    if (payload != null &&
                        payload.scheme.equals("buddydash", ignoreCase = true)
                    ) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Extracted URI from NDEF record: $payload")
                        }
                        return payload
                    }
                }
            }
        }
        return null
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun openSettingsWithMessage(message: String) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
                putExtra(MainActivity.EXTRA_STATUS_MESSAGE, message)
            },
        )
    }

    companion object {
        private const val TAG = "BuddyDash/NfcDispatch"
    }
}
