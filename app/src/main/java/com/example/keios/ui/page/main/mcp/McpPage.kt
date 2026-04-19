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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import com.example.keios.ui.page.main.os.appLucideEditIcon
import com.example.keios.ui.page.main.os.appLucideNotesIcon
import com.example.keios.ui.page.main.os.appLucidePauseIcon
import com.example.keios.ui.page.main.os.appLucideRefreshIcon
import com.example.keios.ui.page.main.mcp.dialog.McpResetConfigDialog
import com.example.keios.ui.page.main.mcp.dialog.McpResetTokenDialog
import com.example.keios.ui.page.main.mcp.model.McpOverviewMetric
import com.example.keios.ui.page.main.mcp.model.toMcpTokenPreview
import com.example.keios.ui.page.main.mcp.section.McpLogsSection
import com.example.keios.ui.page.main.mcp.section.McpOverviewCardSection
import com.example.keios.ui.page.main.mcp.section.McpServiceControlSection
import com.example.keios.ui.page.main.mcp.section.McpToolsSection
import com.example.keios.ui.page.main.mcp.sheet.McpEditServiceSheet
import com.example.keios.ui.page.main.mcp.util.buildMcpLogsExportJson
import com.example.keios.ui.page.main.mcp.util.copyToClipboard
import com.example.keios.ui.page.main.mcp.util.formatMcpUptimeText
import com.example.keios.ui.page.main.os.osLucideCopyIcon
import com.example.keios.ui.page.main.os.osLucideRunIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun McpPage(
    mcpServerManager: McpServerManager,
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    isPageActive: Boolean = true,
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
    val overviewAccentColor = if (uiState.running) runningColor else stoppedColor
    val isDark = isSystemInDarkTheme()
    val overviewCardColor = if (isDark) {
        overviewAccentColor.copy(alpha = 0.16f)
    } else {
        overviewAccentColor.copy(alpha = 0.10f)
    }
    val overviewBorderColor = if (isDark) {
        overviewAccentColor.copy(alpha = 0.32f)
    } else {
        overviewAccentColor.copy(alpha = 0.26f)
    }
    val runtimeNowMs by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = uiState.running,
        key2 = uiState.runningSinceEpochMs,
        key3 = isPageActive
    ) {
        value = System.currentTimeMillis()
        while (uiState.running && uiState.runningSinceEpochMs > 0L) {
            delay(if (isPageActive) 1_000L else 3_000L)
            value = System.currentTimeMillis()
        }
    }
    val runtimeText = if (!uiState.running || uiState.runningSinceEpochMs <= 0L) {
        runtimePendingText
    } else {
        formatMcpUptimeText(runtimeNowMs - uiState.runningSinceEpochMs)
    }
    val bindAddress = remember(uiState.allowExternal, uiState.addresses) {
        when {
            !uiState.allowExternal -> "127.0.0.1"
            uiState.addresses.isNotEmpty() -> uiState.addresses.first()
            else -> "0.0.0.0"
        }
    }
    val tokenPreview = remember(uiState.authToken) {
        uiState.authToken.toMcpTokenPreview()
    }.ifBlank { context.getString(R.string.common_na) }
    val overviewMetrics = listOf(
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_service_short),
            value = uiState.serverName.ifBlank { context.getString(R.string.mcp_default_service_name) },
            spanFullWidth = true,
            valueMaxLines = 1,
            labelWeight = 0.24f,
            valueWeight = 0.76f
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_endpoint_short),
            value = uiState.endpointPath
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_bind_short),
            value = bindAddress
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_port_short),
            value = uiState.port.toString()
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_network_short),
            value = if (uiState.allowExternal) {
                context.getString(R.string.mcp_network_mode_lan_short)
            } else {
                context.getString(R.string.mcp_network_mode_local_only_short)
            }
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_clients_short),
            value = context.getString(R.string.mcp_clients_count, uiState.connectedClients),
            valueColor = if (uiState.connectedClients > 0) runningColor else subtitleColor,
            valueMaxLines = 1
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_token_short),
            value = tokenPreview,
            valueColor = titleColor,
            valueMaxLines = 1,
            labelWeight = 0.34f,
            valueWeight = 0.66f
        )
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
    val surfaceColor = MiuixTheme.colorScheme.surface
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    val topBarBackdrop: LayerBackdrop = key("mcp-topbar-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val contentBackdrop: LayerBackdrop = key("mcp-content-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val sheetBackdrop: LayerBackdrop = key("mcp-sheet-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    LaunchedEffect(scrollToTopSignal, isPageActive) {
        if (isPageActive && scrollToTopSignal > 0) listState.animateScrollToItem(0)
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
                    val json = mcpServerManager.buildConfigJson(endpoint)
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
                backdrop = topBarBackdrop,
                layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
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
                bottomExtra = appPageBottomPaddingWithFloatingOverlay(contentBottomPadding),
                sectionSpacing = 12.dp
            ) {
                item {
                    McpOverviewCardSection(
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        overviewCardColor = overviewCardColor,
                        overviewBorderColor = overviewBorderColor,
                        overviewAccentColor = overviewAccentColor,
                        runtimeText = runtimeText,
                        isDark = isDark,
                        running = uiState.running,
                        overviewMetrics = overviewMetrics,
                        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                        onToggleServer = toggleServer,
                        onOpenEditSheet = { showEditSheet = true }
                    )
                }
                item {
                    McpServiceControlSection(
                        backdrop = contentBackdrop,
                        expanded = controlExpanded,
                        onExpandedChange = { controlExpanded = it },
                        mcpServerManager = mcpServerManager,
                        unknownText = unknownText,
                        onShowResetConfigConfirm = { showResetConfigConfirm = true }
                    )
                }
                item {
                    McpToolsSection(
                        backdrop = contentBackdrop,
                        expanded = configExpanded,
                        onExpandedChange = { configExpanded = it },
                        uiState = uiState
                    )
                }
                item {
                    McpLogsSection(
                        backdrop = contentBackdrop,
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
                    backdrop = contentBackdrop,
                    icon = if (uiState.running) appLucidePauseIcon() else osLucideRunIcon(),
                    contentDescription = if (uiState.running) {
                        stringResource(R.string.mcp_action_stop_service)
                    } else {
                        stringResource(R.string.mcp_action_start_service)
                    },
                    onClick = toggleServer,
                    modifier = Modifier.padding(end = 14.dp, bottom = contentBottomPadding - 24.dp),
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
        backdrop = sheetBackdrop,
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
