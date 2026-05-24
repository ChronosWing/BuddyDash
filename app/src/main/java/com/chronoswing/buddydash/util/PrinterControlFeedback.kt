package com.chronoswing.buddydash.util

import android.util.Log

private const val TAG_CONTROL = "BuddyDash/Control"

enum class ControlAction {
    HomeAxes,
    BedJog,
    ChamberLight,
    PrintSpeed,
    Pause,
    Resume,
    Stop,
    SmartPlugOn,
    SmartPlugOff,
}

data class ControlFeedback(
    val action: ControlAction,
    val success: Boolean,
    /** Full API / exception detail for logging only. */
    val logDetail: String? = null,
)

fun logControlFailure(action: ControlAction, printerId: Int, detail: String?, cause: Throwable?) {
    Log.e(
        TAG_CONTROL,
        "${action.name} failed printer=$printerId detail=${detail ?: cause?.message ?: "unknown"}",
        cause,
    )
}
