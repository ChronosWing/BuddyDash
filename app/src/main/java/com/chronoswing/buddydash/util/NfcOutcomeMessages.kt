package com.chronoswing.buddydash.util

import android.content.Context
import com.chronoswing.buddydash.R

/** User-visible message for an [NfcActionOutcome], or null when silent (e.g. debounced). */
fun resolveNfcActionOutcomeMessage(context: Context, outcome: NfcActionOutcome): String? =
    when (outcome) {
        NfcActionOutcome.Debounced -> null
        is NfcActionOutcome.PlateCleared ->
            context.getString(R.string.nfc_plate_cleared, outcome.printerName)
        NfcActionOutcome.PlateAlreadyClear ->
            context.getString(R.string.nfc_plate_already_clear)
        NfcActionOutcome.PrinterBusyPlateUnchanged ->
            context.getString(R.string.nfc_printer_busy_plate)
        is NfcActionOutcome.PowerOn ->
            context.getString(R.string.nfc_power_on, outcome.printerName)
        is NfcActionOutcome.PowerOff ->
            context.getString(R.string.nfc_power_off, outcome.printerName)
        NfcActionOutcome.PrinterBusyPowerUnchanged ->
            context.getString(R.string.nfc_printer_busy_power)
        NfcActionOutcome.SmartOutletUnavailable ->
            context.getString(R.string.nfc_outlet_unavailable)
        NfcActionOutcome.SmartOutletStateUnknown ->
            context.getString(R.string.nfc_outlet_state_unknown)
        NfcActionOutcome.FinishedWithPowerOff ->
            context.getString(R.string.nfc_finished_power_off)
        NfcActionOutcome.FinishedPlateClear ->
            context.getString(R.string.nfc_finished_plate_clear)
        NfcActionOutcome.PrinterBusyFinishSkipped ->
            context.getString(R.string.nfc_printer_busy_finish)
        NfcActionOutcome.InvalidLink ->
            context.getString(R.string.nfc_invalid_link)
        NfcActionOutcome.MissingCredentials ->
            context.getString(R.string.nfc_configure_first)
        NfcActionOutcome.ConnectionRequired ->
            context.getString(R.string.nfc_connection_required)
        NfcActionOutcome.PrinterNotFound ->
            context.getString(R.string.nfc_printer_not_found)
        NfcActionOutcome.ApiFailed ->
            context.getString(R.string.nfc_action_failed)
    }
