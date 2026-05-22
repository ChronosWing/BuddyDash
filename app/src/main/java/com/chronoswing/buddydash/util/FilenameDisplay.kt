package com.chronoswing.buddydash.util

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale

private val PRINT_FILE_EXTENSIONS = setOf(
    "3mf",
    "gcode",
    "stl",
    "step",
    "stp",
)

private val PLATE_SUFFIX_PATTERN = Regex(
    """\s+(?:-\s+)?Plate\s+\d+$""",
    RegexOption.IGNORE_CASE,
)

/**
 * Formats a raw print filename for on-screen display only. Does not change stored/API values.
 * Preserves CJK characters; only removes obvious filename clutter.
 */
fun formatFilenameForDisplay(raw: String): String {
    if (raw.isBlank() || raw == "—") return raw
    var text = decodeUrlEncodedFilename(raw.trim())
    text = Normalizer.normalize(text, Normalizer.Form.NFC)
    text = stripPrintFileExtension(text)
    text = text.replace('_', ' ')
    text = collapseRepeatedSpaces(text)
    return text.trim()
}

/** Normalized title key for archive/spool usage matching (NFC, CJK preserved, Latin folded). */
fun normalizePrintTitleForMatch(raw: String): String? {
    val trimmed = raw.trim()
    if (!isMeaningfulSpoolField(trimmed)) return null
    var text = decodeUrlEncodedFilename(trimmed)
    text = Normalizer.normalize(text, Normalizer.Form.NFC)
    text = stripPrintFileExtension(text)
    text = stripPlateSuffixForTitleMatch(text)
    text = normalizeSeparatorsForTitleMatch(text)
    text = collapseRepeatedSpaces(text)
    text = foldLatinCaseForTitleMatch(text)
    return text.trim().takeIf { it.isNotBlank() }
}

/** @deprecated Use [normalizePrintTitleForMatch]. */
fun normalizePrintNameForArchiveMatch(raw: String): String? = normalizePrintTitleForMatch(raw)

fun stripPlateSuffixForTitleMatch(name: String): String =
    name.replace(PLATE_SUFFIX_PATTERN, "").trim()

fun titleHasCjkOrMixedScript(text: String): Boolean {
    var hasCjk = false
    var hasLatin = false
    for (ch in text) {
        if (ch.isCjkScriptChar()) hasCjk = true
        if (ch.isLatinLetterChar()) hasLatin = true
        if (hasCjk && hasLatin) return true
    }
    return hasCjk
}

fun titlesContainMatchHighConfidence(usageKey: String, archiveKey: String): Boolean {
    if (usageKey == archiveKey) return false
    val (shorter, longer) = if (usageKey.length <= archiveKey.length) {
        usageKey to archiveKey
    } else {
        archiveKey to usageKey
    }
    if (shorter.length < 4) return false
    if (!longer.contains(shorter)) return false
    val ratio = shorter.length.toDouble() / longer.length.toDouble()
    return ratio >= 0.75
}

internal fun decodeUrlEncodedFilename(raw: String): String {
    if (!raw.contains('%')) return raw
    return try {
        URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    } catch (_: IllegalArgumentException) {
        raw
    }
}

internal fun stripPrintFileExtension(name: String): String {
    val dot = name.lastIndexOf('.')
    if (dot <= 0) return name
    val ext = name.substring(dot + 1).lowercase(Locale.ROOT)
    if (ext in PRINT_FILE_EXTENSIONS) {
        return name.substring(0, dot).trim()
    }
    return name
}

internal fun collapseRepeatedSpaces(text: String): String =
    text.replace(Regex("\\s+"), " ").trim()

/** Underscores/hyphens become spaces for comparison only (CJK preserved). */
internal fun normalizeSeparatorsForTitleMatch(text: String): String =
    text.replace(Regex("[_\\-]+"), " ").let { collapseRepeatedSpaces(it) }

/** Lowercase Latin letters only; CJK and other scripts unchanged. */
internal fun foldLatinCaseForTitleMatch(text: String): String = buildString(text.length) {
    for (ch in text) {
        append(
            when {
                ch.isCjkScriptChar() -> ch
                ch.isLatinLetterChar() -> ch.lowercaseChar()
                else -> ch
            },
        )
    }
}

internal fun Char.isCjkScriptChar(): Boolean {
    if (!Character.isLetter(this)) return false
    return when (Character.UnicodeBlock.of(this)) {
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
        -> true
        else -> false
    }
}

internal fun Char.isLatinLetterChar(): Boolean =
    Character.isLetter(this) && !isCjkScriptChar()
