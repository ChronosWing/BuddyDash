package com.chronoswing.buddydash.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.content.ContextCompat

/** Subtle haptic aligned with NFC / Home action feedback tiers. Never throws. */
fun performNfcOutcomeHaptic(hapticFeedback: HapticFeedback, tier: NfcActionOutcome.Tier) {
    safeHaptic {
        hapticFeedback.performHapticFeedback(tier.toComposeHapticType())
    }
}

/** View-based haptic for Activities and other non-Compose surfaces. Never throws. */
fun performNfcOutcomeHaptic(view: View?, tier: NfcActionOutcome.Tier) {
    performViewHaptic(view, tier.toViewHapticConstant())
}

/** Context fallback: prefers the Activity decor view; vibrator only as last resort. Never throws. */
fun performNfcOutcomeHaptic(context: Context, tier: NfcActionOutcome.Tier) {
    val view = (context as? Activity)?.window?.decorView
    if (view != null) {
        performNfcOutcomeHaptic(view, tier)
    } else {
        performVibratorFallback(context, tier)
    }
}

fun performLongPressHaptic(view: View?) {
    performViewHaptic(view, HapticFeedbackConstants.LONG_PRESS)
}

fun performLongPressHaptic(hapticFeedback: HapticFeedback) {
    safeHaptic {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

fun performViewHaptic(view: View?, feedbackConstant: Int) {
    if (view == null) return
    safeHaptic {
        view.performHapticFeedback(
            feedbackConstant,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
        )
    }
}

internal fun NfcActionOutcome.Tier.toComposeHapticType(): HapticFeedbackType = when (this) {
    NfcActionOutcome.Tier.Success -> HapticFeedbackType.Confirm
    NfcActionOutcome.Tier.Noop -> HapticFeedbackType.TextHandleMove
    NfcActionOutcome.Tier.Warning -> HapticFeedbackType.Reject
    NfcActionOutcome.Tier.Failure -> HapticFeedbackType.Reject
}

internal fun NfcActionOutcome.Tier.toViewHapticConstant(): Int = when (this) {
    NfcActionOutcome.Tier.Success ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
    NfcActionOutcome.Tier.Noop -> HapticFeedbackConstants.CLOCK_TICK
    NfcActionOutcome.Tier.Warning ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
    NfcActionOutcome.Tier.Failure ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
}

private inline fun safeHaptic(block: () -> Unit) {
    try {
        block()
    } catch (_: SecurityException) {
        // Permission denied or haptics unavailable — skip silently.
    } catch (_: RuntimeException) {
        // Some OEM implementations throw on unsupported feedback types.
    }
}

private fun performVibratorFallback(context: Context, tier: NfcActionOutcome.Tier) {
    if (!context.hasVibratePermission()) return
    val vibrator = context.getVibrator() ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !vibrator.hasVibrator()) return

    safeHaptic {
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
}

private fun Context.hasVibratePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, android.Manifest.permission.VIBRATE) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.getVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
