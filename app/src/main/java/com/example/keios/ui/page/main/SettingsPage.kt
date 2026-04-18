package com.example.keios.ui.page.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.CacheEntrySummary
import com.example.keios.core.prefs.CacheStores
import com.example.keios.core.log.AppLogStore
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.keios.ui.page.main.widget.AppControlRow
import com.example.keios.ui.page.main.widget.AppDualActionRow
import com.example.keios.ui.page.main.widget.AppDropdownSelector
import com.example.keios.ui.page.main.widget.AppFeatureCard
import com.example.keios.ui.page.main.widget.AppInfoRow
import com.example.keios.ui.page.main.widget.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.AppPageScaffold
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import com.yalantis.ucrop.UCrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    liquidBottomBarEnabled: Boolean,
    onLiquidBottomBarChanged: (Boolean) -> Unit,
    liquidActionBarLayeredStyleEnabled: Boolean,
    onLiquidActionBarLayeredStyleChanged: (Boolean) -> Unit,
    transitionAnimationsEnabled: Boolean,
    onTransitionAnimationsChanged: (Boolean) -> Unit,
    cardPressFeedbackEnabled: Boolean,
    onCardPressFeedbackChanged: (Boolean) -> Unit,
    homeIconHdrEnabled: Boolean,
    onHomeIconHdrChanged: (Boolean) -> Unit,
    preloadingEnabled: Boolean,
    onPreloadingEnabledChanged: (Boolean) -> Unit,
    nonHomeBackgroundEnabled: Boolean,
    onNonHomeBackgroundEnabledChanged: (Boolean) -> Unit,
    nonHomeBackgroundUri: String,
    onNonHomeBackgroundUriChanged: (String) -> Unit,
    nonHomeBackgroundOpacity: Float,
    onNonHomeBackgroundOpacityChanged: (Float) -> Unit,
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
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f)
    val enabledCardColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    val disabledCardColor = Color(0x2264748B)
    val surfaceColor = MiuixTheme.colorScheme.surface

    var showThemeModePopup by remember { mutableStateOf(false) }
    var themePopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var cacheReloadSignal by remember { mutableIntStateOf(0) }
    var clearingCacheId by remember { mutableStateOf<String?>(null) }
    var logReloadSignal by remember { mutableIntStateOf(0) }
    var exportingLogZip by remember { mutableStateOf(false) }
    var clearingLogs by remember { mutableStateOf(false) }
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    val contentBackdrop: LayerBackdrop = key("settings-content-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
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
        transitionAnimationsEnabled ||
        cardPressFeedbackEnabled ||
        preloadingEnabled ||
        homeIconHdrEnabled
    val backgroundGroupActive = nonHomeBackgroundEnabled || nonHomeBackgroundUri.isNotBlank()
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
    val nonHomeBackgroundCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val cropError = data?.let { UCrop.getError(it) }
        if (result.resultCode != Activity.RESULT_OK) {
            if (cropError != null) {
                val reason = cropError.javaClass.simpleName.ifBlank {
                    context.getString(R.string.common_unknown)
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_non_home_background_toast_crop_failed, reason),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return@rememberLauncherForActivityResult
        }
        val outputUri = data?.let { UCrop.getOutput(it) } ?: run {
            Toast.makeText(
                context,
                context.getString(
                    R.string.settings_non_home_background_toast_crop_failed,
                    context.getString(R.string.common_unknown)
                ),
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }
        deleteManagedNonHomeBackgroundFile(context, nonHomeBackgroundUri)
        onNonHomeBackgroundUriChanged(outputUri.toString())
        if (!nonHomeBackgroundEnabled) onNonHomeBackgroundEnabledChanged(true)
        Toast.makeText(
            context,
            context.getString(R.string.settings_non_home_background_toast_selected),
            Toast.LENGTH_SHORT
        ).show()
    }
    val nonHomeBackgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val outputUri = createNonHomeBackgroundCropOutputUri(context)
        val (aspectRatioX, aspectRatioY) = resolveNonHomeBackgroundAspectRatio(context)
        val (maxResultWidth, maxResultHeight) = resolveNonHomeBackgroundCropSize(context)
        val cropOptions = UCrop.Options().apply {
            setToolbarTitle(context.getString(R.string.settings_non_home_background_crop_title))
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(92)
            setFreeStyleCropEnabled(false)
            setHideBottomControls(false)
            setShowCropFrame(true)
            setShowCropGrid(true)
        }
        val cropIntent = runCatching {
            UCrop.of(uri, outputUri)
                .withAspectRatio(aspectRatioX, aspectRatioY)
                .withMaxResultSize(maxResultWidth, maxResultHeight)
                .withOptions(cropOptions)
                .getIntent(context)
        }.getOrElse { error ->
            val reason = error.javaClass.simpleName.ifBlank {
                context.getString(R.string.common_unknown)
            }
            Toast.makeText(
                context,
                context.getString(R.string.settings_non_home_background_toast_crop_failed, reason),
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }
        cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        nonHomeBackgroundCropLauncher.launch(cropIntent)
    }

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()

    AppPageScaffold(
        title = settingsTitle,
        modifier = Modifier.fillMaxSize(),
        scrollBehavior = scrollBehavior,
        topBarColor = Color.Transparent,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = MiuixIcons.Regular.Back,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        AppPageLazyColumn(
            innerPadding = innerPadding,
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            sectionSpacing = 12.dp
        ) {
            item {
                SettingsGroupCard(
                    header = stringResource(R.string.settings_group_visual_header),
                    title = stringResource(R.string.settings_group_visual_title),
                    summary = stringResource(R.string.settings_group_visual_summary),
                    containerColor = if (uiGroupActive) enabledCardColor else disabledCardColor
                ) {
                    SettingsActionItem(
                        title = stringResource(R.string.settings_theme_mode_title),
                        summary = themeSummary
                    ) {
                        AppDropdownSelector(
                            selectedText = currentThemeLabel,
                            options = themeModeOptions.map { it.second },
                            selectedIndex = themeModeOptions.indexOfFirst { it.first == appThemeMode }
                                .coerceAtLeast(0),
                            expanded = showThemeModePopup,
                            anchorBounds = themePopupAnchorBounds,
                            onExpandedChange = { showThemeModePopup = it },
                            onSelectedIndexChange = { selectedIndex ->
                                onAppThemeModeChanged(themeModeOptions[selectedIndex].first)
                            },
                            onAnchorBoundsChange = { themePopupAnchorBounds = it },
                            backdrop = null,
                            variant = GlassVariant.SheetAction
                        )
                    }

                    SettingsToggleItem(
                        title = stringResource(R.string.settings_transition_animations_title),
                        summary = if (transitionAnimationsEnabled) {
                            stringResource(R.string.settings_transition_animations_summary_enabled)
                        } else {
                            stringResource(R.string.settings_transition_animations_summary_disabled)
                        },
                        checked = transitionAnimationsEnabled,
                        onCheckedChange = onTransitionAnimationsChanged,
                        infoKey = stringResource(R.string.common_scope),
                        infoValue = stringResource(R.string.settings_transition_animations_scope)
                    )

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
                        title = stringResource(R.string.settings_preloading_title),
                        summary = if (preloadingEnabled) {
                            stringResource(R.string.settings_preloading_summary_enabled)
                        } else {
                            stringResource(R.string.settings_preloading_summary_disabled)
                        },
                        checked = preloadingEnabled,
                        onCheckedChange = onPreloadingEnabledChanged,
                        infoKey = stringResource(R.string.common_scope),
                        infoValue = stringResource(R.string.settings_preloading_scope)
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
            item {
                SettingsGroupCard(
                    header = stringResource(R.string.settings_group_background_header),
                    title = stringResource(R.string.settings_group_background_title),
                    summary = stringResource(R.string.settings_group_background_summary),
                    containerColor = if (backgroundGroupActive) enabledCardColor else disabledCardColor
                ) {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_non_home_background_title),
                        summary = if (nonHomeBackgroundEnabled) {
                            stringResource(R.string.settings_non_home_background_summary_enabled)
                        } else {
                            stringResource(R.string.settings_non_home_background_summary_disabled)
                        },
                        checked = nonHomeBackgroundEnabled,
                        onCheckedChange = onNonHomeBackgroundEnabledChanged,
                        infoKey = stringResource(R.string.common_scope),
                        infoValue = stringResource(R.string.settings_non_home_background_scope)
                    )

                    SettingsActionItem(
                        title = stringResource(R.string.settings_non_home_background_image_title),
                        summary = if (nonHomeBackgroundUri.isBlank()) {
                            stringResource(R.string.settings_non_home_background_image_summary_empty)
                        } else {
                            stringResource(R.string.settings_non_home_background_image_summary_ready)
                        }
                    )
                    AppDualActionRow(
                        first = { modifier ->
                            GlassTextButton(
                                backdrop = contentBackdrop,
                                variant = GlassVariant.SheetPrimaryAction,
                                text = stringResource(R.string.settings_non_home_background_action_select),
                                modifier = modifier,
                                textColor = MiuixTheme.colorScheme.primary,
                                onClick = {
                                    nonHomeBackgroundPickerLauncher.launch(arrayOf("image/*"))
                                }
                            )
                        },
                        second = { modifier ->
                            GlassTextButton(
                                backdrop = contentBackdrop,
                                variant = GlassVariant.SheetDangerAction,
                                text = stringResource(R.string.settings_non_home_background_action_clear),
                                modifier = modifier,
                                textColor = MiuixTheme.colorScheme.error,
                                enabled = nonHomeBackgroundUri.isNotBlank(),
                                onClick = {
                                    deleteManagedNonHomeBackgroundFile(context, nonHomeBackgroundUri)
                                    onNonHomeBackgroundUriChanged("")
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_non_home_background_toast_cleared),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    )

                    SettingsActionItem(
                        title = stringResource(R.string.settings_non_home_background_opacity_title),
                        summary = stringResource(
                            R.string.settings_non_home_background_opacity_summary,
                            formatOpacityPercent(nonHomeBackgroundOpacity)
                        )
                    )
                    Slider(
                        value = nonHomeBackgroundOpacity.coerceIn(
                            NON_HOME_BACKGROUND_OPACITY_MIN,
                            NON_HOME_BACKGROUND_OPACITY_MAX
                        ),
                        onValueChange = onNonHomeBackgroundOpacityChanged,
                        valueRange = NON_HOME_BACKGROUND_OPACITY_MIN..NON_HOME_BACKGROUND_OPACITY_MAX,
                        showKeyPoints = true,
                        keyPoints = NON_HOME_BACKGROUND_OPACITY_KEY_POINTS,
                        magnetThreshold = NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD,
                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        enabled = nonHomeBackgroundEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )
                    SettingsInfoItem(
                        key = stringResource(R.string.common_note),
                        value = stringResource(
                            R.string.settings_non_home_background_opacity_default,
                            formatOpacityPercent(NON_HOME_BACKGROUND_OPACITY_DEFAULT)
                        )
                    )
                }
            }
            item {
                SettingsGroupCard(
                    header = stringResource(R.string.settings_group_log_header),
                    title = stringResource(R.string.settings_group_log_title),
                    summary = stringResource(R.string.settings_group_log_summary),
                    containerColor = if (logGroupActive) enabledCardColor else disabledCardColor
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
                    AppDualActionRow(
                        first = { modifier ->
                            GlassTextButton(
                                backdrop = contentBackdrop,
                                variant = GlassVariant.SheetPrimaryAction,
                                text = if (exportingLogZip) {
                                    stringResource(R.string.common_processing)
                                } else {
                                    stringResource(R.string.settings_log_action_export_zip)
                                },
                                modifier = modifier,
                                textColor = MiuixTheme.colorScheme.primary,
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
                        },
                        second = { modifier ->
                            GlassTextButton(
                                backdrop = contentBackdrop,
                                variant = GlassVariant.SheetDangerAction,
                                text = if (clearingLogs) {
                                    stringResource(R.string.common_processing)
                                } else {
                                    stringResource(R.string.settings_log_action_clear)
                                },
                                modifier = modifier,
                                textColor = MiuixTheme.colorScheme.error,
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
                    )
                }
            }
            item {
                SettingsGroupCard(
                    header = stringResource(R.string.settings_group_notify_header),
                    title = stringResource(R.string.settings_group_notify_title),
                    summary = stringResource(R.string.settings_group_notify_summary),
                    containerColor = if (notifyGroupActive) enabledCardColor else disabledCardColor
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
            item {
                SettingsGroupCard(
                    header = stringResource(R.string.settings_group_copy_header),
                    title = stringResource(R.string.settings_group_copy_title),
                    summary = stringResource(R.string.settings_group_copy_summary),
                    containerColor = if (textCopyCapabilityExpanded) enabledCardColor else disabledCardColor
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
            item {
                SettingsGroupCard(
                    header = stringResource(R.string.settings_cache_header),
                    title = stringResource(R.string.settings_cache_diagnostics_title),
                    summary = if (cacheDiagnosticsEnabled) {
                        stringResource(R.string.settings_cache_diagnostics_summary_enabled)
                    } else {
                        stringResource(R.string.settings_cache_diagnostics_summary_disabled)
                    },
                    containerColor = if (cacheDiagnosticsEnabled) enabledCardColor else disabledCardColor
                ) {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_cache_diagnostics_title),
                        summary = if (cacheDiagnosticsEnabled) {
                            stringResource(R.string.settings_cache_diagnostics_summary_enabled)
                        } else {
                            stringResource(R.string.settings_cache_diagnostics_summary_disabled)
                        },
                        checked = cacheDiagnosticsEnabled,
                        onCheckedChange = onCacheDiagnosticsChanged,
                        infoKey = stringResource(R.string.common_scope),
                        infoValue = if (cacheDiagnosticsEnabled) {
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
                                fontSize = AppTypographyTokens.Supporting.fontSize,
                                lineHeight = AppTypographyTokens.Supporting.lineHeight
                            )
                        }

                        cacheEntries == null -> {
                            Text(
                                text = stringResource(R.string.settings_cache_loading_desc),
                                color = subtitleColor,
                                fontSize = AppTypographyTokens.Supporting.fontSize,
                                lineHeight = AppTypographyTokens.Supporting.lineHeight
                            )
                        }

                        cacheEntries!!.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.settings_cache_empty_desc),
                                color = subtitleColor,
                                fontSize = AppTypographyTokens.Supporting.fontSize,
                                lineHeight = AppTypographyTokens.Supporting.lineHeight
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

@Composable
internal fun SettingsGroupCard(
    header: String,
    title: String,
    summary: String,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    AppFeatureCard(
        title = title,
        subtitle = summary,
        eyebrow = header,
        containerColor = containerColor,
        showIndication = false,
        contentVerticalSpacing = CardLayoutRhythm.denseSectionGap,
        contentPadding = PaddingValues(
            start = CardLayoutRhythm.cardHorizontalPadding,
            end = CardLayoutRhythm.cardHorizontalPadding,
            bottom = CardLayoutRhythm.cardVerticalPadding
        ),
        content = content
    )
}

@Composable
internal fun SettingsActionItem(
    title: String,
    summary: String,
    infoKey: String? = null,
    infoValue: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
    ) {
        AppControlRow(
            title = title,
            summary = summary,
            titleColor = MiuixTheme.colorScheme.onBackground,
            minHeight = 48.dp,
            onClick = onClick,
            trailing = trailing
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
internal fun SettingsToggleItem(
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
        labelFontSize = AppTypographyTokens.Supporting.fontSize,
        labelLineHeight = AppTypographyTokens.Supporting.lineHeight,
        valueFontSize = AppTypographyTokens.Body.fontSize,
        valueLineHeight = AppTypographyTokens.Body.lineHeight,
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
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f)
    val resetLabel = stringResource(R.string.common_reset)
    val actionColor = if (entry.clearLabel == resetLabel) {
        MiuixTheme.colorScheme.error
    } else {
        MiuixTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
    ) {
        AppControlRow(
            title = entry.title,
            summary = entry.summary,
            titleColor = titleColor,
            minHeight = 48.dp,
            trailing = {
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
        )
        Text(
            text = entry.detail,
            color = subtitleColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight
        )
        Text(
            text = entry.activity,
            color = subtitleColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight
        )
        Text(
            text = entry.storage,
            color = subtitleColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight
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

private fun formatOpacityPercent(alpha: Float): Int {
    return (alpha.coerceIn(0f, 1f) * 100f).roundToInt()
}

private const val NON_HOME_BACKGROUND_CROP_DIR = "non_home_background"
private const val NON_HOME_BACKGROUND_CROP_FILE_PREFIX = "cropped_non_home_"
private const val NON_HOME_BACKGROUND_CROP_TARGET_SHORT_EDGE = 1440
private const val NON_HOME_BACKGROUND_CROP_MAX_WIDTH = 2560
private const val NON_HOME_BACKGROUND_CROP_MAX_HEIGHT = 4096
private const val NON_HOME_BACKGROUND_OPACITY_DEFAULT = 0.16f
private const val NON_HOME_BACKGROUND_OPACITY_MIN = 0.06f
private const val NON_HOME_BACKGROUND_OPACITY_MAX = 0.40f
private const val NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD = 0.03f
private val NON_HOME_BACKGROUND_OPACITY_KEY_POINTS = listOf(
    0.06f,
    0.10f,
    0.13f,
    NON_HOME_BACKGROUND_OPACITY_DEFAULT,
    0.20f,
    0.26f,
    0.33f,
    NON_HOME_BACKGROUND_OPACITY_MAX
)

private fun createNonHomeBackgroundCropOutputUri(context: Context): Uri {
    val dir = File(context.filesDir, NON_HOME_BACKGROUND_CROP_DIR)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val output = File(
        dir,
        "$NON_HOME_BACKGROUND_CROP_FILE_PREFIX${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        output
    )
}

private fun resolveNonHomeBackgroundAspectRatio(context: Context): Pair<Float, Float> {
    val metrics = context.resources.displayMetrics
    val widthPx = metrics.widthPixels.coerceAtLeast(1)
    val heightPx = metrics.heightPixels.coerceAtLeast(1)
    return widthPx.toFloat() to heightPx.toFloat()
}

private fun resolveNonHomeBackgroundCropSize(context: Context): Pair<Int, Int> {
    val metrics = context.resources.displayMetrics
    val widthPx = metrics.widthPixels.coerceAtLeast(1)
    val heightPx = metrics.heightPixels.coerceAtLeast(1)
    val shortEdge = min(widthPx, heightPx).coerceAtLeast(1)
    val upscale = (NON_HOME_BACKGROUND_CROP_TARGET_SHORT_EDGE.toFloat() / shortEdge.toFloat())
        .coerceAtLeast(1f)
    val width = (widthPx * upscale).roundToInt().coerceIn(widthPx, NON_HOME_BACKGROUND_CROP_MAX_WIDTH)
    val height = (heightPx * upscale).roundToInt().coerceIn(heightPx, NON_HOME_BACKGROUND_CROP_MAX_HEIGHT)
    return width to height
}

private fun deleteManagedNonHomeBackgroundFile(context: Context, uriText: String) {
    if (uriText.isBlank()) return
    val uri = runCatching { uriText.toUri() }.getOrNull() ?: return
    val target = when (uri.scheme) {
        "file" -> File(uri.path ?: return)
        "content" -> resolveManagedNonHomeBackgroundFileByContentUri(context, uri) ?: return
        else -> return
    }
    if (target.name.startsWith(NON_HOME_BACKGROUND_CROP_FILE_PREFIX).not()) return
    if (target.parentFile?.name != NON_HOME_BACKGROUND_CROP_DIR) return
    runCatching { target.delete() }
}

private fun resolveManagedNonHomeBackgroundFileByContentUri(context: Context, uri: Uri): File? {
    val expectedAuthority = "${context.packageName}.fileprovider"
    if (uri.authority != expectedAuthority) return null
    val fileName = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.startsWith(NON_HOME_BACKGROUND_CROP_FILE_PREFIX) }
        ?: return null
    val dir = File(context.filesDir, NON_HOME_BACKGROUND_CROP_DIR)
    return File(dir, fileName)
}
