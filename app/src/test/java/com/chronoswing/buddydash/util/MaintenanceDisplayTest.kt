package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.MaintenanceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaintenanceDisplayTest {

    @Test
    fun shortenMaintenanceName_mapsCommonTasks() {
        assertEquals("Carbon rods", shortenMaintenanceName("Clean Carbon Rods"))
        assertEquals("Nozzle / hotend", shortenMaintenanceName("Clean Nozzle/Hotend"))
        assertEquals("Belt tension", shortenMaintenanceName("Check Belt Tension"))
        assertEquals("Clean build plate", shortenMaintenanceName("Clean Build Plate"))
        assertEquals("PTFE tube", shortenMaintenanceName("Check PTFE Tube"))
    }

    @Test
    fun formatMaintenanceRemainingText_okShowsLeft() {
        val item = MaintenanceItem(
            id = 1,
            name = "Clean Carbon Rods",
            isDue = false,
            isWarning = false,
            enabled = true,
            hoursUntilDue = 72.0,
        )
        assertEquals("3d left", formatMaintenanceRemainingText(item, MaintenanceLineKind.Ok))
    }

    @Test
    fun formatMaintenanceRemainingText_dueSoonShowsDueIn() {
        val item = MaintenanceItem(
            id = 1,
            name = "Clean Build Plate",
            isDue = false,
            isWarning = true,
            enabled = true,
            hoursUntilDue = 49.0 / 60.0,
        )
        assertEquals("Due in 49m", formatMaintenanceRemainingText(item, MaintenanceLineKind.DueSoon))
    }

    @Test
    fun formatMaintenanceDuration_weeksWithDecimal() {
        assertEquals("1.0w", formatMaintenanceDuration(168.0))
        assertEquals("3w", formatMaintenanceDuration(504.0))
    }

    @Test
    fun maintenanceProgressFraction_usesApiIntervalFields() {
        val item = MaintenanceItem(
            id = 1,
            name = "Check Belt",
            isDue = false,
            isWarning = true,
            enabled = true,
            intervalHours = 100.0,
            hoursSinceMaintenance = 80.0,
        )
        assertEquals(0.8f, maintenanceProgressFraction(item, MaintenanceLineKind.DueSoon))
    }

    @Test
    fun maintenanceProgressFraction_okHiddenWhenLow() {
        val item = MaintenanceItem(
            id = 1,
            name = "Check Belt",
            isDue = false,
            isWarning = false,
            enabled = true,
            intervalHours = 100.0,
            hoursSinceMaintenance = 10.0,
        )
        assertNull(maintenanceProgressFraction(item, MaintenanceLineKind.Ok))
    }
}
