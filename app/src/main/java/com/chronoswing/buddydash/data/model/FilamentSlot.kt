package com.chronoswing.buddydash.data.model

import com.chronoswing.buddydash.util.SlotInventoryKey

data class FilamentSlot(
    val label: String,
    val filamentType: String?,
    val colorHex: String?,
    /** Remaining % from Bambuddy inventory assignment when available. */
    val remainPercent: Int? = null,
    val metadata: String? = null,
    val isExternal: Boolean = false,
    val isLoaded: Boolean = true,
    val amsId: Int? = null,
    val trayId: Int? = null,
) {
    val inventoryKey: SlotInventoryKey?
        get() = if (amsId != null && trayId != null) SlotInventoryKey(amsId, trayId) else null
}
