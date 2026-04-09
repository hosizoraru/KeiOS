package com.example.keios.ui.page.main

import android.content.pm.PackageInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.utils.ShizukuApiUtils
import com.example.keios.ui.utils.UiPrefs
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
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
    var currentPage by remember { mutableStateOf(BottomPage.Home) }
    var settingsVisible by remember { mutableStateOf(false) }
    var systemScrollToTopSignal by remember { mutableIntStateOf(0) }
    var aboutScrollToTopSignal by remember { mutableIntStateOf(0) }
    var mcpScrollToTopSignal by remember { mutableIntStateOf(0) }
    var githubScrollToTopSignal by remember { mutableIntStateOf(0) }
    var showBottomBar by remember { mutableStateOf(true) }
    var liquidBottomBarEnabled by remember { mutableStateOf(UiPrefs.isLiquidBottomBarEnabled()) }
    val backdrop = rememberLayerBackdrop()
    val tabs = BottomPage.entries
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(currentPage).coerceAtLeast(0),
        pageCount = { tabs.size }
    )
    val mcpUiState by mcpServerManager.uiState.collectAsState()
    val density = LocalDensity.current
    val navigationBarBottom = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val bottomOverlayPadding = 0.dp
    val systemInsets = WindowInsets.safeDrawing.union(WindowInsets.navigationBars).asPaddingValues()
    val isImmersiveHome = currentPage == BottomPage.Home && !settingsVisible
    val contentInsets = if (isImmersiveHome) PaddingValues(0.dp) else PaddingValues(
        start = systemInsets.calculateStartPadding(LayoutDirection.Ltr),
        top = systemInsets.calculateTopPadding(),
        end = systemInsets.calculateEndPadding(LayoutDirection.Ltr),
        bottom = 0.dp
    )
    val homeTopInset = systemInsets.calculateTopPadding()
    val homeBottomInset = systemInsets.calculateBottomPadding()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) showBottomBar = false
                if (available.y > 1f) showBottomBar = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(currentPage, settingsVisible) {
        if (!settingsVisible) {
            val targetIndex = tabs.indexOf(currentPage).coerceAtLeast(0)
            if (pagerState.currentPage != targetIndex) {
                pagerState.animateScrollToPage(targetIndex)
            }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        val page = tabs[pagerState.currentPage]
        if (currentPage != page) {
            currentPage = page
            settingsVisible = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                        animationSpec = tween(220),
                        initialOffsetY = { it / 2 }
                    ),
                    exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
                        animationSpec = tween(180),
                        targetOffsetY = { it / 2 }
                    )
                ) {
                    FloatingBottomBar(
                        backdrop = backdrop,
                        currentPage = currentPage,
                        liquidGlassEnabled = liquidBottomBarEnabled,
                        onPageSelected = { selected ->
                            showBottomBar = true
                            if (selected == currentPage) {
                                if (selected == BottomPage.System) {
                                    systemScrollToTopSignal++
                                }
                                if (selected == BottomPage.About) {
                                    aboutScrollToTopSignal++
                                }
                                if (selected == BottomPage.Mcp) {
                                    mcpScrollToTopSignal++
                                }
                                if (selected == BottomPage.GitHub) {
                                    githubScrollToTopSignal++
                                }
                            } else {
                                settingsVisible = false
                                currentPage = selected
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(tabs.indexOf(selected).coerceAtLeast(0))
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 12.dp + navigationBarBottom)
                    )
                }
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .nestedScroll(nestedScrollConnection)
        ) {
            if (settingsVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp)
                        .padding(systemInsets),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsPage(
                        backdrop = backdrop,
                        liquidBottomBarEnabled = liquidBottomBarEnabled,
                        onLiquidBottomBarChanged = {
                            liquidBottomBarEnabled = it
                            UiPrefs.setLiquidBottomBarEnabled(it)
                        },
                        onBack = { settingsVisible = false }
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 0,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val shouldComposeHeavyPage =
                        pageIndex == pagerState.currentPage || pageIndex == pagerState.targetPage
                    if (!shouldComposeHeavyPage) {
                        Box(modifier = Modifier.fillMaxSize())
                        return@HorizontalPager
                    }
                    when (tabs[pageIndex]) {
                        BottomPage.Home -> {
                            HomePage(
                                backdrop = backdrop,
                                shizukuStatus = shizukuStatus,
                                mcpRunning = mcpUiState.running,
                                mcpPort = mcpUiState.port,
                                shizukuApiVersion = ShizukuApiUtils.API_VERSION,
                                mcpConnectedClients = mcpUiState.connectedClients,
                                onOpenSettings = { settingsVisible = true },
                                contentTopPadding = homeTopInset,
                                contentBottomPadding = homeBottomInset
                            )
                        }

                        BottomPage.System -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp)
                                    .padding(contentInsets)
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                SystemPage(
                                    backdrop = backdrop,
                                    scrollToTopSignal = systemScrollToTopSignal,
                                    shizukuStatus = shizukuStatus,
                                    shizukuApiUtils = shizukuApiUtils,
                                    contentBottomPadding = bottomOverlayPadding
                                )
                            }
                        }

                        BottomPage.About -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp)
                                    .padding(contentInsets)
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                AboutPage(
                                    backdrop = backdrop,
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
                        }

                        BottomPage.Mcp -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp)
                                    .padding(contentInsets)
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                McpPage(
                                    backdrop = backdrop,
                                    mcpServerManager = mcpServerManager,
                                    contentBottomPadding = bottomOverlayPadding,
                                    scrollToTopSignal = mcpScrollToTopSignal
                                )
                            }
                        }

                        BottomPage.GitHub -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp)
                                    .padding(contentInsets)
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                GitHubPage(
                                    backdrop = backdrop,
                                    contentBottomPadding = bottomOverlayPadding,
                                    scrollToTopSignal = githubScrollToTopSignal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
