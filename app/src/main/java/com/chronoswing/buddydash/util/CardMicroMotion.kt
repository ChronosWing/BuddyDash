package com.chronoswing.buddydash.util

/** Home printer card micro-animation mode. */
enum class CardMicroMotion {
    /** Error, offline, failed — no motion. */
    None,
    /** Paused — visually frozen. */
    Frozen,
    /** Idle / busy — barely perceptible ambient tone. */
    IdleAmbient,
    /** Actively printing — progress sheen, chip breath, thumbnail life. */
    Printing,
    /** Just finished — brief soft success glow, then static. */
    CompletedFlash,
}

fun resolveCardMicroMotion(
    activityKind: PrinterActivityKind,
    rawState: String? = null,
): CardMicroMotion {
    val raw = rawState?.uppercase()
    if (raw == "FAILED" || activityKind == PrinterActivityKind.Error ||
        activityKind == PrinterActivityKind.Offline
    ) {
        return CardMicroMotion.None
    }
    if (raw == "PAUSE" || activityKind == PrinterActivityKind.Paused) {
        return CardMicroMotion.Frozen
    }
    if (raw == "FINISH") {
        return CardMicroMotion.CompletedFlash
    }
    return when (activityKind) {
        PrinterActivityKind.Printing -> CardMicroMotion.Printing
        PrinterActivityKind.Idle, PrinterActivityKind.Busy -> CardMicroMotion.IdleAmbient
        PrinterActivityKind.Paused -> CardMicroMotion.Frozen
        PrinterActivityKind.Error, PrinterActivityKind.Offline -> CardMicroMotion.None
    }
}

/** Maps home/detail card motion to filament-slot glow motion (same state rules). */
fun CardMicroMotion.toFilamentGlowMotion(): FilamentGlowMotion = when (this) {
    CardMicroMotion.Printing -> FilamentGlowMotion.Breathing
    CardMicroMotion.Frozen -> FilamentGlowMotion.Frozen
    CardMicroMotion.IdleAmbient -> FilamentGlowMotion.SoftIdle
    CardMicroMotion.None, CardMicroMotion.CompletedFlash -> FilamentGlowMotion.None
}
