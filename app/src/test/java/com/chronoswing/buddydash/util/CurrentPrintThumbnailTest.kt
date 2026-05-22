package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.BambuddyApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CurrentPrintThumbnailTest {

    @Test
    fun resolveCurrentPrintFileKey_prefersCurrentPrintAndGcode() {
        val key = resolveCurrentPrintFileKey(
            fileName = "fallback.3mf",
            currentPrint = "Plate A",
            subtaskName = null,
            gcodeFile = "/data/foo/bar.gcode",
        )
        assertEquals("Plate A|bar.gcode", key)
    }

    @Test
    fun cacheKey_changesWhenFileChanges() {
        val first = sampleStatus(fileName = "a.3mf", currentPrint = "Job A")
            .toCurrentPrintThumbnailIdentity(printerId = 1)
        val second = sampleStatus(fileName = "b.3mf", currentPrint = "Job B")
            .toCurrentPrintThumbnailIdentity(printerId = 1)
        assertNotEquals(first.cacheKey(), second.cacheKey())
        assertFalse(first.samePrintAs(second))
    }

    @Test
    fun cacheKey_includesQueueJobId() {
        val withoutJob = sampleStatus(currentPrint = "Job A")
            .toCurrentPrintThumbnailIdentity(printerId = 2, queueJobId = null)
        val withJob = sampleStatus(currentPrint = "Job A")
            .toCurrentPrintThumbnailIdentity(printerId = 2, queueJobId = 99)
        assertNotEquals(withoutJob.cacheKey(), withJob.cacheKey())
    }

    @Test
    fun normalizePrintThumbnailPath_usesCoverUrlWhenPresent() {
        val path = normalizePrintThumbnailPath("/cache/covers/abc.png", printerId = 3)
        assertEquals("/cache/covers/abc.png", path)
    }

    @Test
    fun normalizePrintThumbnailPath_fallsBackToApiCoverPath() {
        val path = normalizePrintThumbnailPath(null, printerId = 3)
        assertEquals(BambuddyApi.printerCoverPath(3), path)
    }

    private fun sampleStatus(
        fileName: String? = "test.3mf",
        currentPrint: String? = null,
    ) = PrinterStatus(
        connected = true,
        rawState = "RUNNING",
        progress = 10f,
        fileName = fileName,
        currentPrint = currentPrint,
        coverUrl = "/covers/test.png",
        remainingTimeSeconds = null,
        nozzleTemp = null,
        bedTemp = null,
    )
}
