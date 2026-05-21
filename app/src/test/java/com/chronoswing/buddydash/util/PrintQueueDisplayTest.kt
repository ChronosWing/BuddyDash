package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrintQueueJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintQueueDisplayTest {

    @Test
    fun formatQueueDuration_formatsHoursAndMinutes() {
        assertEquals("1h 5m", formatQueueDuration(3900))
        assertEquals("12m", formatQueueDuration(720))
        assertNull(formatQueueDuration(null))
        assertNull(formatQueueDuration(0))
    }

    @Test
    fun normalizeQueueFieldValue_skipsLiteralNullAndUsesTitle() {
        assertNull(normalizeQueueFieldValue("library_file_name", "null"))
        assertEquals("Cable clip", normalizeQueueFieldValue("title", "Cable clip"))
    }

    @Test
    fun normalizeQueueFieldValue_gcodeBasename() {
        assertEquals(
            "widget_bracket.gcode",
            normalizeQueueFieldValue("gcode_file", "/data/prints/widget_bracket.gcode"),
        )
    }

    @Test
    fun resolveQueueThumbnailSource_matchesBambuddyWebRouting() {
        val libraryPlate = PrintQueueJob(
            id = 1,
            position = 0,
            displayName = "A",
            hasLibraryThumbnail = true,
            libraryFileId = 12,
            plateId = 2,
        )
        assertEquals(
            QueueThumbnailSource.LIBRARY_PLATE,
            resolveQueueThumbnailSource(libraryPlate).source,
        )
        assertEquals(
            "/api/v1/library/files/12/plate-thumbnail/2",
            resolveQueueThumbnailSource(libraryPlate).apiPath,
        )

        val archive = PrintQueueJob(
            id = 2,
            position = 1,
            displayName = "B",
            hasArchiveThumbnail = true,
            archiveId = 5,
        )
        assertEquals(QueueThumbnailSource.ARCHIVE, resolveQueueThumbnailSource(archive).source)
        assertEquals(
            "/api/v1/archives/5/thumbnail",
            resolveQueueThumbnailSource(archive).apiPath,
        )
    }

    @Test
    fun isUsableQueueFieldValue_rejectsNullAndBlank() {
        assertFalse(isUsableQueueFieldValue(null))
        assertFalse(isUsableQueueFieldValue("null"))
        assertFalse(isUsableQueueFieldValue("  "))
        assertTrue(isUsableQueueFieldValue("Part name"))
    }
}
