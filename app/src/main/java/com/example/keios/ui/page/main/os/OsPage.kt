package com.example.keios.ui.page.main

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.AppChromeTokens
import com.example.keios.ui.page.main.widget.AppPageScaffold
import com.example.keios.ui.page.main.widget.AppTopBarSearchField
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.StatusLabelText
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.ui.page.main.os.OsPageViewModel
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Refresh
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
    val editActivityCardTitle = stringResource(R.string.os_activity_sheet_title_edit)
    val addActivityCardTitle = stringResource(R.string.os_activity_sheet_title_add)
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
    var shellRunnerExpanded by remember { mutableStateOf(initialUiSnapshot.shellRunnerExpanded) }
    var systemTableExpanded by remember { mutableStateOf(initialUiSnapshot.systemTableExpanded) }
    var secureTableExpanded by remember { mutableStateOf(initialUiSnapshot.secureTableExpanded) }
    var globalTableExpanded by remember { mutableStateOf(initialUiSnapshot.globalTableExpanded) }
    var androidPropsExpanded by remember { mutableStateOf(initialUiSnapshot.androidPropsExpanded) }
    var javaPropsExpanded by remember { mutableStateOf(initialUiSnapshot.javaPropsExpanded) }
    var linuxEnvExpanded by remember { mutableStateOf(initialUiSnapshot.linuxEnvExpanded) }
    var visibleCards by remember { mutableStateOf(initialUiSnapshot.visibleCards) }
    var activityShortcutCards by remember {
        mutableStateOf(
            OsActivityShortcutCardStore.loadCards(
                defaults = googleSystemServiceDefaults
            )
        )
    }
    val activityCardExpanded = remember { mutableStateMapOf<String, Boolean>() }
    var activityShortcutDraft by remember {
        mutableStateOf(createDefaultActivityShortcutDraft(googleSystemServiceDefaults))
    }
    var showActivityShortcutEditor by rememberSaveable { mutableStateOf(false) }
    var showActivitySuggestionSheet by rememberSaveable { mutableStateOf(false) }
    var activityCardEditMode by rememberSaveable { mutableStateOf(OsActivityCardEditMode.Edit) }
    var editingActivityShortcutCardId by rememberSaveable { mutableStateOf<String?>(null) }
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
    fun updateSection(section: SectionKind, transform: (SectionState) -> SectionState) {
        sectionStates = sectionStates.toMutableMap().also { map ->
            val old = map[section] ?: SectionState()
            map[section] = transform(old)
        }
    }
    suspend fun ensureLoad(section: SectionKind, forceRefresh: Boolean = false) {
        if (!visibleSectionKinds(visibleCards).contains(section)) return
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
            OsInfoCache.readSnapshot(visibleSectionKinds(visibleCards)).hasPersistedCache
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
            OsSectionCard.SHELL_RUNNER -> {
                if (!visible) shellRunnerExpanded = false
            }
            OsSectionCard.GOOGLE_SYSTEM_SERVICE -> Unit
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
            OsInfoCache.readSnapshot(visibleSectionKinds(visibleCards)).hasPersistedCache
        }
    }

    suspend fun applyActivityCardVisibility(cardId: String, visible: Boolean) {
        val updatedCards = activityShortcutCards.map { card ->
            if (card.id == cardId) card.copy(visible = visible) else card
        }
        activityShortcutCards = updatedCards
        withContext(Dispatchers.IO) {
            OsActivityShortcutCardStore.saveCards(
                cards = updatedCards,
                defaults = googleSystemServiceDefaults
            )
        }
    }

    suspend fun refreshAllSections() {
        refreshing = true
        refreshProgress = 0f
        try {
            val targets = SectionKind.entries.filter { visibleSectionKinds(visibleCards).contains(it) }
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

    LaunchedEffect(activityShortcutCards) {
        val currentIds = activityShortcutCards.map { it.id }.toSet()
        activityCardExpanded.keys.toList().forEach { id ->
            if (!currentIds.contains(id)) {
                activityCardExpanded.remove(id)
            }
        }
        activityShortcutCards.forEachIndexed { index, card ->
            if (!activityCardExpanded.containsKey(card.id)) {
                activityCardExpanded[card.id] =
                    if (index == 0 && card.id == LEGACY_GOOGLE_SYSTEM_SERVICE_CARD_ID) {
                        initialUiSnapshot.googleSystemServiceExpanded
                    } else {
                        false
                    }
            }
        }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(Unit) {
        if (!visibleCards.contains(OsSectionCard.GOOGLE_SYSTEM_SERVICE)) {
            val ensuredVisibleCards = visibleCards + OsSectionCard.GOOGLE_SYSTEM_SERVICE
            visibleCards = ensuredVisibleCards
            withContext(Dispatchers.IO) {
                OsCardVisibilityStore.saveVisibleCards(ensuredVisibleCards)
            }
        }
        val visibleSections = visibleSectionKinds(visibleCards)
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
    LaunchedEffect(shellRunnerExpanded) {
        if (!uiStatePersistenceReady) return@LaunchedEffect
        withContext(Dispatchers.IO) { OsUiStateStore.setShellRunnerExpanded(shellRunnerExpanded) }
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
        if (isPageActive && systemTableExpanded && isCardVisible(visibleCards, OsSectionCard.SYSTEM)) ensureLoad(SectionKind.SYSTEM)
    }
    LaunchedEffect(secureTableExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && secureTableExpanded && isCardVisible(visibleCards, OsSectionCard.SECURE)) ensureLoad(SectionKind.SECURE)
    }
    LaunchedEffect(globalTableExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && globalTableExpanded && isCardVisible(visibleCards, OsSectionCard.GLOBAL)) ensureLoad(SectionKind.GLOBAL)
    }
    LaunchedEffect(androidPropsExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && androidPropsExpanded && isCardVisible(visibleCards, OsSectionCard.ANDROID)) ensureLoad(SectionKind.ANDROID)
    }
    LaunchedEffect(javaPropsExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && javaPropsExpanded && isCardVisible(visibleCards, OsSectionCard.JAVA)) ensureLoad(SectionKind.JAVA)
    }
    LaunchedEffect(linuxEnvExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && linuxEnvExpanded && isCardVisible(visibleCards, OsSectionCard.LINUX)) ensureLoad(SectionKind.LINUX)
    }
    LaunchedEffect(
        showActivitySuggestionSheet,
        googleSystemServiceSuggestionTarget,
        activityShortcutDraft.packageName
    ) {
        if (!showActivitySuggestionSheet) return@LaunchedEffect
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
                val targetPackageName = activityShortcutDraft.packageName.trim()
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
    val shellRunnerRows = remember(shizukuStatus, context) {
        listOf(
            InfoRow(
                key = context.getString(R.string.os_shell_card_status_label),
                value = shizukuStatus
            ),
            InfoRow(
                key = context.getString(R.string.os_shell_card_hint_label),
                value = context.getString(R.string.os_shell_card_hint_value)
            )
        )
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

    suspend fun exportCard(card: OsSectionCard) {
        if (exportingCard != null) return
        exportingCard = card
        try {
            when (card) {
                OsSectionCard.TOP_INFO -> {
                    visibleSectionKinds(visibleCards).forEach { section ->
                        ensureLoad(section, forceRefresh = false)
                    }
                }

                else -> {
                    sectionKindByCard(card)?.let { section ->
                        ensureLoad(section, forceRefresh = false)
                    }
                }
            }

            val rows = currentRowsForCard(
                card = card,
                sectionStates = sectionStates,
                googleSystemServiceConfig = activityShortcutCards.firstOrNull()?.config
                    ?: googleSystemServiceDefaults,
                googleSystemServiceDefaults = googleSystemServiceDefaults,
                context = context
            )
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

    val currentVisibleSectionKinds = visibleSectionKinds(visibleCards)
    val visibleSectionStates = currentVisibleSectionKinds.mapNotNull { sectionStates[it] }
    val loadedFreshCount = visibleSectionStates.count { it.loadedFresh }
    val cachedSectionCount = visibleSectionStates.count { !it.loadedFresh && it.rows.isNotEmpty() }
    val sectionCount = currentVisibleSectionKinds.size
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
    val totalParameterCardCount = remember {
        OsSectionCard.entries.count { it != OsSectionCard.GOOGLE_SYSTEM_SERVICE }
    }
    val visibleParameterCardCount = remember(visibleCards) {
        visibleCards.count { it != OsSectionCard.GOOGLE_SYSTEM_SERVICE }
    }
    val activityOverviewStats = remember(activityShortcutCards) {
        buildOsActivityOverviewStats(
            cards = activityShortcutCards
        )
    }
    val overviewMetrics = remember(
        topInfoRows.size,
        visibleRowsCount,
        totalParameterCardCount,
        visibleParameterCardCount,
        activityOverviewStats
    ) {
        buildOsOverviewMetrics(
            context = context,
            topInfoCount = topInfoRows.size,
            visibleRowsCount = visibleRowsCount,
            visibleParameterCardCount = visibleParameterCardCount,
            totalParameterCardCount = totalParameterCardCount,
            activityStats = activityOverviewStats
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
        OsCardVisibilityManagerSheet(
            show = showCardManager,
            title = visibleCardsTitle,
            sheetBackdrop = sheetBackdrop,
            cardsHintText = "隐藏卡片后会清空对应缓存；重新显示时会立即重新获取并缓存。",
            onDismissRequest = { showCardManager = false },
            isCardVisible = { card -> isCardVisible(visibleCards, card) },
            onCardVisibilityChange = { card, checked ->
                scope.launch { applyCardVisibility(card, checked) }
            }
        )
        OsActivityVisibilityManagerSheet(
            show = showActivityVisibilityManager,
            title = visibleActivitiesTitle,
            sheetBackdrop = sheetBackdrop,
            activityHintText = stringResource(R.string.os_sheet_visible_activities_desc),
            cards = activityShortcutCards,
            defaultCardTitle = googleSystemServiceDefaultTitle,
            onDismissRequest = { showActivityVisibilityManager = false },
            onCardVisibilityChange = { cardId, checked ->
                scope.launch { applyActivityCardVisibility(cardId, checked) }
            }
        )
        OsActivityShortcutEditorHost(
            showEditor = showActivityShortcutEditor,
            editorTitle = if (activityCardEditMode == OsActivityCardEditMode.Add) {
                addActivityCardTitle
            } else {
                editActivityCardTitle
            },
            sheetBackdrop = sheetBackdrop,
            draft = activityShortcutDraft,
            onDraftChange = { activityShortcutDraft = it },
            onOpenSuggestionSheet = { target ->
                googleSystemServiceSuggestionTarget = target
                when (target) {
                    ShortcutSuggestionField.PackageName -> googleSystemServicePackageSuggestionQuery = ""
                    ShortcutSuggestionField.ClassName -> googleSystemServiceClassSuggestionQuery = ""
                    else -> Unit
                }
                showActivitySuggestionSheet = true
            },
            onDismissEditor = { showActivityShortcutEditor = false },
            onSaveEditor = {
                val normalized = normalizeActivityShortcutConfig(
                    config = activityShortcutDraft,
                    defaults = googleSystemServiceDefaults
                )
                val updatedCards = if (activityCardEditMode == OsActivityCardEditMode.Add) {
                    activityShortcutCards + OsActivityShortcutCard(
                        id = newOsActivityShortcutCardId(),
                        visible = true,
                        config = normalized
                    )
                } else {
                    val targetId = editingActivityShortcutCardId
                    if (targetId.isNullOrBlank()) {
                        activityShortcutCards + OsActivityShortcutCard(
                            id = newOsActivityShortcutCardId(),
                            visible = true,
                            config = normalized
                        )
                    } else {
                        activityShortcutCards.map { card ->
                            if (card.id == targetId) card.copy(config = normalized) else card
                        }
                    }
                }
                activityShortcutCards = updatedCards
                scope.launch(Dispatchers.IO) {
                    OsActivityShortcutCardStore.saveCards(
                        cards = updatedCards,
                        defaults = googleSystemServiceDefaults
                    )
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.os_google_system_service_toast_saved),
                    Toast.LENGTH_SHORT
                ).show()
                showActivityShortcutEditor = false
            },
            showSuggestionSheet = showActivitySuggestionSheet,
            suggestionTarget = googleSystemServiceSuggestionTarget,
            packageSuggestions = googleSystemServicePackageSuggestions,
            packageSuggestionsLoading = googleSystemServicePackageSuggestionsLoading,
            packageSuggestionQuery = googleSystemServicePackageSuggestionQuery,
            onPackageSuggestionQueryChange = { googleSystemServicePackageSuggestionQuery = it },
            classSuggestions = googleSystemServiceClassSuggestions,
            classSuggestionsLoading = googleSystemServiceClassSuggestionsLoading,
            classSuggestionQuery = googleSystemServiceClassSuggestionQuery,
            onClassSuggestionQueryChange = { googleSystemServiceClassSuggestionQuery = it },
            noMatchedResultsText = noMatchedResultsText,
            onDismissSuggestionSheet = { showActivitySuggestionSheet = false },
            onApplySuggestion = { suggestion ->
                activityShortcutDraft = applyGoogleSystemServiceSuggestion(
                    draft = activityShortcutDraft,
                    target = googleSystemServiceSuggestionTarget,
                    item = suggestion,
                    defaultIntentFlags = googleSystemServiceDefaultIntentFlags
                )
                showActivitySuggestionSheet = false
            },
            onApplyExplicitActionRecommendation = {
                activityShortcutDraft = activityShortcutDraft.copy(
                    intentAction = Intent.ACTION_VIEW
                )
            },
            onApplyImplicitActionRecommendation = {
                activityShortcutDraft = applyShortcutImplicitDefaults(
                    draft = activityShortcutDraft,
                    defaultIntentFlags = googleSystemServiceDefaultIntentFlags
                )
            },
            onApplyExplicitCategoryRecommendation = {
                activityShortcutDraft = activityShortcutDraft.copy(
                    intentCategory = ""
                )
            },
            onApplyImplicitCategoryRecommendation = {
                activityShortcutDraft = applyShortcutImplicitDefaults(
                    draft = activityShortcutDraft,
                    defaultIntentFlags = googleSystemServiceDefaultIntentFlags
                )
            }
        )
        OsPageMainList(
            context = context,
            listState = listState,
            innerPadding = innerPadding,
            searchBarScrollConnection = searchBarScrollConnection,
            scrollBehaviorConnection = scrollBehavior.nestedScrollConnection,
            contentBackdrop = contentBackdrop,
            isDark = isDark,
            titleColor = titleColor,
            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
            refreshing = refreshing,
            overviewState = overviewState,
            indicatorProgress = indicatorProgress,
            statusColor = statusColor,
            indicatorBg = indicatorBg,
            statusLabel = statusLabel,
            overviewCardColor = overviewCardColor,
            overviewBorderColor = overviewBorderColor,
            overviewMetrics = overviewMetrics,
            noMatchedResultsText = noMatchedResultsText,
            query = q,
            displayedTopInfoRows = displayedTopInfoRows,
            groupedTopInfoRows = groupedTopInfoRows,
            topInfoExpanded = topInfoExpanded,
            onTopInfoExpandedChange = { topInfoExpanded = it },
            shellRunnerRows = shellRunnerRows,
            shellRunnerExpanded = shellRunnerExpanded,
            onShellRunnerExpandedChange = { shellRunnerExpanded = it },
            onOpenShellRunner = { OsShellRunnerActivity.launch(context) },
            activityShortcutCards = activityShortcutCards,
            defaultActivityCardTitle = googleSystemServiceDefaultTitle,
            activityCardExpanded = activityCardExpanded,
            onActivityCardExpandedChange = { cardId, expanded ->
                activityCardExpanded[cardId] = expanded
            },
            onOpenActivityShortcutCard = { card ->
                val normalized = normalizeActivityShortcutConfig(
                    config = card.config,
                    defaults = googleSystemServiceDefaults
                )
                if (normalized.packageName.isBlank()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.os_google_system_service_toast_invalid_target),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    runCatching {
                        launchGoogleSystemServiceActivity(
                            context = context,
                            config = normalized,
                            defaults = googleSystemServiceDefaults
                        )
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
            },
            onOpenActivityShortcutCardEditor = { card ->
                activityCardEditMode = OsActivityCardEditMode.Edit
                editingActivityShortcutCardId = card.id
                activityShortcutDraft = ensureEditorActivityShortcutDraft(
                    normalizeActivityShortcutConfig(
                        config = card.config,
                        defaults = googleSystemServiceDefaults
                    )
                )
                showActivityShortcutEditor = true
            },
            displayedSystemRows = displayedSystemRows,
            displayedSecureRows = displayedSecureRows,
            displayedGlobalRows = displayedGlobalRows,
            displayedAndroidRows = displayedAndroidRows,
            displayedJavaRows = displayedJavaRows,
            displayedLinuxRows = displayedLinuxRows,
            prunedSystemRows = prunedSystemRows,
            prunedSecureRows = prunedSecureRows,
            prunedGlobalRows = prunedGlobalRows,
            prunedAndroidRows = prunedAndroidRows,
            prunedJavaRows = prunedJavaRows,
            prunedLinuxRows = prunedLinuxRows,
            systemTableExpanded = systemTableExpanded,
            onSystemTableExpandedChange = { systemTableExpanded = it },
            secureTableExpanded = secureTableExpanded,
            onSecureTableExpandedChange = { secureTableExpanded = it },
            globalTableExpanded = globalTableExpanded,
            onGlobalTableExpandedChange = { globalTableExpanded = it },
            androidPropsExpanded = androidPropsExpanded,
            onAndroidPropsExpandedChange = { androidPropsExpanded = it },
            javaPropsExpanded = javaPropsExpanded,
            onJavaPropsExpandedChange = { javaPropsExpanded = it },
            linuxEnvExpanded = linuxEnvExpanded,
            onLinuxEnvExpandedChange = { linuxEnvExpanded = it },
            isCardVisible = { card -> isCardVisible(visibleCards, card) },
            sectionSubtitle = { section, size ->
                sectionSubtitle(
                    sectionStates = sectionStates,
                    context = context,
                    section = section,
                    size = size
                )
            },
            exportingCard = exportingCard,
            onExportCard = { card -> scope.launch { exportCard(card) } },
            onRefreshAll = { scope.launch { refreshAllSections() } },
            contentBottomPadding = contentBottomPadding,
            showFloatingAddButton = !showActivityShortcutEditor && !showActivitySuggestionSheet,
            onOpenAddActivityShortcutCard = {
                activityCardEditMode = OsActivityCardEditMode.Add
                editingActivityShortcutCardId = null
                activityShortcutDraft = createDefaultActivityShortcutDraft(googleSystemServiceDefaults)
                showActivityShortcutEditor = true
            }
        )
    }
}
