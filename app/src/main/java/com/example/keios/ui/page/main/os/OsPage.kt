package com.example.keios.ui.page.main.os

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.keios.R
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keios.ui.page.main.os.components.OsPageMainList
import com.example.keios.ui.page.main.os.components.OsPageOverlaySheets
import com.example.keios.ui.page.main.os.shell.OsShellCommandCard
import com.example.keios.ui.page.main.os.shell.OsShellCommandCardStore
import com.example.keios.ui.page.main.os.shell.OsShellRunnerActivity
import com.example.keios.ui.page.main.os.shell.createDefaultShellCommandCardDraft
import com.example.keios.ui.page.main.os.shell.defaultOsShellCommandCardTitle
import com.example.keios.ui.page.main.os.shortcut.OsActivityCardEditMode
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCard
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import com.example.keios.ui.page.main.os.shortcut.ShortcutActivityClassOption
import com.example.keios.ui.page.main.os.shortcut.ShortcutInstalledAppOption
import com.example.keios.ui.page.main.os.shortcut.ShortcutSuggestionField
import com.example.keios.ui.page.main.os.shortcut.applyGoogleSystemServiceSuggestion
import com.example.keios.ui.page.main.os.shortcut.applyShortcutImplicitDefaults
import com.example.keios.ui.page.main.os.shortcut.createDefaultActivityShortcutDraft
import com.example.keios.ui.page.main.os.shortcut.ensureEditorActivityShortcutDraft
import com.example.keios.ui.page.main.os.shortcut.launchGoogleSystemServiceActivity
import com.example.keios.ui.page.main.os.shortcut.loadActivityClassOptions
import com.example.keios.ui.page.main.os.shortcut.loadInstalledAppOptions
import com.example.keios.ui.page.main.os.shortcut.newOsActivityShortcutCardId
import com.example.keios.ui.page.main.os.shortcut.normalizeActivityShortcutConfig
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class OsCardImportTarget {
    Activity,
    Shell
}

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
    val manageShellCardsContentDescription = stringResource(R.string.os_action_manage_shell_cards)
    val refreshParamsContentDescription = stringResource(R.string.os_action_refresh_params)
    val searchLabel = stringResource(R.string.os_search_label)
    val visibleCardsTitle = stringResource(R.string.os_sheet_visible_cards_title)
    val visibleActivitiesTitle = stringResource(R.string.os_sheet_visible_activities_title)
    val visibleShellCardsTitle = stringResource(R.string.os_sheet_visible_shell_cards_title)
    val visibleShellCardsDesc = stringResource(R.string.os_sheet_visible_shell_cards_desc)
    val googleSystemServiceDefaultTitle = stringResource(R.string.os_section_google_system_service_title)
    val googleSystemServiceDefaultSubtitle = stringResource(R.string.os_google_system_service_default_subtitle)
    val googleSystemServiceDefaultAppName = stringResource(R.string.os_google_system_service_default_app_name)
    val shellSavedCountLabel = stringResource(R.string.os_shell_card_saved_count_label)
    val shellCardSavedToast = stringResource(R.string.os_shell_card_toast_saved)
    val shellCardDeletedToast = stringResource(R.string.os_shell_card_toast_deleted)
    val shellCardCommandRequiredToast = stringResource(R.string.os_shell_card_toast_command_required)
    val shellCardDeleteDialogTitle = stringResource(R.string.os_shell_card_delete_dialog_title)
    val shellRunNoPermissionText = stringResource(R.string.os_shell_run_requires_permission)
    val shellRunNoOutputText = stringResource(R.string.os_shell_run_empty_output)
    val editShellCommandCardTitle = stringResource(R.string.os_shell_card_sheet_title_edit)
    val editActivityCardTitle = stringResource(R.string.os_activity_sheet_title_edit)
    val addActivityCardTitle = stringResource(R.string.os_activity_sheet_title_add)
    val activityCardDeletedToast = stringResource(R.string.os_activity_card_toast_deleted)
    val activityCardDeleteDialogTitle = stringResource(R.string.os_activity_card_delete_dialog_title)
    val cardImportFailedWithReason = stringResource(R.string.os_card_toast_import_failed_with_reason)
    val activityBuiltInGoogleSettingsTitle =
        stringResource(R.string.os_activity_builtin_google_settings_title)
    val activityBuiltInGoogleSettingsSubtitle =
        stringResource(R.string.os_activity_builtin_google_settings_subtitle)
    val activityBuiltInGoogleSettingsAppName =
        stringResource(R.string.os_activity_builtin_google_settings_app_name)
    val activityBuiltInGoogleSettingsPackage =
        stringResource(R.string.os_activity_builtin_google_settings_package)
    val activityBuiltInGoogleSettingsClass =
        stringResource(R.string.os_activity_builtin_google_settings_class)
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
    val googleSettingsBuiltInSampleDefaults = remember(
        activityBuiltInGoogleSettingsTitle,
        activityBuiltInGoogleSettingsSubtitle,
        activityBuiltInGoogleSettingsAppName,
        activityBuiltInGoogleSettingsPackage,
        activityBuiltInGoogleSettingsClass,
        googleSystemServiceDefaultIntentFlags
    ) {
        OsGoogleSystemServiceConfig(
            title = activityBuiltInGoogleSettingsTitle,
            subtitle = activityBuiltInGoogleSettingsSubtitle,
            appName = activityBuiltInGoogleSettingsAppName,
            packageName = activityBuiltInGoogleSettingsPackage,
            className = activityBuiltInGoogleSettingsClass,
            intentAction = Intent.ACTION_VIEW,
            intentFlags = googleSystemServiceDefaultIntentFlags
        ).normalized(googleSystemServiceDefaults)
    }
    val noMatchedResultsText = stringResource(R.string.common_no_matched_results)
    val shizukuReady = shizukuStatus.contains("granted", ignoreCase = true)
    val initialUiSnapshot = remember { OsUiStateStore.loadSnapshot() }
    val lifecycleOwner = LocalLifecycleOwner.current
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
                defaults = googleSystemServiceDefaults,
                builtInSampleDefaults = googleSettingsBuiltInSampleDefaults
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
    var editingActivityShortcutBuiltIn by rememberSaveable { mutableStateOf(false) }
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
    var showShellCardVisibilityManager by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingImportTarget by remember { mutableStateOf<OsCardImportTarget?>(null) }
    var cardTransferInProgress by remember { mutableStateOf(false) }
    var exportingCard by remember { mutableStateOf<OsSectionCard?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshProgress by remember { mutableStateOf(0f) }
    var shellCommandCards by remember { mutableStateOf(OsShellCommandCardStore.loadCards()) }
    val shellCommandCardExpanded = remember { mutableStateMapOf<String, Boolean>() }
    var runningShellCommandCardIds by remember { mutableStateOf(emptySet<String>()) }
    val sectionLoadMutex = remember { Mutex() }
    val sectionLoadDeferreds = remember { mutableStateMapOf<SectionKind, Deferred<List<InfoRow>>>() }
    var showShellCommandCardEditor by rememberSaveable { mutableStateOf(false) }
    var editingShellCommandCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var shellCommandCardDraft by remember { mutableStateOf(createDefaultShellCommandCardDraft()) }
    var showShellCardDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showActivityCardDeleteConfirm by rememberSaveable { mutableStateOf(false) }
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
                    if (!showSearchBar) {
                        showSearchBar = true
                    }
                    if (searchBarHideOffsetPx != 0f) {
                        searchBarHideOffsetPx = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    BindOsShellCardReloadOnResume(
        lifecycleOwner = lifecycleOwner,
        reloadCards = { shellCommandCards = OsShellCommandCardStore.loadCards() }
    )
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
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = pendingImportTarget
        pendingImportTarget = null
        if (uri == null || target == null) {
            cardTransferInProgress = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                        reader?.readText().orEmpty()
                    }
                }
                when (target) {
                    OsCardImportTarget.Activity -> {
                        val imported = OsActivityShortcutCardStore.importCardsFromJson(
                            raw = raw,
                            defaults = googleSystemServiceDefaults,
                            builtInSampleDefaults = googleSettingsBuiltInSampleDefaults
                        )
                        activityShortcutCards = imported
                        val validIds = imported.mapTo(mutableSetOf()) { it.id }
                        activityCardExpanded.keys.retainAll(validIds)
                        if (!validIds.contains(editingActivityShortcutCardId.orEmpty())) {
                            showActivityShortcutEditor = false
                            showActivityCardDeleteConfirm = false
                            editingActivityShortcutCardId = null
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.os_activity_card_toast_imported_summary,
                                imported.size
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    OsCardImportTarget.Shell -> {
                        val imported = OsShellCommandCardStore.importCardsFromJson(raw)
                        shellCommandCards = imported
                        val validIds = imported.mapTo(mutableSetOf()) { it.id }
                        shellCommandCardExpanded.keys.retainAll(validIds)
                        if (!validIds.contains(editingShellCommandCardId.orEmpty())) {
                            showShellCommandCardEditor = false
                            showShellCardDeleteConfirm = false
                            editingShellCommandCardId = null
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.os_shell_card_toast_imported_summary,
                                imported.size
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    String.format(
                        cardImportFailedWithReason,
                        error.message ?: error.javaClass.simpleName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            cardTransferInProgress = false
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
        ensureOsSectionLoaded(
            section = section,
            forceRefresh = forceRefresh,
            visibleCardsProvider = { visibleCards },
            sectionStatesProvider = { sectionStates },
            sectionLoadMutex = sectionLoadMutex,
            sectionLoadDeferreds = sectionLoadDeferreds,
            scope = scope,
            context = context,
            shizukuStatus = shizukuStatus,
            shizukuApiUtils = shizukuApiUtils,
            updateSection = ::updateSection,
            onCachePersistedChanged = { cachePersisted = it }
        )
    }

    suspend fun applyCardVisibility(card: OsSectionCard, visible: Boolean) {
        applyOsCardVisibility(
            card = card,
            visible = visible,
            currentVisibleCards = visibleCards,
            updateVisibleCards = { visibleCards = it },
            setTopInfoExpanded = { topInfoExpanded = it },
            setShellRunnerExpanded = { shellRunnerExpanded = it },
            setSystemTableExpanded = { systemTableExpanded = it },
            setSecureTableExpanded = { secureTableExpanded = it },
            setGlobalTableExpanded = { globalTableExpanded = it },
            setAndroidPropsExpanded = { androidPropsExpanded = it },
            setJavaPropsExpanded = { javaPropsExpanded = it },
            setLinuxEnvExpanded = { linuxEnvExpanded = it },
            updateSection = ::updateSection,
            ensureLoad = ::ensureLoad,
            visibleCardsProvider = { visibleCards },
            onCachePersistedChanged = { cachePersisted = it }
        )
    }

    suspend fun applyActivityCardVisibility(cardId: String, visible: Boolean) {
        applyOsActivityCardVisibility(
            cardId = cardId,
            visible = visible,
            currentCards = activityShortcutCards,
            defaults = googleSystemServiceDefaults,
            updateCards = { activityShortcutCards = it }
        )
    }

    suspend fun applyShellCommandCardVisibility(cardId: String, visible: Boolean) {
        applyOsShellCommandCardVisibility(
            cardId = cardId,
            visible = visible,
            updateCards = { shellCommandCards = it }
        )
    }

    suspend fun runShellCommandCard(card: OsShellCommandCard) {
        runOsShellCommandCard(
            card = card,
            context = context,
            shizukuApiUtils = shizukuApiUtils,
            shellCardCommandRequiredToast = shellCardCommandRequiredToast,
            shellRunNoPermissionToast = shellRunNoPermissionText,
            shellRunNoOutputText = shellRunNoOutputText,
            runningCardIdsProvider = { runningShellCommandCardIds },
            updateRunningCardIds = { runningShellCommandCardIds = it },
            onCardsReload = { shellCommandCards = OsShellCommandCardStore.loadCards() },
            runFailedMessage = { throwable ->
                context.getString(
                    R.string.os_shell_card_toast_run_failed,
                    throwable.javaClass.simpleName
                )
            }
        )
    }

    suspend fun refreshAllSections() {
        refreshAllOsSections(
            context = context,
            visibleCardsProvider = { visibleCards },
            setRefreshing = { refreshing = it },
            setRefreshProgress = { refreshProgress = it },
            ensureLoad = ::ensureLoad,
            noRefreshableCardText = noRefreshableCardText,
            refreshCompletedText = refreshCompletedText
        )
    }

    BindOsCardExpandedStateMaps(
        activityShortcutCards = activityShortcutCards,
        activityCardExpanded = activityCardExpanded,
        initialGoogleSystemServiceExpanded = initialUiSnapshot.googleSystemServiceExpanded,
        shellCommandCards = shellCommandCards,
        shellCommandCardExpanded = shellCommandCardExpanded
    )

    BindOsScrollToTopEffect(
        scrollToTopSignal = scrollToTopSignal,
        listState = listState
    )

    BindOsInitialCacheLoad(
        visibleCards = visibleCards,
        onVisibleCardsChange = { visibleCards = it },
        onSectionStatesChange = { sectionStates = it },
        onCachePersistedChange = { cachePersisted = it },
        onCacheLoadedChange = { cacheLoaded = it },
        onUiStatePersistenceReadyChange = { uiStatePersistenceReady = it },
        isPageActive = isPageActive,
        ensureLoad = ::ensureLoad
    )

    BindOsShizukuInvalidation(
        shizukuReady = shizukuReady,
        updateSection = ::updateSection
    )

    BindOsExpandedStatePersistence(
        ready = uiStatePersistenceReady,
        snapshotProvider = {
            OsUiSnapshot(
                topInfoExpanded = topInfoExpanded,
                shellRunnerExpanded = shellRunnerExpanded,
                systemTableExpanded = systemTableExpanded,
                secureTableExpanded = secureTableExpanded,
                globalTableExpanded = globalTableExpanded,
                androidPropsExpanded = androidPropsExpanded,
                javaPropsExpanded = javaPropsExpanded,
                linuxEnvExpanded = linuxEnvExpanded
            )
        }
    )

    LaunchedEffect(systemTableExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && systemTableExpanded && isCardVisible(
                visibleCards,
                OsSectionCard.SYSTEM
            )
        ) ensureLoad(SectionKind.SYSTEM)
    }
    LaunchedEffect(secureTableExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && secureTableExpanded && isCardVisible(
                visibleCards,
                OsSectionCard.SECURE
            )
        ) ensureLoad(SectionKind.SECURE)
    }
    LaunchedEffect(globalTableExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && globalTableExpanded && isCardVisible(
                visibleCards,
                OsSectionCard.GLOBAL
            )
        ) ensureLoad(SectionKind.GLOBAL)
    }
    LaunchedEffect(androidPropsExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && androidPropsExpanded && isCardVisible(
                visibleCards,
                OsSectionCard.ANDROID
            )
        ) ensureLoad(SectionKind.ANDROID)
    }
    LaunchedEffect(javaPropsExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && javaPropsExpanded && isCardVisible(visibleCards, OsSectionCard.JAVA)) ensureLoad(
            SectionKind.JAVA)
    }
    LaunchedEffect(linuxEnvExpanded, visibleCards, cacheLoaded, isPageActive) {
        if (!cacheLoaded) return@LaunchedEffect
        if (isPageActive && linuxEnvExpanded && isCardVisible(visibleCards, OsSectionCard.LINUX)) ensureLoad(
            SectionKind.LINUX)
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

    val prunedSystemRows = remember(systemRows) {
        removeTopInfoRows(
            SectionKind.SYSTEM,
            systemRows
        )
    }
    val prunedSecureRows = remember(secureRows) {
        removeTopInfoRows(
            SectionKind.SECURE,
            secureRows
        )
    }
    val prunedGlobalRows = remember(globalRows) {
        removeTopInfoRows(
            SectionKind.GLOBAL,
            globalRows
        )
    }
    val prunedAndroidRows = remember(androidRows) {
        removeTopInfoRows(
            SectionKind.ANDROID,
            androidRows
        )
    }
    val prunedJavaRows = remember(javaRows) { removeTopInfoRows(SectionKind.JAVA, javaRows) }
    val prunedLinuxRows = remember(linuxRows) { removeTopInfoRows(SectionKind.LINUX, linuxRows) }

    val q = queryApplied.trim()
    val displayedTopInfoRows = remember(q, topInfoRows, topInfoExpanded) {
        if (q.isBlank() && !topInfoExpanded) topInfoRows else sortRowsByType(
            filterRows(
                topInfoRows,
                q
            )
        )
    }
    val displayedSystemRows = remember(q, prunedSystemRows, systemTableExpanded) {
        if (q.isBlank() && !systemTableExpanded) prunedSystemRows else sortRowsByType(
            filterRows(
                prunedSystemRows,
                q
            )
        )
    }
    val displayedSecureRows = remember(q, prunedSecureRows, secureTableExpanded) {
        if (q.isBlank() && !secureTableExpanded) prunedSecureRows else sortRowsByType(
            filterRows(
                prunedSecureRows,
                q
            )
        )
    }
    val displayedGlobalRows = remember(q, prunedGlobalRows, globalTableExpanded) {
        if (q.isBlank() && !globalTableExpanded) prunedGlobalRows else sortRowsByType(
            filterRows(
                prunedGlobalRows,
                q
            )
        )
    }
    val displayedAndroidRows = remember(q, prunedAndroidRows, androidPropsExpanded) {
        if (q.isBlank() && !androidPropsExpanded) prunedAndroidRows else sortRowsByType(
            filterRows(
                prunedAndroidRows,
                q
            )
        )
    }
    val displayedJavaRows = remember(q, prunedJavaRows, javaPropsExpanded) {
        if (q.isBlank() && !javaPropsExpanded) prunedJavaRows else sortRowsByType(
            filterRows(
                prunedJavaRows,
                q
            )
        )
    }
    val displayedLinuxRows = remember(q, prunedLinuxRows, linuxEnvExpanded) {
        if (q.isBlank() && !linuxEnvExpanded) prunedLinuxRows else sortRowsByType(
            filterRows(
                prunedLinuxRows,
                q
            )
        )
    }
    val groupedTopInfoRows = remember(displayedTopInfoRows, topInfoExpanded, q) {
        if (q.isBlank() && !topInfoExpanded) emptyList() else groupTopInfoRows(displayedTopInfoRows)
    }
    val shellRunnerRows = remember(
        shizukuStatus,
        context,
        shellSavedCountLabel,
        shellCommandCards
    ) {
        listOf(
            InfoRow(
                key = context.getString(R.string.os_shell_card_status_label),
                value = shizukuStatus
            ),
            InfoRow(
                key = shellSavedCountLabel,
                value = context.getString(R.string.common_item_count, shellCommandCards.size)
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
        exportOsSectionCard(
            card = card,
            currentExportingCard = exportingCard,
            updateExportingCard = { exportingCard = it },
            visibleCardsProvider = { visibleCards },
            ensureLoad = ::ensureLoad,
            sectionStatesProvider = { sectionStates },
            activityShortcutCardsProvider = { activityShortcutCards },
            googleSystemServiceDefaults = googleSystemServiceDefaults,
            context = context,
            shizukuStatus = shizukuStatus,
            launchExport = { fileName, payload ->
                pendingExportContent = payload
                exportLauncher.launch(fileName)
            },
            onExportFailed = { throwable ->
                Toast.makeText(
                    context,
                    "导出失败: ${throwable.javaClass.simpleName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    val overviewUiState = remember(
        isDark,
        inactive,
        cachedColor,
        refreshingColor,
        syncedColor,
        refreshing,
        refreshProgress,
        cachePersisted,
        visibleCards,
        sectionStates,
        topInfoRows.size,
        visibleRowsCount,
        activityShortcutCards,
        shellCommandCards,
        surfaceColor
    ) {
        buildOsOverviewUiState(
            context = context,
            isDark = isDark,
            inactiveColor = inactive,
            cachedColor = cachedColor,
            refreshingColor = refreshingColor,
            syncedColor = syncedColor,
            surfaceColor = surfaceColor,
            refreshing = refreshing,
            refreshProgress = refreshProgress,
            cachePersisted = cachePersisted,
            visibleCards = visibleCards,
            sectionStates = sectionStates,
            topInfoCount = topInfoRows.size,
            visibleRowsCount = visibleRowsCount,
            activityCards = activityShortcutCards,
            shellCommandCards = shellCommandCards
        )
    }
    val overviewState = overviewUiState.overviewState
    val statusLabel = overviewUiState.statusLabel
    val statusColor = overviewUiState.statusColor
    val overviewCardColor = overviewUiState.overviewCardColor
    val overviewBorderColor = overviewUiState.overviewBorderColor
    val indicatorProgress = overviewUiState.indicatorProgress
    val indicatorBg = overviewUiState.indicatorBg
    val overviewMetrics = overviewUiState.metrics

    OsPageScaffoldShell(
        scrollBehavior = scrollBehavior,
        topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
        topBarBackdrop = topBarBackdrop,
        layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
        manageCardsContentDescription = manageCardsContentDescription,
        manageActivitiesContentDescription = manageActivitiesContentDescription,
        manageShellCardsContentDescription = manageShellCardsContentDescription,
        refreshParamsContentDescription = refreshParamsContentDescription,
        refreshing = refreshing,
        onOpenCardManager = { showCardManager = true },
        onOpenActivityVisibilityManager = { showActivityVisibilityManager = true },
        onOpenShellCardVisibilityManager = { showShellCardVisibilityManager = true },
        onRefresh = { scope.launch { refreshAllSections() } },
        onActionBarInteractingChanged = onActionBarInteractingChanged,
        searchBarVisible = enableSearchBar && showSearchBar,
        queryInput = queryInput,
        onQueryInputChange = osPageViewModel::updateQueryInput,
        searchLabel = searchLabel
    ) { innerPadding ->
        OsPageOverlaySheets(
            showCardManager = showCardManager,
            visibleCardsTitle = visibleCardsTitle,
            sheetBackdrop = sheetBackdrop,
            cardsHintText = "隐藏卡片后会清空对应缓存；重新显示时会立即重新获取并缓存。",
            visibleCards = visibleCards,
            onDismissCardManager = { showCardManager = false },
            onCardVisibilityChange = { card, checked ->
                scope.launch { applyCardVisibility(card, checked) }
            },
            showActivityVisibilityManager = showActivityVisibilityManager,
            visibleActivitiesTitle = visibleActivitiesTitle,
            activityHintText = stringResource(R.string.os_sheet_visible_activities_desc),
            activityShortcutCards = activityShortcutCards,
            defaultActivityCardTitle = googleSystemServiceDefaultTitle,
            cardTransferInProgress = cardTransferInProgress,
            onExportAllActivityCards = {
                runCatching {
                    val payload = OsActivityShortcutCardStore.buildCardsExportJson(
                        cards = activityShortcutCards,
                        defaults = googleSystemServiceDefaults
                    )
                    pendingExportContent = payload
                    exportLauncher.launch("keios-os-activity-cards.json")
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.common_export_failed_with_reason,
                            error.message ?: error.javaClass.simpleName
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onImportAllActivityCards = {
                pendingImportTarget = OsCardImportTarget.Activity
                cardTransferInProgress = true
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismissActivityVisibilityManager = { showActivityVisibilityManager = false },
            onActivityCardVisibilityChange = { cardId, checked ->
                scope.launch { applyActivityCardVisibility(cardId, checked) }
            },
            showShellCardVisibilityManager = showShellCardVisibilityManager,
            visibleShellCardsTitle = visibleShellCardsTitle,
            visibleShellCardsDesc = visibleShellCardsDesc,
            shellRunnerVisible = visibleCards.contains(OsSectionCard.SHELL_RUNNER),
            onShellRunnerVisibilityChange = { checked ->
                scope.launch { applyCardVisibility(OsSectionCard.SHELL_RUNNER, checked) }
            },
            shellCommandCards = shellCommandCards,
            onExportAllShellCards = {
                runCatching {
                    val payload = OsShellCommandCardStore.buildCardsExportJson(shellCommandCards)
                    pendingExportContent = payload
                    exportLauncher.launch("keios-os-shell-cards.json")
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.common_export_failed_with_reason,
                            error.message ?: error.javaClass.simpleName
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onImportAllShellCards = {
                pendingImportTarget = OsCardImportTarget.Shell
                cardTransferInProgress = true
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismissShellVisibilityManager = { showShellCardVisibilityManager = false },
            onShellCommandCardVisibilityChange = { cardId, checked ->
                scope.launch { applyShellCommandCardVisibility(cardId, checked) }
            },
            showShellCommandCardEditor = showShellCommandCardEditor,
            editShellCommandCardTitle = editShellCommandCardTitle,
            shellCommandCardDraft = shellCommandCardDraft,
            onShellCommandCardDraftChange = { shellCommandCardDraft = it },
            showShellCardDeleteAction = !editingShellCommandCardId.isNullOrBlank(),
            onDeleteShellCommandCard = {
                val targetId = editingShellCommandCardId.orEmpty().trim()
                if (targetId.isBlank()) {
                    showShellCommandCardEditor = false
                    showShellCardDeleteConfirm = false
                    return@OsPageOverlaySheets
                }
                showShellCardDeleteConfirm = true
            },
            onDismissShellCommandCardEditor = {
                showShellCommandCardEditor = false
                showShellCardDeleteConfirm = false
            },
            onSaveShellCommandCard = {
                val targetId = editingShellCommandCardId.orEmpty().trim()
                if (targetId.isBlank()) {
                    Toast.makeText(context, shellCardCommandRequiredToast, Toast.LENGTH_SHORT)
                        .show()
                    return@OsPageOverlaySheets
                }
                val updated = OsShellCommandCardStore.updateCard(
                    cardId = targetId,
                    title = shellCommandCardDraft.title,
                    subtitle = shellCommandCardDraft.subtitle,
                    command = shellCommandCardDraft.command
                )
                if (updated == null) {
                    Toast.makeText(context, shellCardCommandRequiredToast, Toast.LENGTH_SHORT)
                        .show()
                    return@OsPageOverlaySheets
                }
                shellCommandCards = OsShellCommandCardStore.loadCards()
                Toast.makeText(context, shellCardSavedToast, Toast.LENGTH_SHORT).show()
                showShellCommandCardEditor = false
                showShellCardDeleteConfirm = false
            },
            showActivityShortcutEditor = showActivityShortcutEditor,
            activityEditorTitle = if (activityCardEditMode == OsActivityCardEditMode.Add) {
                addActivityCardTitle
            } else {
                editActivityCardTitle
            },
            activityShortcutDraft = activityShortcutDraft,
            onActivityShortcutDraftChange = { activityShortcutDraft = it },
            onOpenActivitySuggestionSheet = { target ->
                googleSystemServiceSuggestionTarget = target
                when (target) {
                    ShortcutSuggestionField.PackageName -> googleSystemServicePackageSuggestionQuery =
                        ""

                    ShortcutSuggestionField.ClassName -> googleSystemServiceClassSuggestionQuery =
                        ""

                    else -> Unit
                }
                showActivitySuggestionSheet = true
            },
            showBuiltInActivityCardBadge = editingActivityShortcutBuiltIn,
            showDeleteActivityAction = activityCardEditMode == OsActivityCardEditMode.Edit &&
                    !editingActivityShortcutCardId.isNullOrBlank(),
            onDeleteActivityCard = {
                val targetId = editingActivityShortcutCardId.orEmpty().trim()
                if (targetId.isBlank()) {
                    showActivityShortcutEditor = false
                    showActivityCardDeleteConfirm = false
                    return@OsPageOverlaySheets
                }
                showActivityCardDeleteConfirm = true
            },
            onDismissActivityEditor = {
                showActivityShortcutEditor = false
                showActivityCardDeleteConfirm = false
                editingActivityShortcutBuiltIn = false
            },
            onSaveActivityEditor = {
                val normalized = normalizeActivityShortcutConfig(
                    config = activityShortcutDraft,
                    defaults = googleSystemServiceDefaults
                )
                val updatedCards = if (activityCardEditMode == OsActivityCardEditMode.Add) {
                    activityShortcutCards + OsActivityShortcutCard(
                        id = newOsActivityShortcutCardId(),
                        visible = true,
                        isBuiltInSample = false,
                        config = normalized
                    )
                } else {
                    val targetId = editingActivityShortcutCardId
                    if (targetId.isNullOrBlank()) {
                        activityShortcutCards + OsActivityShortcutCard(
                            id = newOsActivityShortcutCardId(),
                            visible = true,
                            isBuiltInSample = false,
                            config = normalized
                        )
                    } else {
                        activityShortcutCards.map { card ->
                            if (card.id == targetId) card.copy(config = normalized) else card
                        }
                    }
                }
                activityShortcutCards = updatedCards
                OsActivityShortcutCardStore.saveCards(
                    cards = updatedCards,
                    defaults = googleSystemServiceDefaults
                )
                Toast.makeText(
                    context,
                    context.getString(R.string.os_google_system_service_toast_saved),
                    Toast.LENGTH_SHORT
                ).show()
                showActivityShortcutEditor = false
                showActivityCardDeleteConfirm = false
                editingActivityShortcutBuiltIn = false
            },
            showActivitySuggestionSheet = showActivitySuggestionSheet,
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
            },
            showShellCardDeleteConfirm = showShellCardDeleteConfirm,
            shellCardDeleteDialogTitle = shellCardDeleteDialogTitle,
            shellCardDeleteDialogSummary = context.getString(
                R.string.os_shell_card_delete_dialog_summary,
                shellCommandCardDraft.title.ifBlank {
                    defaultOsShellCommandCardTitle(shellCommandCardDraft.command)
                }
            ),
            onDismissShellCardDeleteConfirm = { showShellCardDeleteConfirm = false },
            onConfirmShellCardDelete = {
                val targetId = editingShellCommandCardId.orEmpty().trim()
                showShellCardDeleteConfirm = false
                if (targetId.isBlank()) return@OsPageOverlaySheets
                shellCommandCards = OsShellCommandCardStore.deleteCard(targetId)
                shellCommandCardExpanded.remove(targetId)
                editingShellCommandCardId = null
                showShellCommandCardEditor = false
                Toast.makeText(context, shellCardDeletedToast, Toast.LENGTH_SHORT).show()
            },
            showActivityCardDeleteConfirm = showActivityCardDeleteConfirm,
            activityCardDeleteDialogTitle = activityCardDeleteDialogTitle,
            activityCardDeleteDialogSummary = context.getString(
                R.string.os_activity_card_delete_dialog_summary,
                activityShortcutDraft.title.ifBlank { googleSystemServiceDefaultTitle }
            ),
            onDismissActivityCardDeleteConfirm = { showActivityCardDeleteConfirm = false },
            onConfirmActivityCardDelete = {
                val targetId = editingActivityShortcutCardId.orEmpty().trim()
                showActivityCardDeleteConfirm = false
                if (targetId.isBlank()) return@OsPageOverlaySheets
                val updatedCards = activityShortcutCards.filterNot { card -> card.id == targetId }
                activityShortcutCards = updatedCards
                activityCardExpanded.remove(targetId)
                OsActivityShortcutCardStore.saveCards(
                    cards = updatedCards,
                    defaults = googleSystemServiceDefaults
                )
                editingActivityShortcutCardId = null
                showActivityShortcutEditor = false
                showActivitySuggestionSheet = false
                editingActivityShortcutBuiltIn = false
                Toast.makeText(context, activityCardDeletedToast, Toast.LENGTH_SHORT).show()
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
            shellCommandCards = shellCommandCards,
            shellCommandCardExpanded = shellCommandCardExpanded,
            runningShellCommandCardIds = runningShellCommandCardIds,
            onShellCommandCardExpandedChange = { cardId, expanded ->
                shellCommandCardExpanded[cardId] = expanded
            },
            onOpenShellCommandCardEditor = { card ->
                editingShellCommandCardId = card.id
                shellCommandCardDraft = card
                showShellCommandCardEditor = true
            },
            onRunShellCommandCard = { card ->
                scope.launch { runShellCommandCard(card) }
            },
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
                editingActivityShortcutBuiltIn = card.isBuiltInSample
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
            showFloatingAddButton = !showActivityShortcutEditor &&
                    !showActivitySuggestionSheet &&
                    !showShellCommandCardEditor &&
                    !showShellCardVisibilityManager,
            onOpenAddActivityShortcutCard = {
                activityCardEditMode = OsActivityCardEditMode.Add
                editingActivityShortcutCardId = null
                editingActivityShortcutBuiltIn = false
                activityShortcutDraft =
                    createDefaultActivityShortcutDraft(googleSystemServiceDefaults)
                showActivityShortcutEditor = true
            }
        )
    }
}
