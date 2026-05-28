package com.chronoswing.buddydash.util

import kotlin.math.roundToInt

fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }

fun Float?.finiteOrNull(): Float? = this?.takeIf { it.isFinite() }

fun Double.finiteOrNull(): Double? = takeIf { isFinite() }

fun Float.finiteOrNull(): Float? = takeIf { isFinite() }

fun Double?.finiteOrZero(): Double = finiteOrNull() ?: 0.0

fun Float?.finiteOrZero(): Float = finiteOrNull() ?: 0f

/** Percentage value in 0..100, or null when missing/invalid. */
fun Float?.percentOrNull(): Float? = finiteOrNull()?.takeIf { it in 0f..100f }

fun Double?.percentOrNull(): Float? = finiteOrNull()?.toFloat()?.takeIf { it in 0f..100f }

/** Unit interval 0..1 for progress bars, or null when invalid. */
fun Float?.unitFractionOrNull(): Float? = finiteOrNull()?.takeIf { it in 0f..1f }

/** Converts a 0..100 progress percentage to a 0..1 bar fraction. */
fun Float?.percentToUnitFractionOrNull(): Float? = percentOrNull()?.div(100f)

fun Float.clampFinite(min: Float, max: Float, fallback: Float = min): Float =
    if (!isFinite()) fallback else coerceIn(min, max)

fun Double.clampFinite(min: Double, max: Double, fallback: Double = min): Double =
    if (!isFinite()) fallback else coerceIn(min, max)

fun Float?.clampFinite(min: Float, max: Float, fallback: Float = min): Float =
    if (this == null || !isFinite()) fallback else coerceIn(min, max)

fun Double?.roundToIntOrNull(): Int? = finiteOrNull()?.roundToInt()

fun Float?.roundToIntOrNull(): Int? = finiteOrNull()?.roundToInt()

/** Filters sparkline/history samples to finite, non-negative values. */
fun List<Float>.finiteSamplesOrEmpty(): List<Float> =
    mapNotNull { value -> value.finiteOrNull()?.takeIf { it >= 0f } }
