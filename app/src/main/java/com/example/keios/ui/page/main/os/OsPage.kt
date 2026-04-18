package com.example.keios.ui.page.main

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.keios.R
import com.example.keios.ui.page.main.widget.AppInfoRow
import com.example.keios.ui.page.main.widget.AppOverviewCard
import com.example.keios.ui.page.main.widget.AppChromeTokens
import com.example.keios.ui.page.main.widget.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.AppPageScaffold
import com.example.keios.ui.page.main.widget.AppTopBarSearchField
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetChoiceCard
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetFieldBlock
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.StatusLabelText
import com.example.keios.ui.page.main.widget.copyModeAwareRow
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.core.system.getAllJavaPropString
import com.example.keios.core.system.getAllSystemProperties
import com.example.keios.ui.page.main.os.OsPageViewModel
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OsPage(
    scrollToTopSignal: Int,
    isPageActive: Boolean = true,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    cardPressFeedbackEnabled: Boolean = true,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    contentBottomPadding: Dp = 72.dp,
    enableSearchBar: Boolean = true,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val inactive = MiuixTheme.colorScheme.onBackgroundVariant
    val titleColor = MiuixTheme.colorScheme.onBackground
    val cachedColor = Color(0xFFF59E0B)
    val refreshingColor = Color(0xFF3B82F6)
    val syncedColor = Color(0xFF22C55E)

    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val exportSuccessText = stringResource(R.string.common_export_success)
    val noRefreshableCardText = stringResource(R.string.os_toast_no_refreshable_card)
    val refreshCompletedText = stringResource(R.string.os_toast_refresh_completed)
    val manageCardsContentDescription = stringResource(R.string.os_action_manage_cards)
    val manageActivitiesContentDescription = stringResource(R.string.os_action_manage_activities)
    val refreshParamsContentDescription = stringResource(R.string.os_action_refresh_params)
    val searchLabel = stringResource(R.string.os_search_label)
    val visibleCardsTitle = stringResource(R.string.os_sheet_visible_cards_title)
    val visibleActivitiesTitle = stringResource(R.string.os_sheet_visible_activities_title)
    val googleSystemServiceDefaultTitle = stringResource(R.string.os_section_google_system_service_title)
    val googleSystemServiceDefaultSubtitle = stringResource(R.string.os_google_system_service_default_subtitle)
    val googleSystemServiceDefaultAppName = stringResource(R.string.os_google_system_service_default_app_name)
    val googleSystemServiceDefaultIntentFlags =
        stringResource(R.string.os_google_system_service_default_intent_flags)
    val googleSystemServiceDefaults = remember(
        googleSystemServiceDefaultTitle,
        googleSystemServiceDefaultSubtitle,
        googleSystemServiceDefaultAppName,
        googleSystemServiceDefaultIntentFlags
    ) {
        OsGoogleSystemServiceConfig(
            title = googleSystemServiceDefaultTitle,
            subtitle = googleSystemServiceDefaultSubtitle,
            appName = googleSystemServiceDefaultAppName,
            intentFlags = googleSystemServiceDefaultIntentFlags
        ).normalized()
    }
    val noMatchedResultsText = stringResource(R.string.common_no_matched_results)
    val shizukuReady = shizukuStatus.contains("granted", ignoreCase = true)
    val initialUiSnapshot = remember { OsUiStateStore.loadSnapshot() }
    var cacheLoaded by remember { mutableStateOf(false) }
    var cachePersisted by remember { mutableStateOf(false) }
    val osPageViewModel: OsPageViewModel = viewModel()
    val queryInput by osPageViewModel.queryInput.collectAsState()
    val queryApplied by osPageViewModel.queryApplied.collectAsState()
    var topInfoExpanded by remember { mutableStateOf(initialUiSnapshot.topInfoExpanded) }
    var googleSystemServiceExpanded by remember { mutableStateOf(initialUiSnapshot.googleSystemServiceExpanded) }
    var systemTableExpanded by remember { mutableStateOf(initialUiSnapshot.systemTableExpanded) }
    var secureTableExpanded by remember { mutableStateOf(initialUiSnapshot.secureTableExpanded) }
    var globalTableExpanded by remember { mutableStateOf(initialUiSnapshot.globalTableExpanded) }
    var androidPropsExpanded by remember { mutableStateOf(initialUiSnapshot.androidPropsExpanded) }
    var javaPropsExpanded by remember { mutableStateOf(initialUiSnapshot.javaPropsExpanded) }
    var linuxEnvExpanded by remember { mutableStateOf(initialUiSnapshot.linuxEnvExpanded) }
    var visibleCards by remember { mutableStateOf(initialUiSnapshot.visibleCards) }
    var googleSystemServiceConfig by remember {
        mutableStateOf(
            OsShortcutCardStore.loadGoogleSystemServiceConfig(
                defaults = googleSystemServiceDefaults
            )
        )
    }
    var googleSystemServiceDraft by remember { mutableStateOf(googleSystemServiceConfig) }
    var showGoogleSystemServiceEditor by rememberSaveable { mutableStateOf(false) }
    var showGoogleSystemServiceSuggestionSheet by rememberSaveable { mutableStateOf(false) }
    var googleSystemServiceSuggestionTarget by remember {
        mutableStateOf(ShortcutSuggestionField.IntentAction)
    }
    var googleSystemServicePackageSuggestions by remember {
        mutableStateOf<List<ShortcutInstalledAppOption>>(emptyList())
    }
    var googleSystemServicePackageSuggestionsLoading by remember { mutableStateOf(false) }
    var googleSystemServicePackageSuggestionQuery by rememberSaveable { mutableStateOf("") }
    var googleSystemServiceClassSuggestions by remember {
        mutableStateOf<List<ShortcutActivityClassOption>>(emptyList())
    }
    var googleSystemServiceClassSuggestionsLoading by remember { mutableStateOf(false) }
    var googleSystemServiceClassSuggestionQuery by rememberSaveable { mutableStateOf("") }
    var uiStatePersistenceReady by remember { mutableStateOf(false) }
    var showCardManager by rememberSaveable { mutableStateOf(false) }
    var showActivityVisibilityManager by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var exportingCard by remember { mutableStateOf<OsSectionCard?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshProgress by remember { mutableStateOf(0f) }
    var showSearchBar by remember { mutableStateOf(true) }
    var searchBarHideOffsetPx by remember { mutableStateOf(0f) }
    val surfaceColor = MiuixTheme.colorScheme.surface
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    val topBarBackdrop: LayerBackdrop = key("os-topbar-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val contentBackdrop: LayerBackdrop = key("os-content-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val sheetBackdrop: LayerBackdrop = key("os-sheet-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val searchBarHideThresholdPx = remember(density) { with(density) { 28.dp.toPx() } }
    val searchBarScrollConnection = remember(searchBarHideThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) {
                    if (showSearchBar) {
                        searchBarHideOffsetPx = (searchBarHideOffsetPx + (-available.y)).coerceAtMost(searchBarHideThresholdPx)
                        if (searchBarHideOffsetPx >= searchBarHideThresholdPx) {
                            showSearchBar = false
                            searchBarHideOffsetPx = 0f
                        }
                    }
                }
                if (available.y > 1f) {
                    showSearchBar = true
                    searchBarHideOffsetPx = 0f
                }
                return Offset.Zero
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingExportContent
        if (uri == null || content.isNullOrBlank()) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                writer?.write(content)
            }
        }.onSuccess {
            Toast.makeText(context, exportSuccessText, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(
                context,
                context.getString(R.string.common_export_failed_with_reason, it.javaClass.simpleName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var sectionStates by remember {
        mutableStateOf(
            mapOf(
                SectionKind.SYSTEM to SectionState(),
                SectionKind.SECURE to SectionState(),
                SectionKind.GLOBAL to SectionState(),
                SectionKind.ANDROID to SectionState(),
                SectionKind.JAVA to SectionState(),
                SectionKind.LINUX to SectionState()
            )
        )
    }

    fun visibleSectionKinds(): Set<SectionKind> = buildSet {
        if (visibleCards.contains(OsSectionCard.SYSTEM)) add(SectionKind.SYSTEM)
        if (visibleCards.contains(OsSectionCard.SECURE)) add(SectionKind.SECURE)
        if (visibleCards.contains(OsSectionCard.GLOBAL)) add(SectionKind.GLOBAL)
        if (visibleCards.contains(OsSectionCard.ANDROID)) add(SectionKind.ANDROID)
        if (visibleCards.contains(OsSectionCard.JAVA)) add(SectionKind.JAVA)
        if (visibleCards.contains(OsSectionCard.LINUX)) add(SectionKind.LINUX)
    }

    fun isCardVisible(card: OsSectionCard): Boolean = visibleCards.contains(card)

    fun updateSection(section: SectionKind, transform: (SectionState) -> SectionState) {
        sectionStates = sectionStates.toMutableMap().also { map ->
            val old = map[section] ?: SectionState()
            map[section] = transform(old)
        }
    }

    suspend fun ensureLoad(section: SectionKind, forceRefresh: Boolean = false) {
        if (!visibleSectionKinds().contains(section)) return
        val current = sectionStates[section] ?: SectionState()
        if (current.loading) return
        if (!forceRefresh) {
            if (current.loadedFresh) return
            if (current.rows.isNotEmpty()) return
        }
        updateSection(section) { it.copy(loading = true) }
        val fresh = withContext(Dispatchers.IO) {
            buildSectionRows(section, context, shizukuStatus, shizukuApiUtils)
        }
        updateSection(section) { it.copy(rows = fresh, loading = false, loadedFresh = true) }
        cachePersisted = withContext(Dispatchers.IO) {
            OsInfoCache.write(section, fresh)
            OsInfoCache.readSnapshot(visibleSectionKinds()).hasPersistedCache
        }
    }

    suspend fun applyCardVisibility(card: OsSectionCard, visible: Boolean) {
        val updated = visibleCards.toMutableSet().apply {
            if (visible) add(card) else remove(card)
        }.toSet()
        visibleCards = updated
        withContext(Dispatchers.IO) { OsCardVisibilityStore.saveVisibleCards(updated) }
        when (card) {
            OsSectionCard.TOP_INFO -> {
                if (!visible) topInfoExpanded = false
            }
            OsSectionCard.GOOGLE_SYSTEM_SERVICE -> {
                if (!visible) {
                    googleSystemServiceExpanded = false
                }
            }
            OsSectionCard.SYSTEM -> {
                if (!visible) {
                    systemTableExpanded = false
                    updateSection(SectionKind.SYSTEM) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.SYSTEM) }
                } else ensureLoad(SectionKind.SYSTEM, forceRefresh = true)
            }
            OsSectionCard.SECURE -> {
                if (!visible) {
                    secureTableExpanded = false
                    updateSection(SectionKind.SECURE) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.SECURE) }
                } else ensureLoad(SectionKind.SECURE, forceRefresh = true)
            }
            OsSectionCard.GLOBAL -> {
                if (!visible) {
                    globalTableExpanded = false
                    updateSection(SectionKind.GLOBAL) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.GLOBAL) }
                } else ensureLoad(SectionKind.GLOBAL, forceRefresh = true)
            }
            OsSectionCard.ANDROID -> {
                if (!visible) {
                    androidPropsExpanded = false
                    updateSection(SectionKind.ANDROID) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.ANDROID) }
                } else ensureLoad(SectionKind.ANDROID, forceRefresh = true)
            }
            OsSectionCard.JAVA -> {
                if (!visible) {
                    javaPropsExpanded = false
                    updateSection(SectionKind.JAVA) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.JAVA) }
                } else ensureLoad(SectionKind.JAVA, forceRefresh = true)
            }
            OsSectionCard.LINUX -> {
                if (!visible) {
                    linuxEnvExpanded = false
                    updateSection(SectionKind.LINUX) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.LINUX) }
                } else ensureLoad(SectionKind.LINUX, forceRefresh = true)
            }
        }
        cachePersisted = withContext(Dispatchers.IO) {
            OsInfoCache.readSnapshot(visibleSectionKinds()).hasPersistedCache
        }
    }

    suspend fun refreshAllSections() {
        refreshing = true
        refreshProgress = 0f
        try {
            val targets = SectionKind.entries.filter { visibleSectionKinds().contains(it) }
            val sectionCount = targets.size.coerceAtLeast(1)
            targets.forEachIndexed { index, section ->
                ensureLoad(section, forceRefresh = true)
                refreshProgress = (index + 1).toFloat() / sectionCount.toFloat()
            }
            Toast.makeText(
                context,
                if (targets.isEmpty()) noRefreshableCardText else refreshCompletedText,
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(Unit) {
        val visibleSections = visibleSectionKinds()
        val snapshot = withContext(Dispatchers.IO) {
            OsInfoCache.readSnapshot(visibleSections)
        }
        val cached = snapshot.cached
        sectionStates = mapOf(
            SectionKind.SYSTEM to SectionState(rows = if (visibleSections.contains(SectionKind.SYSTEM)) cached.system else emptyList()),
            SectionKind.SECURE to SectionState(rows = if (visibleSections.contains(SectionKind.SECURE)) cached.secure else emptyList()),
            SectionKind.GLOBAL to SectionState(rows = if (visibleSections.contains(SectionKind.GLOBAL)) cached.global else emptyList()),
            SectionKind.ANDROID to SectionState(rows = if (visibleSections.contains(SectionKind.ANDROID)) cached.android else emptyList()),
            SectionKind.JAVA to SectionState(rows = if (visibleSections.contains(SectionKind.JAVA)) cached.java else emptyList()),
            SectionKind.LINUX to SectionState(rows = if (visibleSections.contains(SectionKind.LINUX)) cached.linux else emptyList())
        )
        cachePersisted = snapshot.hasPersistedCache
        cacheLoaded = true
        uiStatePersistenceReady = true
        if (isPageActive) {
            visibleSections.forEach { section ->
                ensureLoad(section, forceRefresh = false)
            }
        }
    }

    LaunchedEffect(shizukuReady) {
        updateSection(SectionKind.SYSTEM) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.SECURE) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.GLOBAL) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.LINUX) { it.copy(loadedFresh = false) }
    }

    LaunchedEffect(topInfoExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) { OsUiStateStore.setTopInfoExpanded(topInfoExpanded) }
    }
    LaunchedEffect(googleSystemServiceExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            OsUiStateStore.setGoogleSystemServiceExpanded(googleSystemServiceExpanded)
        }
    }
    LaunchedEffect(systemTableExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) { OsUiStateStore.setOsSystemTableExpanded(systemTableExpanded) }
    }
    LaunchedEffect(secureTableExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) { OsUiStateStore.setSecureTableExpanded(secureTableExpanded) }
    }
    LaunchedEffect(globalTableExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) { OsUiStateStore.setGlobalTableExpanded(globalTableExpanded) }
    }
    LaunchedEffect(androidPropsExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) { OsUiStateStore.setAndroidPropsExpanded(androidPropsExpanded) }
    }
    LaunchedEffect(javaPropsExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) { OsUiStateStore.setJavaPropsExpanded(javaPropsExpanded) }
    }
    LaunchedEffect(linuxEnvExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) { OsUiStateStore.setLinuxEnvExpanded(linuxEnvExpanded) }
    }

    LaunchedEffect(systemTableExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && systemTableExpanded && isCardVisible(OsSectionCard.SYSTEM)) ensureLoad(SectionKind.SYSTEM)
    }
    LaunchedEffect(secureTableExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && secureTableExpanded && isCardVisible(OsSectionCard.SECURE)) ensureLoad(SectionKind.SECURE)
    }
    LaunchedEffect(globalTableExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && globalTableExpanded && isCardVisible(OsSectionCard.GLOBAL)) ensureLoad(SectionKind.GLOBAL)
    }
    LaunchedEffect(androidPropsExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && androidPropsExpanded && isCardVisible(OsSectionCard.ANDROID)) ensureLoad(SectionKind.ANDROID)
    }
    LaunchedEffect(javaPropsExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && javaPropsExpanded && isCardVisible(OsSectionCard.JAVA)) ensureLoad(SectionKind.JAVA)
    }
    LaunchedEffect(linuxEnvExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && linuxEnvExpanded && isCardVisible(OsSectionCard.LINUX)) ensureLoad(SectionKind.LINUX)
    }
    LaunchedEffect(
        showGoogleSystemServiceSuggestionSheet,
        googleSystemServiceSuggestionTarget,
        googleSystemServiceDraft.packageName
    ) {
        if (!showGoogleSystemServiceSuggestionSheet) return@LaunchedEffect
        when (googleSystemServiceSuggestionTarget) {
            ShortcutSuggestionField.PackageName -> {
                googleSystemServicePackageSuggestionsLoading = true
                runCatching {
                    withContext(Dispatchers.IO) { loadInstalledAppOptions(context) }
                }.onSuccess { apps ->
                    googleSystemServicePackageSuggestions = apps
                }.onFailure {
                    googleSystemServicePackageSuggestions = emptyList()
                }
                googleSystemServicePackageSuggestionsLoading = false
            }

            ShortcutSuggestionField.ClassName -> {
                val targetPackageName = googleSystemServiceDraft.packageName.trim()
                if (targetPackageName.isBlank()) {
                    googleSystemServiceClassSuggestions = emptyList()
                    return@LaunchedEffect
                }
                googleSystemServiceClassSuggestionsLoading = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        loadActivityClassOptions(
                            context = context,
                            packageName = targetPackageName
                        )
                    }
                }.onSuccess { classes ->
                    googleSystemServiceClassSuggestions = classes
                }.onFailure {
                    googleSystemServiceClassSuggestions = emptyList()
                }
                googleSystemServiceClassSuggestionsLoading = false
            }

            else -> Unit
        }
    }

    val systemRows = sectionStates[SectionKind.SYSTEM]?.rows ?: emptyList()
    val secureRows = sectionStates[SectionKind.SECURE]?.rows ?: emptyList()
    val globalRows = sectionStates[SectionKind.GLOBAL]?.rows ?: emptyList()
    val androidRows = sectionStates[SectionKind.ANDROID]?.rows ?: emptyList()
    val javaRows = sectionStates[SectionKind.JAVA]?.rows ?: emptyList()
    val linuxRows = sectionStates[SectionKind.LINUX]?.rows ?: emptyList()

    val topInfoRows = remember(systemRows, secureRows, globalRows, androidRows, javaRows, linuxRows) {
        buildTopInfoRows(systemRows, secureRows, globalRows, androidRows, javaRows, linuxRows)
    }

    val prunedSystemRows = remember(systemRows) { removeTopInfoRows(SectionKind.SYSTEM, systemRows) }
    val prunedSecureRows = remember(secureRows) { removeTopInfoRows(SectionKind.SECURE, secureRows) }
    val prunedGlobalRows = remember(globalRows) { removeTopInfoRows(SectionKind.GLOBAL, globalRows) }
    val prunedAndroidRows = remember(androidRows) { removeTopInfoRows(SectionKind.ANDROID, androidRows) }
    val prunedJavaRows = remember(javaRows) { removeTopInfoRows(SectionKind.JAVA, javaRows) }
    val prunedLinuxRows = remember(linuxRows) { removeTopInfoRows(SectionKind.LINUX, linuxRows) }

    val q = queryApplied.trim()
    val displayedTopInfoRows = remember(q, topInfoRows, topInfoExpanded) {
        if (q.isBlank() && !topInfoExpanded) topInfoRows else sortRowsByType(filterRows(topInfoRows, q))
    }
    val displayedSystemRows = remember(q, prunedSystemRows, systemTableExpanded) {
        if (q.isBlank() && !systemTableExpanded) prunedSystemRows else sortRowsByType(filterRows(prunedSystemRows, q))
    }
    val displayedSecureRows = remember(q, prunedSecureRows, secureTableExpanded) {
        if (q.isBlank() && !secureTableExpanded) prunedSecureRows else sortRowsByType(filterRows(prunedSecureRows, q))
    }
    val displayedGlobalRows = remember(q, prunedGlobalRows, globalTableExpanded) {
        if (q.isBlank() && !globalTableExpanded) prunedGlobalRows else sortRowsByType(filterRows(prunedGlobalRows, q))
    }
    val displayedAndroidRows = remember(q, prunedAndroidRows, androidPropsExpanded) {
        if (q.isBlank() && !androidPropsExpanded) prunedAndroidRows else sortRowsByType(filterRows(prunedAndroidRows, q))
    }
    val displayedJavaRows = remember(q, prunedJavaRows, javaPropsExpanded) {
        if (q.isBlank() && !javaPropsExpanded) prunedJavaRows else sortRowsByType(filterRows(prunedJavaRows, q))
    }
    val displayedLinuxRows = remember(q, prunedLinuxRows, linuxEnvExpanded) {
        if (q.isBlank() && !linuxEnvExpanded) prunedLinuxRows else sortRowsByType(filterRows(prunedLinuxRows, q))
    }
    val groupedTopInfoRows = remember(displayedTopInfoRows, topInfoExpanded, q) {
        if (q.isBlank() && !topInfoExpanded) emptyList() else groupTopInfoRows(displayedTopInfoRows)
    }
    val totalRowsCount = remember(
        topInfoRows.size,
        prunedSystemRows.size,
        prunedSecureRows.size,
        prunedGlobalRows.size,
        prunedAndroidRows.size,
        prunedJavaRows.size,
        prunedLinuxRows.size
    ) {
        topInfoRows.size +
            prunedSystemRows.size +
            prunedSecureRows.size +
            prunedGlobalRows.size +
            prunedAndroidRows.size +
            prunedJavaRows.size +
            prunedLinuxRows.size
    }
    val visibleRowsCount = remember(
        displayedTopInfoRows.size,
        displayedSystemRows.size,
        displayedSecureRows.size,
        displayedGlobalRows.size,
        displayedAndroidRows.size,
        displayedJavaRows.size,
        displayedLinuxRows.size
    ) {
        displayedTopInfoRows.size +
            displayedSystemRows.size +
            displayedSecureRows.size +
            displayedGlobalRows.size +
            displayedAndroidRows.size +
            displayedJavaRows.size +
            displayedLinuxRows.size
    }

    fun sectionSubtitle(section: SectionKind, size: Int): String {
        val state = sectionStates[section] ?: SectionState()
        return when {
            state.loading -> context.getString(R.string.common_loading)
            !state.loadedFresh && state.rows.isNotEmpty() -> {
                context.getString(R.string.common_item_count_cached, size)
            }
            !state.loadedFresh && state.rows.isEmpty() -> context.getString(R.string.common_not_loaded)
            else -> context.getString(R.string.common_item_count, size)
        }
    }

    fun buildGoogleSystemServiceRows(config: OsGoogleSystemServiceConfig): List<InfoRow> {
        val normalized = config.normalized(googleSystemServiceDefaults)
        val emptyDataValue = context.getString(R.string.os_google_system_service_value_data_empty)
        val actionValue = normalized.intentAction.ifBlank {
            context.getString(R.string.os_google_system_service_value_action_auto_default)
        }
        val flagsValue = normalized.intentFlags.ifBlank {
            context.getString(R.string.os_google_system_service_value_flags_auto_default)
        }
        return listOf(
            InfoRow(
                key = context.getString(R.string.os_google_system_service_label_app_name),
                value = normalized.appName
            ),
            InfoRow(
                key = context.getString(R.string.os_google_system_service_label_package_name),
                value = normalized.packageName
            ),
            InfoRow(
                key = context.getString(R.string.os_google_system_service_label_class_name),
                value = normalized.className
            ),
            InfoRow(
                key = context.getString(R.string.os_google_system_service_label_intent_action),
                value = actionValue
            ),
            InfoRow(
                key = context.getString(R.string.os_google_system_service_label_intent_category),
                value = normalized.intentCategory.ifBlank { emptyDataValue }
            ),
            InfoRow(
                key = context.getString(R.string.os_google_system_service_label_intent_flags),
                value = flagsValue
            ),
            InfoRow(
                key = context.getString(R.string.os_google_system_service_label_intent_data),
                value = normalized.intentUriData.ifBlank { emptyDataValue }
            ),
            InfoRow(
                key = context.getString(R.string.os_google_system_service_label_intent_mime_type),
                value = normalized.intentMimeType.ifBlank { emptyDataValue }
            )
        )
    }

    fun openGoogleSystemServiceActivity() {
        val config = googleSystemServiceConfig.normalized(googleSystemServiceDefaults)
        val packageName = config.packageName.trim()
        val className = config.className.trim()
        val action = config.intentAction.trim()
        if (packageName.isBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.os_google_system_service_toast_invalid_target),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        runCatching {
            val intent = if (className.isBlank()) {
                context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    if (action.isNotBlank()) {
                        this.action = action
                    }
                } ?: if (action.isBlank()) {
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setPackage(packageName)
                    }
                } else {
                    Intent(action).apply { setPackage(packageName) }
                }
            } else {
                Intent(action.ifBlank { Intent.ACTION_VIEW }).apply {
                    setClassName(packageName, className)
                }
            }

            intent.apply {
                val dataText = config.intentUriData.trim()
                val mimeType = config.intentMimeType.trim()
                if (dataText.isNotBlank() && mimeType.isNotBlank()) {
                    setDataAndType(Uri.parse(dataText), mimeType)
                } else if (dataText.isNotBlank()) {
                    data = Uri.parse(dataText)
                } else if (mimeType.isNotBlank()) {
                    type = mimeType
                }
                parseIntentCategories(config.intentCategory).forEach { category ->
                    addCategory(category)
                }
                val parsedFlags = parseIntentFlags(config.intentFlags)
                addFlags(parsedFlags.ifBlankUse(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            context.startActivity(intent)
        }.onFailure { error ->
            Toast.makeText(
                context,
                context.getString(
                    R.string.os_google_system_service_toast_open_failed,
                    error.javaClass.simpleName
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openGoogleSystemServiceSuggestionSheet(target: ShortcutSuggestionField) {
        googleSystemServiceSuggestionTarget = target
        when (target) {
            ShortcutSuggestionField.PackageName -> googleSystemServicePackageSuggestionQuery = ""
            ShortcutSuggestionField.ClassName -> googleSystemServiceClassSuggestionQuery = ""
            else -> Unit
        }
        showGoogleSystemServiceSuggestionSheet = true
    }

    fun currentGoogleSystemServiceSuggestionFieldValue(target: ShortcutSuggestionField): String {
        return when (target) {
            ShortcutSuggestionField.PackageName -> googleSystemServiceDraft.packageName
            ShortcutSuggestionField.ClassName -> googleSystemServiceDraft.className
            ShortcutSuggestionField.IntentAction -> googleSystemServiceDraft.intentAction
            ShortcutSuggestionField.IntentCategory -> googleSystemServiceDraft.intentCategory
            ShortcutSuggestionField.IntentFlags -> googleSystemServiceDraft.intentFlags
            ShortcutSuggestionField.IntentUriData -> googleSystemServiceDraft.intentUriData
            ShortcutSuggestionField.IntentMimeType -> googleSystemServiceDraft.intentMimeType
        }
    }

    fun applyShortcutExplicitDefaults(
        draft: OsGoogleSystemServiceConfig,
        className: String
    ): OsGoogleSystemServiceConfig {
        return draft.copy(
            className = className.trim(),
            intentAction = Intent.ACTION_VIEW,
            intentCategory = "",
            intentFlags = draft.intentFlags.ifBlank { googleSystemServiceDefaultIntentFlags }
        )
    }

    fun applyShortcutImplicitDefaults(
        draft: OsGoogleSystemServiceConfig
    ): OsGoogleSystemServiceConfig {
        return draft.copy(
            className = "",
            intentAction = Intent.ACTION_MAIN,
            intentCategory = Intent.CATEGORY_LAUNCHER,
            intentFlags = draft.intentFlags.ifBlank { googleSystemServiceDefaultIntentFlags }
        )
    }

    fun applyGoogleSystemServiceSuggestion(item: ShortcutSuggestionItem) {
        when (googleSystemServiceSuggestionTarget) {
            ShortcutSuggestionField.PackageName -> {
                val nextPackageName = item.value.trim()
                val nextAppName = item.relatedAppName.trim()
                googleSystemServiceDraft = googleSystemServiceDraft.copy(
                    packageName = nextPackageName,
                    appName = if (nextAppName.isNotBlank()) nextAppName else googleSystemServiceDraft.appName
                )
            }

            ShortcutSuggestionField.ClassName -> {
                val nextClassName = item.value.trim()
                googleSystemServiceDraft = if (nextClassName.isBlank()) {
                    applyShortcutImplicitDefaults(googleSystemServiceDraft)
                } else {
                    applyShortcutExplicitDefaults(
                        draft = googleSystemServiceDraft,
                        className = nextClassName
                    )
                }
            }

            ShortcutSuggestionField.IntentAction -> {
                googleSystemServiceDraft = googleSystemServiceDraft.copy(intentAction = item.value)
            }

            ShortcutSuggestionField.IntentCategory -> {
                val next = mergeIntentTokenText(
                    current = googleSystemServiceDraft.intentCategory,
                    incoming = item.value,
                    append = item.append
                )
                googleSystemServiceDraft = googleSystemServiceDraft.copy(intentCategory = next)
            }

            ShortcutSuggestionField.IntentFlags -> {
                val next = mergeIntentTokenText(
                    current = googleSystemServiceDraft.intentFlags,
                    incoming = item.value,
                    append = item.append
                )
                googleSystemServiceDraft = googleSystemServiceDraft.copy(intentFlags = next)
            }

            ShortcutSuggestionField.IntentUriData -> {
                googleSystemServiceDraft = googleSystemServiceDraft.copy(intentUriData = item.value)
            }

            ShortcutSuggestionField.IntentMimeType -> {
                googleSystemServiceDraft = googleSystemServiceDraft.copy(intentMimeType = item.value)
            }
        }
        showGoogleSystemServiceSuggestionSheet = false
    }

    fun sectionKindByCard(card: OsSectionCard): SectionKind? = when (card) {
        OsSectionCard.TOP_INFO -> null
        OsSectionCard.GOOGLE_SYSTEM_SERVICE -> null
        OsSectionCard.SYSTEM -> SectionKind.SYSTEM
        OsSectionCard.SECURE -> SectionKind.SECURE
        OsSectionCard.GLOBAL -> SectionKind.GLOBAL
        OsSectionCard.ANDROID -> SectionKind.ANDROID
        OsSectionCard.JAVA -> SectionKind.JAVA
        OsSectionCard.LINUX -> SectionKind.LINUX
    }

    fun currentRowsForCard(card: OsSectionCard): List<InfoRow> {
        return when (card) {
            OsSectionCard.TOP_INFO -> {
                val system = sectionStates[SectionKind.SYSTEM]?.rows ?: emptyList()
                val secure = sectionStates[SectionKind.SECURE]?.rows ?: emptyList()
                val global = sectionStates[SectionKind.GLOBAL]?.rows ?: emptyList()
                val android = sectionStates[SectionKind.ANDROID]?.rows ?: emptyList()
                val java = sectionStates[SectionKind.JAVA]?.rows ?: emptyList()
                val linux = sectionStates[SectionKind.LINUX]?.rows ?: emptyList()
                buildTopInfoRows(system, secure, global, android, java, linux)
            }

            OsSectionCard.GOOGLE_SYSTEM_SERVICE -> {
                buildGoogleSystemServiceRows(googleSystemServiceConfig)
            }

            else -> {
                val section = sectionKindByCard(card) ?: return emptyList()
                removeTopInfoRows(section, sectionStates[section]?.rows ?: emptyList())
            }
        }
    }

    fun exportSlug(card: OsSectionCard): String = when (card) {
        OsSectionCard.TOP_INFO -> "top-info"
        OsSectionCard.GOOGLE_SYSTEM_SERVICE -> "google-system-service"
        OsSectionCard.SYSTEM -> "system-table"
        OsSectionCard.SECURE -> "secure-table"
        OsSectionCard.GLOBAL -> "global-table"
        OsSectionCard.ANDROID -> "android-properties"
        OsSectionCard.JAVA -> "java-properties"
        OsSectionCard.LINUX -> "linux-environment"
    }

    suspend fun exportCard(card: OsSectionCard) {
        if (exportingCard != null) return
        exportingCard = card
        try {
            when (card) {
                OsSectionCard.TOP_INFO -> {
                    visibleSectionKinds().forEach { section ->
                        ensureLoad(section, forceRefresh = false)
                    }
                }

                else -> {
                    sectionKindByCard(card)?.let { section ->
                        ensureLoad(section, forceRefresh = false)
                    }
                }
            }

            val rows = currentRowsForCard(card)
            val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val payload = buildOsCardJson(
                generatedAt = generatedAt,
                shizukuStatus = shizukuStatus,
                cardTitle = card.title,
                rows = rows
            )
            val exportStamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.getDefault()).format(Date())
            val fileName = "keios-os-${exportSlug(card)}-$exportStamp.json"
            pendingExportContent = payload
            exportLauncher.launch(fileName)
        } catch (t: Throwable) {
            Toast.makeText(context, "导出失败: ${t.javaClass.simpleName}", Toast.LENGTH_SHORT).show()
        } finally {
            exportingCard = null
        }
    }

    @Composable
    fun CardExportAction(card: OsSectionCard) {
        val isExporting = exportingCard == card
        val enabled = exportingCard == null || isExporting
        val tint = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackgroundVariant
        Icon(
            imageVector = if (isExporting) MiuixIcons.Regular.Refresh else MiuixIcons.Regular.Download,
            contentDescription = if (isExporting) "准备导出中" else "导出${card.title}",
            tint = tint,
            modifier = Modifier.clickable(enabled = enabled && !isExporting) {
                scope.launch { exportCard(card) }
            }
        )
    }

    val visibleSectionKinds = visibleSectionKinds()
    val visibleSectionStates = visibleSectionKinds.mapNotNull { sectionStates[it] }
    val loadedFreshCount = visibleSectionStates.count { it.loadedFresh }
    val cachedSectionCount = visibleSectionStates.count { !it.loadedFresh && it.rows.isNotEmpty() }
    val sectionCount = visibleSectionKinds.size
    val overviewState = when {
        refreshing -> SystemOverviewState.Refreshing
        loadedFreshCount == sectionCount && sectionCount > 0 -> SystemOverviewState.Completed
        cachePersisted || cachedSectionCount > 0 -> SystemOverviewState.Cached
        else -> SystemOverviewState.Idle
    }
    val statusLabel = when (overviewState) {
        SystemOverviewState.Cached -> StatusLabelText.Cached
        SystemOverviewState.Refreshing -> StatusLabelText.Syncing
        SystemOverviewState.Completed -> StatusLabelText.Synced
        SystemOverviewState.Idle -> StatusLabelText.PendingSync
    }
    val statusColor = when (overviewState) {
        SystemOverviewState.Cached -> cachedColor
        SystemOverviewState.Refreshing -> refreshingColor
        SystemOverviewState.Completed -> syncedColor
        SystemOverviewState.Idle -> inactive
    }
    val overviewCardColor = if (isDark) {
        statusColor.copy(alpha = 0.16f)
    } else {
        statusColor.copy(alpha = 0.09f)
    }
    val overviewBorderColor = if (isDark) {
        statusColor.copy(alpha = 0.32f)
    } else {
        statusColor.copy(alpha = 0.26f)
    }
    val indicatorProgress = when (overviewState) {
        SystemOverviewState.Refreshing -> refreshProgress.coerceIn(0f, 1f)
        SystemOverviewState.Completed,
        SystemOverviewState.Cached -> 1f
        SystemOverviewState.Idle -> 0f
    }
    val indicatorBg = when (overviewState) {
        SystemOverviewState.Refreshing -> Color(0x553B82F6)
        SystemOverviewState.Completed -> Color(0x5522C55E)
        SystemOverviewState.Cached -> Color(0x55F59E0B)
        SystemOverviewState.Idle -> MiuixTheme.colorScheme.surface
    }
    val overviewMetrics = remember(
        topInfoRows.size,
        totalRowsCount,
        visibleRowsCount,
        sectionCount,
        loadedFreshCount
    ) {
        listOf(
            OsOverviewMetric(
                label = context.getString(R.string.os_overview_metric_visible_rows),
                value = "$visibleRowsCount/$totalRowsCount"
            ),
            OsOverviewMetric(
                label = context.getString(R.string.os_overview_metric_top_info),
                value = context.getString(R.string.common_item_count, topInfoRows.size)
            ),
            OsOverviewMetric(
                label = context.getString(R.string.os_overview_metric_fresh_coverage),
                value = "$loadedFreshCount/$sectionCount"
            ),
            OsOverviewMetric(
                label = context.getString(R.string.os_overview_metric_visible_sections),
                value = context.getString(R.string.os_overview_metric_visible_sections_value, sectionCount)
            )
        )
    }

    AppPageScaffold(
        title = "",
        modifier = Modifier.fillMaxSize(),
        largeTitle = "OS",
        scrollBehavior = scrollBehavior,
        topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
        actions = {
            LiquidActionBar(
                backdrop = topBarBackdrop,
                layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                items = listOf(
                    LiquidActionItem(
                        icon = MiuixIcons.Regular.Layers,
                        contentDescription = manageCardsContentDescription,
                        onClick = { showCardManager = true }
                    ),
                    LiquidActionItem(
                        icon = MiuixIcons.Regular.GridView,
                        contentDescription = manageActivitiesContentDescription,
                        onClick = { showActivityVisibilityManager = true }
                    ),
                    LiquidActionItem(
                        icon = MiuixIcons.Regular.Refresh,
                        contentDescription = refreshParamsContentDescription,
                        onClick = {
                            if (refreshing) return@LiquidActionItem
                            scope.launch { refreshAllSections() }
                        }
                    )
                ),
                onInteractionChanged = onActionBarInteractingChanged
            )
        },
        searchBarVisible = enableSearchBar && showSearchBar,
        searchBarAnimationLabelPrefix = "osSearchBar",
        searchBarContent = {
            Column {
                AppTopBarSearchField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppChromeTokens.searchFieldHorizontalPadding),
                    value = queryInput,
                    onValueChange = { osPageViewModel.updateQueryInput(it) },
                    label = searchLabel,
                    backdrop = topBarBackdrop
                )
            }
        }
    ) { innerPadding ->
        SnapshotWindowBottomSheet(
            show = showCardManager,
            title = visibleCardsTitle,
            onDismissRequest = { showCardManager = false },
            startAction = {
                GlassIconButton(
                    backdrop = sheetBackdrop,
                    variant = GlassVariant.Bar,
                    icon = MiuixIcons.Regular.Close,
                    contentDescription = stringResource(R.string.common_close),
                    onClick = { showCardManager = false }
                )
            }
        ) {
            SheetContentColumn(
                scrollable = false,
                verticalSpacing = 10.dp
            ) {
                @Composable
                fun CardLabel(card: OsSectionCard, modifier: Modifier = Modifier) {
                    Row(
                        modifier = modifier,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconModifier = Modifier
                            .size(18.dp)
                            .defaultMinSize(minHeight = 18.dp)
                        val icon = sectionCardIcon(card)
                        Icon(
                            imageVector = icon,
                            contentDescription = card.title,
                            tint = MiuixTheme.colorScheme.onBackground,
                            modifier = iconModifier
                        )
                        Text(text = card.title, color = MiuixTheme.colorScheme.onBackground)
                    }
                }

                SheetSectionCard(verticalSpacing = 10.dp) {
                    OsSectionCard.entries.filter { it != OsSectionCard.GOOGLE_SYSTEM_SERVICE }.forEach { card ->
                        SheetControlRow(
                            labelContent = {
                                CardLabel(card = card, modifier = Modifier.defaultMinSize(minHeight = 24.dp))
                            }
                        ) {
                            Switch(
                                checked = isCardVisible(card),
                                onCheckedChange = { checked ->
                                    scope.launch { applyCardVisibility(card, checked) }
                                }
                            )
                        }
                    }
                }

                SheetDescriptionText(
                    text = "隐藏卡片后会清空对应缓存；重新显示时会立即重新获取并缓存。"
                )
            }
        }
        SnapshotWindowBottomSheet(
            show = showActivityVisibilityManager,
            title = visibleActivitiesTitle,
            onDismissRequest = { showActivityVisibilityManager = false },
            startAction = {
                GlassIconButton(
                    backdrop = sheetBackdrop,
                    variant = GlassVariant.Bar,
                    icon = MiuixIcons.Regular.Close,
                    contentDescription = stringResource(R.string.common_close),
                    onClick = { showActivityVisibilityManager = false }
                )
            }
        ) {
            SheetContentColumn(
                scrollable = false,
                verticalSpacing = 10.dp
            ) {
                val shortcutConfig = googleSystemServiceConfig.normalized(googleSystemServiceDefaults)
                val activityVisibilityItems = listOf(
                    OsActivityVisibilityItem(
                        card = OsSectionCard.GOOGLE_SYSTEM_SERVICE,
                        title = shortcutConfig.title.ifBlank { OsSectionCard.GOOGLE_SYSTEM_SERVICE.title },
                        packageName = shortcutConfig.packageName
                    )
                )
                SheetSectionCard(verticalSpacing = 10.dp) {
                    activityVisibilityItems.forEach { item ->
                        SheetControlRow(
                            labelContent = {
                                Row(
                                    modifier = Modifier.defaultMinSize(minHeight = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.packageName.isNotBlank()) {
                                        AppIcon(
                                            packageName = item.packageName,
                                            size = 18.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = sectionCardIcon(item.card),
                                            contentDescription = item.title,
                                            tint = MiuixTheme.colorScheme.onBackground,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .defaultMinSize(minHeight = 18.dp)
                                        )
                                    }
                                    Text(
                                        text = item.title,
                                        color = MiuixTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        ) {
                            Switch(
                                checked = isCardVisible(item.card),
                                onCheckedChange = { checked ->
                                    scope.launch { applyCardVisibility(item.card, checked) }
                                }
                            )
                        }
                    }
                }
                SheetDescriptionText(
                    text = stringResource(R.string.os_sheet_visible_activities_desc)
                )
            }
        }
        SnapshotWindowBottomSheet(
            show = showGoogleSystemServiceEditor,
            title = stringResource(R.string.os_google_system_service_sheet_title),
            onDismissRequest = { showGoogleSystemServiceEditor = false },
            startAction = {
                GlassIconButton(
                    backdrop = sheetBackdrop,
                    variant = GlassVariant.Bar,
                    icon = MiuixIcons.Regular.Close,
                    contentDescription = stringResource(R.string.common_close),
                    onClick = { showGoogleSystemServiceEditor = false }
                )
            },
            endAction = {
                GlassIconButton(
                    backdrop = sheetBackdrop,
                    variant = GlassVariant.Bar,
                    icon = MiuixIcons.Regular.Ok,
                    contentDescription = stringResource(R.string.common_save),
                    onClick = {
                        val normalized = googleSystemServiceDraft.normalized(googleSystemServiceDefaults)
                        googleSystemServiceConfig = normalized
                        scope.launch(Dispatchers.IO) {
                            OsShortcutCardStore.saveGoogleSystemServiceConfig(
                                normalized,
                                defaults = googleSystemServiceDefaults
                            )
                        }
                        Toast.makeText(
                            context,
                            context.getString(R.string.os_google_system_service_toast_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        showGoogleSystemServiceEditor = false
                    }
                )
            }
        ) {
            SheetContentColumn(verticalSpacing = 10.dp) {
                SheetSectionTitle(stringResource(R.string.os_google_system_service_sheet_section_card))
                SheetSectionCard(verticalSpacing = 10.dp) {
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_title)
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.title,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(title = it) },
                            label = stringResource(R.string.os_google_system_service_hint_title),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_subtitle)
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.subtitle,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(subtitle = it) },
                            label = stringResource(R.string.os_google_system_service_hint_subtitle),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                SheetSectionTitle(stringResource(R.string.os_google_system_service_sheet_section_target))
                SheetSectionCard(verticalSpacing = 10.dp) {
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_app_name)
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.appName,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(appName = it) },
                            label = stringResource(R.string.os_google_system_service_hint_app_name),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_package_name),
                        trailing = {
                            GlassIconButton(
                                backdrop = sheetBackdrop,
                                variant = GlassVariant.SheetAction,
                                icon = MiuixIcons.Regular.AddCircle,
                                contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                                width = 40.dp,
                                height = 32.dp,
                                onClick = {
                                    openGoogleSystemServiceSuggestionSheet(ShortcutSuggestionField.PackageName)
                                }
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.packageName,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(packageName = it) },
                            label = stringResource(R.string.os_google_system_service_hint_package_name),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_class_name),
                        trailing = {
                            GlassIconButton(
                                backdrop = sheetBackdrop,
                                variant = GlassVariant.SheetAction,
                                icon = MiuixIcons.Regular.AddCircle,
                                contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                                width = 40.dp,
                                height = 32.dp,
                                onClick = {
                                    openGoogleSystemServiceSuggestionSheet(ShortcutSuggestionField.ClassName)
                                }
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.className,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(className = it) },
                            label = stringResource(R.string.os_google_system_service_hint_class_name),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_intent_action),
                        trailing = {
                            GlassIconButton(
                                backdrop = sheetBackdrop,
                                variant = GlassVariant.SheetAction,
                                icon = MiuixIcons.Regular.AddCircle,
                                contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                                width = 40.dp,
                                height = 32.dp,
                                onClick = {
                                    openGoogleSystemServiceSuggestionSheet(ShortcutSuggestionField.IntentAction)
                                }
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.intentAction,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(intentAction = it) },
                            label = stringResource(R.string.os_google_system_service_hint_intent_action),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_intent_category),
                        trailing = {
                            GlassIconButton(
                                backdrop = sheetBackdrop,
                                variant = GlassVariant.SheetAction,
                                icon = MiuixIcons.Regular.AddCircle,
                                contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                                width = 40.dp,
                                height = 32.dp,
                                onClick = {
                                    openGoogleSystemServiceSuggestionSheet(ShortcutSuggestionField.IntentCategory)
                                }
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.intentCategory,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(intentCategory = it) },
                            label = stringResource(R.string.os_google_system_service_hint_intent_category),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_intent_flags),
                        trailing = {
                            GlassIconButton(
                                backdrop = sheetBackdrop,
                                variant = GlassVariant.SheetAction,
                                icon = MiuixIcons.Regular.AddCircle,
                                contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                                width = 40.dp,
                                height = 32.dp,
                                onClick = {
                                    openGoogleSystemServiceSuggestionSheet(ShortcutSuggestionField.IntentFlags)
                                }
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.intentFlags,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(intentFlags = it) },
                            label = stringResource(R.string.os_google_system_service_hint_intent_flags),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_intent_data),
                        trailing = {
                            GlassIconButton(
                                backdrop = sheetBackdrop,
                                variant = GlassVariant.SheetAction,
                                icon = MiuixIcons.Regular.AddCircle,
                                contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                                width = 40.dp,
                                height = 32.dp,
                                onClick = {
                                    openGoogleSystemServiceSuggestionSheet(ShortcutSuggestionField.IntentUriData)
                                }
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.intentUriData,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(intentUriData = it) },
                            label = stringResource(R.string.os_google_system_service_hint_intent_data),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SheetFieldBlock(
                        title = stringResource(R.string.os_google_system_service_field_intent_mime_type),
                        trailing = {
                            GlassIconButton(
                                backdrop = sheetBackdrop,
                                variant = GlassVariant.SheetAction,
                                icon = MiuixIcons.Regular.AddCircle,
                                contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                                width = 40.dp,
                                height = 32.dp,
                                onClick = {
                                    openGoogleSystemServiceSuggestionSheet(ShortcutSuggestionField.IntentMimeType)
                                }
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = googleSystemServiceDraft.intentMimeType,
                            onValueChange = { googleSystemServiceDraft = googleSystemServiceDraft.copy(intentMimeType = it) },
                            label = stringResource(R.string.os_google_system_service_hint_intent_mime_type),
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetInput,
                            textColor = MiuixTheme.colorScheme.primary,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                SheetDescriptionText(
                    text = stringResource(R.string.os_google_system_service_sheet_desc)
                )
            }
        }
        SnapshotWindowBottomSheet(
            show = showGoogleSystemServiceSuggestionSheet,
            title = when (googleSystemServiceSuggestionTarget) {
                ShortcutSuggestionField.PackageName ->
                    stringResource(R.string.os_google_system_service_field_package_name)
                ShortcutSuggestionField.ClassName ->
                    stringResource(R.string.os_google_system_service_field_class_name)
                ShortcutSuggestionField.IntentAction ->
                    stringResource(R.string.os_google_system_service_field_intent_action)
                ShortcutSuggestionField.IntentCategory ->
                    stringResource(R.string.os_google_system_service_field_intent_category)
                ShortcutSuggestionField.IntentFlags ->
                    stringResource(R.string.os_google_system_service_field_intent_flags)
                ShortcutSuggestionField.IntentUriData ->
                    stringResource(R.string.os_google_system_service_field_intent_data)
                ShortcutSuggestionField.IntentMimeType ->
                    stringResource(R.string.os_google_system_service_field_intent_mime_type)
            },
            onDismissRequest = { showGoogleSystemServiceSuggestionSheet = false },
            startAction = {
                GlassIconButton(
                    backdrop = sheetBackdrop,
                    variant = GlassVariant.Bar,
                    icon = MiuixIcons.Regular.Close,
                    contentDescription = stringResource(R.string.common_close),
                    onClick = { showGoogleSystemServiceSuggestionSheet = false }
                )
            }
        ) {
            val packageSuggestionQuery = googleSystemServicePackageSuggestionQuery.trim()
            val currentPackageName = googleSystemServiceDraft.packageName.trim()
            val filteredInstalledApps = remember(
                googleSystemServicePackageSuggestions,
                packageSuggestionQuery,
                currentPackageName
            ) {
                val filtered = if (packageSuggestionQuery.isBlank()) {
                    googleSystemServicePackageSuggestions
                } else {
                    googleSystemServicePackageSuggestions.filter { app ->
                        app.appName.contains(packageSuggestionQuery, ignoreCase = true) ||
                            app.packageName.contains(packageSuggestionQuery, ignoreCase = true)
                    }
                }
                if (currentPackageName.isBlank()) {
                    filtered
                } else {
                    val selected = filtered.filter {
                        it.packageName.equals(currentPackageName, ignoreCase = true)
                    }
                    val others = filtered.filterNot {
                        it.packageName.equals(currentPackageName, ignoreCase = true)
                    }
                    selected + others
                }
            }
            val classSuggestionQuery = googleSystemServiceClassSuggestionQuery.trim()
            val currentClassName = googleSystemServiceDraft.className.trim()
            val filteredActivityClasses = remember(
                googleSystemServiceClassSuggestions,
                classSuggestionQuery,
                currentClassName
            ) {
                val filtered = if (classSuggestionQuery.isBlank()) {
                    googleSystemServiceClassSuggestions
                } else {
                    googleSystemServiceClassSuggestions.filter { option ->
                        option.className.contains(classSuggestionQuery, ignoreCase = true) ||
                            option.activityName.contains(classSuggestionQuery, ignoreCase = true)
                    }
                }
                if (currentClassName.isBlank()) {
                    filtered
                } else {
                    val selected = filtered.filter { option ->
                        option.className.equals(currentClassName, ignoreCase = true)
                    }
                    val others = filtered.filterNot { option ->
                        option.className.equals(currentClassName, ignoreCase = true)
                    }
                    selected + others
                }
            }
            val currentIntentAction = googleSystemServiceDraft.intentAction.trim()
            val currentCategoryTokensUpper = remember(googleSystemServiceDraft.intentCategory) {
                parseIntentTokenText(googleSystemServiceDraft.intentCategory)
                    .map { it.uppercase(Locale.ROOT) }
                    .toSet()
            }
            val explicitActionRecommendationSelected = currentClassName.isNotBlank() &&
                currentIntentAction == Intent.ACTION_VIEW
            val implicitActionRecommendationSelected = currentClassName.isBlank() &&
                currentIntentAction == Intent.ACTION_MAIN
            val explicitCategoryRecommendationSelected = currentClassName.isNotBlank() &&
                currentCategoryTokensUpper.isEmpty()
            val implicitCategoryRecommendationSelected = currentClassName.isBlank() &&
                currentCategoryTokensUpper == setOf(Intent.CATEGORY_LAUNCHER.uppercase(Locale.ROOT))
            val suggestions = when (googleSystemServiceSuggestionTarget) {
                ShortcutSuggestionField.PackageName -> filteredInstalledApps.map { app ->
                    ShortcutSuggestionItem(
                        label = app.appName,
                        value = app.packageName,
                        summary = app.packageName,
                        relatedAppName = app.appName
                    )
                }

                ShortcutSuggestionField.ClassName -> {
                    val implicitItem = ShortcutSuggestionItem(
                        label = stringResource(R.string.os_google_system_service_class_suggestion_implicit_title),
                        value = "",
                        summary = stringResource(R.string.os_google_system_service_class_suggestion_implicit_summary)
                    )
                    listOf(implicitItem) + filteredActivityClasses.map { option ->
                        ShortcutSuggestionItem(
                            label = option.activityName,
                            value = option.className,
                            summary = option.className,
                            classItemExported = option.isExported
                        )
                    }
                }

                ShortcutSuggestionField.IntentAction -> listOf(
                    ShortcutSuggestionItem(
                        label = stringResource(R.string.os_google_system_service_suggestion_clear),
                        value = "",
                        summary = stringResource(R.string.os_google_system_service_suggestion_action_auto_summary),
                        append = false
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.ACTION_MAIN,
                        value = Intent.ACTION_MAIN,
                        summary = stringResource(R.string.os_google_system_service_suggestion_action_main_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.ACTION_VIEW,
                        value = Intent.ACTION_VIEW,
                        summary = stringResource(R.string.os_google_system_service_suggestion_action_view_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.ACTION_SEND,
                        value = Intent.ACTION_SEND,
                        summary = stringResource(R.string.os_google_system_service_suggestion_action_send_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.ACTION_SEND_MULTIPLE,
                        value = Intent.ACTION_SEND_MULTIPLE,
                        summary = stringResource(R.string.os_google_system_service_suggestion_action_send_multiple_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.ACTION_EDIT,
                        value = Intent.ACTION_EDIT,
                        summary = stringResource(R.string.os_google_system_service_suggestion_action_edit_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.ACTION_PICK,
                        value = Intent.ACTION_PICK,
                        summary = stringResource(R.string.os_google_system_service_suggestion_action_pick_summary)
                    )
                )

                ShortcutSuggestionField.IntentCategory -> listOf(
                    ShortcutSuggestionItem(
                        label = stringResource(R.string.os_google_system_service_suggestion_clear),
                        value = "",
                        summary = stringResource(R.string.os_google_system_service_suggestion_category_clear_summary),
                        append = false
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.CATEGORY_DEFAULT,
                        value = Intent.CATEGORY_DEFAULT,
                        summary = stringResource(R.string.os_google_system_service_suggestion_category_default_summary),
                        append = true
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.CATEGORY_BROWSABLE,
                        value = Intent.CATEGORY_BROWSABLE,
                        summary = stringResource(R.string.os_google_system_service_suggestion_category_browsable_summary),
                        append = true
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.CATEGORY_LAUNCHER,
                        value = Intent.CATEGORY_LAUNCHER,
                        summary = stringResource(R.string.os_google_system_service_suggestion_category_launcher_summary),
                        append = true
                    ),
                    ShortcutSuggestionItem(
                        label = Intent.CATEGORY_INFO,
                        value = Intent.CATEGORY_INFO,
                        summary = stringResource(R.string.os_google_system_service_suggestion_category_info_summary),
                        append = true
                    )
                )

                ShortcutSuggestionField.IntentFlags -> listOf(
                    ShortcutSuggestionItem(
                        label = stringResource(R.string.os_google_system_service_suggestion_clear),
                        value = "",
                        summary = stringResource(R.string.os_google_system_service_suggestion_flags_auto_summary),
                        append = false
                    ),
                    ShortcutSuggestionItem(
                        label = "FLAG_ACTIVITY_NEW_TASK",
                        value = "FLAG_ACTIVITY_NEW_TASK",
                        summary = stringResource(R.string.os_google_system_service_suggestion_flags_new_task_summary),
                        append = true
                    ),
                    ShortcutSuggestionItem(
                        label = "FLAG_ACTIVITY_CLEAR_TOP",
                        value = "FLAG_ACTIVITY_CLEAR_TOP",
                        summary = stringResource(R.string.os_google_system_service_suggestion_flags_clear_top_summary),
                        append = true
                    ),
                    ShortcutSuggestionItem(
                        label = "FLAG_ACTIVITY_SINGLE_TOP",
                        value = "FLAG_ACTIVITY_SINGLE_TOP",
                        summary = stringResource(R.string.os_google_system_service_suggestion_flags_single_top_summary),
                        append = true
                    ),
                    ShortcutSuggestionItem(
                        label = "FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TOP",
                        value = "FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TOP",
                        summary = stringResource(R.string.os_google_system_service_suggestion_flags_combo_summary),
                        append = true
                    )
                )

                ShortcutSuggestionField.IntentUriData -> listOf(
                    ShortcutSuggestionItem(
                        label = stringResource(R.string.os_google_system_service_suggestion_clear),
                        value = "",
                        summary = stringResource(R.string.os_google_system_service_suggestion_uri_clear_summary),
                        append = false
                    ),
                    ShortcutSuggestionItem(
                        label = "market://details?id=com.android.vending",
                        value = "market://details?id=com.android.vending",
                        summary = stringResource(R.string.os_google_system_service_suggestion_uri_market_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = "https://play.google.com/store/apps/details?id=com.android.vending",
                        value = "https://play.google.com/store/apps/details?id=com.android.vending",
                        summary = stringResource(R.string.os_google_system_service_suggestion_uri_https_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = "package:com.android.vending",
                        value = "package:com.android.vending",
                        summary = stringResource(R.string.os_google_system_service_suggestion_uri_package_summary)
                    )
                )

                ShortcutSuggestionField.IntentMimeType -> listOf(
                    ShortcutSuggestionItem(
                        label = stringResource(R.string.os_google_system_service_suggestion_clear),
                        value = "",
                        summary = stringResource(R.string.os_google_system_service_suggestion_mime_clear_summary),
                        append = false
                    ),
                    ShortcutSuggestionItem(
                        label = "text/plain",
                        value = "text/plain",
                        summary = stringResource(R.string.os_google_system_service_suggestion_mime_text_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = "application/vnd.android.package-archive",
                        value = "application/vnd.android.package-archive",
                        summary = stringResource(R.string.os_google_system_service_suggestion_mime_apk_summary)
                    ),
                    ShortcutSuggestionItem(
                        label = "*/*",
                        value = "*/*",
                        summary = stringResource(R.string.os_google_system_service_suggestion_mime_any_summary)
                    )
                )
            }
            val currentValue = currentGoogleSystemServiceSuggestionFieldValue(googleSystemServiceSuggestionTarget)
            val isCurrentSuggestionSelected: (ShortcutSuggestionItem) -> Boolean = { suggestion ->
                when (googleSystemServiceSuggestionTarget) {
                    ShortcutSuggestionField.PackageName -> {
                        currentValue.trim().equals(suggestion.value.trim(), ignoreCase = true)
                    }

                    ShortcutSuggestionField.ClassName -> {
                        currentValue.trim().equals(suggestion.value.trim(), ignoreCase = true)
                    }

                    ShortcutSuggestionField.IntentAction,
                    ShortcutSuggestionField.IntentUriData,
                    ShortcutSuggestionField.IntentMimeType -> {
                        currentValue.trim() == suggestion.value.trim()
                    }

                    ShortcutSuggestionField.IntentCategory,
                    ShortcutSuggestionField.IntentFlags -> {
                        val currentTokens = parseIntentTokenText(currentValue)
                            .map { it.uppercase(Locale.ROOT) }
                            .toSet()
                        val suggestionTokens = parseIntentTokenText(suggestion.value)
                            .map { it.uppercase(Locale.ROOT) }
                            .toSet()
                        if (suggestionTokens.isEmpty()) {
                            currentTokens.isEmpty()
                        } else {
                            currentTokens.containsAll(suggestionTokens)
                        }
                    }
                }
            }
            val orderedSuggestions = when (googleSystemServiceSuggestionTarget) {
                ShortcutSuggestionField.ClassName -> suggestions
                else -> {
                    val selected = suggestions.filter(isCurrentSuggestionSelected)
                    val others = suggestions.filterNot(isCurrentSuggestionSelected)
                    selected + others
                }
            }
            SheetContentColumn(verticalSpacing = 10.dp) {
                if (googleSystemServiceSuggestionTarget == ShortcutSuggestionField.PackageName) {
                    GlassSearchField(
                        value = googleSystemServicePackageSuggestionQuery,
                        onValueChange = { googleSystemServicePackageSuggestionQuery = it },
                        label = stringResource(R.string.os_google_system_service_hint_package_suggestion_search),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (googleSystemServiceSuggestionTarget == ShortcutSuggestionField.ClassName) {
                    GlassSearchField(
                        value = googleSystemServiceClassSuggestionQuery,
                        onValueChange = { googleSystemServiceClassSuggestionQuery = it },
                        label = stringResource(R.string.os_google_system_service_hint_class_suggestion_search),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (googleSystemServiceSuggestionTarget == ShortcutSuggestionField.IntentAction) {
                    SheetSectionTitle(
                        text = stringResource(R.string.os_google_system_service_recommend_section)
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_google_system_service_action_recommend_explicit_title),
                        summary = stringResource(R.string.os_google_system_service_action_recommend_explicit_summary),
                        selected = explicitActionRecommendationSelected,
                        onSelect = {
                            googleSystemServiceDraft = googleSystemServiceDraft.copy(
                                intentAction = Intent.ACTION_VIEW
                            )
                        },
                        accentColor = Color(0xFF16A34A),
                        selectedAccentColor = MiuixTheme.colorScheme.primary,
                        unselectedTitleColor = Color(0xFF16A34A),
                        summaryColor = Color(0xFF16A34A),
                        selectedLabel = null
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_google_system_service_action_recommend_implicit_title),
                        summary = stringResource(R.string.os_google_system_service_action_recommend_implicit_summary),
                        selected = implicitActionRecommendationSelected,
                        onSelect = {
                            googleSystemServiceDraft = applyShortcutImplicitDefaults(googleSystemServiceDraft)
                        },
                        accentColor = Color(0xFF16A34A),
                        selectedAccentColor = MiuixTheme.colorScheme.primary,
                        unselectedTitleColor = Color(0xFF16A34A),
                        summaryColor = Color(0xFF16A34A),
                        selectedLabel = null
                    )
                }
                if (googleSystemServiceSuggestionTarget == ShortcutSuggestionField.IntentCategory) {
                    SheetSectionTitle(
                        text = stringResource(R.string.os_google_system_service_recommend_section)
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_google_system_service_category_recommend_explicit_title),
                        summary = stringResource(R.string.os_google_system_service_category_recommend_explicit_summary),
                        selected = explicitCategoryRecommendationSelected,
                        onSelect = {
                            googleSystemServiceDraft = googleSystemServiceDraft.copy(
                                intentCategory = ""
                            )
                        },
                        accentColor = Color(0xFF16A34A),
                        selectedAccentColor = MiuixTheme.colorScheme.primary,
                        unselectedTitleColor = Color(0xFF16A34A),
                        summaryColor = Color(0xFF16A34A),
                        selectedLabel = null
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_google_system_service_category_recommend_implicit_title),
                        summary = stringResource(R.string.os_google_system_service_category_recommend_implicit_summary),
                        selected = implicitCategoryRecommendationSelected,
                        onSelect = {
                            googleSystemServiceDraft = applyShortcutImplicitDefaults(googleSystemServiceDraft)
                        },
                        accentColor = Color(0xFF16A34A),
                        selectedAccentColor = MiuixTheme.colorScheme.primary,
                        unselectedTitleColor = Color(0xFF16A34A),
                        summaryColor = Color(0xFF16A34A),
                        selectedLabel = null
                    )
                }
                if (
                    googleSystemServiceSuggestionTarget == ShortcutSuggestionField.PackageName &&
                    googleSystemServicePackageSuggestionsLoading
                ) {
                    Text(
                        text = stringResource(R.string.common_loading),
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
                if (
                    googleSystemServiceSuggestionTarget == ShortcutSuggestionField.ClassName &&
                    googleSystemServiceClassSuggestionsLoading
                ) {
                    Text(
                        text = stringResource(R.string.common_loading),
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
                orderedSuggestions.forEach { suggestion ->
                    val selected = isCurrentSuggestionSelected(suggestion)
                    val leading = if (googleSystemServiceSuggestionTarget == ShortcutSuggestionField.PackageName) {
                        @Composable {
                            AppIcon(
                                packageName = suggestion.value.trim(),
                                size = 24.dp
                            )
                        }
                    } else {
                        null
                    }
                    SheetChoiceCard(
                        title = suggestion.label,
                        summary = suggestion.summary,
                        selected = selected,
                        onSelect = { applyGoogleSystemServiceSuggestion(suggestion) },
                        selectedAccentColor = MiuixTheme.colorScheme.primary,
                        unselectedTitleColor = when (googleSystemServiceSuggestionTarget) {
                            ShortcutSuggestionField.ClassName -> {
                                if (suggestion.value.trim().isBlank() || suggestion.classItemExported) {
                                    Color(0xFFDC2626)
                                } else {
                                    Color(0xFF16A34A)
                                }
                            }
                            else -> MiuixTheme.colorScheme.onBackground
                        },
                        summaryColor = when (googleSystemServiceSuggestionTarget) {
                            ShortcutSuggestionField.ClassName -> {
                                if (suggestion.value.trim().isBlank() || suggestion.classItemExported) {
                                    Color(0xFFDC2626)
                                } else {
                                    Color(0xFF16A34A)
                                }
                            }
                            else -> MiuixTheme.colorScheme.onBackgroundVariant
                        },
                        selectedLabel = null,
                        leading = leading
                    )
                }
                if (
                    googleSystemServiceSuggestionTarget == ShortcutSuggestionField.PackageName &&
                    !googleSystemServicePackageSuggestionsLoading &&
                    suggestions.isEmpty()
                ) {
                    Text(
                        text = noMatchedResultsText,
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
                if (
                    googleSystemServiceSuggestionTarget == ShortcutSuggestionField.ClassName &&
                    !googleSystemServiceClassSuggestionsLoading &&
                    suggestions.size <= 1
                ) {
                    Text(
                        text = noMatchedResultsText,
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
                SheetDescriptionText(
                    text = stringResource(R.string.os_google_system_service_suggestion_sheet_desc)
                )
            }
        }
        AppPageLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(searchBarScrollConnection)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            innerPadding = innerPadding,
            topExtra = 0.dp,
            sectionSpacing = 0.dp
        ) {
            item {
                AppOverviewCard(
                    title = stringResource(R.string.os_overview_title),
                    containerColor = overviewCardColor,
                    borderColor = overviewBorderColor,
                    contentColor = titleColor,
                    showIndication = cardPressFeedbackEnabled,
                    onClick = {
                        if (refreshing) return@AppOverviewCard
                        scope.launch { refreshAllSections() }
                    },
                    headerEndActions = {
                        if (overviewState != SystemOverviewState.Idle) {
                            CircularProgressIndicator(
                                progress = indicatorProgress,
                                size = 16.dp,
                                strokeWidth = 2.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = statusColor,
                                    backgroundColor = indicatorBg
                                )
                            )
                        }
                        StatusPill(
                            label = statusLabel,
                            color = statusColor,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            backgroundAlphaOverride = if (isDark) 0.24f else 0.34f,
                            borderAlphaOverride = if (isDark) 0.42f else 0.52f
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
                    ) {
                        overviewMetrics.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricRowGap)
                            ) {
                                GitHubOverviewMetricItem(
                                    label = pair[0].label,
                                    value = pair[0].value,
                                    titleColor = if (isDark) Color.White else MiuixTheme.colorScheme.onBackgroundVariant,
                                    valueColor = pair[0].valueColor ?: MiuixTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                if (pair.size > 1) {
                                    GitHubOverviewMetricItem(
                                        label = pair[1].label,
                                        value = pair[1].value,
                                        titleColor = if (isDark) Color.White else MiuixTheme.colorScheme.onBackgroundVariant,
                                        valueColor = pair[1].valueColor ?: MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(AppChromeTokens.pageSectionGap)) }

            if (isCardVisible(OsSectionCard.TOP_INFO)) {
                item {
                MiuixAccordionCard(
                    backdrop = contentBackdrop,
                    title = stringResource(R.string.os_section_top_info_title),
                    subtitle = stringResource(R.string.common_item_count, displayedTopInfoRows.size),
                    expanded = topInfoExpanded,
                    onExpandedChange = { topInfoExpanded = it },
                    headerStartAction = {
                        OsSectionHeaderIcon(card = OsSectionCard.TOP_INFO)
                    },
                    headerActions = {
                        CardExportAction(card = OsSectionCard.TOP_INFO)
                    }
                ) {
                    if (displayedTopInfoRows.isEmpty()) {
                        Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    } else {
                        if (q.isBlank() && !topInfoExpanded) {
                            displayedTopInfoRows.forEach { row ->
                                OsSectionInfoRow(label = row.key, value = row.value)
                            }
                        } else {
                            groupedTopInfoRows.forEachIndexed { index, (type, rows) ->
                                Text(
                                    text = type,
                                    color = MiuixTheme.colorScheme.onBackground,
                                    fontSize = AppTypographyTokens.CompactTitle.fontSize,
                                    lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                                    fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp, bottom = 2.dp)
                                )
                                rows.forEach { row ->
                                    OsSectionInfoRow(label = row.key, value = row.value)
                                }
                            }
                        }
                    }
                }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (isCardVisible(OsSectionCard.GOOGLE_SYSTEM_SERVICE)) {
                item {
                    val shortcutConfig = googleSystemServiceConfig.normalized(googleSystemServiceDefaults)
                    MiuixAccordionCard(
                        backdrop = contentBackdrop,
                        title = shortcutConfig.title,
                        subtitle = shortcutConfig.subtitle,
                        expanded = googleSystemServiceExpanded,
                        onExpandedChange = { googleSystemServiceExpanded = it },
                        headerStartAction = {
                            AppIcon(packageName = shortcutConfig.packageName, size = 24.dp)
                        },
                        headerActions = {
                            Icon(
                                imageVector = MiuixIcons.Regular.Play,
                                contentDescription = stringResource(R.string.os_google_system_service_cd_open_activity),
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    openGoogleSystemServiceActivity()
                                }
                            )
                        },
                        onHeaderLongClick = {
                            googleSystemServiceDraft = shortcutConfig
                            showGoogleSystemServiceEditor = true
                        }
                    ) {
                        OsSectionInfoRow(
                            label = stringResource(R.string.os_google_system_service_label_app_name),
                            value = shortcutConfig.appName
                        )
                        OsSectionInfoRow(
                            label = stringResource(R.string.os_google_system_service_label_package_name),
                            value = shortcutConfig.packageName
                        )
                        OsSectionInfoRow(
                            label = stringResource(R.string.os_google_system_service_label_class_name),
                            value = shortcutConfig.className
                        )
                        OsSectionInfoRow(
                            label = stringResource(R.string.os_google_system_service_label_intent_action),
                            value = shortcutConfig.intentAction
                        )
                        OsSectionInfoRow(
                            label = stringResource(R.string.os_google_system_service_label_intent_category),
                            value = shortcutConfig.intentCategory.ifBlank {
                                stringResource(R.string.os_google_system_service_value_data_empty)
                            }
                        )
                        OsSectionInfoRow(
                            label = stringResource(R.string.os_google_system_service_label_intent_flags),
                            value = shortcutConfig.intentFlags.ifBlank {
                                stringResource(R.string.os_google_system_service_value_data_empty)
                            }
                        )
                        OsSectionInfoRow(
                            label = stringResource(R.string.os_google_system_service_label_intent_data),
                            value = shortcutConfig.intentUriData.ifBlank {
                                stringResource(R.string.os_google_system_service_value_data_empty)
                            }
                        )
                        OsSectionInfoRow(
                            label = stringResource(R.string.os_google_system_service_label_intent_mime_type),
                            value = shortcutConfig.intentMimeType.ifBlank {
                                stringResource(R.string.os_google_system_service_value_data_empty)
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (isCardVisible(OsSectionCard.SYSTEM)) {
                item {
                MiuixAccordionCard(
                    backdrop = contentBackdrop,
                    title = stringResource(R.string.os_section_system_title),
                    subtitle = sectionSubtitle(SectionKind.SYSTEM, if (q.isBlank()) prunedSystemRows.size else displayedSystemRows.size),
                    expanded = systemTableExpanded,
                    onExpandedChange = { systemTableExpanded = it },
                    headerStartAction = {
                        OsSectionHeaderIcon(card = OsSectionCard.SYSTEM)
                    },
                    headerActions = {
                        CardExportAction(card = OsSectionCard.SYSTEM)
                    }
                ) {
                    if (displayedSystemRows.isEmpty()) Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedSystemRows.forEach { row -> OsSectionInfoRow(label = row.key, value = row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (isCardVisible(OsSectionCard.SECURE)) {
                item {
                MiuixAccordionCard(
                    backdrop = contentBackdrop,
                    title = stringResource(R.string.os_section_secure_title),
                    subtitle = sectionSubtitle(SectionKind.SECURE, if (q.isBlank()) prunedSecureRows.size else displayedSecureRows.size),
                    expanded = secureTableExpanded,
                    onExpandedChange = { secureTableExpanded = it },
                    headerStartAction = {
                        OsSectionHeaderIcon(card = OsSectionCard.SECURE)
                    },
                    headerActions = {
                        CardExportAction(card = OsSectionCard.SECURE)
                    }
                ) {
                    if (displayedSecureRows.isEmpty()) Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedSecureRows.forEach { row -> OsSectionInfoRow(label = row.key, value = row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (isCardVisible(OsSectionCard.GLOBAL)) {
                item {
                MiuixAccordionCard(
                    backdrop = contentBackdrop,
                    title = stringResource(R.string.os_section_global_title),
                    subtitle = sectionSubtitle(SectionKind.GLOBAL, if (q.isBlank()) prunedGlobalRows.size else displayedGlobalRows.size),
                    expanded = globalTableExpanded,
                    onExpandedChange = { globalTableExpanded = it },
                    headerStartAction = {
                        OsSectionHeaderIcon(card = OsSectionCard.GLOBAL)
                    },
                    headerActions = {
                        CardExportAction(card = OsSectionCard.GLOBAL)
                    }
                ) {
                    if (displayedGlobalRows.isEmpty()) Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedGlobalRows.forEach { row -> OsSectionInfoRow(label = row.key, value = row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (isCardVisible(OsSectionCard.ANDROID)) {
                item {
                MiuixAccordionCard(
                    backdrop = contentBackdrop,
                    title = stringResource(R.string.os_section_android_title),
                    subtitle = sectionSubtitle(SectionKind.ANDROID, if (q.isBlank()) prunedAndroidRows.size else displayedAndroidRows.size),
                    expanded = androidPropsExpanded,
                    onExpandedChange = { androidPropsExpanded = it },
                    headerStartAction = {
                        OsSectionHeaderIcon(card = OsSectionCard.ANDROID)
                    },
                    headerActions = {
                        CardExportAction(card = OsSectionCard.ANDROID)
                    }
                ) {
                    if (displayedAndroidRows.isEmpty()) Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedAndroidRows.forEach { row -> OsSectionInfoRow(label = row.key, value = row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (isCardVisible(OsSectionCard.JAVA)) {
                item {
                MiuixAccordionCard(
                    backdrop = contentBackdrop,
                    title = stringResource(R.string.os_section_java_title),
                    subtitle = sectionSubtitle(SectionKind.JAVA, if (q.isBlank()) prunedJavaRows.size else displayedJavaRows.size),
                    expanded = javaPropsExpanded,
                    onExpandedChange = { javaPropsExpanded = it },
                    headerStartAction = {
                        OsSectionHeaderIcon(card = OsSectionCard.JAVA)
                    },
                    headerActions = {
                        CardExportAction(card = OsSectionCard.JAVA)
                    }
                ) {
                    if (displayedJavaRows.isEmpty()) Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedJavaRows.forEach { row -> OsSectionInfoRow(label = row.key, value = row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (isCardVisible(OsSectionCard.LINUX)) {
                item {
                MiuixAccordionCard(
                    backdrop = contentBackdrop,
                    title = stringResource(R.string.os_section_linux_title),
                    subtitle = sectionSubtitle(SectionKind.LINUX, if (q.isBlank()) prunedLinuxRows.size else displayedLinuxRows.size),
                    expanded = linuxEnvExpanded,
                    onExpandedChange = { linuxEnvExpanded = it },
                    headerStartAction = {
                        OsSectionHeaderIcon(card = OsSectionCard.LINUX)
                    },
                    headerActions = {
                        CardExportAction(card = OsSectionCard.LINUX)
                    }
                ) {
                    if (displayedLinuxRows.isEmpty()) Text(text = noMatchedResultsText, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedLinuxRows.forEach { row -> OsSectionInfoRow(label = row.key, value = row.value) }
                }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

private data class OsOverviewMetric(
    val label: String,
    val value: String,
    val valueColor: Color? = null
)

private fun sectionCardIcon(card: OsSectionCard) = when (card) {
    OsSectionCard.TOP_INFO -> MiuixIcons.Regular.Info
    OsSectionCard.GOOGLE_SYSTEM_SERVICE -> MiuixIcons.Regular.Update
    OsSectionCard.SYSTEM -> MiuixIcons.Regular.ListView
    OsSectionCard.SECURE -> MiuixIcons.Regular.Lock
    OsSectionCard.GLOBAL -> MiuixIcons.Regular.Layers
    OsSectionCard.ANDROID -> MiuixIcons.Regular.GridView
    OsSectionCard.JAVA -> MiuixIcons.Regular.Tune
    OsSectionCard.LINUX -> MiuixIcons.Regular.Filter
}

@Composable
private fun OsSectionHeaderIcon(card: OsSectionCard, modifier: Modifier = Modifier) {
    Icon(
        imageVector = sectionCardIcon(card),
        contentDescription = card.title,
        tint = MiuixTheme.colorScheme.primary,
        modifier = modifier
            .size(22.dp)
            .defaultMinSize(minHeight = 22.dp)
    )
}

@Composable
private fun OsSectionInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    AppInfoRow(
        label = label,
        value = value.ifBlank { "N/A" },
        modifier = modifier,
        labelColor = MiuixTheme.colorScheme.onBackgroundVariant,
        valueColor = MiuixTheme.colorScheme.onBackground,
        labelMinWidth = 72.dp,
        labelMaxWidth = 136.dp,
        horizontalSpacing = CardLayoutRhythm.infoRowGap,
        rowVerticalPadding = CardLayoutRhythm.infoRowVerticalPadding,
        valueTextAlign = TextAlign.End,
        labelMaxLines = Int.MAX_VALUE,
        valueMaxLines = 6,
        valueOverflow = TextOverflow.Ellipsis,
        labelFontSize = AppTypographyTokens.Body.fontSize,
        labelLineHeight = AppTypographyTokens.Body.lineHeight,
        valueFontSize = AppTypographyTokens.Body.fontSize,
        valueLineHeight = AppTypographyTokens.Body.lineHeight,
        emphasizedValue = true
    )
}

private data class OsActivityVisibilityItem(
    val card: OsSectionCard,
    val title: String,
    val packageName: String
)

private enum class ShortcutSuggestionField {
    PackageName,
    ClassName,
    IntentAction,
    IntentCategory,
    IntentFlags,
    IntentUriData,
    IntentMimeType
}

private data class ShortcutSuggestionItem(
    val label: String,
    val value: String,
    val summary: String,
    val append: Boolean = false,
    val relatedAppName: String = "",
    val classItemExported: Boolean = false
)

private data class ShortcutInstalledAppOption(
    val appName: String,
    val packageName: String
)

private data class ShortcutActivityClassOption(
    val className: String,
    val activityName: String,
    val isExported: Boolean
)

private fun loadInstalledAppOptions(context: Context): List<ShortcutInstalledAppOption> {
    val pm = context.packageManager
    val packageInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)
    }
    val overlayFlagMask = runCatching {
        ApplicationInfo::class.java.getField("FLAG_IS_RESOURCE_OVERLAY").getInt(null)
    }.getOrDefault(0)
    return packageInfos.mapNotNull { info ->
        val packageName = info.packageName.trim()
        if (packageName.isBlank()) return@mapNotNull null
        val appInfo = info.applicationInfo ?: runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                pm.getApplicationInfo(packageName, 0)
            }
        }.getOrNull() ?: return@mapNotNull null
        if (
            shouldIgnoreInstalledAppForShortcut(
                packageName = packageName,
                appInfo = appInfo,
                hasAnyEnabledActivity = info.activities?.any { it.enabled } == true,
                overlayFlagMask = overlayFlagMask
            )
        ) {
            return@mapNotNull null
        }

        val appName = runCatching {
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName).trim().ifBlank { packageName }

        ShortcutInstalledAppOption(
            appName = appName,
            packageName = packageName
        )
    }.distinctBy { it.packageName }
        .sortedWith(
            compareBy<ShortcutInstalledAppOption> { it.appName.lowercase(Locale.ROOT) }
                .thenBy { it.packageName.lowercase(Locale.ROOT) }
        )
}

private fun loadActivityClassOptions(
    context: Context,
    packageName: String
): List<ShortcutActivityClassOption> {
    val normalizedPackageName = packageName.trim()
    if (normalizedPackageName.isBlank()) return emptyList()
    val pm = context.packageManager
    val packageInfo = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                normalizedPackageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(normalizedPackageName, PackageManager.GET_ACTIVITIES)
        }
    }.getOrNull() ?: return emptyList()

    return packageInfo.activities.orEmpty()
        .asSequence()
        .filter { it.enabled || it.exported }
        .mapNotNull { info ->
            val raw = info.name.trim()
            if (raw.isBlank()) return@mapNotNull null
            val normalized = if (raw.startsWith(".")) {
                "$normalizedPackageName$raw"
            } else {
                raw
            }
            val activityName = runCatching {
                info.loadLabel(pm).toString()
            }.getOrNull()?.trim().orEmpty().ifBlank {
                normalized.substringAfterLast('.')
            }
            ShortcutActivityClassOption(
                className = normalized,
                activityName = activityName,
                isExported = info.exported
            )
        }
        .groupBy { it.className }
        .values
        .mapNotNull { options ->
            options.maxWithOrNull(
                compareBy<ShortcutActivityClassOption> { it.isExported }
                    .thenBy { it.activityName.length }
            )
        }
        .sortedWith(
            compareBy<ShortcutActivityClassOption> { it.activityName.lowercase(Locale.ROOT) }
                .thenBy { it.className.lowercase(Locale.ROOT) }
        )
        .toList()
}

private fun shouldIgnoreInstalledAppForShortcut(
    packageName: String,
    appInfo: ApplicationInfo,
    hasAnyEnabledActivity: Boolean,
    overlayFlagMask: Int
): Boolean {
    if (!hasAnyEnabledActivity) return true
    if (!appInfo.enabled) return true
    if ((appInfo.flags and ApplicationInfo.FLAG_INSTALLED) == 0) return true
    if ((appInfo.flags and ApplicationInfo.FLAG_HAS_CODE) == 0) return true
    if ((appInfo.flags and ApplicationInfo.FLAG_TEST_ONLY) != 0) return true
    if (overlayFlagMask != 0 && (appInfo.flags and overlayFlagMask) != 0) return true
    val normalizedPackageName = packageName.lowercase(Locale.ROOT)
    if (normalizedPackageName.contains(".overlay")) return true
    if (normalizedPackageName.startsWith("overlay.")) return true
    return false
}

private fun parseIntentCategories(raw: String): List<String> {
    return parseIntentTokenText(raw)
}

private fun parseIntentTokenText(raw: String): List<String> {
    return raw.split(',', ';', '|', '\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun mergeIntentTokenText(
    current: String,
    incoming: String,
    append: Boolean
): String {
    val normalizedIncoming = incoming.trim()
    if (!append) return normalizedIncoming
    if (normalizedIncoming.isBlank()) return ""

    val mergedTokens = linkedSetOf<String>()
    parseIntentTokenText(current).forEach { mergedTokens.add(it) }
    parseIntentTokenText(normalizedIncoming).forEach { token ->
        val duplicated = mergedTokens.any { it.equals(token, ignoreCase = true) }
        if (!duplicated) {
            mergedTokens.add(token)
        }
    }
    return mergedTokens.joinToString(", ")
}

private fun parseIntentFlags(raw: String): Int {
    if (raw.isBlank()) return 0
    return raw.split(',', ';', '|', '\n', ' ')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .fold(0) { current, token ->
            current or parseSingleIntentFlag(token)
        }
}

private fun parseSingleIntentFlag(token: String): Int {
    val normalized = token.trim()
    if (normalized.isBlank()) return 0
    normalized.toIntOrNull()?.let { return it }
    normalized.removePrefix("0x").removePrefix("0X")
        .toLongOrNull(16)
        ?.toInt()
        ?.let { return it }

    val compact = normalized
        .removePrefix("Intent.")
        .removePrefix("android.content.Intent.")
        .uppercase(Locale.ROOT)
    return intentFlagNameValues[compact]
        ?: intentFlagNameValues["FLAG_ACTIVITY_$compact"]
        ?: 0
}

private fun Int.ifBlankUse(fallback: Int): Int {
    return if (this == 0) fallback else this
}

private val intentFlagNameValues: Map<String, Int> = mapOf(
    "FLAG_ACTIVITY_NEW_TASK" to Intent.FLAG_ACTIVITY_NEW_TASK,
    "FLAG_ACTIVITY_SINGLE_TOP" to Intent.FLAG_ACTIVITY_SINGLE_TOP,
    "FLAG_ACTIVITY_CLEAR_TOP" to Intent.FLAG_ACTIVITY_CLEAR_TOP,
    "FLAG_ACTIVITY_FORWARD_RESULT" to Intent.FLAG_ACTIVITY_FORWARD_RESULT,
    "FLAG_ACTIVITY_PREVIOUS_IS_TOP" to Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP,
    "FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS" to Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
    "FLAG_ACTIVITY_BROUGHT_TO_FRONT" to Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT,
    "FLAG_ACTIVITY_RESET_TASK_IF_NEEDED" to Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
    "FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY" to Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
    "FLAG_ACTIVITY_MULTIPLE_TASK" to Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
    "FLAG_ACTIVITY_NO_USER_ACTION" to Intent.FLAG_ACTIVITY_NO_USER_ACTION,
    "FLAG_ACTIVITY_REORDER_TO_FRONT" to Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
    "FLAG_ACTIVITY_NO_ANIMATION" to Intent.FLAG_ACTIVITY_NO_ANIMATION,
    "FLAG_ACTIVITY_CLEAR_TASK" to Intent.FLAG_ACTIVITY_CLEAR_TASK,
    "FLAG_ACTIVITY_TASK_ON_HOME" to Intent.FLAG_ACTIVITY_TASK_ON_HOME,
    "FLAG_ACTIVITY_RETAIN_IN_RECENTS" to Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS,
    "FLAG_ACTIVITY_REQUIRE_NON_BROWSER" to Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER,
    "FLAG_ACTIVITY_REQUIRE_DEFAULT" to Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT,
    "FLAG_ACTIVITY_MATCH_EXTERNAL" to Intent.FLAG_ACTIVITY_MATCH_EXTERNAL
)
