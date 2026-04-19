package com.example.keios.ui.page.main.widget

import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.qmdeve.liquidglass.widget.LiquidGlassView
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Suppress("UNUSED_PARAMETER")
fun LiquidGlassBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    tabsCount: Int,
    isLiquidEffectEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val hostView = LocalView.current
    val density = LocalDensity.current
    val safeTabsCount = tabsCount.coerceAtLeast(1)
    val selectedTabIndex = selectedIndex().coerceIn(0, safeTabsCount - 1)
    val shape = RoundedCornerShape(percent = 50)
    val primary = MiuixTheme.colorScheme.primary
    val fallbackContainerColor = if (isLiquidEffectEnabled) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.34f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val indicatorColor = primary.copy(alpha = if (isLiquidEffectEnabled) 0.16f else 0.22f)
    val indicatorBorderColor = primary.copy(alpha = if (isLiquidEffectEnabled) 0.30f else 0.42f)

    BoxWithConstraints(
        modifier = modifier
            .height(AppChromeTokens.floatingBottomBarOuterHeight)
            .clip(shape)
    ) {
        val tabWidth = maxWidth / safeTabsCount
        val indicatorWidth = (tabWidth - AppChromeTokens.floatingBottomBarHorizontalPadding * 2)
            .coerceAtLeast(26.dp)
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTabIndex + AppChromeTokens.floatingBottomBarHorizontalPadding,
            animationSpec = tween(durationMillis = 220),
            label = "liquid_bottom_bar_indicator_offset"
        )

        if (isLiquidEffectEnabled) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LiquidGlassView(context).apply {
                        setDraggableEnabled(false)
                        setElasticEnabled(false)
                        setTouchEffectEnabled(false)
                    }
                },
                update = { view ->
                    val source = (hostView.rootView as? ViewGroup) ?: (hostView.parent as? ViewGroup)
                    if (source != null) {
                        view.bind(source)
                    }

                    view.setCornerRadius(with(density) { (AppChromeTokens.floatingBottomBarOuterHeight / 2).toPx() })
                    view.setRefractionHeight(with(density) { 20.dp.toPx() })
                    view.setRefractionOffset(with(density) { 70.dp.toPx() })
                    view.setDispersion(0.50f)
                    view.setBlurRadius(14f)
                    view.setTintAlpha(0.08f)
                    view.setTintColorRed(primary.red)
                    view.setTintColorGreen(primary.green)
                    view.setTintColorBlue(primary.blue)
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackContainerColor)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset)
                    .height(AppChromeTokens.floatingBottomBarInnerHeight)
                    .width(indicatorWidth)
                    .clip(shape)
                    .background(indicatorColor)
                    .border(width = 1.dp, color = indicatorBorderColor, shape = shape)
            )

            CompositionLocalProvider(LocalFloatingBottomBarTabScale provides { 1f }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppChromeTokens.floatingBottomBarOuterHeight)
                        .padding(horizontal = AppChromeTokens.floatingBottomBarHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}
