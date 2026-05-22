package com.chronoswing.buddydash.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.chronoswing.buddydash.network.printerCameraSnapshotUrl
import com.chronoswing.buddydash.network.printerCameraStreamUrl
import com.chronoswing.buddydash.ui.findActivity

private const val CAMERA_STREAM_FPS = 10

@Composable
fun PrinterCameraFullscreenDialog(
    visible: Boolean,
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    onDismiss: () -> Unit,
    onStopCameraStream: () -> Unit = {},
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
            val streamUrl = remember(serverUrl, printerId, cameraToken) {
                printerCameraStreamUrl(serverUrl, printerId, cameraToken, fps = CAMERA_STREAM_FPS)
            }
            var streamFailed by remember(streamUrl) { mutableStateOf(streamUrl == null) }
            var showLoadingIndicator by remember(streamUrl) { mutableStateOf(streamUrl != null) }
            var snapshotRefreshTick by remember(printerId) {
                mutableLongStateOf(System.currentTimeMillis())
            }
            CameraOrientationWhileOpen()
            val configuration = LocalConfiguration.current
            val isLandscape =
                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val lifecycleOwner = LocalLifecycleOwner.current
            val scope = rememberCoroutineScope()

            LaunchedEffect(streamFailed) {
                if (streamFailed) showLoadingIndicator = true
            }

            LaunchedEffect(streamFailed, lifecycleOwner, printerId) {
                if (!streamFailed) return@LaunchedEffect
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isActive) {
                        delay(5_000L)
                        snapshotRefreshTick = System.currentTimeMillis()
                    }
                }
            }

            DisposableEffect(Unit) {
                onDispose { onStopCameraStream() }
            }

            ImmersiveSystemBars(enabled = isLandscape)

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    streamUrl != null && !streamFailed -> {
                        CameraViewerFrame {
                            PrinterCameraMjpegStream(
                                streamUrl = streamUrl,
                                modifier = Modifier.fillMaxSize(),
                                onStreamFailed = {
                                    scope.launch { streamFailed = true }
                                },
                                onLoadingChanged = { loading ->
                                    if (!loading) showLoadingIndicator = false
                                },
                            )
                        }
                    }
                    streamUrl != null && streamFailed -> {
                        CameraSnapshotFallback(
                            serverUrl = serverUrl,
                            cameraToken = cameraToken,
                            printerId = printerId,
                            refreshTick = snapshotRefreshTick,
                            onLoadingChanged = { loading ->
                                if (!loading) showLoadingIndicator = false
                            },
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.camera_stream_unavailable),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
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
                        if (showLoadingIndicator) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(end = 4.dp),
                                strokeWidth = 2.dp,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                        if (streamFailed) {
                            IconButton(
                                onClick = { snapshotRefreshTick = System.currentTimeMillis() },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh_status),
                                    tint = Color.White.copy(alpha = 0.88f),
                                )
                            }
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
private fun CameraViewerFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Polling fallback when MJPEG stream cannot be displayed. */
@Composable
private fun CameraSnapshotFallback(
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    refreshTick: Long,
    onLoadingChanged: (Boolean) -> Unit,
) {
    val snapshotUrl = remember(serverUrl, printerId, cameraToken, refreshTick) {
        printerCameraSnapshotUrl(serverUrl, printerId, cameraToken, cacheBust = refreshTick)
    }
    if (snapshotUrl == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.camera_stream_unavailable),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    CameraViewerFrame {
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
            snapshotCrossfadeMs = 0,
            onLoadingChanged = onLoadingChanged,
        )
    }
}

/**
 * Opens in portrait, then allows sensor rotation while the camera viewer is visible.
 * [LocalConfiguration] drives landscape layout and immersive chrome.
 */
@Composable
private fun CameraOrientationWhileOpen() {
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(activity) {
        val host = activity ?: return@LaunchedEffect
        host.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        delay(350)
        host.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
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
