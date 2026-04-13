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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val runtimeText = remember(uiState.running, uiState.runningSinceEpochMs, runtimeNowMs) {
        if (!uiState.running || uiState.runningSinceEpochMs <= 0L) {
            "待运行"
        } else {
            formatMcpUptime(runtimeNowMs - uiState.runningSinceEpochMs)
        }
    }
    val bindAddress = remember(uiState.allowExternal, uiState.addresses) {
        when {
            !uiState.allowExternal -> "127.0.0.1"
            uiState.addresses.isNotEmpty() -> uiState.addresses.first()
            else -> "0.0.0.0"
        }
    }
    val overviewMetrics = remember(
        uiState.connectedClients,
        uiState.serverName,
        bindAddress,
        uiState.port,
        uiState.endpointPath,
        uiState.allowExternal
    ) {
        listOf(
            McpOverviewMetric(
                label = "服务名称",
                value = uiState.serverName.ifBlank { "KeiOS MCP" }
            ),
            McpOverviewMetric(label = "入口路径", value = uiState.endpointPath),
            McpOverviewMetric(label = "绑定地址", value = bindAddress),
            McpOverviewMetric(label = "端口", value = uiState.port.toString()),
            McpOverviewMetric(
                label = "网络模式",
                value = if (uiState.allowExternal) "局域网可访问" else "仅本机访问"
            ),
            McpOverviewMetric(
                label = "客户端数量",
                value = "${uiState.connectedClients} 个",
                valueColor = if (uiState.connectedClients > 0) runningColor else subtitleColor
            )
        )
    }
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
    val serverNameFieldWidth = remember(serverName) {
        val visibleChars = serverName.trim().ifBlank { "输入服务名称" }.length.coerceIn(6, 18)
        (visibleChars * 11 + 36).dp
    }
    val portFieldWidth = remember(portText) {
        val visibleChars = portText.trim().ifBlank { "38888" }.length.coerceIn(4, 6)
        (visibleChars * 14 + 28).dp
    }
    val toggleServer: () -> Unit = {
        if (uiState.running) {
            mcpServerManager.stop()
            Toast.makeText(context, "MCP 服务已停止", Toast.LENGTH_SHORT).show()
        } else {
            val port = portText.toIntOrNull()
            if (port == null) {
                Toast.makeText(context, "端口无效", Toast.LENGTH_SHORT).show()
            } else {
                mcpServerManager.updateServerName(serverName)
                mcpServerManager.start(port = port, allowExternal = allowExternal)
                    .onSuccess {
                        mcpServerManager.refreshAddresses()
                        Toast.makeText(context, "MCP 服务已启动", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(context, "启动失败: ${it.message}", Toast.LENGTH_SHORT).show()
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
                largeTitle = "MCP",
                scrollBehavior = scrollBehavior,
                color = topBarMaterialBackdrop.getMiuixAppBarColor(),
                actions = {
                    LiquidActionBar(
                        backdrop = backdrop,
                        items = listOf(
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Edit,
                                contentDescription = "编辑服务参数",
                                onClick = { showEditSheet = true }
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Notes,
                                contentDescription = "查看 SKILL.md",
                                onClick = onOpenSkill
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Copy,
                                contentDescription = "复制当前配置",
                                onClick = {
                                    val endpoint = if (allowExternal && uiState.addresses.isNotEmpty()) {
                                        "http://${uiState.addresses.first()}:${portText.toIntOrNull() ?: uiState.port}${uiState.endpointPath}"
                                    } else {
                                        "http://127.0.0.1:${portText.toIntOrNull() ?: uiState.port}${uiState.endpointPath}"
                                    }
                                    val json = mcpServerManager.buildConfigJson(endpoint)
                                    copyToClipboard(context, "mcp-config", json)
                                    Toast.makeText(context, "MCP 配置已复制", Toast.LENGTH_SHORT).show()
                                }
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Refresh,
                                contentDescription = "刷新",
                                onClick = {
                                    mcpServerManager.refreshNow()
                                    Toast.makeText(context, "已刷新", Toast.LENGTH_SHORT).show()
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
            item { SmallTitle("本地 MCP 服务") }
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
                            Text("MCP Server", color = titleColor)
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
                title = "服务控制",
                subtitle = "通知与连接调试",
                expanded = controlExpanded,
                onExpandedChange = { controlExpanded = it }
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.Content,
                        text = "发送测试通知",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            mcpServerManager.sendTestNotification()
                                .onSuccess { Toast.makeText(context, "已发送 MCP 测试通知", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, "发送失败: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    )
                }
            }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                MiuixExpandableSection(
                backdrop = backdrop,
                title = "工具",
                subtitle = "${uiState.tools.size} 个工具",
                expanded = configExpanded,
                onExpandedChange = { configExpanded = it }
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
                title = "MCP Logs",
                subtitle = "${uiState.logs.size} 条",
                expanded = logsExpanded,
                onExpandedChange = { logsExpanded = it }
            ) {
                if (uiState.logs.isEmpty()) {
                    MiuixInfoItem("Log", "暂无日志")
                } else {
                    uiState.logs.asReversed().forEach { log ->
                        MiuixInfoItem("${log.time} [${log.level}]", log.message)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                GlassTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.Content,
                    text = "清空日志",
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
                    contentDescription = if (uiState.running) "停止服务" else "启动服务",
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
        title = "编辑 MCP 服务",
        onDismissRequest = { showEditSheet = false },
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                onClick = { showEditSheet = false }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = "保存",
                onClick = {
                    val port = portText.toIntOrNull()
                    if (port == null) {
                        Toast.makeText(context, "端口无效", Toast.LENGTH_SHORT).show()
                        return@GlassIconButton
                    }
                    mcpServerManager.updateServerName(serverName)
                    mcpServerManager.updatePort(port).onFailure {
                        Toast.makeText(context, "保存失败: ${it.message}", Toast.LENGTH_SHORT).show()
                        return@GlassIconButton
                    }
                    mcpServerManager.updateAllowExternal(allowExternal).onFailure {
                        Toast.makeText(context, "保存失败: ${it.message}", Toast.LENGTH_SHORT).show()
                        return@GlassIconButton
                    }
                    Toast.makeText(context, "已保存，修改将在下次启动或重启服务后生效", Toast.LENGTH_SHORT).show()
                    showEditSheet = false
                }
            )
        }
    ) {
        SheetContentColumn {
            SheetSectionTitle("基础设置")
            SheetSectionCard {
                SheetControlRow(label = "服务名称") {
                    GlassSearchField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = "输入服务名称",
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(serverNameFieldWidth)
                    )
                }
                SheetControlRow(label = "服务端口") {
                    GlassSearchField(
                        value = portText,
                        onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                        label = "输入端口",
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(portFieldWidth)
                    )
                }
            }
            SheetSectionTitle("网络访问范围")
            SheetActionGroup {
                McpNetworkModeOption(
                    title = "仅本机",
                    summary = "仅允许本机客户端通过 127.0.0.1 访问",
                    selected = !allowExternal,
                    onClick = { allowExternal = false }
                )
                McpNetworkModeOption(
                    title = "局域网",
                    summary = "允许同一局域网设备接入，请注意网络安全",
                    selected = allowExternal,
                    onClick = { allowExternal = true }
                )
            }
            SheetSectionTitle(
                text = "危险操作",
                danger = true
            )
            SheetSectionCard {
                GlassTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetDangerAction,
                    text = "重置 Token",
                    textColor = MiuixTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showResetTokenConfirm = true }
                )
            }
        }
    }

    WindowDialog(
        show = showResetTokenConfirm,
        title = "重置 Token",
        summary = "重置后，现有客户端需要重新配置或重新连接。确定继续吗？",
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
                    text = "取消",
                    onClick = { showResetTokenConfirm = false }
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = "重置",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = MiuixTheme.colorScheme.onError
                    ),
                    onClick = {
                        mcpServerManager.regenerateAuthToken()
                        Toast.makeText(context, "Token 已重置，需重连客户端", Toast.LENGTH_SHORT).show()
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
                text = metric.value.ifBlank { "N/A" },
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

private fun formatMcpUptime(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (days > 0L) {
        "%d天 %02d:%02d:%02d".format(days, hours, minutes, seconds)
    } else {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
