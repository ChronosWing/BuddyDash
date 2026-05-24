package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.data.model.PrinterHmsError

/**
 * Lookup table for Bambu HMS / Error-Code database.
 *
 * Source: Bambu Lab error-code wiki (wiki.bambulab.com/en/hms/error-code) and HMS wiki
 * (wiki.bambulab.com/en/hms/home), retrieved May 2026.
 *
 * BambuBuddy's API sends HMS entries as (module: Int, code: "0xHEX").
 * The formatted display code is "[MODULE-CODE]", e.g. "[0500-C010]".
 *
 * Module encoding:
 *   module ≤ 0xFF  → "%02X00".format(module)  e.g. 5 → "0500", 12 → "0C00"
 *   module > 0xFF  → "%04X".format(module)     e.g. 0x1200 → "1200", 0x0701 → "0701"
 *
 * Severity heuristic from code second-segment first hex digit:
 *   4xxx → Error   (print stopped, must resolve before continuing)
 *   8xxx → Warning (print paused or needs attention)
 *   Cxxx → Warning (Notification per Bambu, but actionable — shown as Warning for clarity)
 *
 * The lookup table provides authoritative messages and overrides the heuristic when needed.
 * Unknown codes still show the formatted [XXXX-YYYY] code; only the message is missing.
 */
object BambuHmsLookup {

    data class HmsInfo(
        /** Human-readable message from Bambu Lab documentation. */
        val message: String,
        /** Alert level for this code. */
        val alertLevel: HmsAlertLevel,
        /**
         * Verified wiki URL for this code.
         * Null for most entries — only populate when a working URL is confirmed.
         * Do NOT auto-generate URLs; unverified links lead to 404 pages.
         */
        val wikiUrl: String? = null,
    )

    // ── Code formatting ──────────────────────────────────────────────────────

    /**
     * Returns (moduleHex4, codeHex4) display parts.
     *   module=5, rawCode="0xc010"  → ("0500", "C010")
     *   module=0x0701, rawCode="0x8011" → ("0701", "8011")
     */
    fun formatCodeParts(module: Int?, rawCode: String?): Pair<String?, String?> {
        val modulePart = module?.let {
            when {
                it <= 0xFF -> "%02X00".format(it)   // standard single-byte module: 5→"0500"
                it <= 0xFFFF -> "%04X".format(it)   // two-byte: 0x0701→"0701", 0x1200→"1200"
                else -> null
            }
        }
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

    // ── Severity inference from code hex prefix ──────────────────────────────

    /**
     * Infers alert level from the first hex digit of the 4-digit error code.
     * Only applies to 4-digit codes in the Bambu 2-segment error-code format.
     *
     * 4xxx → Error   (fatal, stops print)
     * 8xxx → Warning (pauses print or needs attention)
     * Cxxx → Warning (Bambu "Notification" level, but shown as Warning for UX clarity)
     */
    fun inferAlertLevelFromCodeHex(rawCode: String?): HmsAlertLevel? {
        val hex = rawCode?.removePrefix("0x")?.removePrefix("0X")?.uppercase() ?: return null
        if (hex.length != 4) return null
        return when (hex[0]) {
            '4' -> HmsAlertLevel.Error
            '8' -> HmsAlertLevel.Warning
            'C' -> HmsAlertLevel.Warning
            else -> null
        }
    }

    // ── Lookup ───────────────────────────────────────────────────────────────

    /** Returns HMS info for this (module, rawCode), or null when not in the table. */
    fun lookup(module: Int?, rawCode: String?): HmsInfo? {
        val (modulePart, codePart) = formatCodeParts(module, rawCode)
        val key = buildString {
            if (modulePart != null) append(modulePart)
            if (codePart != null) { if (isNotEmpty()) append("-"); append(codePart) }
        }
        return if (key.isEmpty()) null else table[key]
    }

    fun lookup(entry: PrinterHmsError): HmsInfo? = lookup(entry.module, entry.code)

    // ── Table ── source: Bambu Lab HMS wiki + error-code page (May 2026) ─────

    @Suppress("SpellCheckingInspection")
    private val table: Map<String, HmsInfo> = mapOf(

        // ══════════════════════════════════════════════════════════════════════
        // Module 03 (0300) — Motion Controller: bed, nozzle, homing, print flow
        // ══════════════════════════════════════════════════════════════════════

        "0300-4000" to HmsInfo(
            message = "Printing stopped because homing Z axis failed.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-4001" to HmsInfo(
            message = "The printer timed out waiting for the nozzle to cool down before homing.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-4002" to HmsInfo(
            message = "Printing stopped because Auto Bed Leveling failed.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-4005" to HmsInfo(
            message = "The nozzle fan speed is abnormal.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-4006" to HmsInfo(
            message = "The nozzle is clogged.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-4008" to HmsInfo(
            message = "The AMS failed to change filament.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-4009" to HmsInfo(
            message = "Homing XY axis failed.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-400A" to HmsInfo(
            message = "Mechanical resonance frequency identification failed.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-400B" to HmsInfo(
            message = "Internal communication exception.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-400C" to HmsInfo(
            message = "Printing was cancelled.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-400E" to HmsInfo(
            message = "The motor self-check failed.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0300-8001" to HmsInfo(
            message = "Printing was paused by the user. Tap \"Resume\" to continue.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8002" to HmsInfo(
            message = "First layer defects were detected by the Micro Lidar. Please check the quality of the printed model before continuing.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8003" to HmsInfo(
            message = "Spaghetti defects were detected by AI Print Monitoring. Please check the quality of the printed model before continuing.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8004" to HmsInfo(
            message = "Filament ran out. Please load new filament.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8005" to HmsInfo(
            message = "Toolhead front cover fell off. Please remount the front cover and check the print.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8007" to HmsInfo(
            message = "There was an unfinished print job when the printer lost power. If the model is still adhered to the build plate, you can try resuming.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8008" to HmsInfo(
            message = "Printing stopped due to a nozzle temperature problem.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8009" to HmsInfo(
            message = "Heatbed temperature malfunction.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-800A" to HmsInfo(
            message = "A filament pile-up was detected by AI Print Monitoring. Please clean the filament from the waste chute.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-800B" to HmsInfo(
            message = "The cutter is stuck. Please make sure the cutter handle is out.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-800D" to HmsInfo(
            message = "Some objects may have fallen, or the extruder is not extruding normally. Check the print and tap \"Resume\" if acceptable.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-800F" to HmsInfo(
            message = "The door appears to be open, so printing was paused.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8010" to HmsInfo(
            message = "Printing stopped because the hotend fan speed is abnormal.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8011" to HmsInfo(
            message = "Detected build plate is not the same as in the G-code file. Please adjust slicer settings or use the correct plate.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8013" to HmsInfo(
            message = "Printing was paused. Tap \"Resume\" to continue.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8015" to HmsInfo(
            message = "The filament has run out. Please load new filament in the filament page.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8017" to HmsInfo(
            message = "Foreign objects detected on the hotbed. Please check and clean the hotbed, then tap \"Resume\".",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8018" to HmsInfo(
            message = "Chamber temperature malfunction.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0300-8019" to HmsInfo(
            message = "No build plate is placed.",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // ══════════════════════════════════════════════════════════════════════
        // Module 05 (0500) — Main Board / AP: SD card, files, network, firmware
        // ══════════════════════════════════════════════════════════════════════

        "0500-400A" to HmsInfo(
            message = "The file name is not supported. Please rename and restart the print job.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-400B" to HmsInfo(
            message = "There was a problem downloading the file. Please check your network connection and resend the print job.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-400C" to HmsInfo(
            message = "Please insert a MicroSD card and restart the print job.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-400D" to HmsInfo(
            message = "Please run a self-test and restart the print job.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-400E" to HmsInfo(
            message = "Printing was cancelled.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4002" to HmsInfo(
            message = "Unsupported print file path or name. Please resend the print job.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4003" to HmsInfo(
            message = "Printing stopped because the printer was unable to parse the file. Please resend your print job.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4004" to HmsInfo(
            message = "The printer cannot receive new print jobs while printing. Resend after the current print finishes.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4005" to HmsInfo(
            message = "Print jobs cannot be sent while updating firmware.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4006" to HmsInfo(
            message = "Not enough free storage space for the print job. Restoring to factory settings can release space.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4007" to HmsInfo(
            message = "Print jobs cannot be sent during a force update or when a repair update is required.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4008" to HmsInfo(
            message = "Starting printing failed. Please power cycle the printer and resend the print job.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4009" to HmsInfo(
            message = "Print jobs cannot be sent while updating logs.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4014" to HmsInfo(
            message = "Slicing for the print job failed. Please check your settings and restart the print job.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4015" to HmsInfo(
            message = "Not enough free storage space. Please format or clean the MicroSD card to release space.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4016" to HmsInfo(
            message = "The MicroSD Card is write-protected. Please replace the MicroSD Card.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-401A" to HmsInfo(
            message = "Cloud access failed. Network instability or firewall restrictions may be the cause. Try moving the printer closer to the router.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-401C" to HmsInfo(
            message = "Cloud access is rejected. If this persists, please contact customer service.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-401D" to HmsInfo(
            message = "Cloud access failed, which may be caused by network instability or interference. Try moving the printer closer to the router.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4037" to HmsInfo(
            message = "The sliced file is not compatible with the current printer model. This file cannot be printed.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-4038" to HmsInfo(
            message = "The nozzle diameter in the sliced file is not consistent with the current nozzle setting. This file cannot be printed.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-402E" to HmsInfo(
            message = "The system does not support the MicroSD card's current file system. Please format the MicroSD card to FAT32.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-402F" to HmsInfo(
            message = "The MicroSD card sector data is damaged. Please use an SD card repair tool to repair or format it. If still unreadable, replace the card.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-402A" to HmsInfo(
            message = "Failed to connect to the router. This may be caused by wireless interference or distance. Try again or move the printer closer to the router.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-402B" to HmsInfo(
            message = "Router connection failed due to incorrect password. Please check the password and try again.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-403A" to HmsInfo(
            message = "The current temperature is too low. Printing and axis movement are disabled. Please move the printer to an environment above 10°C.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0500-8036" to HmsInfo(
            message = "Your sliced file is not consistent with the current printer model.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0500-C010" to HmsInfo(
            message = "MicroSD Card read/write exception: please reinsert or replace the MicroSD Card.",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // ══════════════════════════════════════════════════════════════════════
        // Module 07 (0700) — AMS unit 1 (and 0701/0702/0703 unit 2/3/4)
        // ══════════════════════════════════════════════════════════════════════

        "0700-4001" to HmsInfo(
            message = "The AMS has been disabled for a print, but it still has filament loaded. Please unload the AMS filament and switch to the spool holder for printing.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0700-8003" to HmsInfo(
            message = "Failed to pull out the filament from the extruder. This might be caused by a clogged extruder or filament broken inside the extruder.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0700-8004" to HmsInfo(
            message = "AMS failed to pull back filament. This could be due to a stuck spool or the end of the filament being stuck in the path.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0700-8005" to HmsInfo(
            message = "The AMS failed to send out filament. You can clip the end of your filament flat and reinsert. If this persists, check the PTFE tubes in the AMS for wear.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0700-8006" to HmsInfo(
            message = "Unable to feed filament into the extruder. This could be due to entangled filament or a stuck spool. Check if the AMS PTFE tube is connected.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0700-8007" to HmsInfo(
            message = "Extruding filament failed. The extruder might be clogged.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0700-8010" to HmsInfo(
            message = "The AMS assist motor is overloaded. This could be due to entangled filament or a stuck spool.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0700-8011" to HmsInfo(
            message = "AMS filament ran out. Please insert new filament into the same AMS slot.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0700-8012" to HmsInfo(
            message = "Failed to get AMS mapping table. Please click \"Retry\" to continue.",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // AMS unit 2 (0701 = module 0x0701 = 1793)
        "0701-4001" to HmsInfo(
            message = "Filament is still loaded from the AMS (unit 2) after it has been disabled. Please unload the filament, load from the spool holder, and restart printing.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "0701-8003" to HmsInfo(
            message = "Failed to pull out the filament from the extruder (AMS unit 2). There may be an extruder clog or broken filament.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0701-8004" to HmsInfo(
            message = "AMS (unit 2) failed to pull back filament. Please check if the filament or spool is stuck.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0701-8005" to HmsInfo(
            message = "The AMS (unit 2) failed to send filament. Clip the filament flat and reinsert, or check the PTFE tubes.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0701-8006" to HmsInfo(
            message = "Unable to feed filament from AMS unit 2 into the extruder. Check for entangled filament or a stuck spool.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0701-8007" to HmsInfo(
            message = "Failed to extrude the filament (AMS unit 2). Please check if the extruder is clogged.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0701-8010" to HmsInfo(
            message = "AMS (unit 2) assist motor is overloaded. Check for entangled filament or a stuck spool.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0701-8011" to HmsInfo(
            message = "AMS (unit 2) filament ran out. Please insert new filament into the same AMS slot.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0701-8012" to HmsInfo(
            message = "Failed to get AMS mapping table (unit 2). Please click \"Retry\" to continue.",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // AMS unit 3/4 (0702/0703)
        "0702-8010" to HmsInfo(
            message = "AMS (unit 3) assist motor is overloaded. Check for entangled filament or a stuck spool.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0702-8011" to HmsInfo(
            message = "AMS (unit 3) filament ran out. Please insert new filament.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0703-8010" to HmsInfo(
            message = "AMS (unit 4) assist motor is overloaded. Check for entangled filament or a stuck spool.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0703-8011" to HmsInfo(
            message = "AMS (unit 4) filament ran out. Please insert new filament.",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // ══════════════════════════════════════════════════════════════════════
        // Module 0C (0C00 = 12) — XCAM / Bambu Micro Lidar
        // ══════════════════════════════════════════════════════════════════════

        "0C00-8001" to HmsInfo(
            message = "First layer defects were detected. If the defects are acceptable, tap \"Resume\" to continue.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0C00-800A" to HmsInfo(
            message = "The detected build plate is not the same as in the G-code file.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0C00-C003" to HmsInfo(
            message = "Possible defects were detected in the first layer.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "0C00-C004" to HmsInfo(
            message = "Possible spaghetti failure was detected.",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // ══════════════════════════════════════════════════════════════════════
        // Module 10 (1000 = 16) — Bambu Studio notifications
        // ══════════════════════════════════════════════════════════════════════

        "1000-C001" to HmsInfo(
            message = "High bed temperature may lead to filament clogging in the nozzle. Please ensure ventilation.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1000-C002" to HmsInfo(
            message = "Printing CF/GF material with a stainless steel nozzle may cause nozzle damage.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1000-C003" to HmsInfo(
            message = "Enabling traditional timelapse might lead to print defects. Please enable it as needed.",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // ══════════════════════════════════════════════════════════════════════
        // Module 1001 = timelapse / feature flags
        // ══════════════════════════════════════════════════════════════════════

        "1001-C001" to HmsInfo(
            message = "Timelapse is not supported because Spiral Vase mode is enabled in the slicing presets.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1001-C002" to HmsInfo(
            message = "Timelapse is not supported because Print Sequence is set to \"By object\".",
            alertLevel = HmsAlertLevel.Warning,
        ),

        // ══════════════════════════════════════════════════════════════════════
        // Module 12 (1200 = 18) — AMS Lite
        // ══════════════════════════════════════════════════════════════════════

        "1200-4001" to HmsInfo(
            message = "Filament is still loaded from the AMS when it has been disabled. Please unload AMS filament, load from the spool holder, and restart.",
            alertLevel = HmsAlertLevel.Error,
        ),
        "1200-8001" to HmsInfo(
            message = "Cutting the filament failed. Please check if the cutter is stuck.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1200-8003" to HmsInfo(
            message = "Failed to pull out the filament from the extruder. Check whether the extruder is clogged or filament is broken inside.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1200-8004" to HmsInfo(
            message = "Failed to pull back the filament from the toolhead. Check if the filament is stuck.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1200-8005" to HmsInfo(
            message = "Failed to feed the filament. Please load the filament, then click \"Retry\".",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1200-8007" to HmsInfo(
            message = "Failed to extrude the filament. The extruder may be clogged or the filament may be stuck.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1200-8010" to HmsInfo(
            message = "The filament or spool may be stuck.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1200-8011" to HmsInfo(
            message = "AMS Lite filament ran out. Please insert new filament into the same AMS slot.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1200-8012" to HmsInfo(
            message = "Failed to get AMS mapping table. Please click \"Retry\" to continue.",
            alertLevel = HmsAlertLevel.Warning,
        ),
        "1200-8015" to HmsInfo(
            message = "Failed to pull out the filament from the toolhead. Check if the filament is stuck or broken inside the extruder or PTFE tube.",
            alertLevel = HmsAlertLevel.Warning,
        ),
    )

    private const val SHARED_HMS_WIKI_URL = "https://wiki.bambulab.com/en/hms/error-code"
    private val LONG_HMS_CODE_PATTERN = Regex("""^[0-9A-Fa-f]{4}([_-][0-9A-Fa-f]{4}){3}$""")

    /**
     * Resolves a wiki URL for [entry].
     * Priority: verified lookup URL → model-specific 16-digit page → shared error-code page.
     */
    fun resolveWikiUrl(entry: PrinterHmsError, printerModel: String?): String? {
        lookup(entry)?.wikiUrl?.let { return it }
        val rawCode = entry.code.trim()
        if (rawCode.isBlank()) return SHARED_HMS_WIKI_URL
        if (isLongHmsCode(rawCode)) {
            val slug = resolvePrinterModelWikiSlug(printerModel) ?: return SHARED_HMS_WIKI_URL
            return "https://wiki.bambulab.com/en/$slug/troubleshooting/hmscode/$rawCode"
        }
        return SHARED_HMS_WIKI_URL
    }

    /** 16-digit HMS codes such as 0300-8001-0001-0001 (dashes or underscores preserved). */
    fun isLongHmsCode(code: String): Boolean = LONG_HMS_CODE_PATTERN.matches(code.trim())

    /**
     * Maps a Bambuddy printer model string to a Bambu wiki slug.
     * Returns null for unknown models — callers should fall back to [SHARED_HMS_WIKI_URL].
     */
    fun resolvePrinterModelWikiSlug(model: String?): String? {
        if (model.isNullOrBlank()) return null
        val upper = model.uppercase()
        return when {
            upper.contains("H2D") -> "h2d"
            upper.contains("A1") -> "a1"
            upper.contains("P1") -> "p1"
            upper.contains("X1") -> "x1"
            else -> null
        }
    }
}
