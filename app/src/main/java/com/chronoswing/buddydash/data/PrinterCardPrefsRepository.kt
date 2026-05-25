package com.chronoswing.buddydash.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chronoswing.buddydash.util.BuddyDashDebug
import kotlinx.coroutines.flow.first
import org.json.JSONObject

private val Context.printerCardPrefsDataStore by preferencesDataStore(
    name = "buddydash_printer_card_prefs",
)

private const val TAG = "BuddyDash/CardPrefs"

data class PrinterCardVisibility(
    val showCameraPreview: Boolean = true,
    val showPowerChip: Boolean = true,
    val showHmsChip: Boolean = true,
    val showMaintenanceChip: Boolean = true,
    val showTemperatures: Boolean = true,
)

data class MaintenanceSnooze(
    val snoozedUntilMillis: Long,
)

class PrinterCardPrefsRepository(private val context: Context) {

    suspend fun loadVisibility(printerId: Int): PrinterCardVisibility {
        val key = stringPreferencesKey("visibility_$printerId")
        val json = context.printerCardPrefsDataStore.data.first()[key] ?: return PrinterCardVisibility()
        return decodeVisibility(json)
    }

    suspend fun saveVisibility(printerId: Int, visibility: PrinterCardVisibility) {
        val key = stringPreferencesKey("visibility_$printerId")
        context.printerCardPrefsDataStore.edit { prefs ->
            prefs[key] = encodeVisibility(visibility)
        }
    }

    suspend fun loadMaintenanceSnooze(printerId: Int, itemId: Int): MaintenanceSnooze? {
        val key = stringPreferencesKey("maint_snooze_${printerId}_$itemId")
        val json = context.printerCardPrefsDataStore.data.first()[key] ?: return null
        return decodeSnooze(json)
    }

    suspend fun saveMaintenanceSnooze(printerId: Int, itemId: Int, snooze: MaintenanceSnooze) {
        val key = stringPreferencesKey("maint_snooze_${printerId}_$itemId")
        context.printerCardPrefsDataStore.edit { prefs ->
            prefs[key] = encodeSnooze(snooze)
        }
    }

    suspend fun clearMaintenanceSnooze(printerId: Int, itemId: Int) {
        val key = stringPreferencesKey("maint_snooze_${printerId}_$itemId")
        context.printerCardPrefsDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    suspend fun loadAllMaintenanceSnoozes(printerId: Int, itemIds: List<Int>): Map<Int, MaintenanceSnooze> {
        val prefs = context.printerCardPrefsDataStore.data.first()
        val now = System.currentTimeMillis()
        val result = mutableMapOf<Int, MaintenanceSnooze>()
        for (itemId in itemIds) {
            val key = stringPreferencesKey("maint_snooze_${printerId}_$itemId")
            val json = prefs[key] ?: continue
            val snooze = decodeSnooze(json) ?: continue
            if (snooze.snoozedUntilMillis > now) {
                result[itemId] = snooze
            }
        }
        return result
    }
}

private fun encodeVisibility(v: PrinterCardVisibility): String = try {
    JSONObject().apply {
        put("camera", v.showCameraPreview)
        put("power", v.showPowerChip)
        put("hms", v.showHmsChip)
        put("maint", v.showMaintenanceChip)
        put("temps", v.showTemperatures)
    }.toString()
} catch (e: Exception) {
    if (BuddyDashDebug.enabled) Log.w(TAG, "encodeVisibility failed", e)
    "{}"
}

private fun decodeVisibility(json: String): PrinterCardVisibility = try {
    val obj = JSONObject(json)
    PrinterCardVisibility(
        showCameraPreview = obj.optBoolean("camera", true),
        showPowerChip = obj.optBoolean("power", true),
        showHmsChip = obj.optBoolean("hms", true),
        showMaintenanceChip = obj.optBoolean("maint", true),
        showTemperatures = obj.optBoolean("temps", true),
    )
} catch (e: Exception) {
    if (BuddyDashDebug.enabled) Log.w(TAG, "decodeVisibility failed", e)
    PrinterCardVisibility()
}

private fun encodeSnooze(s: MaintenanceSnooze): String = try {
    JSONObject().apply {
        put("until", s.snoozedUntilMillis)
    }.toString()
} catch (e: Exception) {
    if (BuddyDashDebug.enabled) Log.w(TAG, "encodeSnooze failed", e)
    "{}"
}

private fun decodeSnooze(json: String): MaintenanceSnooze? = try {
    val obj = JSONObject(json)
    val until = obj.optLong("until", 0L)
    if (until > 0) MaintenanceSnooze(until) else null
} catch (e: Exception) {
    if (BuddyDashDebug.enabled) Log.w(TAG, "decodeSnooze failed", e)
    null
}
