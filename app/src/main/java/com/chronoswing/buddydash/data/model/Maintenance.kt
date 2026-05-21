package com.chronoswing.buddydash.data.model

data class MaintenanceItem(
    /** Bambuddy maintenance item id for POST …/items/{item_id}/perform. */
    val id: Int,
    val name: String,
    /** API `is_due` — maintenance is due or overdue. */
    val isDue: Boolean,
    /** API `is_warning` — due soon, not yet due. */
    val isWarning: Boolean,
    val enabled: Boolean,
    val hoursUntilDue: Double? = null,
    val daysUntilDue: Double? = null,
)

data class PrinterMaintenanceOverview(
    val items: List<MaintenanceItem> = emptyList(),
    /** Lifetime print hours from Bambuddy maintenance overview. */
    val totalPrintHours: Double? = null,
)
