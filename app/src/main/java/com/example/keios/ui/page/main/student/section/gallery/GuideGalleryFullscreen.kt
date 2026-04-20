package com.example.keios.ui.page.main.student.section.gallery

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.keios.ui.page.main.ba.support.BASettingsStore
import com.example.keios.ui.page.main.student.IMAGE_BACK_GESTURE_CONTENT_FADE_FACTOR
import com.example.keios.ui.page.main.student.IMAGE_BACK_GESTURE_SCRIM_FADE_FACTOR
import com.example.keios.ui.page.main.student.IMAGE_BACK_GESTURE_TRANSLATION_FACTOR
import com.example.keios.ui.page.main.student.IMAGE_TAP_DISMISS_GESTURE_COOLDOWN_MS
import com.example.keios.ui.page.main.student.IMAGE_TAP_DISMISS_OFFSET_EPSILON_PX
import com.example.keios.ui.page.main.student.IMAGE_TAP_DISMISS_SCALE_EPSILON
import com.example.keios.ui.page.main.student.detectMediaRatioFromUrl
import com.example.keios.ui.page.main.student.isGifMediaSource
import com.example.keios.ui.page.main.student.loadGuideBitmapSource
import com.example.keios.ui.page.main.student.normalizeGuideMediaSource
import com.example.keios.ui.page.main.student.rememberDeviceRotationDegrees
import com.example.keios.ui.page.main.student.rememberSystemAutoRotateEnabled
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.motion.resolvedMotionDuration
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.ContinuousTransformType
import com.github.panpf.zoomimage.zoom.GestureType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import kotlin.math.abs

@OptIn(ExperimentalActivityApi::class)
@Composable
internal fun GuideImageFullscreenDialog(
    imageUrl: String,
    allowAutoRotate: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mediaAdaptiveRotationEnabled = remember { BASettingsStore.loadMediaAdaptiveRotationEnabled() }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val systemAutoRotateEnabled =
        rememberSystemAutoRotateEnabled(active = !mediaAdaptiveRotationEnabled)
    val systemRotationDegrees = rememberDeviceRotationDegrees(
        active = !mediaAdaptiveRotationEnabled && systemAutoRotateEnabled
    )
    val normalizedImageUrl = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (normalizedImageUrl.isBlank()) return
    val isGifSource = remember(normalizedImageUrl) { isGifMediaSource(normalizedImageUrl) }
    val zoomState = rememberCoilZoomState()
    LaunchedEffect(zoomState) {
        zoomState.zoomable.setDisabledGestureTypes(
            GestureType.ONE_FINGER_SCALE
        )
    }
    var retryToken by rememberSaveable(normalizedImageUrl) { mutableStateOf(0) }
    var lastTransformActiveAtMs by rememberSaveable(normalizedImageUrl) { mutableStateOf(0L) }
    val sampledState by produceState(
        initialValue = GuideFullscreenImageState(loading = !isGifSource),
        normalizedImageUrl,
        isGifSource,
        retryToken
    ) {
        if (isGifSource) {
            value = GuideFullscreenImageState(
                sampledBitmap = null,
                loading = false,
                helperLoadFailed = false
            )
            return@produceState
        }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                loadGuideBitmapSource(
                    context = context,
                    source = normalizedImageUrl,
                    maxDecodeDimension = 2048
                )
            }.getOrNull()
        }
        value = GuideFullscreenImageState(
            sampledBitmap = bitmap,
            loading = false,
            helperLoadFailed = bitmap == null
        )
    }
    val sampledBitmap = sampledState.sampledBitmap
    val ratioFromUrl = remember(normalizedImageUrl) {
        detectMediaRatioFromUrl(normalizedImageUrl)
    }
    val ratio = remember(sampledBitmap?.width, sampledBitmap?.height, ratioFromUrl) {
        val width = sampledBitmap?.width ?: 0
        val height = sampledBitmap?.height ?: 0
        when {
            width > 0 && height > 0 -> width.toFloat() / height.toFloat()
            ratioFromUrl != null -> ratioFromUrl
            else -> 1f
        }
    }
    LaunchedEffect(zoomState.zoomable.continuousTransformType) {
        if (zoomState.zoomable.continuousTransformType != ContinuousTransformType.NONE) {
            lastTransformActiveAtMs = SystemClock.elapsedRealtime()
        }
    }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_NONE) }
    var dialogWidthPx by remember { mutableIntStateOf(0) }
    BackHandler(enabled = true) {
        onDismiss()
    }
    PredictiveBackHandler(enabled = true) { backEvents ->
        var dismissedByPredictiveProgress = false
        try {
            backEvents.collect { event ->
                predictiveBackProgress = event.progress.coerceIn(0f, 1f)
                predictiveBackSwipeEdge = event.swipeEdge
                if (event.progress >= 0.995f) {
                    dismissedByPredictiveProgress = true
                    onDismiss()
                }
            }
            if (!dismissedByPredictiveProgress) {
                onDismiss()
            }
        } catch (_: CancellationException) {
        } finally {
            predictiveBackProgress = 0f
            predictiveBackSwipeEdge = BackEventCompat.EDGE_NONE
        }
    }
    val clampedBackProgress = predictiveBackProgress.coerceIn(0f, 1f)
    val easedBackProgress = clampedBackProgress * clampedBackProgress * (3f - 2f * clampedBackProgress)
    val backEdgeDirection = when (predictiveBackSwipeEdge) {
        BackEventCompat.EDGE_LEFT -> 1f
        BackEventCompat.EDGE_RIGHT -> -1f
        else -> 0f
    }
    val backTranslationX = dialogWidthPx.toFloat() *
            IMAGE_BACK_GESTURE_TRANSLATION_FACTOR *
        backEdgeDirection *
        easedBackProgress
    val backContentAlpha = (1f - easedBackProgress * IMAGE_BACK_GESTURE_CONTENT_FADE_FACTOR).coerceIn(0f, 1f)
    val backScrimAlpha = (1f - easedBackProgress * IMAGE_BACK_GESTURE_SCRIM_FADE_FACTOR).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { dialogWidthPx = it.width }
                .graphicsLayer {
                    translationX = backTranslationX
                    alpha = backContentAlpha
                }
                .background(Color.Black.copy(alpha = backScrimAlpha))
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val safeRatio = ratio.coerceAtLeast(0.1f)
                val viewportWidth = maxWidth
                val viewportHeight = maxHeight
                val viewportRatio = if (viewportHeight.value > 0f) {
                    viewportWidth.value / viewportHeight.value
                } else {
                    1f
                }

                fun fitArea(targetRatio: Float): Float {
                    val normalizedRatio = targetRatio.coerceAtLeast(0.1f)
                    return if (viewportRatio >= normalizedRatio) {
                        val fittedHeight = viewportHeight.value
                        val fittedWidth = fittedHeight * normalizedRatio
                        fittedWidth * fittedHeight
                    } else {
                        val fittedWidth = viewportWidth.value
                        val fittedHeight = fittedWidth / normalizedRatio
                        fittedWidth * fittedHeight
                    }
                }

                val normalArea = fitArea(safeRatio)
                val rotatedRatio = (1f / safeRatio).coerceAtLeast(0.1f)
                val rotatedArea = fitArea(rotatedRatio)
                val shouldRotate90 = safeRatio > 1.02f && rotatedArea > (normalArea * 1.12f)
                val targetRotation = if (mediaAdaptiveRotationEnabled) {
                    if (allowAutoRotate && shouldRotate90) 90 else 0
                } else {
                    if (systemAutoRotateEnabled) systemRotationDegrees else 0
                }
                val rotationTransition = remember(normalizedImageUrl) { Animatable(0f) }
                var appliedZoomRotation by rememberSaveable(normalizedImageUrl) { mutableIntStateOf(0) }
                var initializedRotation by rememberSaveable(normalizedImageUrl) { mutableStateOf(false) }
                val zoomInteracting = zoomState.zoomable.continuousTransformType != ContinuousTransformType.NONE
                LaunchedEffect(normalizedImageUrl) {
                    rotationTransition.snapTo(0f)
                    appliedZoomRotation = 0
                    initializedRotation = false
                }
                LaunchedEffect(normalizedImageUrl, targetRotation) {
                    if (!initializedRotation) {
                        zoomState.zoomable.rotate(targetRotation)
                        appliedZoomRotation = targetRotation
                        initializedRotation = true
                        rotationTransition.snapTo(0f)
                        return@LaunchedEffect
                    }
                    if (targetRotation == appliedZoomRotation) {
                        rotationTransition.snapTo(0f)
                        return@LaunchedEffect
                    }
                    if (zoomInteracting) {
                        zoomState.zoomable.rotate(targetRotation)
                        appliedZoomRotation = targetRotation
                        rotationTransition.snapTo(0f)
                        return@LaunchedEffect
                    }
                    var delta = (targetRotation - appliedZoomRotation) % 360
                    if (delta > 180) delta -= 360
                    if (delta < -180) delta += 360
                    if (delta == 0) {
                        zoomState.zoomable.rotate(targetRotation)
                        appliedZoomRotation = targetRotation
                        rotationTransition.snapTo(0f)
                        return@LaunchedEffect
                    }
                    rotationTransition.snapTo(0f)
                    if (transitionAnimationsEnabled) {
                        rotationTransition.animateTo(
                            targetValue = delta.toFloat(),
                            animationSpec = tween(
                                durationMillis = resolvedMotionDuration(220, transitionAnimationsEnabled),
                                easing = FastOutSlowInEasing
                            )
                        )
                    } else {
                        rotationTransition.snapTo(delta.toFloat())
                    }
                    zoomState.zoomable.rotate(targetRotation)
                    appliedZoomRotation = targetRotation
                    rotationTransition.snapTo(0f)
                }

                CoilZoomAsyncImage(
                    model = if (isGifSource) normalizedImageUrl else sampledBitmap ?: normalizedImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationTransition.value)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit,
                    zoomState = zoomState,
                    scrollBar = null,
                    onTap = {
                        if (zoomState.zoomable.continuousTransformType != ContinuousTransformType.NONE) {
                            return@CoilZoomAsyncImage
                        }
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastTransformActiveAtMs < IMAGE_TAP_DISMISS_GESTURE_COOLDOWN_MS) {
                            return@CoilZoomAsyncImage
                        }
                        val userTransform = zoomState.zoomable.userTransform
                        val scaleNearBase = abs(userTransform.scaleX - 1f) <= IMAGE_TAP_DISMISS_SCALE_EPSILON &&
                            abs(userTransform.scaleY - 1f) <= IMAGE_TAP_DISMISS_SCALE_EPSILON
                        val offsetNearBase = abs(userTransform.offsetX) <= IMAGE_TAP_DISMISS_OFFSET_EPSILON_PX &&
                            abs(userTransform.offsetY) <= IMAGE_TAP_DISMISS_OFFSET_EPSILON_PX
                        if (!scaleNearBase || !offsetNearBase) {
                            return@CoilZoomAsyncImage
                        }
                        onDismiss()
                    }
                )

                if (sampledState.loading) {
                    CircularProgressIndicator(
                        progress = 0.28f,
                        size = 24.dp,
                        strokeWidth = 2.dp,
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = Color(0xFF60A5FA),
                            backgroundColor = Color(0x3360A5FA)
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            if (!sampledState.loading && sampledState.helperLoadFailed && sampledBitmap == null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "图片加载异常",
                        color = Color(0xFFBFDBFE)
                    )
                    GlassTextButton(
                        backdrop = null,
                        text = "重试",
                        leadingIcon = MiuixIcons.Regular.Refresh,
                        textColor = Color(0xFF60A5FA),
                        variant = GlassVariant.Compact,
                        onClick = { retryToken += 1 }
                    )
                }
            }
        }
    }
}

internal data class GuideFullscreenImageState(
    val sampledBitmap: Bitmap? = null,
    val loading: Boolean = false,
    val helperLoadFailed: Boolean = false
)

@Composable
internal fun GuideVideoFullscreenDialog(
    mediaUrl: String,
    forceLandscape: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val normalizedUrl = remember(mediaUrl) { normalizeGuideMediaSource(mediaUrl) }
    var loadError by remember(normalizedUrl) { mutableStateOf<String?>(null) }
    var videoRatio by remember(normalizedUrl) { mutableStateOf(16f / 9f) }

    val player = rememberGuidePreparedVideoPlayer(
        context = context,
        mediaUrl = normalizedUrl,
        active = true
    )
    BindGuideVideoPlayerState(
        player = player,
        onVideoRatioChanged = { ratio -> videoRatio = ratio },
        onPlayerErrorChanged = { errorCode -> loadError = errorCode }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val activePlayer = player
            if (activePlayer != null) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val safeRatio = videoRatio.coerceAtLeast(0.2f)
                    val shouldRotateLandscape = forceLandscape && maxHeight > maxWidth

                    fun fitSize(targetRatio: Float): Pair<Dp, Dp> {
                        val normalizedRatio = targetRatio.coerceAtLeast(0.2f)
                        val viewportRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
                        return if (viewportRatio >= normalizedRatio) {
                            val h = maxHeight
                            (h * normalizedRatio) to h
                        } else {
                            val w = maxWidth
                            w to (w / normalizedRatio)
                        }
                    }

                    val playerModifier = if (shouldRotateLandscape) {
                        val rotatedFinal = fitSize((1f / safeRatio).coerceAtLeast(0.2f))
                        val preRotate = rotatedFinal.second to rotatedFinal.first
                        Modifier
                            .width(preRotate.first)
                            .height(preRotate.second)
                            .rotate(90f)
                            .align(Alignment.Center)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    AndroidView(
                        modifier = playerModifier,
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                this.player = activePlayer
                            }
                        },
                        update = { view ->
                            view.player = activePlayer
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    )
                }
            } else {
                Text(
                    text = "视频地址无效",
                    color = Color(0xFFBFDBFE),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            loadError?.takeIf { it.isNotBlank() }?.let { err ->
                Text(
                    text = "视频播放失败：$err",
                    color = Color(0xFFFCA5A5),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
}
