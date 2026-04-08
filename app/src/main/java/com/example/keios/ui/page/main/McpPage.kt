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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
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
    val warning = MiuixTheme.colorScheme.error
    val inactive = MiuixTheme.colorScheme.onBackgroundVariant
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant

    val context = LocalContext.current
    val uiState by mcpServerManager.uiState.collectAsState()
    var portText by remember(uiState.port) { mutableStateOf(uiState.port.toString()) }
    var allowExternal by remember(uiState.allowExternal) { mutableStateOf(uiState.allowExternal) }
    var serverName by remember(uiState.serverName) { mutableStateOf(uiState.serverName) }
    var configExpanded by remember { mutableStateOf(true) }
    var logsExpanded by remember { mutableStateOf(true) }
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
            Button(
                modifier = Modifier.padding(top = 2.dp),
                onClick = {
                    mcpServerManager.refreshNow()
                    Toast.makeText(context, "已刷新", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.Refresh,
                    contentDescription = "刷新",
                    tint = MiuixTheme.colorScheme.onPrimary
                )
            }
        }
        Text(text = "MCP Server 功能", color = subtitleColor, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(12.dp))

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
        Spacer(modifier = Modifier.height(12.dp))

        FrostedBlock(
            backdrop = backdrop,
            title = "超级岛预览",
            subtitle = "实时模拟岛内文案与状态",
            accent = primary,
            content = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusPill(
                        label = if (uiState.running) "运行中" else "已停止",
                        color = if (uiState.running) success else inactive
                    )
                    StatusPill(
                        label = "在线 ${uiState.connectedClients}",
                        color = primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MiuixInfoItem("岛左侧", if (uiState.running) "MCP 运行中" else "MCP 已停止")
                MiuixInfoItem("岛右侧", "在线 ${uiState.connectedClients}")
                MiuixInfoItem("胶囊短文案", if (uiState.running) "运行中 · 在线 ${uiState.connectedClients}" else "MCP Offline")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

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
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { allowExternal = false }) {
                Text(if (!allowExternal) "仅本机(已选)" else "仅本机")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { allowExternal = true }) {
                Text(if (allowExternal) "局域网(已选)" else "局域网")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val port = portText.toIntOrNull()
                    if (port == null) {
                        Toast.makeText(context, "端口无效", Toast.LENGTH_SHORT).show()
                        return@Button
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
            ) {
                Text("启动服务")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    mcpServerManager.stop()
                    Toast.makeText(context, "MCP 服务已停止", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("停止服务")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    onRequestNotificationPermission()
                    Toast.makeText(context, "已发起通知权限申请", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(if (notificationPermissionGranted) "重新检查通知权限" else "申请通知权限")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    mcpServerManager.sendTestNotification()
                        .onSuccess { Toast.makeText(context, "已发送 MCP 测试通知", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, "发送失败: ${it.message}", Toast.LENGTH_SHORT).show() }
                }
            ) {
                Text("发送测试通知")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                Button(
                    onClick = {
                        val json = mcpServerManager.buildConfigJson(preferredEndpoint)
                        copyToClipboard(context, "mcp-config", json)
                        Toast.makeText(context, "MCP 配置已复制", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("复制当前配置")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        mcpServerManager.regenerateAuthToken()
                        Toast.makeText(context, "Token 已重置，需重连客户端", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("重置 Token")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            uiState.tools.forEach { tool ->
                MiuixInfoItem(tool.name, tool.description)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            Button(onClick = { mcpServerManager.clearLogs() }) {
                Text("清空日志")
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
