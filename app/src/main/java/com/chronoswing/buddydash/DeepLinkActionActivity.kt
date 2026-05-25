package com.chronoswing.buddydash

import android.content.Intent
import android.os.Bundle
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
        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            when (val outcome = clearPlateExecutor.execute(uri)) {
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
        const val ACTION_CLEAR_PLATE = "com.chronoswing.buddydash.action.CLEAR_PLATE"
    }
}
