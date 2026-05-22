package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            var refreshTick by remember(printerId) {
                mutableLongStateOf(System.currentTimeMillis())
            }
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
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
