package com.example.keios.ui.page.main

import android.content.pm.PackageInfo
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
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.navigation.KeiosRoute
import com.example.keios.ui.navigation.Navigator
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.ui.utils.ShizukuApiUtils
import com.example.keios.ui.utils.UiPrefs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
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
) {
    val backStack = remember { mutableStateListOf<NavKey>().apply { add(KeiosRoute.Main) } }
    val navigator = remember { Navigator(backStack) }

    var liquidBottomBarEnabled by remember { mutableStateOf(UiPrefs.isLiquidBottomBarEnabled()) }

    val entryProvider = remember(backStack) {
        entryProvider<NavKey> {
            entry<KeiosRoute.Main> {
                // Removed the dangerous isTopRoute check. The route should manage its own state naturally.
                MainPagerLayout(
                    navigator = navigator,
                    liquidBottomBarEnabled = liquidBottomBarEnabled,
                    appLabel = appLabel,
                    packageInfo = packageInfo,
                    shizukuStatus = shizukuStatus,
                    onCheckOrRequestShizuku = onCheckOrRequestShizuku,
                    notificationPermissionGranted = notificationPermissionGranted,
                    shizukuApiUtils = shizukuApiUtils,
                    mcpServerManager = mcpServerManager
                )
            }
            entry<KeiosRoute.Settings> {
                SettingsPage(
                    liquidBottomBarEnabled = liquidBottomBarEnabled,
                    onLiquidBottomBarChanged = {
                        liquidBottomBarEnabled = it
                        UiPrefs.setLiquidBottomBarEnabled(it)
                    },
                    onBack = { navigator.pop() }
                )
            }
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
    appLabel: String,
    packageInfo: PackageInfo?,
    shizukuStatus: String,
    onCheckOrRequestShizuku: () -> Unit,
    notificationPermissionGranted: Boolean,
    shizukuApiUtils: ShizukuApiUtils,
    mcpServerManager: McpServerManager
) {
    val tabs = remember { BottomPage.entries.toTypedArray() }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val mcpUiState by mcpServerManager.uiState.collectAsState()

    var systemScrollToTopSignal by remember { mutableIntStateOf(0) }
    var aboutScrollToTopSignal by remember { mutableIntStateOf(0) }
    var mcpScrollToTopSignal by remember { mutableIntStateOf(0) }
    var githubScrollToTopSignal by remember { mutableIntStateOf(0) }

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

    val handlePageSelected: (Int) -> Unit = { index ->
        val selected = tabs[index]
        showBottomBar = true
        if (index == pagerState.currentPage) {
            when (selected) {
                BottomPage.System -> systemScrollToTopSignal++
                BottomPage.About -> aboutScrollToTopSignal++
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
                        onSelected = handlePageSelected,
                        backdrop = backdrop,
                        tabsCount = tabs.size,
                        isBlurEnabled = liquidBottomBarEnabled
                    ) {
                        tabs.forEachIndexed { index, page ->
                            FloatingBottomBarItem(
                                onClick = { handlePageSelected(index) },
                                modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                            ) {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = page.label,
                                    tint = MiuixTheme.colorScheme.onSurface
                                )
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
            userScrollEnabled = true,
            overscrollEffect = null,
            beyondViewportPageCount = 1,
            // CRITICAL FIX: NEVER conditionally unmount layerBackdrop.
            // If the node is visible (even during an exit animation), it MUST have the backdrop attached,
            // otherwise consumer composables will attempt to draw a detached Native pointer causing SIGSEGV.
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) { pageIndex ->

            val currentPageType = tabs[pageIndex]
            val isHome = currentPageType == BottomPage.Home
            val isTopBarManagedPage = currentPageType == BottomPage.System ||
                currentPageType == BottomPage.About ||
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
                            shizukuApiVersion = ShizukuApiUtils.API_VERSION,
                            mcpConnectedClients = mcpUiState.connectedClients,
                            onOpenSettings = { navigator.push(KeiosRoute.Settings) },
                            contentTopPadding = homeTopInset,
                            contentBottomPadding = homeBottomInset
                        )
                    }
                    BottomPage.System -> {
                        SystemPage(
                            scrollToTopSignal = systemScrollToTopSignal,
                            shizukuStatus = shizukuStatus,
                            shizukuApiUtils = shizukuApiUtils,
                            contentBottomPadding = bottomOverlayPadding
                        )
                    }
                    BottomPage.About -> {
                        AboutPage(
                            appLabel = appLabel,
                            packageInfo = packageInfo,
                            notificationPermissionGranted = notificationPermissionGranted,
                            shizukuStatus = shizukuStatus,
                            shizukuApiUtils = shizukuApiUtils,
                            onCheckShizuku = onCheckOrRequestShizuku,
                            contentBottomPadding = bottomOverlayPadding,
                            scrollToTopSignal = aboutScrollToTopSignal
                        )
                    }
                    BottomPage.Mcp -> {
                        McpPage(
                            mcpServerManager = mcpServerManager,
                            contentBottomPadding = bottomOverlayPadding,
                            scrollToTopSignal = mcpScrollToTopSignal
                        )
                    }
                    BottomPage.GitHub -> {
                        GitHubPage(
                            contentBottomPadding = bottomOverlayPadding,
                            scrollToTopSignal = githubScrollToTopSignal
                        )
                    }
                }
            }
        }
    }
}
