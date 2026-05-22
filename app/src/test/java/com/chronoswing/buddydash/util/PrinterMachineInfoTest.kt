package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterMachineInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrinterMachineInfoTest {

    @Test
    fun formatWifiSignalDisplay_omitsWhenWired() {
        assertNull(formatWifiSignalDisplay(-55, wiredNetwork = true))
        assertEquals("-55 dBm", formatWifiSignalDisplay(-55, wiredNetwork = false))
    }

    @Test
    fun formatLanModeDisplay_mapsWiredNetwork() {
        assertEquals("Wired", formatLanModeDisplay(true))
        assertEquals("Wi-Fi", formatLanModeDisplay(false))
    }

    @Test
    fun buildMachineInfoRows_followsRecommendedOrder() {
        val labels = PrinterDetailLabels(
            isActivePrint = false,
            activityKind = PrinterActivityKind.Idle,
            progressCompact = null,
            plateKind = null,
            connection = "Connected",
            currentActivity = "Idle",
            lastPrintResult = null,
            showLastPrintOnCard = false,
            showFile = false,
            fileLabel = "Last file",
            fileName = "",
            showProgress = false,
            progressTitle = "Progress",
            progressValue = "—",
            progressFraction = null,
            plateStatus = null,
            showPlateClearAction = false,
            plateClearEndpointAvailable = false,
            showEta = false,
            eta = "",
            filamentUsageCompact = null,
            tempsLine = null,
            hmsSummary = "HMS OK",
            nozzleTemp = "—",
            bedTemp = "—",
            hmsHealth = "OK",
            hmsHasErrors = false,
            printerErrorDisplay = PrinterErrorDisplay(showCard = false, lines = emptyList(), hasKnownDetails = false),
            filamentSlots = emptyList(),
            printerRawState = "IDLE",
            amsUnits = emptyList(),
            showConnectivitySection = false,
            wifiCompact = "-60 dBm",
            wifiSignalDbm = -60,
            wiredNetwork = false,
            doorLine = null,
            firmwareLine = "01.00.00.00",
            totalPrintTimeCompact = "120h",
            chamberTempCompact = null,
            showFansSection = false,
            partFanPercent = null,
            auxFanPercent = null,
            chamberFanPercent = null,
            showPrintSpeedSection = false,
            printSpeedLabel = null,
            speedLevel = null,
            chamberLightOn = null,
            canControlPrint = false,
            canPause = false,
            canResume = false,
            canStop = false,
            canToggleLight = false,
            motionLayout = PrinterMotionLayout.Hidden,
            showMotionControls = false,
            canUseMotionControls = false,
            maintenanceItems = emptyList(),
            developerMode = true,
        )
        val machineInfo = PrinterMachineInfo(
            serialNumber = "SN123",
            ipAddress = "192.168.1.10",
            model = "P1S",
            nozzleCount = 1,
            autoArchiveEnabled = true,
        )
        val keys = buildMachineInfoRows(labels, machineInfo, printerModel = null, statusUpdatedAtMillis = null)
            .map { it.first }
        assertEquals(
            listOf(
                "connection",
                "state",
                "model",
                "firmware",
                "ip",
                "wifi_signal",
                "lan_mode",
                "serial",
                "nozzle_count",
                "developer_mode",
                "print_hours",
                "auto_archive",
            ),
            keys,
        )
    }
}
