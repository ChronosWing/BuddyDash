package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterHmsError

/**
 * Local lookup table for known Bambu HMS codes.
 *
 * BambuBuddy's API sends HMS entries as (module: Int, code: "0xHEX", severity: Int).
 * The API carries no message text, and the raw severity integer does not reliably
 * map to Bambu's Error/Warning/Notification classification for all codes.
 *
 * This table maps the Bambu-style key "MODULE-CODE" (e.g. "0500-C010") to a
 * human-readable message and the correct alert level, sourced from the Bambu Lab
 * HMS wiki and community databases.
 *
 * Module encoding: Bambu HMS module segment = "%02X00".format(moduleInt)
 *   module 3  → "0300"  (AMS)
 *   module 5  → "0500"  (print system / MicroSD)
 *   module 7  → "0700"  (toolhead / motion)
 */
object BambuHmsLookup {

    data class HmsInfo(
        /** Human-readable message from the Bambu Lab HMS wiki. */
        val message: String,
        /** Alert level for this code (source: Bambu Lab classification). */
        val alertLevel: HmsAlertLevel,
    )

    // ── Code formatting ──────────────────────────────────────────────────────

    /**
     * Returns (moduleHex4, codeHex4) display parts.
     *   module=5, rawCode="0xc010" → ("0500", "C010")
     */
    fun formatCodeParts(module: Int?, rawCode: String?): Pair<String?, String?> {
        val modulePart = module?.let { "%02X00".format(it) }
        val codePart = rawCode
            ?.removePrefix("0x")?.removePrefix("0X")
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
        return modulePart to codePart
    }

    /**
     * Full "[MODULE-CODE]" display string, e.g. "[0500-C010]".
     * Returns null only when both module and rawCode are absent.
     */
    fun formatDisplayCode(module: Int?, rawCode: String?): String? {
        val (modulePart, codePart) = formatCodeParts(module, rawCode)
        return when {
            modulePart != null && codePart != null -> "[$modulePart-$codePart]"
            modulePart != null -> "[$modulePart]"
            codePart != null -> "[$codePart]"
            else -> null
        }
    }

    fun formatDisplayCode(entry: PrinterHmsError): String? =
        formatDisplayCode(entry.module, entry.code)

    // ── Lookup ───────────────────────────────────────────────────────────────

    /** Returns HMS info for this entry, or null when not in the table. */
    fun lookup(module: Int?, rawCode: String?): HmsInfo? {
        val (modulePart, codePart) = formatCodeParts(module, rawCode)
        val key = buildString {
            if (modulePart != null) append(modulePart)
            if (codePart != null) { if (isNotEmpty()) append("-"); append(codePart) }
        }
        return if (key.isEmpty()) null else table[key]
    }

    fun lookup(entry: PrinterHmsError): HmsInfo? = lookup(entry.module, entry.code)

    /**
     * Bambu Lab wiki URL for the given code.
     * Returns null when module or code is absent.
     */
    fun wikiUrl(module: Int?, rawCode: String?): String? {
        val codeStr = formatDisplayCode(module, rawCode)
            ?.removePrefix("[")?.removeSuffix("]") ?: return null
        return "https://wiki.bambulab.com/en/home/topics/hms?hms_code=$codeStr"
    }

    fun wikiUrl(entry: PrinterHmsError): String? = wikiUrl(entry.module, entry.code)

    // ── Table ── source: Bambu Lab HMS wiki + community databases ────────────

    private val table: Map<String, HmsInfo> = mapOf(

        // Module 05 (0500) — Print system / MicroSD card
        "0500-C008" to HmsInfo(
            message = "MicroSD Card full: please free up space or replace the MicroSD Card.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0500-C010" to HmsInfo(
            message = "MicroSD Card read/write exception: please reinsert or replace the MicroSD Card.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0500-C011" to HmsInfo(
            message = "MicroSD Card not inserted.",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // Module 03 (0300) — AMS
        "0300-4000" to HmsInfo(
            message = "AMS communication error.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-8001" to HmsInfo(
            message = "AMS 1 slot 1: filament run out.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8002" to HmsInfo(
            message = "AMS 1 slot 2: filament run out.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8003" to HmsInfo(
            message = "AMS 1 slot 3: filament run out.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8004" to HmsInfo(
            message = "AMS 1 slot 4: filament run out.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-1100" to HmsInfo(
            message = "AMS 1 slot 1: filament tangle detected.",
            alertLevel = HmsAlertLevel.Error,
        ),

        // Module 07 (0700) — Toolhead / motion
        "0700-4001" to HmsInfo(
            message = "Toolhead homing failed.",
            alertLevel = HmsAlertLevel.Error,
        ),

        // Module 02 (0200) — Extruder / hot-end
        "0200-4003" to HmsInfo(
            message = "Nozzle clog detected.",
            alertLevel = HmsAlertLevel.Error,
        ),
    )
}
