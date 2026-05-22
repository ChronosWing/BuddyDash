package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi

import com.chronoswing.buddydash.util.BuddyDashDebug

private val debugLogCapabilities: Boolean get() = BuddyDashDebug.enabled
private const val TAG_CAPABILITIES = "BuddyDash/Capabilities"

private fun logCapabilities(message: String) {
    if (!debugLogCapabilities) return
    try {
        Log.d(TAG_CAPABILITIES, message)
    } catch (_: RuntimeException) {
        // android.util.Log is not available in JVM unit tests
    }
}

/** Hardware expectations from printer model (API still gates actual values). */
data class PrinterHardwareProfile(
    val modelNormalized: String,
    val isBedSlinger: Boolean,
    val hasEnclosureDoor: Boolean,
    val hasChamberFan: Boolean,
    val hasChamberTempSensor: Boolean,
)

data class PrinterDisplayCapabilities(
    val doorLine: String?,
    val chamberTempCompact: String?,
    val partFanPercent: Int?,
    val auxFanPercent: Int?,
    val chamberFanPercent: Int?,
    val amsUnits: List<AmsUnitInfo>,
    val showConnectivitySection: Boolean,
    val showFansSection: Boolean,
    val canToggleChamberLight: Boolean,
)

fun resolvePrinterHardwareProfile(printerModel: String?): PrinterHardwareProfile {
    val model = printerModel?.trim()?.uppercase().orEmpty()
    val bedSlinger = isBedSlingerModel(model)
    val openP1 = isOpenFrameP1(model)
    val enclosed = !bedSlinger && !openP1 && (
        model.contains("P1S") ||
            model.contains("X1") ||
            model.contains("H2") ||
            model.contains("ENCLOSED")
    )
    val hasDoor = when {
        bedSlinger || openP1 -> false
        enclosed -> true
        else -> false
    }
    val hasChamberFan = when {
        bedSlinger || openP1 -> false
        enclosed -> true
        else -> false
    }
    val hasChamberTemp = when {
        bedSlinger -> false
        enclosed -> true
        else -> false
    }
    return PrinterHardwareProfile(
        modelNormalized = model,
        isBedSlinger = bedSlinger,
        hasEnclosureDoor = hasDoor,
        hasChamberFan = hasChamberFan,
        hasChamberTempSensor = hasChamberTemp,
    )
}

fun isBedSlingerModel(modelUpper: String): Boolean {
    if (modelUpper.isEmpty()) return false
    return modelUpper.contains("A1 MINI") ||
        modelUpper.contains("A1MINI") ||
        modelUpper == "A1" ||
        modelUpper.startsWith("A1 ") ||
        modelUpper.contains("A1 ")
}

private fun isOpenFrameP1(modelUpper: String): Boolean {
    if (modelUpper.isEmpty()) return false
    if (modelUpper.contains("P1S")) return false
    return modelUpper == "P1" ||
        modelUpper.startsWith("P1 ") ||
        (modelUpper.contains("P1") && !modelUpper.contains("P1S"))
}

fun isAmsLiteModule(moduleType: String?): Boolean {
    val type = moduleType?.trim()?.uppercase().orEmpty()
    if (type.isEmpty()) return false
    return type.contains("LITE") || type == "AMS_LITE" || type == "AMSLITE"
}

fun isMeaningfulAmsTemp(tempC: Double?, isAmsLite: Boolean): Boolean {
    if (tempC == null) return false
    if (isAmsLite && tempC <= 0.0) return false
    return true
}

fun isMeaningfulAmsHumidity(humidityPercent: Int?, isAmsLite: Boolean): Boolean {
    if (humidityPercent == null) return false
    if (isAmsLite && humidityPercent <= 0) return false
    return humidityPercent in 1..100
}

fun filterAmsUnitsForDisplay(units: List<AmsUnitInfo>): List<AmsUnitInfo> =
    units.mapNotNull { unit ->
        val lite = unit.isAmsLite
        val temp = unit.tempC.takeIf { isMeaningfulAmsTemp(it, lite) }
        val humidity = unit.humidityPercent.takeIf { isMeaningfulAmsHumidity(it, lite) }
        if (temp == null && humidity == null) return@mapNotNull null
        if (temp == unit.tempC && humidity == unit.humidityPercent) unit
        else unit.copy(tempC = temp, humidityPercent = humidity)
    }

private fun shouldShowDoor(profile: PrinterHardwareProfile, doorOpen: Boolean?): Boolean {
    if (doorOpen == null) return false
    if (profile.modelNormalized.isEmpty()) return true
    return profile.hasEnclosureDoor
}

private fun shouldShowChamberTemp(profile: PrinterHardwareProfile, chamberTemp: Double?): Boolean {
    if (chamberTemp == null || chamberTemp <= 0.0) return false
    if (profile.modelNormalized.isEmpty()) return true
    return profile.hasChamberTempSensor
}

private fun shouldShowChamberFan(profile: PrinterHardwareProfile, percent: Int?): Boolean {
    if (percent == null) return false
    if (profile.modelNormalized.isEmpty()) return true
    return profile.hasChamberFan
}

fun applyDisplayCapabilities(
    status: PrinterStatus,
    printerModel: String?,
    totalPrintHours: Double? = null,
): PrinterDisplayCapabilities {
    val profile = resolvePrinterHardwareProfile(printerModel)

    val doorLine = if (shouldShowDoor(profile, status.doorOpen)) {
        formatDoorState(status.doorOpen)
    } else {
        null
    }

    val chamberTempCompact = if (shouldShowChamberTemp(profile, status.chamberTemp)) {
        formatChamberTempCompact(status.chamberTemp)
    } else {
        null
    }

    val partFanPercent = status.partFanPercent
    val auxFanPercent = status.auxFanPercent
    val chamberFanPercent = if (shouldShowChamberFan(profile, status.chamberFanPercent)) {
        status.chamberFanPercent
    } else {
        null
    }

    val amsUnits = filterAmsUnitsForDisplay(status.amsUnits)

    val wifiCompact = formatWifiCompact(status.wifiSignalDbm, status.wiredNetwork)
    val firmwareLine = status.firmwareVersion?.takeIf { it.isNotBlank() }
    val totalPrintTimeCompact = formatTotalPrintTimeCompact(totalPrintHours)
    val nozzleDiameterCompact = status.nozzleDiameterDisplay?.takeIf { it.isNotBlank() }

    val showConnectivitySection =
        wifiCompact != null ||
            doorLine != null ||
            !firmwareLine.isNullOrBlank() ||
            chamberTempCompact != null ||
            totalPrintTimeCompact != null ||
            !nozzleDiameterCompact.isNullOrBlank()

    val showFansSection =
        partFanPercent != null || auxFanPercent != null || chamberFanPercent != null

    val canToggleChamberLight =
        status.connected &&
            status.chamberLightOn != null &&
            BambuddyApi.hasChamberLightEndpoint

    logCapabilities(
        "model=${printerModel?.trim().orEmpty().ifEmpty { "(unknown)" }} profile=$profile " +
            "doorRaw=${status.doorOpen} showDoor=${doorLine != null} " +
            "chamberTempRaw=${status.chamberTemp} showChamberTemp=${chamberTempCompact != null} " +
            "fansRaw=(${status.partFanPercent},${status.auxFanPercent},${status.chamberFanPercent}) " +
            "showFans=($partFanPercent,$auxFanPercent,$chamberFanPercent) " +
            "amsUnits=${status.amsUnits.size} amsEnvUnits=${amsUnits.size} " +
            "amsTypes=${status.amsUnits.map { it.moduleType }} " +
            "showConnectivity=$showConnectivitySection showChamberLight=$canToggleChamberLight",
    )

    return PrinterDisplayCapabilities(
        doorLine = doorLine,
        chamberTempCompact = chamberTempCompact,
        partFanPercent = partFanPercent,
        auxFanPercent = auxFanPercent,
        chamberFanPercent = chamberFanPercent,
        amsUnits = amsUnits,
        showConnectivitySection = showConnectivitySection,
        showFansSection = showFansSection,
        canToggleChamberLight = canToggleChamberLight,
    )
}
