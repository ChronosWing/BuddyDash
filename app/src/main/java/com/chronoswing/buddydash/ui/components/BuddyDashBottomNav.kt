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
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
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
            selected = currentRoute == Routes.SPOOLS,
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
            selected = currentRoute == Routes.ARCHIVES,
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
            selected = currentRoute == Routes.SETTINGS,
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
