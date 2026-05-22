package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterMachineInfo
import com.chronoswing.buddydash.network.BambuddyApi

val BED_JOG_STEP_OPTIONS_MM = listOf(1f, 10f, 50f)

data class MachineTabCapabilities(
    val showMotionSection: Boolean,
    val motionEnabled: Boolean,
    val motionDisabledReason: String?,
    val showCamera: Boolean,
    val cameraEnabled: Boolean,
    val showHome: Boolean,
    val homeEnabled: Boolean,
    val showFiles: Boolean,
    val filesEnabled: Boolean,
    val showUtilitiesSection: Boolean,
    val utilitiesEnabled: Boolean,
    val utilitiesDisabledReason: String?,
)

fun PrinterDetailLabels.machineTabCapabilities(
    cameraTokenConfigured: Boolean,
): MachineTabCapabilities {
    val connected = connection != "Offline"
    val idle = activityKind == PrinterActivityKind.Idle
    val motionBlocked = !connected || !idle || motionLayout == PrinterMotionLayout.Hidden
    val motionReason = when {
        motionLayout == PrinterMotionLayout.Hidden -> "not_supported"
        !connected -> "offline"
        !idle -> "busy"
        else -> null
    }
    val utilityBlocked = !connected || !idle
    val utilityReason = when {
        !connected -> "offline"
        !idle -> "busy"
        else -> null
    }
    val showCamera = BambuddyApi.hasCameraEndpoint && cameraTokenConfigured
    val showHome = BambuddyApi.hasHomeAxesEndpoint && showMotionControls
    val showFiles = BambuddyApi.hasArchivesEndpoint
    val showUtilities = showCamera || showHome || showFiles
    return MachineTabCapabilities(
        showMotionSection = showMotionControls,
        motionEnabled = canUseMotionControls && connected && idle,
        motionDisabledReason = motionReason,
        showCamera = showCamera,
        cameraEnabled = showCamera && connected,
        showHome = showHome,
        homeEnabled = showHome && connected && idle,
        showFiles = showFiles,
        filesEnabled = showFiles,
        showUtilitiesSection = showUtilities,
        utilitiesEnabled = showUtilities && !utilityBlocked,
        utilitiesDisabledReason = utilityReason,
    )
}

fun formatWifiSignalDisplay(signalDbm: Int?, wiredNetwork: Boolean?): String? {
    if (wiredNetwork == true) return null
    return signalDbm?.let { "$it dBm" }
}

fun formatLanModeDisplay(wiredNetwork: Boolean?): String? =
    wiredNetwork?.let { wired -> if (wired) "Wired" else "Wi-Fi" }

fun formatYesNo(enabled: Boolean): String = if (enabled) "Yes" else "No"

fun formatNozzleCountDisplay(count: Int?): String? =
    count?.takeIf { it > 0 }?.let { it.toString() }

fun buildMachineInfoRows(
    labels: PrinterDetailLabels,
    machineInfo: PrinterMachineInfo?,
    printerModel: String?,
    statusUpdatedAtMillis: Long?,
): List<Pair<String, String>> = buildList {
    add("connection" to labels.connection)
    add("state" to labels.currentActivity)
    val model = machineInfo?.model ?: printerModel
    model?.takeIf { it.isNotBlank() }?.let { add("model" to it) }
    labels.firmwareLine?.let { add("firmware" to it) }

    machineInfo?.ipAddress?.let { add("ip" to it) }
    formatWifiSignalDisplay(labels.wifiSignalDbm, labels.wiredNetwork)?.let { add("wifi_signal" to it) }
    formatLanModeDisplay(labels.wiredNetwork)?.let { add("lan_mode" to it) }

    machineInfo?.serialNumber?.let { add("serial" to it) }
    formatNozzleCountDisplay(machineInfo?.nozzleCount)?.let { add("nozzle_count" to it) }
    labels.developerMode?.let { add("developer_mode" to formatYesNo(it)) }
    labels.totalPrintTimeCompact?.let { add("print_hours" to it) }
    machineInfo?.autoArchiveEnabled?.let { add("auto_archive" to formatYesNo(it)) }

}
