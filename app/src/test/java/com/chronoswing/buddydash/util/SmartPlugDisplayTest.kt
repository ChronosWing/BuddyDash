package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.data.model.SmartOutletPowerState
import com.chronoswing.buddydash.data.model.SmartPlugEnergyReading
import com.chronoswing.buddydash.data.model.parseSmartOutletPowerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartPlugDisplayTest {

    @Test
    fun parseSmartOutletPowerState_recognizesCommonValues() {
        assertEquals(SmartOutletPowerState.On, parseSmartOutletPowerState("ON"))
        assertEquals(SmartOutletPowerState.Off, parseSmartOutletPowerState("off"))
        assertEquals(SmartOutletPowerState.Unknown, parseSmartOutletPowerState(null))
    }

    @Test
    fun formatSmartPlugPowerWatts_formatsWholeWatts() {
        val formatted = formatSmartPlugPowerWatts(
            SmartPlugEnergyReading(powerWatts = 123.4),
        )
        assertEquals("123 W", formatted)
    }

    @Test
    fun requiresActivePowerOffConfirmation_trueWhilePrinting() {
        val status = PrinterStatus(
            connected = true,
            rawState = "RUNNING",
            progress = 10f,
            fileName = "part.3mf",
            remainingTimeSeconds = 600,
            nozzleTemp = 220.0,
            bedTemp = 60.0,
        )
        assertTrue(status.requiresActivePowerOffConfirmation())
    }

    @Test
    fun requiresActivePowerOffConfirmation_falseWhenIdle() {
        val status = PrinterStatus(
            connected = true,
            rawState = "IDLE",
            progress = null,
            fileName = null,
            remainingTimeSeconds = null,
            nozzleTemp = 25.0,
            bedTemp = 25.0,
        )
        assertFalse(status.requiresActivePowerOffConfirmation())
    }
}
