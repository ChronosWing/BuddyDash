package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi
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

enum class WifiSignalLevel {
    Strong,
    Good,
    Weak,
}

/** dBm-based Wi-Fi cue; null for wired or missing signal (no fake styling). */
fun resolveWifiSignalLevel(signalDbm: Int?, wiredNetwork: Boolean?): WifiSignalLevel? {
    if (wiredNetwork == true || signalDbm == null) return null
    return when {
        signalDbm >= -55 -> WifiSignalLevel.Strong
        signalDbm >= -71 -> WifiSignalLevel.Good
        else -> WifiSignalLevel.Weak
    }
}

enum class MaintenanceHomeIndicator {
    None,
    DueSoon,
    Due,
}

/** Home card maintenance cue from API `is_warning` / `is_due` (not color alone). */
fun resolveMaintenanceHomeIndicator(items: List<MaintenanceItem>): MaintenanceHomeIndicator {
    val enabled = items.filter { it.enabled }
    if (enabled.any { it.isDue }) return MaintenanceHomeIndicator.Due
    if (enabled.any { it.isWarning }) return MaintenanceHomeIndicator.DueSoon
    return MaintenanceHomeIndicator.None
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

/** Compact lifetime print time from Bambuddy `total_print_hours`, e.g. 3h 12m or 2d 4h. */
fun formatTotalPrintTimeCompact(hours: Double?): String? {
    if (hours == null || hours <= 0.0) return null
    val totalMinutes = (hours * 60.0).roundToInt()
    if (totalMinutes < 1) return null
    val days = totalMinutes / (24 * 60)
    val hoursPart = (totalMinutes % (24 * 60)) / 60
    val minutesPart = totalMinutes % 60
    return when {
        days > 0 -> if (hoursPart > 0) "${days}d ${hoursPart}h" else "${days}d"
        hoursPart > 0 -> if (minutesPart > 0) "${hoursPart}h ${minutesPart}m" else "${hoursPart}h"
        else -> "${minutesPart}m"
    }
}

/** Parse API nozzle_diameter string to display form, e.g. "0.4 mm". */
fun formatNozzleDiameterDisplay(raw: String?): String? {
    val cleaned = raw?.trim()?.removeSuffix("mm")?.trim() ?: return null
    if (cleaned.isBlank()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    if (value <= 0.0) return null
    val numeric = if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        "%.2f".format(value).trimEnd('0').trimEnd('.')
    }
    return "$numeric mm"
}

fun formatAmsTempCompact(tempC: Double?): String? =
    tempC?.let { "${it.roundToInt()}°" }

fun formatAmsHumidityCompact(humidityPercent: Int?): String? =
    humidityPercent?.let { "$it%" }

fun formatAmsUnitLabel(amsId: Int): String {
    val unitIndex = if (amsId >= 128) amsId - 128 else amsId
    val letter = ('A'.code + unitIndex).toChar()
    return "AMS-$letter"
}

private const val DEBUG_LOG_MAINTENANCE = true
private const val TAG_MAINTENANCE = "BuddyDash/Maintenance"

enum class MaintenanceLineKind {
    Ok,
    DueSoon,
    Due,
}

data class MaintenanceLine(
    val itemId: Int,
    val name: String,
    val kind: MaintenanceLineKind,
    val remainingText: String? = null,
    val progressFraction: Float? = null,
    val canReset: Boolean = false,
)

/** Reset only when Bambuddy marks the item due (`is_due`), not due-soon (`is_warning`). */
fun canResetMaintenanceItem(item: MaintenanceItem): Boolean =
    BambuddyApi.hasMaintenancePerformEndpoint &&
        item.id > 0 &&
        item.enabled &&
        item.isDue

fun resolveMaintenanceLineKind(item: MaintenanceItem): MaintenanceLineKind = when {
    item.isDue -> MaintenanceLineKind.Due
    item.isWarning -> MaintenanceLineKind.DueSoon
    else -> MaintenanceLineKind.Ok
}

/** Short dashboard-style label from Bambuddy maintenance type names. */
fun shortenMaintenanceName(raw: String): String {
    val normalized = raw.trim().replace("/", " / ")
    when {
        normalized.contains("build plate", ignoreCase = true) -> return "Clean build plate"
        normalized.contains("carbon rod", ignoreCase = true) -> return "Carbon rods"
        normalized.contains("nozzle", ignoreCase = true) && normalized.contains("hotend", ignoreCase = true) ->
            return "Nozzle / hotend"
        normalized.contains("belt tension", ignoreCase = true) -> return "Belt tension"
        normalized.contains("ptfe", ignoreCase = true) -> return "PTFE tube"
    }
    var name = normalized
    if (name.startsWith("Clean ", ignoreCase = true)) {
        name = name.removePrefix("Clean ").trim()
    }
    if (name.startsWith("Check ", ignoreCase = true)) {
        name = name.removePrefix("Check ").trim()
    }
    return name.split(Regex("\\s+")).joinToString(" ") { word ->
        when {
            word.equals("PTFE", ignoreCase = true) -> "PTFE"
            word.length <= 2 -> word.uppercase()
            else -> word.lowercase().replaceFirstChar { it.titlecase() }
        }
    }
}

/** Compact remaining label from API `hours_until_due` / `days_until_due` only. */
fun formatMaintenanceRemainingText(item: MaintenanceItem, kind: MaintenanceLineKind): String? {
    if (kind == MaintenanceLineKind.Due) return "Due"
    val hours = maintenanceHoursUntilDue(item) ?: return null
    if (hours <= 0.0) return null
    val duration = formatMaintenanceDuration(hours) ?: return null
    return when (kind) {
        MaintenanceLineKind.DueSoon -> "Due in $duration"
        MaintenanceLineKind.Ok -> "$duration left"
        MaintenanceLineKind.Due -> "Due"
    }
}

private fun maintenanceHoursUntilDue(item: MaintenanceItem): Double? {
    val hours = item.hoursUntilDue
    if (hours != null && !hours.isNaN()) return hours
    val days = item.daysUntilDue
    if (days != null && !days.isNaN()) return days * 24.0
    return null
}

/** e.g. 49m, 3d, 1.0w — from hours until due. */
fun formatMaintenanceDuration(hours: Double): String? {
    if (hours.isNaN() || hours <= 0.0) return null
    return when {
        hours >= 168.0 -> {
            val weeks = hours / 168.0
            val numeric = when {
                weeks >= 10.0 -> weeks.roundToInt().toString()
                weeks >= 2.0 && kotlin.math.abs(weeks - weeks.roundToInt()) < 0.15 ->
                    weeks.roundToInt().toString()
                else -> "%.1f".format(weeks)
            }
            "${numeric}w"
        }
        hours >= 24.0 -> "${(hours / 24.0).roundToInt()}d"
        hours >= 1.0 -> "${hours.roundToInt()}h"
        else -> "${(hours * 60.0).roundToInt().coerceAtLeast(1)}m"
    }
}

/** Thin micro-bar progress from `hours_since_maintenance` / `interval_hours`. */
fun maintenanceProgressFraction(item: MaintenanceItem, kind: MaintenanceLineKind): Float? {
    val interval = item.intervalHours ?: return null
    if (interval <= 0.0) return null
    val since = item.hoursSinceMaintenance ?: return null
    val fraction = (since / interval).toFloat().coerceIn(0f, 1f)
    return when (kind) {
        MaintenanceLineKind.Due,
        MaintenanceLineKind.DueSoon,
        -> fraction
        MaintenanceLineKind.Ok -> fraction.takeIf { it >= 0.35f }
    }
}

fun maintenanceDisplayLines(items: List<MaintenanceItem>): List<MaintenanceLine> =
    items
        .filter { it.enabled }
        .map { item ->
            val kind = resolveMaintenanceLineKind(item)
            val canReset = canResetMaintenanceItem(item)
            val remainingText = formatMaintenanceRemainingText(item, kind)
            val progressFraction = maintenanceProgressFraction(item, kind)
            val line = MaintenanceLine(
                itemId = item.id,
                name = shortenMaintenanceName(item.name),
                kind = kind,
                remainingText = remainingText,
                progressFraction = progressFraction,
                canReset = canReset,
            )
            if (DEBUG_LOG_MAINTENANCE) {
                Log.d(
                    TAG_MAINTENANCE,
                    "item name=${item.name} is_due=${item.isDue} is_warning=${item.isWarning} " +
                        "hours_until_due=${item.hoursUntilDue} days_until_due=${item.daysUntilDue} " +
                        "interval_hours=${item.intervalHours} hours_since=${item.hoursSinceMaintenance} " +
                        "kind=$kind resettable=$canReset remaining=\"$remainingText\" progress=$progressFraction",
                )
            }
            line
        }

fun PrinterStatus.hasPrintSpeedSection(): Boolean =
    formatPrintSpeedLevel(speedLevel) != null

/** Bambuddy bed-jog step size (mm). API: negative = bed up, positive = bed down. */
const val BED_JOG_STEP_MM = 10f

fun PrinterStatus.canAdjustBedWhenIdle(): Boolean =
    connected && resolveActivityKind() == PrinterActivityKind.Idle
