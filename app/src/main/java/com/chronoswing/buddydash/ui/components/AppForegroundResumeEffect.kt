package com.chronoswing.buddydash.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.chronoswing.buddydash.util.BuddyDashDebug

private const val TAG_LIFECYCLE = "BuddyDash/Lifecycle"

/**
 * Fires [onForegroundResume] when the app process returns to the foreground (ON_RESUME).
 * Does not run on every destination resume — use navigation observers for screen-level refresh.
 */
@Composable
fun AppForegroundResumeEffect(
    onForegroundResume: () -> Unit,
) {
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (BuddyDashDebug.enabled) {
                    Log.d(TAG_LIFECYCLE, "lifecycle event=ON_RESUME (app foreground)")
                }
                onForegroundResume()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
