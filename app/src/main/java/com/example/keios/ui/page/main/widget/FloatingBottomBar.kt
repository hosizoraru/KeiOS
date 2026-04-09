package com.example.keios.ui.page.main.widget

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.example.keios.ui.animation.DampedDragAnimation
import com.example.keios.ui.animation.InteractiveHighlight
import com.example.keios.ui.page.main.model.BottomPage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.sign

private val LocalFloatingBottomBarTabScale = staticCompositionLocalOf { { 1f } }

@Composable
private fun RowScope.FloatingBottomBarItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalFloatingBottomBarTabScale.current
    Column(
        modifier
            .clip(ContinuousCapsule)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val value = scale()
                scaleX = value
                scaleY = value
            }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun FloatingBottomBar(
    backdrop: Backdrop?,
    currentPage: BottomPage,
    liquidGlassEnabled: Boolean,
    onPageSelected: (BottomPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = BottomPage.entries
    val selectedIndex = tabs.indexOf(currentPage).coerceAtLeast(0)

    // Normal Style Fallback
    if (!liquidGlassEnabled) {
        val normalSelectedTint = MiuixTheme.colorScheme.primary
        val normalUnselectedTint = MiuixTheme.colorScheme.onBackgroundVariant
        val normalSurface = MiuixTheme.colorScheme.surfaceContainer
        Row(
            modifier = modifier
                .height(76.dp)
                .clip(ContinuousCapsule)
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {},
                            highlight = { Highlight.Default.copy(alpha = 0.2f) },
                            shadow = { Shadow.Default.copy(color = Color.Black.copy(alpha = 0.12f)) },
                            onDrawSurface = { drawRect(normalSurface.copy(alpha = 0.92f)) }
                        )
                    } else {
                        Modifier.background(normalSurface.copy(alpha = 0.92f))
                    }
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { page ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ContinuousCapsule)
                        .clickable { onPageSelected(page) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = page.label,
                        modifier = Modifier.size(20.dp),
                        tint = if (currentPage == page) normalSelectedTint else normalUnselectedTint
                    )
                    Text(
                        text = page.label,
                        color = if (currentPage == page) normalSelectedTint else normalUnselectedTint,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        return
    }

    if (backdrop == null) return

    // Liquid Glass Style
    val usedBackdrop = backdrop
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val isInLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
    val accentColor = MiuixTheme.colorScheme.primary
    val unselectedTint = MiuixTheme.colorScheme.onBackgroundVariant

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }
    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }
    val holder = remember { DampedDragAnimationHolder() }
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val dampedDragAnimation = remember(animationScope, tabs.size, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabs.size - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset ->
                val anim = holder.instance ?: return@DampedDragAnimation true
                if (tabWidthPx == 0f) return@DampedDragAnimation false

                // 【修正点】使用 anim.value 而不是 value
                val currentValue = anim.value
                val indicatorX = currentValue * tabWidthPx

                val padding = with(density) { 4.dp.toPx() }
                val globalTouchX = if (isLtr) {
                    padding + indicatorX + offset.x
                } else {
                    totalWidthPx - padding - tabWidthPx - indicatorX + offset.x
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabs.size - 1)
                currentIndex = targetIndex
                animateToValue(targetIndex.toFloat())
                animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0) {
                    updateValue((targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f).fastCoerceIn(0f, (tabs.size - 1).toFloat()))
                    animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x) }
                }
            }
        )
    }

    LaunchedEffect(selectedIndex) { currentIndex = selectedIndex }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }.drop(1).collectLatest { index ->
            dampedDragAnimation.animateToValue(index.toFloat())
            onPageSelected(tabs[index])
        }
    }

    val interactiveHighlight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember(animationScope, tabWidthPx, tabs.size, isLtr) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }
    } else null

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        // 1. Bottom Row: Static Background and Unselected Icons
        Row(
            Modifier
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = contentWidthPx / tabs.size
                }
                .graphicsLayer { translationX = panelOffset }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .drawBackdrop(
                    backdrop = usedBackdrop,
                    shape = { ContinuousCapsule },
                    effects = { vibrancy(); blur(8f.dp.toPx()); lens(24f.dp.toPx(), 24f.dp.toPx()) },
                    highlight = { Highlight.Default.copy(alpha = 1f) },
                    shadow = { Shadow.Default.copy(color = Color.Black.copy(if (isInLightTheme) 0.1f else 0.2f)) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = if (isInLightTheme) 0.34f else 0.18f)) }
                )
                .then(interactiveHighlight?.modifier ?: Modifier)
                .height(76.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, page ->
                FloatingBottomBarItem(onClick = { currentIndex = index }) {
                    Icon(imageVector = page.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = unselectedTint)
                    Text(text = page.label, color = unselectedTint, fontSize = 11.sp)
                }
            }
        }

        // 2. Middle Row: The "Source" for the colored highlight (Captured by tabsBackdrop)
        CompositionLocalProvider(LocalFloatingBottomBarTabScale provides { lerp(1f, 1.2f, dampedDragAnimation.pressProgress) }) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f) // Keep it invisible but available for backdrop capture
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                        // FIX: Tint the entire layer to the accent color.
                        // When the indicator lens reads this, it sees colored icons.
                        colorFilter = ColorFilter.tint(accentColor)
                    }
                    .height(68.dp) // Align height with the indicator Box
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, page ->
                    FloatingBottomBarItem(onClick = { currentIndex = index }) {
                        Icon(imageVector = page.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(text = page.label, fontSize = 11.sp)
                    }
                }
            }
        }

        // 3. Top Layer: The Moving Indicator (Lens)
        if (tabWidthPx > 0f) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val contentWidth = totalWidthPx - with(density) { 8.dp.toPx() }
                        val singleTabWidth = contentWidth / tabs.size
                        val progressOffset = dampedDragAnimation.value * singleTabWidth
                        translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                    }
                    .then(interactiveHighlight?.gestureModifier ?: Modifier)
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        // Combined backdrop allows reading both the wallpaper and the colored icons
                        backdrop = rememberCombinedBackdrop(usedBackdrop, tabsBackdrop),
                        shape = { ContinuousCapsule },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            // isLens = true is critical for the "colored glass" effect
                            lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, true)
                        },
                        highlight = { Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress) },
                        shadow = { Shadow(alpha = dampedDragAnimation.pressProgress) },
                        innerShadow = { InnerShadow(radius = 8f.dp * dampedDragAnimation.pressProgress, alpha = dampedDragAnimation.pressProgress) },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            // Draw a subtle surface color for the indicator
                            drawRect(
                                color = if (isInLightTheme) Color.Black.copy(0.12f) else Color.White.copy(0.12f),
                                alpha = 1f - progress
                            )
                        }
                    )
                    .height(68.dp)
                    .width(with(density) { ((totalWidthPx - 8.dp.toPx()) / tabs.size).toDp() })
            )
        }
    }
}