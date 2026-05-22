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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.network.normalizeBambuddyBaseUrl
import com.chronoswing.buddydash.network.cameraSnapshotImageCacheKey
import com.chronoswing.buddydash.network.printerCameraSnapshotUrl
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.printerCoverUrl
import com.chronoswing.buddydash.ui.motion.BuddyDashMotion
import com.chronoswing.buddydash.util.CardMicroMotion
import com.chronoswing.buddydash.util.rememberCurrentPrintThumbnailIdentity

import com.chronoswing.buddydash.util.BuddyDashDebug
private const val TAG_CAMERA = "BuddyDash/Camera"

/** Refresh interval while detail Status tab is visible (RESUMED). */
private const val SNAPSHOT_REFRESH_MS = 7_000L

private const val SNAPSHOT_CROSSFADE_MS = BuddyDashMotion.CAMERA_CROSSFADE_MS

private val HeroScrimGradient = Brush.verticalGradient(
    0f to Color.Transparent,
    0.5f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.38f),
)

/**
 * Large hero image for the detail Status tab: periodic camera snapshots when available,
 * otherwise print cover. Full-screen camera view uses the MJPEG live stream instead.
 */
@Composable
fun DetailStatusHeroImage(
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    status: PrinterStatus? = null,
    printingQueueJobId: Int? = null,
    motion: CardMicroMotion = CardMicroMotion.None,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    onCameraHeroActive: (Boolean) -> Unit = {},
) {
    val coverThumbnailIdentity = rememberCurrentPrintThumbnailIdentity(
        printerId = printerId,
        status = status,
        queueJobId = printingQueueJobId,
    )
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
    var refreshTickNumber by remember(printerId) { mutableIntStateOf(0) }

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
                refreshTickNumber += 1
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
                    refreshTickNumber = refreshTickNumber,
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
                    thumbnailIdentity = coverThumbnailIdentity,
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
    refreshTickNumber: Int = 0,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = false,
    height: Dp = 160.dp,
    contentScale: ContentScale = ContentScale.Crop,
    applyHeroScrim: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    onLoadFailed: () -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {},
    snapshotCrossfadeMs: Int = SNAPSHOT_CROSSFADE_MS,
) {
    PrinterCameraSnapshotImage(
        serverUrl = serverUrl,
        cameraToken = cameraToken,
        printerId = printerId,
        refreshTick = refreshTick,
        refreshTickNumber = refreshTickNumber,
        height = height,
        fillMaxSize = fillMaxSize,
        frameModifier = modifier,
        contentScale = contentScale,
        applyHeroScrim = applyHeroScrim,
        backgroundColor = backgroundColor,
        snapshotCrossfadeMs = snapshotCrossfadeMs,
        onLoadFailed = onLoadFailed,
        onLoadingChanged = onLoadingChanged,
    )
}

@Composable
private fun PrinterCameraSnapshotImage(
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    refreshTick: Long,
    refreshTickNumber: Int,
    height: Dp,
    fillMaxSize: Boolean = false,
    frameModifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    applyHeroScrim: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    snapshotCrossfadeMs: Int = SNAPSHOT_CROSSFADE_MS,
    onLoadFailed: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit = {},
) {
    val imageUrl = remember(serverUrl, printerId, cameraToken, refreshTick) {
        printerCameraSnapshotUrl(serverUrl, printerId, cameraToken, cacheBust = refreshTick)
    }
    val imageCacheKey = remember(printerId, refreshTick) {
        cameraSnapshotImageCacheKey(printerId, refreshTick)
    }
    if (imageUrl == null) {
        LaunchedEffect(Unit) { onLoadFailed() }
        return
    }

    LaunchedEffect(refreshTickNumber, refreshTick, imageUrl, imageCacheKey) {
        if (!BuddyDashDebug.enabled) return@LaunchedEffect
        Log.d(
            TAG_CAMERA,
            "Snapshot refresh tickNumber=$refreshTickNumber cacheBust=$refreshTick " +
                "cacheKey=$imageCacheKey url=${redactImageToken(imageUrl)}",
        )
    }

    var displayedPainter by remember(printerId) { mutableStateOf<Painter?>(null) }
    var displayedRefreshTick by remember(printerId) { mutableLongStateOf(-1L) }
    val context = LocalContext.current
    val request = remember(imageUrl, imageCacheKey) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(imageCacheKey)
            .diskCachePolicy(CachePolicy.DISABLED)
            .crossfade(false)
            .listener(
                onStart = {
                    if (BuddyDashDebug.enabled) {
                        Log.d(
                            TAG_CAMERA,
                            "Snapshot load start tickNumber=$refreshTickNumber " +
                                "cacheBust=$refreshTick cacheKey=$imageCacheKey",
                        )
                    }
                },
                onSuccess = { _, _ ->
                    if (BuddyDashDebug.enabled) {
                        Log.d(
                            TAG_CAMERA,
                            "Snapshot load ok tickNumber=$refreshTickNumber " +
                                "cacheBust=$refreshTick cacheKey=$imageCacheKey",
                        )
                    }
                },
                onError = { _, result ->
                    if (BuddyDashDebug.enabled) {
                        Log.d(
                            TAG_CAMERA,
                            "Snapshot load failed tickNumber=$refreshTickNumber " +
                                "cacheBust=$refreshTick cacheKey=$imageCacheKey " +
                                "error=${result.throwable.message}",
                        )
                    }
                },
            )
            .build()
    }
    val shape = RoundedCornerShape(10.dp)
    val resolvedFrame = if (fillMaxSize) {
        frameModifier
            .fillMaxSize()
            .background(backgroundColor)
    } else {
        frameModifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(backgroundColor)
    }

    Box(
        modifier = resolvedFrame,
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            loading = {
                if (displayedPainter == null) {
                    LaunchedEffect(Unit) { onLoadingChanged(true) }
                }
                displayedPainter?.let { painter ->
                    CameraSnapshotFrame(
                        painter = painter,
                        contentScale = contentScale,
                        applyHeroScrim = applyHeroScrim,
                    )
                }
            },
            error = {
                LaunchedEffect(Unit) { onLoadingChanged(false) }
                if (displayedPainter == null) {
                    LaunchedEffect(imageUrl) { onLoadFailed() }
                } else {
                    displayedPainter?.let { painter ->
                        CameraSnapshotFrame(
                            painter = painter,
                            contentScale = contentScale,
                            applyHeroScrim = applyHeroScrim,
                        )
                    }
                }
            },
            success = { state ->
                LaunchedEffect(Unit) { onLoadingChanged(false) }
                val newPainter = state.painter
                if (refreshTick == displayedRefreshTick) {
                    CameraSnapshotFrame(
                        painter = newPainter,
                        contentScale = contentScale,
                        applyHeroScrim = applyHeroScrim,
                    )
                } else {
                    val priorPainter = displayedPainter
                    if (snapshotCrossfadeMs > 0 && priorPainter != null) {
                        Crossfade(
                            targetState = refreshTick,
                            animationSpec = tween(snapshotCrossfadeMs),
                            label = "cameraSnapshot",
                        ) { tick ->
                            val painter = if (tick == refreshTick) newPainter else priorPainter
                            CameraSnapshotFrame(
                                painter = painter,
                                contentScale = contentScale,
                                applyHeroScrim = applyHeroScrim,
                            )
                        }
                        LaunchedEffect(refreshTick) {
                            delay(snapshotCrossfadeMs.toLong())
                            displayedPainter = newPainter
                            displayedRefreshTick = refreshTick
                        }
                    } else {
                        displayedPainter = newPainter
                        displayedRefreshTick = refreshTick
                        CameraSnapshotFrame(
                            painter = newPainter,
                            contentScale = contentScale,
                            applyHeroScrim = applyHeroScrim,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun CameraSnapshotFrame(
    painter: Painter,
    contentScale: ContentScale,
    applyHeroScrim: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.camera_snapshot),
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )
        if (applyHeroScrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HeroScrimGradient),
            )
        }
    }
}

private fun redactImageToken(url: String): String =
    url.replace(Regex("token=[^&]+"), "token=***")
