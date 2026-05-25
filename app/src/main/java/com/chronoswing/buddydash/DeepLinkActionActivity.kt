package com.chronoswing.buddydash

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.nfc.NfcActionExecutor
import com.chronoswing.buddydash.util.NfcActionOutcome
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
            performHaptic(outcome.tier)
            presentOutcome(outcome)
            finish()
        }
    }

    // ── Outcome → Toast / Navigation ──────────────────────────────

    private fun presentOutcome(outcome: NfcActionOutcome) {
        when (outcome) {
            NfcActionOutcome.Debounced -> Unit

            // clear-plate
            is NfcActionOutcome.PlateCleared ->
                showToast(getString(R.string.nfc_plate_cleared, outcome.printerName))
            NfcActionOutcome.PlateAlreadyClear ->
                showToast(getString(R.string.nfc_plate_already_clear))
            NfcActionOutcome.PrinterBusyPlateUnchanged ->
                showToast(getString(R.string.nfc_printer_busy_plate))

            // toggle-power
            is NfcActionOutcome.PowerOn ->
                showToast(getString(R.string.nfc_power_on, outcome.printerName))
            is NfcActionOutcome.PowerOff ->
                showToast(getString(R.string.nfc_power_off, outcome.printerName))
            NfcActionOutcome.PrinterBusyPowerUnchanged ->
                showToast(getString(R.string.nfc_printer_busy_power))
            NfcActionOutcome.SmartOutletUnavailable ->
                showToast(getString(R.string.nfc_outlet_unavailable))
            NfcActionOutcome.SmartOutletStateUnknown ->
                showToast(getString(R.string.nfc_outlet_state_unknown))

            // finish
            NfcActionOutcome.FinishedWithPowerOff ->
                showToast(getString(R.string.nfc_finished_power_off))
            NfcActionOutcome.FinishedPlateClear ->
                showToast(getString(R.string.nfc_finished_plate_clear))
            NfcActionOutcome.PrinterBusyFinishSkipped ->
                showToast(getString(R.string.nfc_printer_busy_finish))

            // general
            NfcActionOutcome.InvalidLink ->
                showToast(getString(R.string.nfc_invalid_link))
            NfcActionOutcome.MissingCredentials ->
                openSettingsWithMessage(getString(R.string.nfc_configure_first))
            NfcActionOutcome.ConnectionRequired ->
                showToast(getString(R.string.nfc_connection_required))
            NfcActionOutcome.PrinterNotFound ->
                showToast(getString(R.string.nfc_printer_not_found))
            NfcActionOutcome.ApiFailed ->
                showToast(getString(R.string.nfc_action_failed))
        }
    }

    // ── Haptics ───────────────────────────────────────────────────

    private fun performHaptic(tier: NfcActionOutcome.Tier) {
        if (tier == NfcActionOutcome.Tier.Noop && tier == NfcActionOutcome.Tier.Noop) {
            // Debounced gets the lightest possible tick
        }
        val vibrator = getVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (tier) {
                NfcActionOutcome.Tier.Success ->
                    VibrationEffect.createOneShot(35, 80)
                NfcActionOutcome.Tier.Noop ->
                    VibrationEffect.createOneShot(20, 40)
                NfcActionOutcome.Tier.Warning ->
                    VibrationEffect.createOneShot(50, 140)
                NfcActionOutcome.Tier.Failure ->
                    VibrationEffect.createOneShot(60, 180)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val ms = when (tier) {
                NfcActionOutcome.Tier.Success -> 35L
                NfcActionOutcome.Tier.Noop -> 20L
                NfcActionOutcome.Tier.Warning -> 50L
                NfcActionOutcome.Tier.Failure -> 60L
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
    }

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
