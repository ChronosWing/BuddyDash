package com.chronoswing.buddydash.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.ui.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val FULLSCREEN_SNAPSHOT_REFRESH_MS = 5_000L

@Composable
fun PrinterCameraFullscreenDialog(
    visible: Boolean,
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    onDismiss: () -> Unit,
    chamberLightOn: Boolean? = null,
    canToggleLight: Boolean = false,
    onToggleLight: (() -> Unit)? = null,
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
            var refreshTick by remember(printerId) {
                mutableLongStateOf(System.currentTimeMillis())
            }
            var isSnapshotLoading by remember { mutableStateOf(true) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val isLandscape = rememberPhysicalLandscapeHeld()

            ImmersiveSystemBars(enabled = isLandscape)

            LaunchedEffect(lifecycleOwner, printerId) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isActive) {
                        delay(FULLSCREEN_SNAPSHOT_REFRESH_MS)
                        refreshTick = System.currentTimeMillis()
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                CameraSnapshotFitHost(
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    printerId = printerId,
                    refreshTick = refreshTick,
                    onLoadingChanged = { isSnapshotLoading = it },
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                        .then(
                            if (isLandscape) {
                                Modifier
                            } else {
                                Modifier
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            },
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White.copy(alpha = 0.92f),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSnapshotLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(end = 4.dp),
                                strokeWidth = 2.dp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                        IconButton(
                            onClick = { refreshTick = System.currentTimeMillis() },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh_status),
                                tint = Color.White.copy(alpha = 0.88f),
                            )
                        }
                        if (canToggleLight && chamberLightOn != null && onToggleLight != null) {
                            IconButton(onClick = onToggleLight) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = stringResource(
                                        if (chamberLightOn) R.string.light_off else R.string.light_on,
                                    ),
                                    tint = if (chamberLightOn) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                                    } else {
                                        Color.White.copy(alpha = 0.75f)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSnapshotFitHost(
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    refreshTick: Long,
    onLoadingChanged: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        PrinterLiveCameraSnapshot(
            serverUrl = serverUrl,
            cameraToken = cameraToken,
            printerId = printerId,
            refreshTick = refreshTick,
            modifier = Modifier.fillMaxSize(),
            fillMaxSize = true,
            contentScale = ContentScale.Fit,
            applyHeroScrim = false,
            backgroundColor = Color.Black,
            onLoadingChanged = onLoadingChanged,
        )
    }
}

/**
 * Opens in portrait. Switches to landscape activity orientation only when the device
 * is physically held in landscape (not on dialog open).
 */
@Composable
private fun rememberPhysicalLandscapeHeld(): Boolean {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val heldLandscape = remember { mutableStateOf(false) }

    DisposableEffect(context, activity) {
        val host = activity
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val landscape = orientation in 45..135 || orientation in 225..315
                if (landscape == heldLandscape.value) return
                heldLandscape.value = landscape
                host?.requestedOrientation = if (landscape) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
        heldLandscape.value = false
        host?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
        onDispose {
            listener.disable()
            host?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    return heldLandscape.value
}

@Composable
private fun ImmersiveSystemBars(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled, view) {
        if (!view.isInEditMode) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                if (enabled) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
                onDispose {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            } else {
                onDispose { }
            }
        } else {
            onDispose { }
        }
    }
}
