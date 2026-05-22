package com.chronoswing.buddydash.ui.components

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.network.normalizeBambuddyBaseUrl
import com.chronoswing.buddydash.network.printerCameraSnapshotUrl
import com.chronoswing.buddydash.network.printerCoverUrl
import com.chronoswing.buddydash.util.CardMicroMotion

private const val DEBUG_LOG_CAMERA = true
private const val TAG_CAMERA = "BuddyDash/Camera"

/** Refresh interval while detail Status tab is visible (RESUMED). */
private const val SNAPSHOT_REFRESH_MS = 7_000L

private const val SNAPSHOT_CROSSFADE_MS = 200

private val HeroScrimGradient = Brush.verticalGradient(
    0f to Color.Transparent,
    0.5f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.38f),
)

/**
 * Large hero image for the detail Status tab: live camera snapshot when available,
 * otherwise print cover. Invokes [onCameraHeroActive] when the camera image is showing.
 */
@Composable
fun DetailStatusHeroImage(
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    motion: CardMicroMotion = CardMicroMotion.None,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    onCameraHeroActive: (Boolean) -> Unit = {},
) {
    val coverAvailable = remember(serverUrl, printerId, cameraToken) {
        printerCoverUrl(serverUrl, printerId, cameraToken) != null
    }
    val canTryCamera = remember(serverUrl, printerId, cameraToken) {
        BambuddyApi.hasCameraEndpoint &&
            cameraToken.isNotBlank() &&
            printerId >= 0 &&
            normalizeBambuddyBaseUrl(serverUrl) != null
    }
    var cameraFailed by remember(printerId, serverUrl, cameraToken) {
        mutableStateOf(false)
    }
    val showCamera = canTryCamera && !cameraFailed
    var refreshTick by remember(printerId) {
        mutableLongStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(showCamera) {
        onCameraHeroActive(showCamera)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(showCamera, lifecycleOwner, printerId) {
        if (!showCamera) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(SNAPSHOT_REFRESH_MS)
                refreshTick = System.currentTimeMillis()
            }
        }
    }

    val heroContent: @Composable () -> Unit = {
        when {
            showCamera -> {
                PrinterCameraSnapshotImage(
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    printerId = printerId,
                    refreshTick = refreshTick,
                    height = height,
                    fillMaxSize = false,
                    frameModifier = Modifier,
                    onLoadFailed = { cameraFailed = true },
                )
            }
            coverAvailable -> {
                PrinterCoverImage(
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    printerId = printerId,
                    height = height,
                )
            }
        }
    }

    if (!showCamera && !coverAvailable) return

    val frameModifier = modifier.fillMaxWidth()
    if (motion == CardMicroMotion.Printing) {
        MicroMotionThumbnailFrame(motion = motion, modifier = frameModifier) {
            heroContent()
        }
    } else {
        Box(modifier = frameModifier) {
            heroContent()
        }
    }
}

@Composable
fun PrinterLiveCameraSnapshot(
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    refreshTick: Long,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = false,
    height: Dp = 160.dp,
    onLoadFailed: () -> Unit = {},
) {
    PrinterCameraSnapshotImage(
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        refreshTick = refreshTick,
        height = height,
        fillMaxSize = fillMaxSize,
        frameModifier = modifier,
        onLoadFailed = onLoadFailed,
    )
}

@Composable
private fun PrinterCameraSnapshotImage(
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    refreshTick: Long,
    height: Dp,
    fillMaxSize: Boolean = false,
    frameModifier: Modifier = Modifier,
    onLoadFailed: () -> Unit,
) {
    val imageUrl = remember(serverUrl, printerId, cameraToken, refreshTick) {
        printerCameraSnapshotUrl(serverUrl, printerId, cameraToken, cacheBust = refreshTick)
    }
    if (imageUrl == null) {
        LaunchedEffect(Unit) { onLoadFailed() }
        return
    }

    if (DEBUG_LOG_CAMERA) {
        LaunchedEffect(imageUrl) {
            Log.d(TAG_CAMERA, "Snapshot URL printerId=$printerId url=${redactImageToken(imageUrl)}")
        }
    }

    var lastPainter by remember(printerId) { mutableStateOf<Painter?>(null) }
    val context = LocalContext.current
    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(false)
            .listener(
                onSuccess = { _, _ ->
                    if (DEBUG_LOG_CAMERA) {
                        Log.d(TAG_CAMERA, "Snapshot load ok printerId=$printerId")
                    }
                },
                onError = { _, result ->
                    if (DEBUG_LOG_CAMERA) {
                        Log.d(
                            TAG_CAMERA,
                            "Snapshot load failed printerId=$printerId url=${redactImageToken(imageUrl)} " +
                                "error=${result.throwable.message}",
                        )
                    }
                },
            )
            .build()
    }
    val shape = RoundedCornerShape(10.dp)
    val resolvedFrame = if (fillMaxSize) {
        frameModifier.fillMaxSize().clip(shape)
    } else {
        frameModifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
    }

    Box(modifier = resolvedFrame) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            loading = {
                lastPainter?.let { painter ->
                    CameraSnapshotFrame(painter = painter)
                }
            },
            error = {
                if (lastPainter == null) {
                    LaunchedEffect(imageUrl) { onLoadFailed() }
                } else {
                    lastPainter?.let { painter ->
                        CameraSnapshotFrame(painter = painter)
                    }
                }
            },
            success = { state ->
                Crossfade(
                    targetState = state.painter,
                    animationSpec = tween(SNAPSHOT_CROSSFADE_MS),
                    label = "cameraSnapshot",
                ) { painter ->
                    lastPainter = painter
                    CameraSnapshotFrame(painter = painter)
                }
            },
        )
    }
}

@Composable
private fun CameraSnapshotFrame(painter: Painter) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.camera_snapshot),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HeroScrimGradient),
        )
    }
}

private fun redactImageToken(url: String): String =
    url.replace(Regex("token=[^&]+"), "token=***")
