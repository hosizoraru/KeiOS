package os.kei.ui.page.main.settings.page

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBar
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import os.kei.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text

internal enum class SettingsCategory {
    Access,
    Appearance,
    Notify,
    Data
}

@Composable
internal fun SettingsCategoryBottomBar(
    visible: Boolean,
    navigationBarBottom: Dp,
    selectedCategory: SettingsCategory,
    selectedCategoryProvider: () -> SettingsCategory,
    backdrop: Backdrop,
    isLiquidEffectEnabled: Boolean,
    onSelectCategory: (SettingsCategory) -> Unit
) {
    val categories = SettingsCategory.entries
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = visible,
            enter = appFloatingEnter(),
            exit = appFloatingExit(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
            val bottomBarModifier = Modifier.padding(
                horizontal = AppChromeTokens.pageHorizontalPadding,
                vertical = AppChromeTokens.pageSectionGap + navigationBarBottom
            )
            val bottomBarTabs: @Composable RowScope.() -> Unit = {
                categories.forEachIndexed { index, category ->
                    val selected = selectedCategory == category
                    val tabColor = liquidGlassBottomBarItemContentColor(index)
                    val tabContent: @Composable ColumnScope.() -> Unit = {
                        Icon(
                            imageVector = category.icon(),
                            contentDescription = category.label(),
                            tint = tabColor,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = 1f
                                    scaleY = 1f
                                }
                        )
                        Text(
                            text = category.label(),
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
                        onClick = { onSelectCategory(category) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                        content = tabContent
                    )
                }
            }

            LiquidGlassBottomBar(
                modifier = bottomBarModifier,
                selectedIndex = selectedIndex,
                onSelected = { index ->
                    val nextCategory = categories.getOrNull(index) ?: return@LiquidGlassBottomBar
                    if (nextCategory != selectedCategoryProvider()) {
                        onSelectCategory(nextCategory)
                    }
                },
                backdrop = backdrop,
                tabsCount = categories.size,
                isLiquidEffectEnabled = isLiquidEffectEnabled,
                content = bottomBarTabs
            )
        }
    }
}

@Composable
private fun SettingsCategory.label(): String {
    return stringResource(
        when (this) {
            SettingsCategory.Access -> R.string.settings_category_access
            SettingsCategory.Appearance -> R.string.settings_category_appearance
            SettingsCategory.Notify -> R.string.settings_category_notify
            SettingsCategory.Data -> R.string.settings_category_data
        }
    )
}

@Composable
private fun SettingsCategory.icon(): ImageVector {
    val drawableRes = when (this) {
        SettingsCategory.Access -> LucideR.drawable.lucide_ic_shield_check
        SettingsCategory.Appearance -> LucideR.drawable.lucide_ic_palette
        SettingsCategory.Notify -> LucideR.drawable.lucide_ic_bell
        SettingsCategory.Data -> LucideR.drawable.lucide_ic_database
    }
    return ImageVector.vectorResource(drawableRes)
}
