package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartQueuedPrintTest {

    @Test
    fun evaluateStartNextQueuedPrintReadiness_readyWhenIdlePlateClearAndQueued() {
        val status = queueStatus(connected = true, rawState = "IDLE", awaitingPlateClear = false)
        val readiness = evaluateStartNextQueuedPrintReadiness(status, queuedItemCount = 2)
        assertTrue(readiness.canStart)
        assertEquals(StartNextQueuedPrintBlockReason.None, readiness.blockReason)
    }

    @Test
    fun evaluateStartNextQueuedPrintReadiness_blockedWhenQueueEmpty() {
        val status = queueStatus(connected = true, rawState = "IDLE", awaitingPlateClear = false)
        val readiness = evaluateStartNextQueuedPrintReadiness(status, queuedItemCount = 0)
        assertFalse(readiness.canStart)
    }

    @Test
    fun evaluateStartNextQueuedPrintReadiness_blockedWhenPlateNotClear() {
        val status = queueStatus(connected = true, rawState = "IDLE", awaitingPlateClear = true)
        val readiness = evaluateStartNextQueuedPrintReadiness(status, queuedItemCount = 1)
        assertFalse(readiness.canStart)
        assertEquals(StartNextQueuedPrintBlockReason.PlateNotClear, readiness.blockReason)
    }

    @Test
    fun evaluateStartNextQueuedPrintReadiness_blockedWhenPrinting() {
        val status = queueStatus(connected = true, rawState = "RUNNING", awaitingPlateClear = false)
        val readiness = evaluateStartNextQueuedPrintReadiness(status, queuedItemCount = 3)
        assertFalse(readiness.canStart)
        assertEquals(StartNextQueuedPrintBlockReason.PrinterBusy, readiness.blockReason)
    }

    private fun queueStatus(
        connected: Boolean,
        rawState: String,
        awaitingPlateClear: Boolean?,
    ) = PrinterStatus(
        connected = connected,
        rawState = rawState,
        progress = null,
        fileName = null,
        remainingTimeSeconds = null,
        nozzleTemp = null,
        bedTemp = null,
        hmsErrorCount = 0,
        awaitingPlateClear = awaitingPlateClear,
    )
}
