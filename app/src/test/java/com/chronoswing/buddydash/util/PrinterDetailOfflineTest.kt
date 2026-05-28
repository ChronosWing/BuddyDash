package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.Printer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterDetailOfflineTest {

    @Test
    fun disconnectedStatus_mapsToOfflineDetailLabels() {
        val labels = disconnectedPrinterStatus().toDetailLabels(printerModel = "X1 Carbon")
        assertEquals(PrinterActivityKind.Offline, labels.activityKind)
        assertEquals("Disconnected", labels.connection)
        assertEquals("Offline", labels.currentActivity)
        assertFalse(labels.isActivePrint)
        assertFalse(labels.canControlPrint)
        assertFalse(labels.showMotionControls)
    }

    @Test
    fun homeCachePrinterWithoutLiveStatus_getsDisconnectedFallback() {
        val printer = Printer(id = 7, name = "Basement", model = "P1S", liveStatus = null)
        val status = printer.withEnrichFallback().liveStatus
        assertNotNull(status)
        assertFalse(status!!.connected)
        assertEquals(PrinterActivityKind.Offline, status.resolveActivityKind())
    }

    @Test
    fun machineTabCapabilities_disableLiveControlsWhenOffline() {
        val labels = disconnectedPrinterStatus().toDetailLabels(printerModel = "X1 Carbon")
        val caps = labels.machineTabCapabilities(cameraTokenConfigured = true)
        assertFalse(caps.motionEnabled)
        assertFalse(caps.cameraEnabled)
        assertFalse(caps.homeEnabled)
        assertFalse(caps.filesEnabled)
        assertFalse(caps.utilitiesEnabled)
        assertEquals("offline", caps.utilitiesDisabledReason)
    }
}
