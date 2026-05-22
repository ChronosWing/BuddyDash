package com.chronoswing.buddydash

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chronoswing.buddydash.data.ArchivesCacheRepository
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.PrinterDetailCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.SpoolDetailCacheRepository
import com.chronoswing.buddydash.data.SpoolsCacheRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.ui.BuddyDashNav
import com.chronoswing.buddydash.ui.theme.BuddyDashTheme

class MainActivity : ComponentActivity() {

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val homePrintersCacheRepository by lazy { HomePrintersCacheRepository(applicationContext) }
    private val spoolsCacheRepository by lazy { SpoolsCacheRepository(applicationContext) }
    private val archivesCacheRepository by lazy { ArchivesCacheRepository(applicationContext) }
    private val printerDetailCacheRepository by lazy { PrinterDetailCacheRepository(applicationContext) }
    private val spoolDetailCacheRepository by lazy { SpoolDetailCacheRepository(applicationContext) }
    private val apiClient by lazy { BambuddyApiClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            BuddyDashTheme {
                BuddyDashNav(
                    settingsRepository = settingsRepository,
                    homePrintersCacheRepository = homePrintersCacheRepository,
                    spoolsCacheRepository = spoolsCacheRepository,
                    archivesCacheRepository = archivesCacheRepository,
                    printerDetailCacheRepository = printerDetailCacheRepository,
                    spoolDetailCacheRepository = spoolDetailCacheRepository,
                    apiClient = apiClient,
                )
            }
        }
    }
}
