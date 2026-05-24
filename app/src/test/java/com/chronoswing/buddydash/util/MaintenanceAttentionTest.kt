package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.MaintenanceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceAttentionTest {

    @Test
    fun maintenanceAttentionLines_filtersOkItems() {
        val items = listOf(
            MaintenanceItem(
                id = 1,
                name = "Clean Carbon Rods",
                isDue = false,
                isWarning = true,
                enabled = true,
                hoursUntilDue = 12.0,
            ),
            MaintenanceItem(
                id = 2,
                name = "Check Belt",
                isDue = false,
                isWarning = false,
                enabled = true,
                hoursUntilDue = 120.0,
            ),
            MaintenanceItem(
                id = 3,
                name = "Clean Build Plate",
                isDue = true,
                isWarning = false,
                enabled = true,
            ),
        )
        val lines = maintenanceAttentionLines(items)
        assertEquals(2, lines.size)
        assertTrue(lines.any { it.kind == MaintenanceLineKind.DueSoon })
        assertTrue(lines.any { it.kind == MaintenanceLineKind.Due })
    }

    @Test
    fun formatMaintenanceDetailMeta_includesOverdueAndElapsed() {
        val item = MaintenanceItem(
            id = 1,
            name = "Clean Carbon Rods",
            isDue = true,
            isWarning = false,
            enabled = true,
            hoursSinceMaintenance = 240.0,
            intervalHours = 200.0,
            intervalType = "hours",
        )
        val meta = formatMaintenanceDetailMeta(item, MaintenanceLineKind.Due)
        assertTrue(meta!!.contains("Overdue"))
        assertTrue(meta.contains("since last service"))
        assertTrue(meta.contains("Interval"))
    }
}
