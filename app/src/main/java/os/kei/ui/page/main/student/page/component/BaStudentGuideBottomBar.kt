package os.kei.ui.page.main.student.page.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBar
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import os.kei.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun BaStudentGuideBottomBar(
    visible: Boolean,
    navigationBarBottom: Dp,
    bottomTabs: List<GuideBottomTab>,
    selectedPage: Int,
    selectedPageProvider: () -> Int,
    backdrop: LayerBackdrop,
    reduceEffectsDuringPagerScroll: Boolean,
    isLiquidEffectEnabled: Boolean,
    onSelectTab: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = visible,
            enter = appFloatingEnter(),
            exit = appFloatingExit(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val bottomBarModifier = Modifier
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp + navigationBarBottom
                )
            val bottomBarTabs: @Composable RowScope.() -> Unit = {
                bottomTabs.forEachIndexed { index, tab ->
                    val selected = selectedPage == index
                    val tabColor = liquidGlassBottomBarItemContentColor(index)
                    val tabLabel = stringResource(tab.labelRes)
                    val tabContent: @Composable ColumnScope.() -> Unit = {
                        val tabIconModifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = 1f
                                scaleY = 1f
                            }
                        if (tab.localLogoRes != null) {
                            val useThemeTintForLocalLogo =
                                tab == GuideBottomTab.Skills ||
                                    tab == GuideBottomTab.Profile ||
                                    tab == GuideBottomTab.Simulate
                            Icon(
                                painter = painterResource(id = tab.localLogoRes),
                                contentDescription = tabLabel,
                                tint = if (useThemeTintForLocalLogo) tabColor else Color.Unspecified,
                                modifier = tabIconModifier
                            )
                        } else {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tabLabel,
                                tint = tabColor,
                                modifier = tabIconModifier
                            )
                        }
                        Text(
                            text = tabLabel,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = tabColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                    LiquidGlassBottomBarItem(
                        selected = selected,
                        tabIndex = index,
                        onClick = { onSelectTab(index) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                        content = tabContent
                    )
                }
            }

            LiquidGlassBottomBar(
                modifier = bottomBarModifier,
                selectedIndex = selectedPage,
                onSelected = { index ->
                    if (index != selectedPageProvider()) {
                        onSelectTab(index)
                    }
                },
                backdrop = backdrop,
                tabsCount = bottomTabs.size,
                reduceEffectsDuringPagerScroll = reduceEffectsDuringPagerScroll,
                isLiquidEffectEnabled = isLiquidEffectEnabled,
                content = bottomBarTabs
            )
        }
    }
}
