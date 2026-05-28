package com.chronoswing.buddydash.data

import com.chronoswing.buddydash.data.model.PrinterSmartPlugState
import com.chronoswing.buddydash.data.model.SmartOutletPowerState
import com.chronoswing.buddydash.data.model.SmartPlugConfig
import com.chronoswing.buddydash.data.model.SmartPlugEnergyReading
import com.chronoswing.buddydash.data.model.SmartPlugLiveStatus
import org.json.JSONObject

/** JSON encode/decode for [PrinterSmartPlugState] in Home and detail caches. */
object SmartPlugCacheCodec {

    fun encode(state: PrinterSmartPlugState): JSONObject =
        JSONObject()
            .put("config", encodeConfig(state.config))
            .putOptObject("live_status", state.liveStatus?.let { encodeLiveStatus(it) })
            .putOptLong("last_updated_at_millis", state.lastUpdatedAtMillis)

    fun decode(obj: JSONObject?): PrinterSmartPlugState? {
        if (obj == null) return null
        val configObj = obj.optJSONObject("config") ?: return null
        return PrinterSmartPlugState(
            config = decodeConfig(configObj),
            liveStatus = obj.optJSONObject("live_status")?.let { decodeLiveStatus(it) },
            lastUpdatedAtMillis = obj.optNullableLong("last_updated_at_millis"),
        )
    }

    private fun encodeConfig(config: SmartPlugConfig): JSONObject =
        JSONObject()
            .put("id", config.id)
            .put("name", config.name)
            .putOptString("last_state", config.lastState)
            .putOptString("last_checked_iso", config.lastCheckedIso)

    private fun decodeConfig(obj: JSONObject): SmartPlugConfig =
        SmartPlugConfig(
            id = obj.optInt("id", 0),
            name = obj.optString("name", "Smart plug"),
            lastState = obj.optString("last_state").takeIf { it.isNotBlank() },
            lastCheckedIso = obj.optString("last_checked_iso").takeIf { it.isNotBlank() },
        )

    private fun encodeLiveStatus(status: SmartPlugLiveStatus): JSONObject =
        JSONObject()
            .put("power_state", status.powerState.name)
            .put("reachable", status.reachable)
            .putOptString("device_name", status.deviceName)
            .putOptObject("energy", status.energy?.let { encodeEnergy(it) })

    private fun decodeLiveStatus(obj: JSONObject): SmartPlugLiveStatus {
        val powerState = runCatching {
            SmartOutletPowerState.valueOf(
                obj.optString("power_state", SmartOutletPowerState.Unknown.name),
            )
        }.getOrDefault(SmartOutletPowerState.Unknown)
        return SmartPlugLiveStatus(
            powerState = powerState,
            reachable = obj.optBoolean("reachable", true),
            deviceName = obj.optString("device_name").takeIf { it.isNotBlank() },
            energy = obj.optJSONObject("energy")?.let { decodeEnergy(it) },
        )
    }

    private fun encodeEnergy(energy: SmartPlugEnergyReading): JSONObject =
        JSONObject()
            .putOptDouble("power_watts", energy.powerWatts)
            .putOptDouble("voltage_volts", energy.voltageVolts)
            .putOptDouble("current_amps", energy.currentAmps)
            .putOptDouble("power_factor", energy.powerFactor)

    private fun decodeEnergy(obj: JSONObject): SmartPlugEnergyReading =
        SmartPlugEnergyReading(
            powerWatts = obj.optNullableDouble("power_watts"),
            voltageVolts = obj.optNullableDouble("voltage_volts"),
            currentAmps = obj.optNullableDouble("current_amps"),
            powerFactor = obj.optNullableDouble("power_factor"),
        )

    private fun JSONObject.putOptString(key: String, value: String?): JSONObject =
        apply { if (value != null) put(key, value) }

    private fun JSONObject.putOptObject(key: String, value: JSONObject?): JSONObject =
        apply { if (value != null) put(key, value) }

    private fun JSONObject.putOptLong(key: String, value: Long?): JSONObject =
        apply { if (value != null) put(key, value) }

    private fun JSONObject.putOptDouble(key: String, value: Double?): JSONObject =
        apply { if (value != null && !value.isNaN()) put(key, value) }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key)
        return if (v.isNaN()) null else v
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val v = optLong(key, 0L)
        return v.takeIf { it > 0L }
    }
}
