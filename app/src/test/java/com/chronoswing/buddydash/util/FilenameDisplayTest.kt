package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun preservesChineseCharactersForDisplay() {
        assertEquals(
            "万能表探针固定夹 单手操作表笔辅助工具",
            formatFilenameForDisplay("万能表探针固定夹_单手操作表笔辅助工具"),
        )
    }

    @Test
    fun stripsFileExtensionForDisplay() {
        assertEquals(
            "Benchy",
            formatFilenameForDisplay("Benchy.3mf"),
        )
        assertEquals(
            "万能表探针固定夹",
            formatFilenameForDisplay("万能表探针固定夹.3mf"),
        )
    }

    @Test
    fun decodesUrlEncodingForDisplay() {
        assertEquals(
            "万能表探针",
            formatFilenameForDisplay("%E4%B8%87%E8%83%BD%E8%A1%A8%E6%8E%A2%E9%92%88"),
        )
    }

    @Test
    fun collapsesRepeatedSeparators() {
        assertEquals(
            "part one part two",
            formatFilenameForDisplay("part__one___part__two"),
        )
    }

    @Test
    fun normalizePrintTitleForMatch_preservesCjkDoesNotLowercase() {
        val chinese = "万能表探针固定夹 单手操作表笔辅助工具"
        assertEquals(
            chinese,
            normalizePrintTitleForMatch("万能表探针固定夹_单手操作表笔辅助工具.3mf"),
        )
    }

    @Test
    fun normalizePrintTitleForMatch_foldsLatinCaseOnly() {
        assertEquals(
            "mecha robot",
            normalizePrintTitleForMatch("MECHA_ROBOT.3mf"),
        )
    }

    @Test
    fun titlesContainMatchHighConfidence_requiresStrongOverlap() {
        val usage = normalizePrintTitleForMatch("万能表探针固定夹_单手操作表笔辅助")!!
        val archive = normalizePrintTitleForMatch("万能表探针固定夹_单手操作表笔辅助工具")!!
        assertTrue(titlesContainMatchHighConfidence(usage, archive))
        assertFalse(titlesContainMatchHighConfidence("ab", "abcdefgh"))
    }

    @Test
    fun titleHasCjkOrMixedScript_detectsScripts() {
        assertTrue(titleHasCjkOrMixedScript("万能表 Universal"))
        assertTrue(titleHasCjkOrMixedScript("万能表"))
        assertFalse(titleHasCjkOrMixedScript("Universal mount"))
    }
}
