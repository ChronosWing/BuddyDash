package com.chronoswing.buddydash.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.BottomNavTab
import com.chronoswing.buddydash.ui.Routes

@Composable
fun BuddyDashBottomNav(
    currentRoute: String?,
    onPrinters: () -> Unit,
    onSpools: () -> Unit,
    onArchives: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTab = Routes.bottomNavTab(currentRoute)
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.PRINTERS,
            onClick = onPrinters,
            icon = {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = stringResource(R.string.nav_printers),
                )
            },
            label = { Text(stringResource(R.string.nav_printers)) },
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.SPOOLS,
            onClick = onSpools,
            icon = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = stringResource(R.string.nav_spools),
                )
            },
            label = { Text(stringResource(R.string.nav_spools)) },
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.ARCHIVES,
            onClick = onArchives,
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.nav_archives),
                )
            },
            label = { Text(stringResource(R.string.nav_archives)) },
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.SETTINGS,
            onClick = onSettings,
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                )
            },
            label = { Text(stringResource(R.string.nav_settings)) },
        )
    }
}
