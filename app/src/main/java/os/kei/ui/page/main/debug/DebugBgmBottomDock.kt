package os.kei.ui.page.main.debug

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.animation.DampedDragAnimation
import os.kei.ui.animation.InteractiveHighlight
import os.kei.ui.page.main.os.appLucideGridIcon
import os.kei.ui.page.main.os.appLucideHomeIcon
import os.kei.ui.page.main.os.appLucideLibraryIcon
import os.kei.ui.page.main.os.appLucideRadioIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

@Composable
internal fun DebugBgmDockGroupContent(
    tabs: List<DebugBgmDockTab>,
    selectedDockKey: String,
    accent: Color,
    expandedProgress: Float,
    compactProgress: Float,
    backdrop: Backdrop? = null,
    compactInteractionSource: MutableInteractionSource? = null,
    onSelectedDockKeyChange: (String) -> Unit
) {
    val expanded = expandedProgress.coerceIn(0f, 1f)
    val compact = compactProgress.coerceIn(0f, 1f)
    val expandedEnabled = expanded > 0.54f
    val compactEnabled = compact > 0.54f
    val safeTabCount = tabs.size.coerceAtLeast(1)
    val selectedIndex = tabs.indexOfFirst { it.key == selectedDockKey }.coerceAtLeast(0)
    val resolvedCompactInteractionSource = compactInteractionSource ?: remember { MutableInteractionSource() }
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val currentExpandedEnabled by rememberUpdatedState(expandedEnabled)
    val currentAnimationsEnabled by rememberUpdatedState(animationsEnabled)
    val animationScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isDark = isSystemInDarkTheme()
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = if (backdrop != null) rememberCombinedBackdrop(backdrop, tabsBackdrop) else null
    val offsetAnimation = remember { Animatable(0f) }
    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            if (totalWidthPx <= 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }
    var currentIndex by remember(safeTabCount) {
        mutableIntStateOf(selectedIndex.fastCoerceIn(0, safeTabCount - 1))
    }
    var pressedTabIndex by remember { mutableIntStateOf(-1) }
    val itemPressProgress by animateFloatAsState(
        targetValue = if (pressedTabIndex >= 0) 1f else 0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "debug_bgm_dock_item_press"
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentHorizontalPadding = AppChromeTokens.floatingBottomBarHorizontalPadding
        val contentVerticalPadding = (AppChromeTokens.floatingBottomBarOuterHeight -
            AppChromeTokens.floatingBottomBarInnerHeight) / 2f
        val tabSlotWidth = ((maxWidth - contentHorizontalPadding * 2f) / safeTabCount.toFloat())
            .coerceAtLeast(42.dp)
        val selectedPillWidth = (tabSlotWidth * DebugBgmDockSelectionWidthFraction)
            .coerceAtLeast(AppChromeTokens.floatingBottomBarInnerHeight + 12.dp)
            .coerceAtMost(tabSlotWidth)
        val selectedPillInset = (tabSlotWidth - selectedPillWidth) / 2f
        val fallbackTabWidthPx = with(density) { tabSlotWidth.toPx() }.coerceAtLeast(1f)
        val selectedPillHeight = AppChromeTokens.floatingBottomBarInnerHeight.coerceAtMost(maxHeight)
        class DampedDragAnimationHolder {
            var instance: DampedDragAnimation? = null
        }

        val holder = remember { DampedDragAnimationHolder() }
        val dampedDragAnimation = remember(animationScope, safeTabCount, density, isLtr) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentIndex.toFloat(),
                valueRange = 0f..(safeTabCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = DebugBgmDockPressedScale,
                gestureKey = safeTabCount to isLtr,
                canDrag = { offset ->
                    if (!currentExpandedEnabled) return@DampedDragAnimation false
                    val animation = holder.instance ?: return@DampedDragAnimation true
                    val measuredTabWidthPx = tabWidthPx.takeIf { it > 0f } ?: fallbackTabWidthPx
                    if (measuredTabWidthPx <= 0f || totalWidthPx <= 0f) return@DampedDragAnimation true
                    val paddingPx = with(density) { contentHorizontalPadding.toPx() }
                    val indicatorX = animation.value * measuredTabWidthPx
                    val globalTouchX = if (isLtr) {
                        paddingPx + indicatorX + offset.x
                    } else {
                        totalWidthPx - paddingPx - measuredTabWidthPx - indicatorX + offset.x
                    }
                    globalTouchX in 0f..totalWidthPx
                },
                onDragStarted = {
                    pressedTabIndex = currentIndex
                },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, safeTabCount - 1)
                    currentIndex = targetIndex
                    pressedTabIndex = -1
                    if (currentAnimationsEnabled) {
                        animateToValue(targetIndex.toFloat())
                    } else {
                        snapToValue(targetIndex.toFloat())
                    }
                    animationScope.launch {
                        if (currentAnimationsEnabled) {
                            offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                        } else {
                            offsetAnimation.snapTo(0f)
                        }
                    }
                },
                onDrag = { _, dragAmount ->
                    val measuredTabWidthPx = tabWidthPx.takeIf { it > 0f } ?: fallbackTabWidthPx
                    val dragDirection = if (isLtr) 1f else -1f
                    snapToValue(
                        (value + dragAmount.x / measuredTabWidthPx * dragDirection)
                            .fastCoerceIn(0f, (safeTabCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        holder.instance = dampedDragAnimation
        val pressProgress = dampedDragAnimation.pressProgress
        val combinedInteractionProgress = max(pressProgress, itemPressProgress)
        val dockLiftPx = with(density) { 1.25.dp.toPx() } * combinedInteractionProgress
        val dockScaleX = lerpFloat(1f, 1.006f, combinedInteractionProgress)
        val dockScaleY = lerpFloat(1f, 0.996f, combinedInteractionProgress)
        val selectedContentScale = lerpFloat(
            1f,
            DebugBgmDockSelectedContentPressedScale,
            combinedInteractionProgress
        )
        val interactiveHighlight = if (
            backdrop != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            remember(animationScope, dampedDragAnimation, tabWidthPx, isLtr) {
                InteractiveHighlight(
                    animationScope = animationScope,
                    position = { size, _ ->
                        Offset(
                            if (isLtr) {
                                (dampedDragAnimation.value + 0.5f) *
                                    (tabWidthPx.takeIf { it > 0f } ?: fallbackTabWidthPx) + panelOffset
                            } else {
                                size.width - (dampedDragAnimation.value + 0.5f) *
                                    (tabWidthPx.takeIf { it > 0f } ?: fallbackTabWidthPx) + panelOffset
                            },
                            size.height / 2f
                        )
                    },
                    highlightColor = Color.White,
                    highlightStrength = if (isDark) 0.90f else 0.60f,
                    highlightRadiusScale = if (isDark) 1.08f else 0.90f
                )
            }
        } else {
            null
        }

        LaunchedEffect(selectedIndex, safeTabCount) {
            currentIndex = selectedIndex.fastCoerceIn(0, safeTabCount - 1)
        }
        LaunchedEffect(dampedDragAnimation, animationsEnabled) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    if (animationsEnabled) {
                        dampedDragAnimation.animateToValue(index.toFloat())
                    } else {
                        dampedDragAnimation.snapToValue(index.toFloat())
                    }
                    tabs.getOrNull(index)?.key?.let(onSelectedDockKeyChange)
                }
        }
        val selectedPillOffset = contentHorizontalPadding +
            tabSlotWidth * dampedDragAnimation.value + selectedPillInset

        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (expanded >= compact) 1f else 0f)
                .onGloballyPositioned { coords ->
                    val measuredTotalWidthPx = coords.size.width.toFloat()
                    if (abs(totalWidthPx - measuredTotalWidthPx) > 0.5f) {
                        totalWidthPx = measuredTotalWidthPx
                    }
                    val contentWidthPx = measuredTotalWidthPx - with(density) {
                        (contentHorizontalPadding * 2f).toPx()
                    }
                    val measuredTabWidthPx = (contentWidthPx / safeTabCount).coerceAtLeast(0f)
                    if (abs(tabWidthPx - measuredTabWidthPx) > 0.5f) {
                        tabWidthPx = measuredTabWidthPx
                    }
                }
                .graphicsLayer {
                    alpha = expanded
                    translationX = panelOffset
                    translationY = -dockLiftPx
                    scaleX = (0.96f + 0.04f * expanded) * dockScaleX
                    scaleY = (0.96f + 0.04f * expanded) * dockScaleY
                }
                .clip(ContinuousCapsule)
                .then(interactiveHighlight?.modifier ?: Modifier)
                .padding(horizontal = contentHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { tabIndex, tab ->
                val selectionProgress = (1f - abs(dampedDragAnimation.value - tabIndex))
                    .coerceIn(0f, 1f)
                DebugBgmExpandedDockTab(
                    tab = tab,
                    selected = selectionProgress > 0.52f,
                    selectionProgress = selectionProgress,
                    selectedContentScale = selectedContentScale,
                    accent = accent,
                    activeTint = false,
                    onClick = {
                        currentIndex = tabIndex
                    },
                    onPressedChange = { pressed ->
                        pressedTabIndex = if (pressed) tabIndex else -1
                    },
                    enabled = expandedEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        Row(
            modifier = Modifier
                .clearAndSetSemantics {}
                .offset(y = contentVerticalPadding)
                .width(maxWidth)
                .height(selectedPillHeight)
                .alpha(0f)
                .zIndex(if (expanded >= compact) 1.5f else 0f)
                .clip(ContinuousCapsule)
                .then(if (backdrop != null) Modifier.layerBackdrop(tabsBackdrop) else Modifier)
                .graphicsLayer {
                    alpha = expanded
                    translationX = panelOffset
                    translationY = -dockLiftPx
                    scaleX = (0.96f + 0.04f * expanded) * dockScaleX
                    scaleY = (0.96f + 0.04f * expanded) * dockScaleY
                }
                .padding(horizontal = contentHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { tabIndex, tab ->
                val selectionProgress = (1f - abs(dampedDragAnimation.value - tabIndex))
                    .coerceIn(0f, 1f)
                DebugBgmExpandedDockTab(
                    tab = tab,
                    selected = selectionProgress > 0.52f,
                    selectionProgress = selectionProgress,
                    selectedContentScale = selectedContentScale,
                    accent = accent,
                    activeTint = true,
                    onClick = {},
                    onPressedChange = {},
                    enabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        DebugBgmDockSelectionPill(
            modifier = Modifier
                .offset(x = selectedPillOffset, y = contentVerticalPadding)
                .width(selectedPillWidth)
                .height(selectedPillHeight)
                .zIndex(if (expanded >= compact) 2f else 0f)
                .graphicsLayer {
                    alpha = expanded
                    translationX = panelOffset
                    translationY = -dockLiftPx
                    scaleX = (0.96f + 0.04f * expanded) * dockScaleX
                    scaleY = (0.96f + 0.04f * expanded) * dockScaleY
                }
                .then(interactiveHighlight?.gestureModifier ?: Modifier)
                .then(if (expandedEnabled) dampedDragAnimation.modifier else Modifier),
            backdrop = combinedBackdrop ?: backdrop,
            isDark = isDark,
            pressProgress = combinedInteractionProgress,
            itemPressProgress = itemPressProgress,
            dragScaleX = dampedDragAnimation.scaleX,
            dragScaleY = dampedDragAnimation.scaleY,
            velocity = dampedDragAnimation.velocity
        )

        val compactTab = tabs.firstOrNull { it.key == selectedDockKey } ?: tabs.last()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (compact > expanded) 1f else 0f)
                .graphicsLayer {
                    alpha = compact
                    scaleX = 0.88f + 0.12f * compact
                    scaleY = 0.88f + 0.12f * compact
                }
                .clip(CircleShape)
                .then(
                    if (compactEnabled) {
                        Modifier.clickable(
                            interactionSource = resolvedCompactInteractionSource,
                            indication = null
                        ) {
                            onSelectedDockKeyChange(compactTab.key)
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            DebugBgmDockTabIcon(
                icon = compactTab.icon,
                label = compactTab.label,
                selected = true,
                accent = accent,
                iconSize = 27.dp
            )
        }
    }
}

@Composable
private fun DebugBgmDockSelectionPill(
    backdrop: Backdrop?,
    isDark: Boolean,
    pressProgress: Float,
    itemPressProgress: Float,
    dragScaleX: Float,
    dragScaleY: Float,
    velocity: Float,
    modifier: Modifier = Modifier
) {
    val clampedPress = pressProgress.coerceIn(0f, 1f)
    val clickScale = lerpFloat(1f, DebugBgmDockClickScale, itemPressProgress.coerceIn(0f, 1f))
    val velocityScale = velocity / 10f
    val deformationScaleX = dragScaleX * clickScale /
        (1f - (velocityScale * DebugBgmDockVelocityScaleXFactor)
            .coerceIn(-DebugBgmDockVelocityScaleClamp, DebugBgmDockVelocityScaleClamp))
    val deformationScaleY = dragScaleY * clickScale *
        (1f - (velocityScale * DebugBgmDockVelocityScaleYFactor)
            .coerceIn(-DebugBgmDockVelocityScaleClamp, DebugBgmDockVelocityScaleClamp))
    val neutralFill = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }
    Box(
        modifier = modifier
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            if (clampedPress > 0f) {
                                lens(
                                    5.dp.toPx() * clampedPress,
                                    7.dp.toPx() * clampedPress,
                                    true
                                )
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = clampedPress)
                        },
                        shadow = {
                            Shadow(alpha = clampedPress)
                        },
                        innerShadow = {
                            InnerShadow(radius = 8.dp * clampedPress, alpha = clampedPress)
                        },
                        layerBlock = {
                            scaleX = deformationScaleX
                            scaleY = deformationScaleY
                        },
                        onDrawSurface = {
                            drawRect(neutralFill, alpha = 1f - clampedPress)
                            drawRect(Color.Black.copy(alpha = 0.03f * clampedPress))
                        }
                    )
                } else {
                    Modifier
                        .clip(ContinuousCapsule)
                        .background(neutralFill, ContinuousCapsule)
                }
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ContinuousCapsule)
                .background(
                    if (isDark) {
                        Color.White.copy(alpha = 0.03f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    },
                    ContinuousCapsule
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ContinuousCapsule)
                .border(1.dp, borderColor, ContinuousCapsule)
        )
    }
}

@Composable
private fun DebugBgmExpandedDockTab(
    tab: DebugBgmDockTab,
    selected: Boolean,
    selectionProgress: Float,
    selectedContentScale: Float,
    accent: Color,
    activeTint: Boolean,
    onClick: () -> Unit,
    onPressedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val normalizedSelectionProgress = selectionProgress.coerceIn(0f, 1f)
    val itemScale by animateFloatAsState(
        targetValue = when {
            pressed && enabled -> lerpFloat(0.92f, 0.96f, normalizedSelectionProgress)
            normalizedSelectionProgress > 0f -> {
                lerpFloat(1f, selectedContentScale, normalizedSelectionProgress)
            }
            else -> 1f
        },
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "debug_bgm_dock_tab_press_scale"
    )
    LaunchedEffect(pressed, enabled) {
        if (enabled) onPressedChange(pressed)
    }
    Box(
        modifier = modifier
            .clip(ContinuousCapsule)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            },
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val tintProgress = if (activeTint) 1f else 0f
            DebugBgmDockTabIcon(
                icon = tab.icon,
                label = tab.label,
                selected = selected,
                accent = accent,
                selectionProgress = tintProgress
            )
            Text(
                text = tab.label,
                color = DebugBgmDockTint(
                    selected = selected,
                    accent = accent,
                    selectionProgress = tintProgress
                ),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun DebugBgmDockTabIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    iconSize: Dp = 24.dp,
    selectionProgress: Float = if (selected) 1f else 0f
) {
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = DebugBgmDockTint(
            selected = selected,
            accent = accent,
            selectionProgress = selectionProgress
        ),
        modifier = Modifier.size(iconSize)
    )
}

@Composable
internal fun rememberDebugBgmDockTabs(): List<DebugBgmDockTab> {
    val homeLabel = stringResource(R.string.debug_component_lab_nav_home)
    val discoverLabel = stringResource(R.string.debug_component_lab_nav_discover)
    val radioLabel = stringResource(R.string.debug_component_lab_nav_radio)
    val libraryLabel = stringResource(R.string.debug_component_lab_nav_library)
    val homeIcon = appLucideHomeIcon()
    val discoverIcon = appLucideGridIcon()
    val radioIcon = appLucideRadioIcon()
    val libraryIcon = appLucideLibraryIcon()
    return remember(
        homeLabel,
        discoverLabel,
        radioLabel,
        libraryLabel,
        homeIcon,
        discoverIcon,
        radioIcon,
        libraryIcon
    ) {
        listOf(
            DebugBgmDockTab(DebugBgmDockKeys.Home, homeIcon, homeLabel),
            DebugBgmDockTab(DebugBgmDockKeys.Discover, discoverIcon, discoverLabel),
            DebugBgmDockTab(DebugBgmDockKeys.Radio, radioIcon, radioLabel),
            DebugBgmDockTab(DebugBgmDockKeys.Library, libraryIcon, libraryLabel)
        )
    }
}

@Composable
private fun DebugBgmDockTint(
    selected: Boolean,
    accent: Color,
    selectionProgress: Float = if (selected) 1f else 0f
): Color = lerpColor(
    MiuixTheme.colorScheme.onBackground.copy(alpha = 0.90f),
    accent,
    selectionProgress.coerceIn(0f, 1f)
)

internal object DebugBgmDockKeys {
    const val Home = "home"
    const val Discover = "discover"
    const val Radio = "radio"
    const val Library = "library"
}

internal data class DebugBgmDockTab(
    val key: String,
    val icon: ImageVector,
    val label: String
)

private const val DebugBgmDockSelectionWidthFraction = 1f
private const val DebugBgmDockPressedScale = 68f / 54f
private const val DebugBgmDockClickScale = 1.032f
private const val DebugBgmDockVelocityScaleXFactor = 0.48f
private const val DebugBgmDockVelocityScaleYFactor = 0.16f
private const val DebugBgmDockVelocityScaleClamp = 0.12f
private const val DebugBgmDockSelectedContentPressedScale = 1.12f
