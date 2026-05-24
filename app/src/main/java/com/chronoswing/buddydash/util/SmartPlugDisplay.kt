package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.data.model.SmartOutletPowerState
import com.chronoswing.buddydash.data.model.SmartPlugEnergyReading
import java.util.Locale
import kotlin.math.roundToInt

fun SmartOutletPowerState.displayLabel(): String = when (this) {
    SmartOutletPowerState.On -> "On"
    SmartOutletPowerState.Off -> "Off"
    SmartOutletPowerState.Unknown -> "Unknown"
}

fun formatSmartPlugPowerWatts(reading: SmartPlugEnergyReading?): String? {
    val watts = reading?.powerWatts ?: return null
    if (watts.isNaN() || watts < 0) return null
    return "${watts.roundToInt()} W"
}

fun formatSmartPlugVoltage(reading: SmartPlugEnergyReading?): String? {
    val volts = reading?.voltageVolts ?: return null
    if (volts.isNaN() || volts <= 0) return null
    return String.format(Locale.US, "%.1f V", volts)
}

fun formatSmartPlugCurrent(reading: SmartPlugEnergyReading?): String? {
    val amps = reading?.currentAmps ?: return null
    if (amps.isNaN() || amps < 0) return null
    return String.format(Locale.US, "%.2f A", amps)
}

/** True when cutting power could interrupt an active print or motion. */
fun PrinterStatus?.requiresActivePowerOffConfirmation(): Boolean {
    if (this == null || !connected) return false
    return when (resolveActivityKind()) {
        PrinterActivityKind.Printing,
        PrinterActivityKind.Paused,
        PrinterActivityKind.Busy,
        -> true
        else -> {
            val raw = rawState?.uppercase().orEmpty()
            raw.contains("UNLOAD") ||
                raw.contains("LOAD") ||
                raw.contains("HEAT") ||
                raw.contains("COOL")
        }
    }
}
