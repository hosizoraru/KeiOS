package com.example.keios.ui.page.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    liquidBottomBarEnabled: Boolean,
    onLiquidBottomBarChanged: (Boolean) -> Unit,
    cardPressFeedbackEnabled: Boolean,
    onCardPressFeedbackChanged: (Boolean) -> Unit,
    homeIconHdrEnabled: Boolean,
    onHomeIconHdrChanged: (Boolean) -> Unit,
    appThemeMode: AppThemeMode,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    onBack: () -> Unit
) {
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val enabledCardColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    val disabledCardColor = Color(0x2264748B)

    var showThemeModePopup by remember { mutableStateOf(false) }
    val themeModeOptions = remember {
        listOf(
            AppThemeMode.FOLLOW_SYSTEM to "跟随系统",
            AppThemeMode.LIGHT to "浅色模式",
            AppThemeMode.DARK to "深色模式"
        )
    }
    val currentThemeLabel = themeModeOptions.firstOrNull { it.first == appThemeMode }?.second ?: "跟随系统"

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "Settings",
                scrollBehavior = scrollBehavior,
                color = MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item { SmallTitle("界面与样式") }
            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = enabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsSectionCard(
                        header = "Theme Mode",
                        title = "应用主题",
                        summary = themeModeSummary(appThemeMode)
                    ) {
                        Box {
                            GlassTextButton(
                                backdrop = null,
                                variant = GlassVariant.SheetAction,
                                text = currentThemeLabel,
                                onClick = { showThemeModePopup = !showThemeModePopup }
                            )
                            if (showThemeModePopup) {
                                SnapshotWindowListPopup(
                                    show = showThemeModePopup,
                                    alignment = PopupPositionProvider.Align.BottomEnd,
                                    onDismissRequest = { showThemeModePopup = false },
                                    enableWindowDim = false
                                ) {
                                    ListPopupColumn {
                                        themeModeOptions.forEachIndexed { index, option ->
                                            val (mode, label) = option
                                            DropdownImpl(
                                                text = label,
                                                optionSize = themeModeOptions.size,
                                                isSelected = appThemeMode == mode,
                                                index = index,
                                                onSelectedIndexChange = { selectedIndex ->
                                                    onAppThemeModeChanged(themeModeOptions[selectedIndex].first)
                                                    showThemeModePopup = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (liquidBottomBarEnabled) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsSectionCard(
                        header = "Bottom Bar",
                        title = "液态玻璃底栏",
                        summary = "胶囊底栏 + 选中态高亮 + 轻量玻璃质感",
                        trailing = {
                            Switch(
                                checked = liquidBottomBarEnabled,
                                onCheckedChange = { checked -> onLiquidBottomBarChanged(checked) }
                            )
                        },
                        onClick = { onLiquidBottomBarChanged(!liquidBottomBarEnabled) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (cardPressFeedbackEnabled) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsSectionCard(
                        header = "Card Feedback",
                        title = "卡片按压反馈",
                        summary = if (cardPressFeedbackEnabled) {
                            "启用所有支持 PressFeedback 的卡片反馈"
                        } else {
                            "全局禁用 Sink / Tilt 等卡片按压反馈"
                        },
                        infoKey = "作用范围",
                        infoValue = "System / MCP / GitHub / BA 等支持按压反馈的卡片",
                        trailing = {
                            Switch(
                                checked = cardPressFeedbackEnabled,
                                onCheckedChange = { checked -> onCardPressFeedbackChanged(checked) }
                            )
                        },
                        onClick = { onCardPressFeedbackChanged(!cardPressFeedbackEnabled) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (homeIconHdrEnabled) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsSectionCard(
                        header = "Home Shine",
                        title = "主页图标与标题 HDR 高光",
                        summary = if (homeIconHdrEnabled) {
                            "启用主页 Kei 图标与 KeiOS 标题联动高光（亮屏时更明显）"
                        } else {
                            "关闭主页图标与标题高光，减少夜间眩光感"
                        },
                        infoKey = "作用范围",
                        infoValue = "影响主页 Kei 图标与 KeiOS 标题的联动高光效果",
                        trailing = {
                            Switch(
                                checked = homeIconHdrEnabled,
                                onCheckedChange = { checked -> onHomeIconHdrChanged(checked) }
                            )
                        },
                        onClick = { onHomeIconHdrChanged(!homeIconHdrEnabled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    header: String,
    title: String,
    summary: String,
    infoKey: String? = null,
    infoValue: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(header, color = titleColor)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .let { base -> if (onClick != null) base.clickable { onClick() } else base },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    modifier = Modifier.weight(1f)
                )
                trailing()
            }
            Text(
                text = summary,
                color = subtitleColor
            )
        }
        if (!infoKey.isNullOrBlank() && !infoValue.isNullOrBlank()) {
            MiuixInfoItem(
                key = infoKey,
                value = infoValue
            )
        }
    }
}

private fun themeModeSummary(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.FOLLOW_SYSTEM -> "使用系统当前深浅色"
    AppThemeMode.LIGHT -> "始终使用浅色主题"
    AppThemeMode.DARK -> "始终使用深色主题"
}
