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
    val showUtilities = showCamera || showHome
    return MachineTabCapabilities(
        showMotionSection = showMotionControls,
        motionEnabled = canUseMotionControls && connected && idle,
        motionDisabledReason = motionReason,
        showCamera = showCamera,
        cameraEnabled = showCamera && connected,
        showHome = showHome,
        homeEnabled = showHome && connected && idle,
        showUtilitiesSection = showUtilities,
        utilitiesEnabled = showUtilities && !utilityBlocked,
        utilitiesDisabledReason = utilityReason,
    )
}

fun formatMachineInfoUpdatedAt(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return formatSpoolLastUsed(iso)
}

fun buildMachineInfoRows(
    labels: PrinterDetailLabels,
    machineInfo: PrinterMachineInfo?,
    printerModel: String?,
    statusUpdatedAtMillis: Long?,
): List<Pair<String, String>> = buildList {
    add(labels.connection.let { "connection" to it })
    machineInfo?.ipAddress?.let { add("ip" to it) }
    labels.wifiCompact?.let { add("network" to it) }
    labels.firmwareLine?.let { add("firmware" to it) }
    val model = machineInfo?.model ?: printerModel
    model?.takeIf { it.isNotBlank() }?.let { add("model" to it) }
    machineInfo?.serialNumber?.let { add("serial" to it) }
    machineInfo?.location?.takeIf { it.isNotBlank() }?.let { add("location" to it) }
    formatMachineInfoUpdatedAt(machineInfo?.updatedAtIso)?.let { add("updated" to it) }
    statusUpdatedAtMillis?.let { millis ->
        formatRelativeStatusUpdated(millis)?.let { add("status_updated" to it) }
    }
}

private fun formatRelativeStatusUpdated(epochMillis: Long): String? {
    val deltaSeconds = ((System.currentTimeMillis() - epochMillis) / 1000).coerceAtLeast(0)
    return when {
        deltaSeconds < 60 -> "${deltaSeconds}s ago"
        deltaSeconds < 3600 -> "${deltaSeconds / 60}m ago"
        deltaSeconds < 86400 -> "${deltaSeconds / 3600}h ago"
        else -> "${deltaSeconds / 86400}d ago"
    }
}
