package com.chronoswing.buddydash.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Polls [onPoll] while this destination is RESUMED (visible, app in foreground).
 * Cancels when navigating away or when the app backgrounds.
 */
@Composable
fun LifecyclePollingEffect(
    enabled: Boolean,
    intervalMs: Long,
    onPoll: () -> Unit,
    /** Delay before the first poll after resume (avoids doubling with initial load). */
    initialDelayMs: Long = intervalMs,
    /** When true, polls immediately on resume then waits [intervalMs] between polls. */
    pollImmediately: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()

    LaunchedEffect(enabled, intervalMs, initialDelayMs, pollImmediately, lifecycleOwner, lifecycleState) {
        if (!enabled) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!pollImmediately && initialDelayMs > 0L) {
                delay(initialDelayMs)
            }
            while (isActive) {
                onPoll()
                delay(intervalMs)
            }
        }
    }
}
