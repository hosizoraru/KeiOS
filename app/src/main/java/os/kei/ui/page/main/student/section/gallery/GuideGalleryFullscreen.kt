package os.kei.ui.page.main.student.section.gallery

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.student.IMAGE_TAP_DISMISS_GESTURE_COOLDOWN_MS
import os.kei.ui.page.main.student.IMAGE_TAP_DISMISS_OFFSET_EPSILON_PX
import os.kei.ui.page.main.student.IMAGE_TAP_DISMISS_SCALE_EPSILON
import os.kei.ui.page.main.student.detectMediaRatioFromUrl
import os.kei.ui.page.main.student.isGifMediaSource
import os.kei.ui.page.main.student.loadGuideBitmapSource
import os.kei.ui.page.main.student.normalizeGuideMediaSource
import os.kei.ui.page.main.student.rememberDeviceRotationDegrees
import os.kei.ui.page.main.student.rememberSystemAutoRotateEnabled
import os.kei.ui.page.main.widget.dialog.AppWindowDialogHost
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.ContinuousTransformType
import com.github.panpf.zoomimage.zoom.GestureType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import kotlin.math.abs

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
    val backGestureState = rememberGuideFullscreenBackGestureState(onDismiss = onDismiss)

    AppWindowDialogHost(
        show = true,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { backGestureState.onDialogWidthChanged(it.width) }
                .graphicsLayer {
                    translationX = backGestureState.translationX
                    alpha = backGestureState.contentAlpha
                }
                .background(Color.Black.copy(alpha = backGestureState.scrimAlpha))
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val safeRatio = ratio.coerceAtLeast(0.1f)
                val targetRotation = resolveGuideImageTargetRotation(
                    safeRatio = safeRatio,
                    viewportWidth = maxWidth.value,
                    viewportHeight = maxHeight.value,
                    allowAutoRotate = allowAutoRotate,
                    mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled,
                    systemAutoRotateEnabled = systemAutoRotateEnabled,
                    systemRotationDegrees = systemRotationDegrees
                )
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
                    GuideFullscreenImageLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            if (!sampledState.loading && sampledState.helperLoadFailed && sampledBitmap == null) {
                GuideFullscreenImageRetryHint(
                    onRetry = { retryToken += 1 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
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
    BindGuideVideoForegroundPlaybackGuard(player = player)

    AppWindowDialogHost(
        show = true,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val activePlayer = player
            if (activePlayer != null) {
                GuideVideoFullscreenPlayerLayer(
                    activePlayer = activePlayer,
                    videoRatio = videoRatio,
                    forceLandscape = forceLandscape
                )
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
