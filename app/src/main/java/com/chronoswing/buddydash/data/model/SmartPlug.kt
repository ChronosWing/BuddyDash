package com.chronoswing.buddydash.data.model

/** Config row from GET /api/v1/smart-plugs/by-printer/{printer_id}. */
data class SmartPlugConfig(
    val id: Int,
    val name: String,
    val lastState: String? = null,
    val lastCheckedIso: String? = null,
)

enum class SmartOutletPowerState {
    On,
    Off,
    Unknown,
}

/** Energy monitoring from SmartPlugEnergy schema. */
data class SmartPlugEnergyReading(
    val powerWatts: Double? = null,
    val voltageVolts: Double? = null,
    val currentAmps: Double? = null,
)

/** Live status from GET /api/v1/smart-plugs/{plug_id}/status. */
data class SmartPlugLiveStatus(
    val powerState: SmartOutletPowerState,
    val reachable: Boolean = true,
    val deviceName: String? = null,
    val energy: SmartPlugEnergyReading? = null,
)

/** Smart outlet assigned to a printer, with optional live telemetry. */
data class PrinterSmartPlugState(
    val config: SmartPlugConfig,
    val liveStatus: SmartPlugLiveStatus? = null,
    val lastUpdatedAtMillis: Long? = null,
) {
    val displayPowerState: SmartOutletPowerState
        get() = liveStatus?.powerState
            ?: parseSmartOutletPowerState(config.lastState)

    val energy: SmartPlugEnergyReading?
        get() = liveStatus?.energy
}

fun parseSmartOutletPowerState(raw: String?): SmartOutletPowerState {
    when (raw?.trim()?.lowercase()) {
        "on", "true", "1" -> return SmartOutletPowerState.On
        "off", "false", "0" -> return SmartOutletPowerState.Off
    }
    return SmartOutletPowerState.Unknown
}
