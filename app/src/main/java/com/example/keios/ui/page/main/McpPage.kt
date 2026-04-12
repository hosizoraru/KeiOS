package com.example.keios.ui.page.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
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
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

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
    val overviewGreen = runningColor.copy(alpha = 0.16f)
    val overviewRed = stoppedColor.copy(alpha = 0.16f)

    val context = LocalContext.current
    val uiState by mcpServerManager.uiState.collectAsState()
    var portText by remember(uiState.port) { mutableStateOf(uiState.port.toString()) }
    var allowExternal by remember(uiState.allowExternal) { mutableStateOf(uiState.allowExternal) }
    var serverName by remember(uiState.serverName) { mutableStateOf(uiState.serverName) }
    var showEditSheet by remember { mutableStateOf(false) }
    var controlExpanded by remember { mutableStateOf(true) }
    var configExpanded by remember { mutableStateOf(false) }
    var logsExpanded by remember { mutableStateOf(false) }
    var showResetTokenConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
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
                title = "MCP",
                scrollBehavior = scrollBehavior,
                color = topBarMaterialBackdrop.getMiuixAppBarColor(),
                actions = {
                    LiquidActionBar(
                        backdrop = backdrop,
                        items = listOf(
                            LiquidActionItem(
                                icon = if (uiState.running) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                                contentDescription = if (uiState.running) "停止服务" else "启动服务",
                                onClick = toggleServer
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Edit,
                                contentDescription = "编辑服务参数",
                                onClick = { showEditSheet = true }
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Report,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 12.dp,
                end = 12.dp
            )
        ) {
            item { SmallTitle("本地 MCP 服务") }
            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (uiState.running) overviewGreen else overviewRed,
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("MCP Server", color = titleColor)
                            StatusPill(
                                label = if (uiState.running) "Server Running" else "Server Stopped",
                                color = if (uiState.running) runningColor else stoppedColor
                            )
                        }
                        Text(
                            text = "${if (uiState.running) "运行中" else "未运行"} · 在线客户端 ${uiState.connectedClients}",
                            color = subtitleColor
                        )
                        Text(
                            text = "${uiState.port} 端口 · MCP 协议",
                            color = subtitleColor
                        )
                        Text(
                            text = if (uiState.allowExternal) "网络模式: 局域网可访问" else "网络模式: 仅本机访问",
                            color = subtitleColor
                        )
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
                        bottomBarStyle = true,
                        text = "发送测试通知",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            mcpServerManager.sendTestNotification()
                                .onSuccess { Toast.makeText(context, "已发送 MCP 测试通知", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, "发送失败: ${it.message}", Toast.LENGTH_SHORT).show() }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassTextButton(
                        backdrop = backdrop,
                        bottomBarStyle = true,
                        text = "查看 SKILL.md",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSkill
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
                    bottomBarStyle = true,
                    text = "清空日志",
                    onClick = { mcpServerManager.clearLogs() }
                )
            }
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
                bottomBarStyle = true,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                onClick = { showEditSheet = false }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                bottomBarStyle = true,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.34f)
                        .heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "服务名称",
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
                GlassSearchField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = "输入服务名称",
                    backdrop = backdrop,
                    bottomBarStyle = true,
                    singleLine = true,
                    modifier = Modifier.weight(0.66f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.34f)
                        .heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "服务端口",
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
                GlassSearchField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    label = "输入端口",
                    backdrop = backdrop,
                    bottomBarStyle = true,
                    singleLine = true,
                    modifier = Modifier.weight(0.66f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "网络访问范围",
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            McpNetworkModeOption(
                backdrop = backdrop,
                title = "仅本机",
                summary = "仅允许本机客户端通过 127.0.0.1 访问",
                selected = !allowExternal,
                onClick = { allowExternal = false }
            )
            Spacer(modifier = Modifier.height(8.dp))
            McpNetworkModeOption(
                backdrop = backdrop,
                title = "局域网",
                summary = "允许同一局域网设备接入，请注意网络安全",
                selected = allowExternal,
                onClick = { allowExternal = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "危险操作",
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GlassTextButton(
                backdrop = backdrop,
                bottomBarStyle = true,
                text = "重置 Token",
                textColor = MiuixTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showResetTokenConfirm = true }
            )
            Spacer(modifier = Modifier.height(8.dp))
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
    backdrop: Backdrop?,
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = Color(0xFF22C55E)
    val cardColor = if (selected) selectedColor.copy(alpha = 0.12f) else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.defaultColors(
            color = cardColor,
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = MiuixTheme.colorScheme.onBackground
                    )
                    if (selected) {
                        StatusPill(
                            label = "已激活",
                            color = selectedColor
                        )
                    }
                }
                Text(
                    text = summary,
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
