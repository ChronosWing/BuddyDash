package com.chronoswing.buddydash.util

/** Home/detail printer card micro-animation mode. */
enum class CardMicroMotion {
    /** Idle — calm, static. */
    None,
    /** Paused — soft amber pulse. */
    Frozen,
    /** Idle / busy — barely perceptible ambient tone. */
    IdleAmbient,
    /** Printing — accent glow + progress sheen. */
    Printing,
    /** Just finished — brief soft success glow. */
    CompletedFlash,
    /** Error — slow red attention pulse. */
    ErrorAttention,
    /** Offline — muted/dimmed card. */
    OfflineMuted,
}

fun resolveCardMicroMotion(
    activityKind: PrinterActivityKind,
    rawState: String? = null,
): CardMicroMotion {
    val raw = rawState?.uppercase()
    if (raw == "FINISH") {
        return CardMicroMotion.CompletedFlash
    }
    return when (activityKind) {
        PrinterActivityKind.Printing -> CardMicroMotion.Printing
        PrinterActivityKind.Paused -> CardMicroMotion.Frozen
        PrinterActivityKind.Error -> CardMicroMotion.ErrorAttention
        PrinterActivityKind.Offline -> CardMicroMotion.OfflineMuted
        PrinterActivityKind.Idle, PrinterActivityKind.Busy -> CardMicroMotion.IdleAmbient
    }
}

/** Maps home/detail card motion to filament-slot glow motion (same state rules). */
fun CardMicroMotion.toFilamentGlowMotion(): FilamentGlowMotion = when (this) {
    CardMicroMotion.Printing -> FilamentGlowMotion.Breathing
    CardMicroMotion.Frozen -> FilamentGlowMotion.Frozen
    CardMicroMotion.IdleAmbient -> FilamentGlowMotion.SoftIdle
    CardMicroMotion.ErrorAttention -> FilamentGlowMotion.Frozen
    CardMicroMotion.None,
    CardMicroMotion.CompletedFlash,
    CardMicroMotion.OfflineMuted,
    -> FilamentGlowMotion.None
}
