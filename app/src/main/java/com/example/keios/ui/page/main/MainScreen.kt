package com.example.keios.ui.page.main

import android.content.pm.PackageInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.utils.ShizukuApiUtils
import com.example.keios.ui.utils.UiPrefs
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    var systemScrollToTopSignal by remember { mutableIntStateOf(0) }
    var aboutScrollToTopSignal by remember { mutableIntStateOf(0) }
    var mcpScrollToTopSignal by remember { mutableIntStateOf(0) }
    var githubScrollToTopSignal by remember { mutableIntStateOf(0) }
    var showBottomBar by remember { mutableStateOf(true) }
    var liquidBottomBarEnabled by remember { mutableStateOf(UiPrefs.isLiquidBottomBarEnabled()) }
    val backdrop: Backdrop = rememberLayerBackdrop()
    val mcpUiState by mcpServerManager.uiState.collectAsState()
    val density = LocalDensity.current
    val navigationBarBottom = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val bottomOverlayPadding = 112.dp + navigationBarBottom
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) showBottomBar = false
                if (available.y > 1f) showBottomBar = true
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 18.dp)
                .padding(WindowInsets.safeDrawing.union(WindowInsets.navigationBars).asPaddingValues())
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            when (currentPage) {
                BottomPage.Home -> {
                    HomePage(
                        backdrop = backdrop,
                        shizukuStatus = shizukuStatus,
                        mcpRunning = mcpUiState.running,
                        mcpPort = mcpUiState.port,
                        shizukuApiVersion = ShizukuApiUtils.API_VERSION,
                        mcpConnectedClients = mcpUiState.connectedClients
                    )
                }

                BottomPage.System -> {
                    SystemPage(
                        backdrop = backdrop,
                        scrollToTopSignal = systemScrollToTopSignal,
                        shizukuStatus = shizukuStatus,
                        shizukuApiUtils = shizukuApiUtils,
                        contentBottomPadding = bottomOverlayPadding
                    )
                }

                BottomPage.About -> {
                    AboutPage(
                        backdrop = backdrop,
                        appLabel = appLabel,
                        packageInfo = packageInfo,
                        shizukuStatus = shizukuStatus,
                        shizukuApiUtils = shizukuApiUtils,
                        onCheckShizuku = onCheckOrRequestShizuku,
                        contentBottomPadding = bottomOverlayPadding,
                        scrollToTopSignal = aboutScrollToTopSignal
                    )
                }

                BottomPage.Mcp -> {
                    McpPage(
                        backdrop = backdrop,
                        mcpServerManager = mcpServerManager,
                        notificationPermissionGranted = notificationPermissionGranted,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        contentBottomPadding = bottomOverlayPadding,
                        scrollToTopSignal = mcpScrollToTopSignal
                    )
                }

                BottomPage.GitHub -> {
                    GitHubPage(
                        backdrop = backdrop,
                        contentBottomPadding = bottomOverlayPadding,
                        scrollToTopSignal = githubScrollToTopSignal
                    )
                }

                BottomPage.Settings -> {
                    SettingsPage(
                        backdrop = backdrop,
                        liquidBottomBarEnabled = liquidBottomBarEnabled,
                        onLiquidBottomBarChanged = {
                            liquidBottomBarEnabled = it
                            UiPrefs.setLiquidBottomBarEnabled(it)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(bottomOverlayPadding))
        }

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
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
                        currentPage = selected
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp + navigationBarBottom)
            )
        }
    }
}
