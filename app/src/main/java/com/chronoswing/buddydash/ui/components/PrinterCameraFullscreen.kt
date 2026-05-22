package com.chronoswing.buddydash.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.chronoswing.buddydash.R
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
            androidx.compose.runtime.LaunchedEffect(lifecycleOwner, printerId) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isActive) {
                        delay(FULLSCREEN_SNAPSHOT_REFRESH_MS)
                        refreshTick = System.currentTimeMillis()
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
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
                    onLoadingChanged = { isSnapshotLoading = it },
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
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
