package com.chronoswing.buddydash.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.chronoswing.buddydash.ui.theme.OfflineRed
import com.chronoswing.buddydash.ui.theme.OnlineGreen
import com.chronoswing.buddydash.util.WifiSignalLevel

private val WifiFairTint = Color(0xFFFBBF24)

data class WifiSignalStyle(
    val iconTint: Color,
    val valueTint: Color,
)

@Composable
fun wifiSignalStyle(level: WifiSignalLevel?): WifiSignalStyle? {
    if (level == null) return null
    return when (level) {
        WifiSignalLevel.Strong -> WifiSignalStyle(
            iconTint = OnlineGreen.copy(alpha = 0.82f),
            valueTint = OnlineGreen.copy(alpha = 0.9f),
        )
        WifiSignalLevel.Good -> WifiSignalStyle(
            iconTint = WifiFairTint.copy(alpha = 0.78f),
            valueTint = WifiFairTint.copy(alpha = 0.88f),
        )
        WifiSignalLevel.Weak -> WifiSignalStyle(
            iconTint = OfflineRed.copy(alpha = 0.82f),
            valueTint = OfflineRed.copy(alpha = 0.9f),
        )
    }
}
