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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.CacheEntrySummary
import com.example.keios.core.prefs.CacheStores
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
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
    superIslandNotificationEnabled: Boolean,
    onSuperIslandNotificationChanged: (Boolean) -> Unit,
    superIslandBypassRestrictionEnabled: Boolean,
    onSuperIslandBypassRestrictionChanged: (Boolean) -> Unit,
    cacheDiagnosticsEnabled: Boolean,
    onCacheDiagnosticsChanged: (Boolean) -> Unit,
    appThemeMode: AppThemeMode,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val enabledCardColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    val disabledCardColor = Color(0x2264748B)

    var showThemeModePopup by remember { mutableStateOf(false) }
    var themePopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var cacheReloadSignal by remember { mutableIntStateOf(0) }
    var clearingCacheId by remember { mutableStateOf<String?>(null) }
    val themeModeOptions = remember {
        listOf(
            AppThemeMode.FOLLOW_SYSTEM to "跟随系统",
            AppThemeMode.LIGHT to "浅色模式",
            AppThemeMode.DARK to "深色模式"
        )
    }
    val currentThemeLabel = themeModeOptions.firstOrNull { it.first == appThemeMode }?.second ?: "跟随系统"
    val cacheEntries by produceState<List<CacheEntrySummary>?>(
        initialValue = if (cacheDiagnosticsEnabled) null else emptyList(),
        cacheDiagnosticsEnabled,
        cacheReloadSignal
    ) {
        if (!cacheDiagnosticsEnabled) {
            value = emptyList()
            return@produceState
        }
        value = null
        value = withContext(Dispatchers.IO) { CacheStores.list(context) }
    }

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
                        Box(
                            modifier = Modifier.capturePopupAnchor { themePopupAnchorBounds = it }
                        ) {
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
                                    anchorBounds = themePopupAnchorBounds,
                                    placement = SnapshotPopupPlacement.ButtonEnd,
                                    onDismissRequest = { showThemeModePopup = false },
                                    enableWindowDim = false
                                ) {
                                    LiquidDropdownColumn {
                                        themeModeOptions.forEachIndexed { index, option ->
                                            val (mode, label) = option
                                            LiquidDropdownImpl(
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

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (superIslandBypassRestrictionEnabled) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsSectionCard(
                        header = "MCP Notify",
                        title = "超级岛兼容绕过",
                        summary = if (superIslandBypassRestrictionEnabled) {
                            "已启用兼容绕过（高风险）：会临时改动系统网络规则以强触发超级岛。"
                        } else {
                            "默认关闭（推荐）：降低 HyperOS 状态栏/系统界面异常风险。"
                        },
                        infoKey = "说明",
                        infoValue = "仅超级岛样式生效；Live Update 不会使用该绕过。",
                        trailing = {
                            Switch(
                                checked = superIslandBypassRestrictionEnabled,
                                onCheckedChange = { checked -> onSuperIslandBypassRestrictionChanged(checked) }
                            )
                        },
                        onClick = { onSuperIslandBypassRestrictionChanged(!superIslandBypassRestrictionEnabled) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (superIslandNotificationEnabled) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsSectionCard(
                        header = "MCP Notify",
                        title = "超级岛通知样式",
                        summary = if (superIslandNotificationEnabled) {
                            "启用超级岛模板（Hyper Focus）。关闭后改用 AOSP Live Update 实时通知。"
                        } else {
                            "当前使用 AOSP Live Update 实时通知样式。开启后恢复超级岛模板。"
                        },
                        infoKey = "作用范围",
                        infoValue = "影响 MCP 常驻通知与 BA AP 通知的样式呈现",
                        trailing = {
                            Switch(
                                checked = superIslandNotificationEnabled,
                                onCheckedChange = { checked -> onSuperIslandNotificationChanged(checked) }
                            )
                        },
                        onClick = { onSuperIslandNotificationChanged(!superIslandNotificationEnabled) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (cacheDiagnosticsEnabled) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "Cache", color = titleColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCacheDiagnosticsChanged(!cacheDiagnosticsEnabled) },
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "缓存诊断",
                                color = titleColor,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = cacheDiagnosticsEnabled,
                                onCheckedChange = { checked -> onCacheDiagnosticsChanged(checked) }
                            )
                        }
                        Text(
                            text = if (cacheDiagnosticsEnabled) {
                                "统计各页面缓存条数、占用空间与最近活动时间。"
                            } else {
                                "关闭后不再遍历缓存目录与存储项，减少设置页额外统计开销。"
                            },
                            color = subtitleColor
                        )
                        MiuixInfoItem(
                            key = "作用范围",
                            value = if (cacheDiagnosticsEnabled) {
                                "设置页会读取 GitHub / MCP / 系统 / BA 等页面缓存摘要"
                            } else {
                                "仅关闭设置页缓存诊断统计，不影响各页面实际缓存与正常使用"
                            }
                        )
                        when {
                            !cacheDiagnosticsEnabled -> {
                                Text(
                                    text = "已关闭缓存诊断统计。设置页不会再读取缓存大小、更新时间或清理记录。",
                                    color = subtitleColor
                                )
                            }

                            cacheEntries == null -> {
                                Text(
                                    text = "正在读取各页面缓存摘要",
                                    color = subtitleColor
                                )
                            }

                            cacheEntries!!.isEmpty() -> {
                                Text(
                                    text = "暂无可管理缓存",
                                    color = subtitleColor
                                )
                            }

                            else -> {
                                cacheEntries!!.forEachIndexed { index, entry ->
                                    SettingsCacheRow(
                                        entry = entry,
                                        clearing = clearingCacheId == entry.id,
                                        onClear = {
                                            if (clearingCacheId != null) return@SettingsCacheRow
                                            scope.launch {
                                                clearingCacheId = entry.id
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        CacheStores.clear(context, entry.id)
                                                    }
                                                    cacheReloadSignal++
                                                } finally {
                                                    clearingCacheId = null
                                                }
                                            }
                                        }
                                    )
                                    if (index < cacheEntries!!.lastIndex) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
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
                horizontalArrangement = Arrangement.spacedBy(18.dp),
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

@Composable
private fun SettingsCacheRow(
    entry: CacheEntrySummary,
    clearing: Boolean,
    onClear: () -> Unit
) {
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val actionColor = if (entry.clearLabel == "重置") {
        MiuixTheme.colorScheme.error
    } else {
        MiuixTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.title,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            if (entry.clearLabel.isNotBlank()) {
                GlassTextButton(
                    backdrop = null,
                    variant = GlassVariant.Compact,
                    text = if (clearing) "处理中" else entry.clearLabel,
                    textColor = actionColor,
                    containerColor = actionColor,
                    enabled = !clearing,
                    onClick = onClear
                )
            }
        }
        Text(
            text = entry.summary,
            color = subtitleColor
        )
        Text(
            text = entry.detail,
            color = subtitleColor
        )
        Text(
            text = entry.activity,
            color = subtitleColor
        )
        Text(
            text = entry.storage,
            color = subtitleColor
        )
    }
}

private fun themeModeSummary(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.FOLLOW_SYSTEM -> "使用系统当前深浅色"
    AppThemeMode.LIGHT -> "始终使用浅色主题"
    AppThemeMode.DARK -> "始终使用深色主题"
}
