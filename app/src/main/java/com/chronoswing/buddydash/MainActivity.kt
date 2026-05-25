package com.chronoswing.buddydash

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.chronoswing.buddydash.data.ArchivesCacheRepository
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.PrinterCardPrefsRepository
import com.chronoswing.buddydash.data.PrinterDetailCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.SpoolDetailCacheRepository
import com.chronoswing.buddydash.data.SpoolsCacheRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.ui.BuddyDashNav
import com.chronoswing.buddydash.ui.theme.BuddyDashTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_SETTINGS = "com.chronoswing.buddydash.extra.OPEN_SETTINGS"
        const val EXTRA_STATUS_MESSAGE = "com.chronoswing.buddydash.extra.STATUS_MESSAGE"
    }

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val homePrintersCacheRepository by lazy { HomePrintersCacheRepository(applicationContext) }
    private val spoolsCacheRepository by lazy { SpoolsCacheRepository(applicationContext) }
    private val archivesCacheRepository by lazy { ArchivesCacheRepository(applicationContext) }
    private val printerDetailCacheRepository by lazy { PrinterDetailCacheRepository(applicationContext) }
    private val spoolDetailCacheRepository by lazy { SpoolDetailCacheRepository(applicationContext) }
    private val printerCardPrefsRepository by lazy { PrinterCardPrefsRepository(applicationContext) }
    private val apiClient by lazy { BambuddyApiClient() }
    private var settingsNavigationNonce by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        consumeNfcSettingsIntent(intent)
        setContent {
            BuddyDashTheme {
                BuddyDashNav(
                    settingsRepository = settingsRepository,
                    homePrintersCacheRepository = homePrintersCacheRepository,
                    spoolsCacheRepository = spoolsCacheRepository,
                    archivesCacheRepository = archivesCacheRepository,
                    printerDetailCacheRepository = printerDetailCacheRepository,
                    spoolDetailCacheRepository = spoolDetailCacheRepository,
                    printerCardPrefsRepository = printerCardPrefsRepository,
                    apiClient = apiClient,
                    settingsNavigationNonce = settingsNavigationNonce,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNfcSettingsIntent(intent)
    }

    private fun consumeNfcSettingsIntent(intent: Intent?) {
        val message = intent?.getStringExtra(EXTRA_STATUS_MESSAGE)
        if (!message.isNullOrBlank()) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true) {
            settingsNavigationNonce++
        }
    }
}
