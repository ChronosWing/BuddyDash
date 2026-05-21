package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.PrinterStatus
import kotlin.math.roundToInt

enum class PrintSpeedMode(val apiValue: Int, val label: String) {
    Silent(1, "Silent"),
    Standard(2, "Standard"),
    Sport(3, "Sport"),
    Ludicrous(4, "Ludicrous"),
    ;

    companion object {
        fun fromApi(level: Int?): PrintSpeedMode? =
            entries.firstOrNull { it.apiValue == level }
    }
}

fun formatWifiCompact(signalDbm: Int?, wiredNetwork: Boolean?): String? {
    if (wiredNetwork == true) return "Wired"
    return signalDbm?.let { "$it dBm" }
}

fun formatDoorState(doorOpen: Boolean?): String? = doorOpen?.let { open ->
    if (open) "Open" else "Closed"
}

fun formatFanPercentCompact(percent: Int?): String? =
    percent?.takeIf { it in 0..100 }?.let { "$it%" }

fun formatPrintSpeedLevel(level: Int?): String? =
    PrintSpeedMode.fromApi(level)?.label

fun formatChamberTempCompact(tempC: Double?): String? =
    tempC?.let { "${it.roundToInt()}°" }

fun formatAmsTempCompact(tempC: Double?): String? =
    tempC?.let { "${it.roundToInt()}°" }

fun formatAmsHumidityCompact(humidityPercent: Int?): String? =
    humidityPercent?.let { "$it%" }

fun formatAmsUnitLabel(amsId: Int): String {
    val unitIndex = if (amsId >= 128) amsId - 128 else amsId
    val letter = ('A'.code + unitIndex).toChar()
    return "AMS-$letter"
}

enum class MaintenanceLineKind {
    Healthy,
    Warning,
    Due,
}

data class MaintenanceLine(
    val name: String,
    val kind: MaintenanceLineKind,
)

/** Short dashboard-style label from Bambuddy maintenance type names. */
fun shortenMaintenanceName(raw: String): String {
    var name = raw.trim()
    val isClean = name.startsWith("Clean ", ignoreCase = true)
    val isCheck = name.startsWith("Check ", ignoreCase = true)
    when {
        isClean -> {
            name = name.removePrefix("Clean ").trim()
            if (name.contains("build plate", ignoreCase = true)) {
                return "Build plate cleaning"
            }
        }
        isCheck -> name = name.removePrefix("Check ").trim()
    }
    name = name.replace("/", " / ")
    return name.split(Regex("\\s+")).joinToString(" ") { word ->
        when {
            word.equals("PTFE", ignoreCase = true) -> "PTFE"
            word.length <= 2 -> word.uppercase()
            else -> word.lowercase().replaceFirstChar { it.titlecase() }
        }
    }
}

fun maintenanceDisplayLines(items: List<MaintenanceItem>): List<MaintenanceLine> =
    items
        .filter { it.enabled }
        .map { item ->
            val kind = when {
                item.isDue -> MaintenanceLineKind.Due
                item.isWarning -> MaintenanceLineKind.Warning
                else -> MaintenanceLineKind.Healthy
            }
            MaintenanceLine(name = shortenMaintenanceName(item.name), kind = kind)
        }

fun PrinterStatus.hasConnectivitySection(): Boolean =
    formatWifiCompact(wifiSignalDbm, wiredNetwork) != null ||
        formatDoorState(doorOpen) != null ||
        !firmwareVersion.isNullOrBlank() ||
        chamberTemp != null

fun PrinterStatus.hasFansSection(): Boolean =
    partFanPercent != null || auxFanPercent != null || chamberFanPercent != null

fun PrinterStatus.hasPrintSpeedSection(): Boolean =
    formatPrintSpeedLevel(speedLevel) != null

/** Bambuddy bed-jog step size (mm). API: negative = bed up, positive = bed down. */
const val BED_JOG_STEP_MM = 10f

fun PrinterStatus.canAdjustBedWhenIdle(): Boolean =
    connected && resolveActivityKind() == PrinterActivityKind.Idle
