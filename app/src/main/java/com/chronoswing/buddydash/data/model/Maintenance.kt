package com.chronoswing.buddydash.data.model

data class MaintenanceItem(
    val name: String,
    val isDue: Boolean,
    val isWarning: Boolean,
    val enabled: Boolean,
)

data class PrinterMaintenanceOverview(
    val items: List<MaintenanceItem> = emptyList(),
)
