package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.MaintenanceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusCueTest {

    @Test
    fun resolveWifiSignalLevel_usesDbmThresholds() {
        assertNull(resolveWifiSignalLevel(null, wiredNetwork = true))
        assertNull(resolveWifiSignalLevel(-60, wiredNetwork = true))
        assertEquals(WifiSignalLevel.Strong, resolveWifiSignalLevel(-55, false))
        assertEquals(WifiSignalLevel.Good, resolveWifiSignalLevel(-65, false))
        assertEquals(WifiSignalLevel.Weak, resolveWifiSignalLevel(-75, false))
    }

    @Test
    fun resolveMaintenanceHomeIndicator_distinguishesDueSoonFromDue() {
        val dueSoonOnly = listOf(
            MaintenanceItem(1, "Clean Build Plate", isDue = false, isWarning = true, enabled = true),
        )
        val due = listOf(
            MaintenanceItem(1, "Clean Build Plate", isDue = true, isWarning = false, enabled = true),
        )
        assertEquals(MaintenanceHomeIndicator.DueSoon, resolveMaintenanceHomeIndicator(dueSoonOnly))
        assertEquals(MaintenanceHomeIndicator.Due, resolveMaintenanceHomeIndicator(due))
        assertEquals(MaintenanceHomeIndicator.None, resolveMaintenanceHomeIndicator(emptyList()))
    }
}
