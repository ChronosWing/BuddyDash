package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoolUsageDisplayTest {

    @Test
    fun parseSpoolUsageHistoryList_parsesOpenApiShape() {
        val body =
            """[{"id":10,"spool_id":5,"printer_id":2,"print_name":"Benchy.3mf","weight_used":12.5,"percent_used":3,"status":"completed","created_at":"2024-06-01T12:00:00Z"}]"""
        val entries = parseSpoolUsageHistoryList(body)
        assertEquals(1, entries.size)
        assertEquals(10, entries.first().id)
        assertEquals(5, entries.first().spoolId)
        assertEquals(2, entries.first().printerId)
        assertEquals("Benchy.3mf", entries.first().printName)
        assertEquals(12.5, entries.first().weightUsedGrams, 0.001)
    }

    @Test
    fun parseSpoolUsageHistoryList_emptyArray() {
        assertTrue(parseSpoolUsageHistoryList("[]").isEmpty())
    }

    @Test
    fun formatSpoolUsageWeight_formatsGrams() {
        val entry = parseSpoolUsageHistoryList(
            """[{"id":1,"spool_id":1,"weight_used":5,"status":"ok","created_at":"2024-01-01T00:00:00Z"}]""",
        ).first()
        assertEquals("5g", formatSpoolUsageWeight(entry))
    }
}
