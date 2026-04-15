package com.example.keios.ui.page.main

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.keios.mcp.McpNotificationHelper
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.navigation.KeiosRoute
import com.example.keios.ui.navigation.Navigator
import com.example.keios.ui.page.main.about.AboutPage
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
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
    requestedBottomPageToken: Int
) {
    val backStack = remember { mutableStateListOf<NavKey>().apply { add(KeiosRoute.Main) } }
    val navigator = remember { Navigator(backStack) }
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
    var cardPressFeedbackEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.cardPressFeedbackEnabled) }
    var homeIconHdrEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.homeIconHdrEnabled) }
    var superIslandNotificationEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.superIslandNotificationEnabled) }
    var superIslandBypassRestrictionEnabled by remember(uiPrefsSnapshot) {
        mutableStateOf(uiPrefsSnapshot.superIslandBypassRestrictionEnabled)
    }
    var cacheDiagnosticsEnabled by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.cacheDiagnosticsEnabled) }
    var visibleBottomPageNames by remember(uiPrefsSnapshot) { mutableStateOf(uiPrefsSnapshot.visibleBottomPageNames) }
    val view = LocalView.current

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
                    cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                    homeIconHdrEnabled = homeIconHdrEnabled,
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
                    requestedBottomPage = requestedBottomPage,
                    requestedBottomPageToken = requestedBottomPageToken
                )
            }
            entry<KeiosRoute.Settings> {
                SettingsPage(
                    liquidBottomBarEnabled = liquidBottomBarEnabled,
                    onLiquidBottomBarChanged = {
                        liquidBottomBarEnabled = it
                        UiPrefs.setLiquidBottomBarEnabled(it)
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
                    onBack = { navigator.pop() }
                )
            }
            entry<KeiosRoute.BaGuideCatalog> {
                BaGuideCatalogPage(
                    onBack = { navigator.pop() },
                    onOpenGuide = { sourceUrl ->
                        val target = sourceUrl.trim()
                        if (target.isNotBlank()) {
                            BaStudentGuideStore.setCurrentUrl(target)
                            navigator.push(KeiosRoute.BaStudentGuide)
                        }
                    }
                )
            }
        }
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider,
    )

    NavDisplay(
        entries = entries,
        onBack = { navigator.pop() },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MainPagerLayout(
    navigator: Navigator,
    liquidBottomBarEnabled: Boolean,
    cardPressFeedbackEnabled: Boolean,
    homeIconHdrEnabled: Boolean,
    visibleBottomPageNames: Set<String>,
    onVisibleBottomPageNamesChange: (Set<String>) -> Unit,
    appLabel: String,
    packageInfo: PackageInfo?,
    shizukuStatus: String,
    onCheckOrRequestShizuku: () -> Unit,
    notificationPermissionGranted: Boolean,
    shizukuApiUtils: ShizukuApiUtils,
    mcpServerManager: McpServerManager,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int
) {
    val tabs = remember(visibleBottomPageNames) {
        BottomPage.entries.filter { page ->
            page == BottomPage.Home || visibleBottomPageNames.contains(page.name)
        }
    }
    val visibleTabsSnapshot = remember(tabs) { tabs.toSet() }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val mcpUiState by mcpServerManager.uiState.collectAsState()

    var osScrollToTopSignal by remember { mutableIntStateOf(0) }
    var baScrollToTopSignal by remember { mutableIntStateOf(0) }
    var mcpScrollToTopSignal by remember { mutableIntStateOf(0) }
    var githubScrollToTopSignal by remember { mutableIntStateOf(0) }
    var pagerScrollEnabled by remember { mutableStateOf(true) }

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
        val selected = tabs[index]
        showBottomBar = true
        if (index == pagerState.currentPage) {
            when (selected) {
                BottomPage.Os -> osScrollToTopSignal++
                BottomPage.Ba -> baScrollToTopSignal++
                BottomPage.Mcp -> mcpScrollToTopSignal++
                BottomPage.GitHub -> githubScrollToTopSignal++
                else -> {}
            }
        } else {
            coroutineScope.launch {
                pagerState.animateScrollToPage(index)
            }
        }
    }

    LaunchedEffect(requestedBottomPageToken, requestedBottomPage, tabs) {
        val target = requestedBottomPage ?: return@LaunchedEffect
        val index = tabs.indexOfFirst { it.name == target }
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
            showBottomBar = true
        }
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
                    enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                        animationSpec = tween(220),
                        initialOffsetY = { it / 2 }
                    ),
                    exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
                        animationSpec = tween(180),
                        targetOffsetY = { it / 2 }
                    ),
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
                        selectedIndex = { pagerState.currentPage },
                        onSelected = { index ->
                            // Ignore mirror callbacks emitted after pager page sync.
                            // Keep explicit tab click behavior (including reselect-to-top) unchanged.
                            if (index != pagerState.currentPage) {
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
            beyondViewportPageCount = 0,
            // CRITICAL FIX: NEVER conditionally unmount layerBackdrop.
            // If the node is visible (even during an exit animation), it MUST have the backdrop attached,
            // otherwise consumer composables will attempt to draw a detached Native pointer causing SIGSEGV.
            modifier = Modifier
                .fillMaxSize()
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
                            mcpPort = mcpUiState.port,
                            mcpConnectedClients = mcpUiState.connectedClients,
                            mcpAllowExternal = mcpUiState.allowExternal,
                            homeIconHdrEnabled = homeIconHdrEnabled,
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
                            onOpenSettings = { navigator.push(KeiosRoute.Settings) },
                            onOpenAbout = { navigator.push(KeiosRoute.About) },
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
                            shizukuStatus = shizukuStatus,
                            shizukuApiUtils = shizukuApiUtils,
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
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
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                            onOpenPoolStudentGuide = { sourceUrl ->
                                val target = sourceUrl.trim()
                                if (target.isNotBlank()) {
                                    BaStudentGuideStore.setCurrentUrl(target)
                                    navigator.push(KeiosRoute.BaStudentGuide)
                                }
                            },
                            onOpenGuideCatalog = {
                                navigator.push(KeiosRoute.BaGuideCatalog)
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
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                            onOpenSkill = { navigator.push(KeiosRoute.McpSkill) },
                            onActionBarInteractingChanged = { interacting ->
                                pagerScrollEnabled = !interacting
                            }
                        )
                    }
                    BottomPage.GitHub -> {
                        GitHubPage(
                            contentBottomPadding = bottomOverlayPadding,
                            scrollToTopSignal = githubScrollToTopSignal,
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                            onActionBarInteractingChanged = { interacting ->
                                pagerScrollEnabled = !interacting
                            }
                        )
                    }
                }
            }
        }
    }
}
