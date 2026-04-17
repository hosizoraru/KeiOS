package com.example.keios.ui.page.main

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.CacheEntrySummary
import com.example.keios.core.prefs.CacheStores
import com.example.keios.core.log.AppLogStore
import com.example.keios.ui.page.main.widget.AppInfoRow
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    logDebugEnabled: Boolean,
    onLogDebugChanged: (Boolean) -> Unit,
    textCopyCapabilityExpanded: Boolean,
    onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit,
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
    var logReloadSignal by remember { mutableIntStateOf(0) }
    var exportingLogZip by remember { mutableStateOf(false) }
    var clearingLogs by remember { mutableStateOf(false) }
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
    val logStats by produceState(
        initialValue = AppLogStore.Stats.Empty,
        logReloadSignal,
        logDebugEnabled
    ) {
        do {
            value = withContext(Dispatchers.IO) {
                AppLogStore.stats(context)
            }
            if (!logDebugEnabled) break
            delay(1200)
        } while (true)
    }
    val uiGroupActive = liquidActionBarLayeredStyleEnabled ||
        liquidBottomBarEnabled ||
        cardPressFeedbackEnabled ||
        homeIconHdrEnabled
    val notifyGroupActive = superIslandNotificationEnabled || superIslandBypassRestrictionEnabled
    val logGroupActive = logDebugEnabled || logStats.fileCount > 0
    val logLatestText = if (logStats.latestModifiedAtMs <= 0L) {
        stringResource(R.string.settings_log_stat_latest_empty)
    } else {
        formatLogTime(logStats.latestModifiedAtMs)
    }
    val logExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            exportingLogZip = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) { AppLogStore.exportZipToUri(context, uri) }
            exportingLogZip = false
            if (result.isSuccess) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_log_toast_exported),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val reason = result.exceptionOrNull()?.javaClass?.simpleName ?: context.getString(R.string.common_unknown)
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_log_toast_export_failed, reason),
                    Toast.LENGTH_SHORT
                ).show()
            }
            logReloadSignal++
        }
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
                        color = if (uiGroupActive) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsGroupCard(
                        header = stringResource(R.string.settings_group_visual_header),
                        title = stringResource(R.string.settings_group_visual_title),
                        summary = stringResource(R.string.settings_group_visual_summary)
                    ) {
                        SettingsActionItem(
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

                        SettingsToggleItem(
                            title = stringResource(R.string.settings_actionbar_style_title),
                            summary = if (liquidActionBarLayeredStyleEnabled) {
                                stringResource(R.string.settings_actionbar_style_summary_enabled)
                            } else {
                                stringResource(R.string.settings_actionbar_style_summary_disabled)
                            },
                            checked = liquidActionBarLayeredStyleEnabled,
                            onCheckedChange = onLiquidActionBarLayeredStyleChanged,
                            infoKey = stringResource(R.string.common_scope),
                            infoValue = stringResource(R.string.settings_actionbar_style_scope)
                        )

                        SettingsToggleItem(
                            title = stringResource(R.string.settings_bottom_bar_title),
                            summary = stringResource(R.string.settings_bottom_bar_summary),
                            checked = liquidBottomBarEnabled,
                            onCheckedChange = onLiquidBottomBarChanged
                        )

                        SettingsToggleItem(
                            title = stringResource(R.string.settings_card_feedback_title),
                            summary = if (cardPressFeedbackEnabled) {
                                stringResource(R.string.settings_card_feedback_summary_enabled)
                            } else {
                                stringResource(R.string.settings_card_feedback_summary_disabled)
                            },
                            checked = cardPressFeedbackEnabled,
                            onCheckedChange = onCardPressFeedbackChanged,
                            infoKey = stringResource(R.string.common_scope),
                            infoValue = stringResource(R.string.settings_card_feedback_scope)
                        )

                        SettingsToggleItem(
                            title = stringResource(R.string.settings_home_shine_title),
                            summary = if (homeIconHdrEnabled) {
                                stringResource(R.string.settings_home_shine_summary_enabled)
                            } else {
                                stringResource(R.string.settings_home_shine_summary_disabled)
                            },
                            checked = homeIconHdrEnabled,
                            onCheckedChange = onHomeIconHdrChanged,
                            infoKey = stringResource(R.string.common_scope),
                            infoValue = stringResource(R.string.settings_home_shine_scope)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (logGroupActive) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsGroupCard(
                        header = stringResource(R.string.settings_group_log_header),
                        title = stringResource(R.string.settings_group_log_title),
                        summary = stringResource(R.string.settings_group_log_summary)
                    ) {
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_log_debug_title),
                            summary = if (logDebugEnabled) {
                                stringResource(R.string.settings_log_debug_summary_enabled)
                            } else {
                                stringResource(R.string.settings_log_debug_summary_disabled)
                            },
                            checked = logDebugEnabled,
                            onCheckedChange = onLogDebugChanged,
                            infoKey = stringResource(R.string.common_scope),
                            infoValue = stringResource(R.string.settings_log_scope)
                        )
                        SettingsInfoItem(
                            key = stringResource(R.string.common_note),
                            value = if (logDebugEnabled) {
                                stringResource(R.string.settings_log_note_enabled)
                            } else {
                                stringResource(R.string.settings_log_note_disabled)
                            }
                        )
                        SettingsInfoItem(
                            key = stringResource(R.string.settings_log_stat_size),
                            value = formatBytes(logStats.totalBytes)
                        )
                        SettingsInfoItem(
                            key = stringResource(R.string.settings_log_stat_files),
                            value = stringResource(R.string.settings_log_stat_files_count, logStats.fileCount)
                        )
                        SettingsInfoItem(
                            key = stringResource(R.string.settings_log_stat_latest),
                            value = logLatestText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextButton(
                                backdrop = null,
                                variant = GlassVariant.Compact,
                                text = if (exportingLogZip) {
                                    stringResource(R.string.common_processing)
                                } else {
                                    stringResource(R.string.settings_log_action_export_zip)
                                },
                                enabled = !exportingLogZip && !clearingLogs,
                                onClick = {
                                    exportingLogZip = true
                                    val stamp = SimpleDateFormat(
                                        "yyyyMMdd-HHmmss",
                                        Locale.getDefault()
                                    ).format(Date())
                                    logExportLauncher.launch("keios-logs-$stamp.zip")
                                }
                            )
                            GlassTextButton(
                                backdrop = null,
                                variant = GlassVariant.Compact,
                                text = if (clearingLogs) {
                                    stringResource(R.string.common_processing)
                                } else {
                                    stringResource(R.string.settings_log_action_clear)
                                },
                                textColor = MiuixTheme.colorScheme.error,
                                containerColor = MiuixTheme.colorScheme.error,
                                enabled = !exportingLogZip && !clearingLogs,
                                onClick = {
                                    scope.launch {
                                        clearingLogs = true
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching { AppLogStore.clear(context) }
                                        }
                                        clearingLogs = false
                                        if (result.isSuccess) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_log_toast_cleared),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val reason = result.exceptionOrNull()?.javaClass?.simpleName
                                                ?: context.getString(R.string.common_unknown)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_log_toast_clear_failed, reason),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        logReloadSignal++
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (notifyGroupActive) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsGroupCard(
                        header = stringResource(R.string.settings_group_notify_header),
                        title = stringResource(R.string.settings_group_notify_title),
                        summary = stringResource(R.string.settings_group_notify_summary)
                    ) {
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_super_island_style_title),
                            summary = if (superIslandNotificationEnabled) {
                                stringResource(R.string.settings_super_island_style_summary_enabled)
                            } else {
                                stringResource(R.string.settings_super_island_style_summary_disabled)
                            },
                            checked = superIslandNotificationEnabled,
                            onCheckedChange = onSuperIslandNotificationChanged,
                            infoKey = stringResource(R.string.common_scope),
                            infoValue = stringResource(R.string.settings_super_island_style_scope)
                        )

                        SettingsToggleItem(
                            title = stringResource(R.string.settings_super_island_bypass_title),
                            summary = if (superIslandBypassRestrictionEnabled) {
                                stringResource(R.string.settings_super_island_bypass_summary_enabled)
                            } else {
                                stringResource(R.string.settings_super_island_bypass_summary_disabled)
                            },
                            checked = superIslandBypassRestrictionEnabled,
                            onCheckedChange = onSuperIslandBypassRestrictionChanged,
                            infoKey = stringResource(R.string.common_note),
                            infoValue = stringResource(R.string.settings_super_island_bypass_note)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (textCopyCapabilityExpanded) enabledCardColor else disabledCardColor,
                        contentColor = titleColor
                    ),
                    onClick = {}
                ) {
                    SettingsGroupCard(
                        header = stringResource(R.string.settings_group_copy_header),
                        title = stringResource(R.string.settings_group_copy_title),
                        summary = stringResource(R.string.settings_group_copy_summary)
                    ) {
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_copy_capability_title),
                            summary = if (textCopyCapabilityExpanded) {
                                stringResource(R.string.settings_copy_capability_summary_enabled)
                            } else {
                                stringResource(R.string.settings_copy_capability_summary_disabled)
                            },
                            checked = textCopyCapabilityExpanded,
                            onCheckedChange = onTextCopyCapabilityExpandedChanged,
                            infoKey = stringResource(R.string.common_note),
                            infoValue = if (textCopyCapabilityExpanded) {
                                stringResource(R.string.settings_copy_capability_note_enabled)
                            } else {
                                stringResource(R.string.settings_copy_capability_note_disabled)
                            }
                        )
                    }
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
                            .padding(CardLayoutRhythm.cardContentPadding),
                        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_cache_header),
                            color = titleColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 19.sp
                        )
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 21.sp,
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
                            color = subtitleColor,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
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
                                    color = subtitleColor,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            cacheEntries == null -> {
                                Text(
                                    text = stringResource(R.string.settings_cache_loading_desc),
                                    color = subtitleColor,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            cacheEntries!!.isEmpty() -> {
                                Text(
                                    text = stringResource(R.string.settings_cache_empty_desc),
                                    color = subtitleColor,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
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
private fun SettingsGroupCard(
    header: String,
    title: String,
    summary: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardLayoutRhythm.cardContentPadding),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)
    ) {
        Text(
            text = header,
            color = titleColor,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            text = title,
            color = titleColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp
        )
        Text(
            text = summary,
            color = subtitleColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap),
            content = content
        )
    }
}

@Composable
private fun SettingsActionItem(
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
            .let { base -> if (onClick != null) base.clickable { onClick() } else base },
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
        Text(
            text = summary,
            color = subtitleColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        if (!infoKey.isNullOrBlank() && !infoValue.isNullOrBlank()) {
            SettingsInfoItem(
                key = infoKey,
                value = infoValue
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    infoKey: String? = null,
    infoValue: String? = null
) {
    SettingsActionItem(
        title = title,
        summary = summary,
        infoKey = infoKey,
        infoValue = infoValue,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun SettingsInfoItem(
    key: String,
    value: String
) {
    AppInfoRow(
        label = key,
        value = value.ifBlank { stringResource(R.string.common_na) },
        labelColor = MiuixTheme.colorScheme.onBackgroundVariant,
        valueColor = MiuixTheme.colorScheme.onBackground,
        labelMinWidth = 64.dp,
        labelMaxWidth = 112.dp,
        horizontalSpacing = CardLayoutRhythm.infoRowGap,
        rowVerticalPadding = CardLayoutRhythm.infoRowVerticalPadding,
        labelMaxLines = 2,
        valueMaxLines = 6,
        valueOverflow = TextOverflow.Ellipsis,
        labelFontSize = 13.sp,
        labelLineHeight = 18.sp,
        valueFontSize = 13.sp,
        valueLineHeight = 18.sp,
        emphasizedValue = false
    )
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
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.title,
                color = titleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
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
            color = subtitleColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Text(
            text = entry.detail,
            color = subtitleColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Text(
            text = entry.activity,
            color = subtitleColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Text(
            text = entry.storage,
            color = subtitleColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        safe >= gb -> String.format(Locale.US, "%.2f GB", safe / gb)
        safe >= mb -> String.format(Locale.US, "%.2f MB", safe / mb)
        safe >= kb -> String.format(Locale.US, "%.2f KB", safe / kb)
        else -> "${safe.toLong()} B"
    }
}

private fun formatLogTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return ""
    return runCatching {
        SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
    }.getOrElse { "" }
}
