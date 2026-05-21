package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.ArchiveResultKind
import com.chronoswing.buddydash.data.model.PrintArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveDetailDisplayTest {

    @Test
    fun isMeaningfulArchiveField_rejectsNullLiteral() {
        assertFalse(isMeaningfulArchiveField("null"))
        assertFalse(isMeaningfulArchiveField("  "))
        assertTrue(isMeaningfulArchiveField("PLA"))
    }

    @Test
    fun shouldShowArchiveFailureReason_onlyForFailedWithText() {
        val failed = sampleArchive(
            kind = ArchiveResultKind.Failed,
            failureReason = "Bed adhesion",
        )
        assertTrue(shouldShowArchiveFailureReason(failed))

        val success = sampleArchive(
            kind = ArchiveResultKind.Success,
            failureReason = "ignored",
        )
        assertFalse(shouldShowArchiveFailureReason(success))

        val failedNoReason = sampleArchive(
            kind = ArchiveResultKind.Failed,
            failureReason = null,
        )
        assertFalse(shouldShowArchiveFailureReason(failedNoReason))
    }

    @Test
    fun formatArchiveDetailMaterialType_omitsHex() {
        val archive = sampleArchive(
            kind = ArchiveResultKind.Success,
            filamentType = "pla",
            filamentColor = "#FFFFFF",
        )
        assertEquals("PLA", formatArchiveDetailMaterialType(archive))
    }

    private fun sampleArchive(
        kind: ArchiveResultKind,
        failureReason: String? = null,
        filamentType: String? = null,
        filamentColor: String? = null,
    ) = PrintArchive(
        id = 1,
        displayName = "Test",
        printerId = 1,
        printerName = "Printer",
        printerModel = null,
        statusRaw = kind.name.lowercase(),
        resultKind = kind,
        startedAtIso = null,
        completedAtIso = null,
        durationSeconds = null,
        filamentUsage = null,
        filamentType = filamentType,
        filamentColor = filamentColor,
        failureReason = failureReason,
        totalLayers = null,
        quantity = null,
        projectName = null,
        slicedForModel = null,
        notes = null,
    )
}
