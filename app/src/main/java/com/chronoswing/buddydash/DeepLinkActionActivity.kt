package com.chronoswing.buddydash

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.nfc.NfcClearPlateExecutor
import com.chronoswing.buddydash.util.ClearPlateActionOutcome
import kotlinx.coroutines.launch

/**
 * Headless handler for NFC / deep-link automations.
 * Transparent theme — shows only a result toast and finishes without opening Home.
 *
 * Handles both:
 * - `ACTION_VIEW` from deep links / adb testing
 * - `NDEF_DISCOVERED` from physical NFC tag scans
 */
class DeepLinkActionActivity : ComponentActivity() {

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val homePrintersCacheRepository by lazy { HomePrintersCacheRepository(applicationContext) }
    private val apiClient by lazy { BambuddyApiClient() }
    private val clearPlateExecutor by lazy {
        NfcClearPlateExecutor(
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
            val outcome = clearPlateExecutor.execute(uri)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Clear plate result — uri=$uri outcome=$outcome")
            }
            when (outcome) {
                ClearPlateActionOutcome.Debounced -> Unit
                ClearPlateActionOutcome.InvalidLink ->
                    showToast(getString(R.string.nfc_clear_plate_invalid_link))
                ClearPlateActionOutcome.MissingCredentials ->
                    openSettingsWithMessage(getString(R.string.nfc_clear_plate_configure_first))
                ClearPlateActionOutcome.ConnectionRequired ->
                    showToast(getString(R.string.nfc_clear_plate_connection_required))
                ClearPlateActionOutcome.PrinterActive ->
                    showToast(getString(R.string.nfc_clear_plate_printer_active))
                ClearPlateActionOutcome.PrinterNotFound ->
                    showToast(getString(R.string.nfc_clear_plate_printer_not_found))
                ClearPlateActionOutcome.ApiFailed ->
                    showToast(getString(R.string.nfc_clear_plate_api_failed))
                ClearPlateActionOutcome.AlreadyCleared ->
                    showToast(getString(R.string.nfc_clear_plate_already_cleared))
                is ClearPlateActionOutcome.Success ->
                    showToast(getString(R.string.nfc_clear_plate_success, outcome.printerName))
            }
            finish()
        }
    }

    /**
     * Resolves the BuddyDash URI from the intent.
     *
     * For `ACTION_VIEW`, `intent.data` is set directly by the system.
     * For `NDEF_DISCOVERED`, `intent.data` is usually set when the NDEF record
     * contains a URI, but as a fallback we also parse the raw NDEF messages.
     */
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
        const val ACTION_CLEAR_PLATE = "com.chronoswing.buddydash.action.CLEAR_PLATE"
    }
}
