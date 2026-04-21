package com.example.keios.ui.page.main.mcp

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.keios.R
import com.example.keios.mcp.server.McpServerManager
import com.example.keios.ui.page.main.widget.chrome.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.chrome.AppPageScaffold
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBar
import com.example.keios.ui.page.main.widget.chrome.LiquidActionItem
import com.example.keios.ui.page.main.widget.motion.appFloatingEnter
import com.example.keios.ui.page.main.widget.motion.appFloatingExit
import com.example.keios.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import com.example.keios.ui.page.main.host.pager.MainPageRuntime
import com.example.keios.ui.page.main.host.pager.rememberMainPageBackdropSet
import com.example.keios.ui.page.main.os.appLucideEditIcon
import com.example.keios.ui.page.main.os.appLucideNotesIcon
import com.example.keios.ui.page.main.os.appLucidePauseIcon
import com.example.keios.ui.page.main.os.appLucideRefreshIcon
import com.example.keios.ui.page.main.mcp.dialog.McpResetConfigDialog
import com.example.keios.ui.page.main.mcp.dialog.McpResetTokenDialog
import com.example.keios.ui.page.main.mcp.section.McpLogsSection
import com.example.keios.ui.page.main.mcp.section.McpOverviewCardSection
import com.example.keios.ui.page.main.mcp.section.McpServiceControlSection
import com.example.keios.ui.page.main.mcp.section.McpToolsSection
import com.example.keios.ui.page.main.mcp.sheet.McpEditServiceSheet
import com.example.keios.ui.page.main.mcp.state.rememberMcpPageOverviewState
import com.example.keios.ui.page.main.mcp.util.buildMcpLogsExportJson
import com.example.keios.ui.page.main.mcp.util.copyToClipboard
import com.example.keios.ui.page.main.os.osLucideCopyIcon
import com.example.keios.ui.page.main.os.osLucideRunIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val uiState by mcpServerManager.uiState.collectAsState()
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
    var portText by remember(uiState.port) { mutableStateOf(uiState.port.toString()) }
    var allowExternal by remember(uiState.allowExternal) { mutableStateOf(uiState.allowExternal) }
    var serverName by remember(uiState.serverName) { mutableStateOf(uiState.serverName) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showFloatingToggleButton by remember { mutableStateOf(true) }
    val toggleButtonScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f && showFloatingToggleButton) {
                    showFloatingToggleButton = false
                }
                if (available.y > 1f && !showFloatingToggleButton) {
                    showFloatingToggleButton = true
                }
                return Offset.Zero
            }
        }
    }
    var controlExpanded by remember { mutableStateOf(true) }
    var configExpanded by remember { mutableStateOf(false) }
    var logsExpanded by remember { mutableStateOf(false) }
    var logsExporting by remember { mutableStateOf(false) }
    var pendingLogsExportGeneratedAt by remember { mutableStateOf<String?>(null) }
    var pendingLogsExportFileName by remember { mutableStateOf<String?>(null) }
    var showResetTokenConfirm by remember { mutableStateOf(false) }
    var showResetConfigConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()
    val currentUiState by rememberUpdatedState(uiState)
    val serverNameHint = context.getString(R.string.mcp_input_service_name_hint)
    val serverNameFieldWidth = remember(serverName, serverNameHint) {
        val visibleChars = serverName.trim().ifBlank { serverNameHint }.length.coerceIn(6, 18)
        (visibleChars * 11 + 36).dp
    }
    val portFieldWidth = remember(portText) {
        val visibleChars = portText.trim().ifBlank { "38888" }.length.coerceIn(4, 6)
        (visibleChars * 14 + 28).dp
    }
    val toggleServer: () -> Unit = {
        if (uiState.running) {
            mcpServerManager.stop()
            Toast.makeText(context, context.getString(R.string.mcp_toast_service_stopped), Toast.LENGTH_SHORT).show()
        } else {
            val port = portText.toIntOrNull()
            if (port == null) {
                Toast.makeText(context, context.getString(R.string.common_port_invalid), Toast.LENGTH_SHORT).show()
            } else {
                mcpServerManager.updateServerName(serverName)
                mcpServerManager.start(port = port, allowExternal = allowExternal)
                    .onSuccess {
                        mcpServerManager.refreshAddresses()
                        Toast.makeText(context, context.getString(R.string.mcp_toast_service_started), Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(
                            context,
                            context.getString(R.string.mcp_toast_start_failed, it.message ?: unknownText),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }
    val backdrops = rememberMainPageBackdropSet(
        keyPrefix = "mcp",
        refreshOnCompositionEnter = true
    )
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    LaunchedEffect(runtime.scrollToTopSignal, runtime.isPageActive) {
        if (runtime.isPageActive && runtime.scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }
    val logsExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val generatedAt = pendingLogsExportGeneratedAt
        val fileName = pendingLogsExportFileName
        pendingLogsExportGeneratedAt = null
        pendingLogsExportFileName = null
        if (uri == null || generatedAt.isNullOrBlank() || fileName.isNullOrBlank()) {
            logsExporting = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching {
                val exportContent = withContext(Dispatchers.Default) {
                    buildMcpLogsExportJson(
                        generatedAt = generatedAt,
                        state = currentUiState
                    )
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                        checkNotNull(writer) { "openOutputStream returned null" }
                        writer.write(exportContent)
                    }
                }
            }
            logsExporting = false
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
    val portTextSnapshot = rememberUpdatedState(portText)
    val allowExternalSnapshot = rememberUpdatedState(allowExternal)
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
                onClick = { showEditSheet = true }
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
                    val snapshot = uiStateSnapshot.value
                    val port = portTextSnapshot.value.toIntOrNull() ?: snapshot.port
                    val endpoint = if (allowExternalSnapshot.value && snapshot.addresses.isNotEmpty()) {
                        "http://${snapshot.addresses.first()}:$port${snapshot.endpointPath}"
                    } else {
                        "http://127.0.0.1:$port${snapshot.endpointPath}"
                    }
                    val json = mcpServerManager.buildConfigJson(
                        url = endpoint,
                        includeJsonContentTypeHeader = allowExternalSnapshot.value
                    )
                    copyToClipboard(contextSnapshot.value, "mcp-config", json)
                    Toast.makeText(
                        contextSnapshot.value,
                        contextSnapshot.value.getString(R.string.mcp_toast_config_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ),
            LiquidActionItem(
                icon = refreshIcon,
                contentDescription = refreshContentDescription,
                onClick = {
                    mcpServerManager.refreshNow()
                    Toast.makeText(
                        contextSnapshot.value,
                        contextSnapshot.value.getString(R.string.common_refreshed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        )
    }

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
                        onOpenEditSheet = { showEditSheet = true }
                    )
                }
                item {
                    McpServiceControlSection(
                        backdrop = backdrops.content,
                        expanded = controlExpanded,
                        onExpandedChange = { controlExpanded = it },
                        mcpServerManager = mcpServerManager,
                        unknownText = unknownText,
                        onShowResetConfigConfirm = { showResetConfigConfirm = true }
                    )
                }
                item {
                    McpToolsSection(
                        backdrop = backdrops.content,
                        expanded = configExpanded,
                        onExpandedChange = { configExpanded = it },
                        uiState = uiState
                    )
                }
                item {
                    McpLogsSection(
                        backdrop = backdrops.content,
                        expanded = logsExpanded,
                        onExpandedChange = { logsExpanded = it },
                        uiState = uiState,
                        logsExporting = logsExporting,
                        onExportLogs = { generatedAt, fileName ->
                            logsExporting = true
                            pendingLogsExportGeneratedAt = generatedAt
                            pendingLogsExportFileName = fileName
                            runCatching {
                                logsExportLauncher.launch(fileName)
                            }.onFailure {
                                pendingLogsExportGeneratedAt = null
                                pendingLogsExportFileName = null
                                logsExporting = false
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
                        onClearLogs = { mcpServerManager.clearLogs() },
                        subtitleColor = subtitleColor
                    )
                }
            }

            AnimatedVisibility(
                visible = showFloatingToggleButton,
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
        show = showEditSheet,
        backdrop = backdrops.sheet,
        serverName = serverName,
        onServerNameChange = { serverName = it },
        serverNameFieldWidth = serverNameFieldWidth,
        portText = portText,
        onPortTextChange = { portText = it },
        portFieldWidth = portFieldWidth,
        allowExternal = allowExternal,
        onAllowExternalChange = { allowExternal = it },
        mcpServerManager = mcpServerManager,
        unknownText = unknownText,
        onDismissRequest = { showEditSheet = false },
        onShowResetTokenConfirm = { showResetTokenConfirm = true }
    )

    McpResetConfigDialog(
        show = showResetConfigConfirm,
        mcpServerManager = mcpServerManager,
        onDismissRequest = { showResetConfigConfirm = false }
    )

    McpResetTokenDialog(
        show = showResetTokenConfirm,
        mcpServerManager = mcpServerManager,
        onDismissRequest = { showResetTokenConfirm = false }
    )
}
