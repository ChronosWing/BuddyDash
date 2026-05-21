package com.chronoswing.buddydash.data.model

data class MaintenanceItem(
    val name: String,
    val isDue: Boolean,
    val isWarning: Boolean,
    val enabled: Boolean,
)

data class PrinterMaintenanceOverview(
    val items: List<MaintenanceItem> = emptyList(),
    /** Lifetime print hours from Bambuddy maintenance overview. */
    val totalPrintHours: Double? = null,
)
