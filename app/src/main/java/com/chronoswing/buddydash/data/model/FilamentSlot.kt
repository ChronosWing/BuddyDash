package com.chronoswing.buddydash.data.model

import com.chronoswing.buddydash.util.SlotInventoryKey

data class FilamentSlot(
    val label: String,
    val filamentType: String?,
    /** All swatch colors (1 = solid, 2 = diagonal, 3+ = pie segments). */
    val swatchColorHexes: List<String> = emptyList(),
    val isTranslucent: Boolean = false,
    val colorAlpha: Float = 1f,
    /** Remaining % from Bambuddy inventory assignment when available. */
    val remainPercent: Int? = null,
    val metadata: String? = null,
    val isExternal: Boolean = false,
    val isLoaded: Boolean = true,
    val amsId: Int? = null,
    val trayId: Int? = null,
    /** MQTT / Bambuddy tray id from status JSON `id` field, used to match `tray_now`. */
    val mqttTrayId: Int? = null,
    /** Inventory spool id when assignments map this tray (from status enrichment). */
    val inventorySpoolId: Int? = null,
) {
    val colorHex: String?
        get() = swatchColorHexes.firstOrNull()

    val inventoryKey: SlotInventoryKey?
        get() = if (amsId != null && trayId != null) SlotInventoryKey(amsId, trayId) else null
}
