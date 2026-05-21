package com.chronoswing.buddydash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.ui.BuddyDashNav
import com.chronoswing.buddydash.ui.theme.BuddyDashTheme

class MainActivity : ComponentActivity() {

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val apiClient by lazy { BambuddyApiClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuddyDashTheme {
                BuddyDashNav(
                    settingsRepository = settingsRepository,
                    apiClient = apiClient,
                )
            }
        }
    }
}
