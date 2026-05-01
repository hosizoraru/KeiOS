package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import os.kei.ui.animation.DampedDragAnimation
import os.kei.ui.animation.InteractiveHighlight
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sign
import kotlin.math.tanh

// Adapted from Kyant0/AndroidLiquidGlass catalog components, Apache-2.0.

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    shape: Shape = Capsule(),
    height: Dp = 48.dp,
    horizontalPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val clickInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = if (isInteractive && enabled) {
                    {
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                        val maxOffset = size.minDimension
                        val offset = interactiveHighlight.offset
                        val initialDerivative = 0.05f
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX = scale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (size.width / size.height).fastCoerceAtMost(1f)
                        scaleY = scale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (size.height / size.width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = tint.alpha * 0.72f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .clickable(
                interactionSource = clickInteractionSource,
                indication = if (isInteractive) null else LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive && enabled) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
            }
            .height(height)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppInteractiveTokens.controlContentGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = Color.Unspecified
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = if (checkedColor.isSpecified) {
        checkedColor
    } else if (isLightTheme) {
        Color(0xFF34C759)
    } else {
        Color(0xFF30D158)
    }
    val trackColor = if (isLightTheme) {
        Color(0xFF787878).copy(alpha = 0.20f)
    } else {
        Color(0xFF787880).copy(alpha = 0.36f)
    }
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (!enabled) return@DampedDragAnimation
                fraction = if (didDrag) {
                    if (targetValue >= 0.5f) 1f else 0f
                } else {
                    if (selected()) 0f else 1f
                }
                onSelect(fraction == 1f)
                didDrag = false
            },
            onDrag = { _, dragAmount ->
                if (!enabled) return@DampedDragAnimation
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction = if (isLtr) {
                    (fraction + delta).fastCoerceIn(0f, 1f)
                } else {
                    (fraction - delta).fastCoerceIn(0f, 1f)
                }
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { value -> dampedDragAnimation.updateValue(value) }
    }
    val externalSelected = selected()
    LaunchedEffect(externalSelected) {
        val target = if (externalSelected) 1f else 0f
        if (target != fraction) {
            fraction = target
            dampedDragAnimation.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(
        backdrop,
        rememberBackdrop(trackBackdrop) { drawBackdrop ->
            val progress = dampedDragAnimation.pressProgress
            val scaleX = lerp(2f / 3f, 0.75f, progress)
            val scaleY = lerp(0f, 0.75f, progress)
            scale(scaleX, scaleY) { drawBackdrop() }
        }
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
            }
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(externalSelected)
                if (enabled) {
                    onClick {
                        onSelect(!selected())
                        true
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    drawRect(lerpColor(trackColor, accentColor, dampedDragAnimation.value))
                }
                .size(64.dp, 28.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    translationX = if (isLtr) {
                        lerp(padding, padding + dragWidth, dampedDragAnimation.value)
                    } else {
                        lerp(-padding, -(padding + dragWidth), dampedDragAnimation.value)
                    }
                }
                .semantics { role = Role.Switch }
                .then(if (enabled) dampedDragAnimation.modifier else Modifier)
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8.dp.toPx() * (1f - progress))
                        lens(
                            5.dp.toPx() * progress,
                            10.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 4.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 1f - dampedDragAnimation.pressProgress))
                    }
                )
                .size(40.dp, 24.dp)
        )
    }
}

@Composable
fun LiquidPrimaryToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    LiquidToggle(
        selected = selected,
        onSelect = onSelect,
        backdrop = backdrop,
        modifier = modifier,
        enabled = enabled,
        checkedColor = MiuixTheme.colorScheme.primary
    )
}

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val trackColor = if (isLightTheme) {
        Color(0xFF787878).copy(alpha = 0.20f)
    } else {
        Color(0xFF787880).copy(alpha = 0.36f)
    }
    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value(), valueRange, steps = 0)
                if (enabled) {
                    setProgress { target ->
                        onValueChange(target.coerceIn(valueRange))
                        true
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth.coerceAtLeast(1)
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (enabled && didDrag) {
                        onValueChange(targetValue)
                    }
                    didDrag = false
                },
                onDrag = { _, dragAmount ->
                    if (!enabled) return@DampedDragAnimation
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    val target = if (isLtr) targetValue + delta else targetValue - delta
                    onValueChange(target.coerceIn(valueRange))
                }
            )
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { nextValue ->
                    if (dampedDragAnimation.targetValue != nextValue) {
                        dampedDragAnimation.updateValue(nextValue)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(trackColor)
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(valueRange, isLtr, trackWidth) {
                                detectTapGestures { position ->
                                    val delta = (valueRange.endInclusive - valueRange.start) *
                                        (position.x / trackWidth)
                                    val target = if (isLtr) {
                                        valueRange.start + delta
                                    } else {
                                        valueRange.endInclusive - delta
                                    }.coerceIn(valueRange)
                                    dampedDragAnimation.animateToValue(target)
                                    onValueChange(target)
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .height(6.dp)
                    .fillMaxWidth()
            )
            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(6.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress)
                            .fastRoundToInt()
                            .coerceIn(0, constraints.maxWidth)
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX = (
                        -size.width / 2f + trackWidth * dampedDragAnimation.progress
                        ).fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) *
                        if (isLtr) 1f else -1f
                }
                .then(if (enabled) dampedDragAnimation.modifier else Modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) { drawBackdrop() }
                        }
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8.dp.toPx() * (1f - progress))
                        lens(
                            10.dp.toPx() * progress,
                            14.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 4.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 1f - dampedDragAnimation.pressProgress))
                    }
                )
                .size(40.dp, 24.dp)
        )
    }
}

private val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }
private val LocalLiquidBottomTabSelectionProgress = staticCompositionLocalOf<(Int) -> Float> { { 0f } }
private val LocalLiquidBottomTabContentColor = staticCompositionLocalOf<(Int) -> Color> { { Color.Unspecified } }
private val LocalLiquidBottomTabInteractive = staticCompositionLocalOf { true }
private val LocalLiquidBottomTabPressHandler = staticCompositionLocalOf<(Int, Boolean) -> Unit> { { _, _ -> } }

@Composable
fun liquidBottomTabSelectionProgress(tabIndex: Int): Float {
    return LocalLiquidBottomTabSelectionProgress.current(tabIndex)
}

@Composable
fun liquidBottomTabContentColor(tabIndex: Int): Color {
    return LocalLiquidBottomTabContentColor.current(tabIndex)
}

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    tabIndex: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    val selectionProgress = liquidBottomTabSelectionProgress(tabIndex)
    val interactive = LocalLiquidBottomTabInteractive.current
    val onItemPressed = LocalLiquidBottomTabPressHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val targetScale = when {
        interactive && pressed && enabled -> lerp(0.92f, 0.96f, selectionProgress)
        selectionProgress > 0f -> lerp(1f, scale(), selectionProgress)
        else -> 1f
    }
    val animatedScale by appMotionFloatState(
        targetValue = targetScale,
        durationMillis = 160,
        label = "liquid_bottom_tab_scale"
    )

    LaunchedEffect(interactive, pressed, tabIndex) {
        if (interactive) {
            onItemPressed(tabIndex, pressed)
        }
    }
    DisposableEffect(interactive, tabIndex) {
        onDispose {
            if (interactive) {
                onItemPressed(tabIndex, false)
            }
        }
    }

    Column(
        modifier = modifier
            .clip(ContinuousCapsule)
            .then(
                if (interactive) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Tab,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color = if (isSystemInDarkTheme()) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.28f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    },
    content: @Composable RowScope.() -> Unit
) {
    val safeTabsCount = tabsCount.coerceAtLeast(1)
    val isLightTheme = !isSystemInDarkTheme()
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val horizontalPadding = AppChromeTokens.floatingBottomBarHorizontalPadding
    val outerHeight = AppChromeTokens.floatingBottomBarOuterHeight
    val innerHeight = AppChromeTokens.floatingBottomBarInnerHeight
    val effectBlur = UiPerformanceBudget.backdropBlur
    val effectLens = UiPerformanceBudget.backdropLens
    val inactiveContentColor = if (isLightTheme) {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.88f)
    } else {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.92f)
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - (horizontalPadding * 2).toPx()) / safeTabsCount
        }.coerceAtLeast(1f)
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth.coerceAtLeast(1))
                    .fastCoerceIn(-1f, 1f)
                with(density) {
                    4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(safeTabsCount) {
            mutableIntStateOf(selectedTabIndex().fastCoerceIn(0, safeTabsCount - 1))
        }
        var pressedTabIndex by remember(safeTabsCount) { mutableIntStateOf(-1) }
        val dampedDragAnimation = remember(animationScope, safeTabsCount, isLtr) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().fastCoerceIn(0, safeTabsCount - 1).toFloat(),
                valueRange = 0f..(safeTabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, safeTabsCount - 1)
                    currentIndex = targetIndex
                    if (transitionAnimationsEnabled) {
                        animateToValue(targetIndex.toFloat())
                    } else {
                        snapToValue(targetIndex.toFloat())
                    }
                    animationScope.launch {
                        if (transitionAnimationsEnabled) {
                            offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                        } else {
                            offsetAnimation.snapTo(0f)
                        }
                    }
                },
                onDrag = { _, dragAmount ->
                    val delta = dragAmount.x / tabWidth * if (isLtr) 1f else -1f
                    snapToValue((value + delta).fastCoerceIn(0f, (safeTabsCount - 1).toFloat()))
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        val selectedIndex = selectedTabIndex().fastCoerceIn(0, safeTabsCount - 1)
        LaunchedEffect(selectedIndex) {
            currentIndex = selectedIndex
            if (transitionAnimationsEnabled) {
                dampedDragAnimation.animateToValue(selectedIndex.toFloat())
            } else {
                dampedDragAnimation.snapToValue(selectedIndex.toFloat())
            }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index -> onTabSelected(index) }
        }

        val interactiveHighlight = remember(animationScope, dampedDragAnimation, isLtr) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) {
                            (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        } else {
                            size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        },
                        size.height / 2f
                    )
                },
                highlightStrength = if (isLightTheme) 0.72f else 1.05f,
                highlightRadiusScale = if (isLightTheme) 0.92f else 1.08f
            )
        }
        val dragPressProgress = if (isInteractive) dampedDragAnimation.pressProgress else 0f
        val itemPressProgress by appMotionFloatState(
            targetValue = if (isInteractive && pressedTabIndex >= 0) 1f else 0f,
            durationMillis = 120,
            label = "liquid_bottom_tabs_item_press"
        )
        val pressProgress = max(dragPressProgress, itemPressProgress)
        val selectionProgressProvider: (Int) -> Float = remember(dampedDragAnimation) {
            { tabIndex ->
                (1f - abs(dampedDragAnimation.value - tabIndex)).fastCoerceIn(0f, 1f)
            }
        }
        val tactileLiftPx = with(density) { 1.25.dp.toPx() } * pressProgress
        val tactileScaleX = lerp(1f, 1.006f, pressProgress)
        val tactileScaleY = lerp(1f, 0.996f, pressProgress)

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides { lerp(1f, 1.2f, pressProgress) },
            LocalLiquidBottomTabSelectionProgress provides selectionProgressProvider,
            LocalLiquidBottomTabContentColor provides { inactiveContentColor },
            LocalLiquidBottomTabInteractive provides true,
            LocalLiquidBottomTabPressHandler provides { index, isPressed ->
                when {
                    isPressed -> pressedTabIndex = index
                    pressedTabIndex == index -> pressedTabIndex = -1
                }
            }
        ) {
            Row(
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                        translationY = -tactileLiftPx
                        scaleX = tactileScaleX
                        scaleY = tactileScaleY
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(effectBlur.toPx())
                            lens(effectLens.toPx(), effectLens.toPx())
                        },
                        highlight = { Highlight.Default },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(if (isLightTheme) 0.10f else 0.20f)
                            )
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
                    .height(outerHeight)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides { lerp(1f, 1.2f, pressProgress) },
            LocalLiquidBottomTabSelectionProgress provides selectionProgressProvider,
            LocalLiquidBottomTabContentColor provides { accentColor },
            LocalLiquidBottomTabInteractive provides false
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                        translationY = -tactileLiftPx
                        scaleX = tactileScaleX
                        scaleY = tactileScaleY
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(effectBlur.toPx())
                            lens(effectLens.toPx() * pressProgress, effectLens.toPx() * pressProgress)
                        },
                        highlight = { Highlight.Default.copy(alpha = pressProgress) },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(if (isInteractive) interactiveHighlight.modifier else Modifier)
                    .height(innerHeight)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        Box(
            Modifier
                .padding(horizontal = horizontalPadding)
                .graphicsLayer {
                    translationX = if (isLtr) {
                        dampedDragAnimation.value * tabWidth + panelOffset
                    } else {
                        size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                    }
                    translationY = -tactileLiftPx
                    scaleX = tactileScaleX
                    scaleY = tactileScaleY
                }
                .then(if (isInteractive) interactiveHighlight.gestureModifier else Modifier)
                .then(if (isInteractive) dampedDragAnimation.modifier else Modifier)
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        lens(
                            10.dp.toPx() * pressProgress,
                            14.dp.toPx() * pressProgress,
                            depthEffect = true
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = pressProgress) },
                    shadow = { Shadow(alpha = pressProgress) },
                    innerShadow = {
                        InnerShadow(radius = 8.dp * pressProgress, alpha = pressProgress)
                    },
                    layerBlock = {
                        val clickScale = lerp(1f, 1.045f, itemPressProgress)
                        scaleX = dampedDragAnimation.scaleX * clickScale
                        scaleY = dampedDragAnimation.scaleY * clickScale
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isLightTheme) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.10f),
                            alpha = 1f - pressProgress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * pressProgress))
                    }
                )
                .clearAndSetSemantics {}
                .height(innerHeight)
                .width(with(density) { tabWidth.toDp() })
        )
    }
}
