package os.kei.ui.page.main.debug

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideGridIcon
import os.kei.ui.page.main.os.appLucideHomeIcon
import os.kei.ui.page.main.os.appLucideLibraryIcon
import os.kei.ui.page.main.os.appLucideRadioIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.animation.DampedDragAnimation
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun DebugBgmDockGroupContent(
    tabs: List<DebugBgmDockTab>,
    selectedDockKey: String,
    accent: Color,
    expandedProgress: Float,
    compactProgress: Float,
    backdrop: Backdrop? = null,
    onSelectedDockKeyChange: (String) -> Unit
) {
    val expanded = expandedProgress.coerceIn(0f, 1f)
    val compact = compactProgress.coerceIn(0f, 1f)
    val expandedEnabled = expanded > 0.54f
    val compactEnabled = compact > 0.54f
    val selectedIndex = tabs.indexOfFirst { it.key == selectedDockKey }.coerceAtLeast(0)
    val compactInteractionSource = remember { MutableInteractionSource() }
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val animationScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isDark = isSystemInDarkTheme()
    var pressedTabIndex by remember { mutableIntStateOf(-1) }
    val itemPressProgress by animateFloatAsState(
        targetValue = if (pressedTabIndex >= 0) 1f else 0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "debug_bgm_dock_item_press"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val safeTabCount = tabs.size.coerceAtLeast(1)
        val contentHorizontalPadding = AppChromeTokens.floatingBottomBarHorizontalPadding
        val contentVerticalPadding = (AppChromeTokens.floatingBottomBarOuterHeight -
            AppChromeTokens.floatingBottomBarInnerHeight) / 2f
        val tabSlotWidth = ((maxWidth - contentHorizontalPadding * 2f) / safeTabCount.toFloat())
            .coerceAtLeast(42.dp)
        val selectedPillWidth = (tabSlotWidth * DebugBgmDockSelectionWidthFraction)
            .coerceAtLeast(AppChromeTokens.floatingBottomBarInnerHeight + 12.dp)
            .coerceAtMost(tabSlotWidth)
        val selectedPillInset = (tabSlotWidth - selectedPillWidth) / 2f
        val tabWidthPx = with(density) { tabSlotWidth.toPx() }.coerceAtLeast(1f)
        val selectedPillHeight = AppChromeTokens.floatingBottomBarInnerHeight.coerceAtMost(maxHeight)
        val dampedDragAnimation = remember(animationScope, safeTabCount, density, isLtr) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..(safeTabCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.16f,
                gestureKey = safeTabCount to isLtr,
                canDrag = { expandedEnabled },
                onDragStarted = {
                    pressedTabIndex = selectedIndex
                },
                onDragStopped = {
                    val targetIndex = targetValue.roundToInt().coerceIn(0, safeTabCount - 1)
                    pressedTabIndex = -1
                    tabs.getOrNull(targetIndex)?.key?.let(onSelectedDockKeyChange)
                    if (animationsEnabled) {
                        animateToValue(targetIndex.toFloat())
                    } else {
                        snapToValue(targetIndex.toFloat())
                    }
                },
                onDrag = { _, dragAmount ->
                    val dragDirection = if (isLtr) 1f else -1f
                    snapToValue(value + dragAmount.x / tabWidthPx * dragDirection)
                }
            )
        }
        LaunchedEffect(selectedIndex, animationsEnabled, dampedDragAnimation) {
            val target = selectedIndex.toFloat()
            if (abs(dampedDragAnimation.targetValue - target) > 0.001f) {
                if (animationsEnabled) {
                    dampedDragAnimation.animateToValue(target)
                } else {
                    dampedDragAnimation.snapToValue(target)
                }
            }
        }
        val selectedPillOffset = contentHorizontalPadding +
            tabSlotWidth * dampedDragAnimation.value + selectedPillInset

        DebugBgmDockSelectionPill(
            modifier = Modifier
                .offset(x = selectedPillOffset, y = contentVerticalPadding)
                .width(selectedPillWidth)
                .height(selectedPillHeight)
                .zIndex(0f)
                .graphicsLayer {
                    alpha = expanded
                    scaleX = 0.92f + 0.08f * expanded
                    scaleY = 0.92f + 0.08f * expanded
                },
            backdrop = backdrop,
            isDark = isDark,
            pressProgress = maxOf(dampedDragAnimation.pressProgress, itemPressProgress),
            dragScaleX = dampedDragAnimation.scaleX,
            dragScaleY = dampedDragAnimation.scaleY,
            velocity = dampedDragAnimation.velocity
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (expanded >= compact) 1f else 0f)
                .graphicsLayer {
                    alpha = expanded
                    scaleX = 0.96f + 0.04f * expanded
                    scaleY = 0.96f + 0.04f * expanded
                }
                .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val tabIndex = tabs.indexOf(tab).coerceAtLeast(0)
                val selectionProgress = (1f - abs(dampedDragAnimation.value - tabIndex))
                    .coerceIn(0f, 1f)
                DebugBgmExpandedDockTab(
                    tab = tab,
                    selected = selectionProgress > 0.52f,
                    selectionProgress = selectionProgress,
                    accent = accent,
                    onClick = {
                        if (animationsEnabled) {
                            dampedDragAnimation.animateToValue(tabIndex.toFloat())
                        } else {
                            dampedDragAnimation.snapToValue(tabIndex.toFloat())
                        }
                        onSelectedDockKeyChange(tab.key)
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

        Box(
            modifier = Modifier
                .offset(
                    x = contentHorizontalPadding + tabSlotWidth * dampedDragAnimation.value,
                    y = contentVerticalPadding
                )
                .width(tabSlotWidth)
                .height(selectedPillHeight)
                .zIndex(if (expanded >= compact) 2f else 0f)
                .then(
                    if (expandedEnabled) {
                        dampedDragAnimation.modifier
                    } else {
                        Modifier
                    }
                )
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
                            interactionSource = compactInteractionSource,
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
    dragScaleX: Float,
    dragScaleY: Float,
    velocity: Float,
    modifier: Modifier = Modifier
) {
    val clampedPress = pressProgress.coerceIn(0f, 1f)
    val clickScale = lerpFloat(1f, 1.045f, clampedPress)
    val velocityScale = velocity / 10f
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
            .graphicsLayer {
                if (backdrop == null) {
                    scaleX = dragScaleX * clickScale / (1f - (velocityScale * 0.75f).coerceIn(-0.2f, 0.2f))
                    scaleY = dragScaleY * clickScale * (1f - (velocityScale * 0.25f).coerceIn(-0.2f, 0.2f))
                }
            }
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur((UiPerformanceBudget.backdropBlur * 0.52f).toPx())
                            lens(
                                (8.dp + 4.dp * clampedPress).toPx(),
                                (12.dp + 4.dp * clampedPress).toPx(),
                                true
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isDark) 0.44f + clampedPress * 0.16f else 0.76f)
                        },
                        shadow = {
                            Shadow.Default.copy(color = Color.Black.copy(alpha = if (isDark) 0.14f else 0.08f))
                        },
                        innerShadow = {
                            InnerShadow(radius = 7.dp + 2.dp * clampedPress, alpha = if (isDark) 0.18f else 0.10f)
                        },
                        layerBlock = {
                            scaleX = dragScaleX * clickScale / (1f - (velocityScale * 0.75f).coerceIn(-0.2f, 0.2f))
                            scaleY = dragScaleY * clickScale * (1f - (velocityScale * 0.25f).coerceIn(-0.2f, 0.2f))
                        },
                        onDrawSurface = {
                            drawRect(neutralFill)
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
    accent: Color,
    onClick: () -> Unit,
    onPressedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.94f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "debug_bgm_dock_tab_press_scale"
    )
    val contentScale = itemScale * lerpFloat(1f, 1.035f, selectionProgress.coerceIn(0f, 1f))
    LaunchedEffect(pressed, enabled) {
        if (enabled) onPressedChange(pressed)
    }
    Column(
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
            )
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DebugBgmDockTabIcon(
            icon = tab.icon,
            label = tab.label,
            selected = selected,
            accent = accent,
            selectionProgress = selectionProgress
        )
        Text(
            text = tab.label,
            color = DebugBgmDockTint(
                selected = selected,
                accent = accent,
                selectionProgress = selectionProgress
            ),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

private const val DebugBgmDockSelectionWidthFraction = 0.84f
