package com.example.keios.ui.page.main.host.pager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.widget.chrome.LiquidGlassBottomBar
import com.example.keios.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import com.example.keios.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import com.example.keios.ui.page.main.widget.motion.appFloatingEnter
import com.example.keios.ui.page.main.widget.motion.appFloatingExit
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun MainPagerBottomBar(
    visible: Boolean,
    navigationBarBottom: Dp,
    tabs: List<BottomPage>,
    selectedPageIndex: Int,
    selectedPageIndexProvider: () -> Int,
    backdrop: LayerBackdrop,
    reduceEffectsDuringPagerScroll: Boolean,
    liquidBottomBarEnabled: Boolean,
    onPageSelected: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = visible,
            enter = appFloatingEnter(),
            exit = appFloatingExit(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val bottomBarModifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 12.dp + navigationBarBottom
            )
            val bottomBarTabs: @Composable RowScope.() -> Unit = {
                tabs.forEachIndexed { index, page ->
                    val selected = selectedPageIndex == index
                    val tabColor = liquidGlassBottomBarItemContentColor(index)
                    LiquidGlassBottomBarItem(
                        selected = selected,
                        tabIndex = index,
                        onClick = { onPageSelected(index) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                    ) {
                        val tabIconModifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = page.iconScale
                                scaleY = page.iconScale
                            }
                        if (page.iconRes != null) {
                            Icon(
                                painter = painterResource(id = page.iconRes),
                                contentDescription = page.label,
                                tint = if (page.keepOriginalColors) Color.Unspecified else tabColor,
                                modifier = tabIconModifier
                            )
                        } else {
                            page.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = page.label,
                                    tint = tabColor,
                                    modifier = tabIconModifier
                                )
                            }
                        }
                        Text(
                            text = page.label,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = tabColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }

            LiquidGlassBottomBar(
                modifier = bottomBarModifier,
                selectedIndex = selectedPageIndex,
                onSelected = { index ->
                    if (index != selectedPageIndexProvider()) {
                        onPageSelected(index)
                    }
                },
                backdrop = backdrop,
                tabsCount = tabs.size,
                reduceEffectsDuringPagerScroll = reduceEffectsDuringPagerScroll,
                isLiquidEffectEnabled = liquidBottomBarEnabled,
                content = bottomBarTabs
            )
        }
    }
}
