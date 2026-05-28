package com.chronoswing.buddydash.util

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class NfcOutcomeHapticsTest {

    @Test
    fun successTierMapsToConfirmFeedback() {
        assertEquals(
            HapticFeedbackType.Confirm,
            NfcActionOutcome.Tier.Success.toComposeHapticType(),
        )
    }

    @Test
    fun noopTierMapsToLightFeedback() {
        assertEquals(
            HapticFeedbackType.TextHandleMove,
            NfcActionOutcome.Tier.Noop.toComposeHapticType(),
        )
    }

    @Test
    fun warningAndFailureTiersMapToRejectFeedback() {
        assertEquals(
            HapticFeedbackType.Reject,
            NfcActionOutcome.Tier.Warning.toComposeHapticType(),
        )
        assertEquals(
            HapticFeedbackType.Reject,
            NfcActionOutcome.Tier.Failure.toComposeHapticType(),
        )
    }
}
