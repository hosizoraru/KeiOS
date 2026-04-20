package com.example.keios.ui.page.main

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.keios.R
import com.example.keios.mcp.notification.McpNotificationHelper
import com.example.keios.mcp.server.McpServerManager
import com.example.keios.ui.navigation.KeiosRoute
import com.example.keios.ui.navigation.Navigator
import com.example.keios.ui.page.main.about.AboutPage
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.student.fetch.extractGuideContentIdFromUrl
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import com.example.keios.ui.perf.ReportPagerPerformanceState
import com.example.keios.ui.page.main.widget.motion.AppMotionTokens
import com.example.keios.ui.page.main.widget.glass.UiPerformanceBudget
import com.example.keios.ui.page.main.widget.chrome.LiquidGlassBottomBar
import com.example.keios.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import com.example.keios.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import com.example.keios.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.motion.appFloatingEnter
import com.example.keios.ui.page.main.widget.motion.appFloatingExit
import com.example.keios.ui.page.main.widget.motion.resolvedMotionDuration
import com.example.keios.core.log.AppLogger
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import com.example.keios.ui.page.main.mcp.McpPage
import com.example.keios.ui.page.main.os.OsPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MainScreen(
    appLabel: String,
    packageInfo: PackageInfo?,
    shizukuStatus: String,
    onCheckOrRequestShizuku: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    shizukuApiUtils: ShizukuApiUtils,
    mcpServerManager: McpServerManager,
    appThemeMode: AppThemeMode,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int,
    requestedGitHubRefreshToken: Int,
    onRequestedBottomPageConsumed: () -> Unit
) {
    val backStack = remember { mutableStateListOf<NavKey>().apply { add(KeiosRoute.Main) } }
    val navigator = remember { Navigator(backStack) }
    val context = LocalContext.current
    val currentAppLabel by rememberUpdatedState(appLabel)
    val currentPackageInfo by rememberUpdatedState(packageInfo)
    val currentShizukuStatus by rememberUpdatedState(shizukuStatus)
    val currentNotificationPermissionGranted by rememberUpdatedState(notificationPermissionGranted)
    val currentAppThemeMode by rememberUpdatedState(appThemeMode)
    val currentOnCheckOrRequestShizuku by rememberUpdatedState(onCheckOrRequestShizuku)
    val currentOnRequestNotificationPermission by rememberUpdatedState(onRequestNotificationPermission)
    val currentOnAppThemeModeChanged by rememberUpdatedState(onAppThemeModeChanged)
    var mainResumeFromSettingsToken by rememberSaveable { mutableIntStateOf(0) }
    var previousTopRoute by remember { mutableStateOf<NavKey?>(null) }

    LaunchedEffect(backStack.lastOrNull()) {
        val currentTopRoute = backStack.lastOrNull()
        if (previousTopRoute == KeiosRoute.Settings && currentTopRoute == KeiosRoute.Main) {
            mainResumeFromSettingsToken++
        }
        previousTopRoute = currentTopRoute
    }

    LaunchedEffect(requestedBottomPageToken, requestedBottomPage) {
        if (requestedBottomPage.isNullOrBlank()) return@LaunchedEffect
        navigator.popUntil { it == KeiosRoute.Main }
    }

    val uiPrefsSnapshot by produceState(
        initialValue = UiPrefs.defaultSnapshot(currentAppThemeMode)
    ) {
        value = withContext(Dispatchers.IO) { UiPrefs.loadSnapshot() }
    }
    var liquidBottomBarEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.liquidBottomBarEnabled) }
    var liquidActionBarLayeredStyleEnabled by remember(uiPrefsSnapshot) {
        mutableStateOf(uiPrefsSnapshot.liquidActionBarLayeredStyleEnabled)
    }
    var liquidGlassSwitchEnabled by remember(uiPrefsSnapshot) {
        mutableStateOf(uiPrefsSnapshot.liquidGlassSwitchEnabled)
    }
    var transitionAnimationsEnabled by remember(uiPrefsSnapshot) {
        mutableStateOf(uiPrefsSnapshot.transitionAnimationsEnabled)
    }
    var cardPressFeedbackEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.cardPressFeedbackEnabled) }
    var homeIconHdrEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.homeIconHdrEnabled) }
    var preloadingEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.preloadingEnabled) }
    var nonHomeBackgroundEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.nonHomeBackgroundEnabled) }
    var nonHomeBackgroundUri by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.nonHomeBackgroundUri) }
    var nonHomeBackgroundOpacity by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.nonHomeBackgroundOpacity) }
    var superIslandNotificationEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.superIslandNotificationEnabled) }
    var superIslandBypassRestrictionEnabled by remember(uiPrefsSnapshot) {
        mutableStateOf(uiPrefsSnapshot.superIslandBypassRestrictionEnabled)
    }
    var logDebugEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.logDebugEnabled) }
    var textCopyCapabilityExpanded by remember(uiPrefsSnapshot) {
        mutableStateOf(uiPrefsSnapshot.textCopyCapabilityExpanded)
    }
    var cacheDiagnosticsEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.cacheDiagnosticsEnabled) }
    var visibleBottomPageNames by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.visibleBottomPageNames) }
    val view = LocalView.current
    val poolGuideMissingText = stringResource(R.string.main_toast_pool_guide_missing)

    val openGuideDetail: (String) -> Unit = { rawUrl ->
        val normalized = normalizeGuideUrl(rawUrl)
        val contentId = if (normalized.isBlank()) null else extractGuideContentIdFromUrl(normalized)
        if (contentId == null || contentId <= 0L) {
            Toast.makeText(context, poolGuideMissingText, Toast.LENGTH_SHORT).show()
        } else {
            val canonicalGuideUrl = "https://www.gamekee.com/ba/tj/$contentId.html"
            BaStudentGuideStore.setCurrentUrl(canonicalGuideUrl)
            navigator.push(KeiosRoute.BaStudentGuide(nonce = System.nanoTime()))
        }
    }
    if (!view.isInEditMode) {
        SideEffect {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@SideEffect
            val activity = view.context as? Activity ?: return@SideEffect
            runCatching {
                activity.window.colorMode = if (homeIconHdrEnabled) {
                    ActivityInfo.COLOR_MODE_HDR
                } else {
                    ActivityInfo.COLOR_MODE_DEFAULT
                }
            }
        }
    }

    val entryProvider = entryProvider<NavKey> {
            entry<KeiosRoute.Main> {
                // Removed the dangerous isTopRoute check. The route should manage its own state naturally.
                MainPagerLayout(
                    navigator = navigator,
                    settingsReturnToken = mainResumeFromSettingsToken,
                    liquidBottomBarEnabled = liquidBottomBarEnabled,
                    liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                    cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                    homeIconHdrEnabled = homeIconHdrEnabled,
                    preloadingEnabled = preloadingEnabled,
                    nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
                    nonHomeBackgroundUri = nonHomeBackgroundUri,
                    nonHomeBackgroundOpacity = nonHomeBackgroundOpacity,
                    visibleBottomPageNames = visibleBottomPageNames,
                    onVisibleBottomPageNamesChange = { names ->
                        visibleBottomPageNames = names
                        UiPrefs.saveVisibleBottomPageNames(names)
                    },
                    appLabel = currentAppLabel,
                    packageInfo = currentPackageInfo,
                    shizukuStatus = currentShizukuStatus,
                    onCheckOrRequestShizuku = currentOnCheckOrRequestShizuku,
                        notificationPermissionGranted = currentNotificationPermissionGranted,
                        shizukuApiUtils = shizukuApiUtils,
                    mcpServerManager = mcpServerManager,
                    onOpenGuideDetail = openGuideDetail,
                    requestedBottomPage = requestedBottomPage,
                    requestedBottomPageToken = requestedBottomPageToken,
                    requestedGitHubRefreshToken = requestedGitHubRefreshToken,
                    onRequestedBottomPageConsumed = onRequestedBottomPageConsumed
                )
            }
            entry<KeiosRoute.Settings> {
                SettingsPage(
                    liquidBottomBarEnabled = liquidBottomBarEnabled,
                    onLiquidBottomBarChanged = {
                        liquidBottomBarEnabled = it
                        UiPrefs.setLiquidBottomBarEnabled(it)
                    },
                    liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                    onLiquidActionBarLayeredStyleChanged = {
                        liquidActionBarLayeredStyleEnabled = it
                        UiPrefs.setLiquidActionBarLayeredStyleEnabled(it)
                    },
                    liquidGlassSwitchEnabled = liquidGlassSwitchEnabled,
                    onLiquidGlassSwitchChanged = {
                        liquidGlassSwitchEnabled = it
                        UiPrefs.setLiquidGlassSwitchEnabled(it)
                    },
                    transitionAnimationsEnabled = transitionAnimationsEnabled,
                    onTransitionAnimationsChanged = {
                        transitionAnimationsEnabled = it
                        UiPrefs.setTransitionAnimationsEnabled(it)
                    },
                    cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                    onCardPressFeedbackChanged = {
                        cardPressFeedbackEnabled = it
                        UiPrefs.setCardPressFeedbackEnabled(it)
                    },
                    homeIconHdrEnabled = homeIconHdrEnabled,
                    onHomeIconHdrChanged = {
                        homeIconHdrEnabled = it
                        UiPrefs.setHomeIconHdrEnabled(it)
                    },
                    preloadingEnabled = preloadingEnabled,
                    onPreloadingEnabledChanged = {
                        preloadingEnabled = it
                        UiPrefs.setPreloadingEnabled(it)
                    },
                    nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
                    onNonHomeBackgroundEnabledChanged = {
                        nonHomeBackgroundEnabled = it
                        UiPrefs.setNonHomeBackgroundEnabled(it)
                    },
                    nonHomeBackgroundUri = nonHomeBackgroundUri,
                    onNonHomeBackgroundUriChanged = {
                        nonHomeBackgroundUri = it
                        UiPrefs.setNonHomeBackgroundUri(it)
                    },
                    nonHomeBackgroundOpacity = nonHomeBackgroundOpacity,
                    onNonHomeBackgroundOpacityChanged = {
                        nonHomeBackgroundOpacity = it
                        UiPrefs.setNonHomeBackgroundOpacity(it)
                    },
                    superIslandNotificationEnabled = superIslandNotificationEnabled,
                    onSuperIslandNotificationChanged = {
                        superIslandNotificationEnabled = it
                        UiPrefs.setSuperIslandNotificationEnabled(it)
                        mcpServerManager.refreshNotificationNow()
                        McpNotificationHelper.refreshCurrentNotificationStyle(view.context.applicationContext)
                    },
                    superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled,
                    onSuperIslandBypassRestrictionChanged = {
                        superIslandBypassRestrictionEnabled = it
                        UiPrefs.setSuperIslandBypassRestrictionEnabled(it)
                        mcpServerManager.refreshNotificationNow()
                        McpNotificationHelper.refreshCurrentNotificationStyle(view.context.applicationContext)
                    },
                    logDebugEnabled = logDebugEnabled,
                    onLogDebugChanged = {
                        logDebugEnabled = it
                        UiPrefs.setLogDebugEnabled(it)
                        AppLogger.setDebugEnabled(it)
                    },
                    textCopyCapabilityExpanded = textCopyCapabilityExpanded,
                    onTextCopyCapabilityExpandedChanged = {
                        textCopyCapabilityExpanded = it
                        UiPrefs.setTextCopyCapabilityExpanded(it)
                    },
                    cacheDiagnosticsEnabled = cacheDiagnosticsEnabled,
                    onCacheDiagnosticsChanged = {
                        cacheDiagnosticsEnabled = it
                        UiPrefs.setCacheDiagnosticsEnabled(it)
                    },
                    appThemeMode = currentAppThemeMode,
                    onAppThemeModeChanged = currentOnAppThemeModeChanged,
                    onBack = { navigator.pop() }
                )
            }
            entry<KeiosRoute.McpSkill> {
                McpSkillPage(
                    mcpServerManager = mcpServerManager,
                    onBack = { navigator.pop() }
                )
            }
            entry<KeiosRoute.About> {
                AboutPage(
                    appLabel = currentAppLabel,
                    packageInfo = currentPackageInfo,
                    notificationPermissionGranted = currentNotificationPermissionGranted,
                    shizukuStatus = currentShizukuStatus,
                    shizukuApiUtils = shizukuApiUtils,
                    onCheckShizuku = currentOnCheckOrRequestShizuku,
                    onBack = { navigator.pop() }
                )
            }
            entry<KeiosRoute.BaStudentGuide> {
                BaStudentGuidePage(
                    liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                    preloadingEnabled = preloadingEnabled,
                    onBack = { navigator.pop() }
                )
            }
            entry<KeiosRoute.BaGuideCatalog> {
                BaGuideCatalogPage(
                    liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                    preloadingEnabled = preloadingEnabled,
                    onBack = { navigator.pop() },
                    onOpenGuide = { sourceUrl ->
                        openGuideDetail(sourceUrl)
                    }
                )
            }
        }
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider,
    )

    CompositionLocalProvider(LocalTransitionAnimationsEnabled provides transitionAnimationsEnabled) {
        NavDisplay(
            entries = entries,
            onBack = { navigator.pop() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MainPagerLayout(
    navigator: Navigator,
    settingsReturnToken: Int,
    liquidBottomBarEnabled: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean,
    cardPressFeedbackEnabled: Boolean,
    homeIconHdrEnabled: Boolean,
    preloadingEnabled: Boolean,
    nonHomeBackgroundEnabled: Boolean,
    nonHomeBackgroundUri: String,
    nonHomeBackgroundOpacity: Float,
    visibleBottomPageNames: Set<String>,
    onVisibleBottomPageNamesChange: (Set<String>) -> Unit,
    appLabel: String,
    packageInfo: PackageInfo?,
    shizukuStatus: String,
    onCheckOrRequestShizuku: () -> Unit,
    notificationPermissionGranted: Boolean,
    shizukuApiUtils: ShizukuApiUtils,
    mcpServerManager: McpServerManager,
    onOpenGuideDetail: (String) -> Unit,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int,
    requestedGitHubRefreshToken: Int,
    onRequestedBottomPageConsumed: () -> Unit
) {
    val context = LocalContext.current
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val preloadPolicy = remember(preloadingEnabled) {
        UiPerformanceBudget.resolvePreloadPolicy(preloadingEnabled)
    }
    val effectiveNonHomeBackgroundUri = remember(nonHomeBackgroundEnabled, nonHomeBackgroundUri) {
        if (nonHomeBackgroundEnabled) nonHomeBackgroundUri.trim() else ""
    }
    val hasNonHomeBackground = remember(effectiveNonHomeBackgroundUri) {
        effectiveNonHomeBackgroundUri.isNotBlank()
    }
    val tabs = remember(visibleBottomPageNames) {
        BottomPage.entries.filter { page ->
            page == BottomPage.Home || visibleBottomPageNames.contains(page.name)
        }
    }
    val initialPageIndex = remember(tabs, requestedBottomPage, requestedBottomPageToken) {
        val target = requestedBottomPage?.trim().orEmpty()
        tabs.indexOfFirst { it.name == target }
            .takeIf { it >= 0 }
            ?: 0
    }
    val visibleTabsSnapshot = remember(tabs) { tabs.toSet() }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()
    var tabJumpJob by remember { mutableStateOf<Job?>(null) }

    var osScrollToTopSignal by remember { mutableIntStateOf(0) }
    var baScrollToTopSignal by remember { mutableIntStateOf(0) }
    var mcpScrollToTopSignal by remember { mutableIntStateOf(0) }
    var githubScrollToTopSignal by remember { mutableIntStateOf(0) }
    var pagerScrollEnabled by remember { mutableStateOf(true) }
    val farJumpAlpha = remember { Animatable(1f) }
    var temporaryBeyondViewportCount by remember { mutableStateOf<Int?>(null) }
    var homeGitHubOverview by remember { mutableStateOf(HomeGitHubOverview()) }
    var homeBaOverview by remember { mutableStateOf(HomeBaOverview()) }
    suspend fun refreshHomeOverviewState(reason: String) {
        val (baOverview, githubOverview) = withContext(Dispatchers.IO) {
            val loadedBaOverview = runCatching { loadHomeBaOverview() }
                .onFailure { error ->
                    AppLogger.w(
                        "MainScreen",
                        "loadHomeBaOverview failed (reason=$reason)",
                        error
                    )
                }
                .getOrElse { HomeBaOverview(loaded = true) }
            val loadedGitHubOverview = runCatching { loadHomeGitHubOverview() }
                .onFailure { error ->
                    AppLogger.w(
                        "MainScreen",
                        "loadHomeGitHubOverview failed (reason=$reason)",
                        error
                    )
                }
                .getOrElse { HomeGitHubOverview(loaded = true) }
            loadedBaOverview to loadedGitHubOverview
        }
        homeBaOverview = baOverview
        homeGitHubOverview = githubOverview
    }
    val shouldRenderNonHomeBackground by remember(hasNonHomeBackground, tabs, pagerState) {
        derivedStateOf {
            if (!hasNonHomeBackground) return@derivedStateOf false
            val current = tabs.getOrElse(pagerState.currentPage) { BottomPage.Home }
            val target = tabs.getOrElse(pagerState.targetPage) { BottomPage.Home }
            val settled = tabs.getOrElse(pagerState.settledPage) { BottomPage.Home }
            current != BottomPage.Home || target != BottomPage.Home || settled != BottomPage.Home
        }
    }
    val resolveWarmActive: (Int) -> Boolean = { pageIndex ->
        val current = pageIndex == pagerState.currentPage
        val target = pageIndex == pagerState.targetPage
        val settled = pageIndex == pagerState.settledPage
        if (pagerState.isScrollInProgress) {
            current || target
        } else {
            current || settled || (preloadPolicy.includeTargetPageInHeavyRender && target)
        }
    }
    val resolveDataActive: (Int) -> Boolean = { pageIndex ->
        pageIndex == pagerState.settledPage
    }
    val resolvedMainPagerBeyondViewportCount = if (
        temporaryBeyondViewportCount == null && pagerState.isScrollInProgress
    ) {
        maxOf(
            UiPerformanceBudget.mainPagerBeyondViewportPageCount,
            preloadPolicy.mainPagerBeyondViewportPageCount - 1
        )
    } else {
        temporaryBeyondViewportCount ?: preloadPolicy.mainPagerBeyondViewportPageCount
    }
    LaunchedEffect(settingsReturnToken, transitionAnimationsEnabled) {
        if (settingsReturnToken <= 0) return@LaunchedEffect
        temporaryBeyondViewportCount = 0
        withFrameNanos { }
        delay(resolvedMotionDuration(220, transitionAnimationsEnabled).toLong())
        temporaryBeyondViewportCount = null
    }
    LaunchedEffect(Unit) {
        if (homeBaOverview.loaded && homeGitHubOverview.loaded) return@LaunchedEffect
        refreshHomeOverviewState(reason = "initial")
    }
    LaunchedEffect(settingsReturnToken) {
        if (settingsReturnToken <= 0) return@LaunchedEffect
        refreshHomeOverviewState(reason = "settings_return_$settingsReturnToken")
    }
    val farJumpBefore: suspend () -> Unit = {
        if (!transitionAnimationsEnabled) {
            farJumpAlpha.snapTo(1f)
        } else {
            farJumpAlpha.snapTo(1f)
            farJumpAlpha.animateTo(
                targetValue = 0.92f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(
                        AppMotionTokens.farJumpDimMs,
                        transitionAnimationsEnabled
                    )
                )
            )
        }
    }
    val farJumpAfter: suspend () -> Unit = {
        if (!transitionAnimationsEnabled) {
            farJumpAlpha.snapTo(1f)
        } else {
            farJumpAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(
                        AppMotionTokens.farJumpRestoreMs,
                        transitionAnimationsEnabled
                    )
                )
            )
        }
    }
    ReportPagerPerformanceState(
        scope = "main_pager",
        currentPage = tabs.getOrElse(pagerState.currentPage) { BottomPage.Home }.name,
        targetPage = tabs.getOrElse(pagerState.targetPage) { BottomPage.Home }.name,
        scrolling = pagerState.isScrollInProgress
    )

    var showBottomBar by remember { mutableStateOf(true) }
    val homePageBottomBarPinned by remember(tabs, pagerState) {
        derivedStateOf {
            val current = tabs.getOrElse(pagerState.currentPage) { BottomPage.Home }
            val target = tabs.getOrElse(pagerState.targetPage) { BottomPage.Home }
            val settled = tabs.getOrElse(pagerState.settledPage) { BottomPage.Home }
            current == BottomPage.Home || target == BottomPage.Home || settled == BottomPage.Home
        }
    }
    LaunchedEffect(homePageBottomBarPinned) {
        if (homePageBottomBarPinned && !showBottomBar) {
            showBottomBar = true
        }
    }
    val nestedScrollConnection = remember(homePageBottomBarPinned) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (homePageBottomBarPinned) {
                    if (!showBottomBar) {
                        showBottomBar = true
                    }
                    return Offset.Zero
                }
                if (available.y < -1f && showBottomBar) {
                    showBottomBar = false
                }
                if (available.y > 1f && !showBottomBar) {
                    showBottomBar = true
                }
                return Offset.Zero
            }
        }
    }

    // Keep the backdrop stable during in-app route switches.
    // Rebuild only after real app background -> foreground transitions.
    var backdropGeneration by rememberSaveable { mutableIntStateOf(0) }
    val activityLifecycle = remember(context) { (context as? ComponentActivity)?.lifecycle }
    DisposableEffect(activityLifecycle) {
        val lifecycle = activityLifecycle ?: return@DisposableEffect onDispose { }
        var appWentBackground = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> appWentBackground = true
                Lifecycle.Event.ON_START -> {
                    if (appWentBackground) {
                        backdropGeneration++
                        appWentBackground = false
                    }
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = key("main-backdrop-$backdropGeneration") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }

    val density = LocalDensity.current
    val navigationBarBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val bottomOverlayPadding = 112.dp + navigationBarBottom
    val systemInsets = WindowInsets.safeDrawing.union(WindowInsets.navigationBars).asPaddingValues()
    val homeTopInset = systemInsets.calculateTopPadding()
    val homeBottomInset = systemInsets.calculateBottomPadding()

    LaunchedEffect(tabs.size) {
        val lastIndex = tabs.lastIndex
        if (lastIndex >= 0 && pagerState.currentPage > lastIndex) {
            pagerState.scrollToPage(lastIndex)
        }
    }

    val handlePageSelected: (Int) -> Unit = { index ->
        if (index in tabs.indices) {
            val stablePageIndex = if (pagerState.isScrollInProgress) {
                pagerState.targetPage
            } else {
                pagerState.settledPage
            }
            showBottomBar = true
            if (index != stablePageIndex || pagerState.isScrollInProgress) {
                tabJumpJob?.cancel()
                tabJumpJob = coroutineScope.launch {
                    pagerState.animateTabSwitch(
                        fromIndex = stablePageIndex,
                        targetIndex = index,
                        animationsEnabled = transitionAnimationsEnabled,
                        onFarJumpBefore = farJumpBefore,
                        onFarJumpAfter = farJumpAfter
                    )
                }
            }
        }
    }
    LaunchedEffect(requestedBottomPageToken, requestedBottomPage, tabs) {
        val target = requestedBottomPage ?: return@LaunchedEffect
        val index = tabs.indexOfFirst { it.name == target }
        val stablePageIndex = if (pagerState.isScrollInProgress) {
            pagerState.targetPage
        } else {
            pagerState.settledPage
        }
        if (index >= 0 && index != stablePageIndex) {
            tabJumpJob?.cancel()
            tabJumpJob = coroutineScope.launch {
                pagerState.animateTabSwitch(
                    fromIndex = stablePageIndex,
                    targetIndex = index,
                    animationsEnabled = transitionAnimationsEnabled,
                    onFarJumpBefore = farJumpBefore,
                    onFarJumpAfter = farJumpAfter
                )
            }
            showBottomBar = true
        }
        onRequestedBottomPageConsumed()
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection),
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = showBottomBar,
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
                        tabs.forEachIndexed { index, page ->
                            val selected = pagerState.targetPage == index
                            val tabColor = liquidGlassBottomBarItemContentColor(index)
                            LiquidGlassBottomBarItem(
                                selected = selected,
                                tabIndex = index,
                                onClick = { handlePageSelected(index) },
                                modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                            ) {
                                val tabIconModifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = page.iconScale
                                        scaleY = page.iconScale
                                    }
                                if (page.iconRes != null) {
                                    Icon(
                                        painter = painterResource(id = page.iconRes),
                                        contentDescription = page.label,
                                        tint = if (page.keepOriginalColors) Color.Unspecified else tabColor,
                                        modifier = tabIconModifier
                                    )
                                } else {
                                    page.icon?.let { icon ->
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = page.label,
                                            tint = tabColor,
                                            modifier = tabIconModifier
                                        )
                                    }
                                }
                                Text(
                                    text = page.label,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = tabColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }

                    LiquidGlassBottomBar(
                        modifier = bottomBarModifier,
                        selectedIndex = pagerState.targetPage,
                        onSelected = { index ->
                            if (index != pagerState.targetPage) {
                                handlePageSelected(index)
                            }
                        },
                        backdrop = backdrop,
                        tabsCount = tabs.size,
                        reduceEffectsDuringPagerScroll = pagerState.isScrollInProgress,
                        isLiquidEffectEnabled = liquidBottomBarEnabled,
                        content = bottomBarTabs
                    )
                }
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                key = { index -> tabs[index].name },
                userScrollEnabled = pagerScrollEnabled,
                overscrollEffect = null,
                beyondViewportPageCount = resolvedMainPagerBeyondViewportCount,
                // CRITICAL FIX: NEVER conditionally unmount layerBackdrop.
                // If the node is visible (even during an exit animation), it MUST have the backdrop attached,
                // otherwise consumer composables will attempt to draw a detached Native pointer causing SIGSEGV.
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = farJumpAlpha.value }
                    .layerBackdrop(backdrop)
            ) { pageIndex ->

                val currentPageType = tabs[pageIndex]
                val isHome = currentPageType == BottomPage.Home
                val isTopBarManagedPage = currentPageType == BottomPage.Os ||
                    currentPageType == BottomPage.Ba ||
                    currentPageType == BottomPage.Mcp ||
                    currentPageType == BottomPage.GitHub

                val pageHorizontalPadding = when {
                    isHome -> 0.dp
                    isTopBarManagedPage -> 0.dp
                    else -> 18.dp
                }
                val contentInsets = when {
                    isHome -> PaddingValues(0.dp)
                    isTopBarManagedPage -> PaddingValues(0.dp)
                    else -> systemInsets
                }
                val topSpacerPadding = when {
                    isHome -> 0.dp
                    isTopBarManagedPage -> 0.dp
                    else -> 14.dp
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = pageHorizontalPadding)
                        .padding(contentInsets)
                        .padding(top = topSpacerPadding)
                ) {
                    when (currentPageType) {
                        BottomPage.Home -> {
                            val mcpUiState by mcpServerManager.uiState.collectAsState()
                            HomePage(
                                shizukuStatus = shizukuStatus,
                                mcpRunning = mcpUiState.running,
                                mcpRunningSinceEpochMs = mcpUiState.runningSinceEpochMs,
                                mcpPort = mcpUiState.port,
                                mcpEndpointPath = mcpUiState.endpointPath,
                                mcpServerName = mcpUiState.serverName,
                                mcpAuthTokenConfigured = mcpUiState.authToken.isNotBlank(),
                                mcpConnectedClients = mcpUiState.connectedClients,
                                mcpAllowExternal = mcpUiState.allowExternal,
                                homeGitHubOverview = homeGitHubOverview,
                                homeBaOverview = homeBaOverview,
                                homeIconHdrEnabled = homeIconHdrEnabled,
                                liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                                mainPagerScrollInProgress = pagerState.isScrollInProgress,
                                visibleBottomPages = visibleTabsSnapshot,
                                onBottomPageVisibilityChange = { page, visible ->
                                    if (page == BottomPage.Home) return@HomePage
                                    val updated = visibleBottomPageNames
                                        .toMutableSet()
                                        .apply {
                                            if (visible) add(page.name) else remove(page.name)
                                        }
                                        .toSet()
                                    onVisibleBottomPageNamesChange(updated)
                                },
                                onOpenSettings = { navigator.pushSingleTop(KeiosRoute.Settings) },
                                onOpenAbout = { navigator.pushSingleTop(KeiosRoute.About) },
                                onActionBarInteractingChanged = { interacting ->
                                    pagerScrollEnabled = !interacting
                                },
                                contentTopPadding = homeTopInset,
                                contentBottomPadding = homeBottomInset
                            )
                        }
                        BottomPage.Os -> {
                            val isWarmActive = resolveWarmActive(pageIndex)
                            val isDataActive = resolveDataActive(pageIndex)
                            OsPage(
                                scrollToTopSignal = osScrollToTopSignal,
                                isPageActive = isWarmActive,
                                isDataActive = isDataActive,
                                shizukuStatus = shizukuStatus,
                                shizukuApiUtils = shizukuApiUtils,
                                cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                                liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                                mainPagerScrollInProgress = pagerState.isScrollInProgress,
                                contentBottomPadding = bottomOverlayPadding,
                                onActionBarInteractingChanged = { interacting ->
                                    pagerScrollEnabled = !interacting
                                }
                            )
                        }
                        BottomPage.Ba -> {
                            val isWarmActive = resolveWarmActive(pageIndex)
                            val isDataActive = resolveDataActive(pageIndex)
                            BAPage(
                                contentBottomPadding = bottomOverlayPadding,
                                scrollToTopSignal = baScrollToTopSignal,
                                isPageActive = isWarmActive,
                                isDataActive = isDataActive,
                                preloadingEnabled = preloadingEnabled,
                                cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                                liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                                mainPagerScrollInProgress = pagerState.isScrollInProgress,
                                onOpenPoolStudentGuide = { sourceUrl ->
                                    onOpenGuideDetail(sourceUrl)
                                },
                                onOpenGuideCatalog = {
                                    navigator.pushSingleTop(KeiosRoute.BaGuideCatalog)
                                },
                                onActionBarInteractingChanged = { interacting ->
                                    pagerScrollEnabled = !interacting
                                }
                            )
                        }
                        BottomPage.Mcp -> {
                            val isWarmActive = resolveWarmActive(pageIndex)
                            McpPage(
                                mcpServerManager = mcpServerManager,
                                contentBottomPadding = bottomOverlayPadding,
                                scrollToTopSignal = mcpScrollToTopSignal,
                                isPageActive = isWarmActive,
                                cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                                liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                                mainPagerScrollInProgress = pagerState.isScrollInProgress,
                                onOpenSkill = { navigator.pushSingleTop(KeiosRoute.McpSkill) },
                                onActionBarInteractingChanged = { interacting ->
                                    pagerScrollEnabled = !interacting
                                }
                            )
                        }
                        BottomPage.GitHub -> {
                            val isWarmActive = resolveWarmActive(pageIndex)
                            GitHubPage(
                                contentBottomPadding = bottomOverlayPadding,
                                scrollToTopSignal = githubScrollToTopSignal,
                                isPageActive = isWarmActive,
                                externalRefreshTriggerToken = requestedGitHubRefreshToken,
                                cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                                liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                                mainPagerScrollInProgress = pagerState.isScrollInProgress,
                                onActionBarInteractingChanged = { interacting ->
                                    pagerScrollEnabled = !interacting
                                }
                            )
                        }
                    }
                }
            }

            if (shouldRenderNonHomeBackground) {
                // Keep one global foreground overlay for non-home pages.
                // This avoids N-times duplicated AsyncImage work in offscreen pager pages.
                NonHomePageBackground(
                    enabled = hasNonHomeBackground,
                    imageUri = effectiveNonHomeBackgroundUri,
                    opacity = nonHomeBackgroundOpacity,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NonHomePageBackground(
    enabled: Boolean,
    imageUri: String,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    if (!enabled || imageUri.isBlank()) return
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val (targetWidthPx, targetHeightPx) = remember(configuration, density) {
        with(density) {
            val width = configuration.screenWidthDp.dp.roundToPx().coerceAtLeast(1)
            val height = configuration.screenHeightDp.dp.roundToPx().coerceAtLeast(1)
            width to height
        }
    }
    val request: ImageRequest = remember(imageUri, targetWidthPx, targetHeightPx) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .size(targetWidthPx, targetHeightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = opacity.coerceIn(0f, 1f),
        modifier = modifier
    )
}
