package com.example.keios.ui.page.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

@Composable
fun McpPage(
    backdrop: Backdrop?,
    mcpServerManager: McpServerManager,
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0
) {
    val context = LocalContext.current
    val uiState by mcpServerManager.uiState.collectAsState()
    var portText by remember(uiState.port) { mutableStateOf(uiState.port.toString()) }
    var allowExternal by remember(uiState.allowExternal) { mutableStateOf(uiState.allowExternal) }
    var serverName by remember(uiState.serverName) { mutableStateOf(uiState.serverName) }
    var endpointExpanded by remember { mutableStateOf(true) }
    var toolsExpanded by remember { mutableStateOf(true) }
    var logsExpanded by remember { mutableStateOf(true) }
    var usageExpanded by remember { mutableStateOf(false) }
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
            Text(text = "MCP", modifier = Modifier.padding(top = 6.dp))
            Button(
                modifier = Modifier.padding(top = 2.dp),
                onClick = {
                    mcpServerManager.refreshNow()
                    Toast.makeText(context, "已刷新", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("刷新")
            }
        }
        Text(text = "MCP Server 功能", modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(12.dp))
        FrostedBlock(
            backdrop = backdrop,
            title = "Overview",
            subtitle = "MCP 服务总览",
            accent = Color(0xFF4B8DFF),
            content = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusPill(
                        label = if (uiState.running) "Server Running" else "Server Stopped",
                        color = if (uiState.running) Color(0xFF2E7D32) else Color(0xFF9E9E9E)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        label = if (uiState.allowExternal) "LAN Enabled" else "Local Only",
                        color = if (uiState.allowExternal) Color(0xFF1565C0) else Color(0xFF9E9E9E)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MiuixInfoItem("Endpoint", uiState.localEndpoint)
                MiuixInfoItem("Tools", uiState.tools.size.toString())
                MiuixInfoItem("Online Clients", uiState.connectedClients.toString())
                MiuixInfoItem("Auth", "Bearer ${uiState.authToken.take(8)}...${uiState.authToken.takeLast(8)}")
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

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "服务状态与地址",
            subtitle = if (uiState.running) "运行中" else "未运行",
            expanded = endpointExpanded,
            onExpandedChange = { endpointExpanded = it }
        ) {
            MiuixInfoItem("Running", uiState.running.toString())
            MiuixInfoItem("Host", uiState.host)
            MiuixInfoItem("Port", uiState.port.toString())
            MiuixInfoItem("Path", uiState.endpointPath)
            MiuixInfoItem("Local Endpoint", uiState.localEndpoint)
            MiuixInfoItem("Authorization", "Bearer ${uiState.authToken.take(8)}...${uiState.authToken.takeLast(8)}")
            if (uiState.allowExternal) {
                if (uiState.lanEndpoints.isEmpty()) {
                    MiuixInfoItem("LAN Endpoint", "未检测到可用 IPv4 地址")
                } else {
                    uiState.lanEndpoints.forEachIndexed { index, endpoint ->
                        MiuixInfoItem("LAN Endpoint ${index + 1}", endpoint)
                    }
                }
            }
            uiState.lastError?.let { MiuixInfoItem("Last Error", it) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "快速复制配置",
            subtitle = "MCP JSON",
            expanded = true,
            onExpandedChange = { }
        ) {
            val preferredEndpoint = when {
                uiState.allowExternal && uiState.lanEndpoints.isNotEmpty() -> uiState.lanEndpoints.first()
                else -> uiState.localEndpoint
            }
            MiuixInfoItem("推荐地址", preferredEndpoint)

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
                        val json = mcpServerManager.buildConfigJson("http://localhost:${uiState.port}${uiState.endpointPath}")
                        copyToClipboard(context, "mcp-config-localhost", json)
                        Toast.makeText(context, "localhost 配置已复制", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("复制 localhost")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        mcpServerManager.regenerateAuthToken()
                        Toast.makeText(context, "Token 已重置，需重连客户端", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("重置 Token")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "MCP Tools",
            subtitle = "${uiState.tools.size} 条",
            expanded = toolsExpanded,
            onExpandedChange = { toolsExpanded = it }
        ) {
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
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { mcpServerManager.clearLogs() }) {
                    Text("清空日志")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MiuixExpandableSection(
            backdrop = backdrop,
            title = "接入说明",
            subtitle = "Claw / Inspector",
            expanded = usageExpanded,
            onExpandedChange = { usageExpanded = it }
        ) {
            MiuixInfoItem("Step 1", "点击“启动服务”")
            MiuixInfoItem("Step 2", "在 Claw 中添加 MCP Streamable HTTP 连接")
            MiuixInfoItem("Step 3", "地址填写 Local Endpoint 或 LAN Endpoint")
            MiuixInfoItem("Step 4", "Authorization 填写 Bearer Token（可点“快速复制配置”）")
            MiuixInfoItem("Step 5", "连接成功后可调用 keios.get_shizuku_status / keios.get_app_version")
            MiuixInfoItem("Tip", "若 Claw 连接成功，Overview 的 Online Clients 会大于 0，日志也会记录连接变化")
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
