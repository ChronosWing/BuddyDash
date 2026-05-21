package com.chronoswing.buddydash.data.model

import com.chronoswing.buddydash.util.FilamentSwatchColors

/** Read-only spool from GET /api/v1/inventory/spools, enriched with slot assignment if loaded. */
data class SpoolInventoryItem(
    val id: Int,
    val material: String,
    val subtype: String? = null,
    val colorName: String? = null,
    val brand: String? = null,
    val swatch: FilamentSwatchColors,
    val remainPercent: Int?,
    val lowStockThresholdPct: Int?,
    val isLowStock: Boolean,
    val displayName: String,
    val assignment: SpoolSlotAssignment? = null,
)

/** Where a spool is loaded (GET /api/v1/inventory/assignments). */
data class SpoolSlotAssignment(
    val printerId: Int,
    val printerName: String,
    val slotLabel: String,
)
