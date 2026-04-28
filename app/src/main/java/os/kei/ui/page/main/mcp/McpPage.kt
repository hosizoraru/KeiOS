package os.kei.ui.page.main.mcp

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import os.kei.R
import os.kei.core.platform.LocalNetworkPermissionCompat
import os.kei.mcp.server.McpServerManager
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.LiquidActionItem
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.core.ui.effect.getMiuixAppBarColor
import os.kei.core.ui.effect.rememberMiuixBlurBackdrop
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.host.pager.rememberMainPageBackdropSet
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import os.kei.ui.page.main.widget.glass.rememberListScrollGlassRuntime
import os.kei.ui.page.main.os.appLucideEditIcon
import os.kei.ui.page.main.os.appLucideNotesIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.mcp.dialog.McpResetConfigDialog
import os.kei.ui.page.main.mcp.dialog.McpResetTokenDialog
import os.kei.ui.page.main.mcp.section.McpLogsSection
import os.kei.ui.page.main.mcp.section.McpOverviewCardSection
import os.kei.ui.page.main.mcp.section.McpServiceControlSection
import os.kei.ui.page.main.mcp.section.McpToolsSection
import os.kei.ui.page.main.mcp.sheet.McpEditServiceSheet
import os.kei.ui.page.main.mcp.state.rememberMcpPageOverviewState
import os.kei.ui.page.main.mcp.util.copyToClipboard
import os.kei.ui.page.main.os.osLucideCopyIcon
import os.kei.ui.page.main.os.osLucideRunIcon
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun McpPage(
    mcpServerManager: McpServerManager,
    runtime: MainPageRuntime = MainPageRuntime(contentBottomPadding = 72.dp),
    cardPressFeedbackEnabled: Boolean = true,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    onOpenSkill: () -> Unit = {},
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val mcpTitle = stringResource(R.string.page_mcp_title)
    val editServiceParamsContentDescription = stringResource(R.string.mcp_action_edit_service_params)
    val openSkillContentDescription = stringResource(R.string.mcp_action_open_skill_md)
    val copyConfigContentDescription = stringResource(R.string.mcp_action_copy_current_config)
    val refreshContentDescription = stringResource(R.string.common_refresh)
    val unknownText = stringResource(R.string.common_unknown)
    val runtimePendingText = stringResource(R.string.mcp_runtime_pending)
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f)
    val runningColor = Color(0xFF2E7D32)
    val stoppedColor = Color(0xFFC62828)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mcpPageViewModel: McpPageViewModel = viewModel()
    val uiState by mcpServerManager.uiState.collectAsState()
    val pageUiState by mcpPageViewModel.uiState.collectAsState()
    LaunchedEffect(
        mcpServerManager,
        uiState.port,
        uiState.allowExternal,
        uiState.serverName,
        pageUiState.showEditSheet
    ) {
        mcpPageViewModel.syncServiceDraft(uiState)
    }
    val isDark = isSystemInDarkTheme()
    val overviewState = rememberMcpPageOverviewState(
        context = context,
        uiState = uiState,
        runtime = runtime,
        isDark = isDark,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        runningColor = runningColor,
        stoppedColor = stoppedColor,
        runtimePendingText = runtimePendingText
    )
    val portText = pageUiState.portText
    val allowExternal = pageUiState.allowExternal
    val serverName = pageUiState.serverName
    val floatingToggleButtonVisible = rememberUpdatedState(pageUiState.showFloatingToggleButton)
    val toggleButtonScrollConnection = remember(mcpPageViewModel, floatingToggleButtonVisible) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y < -1f && floatingToggleButtonVisible.value) {
                    mcpPageViewModel.updateFloatingToggleButtonVisible(false)
                }
                if (consumed.y > 1f && !floatingToggleButtonVisible.value) {
                    mcpPageViewModel.updateFloatingToggleButtonVisible(true)
                }
                return Offset.Zero
            }
        }
    }
    val listState = rememberLazyListState()
    val isListScrolling by remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }
    val scrollBehavior = MiuixScrollBehavior()
    val currentUiState by rememberUpdatedState(uiState)
    val serverNameHint = context.getString(R.string.mcp_input_service_name_hint)
    var localNetworkPermissionGranted by remember {
        mutableStateOf(hasMcpLocalNetworkPermission(context))
    }
    var startAfterLocalNetworkPermission by remember { mutableStateOf(false) }
    fun launchServerToggle() {
        scope.launch {
            when (val result = mcpPageViewModel.toggleServer(mcpServerManager)) {
                McpToggleServerResult.InvalidPort -> {
                    Toast.makeText(context, context.getString(R.string.common_port_invalid), Toast.LENGTH_SHORT).show()
                }

                is McpToggleServerResult.Failed -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.mcp_toast_start_failed, result.reason ?: unknownText),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                McpToggleServerResult.Started -> {
                    Toast.makeText(context, context.getString(R.string.mcp_toast_service_started), Toast.LENGTH_SHORT).show()
                }

                McpToggleServerResult.Stopped -> {
                    Toast.makeText(context, context.getString(R.string.mcp_toast_service_stopped), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        localNetworkPermissionGranted = granted || hasMcpLocalNetworkPermission(context)
        Toast.makeText(
            context,
            context.getString(
                if (localNetworkPermissionGranted) {
                    R.string.mcp_toast_local_network_permission_granted
                } else {
                    R.string.mcp_toast_local_network_permission_denied
                }
            ),
            Toast.LENGTH_SHORT
        ).show()
        val shouldStartServer = startAfterLocalNetworkPermission && localNetworkPermissionGranted
        startAfterLocalNetworkPermission = false
        if (shouldStartServer && !mcpServerManager.uiState.value.running) {
            launchServerToggle()
        }
    }
    val serverNameFieldWidth = remember(serverName, serverNameHint) {
        val visibleChars = serverName.trim().ifBlank { serverNameHint }.length.coerceIn(6, 18)
        (visibleChars * 11 + 36).dp
    }
    val portFieldWidth = remember(portText) {
        val visibleChars = portText.trim().ifBlank { "38888" }.length.coerceIn(4, 6)
        (visibleChars * 14 + 28).dp
    }
    val pageBackdropEffectsEnabled = runtime.isPageActive &&
        !runtime.isPagerScrollInProgress
    val fullBackdropEffectsEnabled = pageBackdropEffectsEnabled &&
        !isListScrolling
    val mcpGlassRuntime = rememberListScrollGlassRuntime(
        isListScrolling = isListScrolling,
        label = "mcpListGlassEffectProgress"
    )
    val toggleServer: () -> Unit = toggleServer@{
        localNetworkPermissionGranted = hasMcpLocalNetworkPermission(context)
        if (!uiState.running && allowExternal && !localNetworkPermissionGranted) {
            LocalNetworkPermissionCompat.requiredPermissionOrNull()
                ?.let { permission ->
                    startAfterLocalNetworkPermission = true
                    runCatching { localNetworkPermissionLauncher.launch(permission) }
                }
            Toast.makeText(
                context,
                context.getString(R.string.mcp_toast_local_network_permission_requested),
                Toast.LENGTH_SHORT
            ).show()
            return@toggleServer
        }
        startAfterLocalNetworkPermission = false
        launchServerToggle()
    }
    val saveServiceConfig: () -> Unit = {
        scope.launch {
            when (val result = mcpPageViewModel.saveConfig(mcpServerManager)) {
                McpSaveConfigResult.InvalidPort -> {
                    Toast.makeText(context, context.getString(R.string.common_port_invalid), Toast.LENGTH_SHORT).show()
                }

                is McpSaveConfigResult.Failed -> {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.common_save_failed_with_reason,
                            result.reason ?: unknownText
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                McpSaveConfigResult.Success -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.mcp_toast_saved_requires_restart),
                        Toast.LENGTH_SHORT
                    ).show()
                    mcpPageViewModel.updateEditSheetVisible(false)
                    mcpPageViewModel.syncServiceDraft(mcpServerManager.uiState.value, force = true)
                }
            }
        }
    }
    val sendTestNotification: () -> Unit = {
        scope.launch {
            mcpPageViewModel.sendTestNotification(mcpServerManager)
                .onSuccess {
                    Toast.makeText(
                        context,
                        context.getString(R.string.mcp_toast_test_notification_sent),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.common_send_failed_with_reason,
                            it.message ?: unknownText
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
    val resetConfig: () -> Unit = {
        scope.launch {
            val requiresRestart = mcpPageViewModel.resetConfigPreservingToken(mcpServerManager)
            Toast.makeText(
                context,
                context.getString(
                    if (requiresRestart) {
                        R.string.mcp_toast_config_reset_requires_restart
                    } else {
                        R.string.mcp_toast_config_reset
                    }
                ),
                Toast.LENGTH_SHORT
            ).show()
            mcpPageViewModel.updateResetConfigConfirmVisible(false)
        }
    }
    val resetToken: () -> Unit = {
        scope.launch {
            mcpPageViewModel.resetToken(mcpServerManager)
            Toast.makeText(
                context,
                context.getString(R.string.mcp_toast_token_reset_reconnect),
                Toast.LENGTH_SHORT
            ).show()
            mcpPageViewModel.updateResetTokenConfirmVisible(false)
        }
    }
    val backdrops = rememberMainPageBackdropSet(
        keyPrefix = "mcp",
        refreshOnCompositionEnter = true,
        distinctLayers = fullBackdropEffectsEnabled
    )
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = pageBackdropEffectsEnabled)
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    LaunchedEffect(runtime.scrollToTopSignal, runtime.isPageActive) {
        if (runtime.isPageActive && runtime.scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }
    val logsExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val request = mcpPageViewModel.consumePendingLogsExport()
        if (uri == null || request == null) {
            mcpPageViewModel.finishLogsExport()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching {
                mcpPageViewModel.exportLogs(
                    contentResolver = context.contentResolver,
                    uri = uri,
                    request = request,
                    state = currentUiState
                )
            }
            mcpPageViewModel.finishLogsExport()
            result.onSuccess {
                Toast.makeText(
                    context,
                    context.getString(R.string.mcp_toast_logs_exported),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.mcp_toast_logs_export_failed,
                        it.javaClass.simpleName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val onOpenSkillState = rememberUpdatedState(onOpenSkill)
    val uiStateSnapshot = rememberUpdatedState(uiState)
    val contextSnapshot = rememberUpdatedState(context)
    val editIcon = appLucideEditIcon()
    val notesIcon = appLucideNotesIcon()
    val copyIcon = osLucideCopyIcon()
    val refreshIcon = appLucideRefreshIcon()
    val actionItems = remember(
        editServiceParamsContentDescription,
        openSkillContentDescription,
        copyConfigContentDescription,
        refreshContentDescription
    ) {
        listOf(
            LiquidActionItem(
                icon = editIcon,
                contentDescription = editServiceParamsContentDescription,
                onClick = { mcpPageViewModel.updateEditSheetVisible(true) }
            ),
            LiquidActionItem(
                icon = notesIcon,
                contentDescription = openSkillContentDescription,
                onClick = { onOpenSkillState.value() }
            ),
            LiquidActionItem(
                icon = copyIcon,
                contentDescription = copyConfigContentDescription,
                onClick = {
                    scope.launch {
                        val json = mcpPageViewModel.buildConfigJson(
                            manager = mcpServerManager,
                            serverState = uiStateSnapshot.value
                        )
                        copyToClipboard(contextSnapshot.value, "mcp-config", json)
                        Toast.makeText(
                            contextSnapshot.value,
                            contextSnapshot.value.getString(R.string.mcp_toast_config_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ),
            LiquidActionItem(
                icon = refreshIcon,
                contentDescription = refreshContentDescription,
                onClick = {
                    scope.launch {
                        mcpPageViewModel.refreshNow(mcpServerManager)
                        Toast.makeText(
                            contextSnapshot.value,
                            contextSnapshot.value.getString(R.string.common_refreshed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        )
    }

    CompositionLocalProvider(LocalGlassEffectRuntime provides mcpGlassRuntime) {
    AppPageScaffold(
        title = "",
        largeTitle = mcpTitle,
        modifier = Modifier.fillMaxSize(),
        scrollBehavior = scrollBehavior,
        topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
        actions = {
            LiquidActionBar(
                backdrop = backdrops.topBar,
                layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                reduceEffectsDuringPagerScroll = runtime.isPagerScrollInProgress,
                items = actionItems,
                onInteractionChanged = onActionBarInteractingChanged
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(toggleButtonScrollConnection)
        ) {
            AppPageLazyColumn(
                innerPadding = innerPadding,
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                bottomExtra = appPageBottomPaddingWithFloatingOverlay(runtime.contentBottomPadding),
                sectionSpacing = 12.dp
            ) {
                item {
                    McpOverviewCardSection(
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        overviewCardColor = overviewState.overviewCardColor,
                        overviewBorderColor = overviewState.overviewBorderColor,
                        overviewAccentColor = overviewState.overviewAccentColor,
                        runtimeText = overviewState.runtimeText,
                        isDark = isDark,
                        running = uiState.running,
                        overviewMetrics = overviewState.overviewMetrics,
                        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                        onToggleServer = toggleServer,
                        onOpenEditSheet = { mcpPageViewModel.updateEditSheetVisible(true) }
                    )
                }
                item {
                    McpServiceControlSection(
                        backdrop = backdrops.content,
                        expanded = pageUiState.controlExpanded,
                        onExpandedChange = mcpPageViewModel::updateControlExpanded,
                        onSendTestNotification = sendTestNotification,
                        onShowResetConfigConfirm = { mcpPageViewModel.updateResetConfigConfirmVisible(true) }
                    )
                }
                item {
                    McpToolsSection(
                        backdrop = backdrops.content,
                        expanded = pageUiState.configExpanded,
                        onExpandedChange = mcpPageViewModel::updateConfigExpanded,
                        uiState = uiState
                    )
                }
                item {
                    McpLogsSection(
                        backdrop = backdrops.content,
                        expanded = pageUiState.logsExpanded,
                        onExpandedChange = mcpPageViewModel::updateLogsExpanded,
                        uiState = uiState,
                        logsExporting = pageUiState.logsExporting,
                        onExportLogs = { generatedAt, fileName ->
                            mcpPageViewModel.beginLogsExport(generatedAt, fileName)
                            runCatching {
                                logsExportLauncher.launch(fileName)
                            }.onFailure {
                                mcpPageViewModel.finishLogsExport()
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.mcp_toast_logs_export_failed,
                                        it.javaClass.simpleName
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onClearLogs = {
                            scope.launch {
                                mcpPageViewModel.clearLogs(mcpServerManager)
                            }
                        },
                        subtitleColor = subtitleColor
                    )
                }
            }

            AnimatedVisibility(
                visible = pageUiState.showFloatingToggleButton,
                enter = appFloatingEnter(),
                exit = appFloatingExit(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                GlassIconButton(
                    backdrop = backdrops.content,
                    icon = if (uiState.running) appLucidePauseIcon() else osLucideRunIcon(),
                    contentDescription = if (uiState.running) {
                        stringResource(R.string.mcp_action_stop_service)
                    } else {
                        stringResource(R.string.mcp_action_start_service)
                    },
                    onClick = toggleServer,
                    modifier = Modifier.padding(end = 14.dp, bottom = runtime.contentBottomPadding - 24.dp),
                    width = 60.dp,
                    height = 44.dp,
                    iconTint = if (uiState.running) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                    containerColor = if (uiState.running) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                    variant = GlassVariant.Floating
                )
            }
        }
    }

    McpEditServiceSheet(
        show = pageUiState.showEditSheet,
        backdrop = backdrops.sheet,
        serverName = serverName,
        onServerNameChange = mcpPageViewModel::updateServerName,
        serverNameFieldWidth = serverNameFieldWidth,
        portText = portText,
        onPortTextChange = mcpPageViewModel::updatePortText,
        portFieldWidth = portFieldWidth,
        allowExternal = allowExternal,
        onAllowExternalChange = mcpPageViewModel::updateAllowExternal,
        onSave = saveServiceConfig,
        onDismissRequest = { mcpPageViewModel.updateEditSheetVisible(false) },
        onShowResetTokenConfirm = { mcpPageViewModel.updateResetTokenConfirmVisible(true) }
    )

    McpResetConfigDialog(
        show = pageUiState.showResetConfigConfirm,
        onConfirm = resetConfig,
        onDismissRequest = { mcpPageViewModel.updateResetConfigConfirmVisible(false) }
    )

    McpResetTokenDialog(
        show = pageUiState.showResetTokenConfirm,
        onConfirm = resetToken,
        onDismissRequest = { mcpPageViewModel.updateResetTokenConfirmVisible(false) }
    )
    }
}

private fun hasMcpLocalNetworkPermission(context: Context): Boolean {
    return LocalNetworkPermissionCompat.hasPermission(context)
}
