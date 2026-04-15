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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.CacheEntrySummary
import com.example.keios.core.prefs.CacheStores
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
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
    liquidActionBarLayeredStyleEnabled: Boolean,
    onLiquidActionBarLayeredStyleChanged: (Boolean) -> Unit,
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
    val settingsTitle = stringResource(R.string.settings_title)
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val enabledCardColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    val disabledCardColor = Color(0x2264748B)

    var showThemeModePopup by remember { mutableStateOf(false) }
    var themePopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var cacheReloadSignal by remember { mutableIntStateOf(0) }
    var clearingCacheId by remember { mutableStateOf<String?>(null) }
    val themeModeOptions = listOf(
        AppThemeMode.FOLLOW_SYSTEM to stringResource(R.string.settings_theme_follow_system),
        AppThemeMode.LIGHT to stringResource(R.string.settings_theme_light_mode),
        AppThemeMode.DARK to stringResource(R.string.settings_theme_dark_mode)
    )
    val currentThemeLabel =
        themeModeOptions.firstOrNull { it.first == appThemeMode }?.second
            ?: stringResource(R.string.settings_theme_follow_system)
    val themeSummary = when (appThemeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_summary_follow_system)
        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_summary_light)
        AppThemeMode.DARK -> stringResource(R.string.settings_theme_summary_dark)
    }
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
                title = settingsTitle,
                scrollBehavior = scrollBehavior,
                color = Color.Transparent,
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
            item { SmallTitle(stringResource(R.string.settings_section_ui_style)) }
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
                        header = stringResource(R.string.settings_theme_mode_header),
                        title = stringResource(R.string.settings_theme_mode_title),
                        summary = themeSummary
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
                        color = if (liquidActionBarLayeredStyleEnabled) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsSectionCard(
                        header = stringResource(R.string.settings_actionbar_style_header),
                        title = stringResource(R.string.settings_actionbar_style_title),
                        summary = if (liquidActionBarLayeredStyleEnabled) {
                            stringResource(R.string.settings_actionbar_style_summary_enabled)
                        } else {
                            stringResource(R.string.settings_actionbar_style_summary_disabled)
                        },
                        infoKey = stringResource(R.string.common_scope),
                        infoValue = stringResource(R.string.settings_actionbar_style_scope),
                        trailing = {
                            Switch(
                                checked = liquidActionBarLayeredStyleEnabled,
                                onCheckedChange = { checked -> onLiquidActionBarLayeredStyleChanged(checked) }
                            )
                        },
                        onClick = { onLiquidActionBarLayeredStyleChanged(!liquidActionBarLayeredStyleEnabled) }
                    )
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
                        header = stringResource(R.string.settings_bottom_bar_header),
                        title = stringResource(R.string.settings_bottom_bar_title),
                        summary = stringResource(R.string.settings_bottom_bar_summary),
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
                        header = stringResource(R.string.settings_card_feedback_header),
                        title = stringResource(R.string.settings_card_feedback_title),
                        summary = if (cardPressFeedbackEnabled) {
                            stringResource(R.string.settings_card_feedback_summary_enabled)
                        } else {
                            stringResource(R.string.settings_card_feedback_summary_disabled)
                        },
                        infoKey = stringResource(R.string.common_scope),
                        infoValue = stringResource(R.string.settings_card_feedback_scope),
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
                        header = stringResource(R.string.settings_home_shine_header),
                        title = stringResource(R.string.settings_home_shine_title),
                        summary = if (homeIconHdrEnabled) {
                            stringResource(R.string.settings_home_shine_summary_enabled)
                        } else {
                            stringResource(R.string.settings_home_shine_summary_disabled)
                        },
                        infoKey = stringResource(R.string.common_scope),
                        infoValue = stringResource(R.string.settings_home_shine_scope),
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
                        header = stringResource(R.string.settings_mcp_notify_header),
                        title = stringResource(R.string.settings_super_island_bypass_title),
                        summary = if (superIslandBypassRestrictionEnabled) {
                            stringResource(R.string.settings_super_island_bypass_summary_enabled)
                        } else {
                            stringResource(R.string.settings_super_island_bypass_summary_disabled)
                        },
                        infoKey = stringResource(R.string.common_note),
                        infoValue = stringResource(R.string.settings_super_island_bypass_note),
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
                        header = stringResource(R.string.settings_mcp_notify_header),
                        title = stringResource(R.string.settings_super_island_style_title),
                        summary = if (superIslandNotificationEnabled) {
                            stringResource(R.string.settings_super_island_style_summary_enabled)
                        } else {
                            stringResource(R.string.settings_super_island_style_summary_disabled)
                        },
                        infoKey = stringResource(R.string.common_scope),
                        infoValue = stringResource(R.string.settings_super_island_style_scope),
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
                        Text(text = stringResource(R.string.settings_cache_header), color = titleColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCacheDiagnosticsChanged(!cacheDiagnosticsEnabled) },
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_cache_diagnostics_title),
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
                                stringResource(R.string.settings_cache_diagnostics_summary_enabled)
                            } else {
                                stringResource(R.string.settings_cache_diagnostics_summary_disabled)
                            },
                            color = subtitleColor
                        )
                        SettingsInfoItem(
                            key = stringResource(R.string.common_scope),
                            value = if (cacheDiagnosticsEnabled) {
                                stringResource(R.string.settings_cache_scope_enabled)
                            } else {
                                stringResource(R.string.settings_cache_scope_disabled)
                            }
                        )
                        when {
                            !cacheDiagnosticsEnabled -> {
                                Text(
                                    text = stringResource(R.string.settings_cache_disabled_desc),
                                    color = subtitleColor
                                )
                            }

                            cacheEntries == null -> {
                                Text(
                                    text = stringResource(R.string.settings_cache_loading_desc),
                                    color = subtitleColor
                                )
                            }

                            cacheEntries!!.isEmpty() -> {
                                Text(
                                    text = stringResource(R.string.settings_cache_empty_desc),
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
            SettingsInfoItem(
                key = infoKey,
                value = infoValue
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    key: String,
    value: String
) {
    val titleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val valueColor = MiuixTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = key,
            color = titleColor,
            modifier = Modifier.wrapContentWidth()
        )
        Text(
            text = value.ifBlank { stringResource(R.string.common_na) },
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
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
    val resetLabel = stringResource(R.string.common_reset)
    val actionColor = if (entry.clearLabel == resetLabel) {
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
                    text = if (clearing) stringResource(R.string.common_processing) else entry.clearLabel,
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
