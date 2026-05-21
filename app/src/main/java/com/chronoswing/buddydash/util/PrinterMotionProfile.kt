package com.chronoswing.buddydash.util

import android.util.Log
import com.chronoswing.buddydash.network.BambuddyApi

private const val TAG_MOTION = "BuddyDash/Motion"

/** UI layout for Bambuddy motion endpoints (OpenAPI v0.2.4: bed-jog Z only). */
enum class PrinterMotionLayout {
    /** bed-jog not available or printer offline. */
    Hidden,
    /** P/X/CoreXY and unknown models — Z jog labeled as bed up/down. */
    ZBedJog,
    /** A1 / A1 Mini bed slingers — same Z jog API, toolhead up/down labels. */
    BedSlingerZ,
}

fun resolveMotionLayout(printerModel: String?): PrinterMotionLayout {
    if (!BambuddyApi.hasBedJogEndpoint) return PrinterMotionLayout.Hidden
    val model = printerModel?.trim()?.uppercase().orEmpty()
    val layout = when {
        isBedSlingerModel(model) -> PrinterMotionLayout.BedSlingerZ
        else -> PrinterMotionLayout.ZBedJog
    }
    if (model.isNotEmpty()) {
        Log.d(TAG_MOTION, "motion layout=$layout model=$model")
    }
    return layout
}

private fun isBedSlingerModel(modelUpper: String): Boolean {
    if (modelUpper.isEmpty()) return false
    return modelUpper.contains("A1 MINI") ||
        modelUpper.contains("A1MINI") ||
        modelUpper == "A1" ||
        modelUpper.startsWith("A1 ") ||
        modelUpper.contains("A1 ")
}

fun motionDebugLog(action: String, printerId: Int, distanceMm: Float) {
    Log.d(TAG_MOTION, "bed-jog action=$action printer=$printerId distance=$distanceMm")
}
