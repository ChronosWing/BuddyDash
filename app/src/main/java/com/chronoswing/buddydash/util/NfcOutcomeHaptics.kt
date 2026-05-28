package com.chronoswing.buddydash.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Subtle vibration aligned with NFC deep-link action feedback tiers. */
fun performNfcOutcomeHaptic(context: Context, tier: NfcActionOutcome.Tier) {
    val vibrator = context.getVibrator() ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = when (tier) {
            NfcActionOutcome.Tier.Success ->
                VibrationEffect.createOneShot(35, 80)
            NfcActionOutcome.Tier.Noop ->
                VibrationEffect.createOneShot(20, 40)
            NfcActionOutcome.Tier.Warning ->
                VibrationEffect.createOneShot(50, 140)
            NfcActionOutcome.Tier.Failure ->
                VibrationEffect.createOneShot(60, 180)
        }
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        val ms = when (tier) {
            NfcActionOutcome.Tier.Success -> 35L
            NfcActionOutcome.Tier.Noop -> 20L
            NfcActionOutcome.Tier.Warning -> 50L
            NfcActionOutcome.Tier.Failure -> 60L
        }
        @Suppress("DEPRECATION")
        vibrator.vibrate(ms)
    }
}

private fun Context.getVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
