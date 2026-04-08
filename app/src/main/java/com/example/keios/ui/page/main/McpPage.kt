package com.example.keios.ui.page.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun McpPage(
    backdrop: Backdrop?,
    mcpServerManager: McpServerManager,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0
) {
    val primary = MiuixTheme.colorScheme.primary
    val success = MiuixTheme.colorScheme.secondary
    val inactive = MiuixTheme.colorScheme.onBackgroundVariant
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant

    val context = LocalContext.current
    val uiState by mcpServerManager.uiState.collectAsState()
    var portText by remember(uiState.port) { mutableStateOf(uiState.port.toString()) }
    var allowExternal by remember(uiState.allowExternal) { mutableStateOf(uiState.allowExternal) }
    var serverName by remember(uiState.serverName) { mutableStateOf(uiState.serverName) }
    var controlExpanded by remember { mutableStateOf(true) }
    var configExpanded by remember { mutableStateOf(true) }
    var logsExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) scrollState.animateScrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = contentBottomPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "MCP", color = titleColor, modifier = Modifier.padding(top = 6.dp))
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Refresh,
                contentDescription = "刷新",
                modifier = Modifier.padding(top = 2.dp),
                onClick = {
                    mcpServerManager.refreshNow()
                    Toast.makeText(context, "已刷新", Toast.LENGTH_SHORT).show()
                }
            )
        }
        Text(text = "MCP Server 功能", color = subtitleColor, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(10.dp))

        FrostedBlock(
            backdrop = backdrop,
            title = "Overview",
            subtitle = "服务状态总览",
            accent = primary,
            content = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusPill(
                        label = if (uiState.running) "Server Running" else "Server Stopped",
                        color = if (uiState.running) success else inactive
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MiuixInfoItem(
                    "MCP Server",
                    "${if (uiState.running) "运行中" else "未运行"} · 在线 ${uiState.connectedClients} · ${uiState.port} 端口 · MCP 协议"
                )
                MiuixInfoItem("Tools", uiState.tools.size.toString())
                MiuixInfoItem("通知权限", if (notificationPermissionGranted) "已授权" else "未授权")
            }
        )
        Spacer(modifier = Modifier.height(10.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "服务控制",
            subtitle = "启动 / 停止 / 通知 / 网络范围",
            expanded = controlExpanded,
            onExpandedChange = { controlExpanded = it }
        ) {
            TextField(
                value = portText,
                onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                label = "服务端口",
                useLabelAsPlaceholder = true,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = serverName,
                onValueChange = {
                    serverName = it
                    mcpServerManager.updateServerName(it)
                },
                label = "服务名称（配置展示名）",
                useLabelAsPlaceholder = true,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = if (!allowExternal) "仅本机(已选)" else "仅本机",
                    onClick = { allowExternal = false }
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlassTextButton(
                    backdrop = backdrop,
                    text = if (allowExternal) "局域网(已选)" else "局域网",
                    onClick = { allowExternal = true }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = "启动服务",
                    onClick = {
                        val port = portText.toIntOrNull()
                        if (port == null) {
                            Toast.makeText(context, "端口无效", Toast.LENGTH_SHORT).show()
                            return@GlassTextButton
                        }
                        mcpServerManager.start(port = port, allowExternal = allowExternal)
                            .onSuccess {
                                mcpServerManager.refreshAddresses()
                                Toast.makeText(context, "MCP 服务已启动", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure {
                                Toast.makeText(context, "启动失败: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlassTextButton(
                    backdrop = backdrop,
                    text = "停止服务",
                    onClick = {
                        mcpServerManager.stop()
                        Toast.makeText(context, "MCP 服务已停止", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = if (notificationPermissionGranted) "重新检查通知权限" else "申请通知权限",
                    onClick = {
                        onRequestNotificationPermission()
                        Toast.makeText(context, "已发起通知权限申请", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlassTextButton(
                    backdrop = backdrop,
                    text = "发送测试通知",
                    onClick = {
                        mcpServerManager.sendTestNotification()
                            .onSuccess { Toast.makeText(context, "已发送 MCP 测试通知", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, "发送失败: ${it.message}", Toast.LENGTH_SHORT).show() }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "配置与工具",
            subtitle = "Endpoint / JSON / Tools",
            expanded = configExpanded,
            onExpandedChange = { configExpanded = it }
        ) {
            val preferredEndpoint = when {
                uiState.allowExternal && uiState.lanEndpoints.isNotEmpty() -> uiState.lanEndpoints.first()
                else -> uiState.localEndpoint
            }
            MiuixInfoItem("Running", uiState.running.toString())
            MiuixInfoItem("Host", uiState.host)
            MiuixInfoItem("Port", uiState.port.toString())
            MiuixInfoItem("Path", uiState.endpointPath)
            MiuixInfoItem("推荐地址", preferredEndpoint)
            MiuixInfoItem("Authorization", "Bearer ${uiState.authToken.take(8)}...${uiState.authToken.takeLast(8)}")
            uiState.lastError?.let { MiuixInfoItem("Last Error", it) }
            if (uiState.allowExternal && uiState.lanEndpoints.isNotEmpty()) {
                uiState.lanEndpoints.forEachIndexed { index, endpoint ->
                    MiuixInfoItem("LAN Endpoint ${index + 1}", endpoint)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = "复制当前配置",
                    onClick = {
                        val json = mcpServerManager.buildConfigJson(preferredEndpoint)
                        copyToClipboard(context, "mcp-config", json)
                        Toast.makeText(context, "MCP 配置已复制", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlassTextButton(
                    backdrop = backdrop,
                    text = "重置 Token",
                    onClick = {
                        mcpServerManager.regenerateAuthToken()
                        Toast.makeText(context, "Token 已重置，需重连客户端", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            uiState.tools.forEach { tool ->
                MiuixInfoItem(tool.name, tool.description)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                text = "清空日志",
                onClick = { mcpServerManager.clearLogs() }
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
