package com.chronoswing.buddydash.ui.components

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size as CoilSize
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.network.BambuddyApi
import com.chronoswing.buddydash.network.normalizeBambuddyBaseUrl
import com.chronoswing.buddydash.network.cameraSnapshotImageCacheKey
import com.chronoswing.buddydash.network.printerCameraSnapshotUrl
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.network.printerCoverUrl
import com.chronoswing.buddydash.ui.motion.BuddyDashMotion
import com.chronoswing.buddydash.ui.motion.rememberPrefersReducedMotion
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.CardMicroMotion
import com.chronoswing.buddydash.util.rememberCurrentPrintThumbnailIdentity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    printerModel: String? = null,
    status: PrinterStatus? = null,
    printingQueueJobId: Int? = null,
    motion: CardMicroMotion = CardMicroMotion.None,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    fillContainer: Boolean = false,
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
    val printerConnected = status?.connected == true
    var cameraFailed by remember(printerId, serverUrl, cameraToken) {
        mutableStateOf(false)
    }
    val showCamera = canTryCamera && !cameraFailed && printerConnected
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
                    printerModel = printerModel,
                    refreshTick = refreshTick,
                    refreshTickNumber = refreshTickNumber,
                    height = height,
                    fillMaxSize = fillContainer,
                    frameModifier = Modifier,
                    onLoadFailed = { cameraFailed = true },
                )
            }
            coverAvailable -> {
                PrinterCoverImage(
                    serverUrl = serverUrl,
                    cameraToken = cameraToken,
                    thumbnailIdentity = coverThumbnailIdentity,
                    modifier = Modifier,
                    height = if (fillContainer) null else height,
                    fillContainer = fillContainer,
                )
            }
        }
    }

    if (!showCamera && !coverAvailable) return

    val frameModifier = if (fillContainer) {
        modifier
    } else {
        modifier.fillMaxWidth()
    }
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
    printerModel: String? = null,
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
        printerModel = printerModel,
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

/**
 * Camera snapshot with smooth crossfade between frames.
 *
 * Architecture: [imageLoader.execute] is called imperatively inside [LaunchedEffect] keyed on
 * [imageCacheKey]. The coroutine owns the full load→animate→promote lifecycle. When a new key
 * arrives (next refresh tick), Compose automatically cancels the previous coroutine, which also
 * cancels any in-progress animation. The new coroutine resets state before starting the next load.
 *
 * Display is a plain [Box] with two layers:
 *   1. Back layer  — [stablePainter]: the last committed frame, always at alpha 1.
 *   2. Front layer — [incomingPainter]: the newly loaded frame, alpha 0→1 via [Animatable].
 *
 * [BitmapPainter] is used instead of [AsyncImagePainter] so the painter is a stable, self-contained
 * object that is not tied to Coil's internal request lifecycle.
 */
@Composable
fun PrinterCameraSnapshotImage(
    serverUrl: String,
    cameraToken: String,
    printerId: Int,
    printerModel: String? = null,
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

    val reducedMotion = rememberPrefersReducedMotion()
    val fadeMs = if (reducedMotion || snapshotCrossfadeMs <= 0) 0 else snapshotCrossfadeMs

    // Persistent display state — survives request changes, resets on printer change.
    var stablePainter by remember(printerId) { mutableStateOf<Painter?>(null) }
    var incomingPainter by remember(printerId) { mutableStateOf<Painter?>(null) }
    val incomingAlpha = remember(printerId) { Animatable(0f) }

    val context = LocalContext.current

    // One coroutine per imageCacheKey. Cancelled automatically when the key changes,
    // which also stops any in-progress animation.
    LaunchedEffect(imageCacheKey) {
        // Clean up any interrupted crossfade from a previous key.
        incomingAlpha.stop()
        incomingAlpha.snapTo(0f)
        incomingPainter = null

        if (BuddyDashDebug.enabled) {
            Log.d(
                TAG_CAMERA,
                "Snapshot key=$imageCacheKey tickNumber=$refreshTickNumber " +
                    "model=${printerModel.orEmpty().ifEmpty { "(unknown)" }} " +
                    "stableExists=${stablePainter != null}",
            )
        }

        if (stablePainter == null) {
            onLoadingChanged(true)
        }

        if (BuddyDashDebug.enabled) {
            Log.d(TAG_CAMERA, "Snapshot load started key=$imageCacheKey")
        }

        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(imageCacheKey)
            .diskCachePolicy(CachePolicy.DISABLED)
            .size(CoilSize.ORIGINAL)
            .build()

        val result = context.imageLoader.execute(request)

        when (result) {
            is SuccessResult -> {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap == null) {
                    // Non-bitmap drawable: not expected for camera snapshots, treat as error.
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_CAMERA, "Snapshot load success but non-bitmap drawable key=$imageCacheKey")
                    }
                    onLoadingChanged(false)
                    if (stablePainter == null) onLoadFailed()
                    return@LaunchedEffect
                }

                // BitmapPainter is a stable, self-contained painter not tied to Coil internals.
                val newPainter: Painter = BitmapPainter(bitmap.asImageBitmap())

                if (BuddyDashDebug.enabled) {
                    Log.d(
                        TAG_CAMERA,
                        "Snapshot load success key=$imageCacheKey hasPrior=${stablePainter != null}",
                    )
                }

                onLoadingChanged(false)

                if (fadeMs > 0 && stablePainter != null) {
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_CAMERA, "Snapshot fade start key=$imageCacheKey fadeMs=$fadeMs")
                    }
                    // incomingAlpha is already at 0f from the reset above.
                    incomingPainter = newPainter
                    incomingAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = fadeMs,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                    // Promote: move incoming to stable and clear the front layer.
                    stablePainter = newPainter
                    incomingPainter = null
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_CAMERA, "Snapshot fade complete, stable promoted key=$imageCacheKey")
                    }
                } else {
                    // First load or reduced motion: display immediately, no animation.
                    stablePainter = newPainter
                    if (BuddyDashDebug.enabled) {
                        Log.d(TAG_CAMERA, "Snapshot stable promoted immediately key=$imageCacheKey")
                    }
                }
            }

            is ErrorResult -> {
                if (BuddyDashDebug.enabled) {
                    Log.d(
                        TAG_CAMERA,
                        "Snapshot load failed key=$imageCacheKey " +
                            "hasPrior=${stablePainter != null} " +
                            "error=${result.throwable.message}",
                    )
                }
                onLoadingChanged(false)
                // If we have a stable frame, keep it — do not blank or signal failure.
                if (stablePainter == null) {
                    onLoadFailed()
                }
            }
        }
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
        // ── Back layer: stable committed frame ──────────────────────────────────────────────
        // Permanently visible once the first frame loads. Never blanked during refreshes.
        if (stablePainter != null) {
            CameraSnapshotFrame(
                painter = stablePainter!!,
                contentScale = contentScale,
                applyHeroScrim = applyHeroScrim,
            )
        } else {
            CameraSnapshotPlaceholder(contentScale = contentScale, applyHeroScrim = applyHeroScrim)
        }

        // ── Front layer: incoming frame fading in ───────────────────────────────────────────
        // Only present during an active crossfade (incomingAlpha 0→1).
        incomingPainter?.let { incoming ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = incomingAlpha.value },
            ) {
                CameraSnapshotFrame(
                    painter = incoming,
                    contentScale = contentScale,
                    applyHeroScrim = applyHeroScrim,
                )
            }
        }
    }
}

@Composable
private fun CameraSnapshotPlaceholder(
    contentScale: ContentScale,
    applyHeroScrim: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        if (applyHeroScrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HeroScrimGradient),
            )
        }
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
