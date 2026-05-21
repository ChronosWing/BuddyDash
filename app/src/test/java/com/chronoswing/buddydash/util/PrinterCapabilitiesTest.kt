package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterCapabilitiesTest {

    @Test
    fun bedSlingerProfile_hidesDoorAndChamberFan() {
        val profile = resolvePrinterHardwareProfile("Bambu Lab A1 mini")
        assertTrue(profile.isBedSlinger)
        assertFalse(profile.hasEnclosureDoor)
        assertFalse(profile.hasChamberFan)
    }

    @Test
    fun p1sProfile_supportsEnclosureStats() {
        val profile = resolvePrinterHardwareProfile("P1S")
        assertFalse(profile.isBedSlinger)
        assertTrue(profile.hasEnclosureDoor)
        assertTrue(profile.hasChamberFan)
    }

    @Test
    fun applyCapabilities_a1HidesDoorEvenWhenApiReportsDoor() {
        val caps = applyDisplayCapabilities(
            PrinterStatus(
                connected = true,
                rawState = "IDLE",
                progress = null,
                fileName = null,
                remainingTimeSeconds = null,
                nozzleTemp = 25.0,
                bedTemp = 25.0,
                doorOpen = false,
                chamberFanPercent = 0,
                chamberTemp = 0.0,
            ),
            printerModel = "A1",
        )
        assertNull(caps.doorLine)
        assertNull(caps.chamberFanPercent)
        assertNull(caps.chamberTempCompact)
    }

    @Test
    fun filterAmsUnits_dropsLiteFakeZeros() {
        val filtered = filterAmsUnitsForDisplay(
            listOf(
                AmsUnitInfo(
                    amsId = 0,
                    label = "AMS-A",
                    moduleType = "AMS_LITE",
                    tempC = 0.0,
                    humidityPercent = 0,
                ),
                AmsUnitInfo(
                    amsId = 1,
                    label = "AMS-B",
                    moduleType = "AMS",
                    tempC = 28.0,
                    humidityPercent = 42,
                ),
            ),
        )
        assertEquals(1, filtered.size)
        assertEquals(28.0, filtered[0].tempC!!, 0.001)
        assertEquals(42, filtered[0].humidityPercent)
    }

    @Test
    fun isAmsLiteModule_detectsLiteVariants() {
        assertTrue(isAmsLiteModule("AMS_LITE"))
        assertTrue(isAmsLiteModule("ams lite"))
        assertFalse(isAmsLiteModule("AMS"))
    }
}
