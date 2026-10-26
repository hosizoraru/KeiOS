@file:Suppress("FunctionName", "PropertyName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import os.kei.ui.page.main.widget.isAppInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.animation.DampedDragAnimation
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

@Immutable
data class LiquidSliderKeyPoint(
    val value: Float,
    val color: Color = Color.Unspecified,
    val size: Dp = 5.dp,
)

/**
 * Music progress slider with a 48dp interactive root.
 *
 * [visualVerticalOffset] moves only the track, key points, and thumb inside that root. Positive
 * values move the visual treatment downward while preserving gesture and semantics bounds. The
 * caller keeps the moved treatment inside any ancestor clip. A null [backdrop] keeps the same
 * interaction and geometry while using the static track and thumb fallback.
 */
@Composable
fun LiquidMusicProgressSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
    visualVerticalOffset: Dp = 0.dp,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    onInteractionChanged: (Boolean) -> Unit = {},
) {
    val isLightTheme = !isAppInDarkTheme()
    val defaultActiveColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF5DAEFF)
    val defaultInactiveColor =
        if (isLightTheme) {
            Color(0xFF1D1D1F).copy(alpha = 0.16f)
        } else {
            Color.White.copy(alpha = 0.18f)
        }
    LiquidTrackSlider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        visibilityThreshold = visibilityThreshold,
        backdrop = backdrop,
        modifier = modifier,
        enabled = enabled,
        contentDescription = contentDescription,
        onInteractionChanged = onInteractionChanged,
        visualVerticalOffset = visualVerticalOffset,
        style =
            LiquidTrackSliderStyle(
                activeColor = if (activeColor.isSpecified) activeColor else defaultActiveColor,
                inactiveColor = if (inactiveColor.isSpecified) inactiveColor else defaultInactiveColor,
                trackHeight = 4.dp,
                thumbWidth = 30.dp,
                thumbHeight = 18.dp,
                pressedWidthScale = 1.46f,
                pressedHeightScale = 1.30f,
                thumbGlassSurfaceAlpha = thumbGlassSurfaceAlphaFor(isLightTheme),
                thumbPressedSurfaceAlpha = thumbPressedSurfaceAlphaFor(isLightTheme),
                thumbRestingHighlightAlpha = thumbRestingHighlightAlphaFor(isLightTheme),
            ),
    )
}

@Composable
fun LiquidVolumeSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    onInteractionChanged: (Boolean) -> Unit = {},
) {
    val isLightTheme = !isAppInDarkTheme()
    val accentColor =
        if (activeColor.isSpecified) {
            activeColor
        } else if (isLightTheme) {
            Color(0xFF0088FF)
        } else {
            Color(0xFF5DAEFF)
        }
    val defaultInactiveColor =
        if (isLightTheme) {
            Color(0xFF1D1D1F).copy(alpha = 0.15f)
        } else {
            Color.White.copy(alpha = 0.18f)
        }
    LiquidTrackSlider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        visibilityThreshold = visibilityThreshold,
        backdrop = backdrop,
        modifier = modifier,
        enabled = enabled,
        contentDescription = contentDescription,
        onInteractionChanged = onInteractionChanged,
        visualVerticalOffset = 0.dp,
        style =
            LiquidTrackSliderStyle(
                activeColor = accentColor.copy(alpha = 0.92f),
                inactiveColor = if (inactiveColor.isSpecified) inactiveColor else defaultInactiveColor,
                trackHeight = 6.dp,
                thumbWidth = 40.dp,
                thumbHeight = 26.dp,
                pressedWidthScale = 1.35f,
                pressedHeightScale = 1.22f,
                thumbGlassSurfaceAlpha = thumbGlassSurfaceAlphaFor(isLightTheme),
                thumbPressedSurfaceAlpha = thumbPressedSurfaceAlphaFor(isLightTheme),
                thumbRestingHighlightAlpha = thumbRestingHighlightAlphaFor(isLightTheme),
            ),
    )
}

@Composable
fun LiquidKeyPointSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop?,
    keyPoints: List<LiquidSliderKeyPoint>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    snapToKeyPoints: Boolean = false,
    snapThreshold: Float? = null,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    onInteractionChanged: (Boolean) -> Unit = {},
) {
    val isLightTheme = !isAppInDarkTheme()
    val accentColor =
        if (activeColor.isSpecified) {
            activeColor
        } else if (isLightTheme) {
            Color(0xFF0088FF)
        } else {
            Color(0xFF5DAEFF)
        }
    val defaultInactiveColor =
        if (isLightTheme) {
            Color(0xFF1D1D1F).copy(alpha = 0.14f)
        } else {
            Color.White.copy(alpha = 0.18f)
        }
    LiquidTrackSlider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        visibilityThreshold = visibilityThreshold,
        backdrop = backdrop,
        modifier = modifier,
        enabled = enabled,
        contentDescription = contentDescription,
        onInteractionChanged = onInteractionChanged,
        visualVerticalOffset = 0.dp,
        keyPoints = keyPoints,
        snapToKeyPoints = snapToKeyPoints,
        snapThreshold = snapThreshold,
        style =
            LiquidTrackSliderStyle(
                activeColor = accentColor,
                inactiveColor = if (inactiveColor.isSpecified) inactiveColor else defaultInactiveColor,
                keyPointColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                keyPointActiveColor = Color.White.copy(alpha = 0.90f),
                trackHeight = 7.dp,
                thumbWidth = 38.dp,
                thumbHeight = 22.dp,
                pressedWidthScale = 1.38f,
                pressedHeightScale = 1.26f,
                thumbGlassSurfaceAlpha = thumbGlassSurfaceAlphaFor(isLightTheme),
                thumbPressedSurfaceAlpha = thumbPressedSurfaceAlphaFor(isLightTheme),
                thumbRestingHighlightAlpha = thumbRestingHighlightAlphaFor(isLightTheme),
            ),
    )
}

@Composable
private fun LiquidTrackSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: ((Float) -> Unit)?,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop?,
    modifier: Modifier,
    enabled: Boolean,
    contentDescription: String?,
    onInteractionChanged: (Boolean) -> Unit,
    visualVerticalOffset: Dp,
    style: LiquidTrackSliderStyle,
    keyPoints: List<LiquidSliderKeyPoint> = emptyList(),
    snapToKeyPoints: Boolean = false,
    snapThreshold: Float? = null,
) {
    require(visibilityThreshold.isFinite() && visibilityThreshold >= 0f) {
        "visibilityThreshold must be a finite non-negative value"
    }
    require(snapThreshold == null || (snapThreshold.isFinite() && snapThreshold >= 0f)) {
        "snapThreshold must be null or a finite non-negative value"
    }
    val safeValueRange =
        remember(valueRange.start, valueRange.endInclusive) {
            liquidFiniteRange(valueRange)
        }
    val safeKeyPoints =
        remember(keyPoints, safeValueRange) {
            sanitizeLiquidSliderKeyPoints(keyPoints, safeValueRange)
        }
    val valueResolver = remember { LiquidFiniteValueResolver() }
    val safeVisualVerticalOffset =
        visualVerticalOffset.takeIf { offset -> offset.value.isFinite() } ?: 0.dp
    val glassRuntime = glassEffectRuntime()
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val trackBackdrop =
        if (activeBackdrop != null) {
            rememberLayerBackdrop()
        } else {
            null
        }
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val onValueChangeFinishedState = rememberUpdatedState(onValueChangeFinished)
    val onInteractionChangedState = rememberUpdatedState(onInteractionChanged)
    DisposableEffect(Unit) {
        onDispose {
            onInteractionChangedState.value(false)
        }
    }
    BoxWithConstraints(
        modifier =
            Modifier
                .defaultMinSize(minHeight = LiquidSliderMinimumInteractiveHeight)
                .then(modifier)
                .fillMaxWidth()
                .liquidSliderInteractionLock(
                    enabled = enabled,
                    onInteractionChanged = onInteractionChangedState.value,
                ).then(disabledContentAlphaModifier(enabled))
                .semantics(mergeDescendants = true) {
                    val currentValue = valueResolver.resolve(value(), safeValueRange)
                    contentDescription
                        ?.takeIf(String::isNotBlank)
                        ?.let { description -> this.contentDescription = description }
                    progressBarRangeInfo = ProgressBarRangeInfo(currentValue, safeValueRange, steps = 0)
                    if (enabled) {
                        setProgress { target ->
                            val next =
                                resolveSliderProgressChange(
                                    currentValue = valueResolver.resolve(value(), safeValueRange),
                                    target = target,
                                    valueRange = safeValueRange,
                                    keyPoints = safeKeyPoints,
                                    snapToKeyPoints = snapToKeyPoints,
                                    snapThreshold = snapThreshold,
                                ) ?: return@setProgress false
                            onValueChangeState.value(next)
                            onValueChangeFinishedState.value?.invoke(next)
                            true
                        }
                    } else {
                        disabled()
                    }
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = constraints.maxWidth.coerceAtLeast(1)
        val trackWidthPx = trackWidth.toFloat()
        val localDensity = LocalDensity.current
        val thumbWidthPx = with(localDensity) { style.thumbWidth.toPx() }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val touchSlop = LocalViewConfiguration.current.touchSlop
        val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
        val hapticFeedback = LocalHapticFeedback.current
        val hapticState = remember { LiquidSliderHapticState() }
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val rangeSpan = safeValueRange.endInclusive - safeValueRange.start
        val dampedDragAnimation =
            remember(
                animationScope,
                safeValueRange,
                visibilityThreshold,
                enabled,
                trackWidth,
                isLtr,
                touchSlop,
                style.pressedWidthScale,
                safeKeyPoints,
                snapToKeyPoints,
                snapThreshold,
                transitionAnimationsEnabled,
            ) {
                DampedDragAnimation(
                    animationScope = animationScope,
                    // The value observer below owns subsequent ticks. Initializing the animation
                    // must not subscribe this subcomposition to the first external value change.
                    initialValue = Snapshot.withoutReadObservation {
                        valueResolver.resolve(value(), safeValueRange)
                    },
                    valueRange = safeValueRange,
                    visibilityThreshold = visibilityThreshold,
                    initialScale = 1f,
                    pressedScale = style.pressedWidthScale,
                    animationsEnabled = transitionAnimationsEnabled,
                    consumeDragChanges = true,
                    dragOrientation = Orientation.Horizontal,
                    dragTouchSlop = touchSlop,
                    onDragStarted = { position ->
                        if (enabled) {
                            val target =
                                resolveSliderTarget(
                                    target =
                                        sliderValueAt(
                                            offset = position,
                                            widthPx = trackWidth.toFloat(),
                                            valueRange = safeValueRange,
                                            isLtr = isLtr,
                                        ),
                                    valueRange = safeValueRange,
                                    keyPoints = safeKeyPoints,
                                    snapToKeyPoints = snapToKeyPoints,
                                    snapThreshold = snapThreshold,
                                )
                            snapToValue(target)
                            onValueChangeState.value(target)
                            hapticState.reset(target)
                            hapticState.handleHapticFeedback(
                                currentValue = target,
                                valueRange = safeValueRange,
                                hapticFeedback = hapticFeedback,
                                keyPoints = safeKeyPoints,
                            )
                            onInteractionChangedState.value(true)
                        }
                    },
                    onDragStopped = {
                        onInteractionChangedState.value(false)
                        if (enabled && didDrag) {
                            val next =
                                resolveSliderTarget(
                                    target = targetValue,
                                    valueRange = safeValueRange,
                                    keyPoints = safeKeyPoints,
                                    snapToKeyPoints = snapToKeyPoints,
                                    snapThreshold = snapThreshold,
                                )
                            onValueChangeState.value(next)
                            onValueChangeFinishedState.value?.invoke(next)
                            if (snapToKeyPoints) {
                                animateToValue(next, pressFeedback = false)
                            }
                        }
                        didDrag = false
                    },
                    onDragCancelled = {
                        onInteractionChangedState.value(false)
                        didDrag = false
                    },
                    onDrag = { _, dragAmount ->
                        if (!enabled || rangeSpan == 0f) return@DampedDragAnimation
                        if (!didDrag) {
                            didDrag = dragAmount.x != 0f
                        }
                        val delta = rangeSpan * (dragAmount.x / trackWidth)
                        val target = if (isLtr) targetValue + delta else targetValue - delta
                        val boundedTarget = target.coerceIn(safeValueRange)
                        snapToValue(boundedTarget)
                        onValueChangeState.value(boundedTarget)
                        hapticState.handleHapticFeedback(
                            currentValue = boundedTarget,
                            valueRange = safeValueRange,
                            hapticFeedback = hapticFeedback,
                            keyPoints = safeKeyPoints,
                        )
                    },
                )
            }
        // P1: read the deferred value() inside a snapshotFlow instead of at composition scope.
        // Reading value() here would recompose the whole slider on every external tick (e.g. the
        // music progress slider advancing during playback). All visual state is driven off the
        // animation in deferred blocks, so this side-effect is the only thing that forced recompose.
        // The > visibilityThreshold guard preserves the two-way-binding protection during drag.
        val valueState = rememberUpdatedState(value)
        val snapshotFlowManager = rememberAppSnapshotFlowManager()
        LaunchedEffect(
            dampedDragAnimation,
            snapshotFlowManager,
            safeValueRange,
            visibilityThreshold,
        ) {
            snapshotFlowManager
                .snapshotFlow { valueResolver.resolve(valueState.value(), safeValueRange) }
                .collectLatest { currentValue ->
                    if (abs(dampedDragAnimation.targetValue - currentValue) > visibilityThreshold) {
                        dampedDragAnimation.updateValue(currentValue)
                    }
                }
        }
        val visualProgress by remember(dampedDragAnimation, isLtr) {
            derivedStateOf {
                sliderVisualProgress(
                    progress = dampedDragAnimation.progress.fastCoerceIn(0f, 1f),
                    isLtr = isLtr,
                )
            }
        }
        val thumbBackdrop =
            if (activeBackdrop != null && trackBackdrop != null) {
                // Per the Backdrop "Glass Slider" tutorial: the thumb must refract the background
                // AND the track simultaneously, so combine both layers directly. (The previous
                // build wrapped trackBackdrop in a scaled rememberBackdrop, which squashed the
                // refraction at rest and broke the clean magnifying-lens look from the screenshot.)
                rememberCombinedBackdrop(activeBackdrop, trackBackdrop)
            } else {
                null
            }

        val trackLayerHeight =
            safeKeyPoints
                .maxOfOrNull { keyPoint -> keyPoint.size }
                ?.let { keyPointSize -> maxOf(style.trackHeight, keyPointSize) }
                ?: style.trackHeight

        Box(
            modifier =
                Modifier
                    .offset(y = safeVisualVerticalOffset)
                    .then(
                        if (trackBackdrop != null) {
                            Modifier.layerBackdrop(trackBackdrop)
                        } else {
                            Modifier
                        },
                    ).fillMaxWidth()
                    .height(trackLayerHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .appSquircleBackground(style.inactiveColor, 999.dp)
                    .height(style.trackHeight)
                    .fillMaxWidth(),
            )
            Box(
                Modifier
                    .appSquircleBackground(style.activeColor, 999.dp)
                    .height(style.trackHeight)
                    .layout { measurable, constraints ->
                        // Ends under the thumb's centre, which now travels the inset span rather than the
                        // whole width. Using `maxWidth * progress` here would leave the fill ahead of the
                        // thumb below the midpoint and behind it above.
                        val width =
                            liquidSliderCenterAt(
                                trackWidth = constraints.maxWidth.toFloat(),
                                thumbWidth = thumbWidthPx,
                                progress = dampedDragAnimation.progress.fastCoerceIn(0f, 1f),
                            ).fastRoundToInt()
                                .coerceIn(0, constraints.maxWidth)
                        val placeable =
                            measurable.measure(
                                constraints.copy(
                                    minWidth = width,
                                    maxWidth = width,
                                ),
                            )
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }.graphicsLayer {
                        translationX = if (isLtr) 0f else trackWidth - size.width
                    },
            )

            safeKeyPoints.forEach { keyPoint ->
                val progress = valueProgress(keyPoint.value, safeValueRange)
                val visualKeyPointProgress = sliderVisualProgress(progress, isLtr)
                LiquidSliderKeyPointDot(
                    keyPoint = keyPoint,
                    style = style,
                    animation = dampedDragAnimation,
                    progress = progress,
                    visualProgress = visualKeyPointProgress,
                    trackWidthPx = trackWidthPx,
                    thumbWidthPx = thumbWidthPx,
                )
            }
        }
        if (enabled) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(maxHeight)
                    .then(dampedDragAnimation.modifier)
                    .pointerInput(
                        safeValueRange,
                        isLtr,
                        trackWidth,
                        safeKeyPoints,
                        snapToKeyPoints,
                        snapThreshold,
                    ) {
                        detectTapGestures { position ->
                            val rawTarget =
                                sliderValueAt(
                                    offset = position,
                                    widthPx = trackWidth.toFloat(),
                                    valueRange = safeValueRange,
                                    isLtr = isLtr,
                                )
                            val target =
                                resolveSliderTarget(
                                    target = rawTarget,
                                    valueRange = safeValueRange,
                                    keyPoints = safeKeyPoints,
                                    snapToKeyPoints = snapToKeyPoints,
                                    snapThreshold = snapThreshold,
                                )
                            dampedDragAnimation.animateToValue(target)
                            onValueChangeState.value(target)
                            hapticState.reset(valueResolver.resolve(value(), safeValueRange))
                            hapticState.handleHapticFeedback(
                                currentValue = target,
                                valueRange = safeValueRange,
                                hapticFeedback = hapticFeedback,
                                keyPoints = safeKeyPoints,
                            )
                            onValueChangeFinishedState.value?.invoke(target)
                        }
                    },
            )
        }

        Box(
            Modifier
                .offset(y = safeVisualVerticalOffset)
                .graphicsLayer { clip = false }
                .graphicsLayer {
                    // The capsule never leaves its own row, so nothing can cut it.
                    //
                    // It used to be centred on the value — `-width/2 + trackWidth * progress` — which puts
                    // a quarter of it past x = 0 at the minimum and past the right edge at the maximum,
                    // and something up the tree clips there: the flat-left, round-right half-capsule the
                    // 0% sliders were showing. Reserving padding on the row did not help, so the clip is
                    // not the card; rather than keep hunting the exact ancestor, the geometry now simply
                    // never asks to draw outside.
                    //
                    // Travel is the standard one every platform slider uses: the left edge runs 0 ..
                    // trackWidth - width, so the centre runs width/2 .. trackWidth - width/2. The filled
                    // track and the key points below use the same mapping, so the fill still ends exactly
                    // under the centre of the thumb at every value.
                    translationX = liquidSliderThumbTravel(trackWidthPx, size.width) * visualProgress
                }.then(
                    if (thumbBackdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = thumbBackdrop,
                            shape = { ContinuousCapsule },
                            // Effect order per Backdrop API: color filter -> blur -> lens.
                            // The tutorial's key visual: rest = frosted pill, press = clear magnifying
                            // lens. So blur DECREASES on press (glass clears up, refraction shows
                            // through) and lens INTENSIFIES (stronger magnification).
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                // Past the tutorial: it stops at a fixed `vibrancy()`. At rest these
                                // values *are* `vibrancy()` (brightness 0, contrast 1, saturation 1.5),
                                // so idle is unchanged; grabbing the thumb lifts both so the glass
                                // catches light. See LiquidTrackSliderStyle.restingSaturation.
                                colorControls(
                                    brightness = lerp(0f, style.pressedBrightness, progress),
                                    saturation = lerp(style.restingSaturation, style.pressedSaturation, progress),
                                )
                                // Keep the static thumb close to the Backdrop tutorial: clear lens first,
                                // light frosting second. Pressing removes most frosting — but not all of
                                // it. Lerping to exactly 0f left nothing to hold the refraction together
                                // at the moment the lens amount is at its largest, and a recording of a
                                // real drag showed the track's blue edge tearing into separate blobs
                                // inside the capsule. A fifth of the resting frost is invisible as frost
                                // and enough to keep that edge continuous.
                                blur(
                                    lerp(
                                        style.thumbRestingBlur.toPx() *
                                            glassRuntime.blurScaleFor(GlassVariant.Compact),
                                        style.thumbRestingBlur.toPx() *
                                            glassRuntime.blurScaleFor(GlassVariant.Compact) *
                                            SliderThumbPressedBlurFloorFraction,
                                        progress,
                                    ),
                                )
                                // Lens stays visible at rest and gains strength while pressed or dragged.
                                safeLiquidLens(
                                    lerp(
                                        style.lensRefractionHeight.toPx() *
                                            glassRuntime.lensScaleFor(GlassVariant.Compact),
                                        style.lensRefractionHeight.toPx() *
                                            style.pressedLensHeightScale *
                                            glassRuntime.interactionLensScale,
                                        progress,
                                    ),
                                    lerp(
                                        style.lensRefractionAmount.toPx() *
                                            glassRuntime.lensScaleFor(GlassVariant.Compact),
                                        style.lensRefractionAmount.toPx() *
                                            style.pressedLensAmountScale *
                                            glassRuntime.interactionLensScale,
                                        progress,
                                    ),
                                    // Off, against the Backdrop tutorial and deliberately. Dispersion is
                                    // real in Liquid Glass, but Apple's is subtle, and subtlety here is
                                    // not available: this capsule is ~20dp over a hard blue-on-near-black
                                    // track edge, which is the worst case for channel offsets. Two
                                    // recordings of a real drag both show it as a green band above the
                                    // centre and an orange one below — complementary fringes, which read
                                    // as a smear rather than as glass. The track edge is the whole
                                    // backdrop this thumb ever refracts, so there is no case where the
                                    // aberration has gentle content to work with.
                                    chromaticAberration = false,
                                    depthEffect = true,
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Ambient.copy(
                                    alpha = lerp(style.thumbRestingHighlightAlpha, 1f, progress),
                                )
                            },
                            shadow = {
                                val progress = dampedDragAnimation.pressProgress
                                Shadow(
                                    radius = lerp(3.5f, 7f, progress).dp,
                                    offset = DpOffset(0.dp, lerp(0.5f, 1.5f, progress).dp),
                                    color = Color.Black.copy(alpha = lerp(0.10f, 0.16f, progress)),
                                )
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(radius = 4.dp * progress, alpha = progress)
                            },
                            // Scale: use the shared drag animation for press expansion, then add a
                            // small velocity stretch so dragging feels elastic while the backdrop
                            // sampling stays anchored.
                            layerBlock = {
                                val progress = dampedDragAnimation.deformationProgress
                                val velocity = dampedDragAnimation.velocity / 60f
                                val velocityStretch = (velocity * 0.55f).fastCoerceIn(-0.16f, 0.16f)
                                val velocitySquash = (velocity * 0.20f).fastCoerceIn(-0.10f, 0.10f)
                                scaleX = dampedDragAnimation.scaleX / (1f - velocityStretch)
                                scaleY = lerp(1f, style.pressedHeightScale, progress) * (1f - velocitySquash)
                            },
                            // Surface: fades from a light tint (rest) toward near-transparent (press)
                            // so the intensified refraction shows through clearly.
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                val alpha =
                                    lerp(
                                        style.thumbGlassSurfaceAlpha,
                                        style.thumbPressedSurfaceAlpha,
                                        progress,
                                    )
                                drawRect(Color.White.copy(alpha = alpha))
                            },
                        )
                    } else {
                        Modifier.appSquircleBackground(
                            Color.White.copy(alpha = SliderThumbFallbackSurfaceAlpha),
                            999.dp,
                        )
                    },
                ).width(style.thumbWidth)
                .height(style.thumbHeight),
        )
    }
}

private fun sliderValueAt(
    offset: Offset,
    widthPx: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    isLtr: Boolean,
): Float {
    val safeRange = liquidFiniteRange(valueRange)
    val safeWidth = widthPx.takeIf { it.isFinite() && it > 0f } ?: 1f
    val rawFraction = offset.x / safeWidth
    val fraction = rawFraction.takeIf(Float::isFinite)?.fastCoerceIn(0f, 1f) ?: 0f
    val resolvedFraction = if (isLtr) fraction else 1f - fraction
    return safeRange.start + (safeRange.endInclusive - safeRange.start) * resolvedFraction
}

internal fun sanitizeLiquidSliderKeyPoints(
    keyPoints: List<LiquidSliderKeyPoint>,
    valueRange: ClosedFloatingPointRange<Float>,
): List<LiquidSliderKeyPoint> {
    val safeRange = liquidFiniteRange(valueRange)
    return keyPoints.mapNotNull { keyPoint ->
        if (
            !keyPoint.value.isFinite() ||
            !keyPoint.size.value.isFinite() ||
            keyPoint.size.value < 0f
        ) {
            null
        } else {
            keyPoint.copy(value = keyPoint.value.coerceIn(safeRange))
        }
    }
}

internal fun resolveSliderTarget(
    target: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    keyPoints: List<LiquidSliderKeyPoint>,
    snapToKeyPoints: Boolean,
    snapThreshold: Float?,
): Float {
    val safeRange = liquidFiniteRange(valueRange)
    val bounded = liquidFiniteValue(target, safeRange)
    val safeKeyPoints = sanitizeLiquidSliderKeyPoints(keyPoints, safeRange)
    if (!snapToKeyPoints || safeKeyPoints.isEmpty()) {
        return bounded
    }
    val closest =
        safeKeyPoints
            .minByOrNull { keyPoint -> abs(keyPoint.value - bounded) }
            ?.value
            ?: return bounded
    if (snapThreshold != null && abs(closest - bounded) > snapThreshold) {
        return bounded
    }
    return closest
}

internal fun resolveSliderProgressChange(
    currentValue: Float,
    target: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    keyPoints: List<LiquidSliderKeyPoint>,
    snapToKeyPoints: Boolean,
    snapThreshold: Float?,
): Float? {
    if (!target.isFinite()) return null
    val safeRange = liquidFiniteRange(valueRange)
    val safeCurrentValue = liquidFiniteValue(currentValue, safeRange)
    val safeTarget = liquidFiniteValue(target, safeRange, fallback = safeCurrentValue)
    if (safeTarget == safeCurrentValue) return null
    val targetDirection = safeTarget.compareTo(safeCurrentValue)
    val safeKeyPoints = sanitizeLiquidSliderKeyPoints(keyPoints, safeRange)
    val resolved =
        resolveSliderTarget(
            target = safeTarget,
            valueRange = safeRange,
            keyPoints = safeKeyPoints,
            snapToKeyPoints = snapToKeyPoints,
            snapThreshold = snapThreshold,
        )
    val resolvedDirection = resolved.compareTo(safeCurrentValue)
    if (resolvedDirection == targetDirection) return resolved
    if (!snapToKeyPoints || safeKeyPoints.isEmpty() || snapThreshold != null) {
        return safeTarget
    }
    return if (targetDirection > 0) {
        safeKeyPoints
            .minOfOrNull { keyPoint ->
                keyPoint.value.takeIf { value -> value > safeCurrentValue } ?: Float.POSITIVE_INFINITY
            }?.takeIf(Float::isFinite)
    } else {
        safeKeyPoints
            .maxOfOrNull { keyPoint ->
                keyPoint.value.takeIf { value -> value < safeCurrentValue } ?: Float.NEGATIVE_INFINITY
            }?.takeIf(Float::isFinite)
    }
}

internal fun valueProgress(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float {
    val safeRange = liquidFiniteRange(valueRange)
    val span = safeRange.endInclusive - safeRange.start
    if (!span.isFinite() || span <= 0f) return 0f
    val safeValue = liquidFiniteValue(value, safeRange)
    val progress = (safeValue - safeRange.start) / span
    return progress.takeIf(Float::isFinite)?.fastCoerceIn(0f, 1f) ?: 0f
}

internal fun sliderVisualProgress(
    progress: Float,
    isLtr: Boolean,
): Float = if (isLtr) progress else 1f - progress

internal val LiquidSliderMinimumInteractiveHeight = 48.dp

/**
 * How far the thumb's *left edge* may travel, so the capsule stays whole at both ends.
 *
 * Zero when a thumb is somehow wider than its track, which keeps it parked rather than inverted.
 */
internal fun liquidSliderThumbTravel(
    trackWidth: Float,
    thumbWidth: Float,
): Float = (trackWidth - thumbWidth).coerceAtLeast(0f)

/**
 * Where the thumb's *centre* sits at [progress] — which is also where the filled track has to end and
 * where a key-point dot has to sit, so all three read from here.
 *
 * Deliberately not `trackWidth * progress`: the centre travels the inset span, so at 0 it rests half a
 * thumb in rather than hanging off the edge.
 */
internal fun liquidSliderCenterAt(
    trackWidth: Float,
    thumbWidth: Float,
    progress: Float,
): Float = thumbWidth / 2f + liquidSliderThumbTravel(trackWidth, thumbWidth) * progress

private class LiquidSliderHapticState {
    private var edgeFeedbackTriggered = false
    private var activeKeyPoint: Float? = null

    fun reset(currentValue: Float) {
        edgeFeedbackTriggered = false
        activeKeyPoint = null
        lastValue = currentValue
    }

    private var lastValue = Float.NaN

    fun handleHapticFeedback(
        currentValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        hapticFeedback: HapticFeedback,
        keyPoints: List<LiquidSliderKeyPoint>,
    ) {
        val isAtEdge = currentValue == valueRange.start || currentValue == valueRange.endInclusive
        if (isAtEdge && !edgeFeedbackTriggered) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            edgeFeedbackTriggered = true
        } else if (!isAtEdge) {
            edgeFeedbackTriggered = false
        }

        if (keyPoints.isNotEmpty() && !isAtEdge) {
            val rangeSpan = valueRange.endInclusive - valueRange.start
            val threshold = rangeSpan * 0.005f
            val previousValue = lastValue
            val hit =
                keyPoints
                    .map { keyPoint -> keyPoint.value.coerceIn(valueRange) }
                    .firstOrNull { keyPointValue ->
                        abs(keyPointValue - currentValue) <= threshold ||
                            (
                                !previousValue.isNaN() &&
                                    keyPointValue in
                                    minOf(previousValue, currentValue)..maxOf(previousValue, currentValue)
                            )
                    }
            if (hit != null && activeKeyPoint != hit) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            activeKeyPoint = hit
        }
        lastValue = currentValue
    }
}

@Composable
private fun LiquidSliderKeyPointDot(
    keyPoint: LiquidSliderKeyPoint,
    style: LiquidTrackSliderStyle,
    animation: DampedDragAnimation,
    progress: Float,
    visualProgress: Float,
    trackWidthPx: Float,
    thumbWidthPx: Float,
) {
    val isActive by remember(animation, progress) {
        derivedStateOf { animation.progress >= progress }
    }
    // The Miuix background uses an SDF shader. Keep that exact rendering path and isolate its color
    // changes here: crossing a dot must not recompose the slider's material or gesture tree. A dot
    // with an explicit color does not need to observe the animation at all.
    val color = if (keyPoint.color.isSpecified) {
        keyPoint.color
    } else if (isActive) {
        style.keyPointActiveColor
    } else {
        style.keyPointColor
    }
    Box(
        Modifier
            .graphicsLayer {
                translationX = liquidSliderCenterAt(trackWidthPx, thumbWidthPx, visualProgress) - size.width / 2f
            }.appSquircleBackground(color, 999.dp)
            .size(keyPoint.size),
    )
}

@Immutable
private data class LiquidTrackSliderStyle(
    val activeColor: Color,
    val inactiveColor: Color,
    val keyPointColor: Color = Color.White.copy(alpha = 0.74f),
    val keyPointActiveColor: Color = Color.White.copy(alpha = 0.92f),
    val trackHeight: Dp,
    val thumbWidth: Dp,
    val thumbHeight: Dp,
    val pressedWidthScale: Float,
    val pressedHeightScale: Float,
    // Lens refraction for the glass thumb. Mirrors the Backdrop "Glass Slider" tutorial's
    // proportions (height ~0.375x, amount ~0.5x of the thumb height), which keeps refractionHeight
    // safely under the capsule corner radius (thumbHeight / 2) and refractionAmount under the min
    // dimension (thumbHeight) — exceeding either produces corner discontinuities.
    val lensRefractionHeight: Dp = thumbHeight * 0.375f,
    val lensRefractionAmount: Dp = thumbHeight * 0.5f,
    val pressedLensHeightScale: Float = 1.35f,
    val pressedLensAmountScale: Float = 1.65f,
    val thumbRestingBlur: Dp = thumbHeight * 0.10f,
    // Theme-aware thumb tuning. In dark mode the black shadow adds almost no definition, so the
    // clear thumb leans on the white surface + highlight; we lift both a touch to keep it crisp.
    val thumbGlassSurfaceAlpha: Float = SliderThumbGlassSurfaceAlpha,
    val thumbPressedSurfaceAlpha: Float = SliderThumbPressedSurfaceAlpha,
    val thumbRestingHighlightAlpha: Float = SliderThumbRestingHighlightAlpha,
    /**
     * How much the thumb's lens brightens what it refracts, once grabbed.
     *
     * This is the one place the thumb goes past the Backdrop tutorial's effect stack, which stops at a
     * fixed `vibrancy()`. `vibrancy()` is documented as exactly `colorControls(saturation = 1.5f)`, so
     * the resting values reproduce it byte for byte and only the *active* end is new — the glass catches
     * light as the finger lands on it rather than looking identical grabbed and idle.
     *
     * That is also how this resolves Apple's rule for content-layer controls, which says a slider
     * "takes on a Liquid Glass appearance to emphasize its interactivity when a person activates it".
     * The app keeps its glass at rest, which the Backdrop slider does too and which the rest of this
     * app's language expects; the emphasis Apple asks for arrives as light instead of as a swap from
     * standard material to glass. A small capsule turning opaque at rest would read as foreign here.
     *
     * **Light means brightness, not saturation.** [pressedSaturation] deliberately equals
     * [restingSaturation] — see its own note for the measurement that forced that, and for why a chroma
     * lift on this particular surface cannot help.
     */
    val restingSaturation: Float = SliderThumbVibrancySaturation,
    val pressedSaturation: Float = SliderThumbPressedSaturation,
    val pressedBrightness: Float = SliderThumbPressedBrightness,
)

// Glass thumb (Backdrop available): clear refractive capsule matching the Glass Slider tutorial.
// Light mode keeps the thumb nearly clear; dark mode lifts the white surface + highlight a touch
// because the black drop shadow adds little contrast over dark backgrounds.
private const val SliderThumbGlassSurfaceAlpha = 0.10f
private const val SliderThumbGlassSurfaceAlphaDark = 0.18f
private const val SliderThumbPressedSurfaceAlpha = 0.04f
private const val SliderThumbPressedSurfaceAlphaDark = 0.08f
private const val SliderThumbRestingHighlightAlpha = 0.5f
private const val SliderThumbRestingHighlightAlphaDark = 0.7f

// Fallback thumb (glass effects disabled): needs an opaque-ish fill to stay visible without a lens.
private const val SliderThumbFallbackSurfaceAlpha = 0.42f

/** What `vibrancy()` is, per the Backdrop docs: `colorControls(saturation = 1.5f)`. */
internal const val SliderThumbVibrancySaturation = 1.5f

/**
 * Equal to [SliderThumbVibrancySaturation] on purpose: **the thumb's chroma must not rise on press.**
 *
 * This was 1.85f, and the note here said an aggressive lift "reads as a colour bug rather than as light…
 * Tuned on the API 37 AVD against both themes". Both halves were wrong in the same way — the tuning only
 * ever saw the *resting* thumb, because `adb` input cannot reach the active state, so the value that
 * governs the active end was never looked at. Two screen recordings of a real mouse drag showed it.
 *
 * The mechanism is the Backdrop effect order, **color filter ⇒ blur ⇒ lens**. `colorControls` is the
 * colour filter, so it saturates the backdrop *before* the lens splits it. Raising chroma there therefore
 * feeds whatever the lens does next, which is the one thing a colour control on this surface must not do.
 *
 * Measured over the thumb's own pixels — masked to the capsule, excluding the track and the recording's
 * pointer sprite — the rest-to-press amplification of mean chroma went **×2.86 before this change to
 * ×1.66 after**. So the retraction is worth keeping and it is *not* the whole story: see the note on
 * `chromaticAberration` at the call site for what actually dominates the visible artifact.
 *
 * The emphasis Apple asks for arrives through [SliderThumbPressedBrightness] alone. Brightness raises
 * luminance without multiplying chroma, so it cannot feed the lens the same way. The knob stays a knob
 * rather than being deleted because the ramp machinery is right and only the channel was wrong.
 */
internal const val SliderThumbPressedSaturation = SliderThumbVibrancySaturation

/**
 * Residual frost the pressed thumb keeps, as a fraction of [LiquidTrackSliderStyle.thumbRestingBlur].
 *
 * The press used to clear the frosting to exactly zero, which is the Backdrop tutorial's look and is fine
 * over a photograph. This thumb refracts a 4dp blue track on near-black instead, and with no blur left at
 * the moment the lens amount is at its largest the sampled edge tears — visible in both recordings as the
 * track's blue arriving inside the capsule in two or three disconnected blobs rather than as one edge.
 *
 * A fifth reads as "cleared" to the eye while still band-limiting what the lens samples.
 */
internal const val SliderThumbPressedBlurFloorFraction = 0.2f

/**
 * The whole of the thumb's "catches light" now, so it carries what the saturation ramp used to share.
 *
 * Doubled from 0.06f. Safe to push where saturation was not: a luminance offset moves all three channels
 * together, so it cannot widen the gap the chromatic aberration opens between them.
 */
internal const val SliderThumbPressedBrightness = 0.12f

private fun thumbGlassSurfaceAlphaFor(isLightTheme: Boolean): Float =
    if (isLightTheme) SliderThumbGlassSurfaceAlpha else SliderThumbGlassSurfaceAlphaDark

private fun thumbRestingHighlightAlphaFor(isLightTheme: Boolean): Float =
    if (isLightTheme) SliderThumbRestingHighlightAlpha else SliderThumbRestingHighlightAlphaDark

private fun thumbPressedSurfaceAlphaFor(isLightTheme: Boolean): Float =
    if (isLightTheme) SliderThumbPressedSurfaceAlpha else SliderThumbPressedSurfaceAlphaDark
