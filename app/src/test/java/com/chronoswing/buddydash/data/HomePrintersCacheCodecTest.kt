package com.chronoswing.buddydash.data

import com.chronoswing.buddydash.data.model.AmsUnitInfo
import com.chronoswing.buddydash.data.model.FilamentSlot
import com.chronoswing.buddydash.data.model.FilamentUsage
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.util.MaintenanceHomeIndicator
import com.chronoswing.buddydash.util.SlotInventoryKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HomePrintersCacheCodecTest {

    @Test
    fun roundTrip_preservesHomePrinterCards() {
        val printer = Printer(
            id = 42,
            name = "Workshop",
            model = "X1C",
            maintenanceIndicator = MaintenanceHomeIndicator.DueSoon,
            pendingQueueCount = 2,
            liveStatus = PrinterStatus(
                connected = true,
                rawState = "RUNNING",
                progress = 0.42f,
                fileName = "benchy.gcode",
                remainingTimeSeconds = 900,
                nozzleTemp = 220.0,
                bedTemp = 60.0,
                hmsErrors = listOf(PrinterHmsError(code = "HMS_1", attr = 1, detail = "Test")),
                statusFaultMessages = listOf("Fault A"),
                filamentSlots = listOf(
                    FilamentSlot(
                        label = "A1",
                        filamentType = "PLA",
                        swatchColorHexes = listOf("#FF0000", "#00FF00"),
                        amsId = 0,
                        trayId = 1,
                    ),
                ),
                activeFilamentSlot = SlotInventoryKey(0, 1),
                amsUnits = listOf(
                    AmsUnitInfo(amsId = 0, label = "AMS", moduleType = "AMS", tempC = 28.0, humidityPercent = 10),
                ),
                wifiSignalDbm = -55,
                filamentUsage = FilamentUsage(weightGrams = 12.5, lengthMeters = 4.2),
            ),
        )
        val snapshot = HomePrintersCacheSnapshot(
            serverUrl = "http://192.168.1.10",
            printers = listOf(printer),
            lastUpdatedAtMillis = 1_700_000_000_000L,
        )
        val encoded = HomePrintersCacheCodec.encode(snapshot)
        val decoded = HomePrintersCacheCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(snapshot, decoded)
    }
}
