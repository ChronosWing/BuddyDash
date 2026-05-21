package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FilenameDisplayTest {

    @Test
    fun replacesUnderscoresWithSpaces() {
        assertEquals(
            "ISOFIX SYSTEM for umbrella documents bags etc",
            formatFilenameForDisplay("ISOFIX_SYSTEM_for_umbrella_documents_bags_etc"),
        )
        assertEquals(
            "Universal 4 pin trailer plug mount",
            formatFilenameForDisplay("Universal_4_pin_trailer_plug_mount"),
        )
    }

    @Test
    fun collapsesRepeatedSeparators() {
        assertEquals(
            "part one part two",
            formatFilenameForDisplay("part__one___part__two"),
        )
    }
}
