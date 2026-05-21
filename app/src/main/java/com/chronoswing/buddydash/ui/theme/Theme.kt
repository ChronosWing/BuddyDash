package com.chronoswing.buddydash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BuddyDashDarkScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Slate950,
    primaryContainer = CyanAccentDim,
    onPrimaryContainer = TextPrimary,
    secondary = Slate800,
    onSecondary = TextPrimary,
    background = Slate950,
    onBackground = TextPrimary,
    surface = Slate900,
    onSurface = TextPrimary,
    surfaceVariant = Slate800,
    onSurfaceVariant = TextSecondary,
    error = OfflineRed,
    onError = TextPrimary,
)

@Composable
fun BuddyDashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BuddyDashDarkScheme,
        typography = Typography,
        content = content,
    )
}
