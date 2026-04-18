package com.example.keios.ui.page.main

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.example.keios.R
import com.example.keios.mcp.McpNotificationHelper
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.navigation.KeiosRoute
import com.example.keios.ui.navigation.Navigator
import com.example.keios.ui.page.main.about.AboutPage
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.student.extractGuideContentIdFromUrl
import com.example.keios.ui.page.main.student.normalizeGuideUrl
import com.example.keios.ui.perf.ReportPagerPerformanceState
import com.example.keios.ui.page.main.widget.AppMotionTokens
import com.example.keios.ui.page.main.widget.UiPerformanceBudget
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.ui.page.main.widget.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.appFloatingEnter
import com.example.keios.ui.page.main.widget.appFloatingExit
import com.example.keios.ui.page.main.widget.resolvedMotionDuration
import com.example.keios.core.log.AppLogger
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    var transitionAnimationsEnabled by remember(uiPrefsSnapshot) {
        mutableStateOf(uiPrefsSnapshot.transitionAnimationsEnabled)
    }
    var cardPressFeedbackEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.cardPressFeedbackEnabled) }
    var homeIconHdrEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.homeIconHdrEnabled) }
    var nonHomeBackgroundEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.nonHomeBackgroundEnabled) }
    var nonHomeBackgroundUri by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.nonHomeBackgroundUri) }
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
                    liquidBottomBarEnabled = liquidBottomBarEnabled,
                    liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                    cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                    homeIconHdrEnabled = homeIconHdrEnabled,
                    nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
                    nonHomeBackgroundUri = nonHomeBackgroundUri,
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
                    onBack = { navigator.pop() }
                )
            }
            entry<KeiosRoute.BaGuideCatalog> {
                BaGuideCatalogPage(
                    liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
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
    liquidBottomBarEnabled: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean,
    cardPressFeedbackEnabled: Boolean,
    homeIconHdrEnabled: Boolean,
    nonHomeBackgroundEnabled: Boolean,
    nonHomeBackgroundUri: String,
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
    onRequestedBottomPageConsumed: () -> Unit
) {
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val tabs = remember(visibleBottomPageNames) {
        BottomPage.entries.filter { page ->
            page == BottomPage.Home || visibleBottomPageNames.contains(page.name)
        }
    }
    val visibleTabsSnapshot = remember(tabs) { tabs.toSet() }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var tabJumpJob by remember { mutableStateOf<Job?>(null) }
    val mcpUiState by mcpServerManager.uiState.collectAsState()

    var osScrollToTopSignal by remember { mutableIntStateOf(0) }
    var baScrollToTopSignal by remember { mutableIntStateOf(0) }
    var mcpScrollToTopSignal by remember { mutableIntStateOf(0) }
    var githubScrollToTopSignal by remember { mutableIntStateOf(0) }
    var pagerScrollEnabled by remember { mutableStateOf(true) }
    val farJumpAlpha = remember { Animatable(1f) }
    ReportPagerPerformanceState(
        scope = "main_pager",
        currentPage = tabs.getOrElse(pagerState.currentPage) { BottomPage.Home }.name,
        targetPage = tabs.getOrElse(pagerState.targetPage) { BottomPage.Home }.name,
        scrolling = pagerState.isScrollInProgress
    )

    var showBottomBar by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) showBottomBar = false
                if (available.y > 1f) showBottomBar = true
                return Offset.Zero
            }
        }
    }

    // This is the ONLY hack needed: track ReusableContentHost lifecycle to prevent memory leaks or stale nodes.
    // It forces a fresh GraphicsLayer allocation when returning from the background.
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = key(activationCount) {
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
            val selected = tabs[index]
            val stablePageIndex = if (pagerState.isScrollInProgress) {
                pagerState.targetPage
            } else {
                pagerState.settledPage
            }
            showBottomBar = true
            if (index == stablePageIndex && !pagerState.isScrollInProgress) {
                when (selected) {
                    BottomPage.Os -> osScrollToTopSignal++
                    BottomPage.Ba -> baScrollToTopSignal++
                    BottomPage.Mcp -> mcpScrollToTopSignal++
                    BottomPage.GitHub -> githubScrollToTopSignal++
                    else -> {}
                }
            } else {
                tabJumpJob?.cancel()
                tabJumpJob = coroutineScope.launch {
                    pagerState.animateTabSwitch(
                        fromIndex = stablePageIndex,
                        targetIndex = index,
                        animationsEnabled = transitionAnimationsEnabled,
                        onFarJumpBefore = {
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
                        },
                        onFarJumpAfter = {
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
                    onFarJumpBefore = {
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
                    },
                    onFarJumpAfter = {
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
                    FloatingBottomBar(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 12.dp + navigationBarBottom
                            ),
                        selectedIndex = { pagerState.targetPage },
                        onSelected = { index ->
                            // Ignore mirror callbacks emitted after pager page sync.
                            // Keep explicit tab click behavior (including reselect-to-top) unchanged.
                            if (index != pagerState.targetPage) {
                                handlePageSelected(index)
                            }
                        },
                        backdrop = backdrop,
                        tabsCount = tabs.size,
                        isBlurEnabled = liquidBottomBarEnabled
                    ) {
                        tabs.forEachIndexed { index, page ->
                            FloatingBottomBarItem(
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
                                        tint = if (page.keepOriginalColors) Color.Unspecified else MiuixTheme.colorScheme.onSurface,
                                        modifier = tabIconModifier
                                    )
                                } else {
                                    page.icon?.let { icon ->
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = page.label,
                                            tint = MiuixTheme.colorScheme.onSurface,
                                            modifier = tabIconModifier
                                        )
                                    }
                                }
                                Text(
                                    text = page.label,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { _ ->
        HorizontalPager(
            state = pagerState,
            key = { index -> tabs[index].name },
            userScrollEnabled = pagerScrollEnabled,
            overscrollEffect = null,
            beyondViewportPageCount = UiPerformanceBudget.mainPagerBeyondViewportPageCount,
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
                            homeIconHdrEnabled = homeIconHdrEnabled,
                            liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
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
                        OsPage(
                            scrollToTopSignal = osScrollToTopSignal,
                            isPageActive = pageIndex == pagerState.currentPage || pageIndex == pagerState.settledPage,
                            shizukuStatus = shizukuStatus,
                            shizukuApiUtils = shizukuApiUtils,
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                            liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                            contentBottomPadding = bottomOverlayPadding,
                            onActionBarInteractingChanged = { interacting ->
                                pagerScrollEnabled = !interacting
                            }
                        )
                    }
                    BottomPage.Ba -> {
                        BAPage(
                            contentBottomPadding = bottomOverlayPadding,
                            scrollToTopSignal = baScrollToTopSignal,
                            isPageActive = pageIndex == pagerState.currentPage || pageIndex == pagerState.settledPage,
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                            liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
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
                        McpPage(
                            mcpServerManager = mcpServerManager,
                            contentBottomPadding = bottomOverlayPadding,
                            scrollToTopSignal = mcpScrollToTopSignal,
                            isPageActive = pageIndex == pagerState.currentPage || pageIndex == pagerState.settledPage,
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                            liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                            onOpenSkill = { navigator.pushSingleTop(KeiosRoute.McpSkill) },
                            onActionBarInteractingChanged = { interacting ->
                                pagerScrollEnabled = !interacting
                            }
                        )
                    }
                    BottomPage.GitHub -> {
                        GitHubPage(
                            contentBottomPadding = bottomOverlayPadding,
                            scrollToTopSignal = githubScrollToTopSignal,
                            isPageActive = pageIndex == pagerState.currentPage || pageIndex == pagerState.settledPage,
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                            liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                            onActionBarInteractingChanged = { interacting ->
                                pagerScrollEnabled = !interacting
                            }
                        )
                    }
                }
                if (!isHome) {
                    // Render as a soft foreground overlay so it remains visible even when
                    // destination pages use opaque surface/background containers.
                    NonHomePageBackground(
                        enabled = nonHomeBackgroundEnabled,
                        imageUri = nonHomeBackgroundUri
                    )
                }
            }
        }
    }
}

@Composable
private fun NonHomePageBackground(
    enabled: Boolean,
    imageUri: String
) {
    if (!enabled || imageUri.isBlank()) return
    AsyncImage(
        model = imageUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = 0.16f,
        modifier = Modifier.fillMaxSize()
    )
}
