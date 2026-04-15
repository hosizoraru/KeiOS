package com.example.keios.ui.page.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SheetActionGroup
import com.example.keios.ui.page.main.widget.SheetChoiceCard
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.StatusLabelText
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlinx.coroutines.delay

@Composable
fun McpPage(
    mcpServerManager: McpServerManager,
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    cardPressFeedbackEnabled: Boolean = true,
    onOpenSkill: () -> Unit = {},
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val mcpTitle = stringResource(R.string.page_mcp_title)
    val unknownText = stringResource(R.string.common_unknown)
    val runtimePendingText = stringResource(R.string.mcp_runtime_pending)
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
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
    val overviewItemColor = overviewAccentColor.copy(alpha = 0.08f)
    val runtimeNowMs by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = uiState.running,
        key2 = uiState.runningSinceEpochMs
    ) {
        value = System.currentTimeMillis()
        while (uiState.running && uiState.runningSinceEpochMs > 0L) {
            delay(1_000L)
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
    val overviewMetrics = listOf(
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_service_name),
            value = uiState.serverName.ifBlank { context.getString(R.string.mcp_default_service_name) }
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_endpoint_path),
            value = uiState.endpointPath
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_bind_address),
            value = bindAddress
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_port),
            value = uiState.port.toString()
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_network_mode),
            value = if (uiState.allowExternal) {
                context.getString(R.string.mcp_network_mode_lan_accessible)
            } else {
                context.getString(R.string.mcp_network_mode_local_only_access)
            }
        ),
        McpOverviewMetric(
            label = context.getString(R.string.mcp_overview_label_clients),
            value = context.getString(R.string.mcp_clients_count, uiState.connectedClients),
            valueColor = if (uiState.connectedClients > 0) runningColor else subtitleColor
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
                if (available.y < -1f) showFloatingToggleButton = false
                if (available.y > 1f) showFloatingToggleButton = true
                return Offset.Zero
            }
        }
    }
    var controlExpanded by remember { mutableStateOf(true) }
    var configExpanded by remember { mutableStateOf(false) }
    var logsExpanded by remember { mutableStateOf(false) }
    var showResetTokenConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
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
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "",
                largeTitle = mcpTitle,
                scrollBehavior = scrollBehavior,
                color = topBarMaterialBackdrop.getMiuixAppBarColor(),
                actions = {
                    LiquidActionBar(
                        backdrop = backdrop,
                        items = listOf(
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Edit,
                                contentDescription = stringResource(R.string.mcp_action_edit_service_params),
                                onClick = { showEditSheet = true }
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Notes,
                                contentDescription = stringResource(R.string.mcp_action_open_skill_md),
                                onClick = onOpenSkill
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Copy,
                                contentDescription = stringResource(R.string.mcp_action_copy_current_config),
                                onClick = {
                                    val endpoint = if (allowExternal && uiState.addresses.isNotEmpty()) {
                                        "http://${uiState.addresses.first()}:${portText.toIntOrNull() ?: uiState.port}${uiState.endpointPath}"
                                    } else {
                                        "http://127.0.0.1:${portText.toIntOrNull() ?: uiState.port}${uiState.endpointPath}"
                                    }
                                    val json = mcpServerManager.buildConfigJson(endpoint)
                                    copyToClipboard(context, "mcp-config", json)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.mcp_toast_config_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Refresh,
                                contentDescription = stringResource(R.string.common_refresh),
                                onClick = {
                                    mcpServerManager.refreshNow()
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.common_refreshed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        ),
                        onInteractionChanged = onActionBarInteractingChanged
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(toggleButtonScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 64.dp,
                    start = 12.dp,
                    end = 12.dp
                )
            ) {
            item { SmallTitle(stringResource(R.string.mcp_page_local_service_title)) }
            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                val overviewShape = RoundedCornerShape(16.dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(overviewShape)
                        .background(overviewCardColor, overviewShape)
                        .border(width = 1.dp, color = overviewBorderColor, shape = overviewShape),
                    colors = CardDefaults.defaultColors(
                        color = overviewCardColor,
                        contentColor = titleColor
                    ),
                    showIndication = cardPressFeedbackEnabled,
                    onClick = toggleServer,
                    onLongPress = { showEditSheet = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.mcp_overview_title), color = titleColor)
                            Spacer(modifier = Modifier.weight(1f))
                            StatusPill(
                                label = runtimeText,
                                color = overviewAccentColor,
                                backgroundAlphaOverride = if (isDark) 0.18f else 0.24f,
                                borderAlphaOverride = if (isDark) 0.35f else 0.42f
                            )
                            StatusPill(
                                label = if (uiState.running) StatusLabelText.Running else StatusLabelText.NotRunning,
                                color = overviewAccentColor
                            )
                        }
                        overviewMetrics.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                McpOverviewMetricItem(
                                    metric = pair[0],
                                    cardColor = overviewItemColor,
                                    labelColor = subtitleColor,
                                    defaultValueColor = titleColor,
                                    modifier = Modifier.weight(1f)
                                )
                                if (pair.size > 1) {
                                    McpOverviewMetricItem(
                                        metric = pair[1],
                                        cardColor = overviewItemColor,
                                        labelColor = subtitleColor,
                                        defaultValueColor = titleColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                MiuixExpandableSection(
                backdrop = backdrop,
                title = stringResource(R.string.mcp_section_service_control_title),
                subtitle = stringResource(R.string.mcp_section_service_control_subtitle),
                expanded = controlExpanded,
                onExpandedChange = { controlExpanded = it },
                headerStartAction = {
                    McpSectionHeaderIcon(
                        icon = MiuixIcons.Regular.Tune,
                        contentDescription = stringResource(R.string.mcp_section_service_control_title)
                    )
                }
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.Content,
                        text = stringResource(R.string.mcp_action_send_test_notification),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            mcpServerManager.sendTestNotification()
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
                    )
                }
            }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                MiuixExpandableSection(
                backdrop = backdrop,
                title = stringResource(R.string.mcp_section_tools_title),
                subtitle = stringResource(R.string.mcp_section_tools_subtitle, uiState.tools.size),
                expanded = configExpanded,
                onExpandedChange = { configExpanded = it },
                headerStartAction = {
                    McpSectionHeaderIcon(
                        icon = MiuixIcons.Regular.GridView,
                        contentDescription = stringResource(R.string.mcp_section_tools_title)
                    )
                }
            ) {
                uiState.tools.forEach { tool ->
                    MiuixInfoItem(tool.name, tool.description)
                }
            }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                MiuixExpandableSection(
                backdrop = backdrop,
                title = stringResource(R.string.mcp_section_logs_title),
                subtitle = stringResource(R.string.mcp_section_logs_subtitle, uiState.logs.size),
                expanded = logsExpanded,
                onExpandedChange = { logsExpanded = it },
                headerStartAction = {
                    McpSectionHeaderIcon(
                        icon = MiuixIcons.Regular.Notes,
                        contentDescription = stringResource(R.string.mcp_section_logs_title)
                    )
                }
            ) {
                if (uiState.logs.isEmpty()) {
                    MiuixInfoItem(stringResource(R.string.mcp_log_label), stringResource(R.string.mcp_log_empty))
                } else {
                    uiState.logs.asReversed().forEach { log ->
                        MiuixInfoItem("${log.time} [${log.level}]", log.message)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                GlassTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.Content,
                    text = stringResource(R.string.mcp_action_clear_logs),
                    onClick = { mcpServerManager.clearLogs() }
                )
            }
            }
            }

            AnimatedVisibility(
                visible = showFloatingToggleButton,
                enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                    animationSpec = tween(220),
                    initialOffsetY = { it / 2 }
                ),
                exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { it / 2 }
                ),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                GlassIconButton(
                    backdrop = backdrop,
                    icon = if (uiState.running) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                    contentDescription = if (uiState.running) {
                        stringResource(R.string.mcp_action_stop_service)
                    } else {
                        stringResource(R.string.mcp_action_start_service)
                    },
                    onClick = toggleServer,
                    modifier = Modifier.padding(end = 14.dp, bottom = contentBottomPadding - 24.dp),
                    width = 60.dp,
                    height = 44.dp,
                    variant = GlassVariant.Bar
                )
            }
        }
    }

    SnapshotWindowBottomSheet(
        show = showEditSheet,
        title = stringResource(R.string.mcp_sheet_edit_service_title),
        onDismissRequest = { showEditSheet = false },
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Close,
                contentDescription = stringResource(R.string.common_close),
                onClick = { showEditSheet = false }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = stringResource(R.string.common_save),
                onClick = {
                    val port = portText.toIntOrNull()
                    if (port == null) {
                        Toast.makeText(context, context.getString(R.string.common_port_invalid), Toast.LENGTH_SHORT).show()
                        return@GlassIconButton
                    }
                    mcpServerManager.updateServerName(serverName)
                    mcpServerManager.updatePort(port).onFailure {
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.common_save_failed_with_reason,
                                it.message ?: unknownText
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@GlassIconButton
                    }
                    mcpServerManager.updateAllowExternal(allowExternal).onFailure {
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.common_save_failed_with_reason,
                                it.message ?: unknownText
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@GlassIconButton
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.mcp_toast_saved_requires_restart),
                        Toast.LENGTH_SHORT
                    ).show()
                    showEditSheet = false
                }
            )
        }
    ) {
        SheetContentColumn {
            SheetSectionTitle(stringResource(R.string.mcp_sheet_section_basic))
            SheetSectionCard {
                SheetControlRow(label = stringResource(R.string.mcp_overview_label_service_name)) {
                    GlassSearchField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = stringResource(R.string.mcp_input_service_name_hint),
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(serverNameFieldWidth)
                    )
                }
                SheetControlRow(label = stringResource(R.string.mcp_sheet_label_service_port)) {
                    GlassSearchField(
                        value = portText,
                        onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                        label = stringResource(R.string.mcp_input_port_hint),
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(portFieldWidth)
                    )
                }
            }
            SheetSectionTitle(stringResource(R.string.mcp_sheet_section_network_access))
            SheetActionGroup {
                McpNetworkModeOption(
                    title = stringResource(R.string.mcp_network_mode_local_only_short),
                    summary = stringResource(R.string.mcp_network_mode_local_only_summary),
                    selected = !allowExternal,
                    onClick = { allowExternal = false }
                )
                McpNetworkModeOption(
                    title = stringResource(R.string.mcp_network_mode_lan_short),
                    summary = stringResource(R.string.mcp_network_mode_lan_summary),
                    selected = allowExternal,
                    onClick = { allowExternal = true }
                )
            }
            SheetSectionTitle(
                text = stringResource(R.string.common_danger_zone),
                danger = true
            )
            SheetSectionCard {
                GlassTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetDangerAction,
                    text = stringResource(R.string.mcp_action_reset_token),
                    textColor = MiuixTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showResetTokenConfirm = true }
                )
            }
        }
    }

    WindowDialog(
        show = showResetTokenConfirm,
        title = stringResource(R.string.mcp_action_reset_token),
        summary = stringResource(R.string.mcp_reset_token_confirm_summary),
        onDismissRequest = { showResetTokenConfirm = false }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_cancel),
                    onClick = { showResetTokenConfirm = false }
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_reset),
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = MiuixTheme.colorScheme.onError
                    ),
                    onClick = {
                        mcpServerManager.regenerateAuthToken()
                        Toast.makeText(
                            context,
                            context.getString(R.string.mcp_toast_token_reset_reconnect),
                            Toast.LENGTH_SHORT
                        ).show()
                        showResetTokenConfirm = false
                    }
                )
            }
        }
    }
}

@Composable
private fun McpNetworkModeOption(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = Color(0xFF22C55E)
    SheetChoiceCard(
        title = title,
        summary = summary,
        selected = selected,
        onSelect = onClick,
        accentColor = selectedColor,
        selectedLabel = StatusLabelText.Activated
    )
}

@Composable
private fun McpOverviewMetricItem(
    metric: McpOverviewMetric,
    cardColor: Color,
    labelColor: Color,
    defaultValueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.defaultColors(
            color = cardColor,
            contentColor = defaultValueColor
        ),
        showIndication = false,
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = metric.label,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metric.value.ifBlank { stringResource(R.string.common_na) },
                color = metric.valueColor ?: defaultValueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class McpOverviewMetric(
    val label: String,
    val value: String,
    val valueColor: Color? = null
)

@Composable
private fun McpSectionHeaderIcon(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    top.yukonga.miuix.kmp.basic.Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MiuixTheme.colorScheme.primary,
        modifier = modifier
            .size(22.dp)
            .defaultMinSize(minHeight = 22.dp)
    )
}

@Composable
private fun formatMcpUptimeText(durationMs: Long): String {
    val totalMinutes = (durationMs.coerceAtLeast(0L) / 60_000L)
    val days = totalMinutes / 1_440L
    val hours = (totalMinutes % 1_440L) / 60L
    val minutes = totalMinutes % 60L
    return when {
        days > 0L && minutes == 0L -> stringResource(R.string.mcp_uptime_days_hours, days, hours)
        days > 0L -> stringResource(R.string.mcp_uptime_days_hours_minutes, days, hours, minutes)
        hours > 0L && minutes == 0L -> stringResource(R.string.mcp_uptime_hours, hours)
        hours > 0L -> stringResource(R.string.mcp_uptime_hours_minutes, hours, minutes)
        else -> stringResource(R.string.mcp_uptime_minutes, minutes)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
