package com.example.keios.mcp

import android.net.Uri
import com.example.keios.ui.utils.ShizukuApiUtils
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

data class McpToolMeta(
    val name: String,
    val description: String
)

class LocalMcpService(
    private val shizukuApiUtils: ShizukuApiUtils,
    private val appVersionName: String,
    private val appVersionCode: Long,
    private val appPackageName: String,
    private val appLabel: String
) {
    private data class InfoRow(
        val key: String,
        val value: String
    )

    companion object {
        private const val SYSTEM_CACHE_KV_ID = "system_info_cache"
        private const val KEY_SYSTEM = "section_system_table"
        private const val KEY_SECURE = "section_secure_table"
        private const val KEY_GLOBAL = "section_global_table"
        private const val KEY_ANDROID = "section_android_properties"
        private const val KEY_JAVA = "section_java_properties"
        private const val KEY_LINUX = "section_linux_environment"
    }

    @Volatile
    private var cachedServer: Server? = null

    @Volatile
    private var mcpStateProvider: (() -> McpServerUiState)? = null

    fun bindMcpStateProvider(provider: () -> McpServerUiState) {
        mcpStateProvider = provider
    }

    fun getOrCreateServer(): Server {
        cachedServer?.let { return it }
        val created = createServer()
        cachedServer = created
        return created
    }

    fun listLocalTools(): List<McpToolMeta> {
        return listOf(
            McpToolMeta("keios.ping", "Health check，用于测试 MCP 通道是否可用"),
            McpToolMeta("keios.get_app_info", "读取本 APP 关键信息（包名、版本、Shizuku API）"),
            McpToolMeta("keios.get_app_version", "读取本 APP 版本名与版本号"),
            McpToolMeta("keios.get_shizuku_status", "读取当前 Shizuku 权限与状态"),
            McpToolMeta("keios.get_system_topinfo", "读取系统参数 TopInfo 缓存快照"),
            McpToolMeta("keios.search_system_topinfo", "按关键字筛选 TopInfo（参数 query）"),
            McpToolMeta("keios.get_mcp_status", "读取 MCP 服务状态（端口、连接数、地址）"),
            McpToolMeta("keios.get_mcp_logs", "读取 MCP 最近日志"),
            McpToolMeta("keios.get_mcp_config_template", "读取 MCP 客户端接入配置模板 JSON")
        )
    }

    private fun createServer(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "keios-local-mcp",
                version = appVersionName
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false)
                )
            )
        )

        server.addTool(
            name = "keios.ping",
            description = "Health check for MCP connection.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            CallToolResult(content = listOf(TextContent("pong")))
        }

        server.addTool(
            name = "keios.get_app_info",
            description = "Get app info from KeiOS.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val lines = listOf(
                "label=$appLabel",
                "package=$appPackageName",
                "versionName=$appVersionName",
                "versionCode=$appVersionCode",
                "shizukuApi=${ShizukuApiUtils.API_VERSION}"
            )
            CallToolResult(content = listOf(TextContent(lines.joinToString("\n"))))
        }

        server.addTool(
            name = "keios.get_shizuku_status",
            description = "Get current Shizuku status from KeiOS app.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            CallToolResult(content = listOf(TextContent(shizukuApiUtils.currentStatus())))
        }

        server.addTool(
            name = "keios.get_app_version",
            description = "Get KeiOS app version name and version code.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val text = "versionName=$appVersionName, versionCode=$appVersionCode"
            CallToolResult(content = listOf(TextContent(text)))
        }

        server.addTool(
            name = "keios.get_system_topinfo",
            description = "Get TopInfo snapshot from cached system sections.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val rows = readSystemTopInfoRows(maxCount = 120, query = null)
            val text = if (rows.isEmpty()) "TopInfo cache empty. Open System page once to build cache." else {
                rows.joinToString("\n") { "${it.key}=${it.value}" }
            }
            CallToolResult(content = listOf(TextContent(text)))
        }

        server.addTool(
            name = "keios.search_system_topinfo",
            description = "Search TopInfo with keyword argument: query.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("query", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val query = request.arguments?.get("query")?.let { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
            val rows = readSystemTopInfoRows(maxCount = 120, query = query)
            val text = if (rows.isEmpty()) "No matched TopInfo rows." else {
                rows.joinToString("\n") { "${it.key}=${it.value}" }
            }
            CallToolResult(content = listOf(TextContent(text)))
        }

        server.addTool(
            name = "keios.get_mcp_status",
            description = "Get MCP server runtime status.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val state = mcpStateProvider?.invoke()
            val text = if (state == null) {
                "MCP state unavailable"
            } else {
                buildString {
                    appendLine("running=${state.running}")
                    appendLine("host=${state.host}")
                    appendLine("port=${state.port}")
                    appendLine("path=${state.endpointPath}")
                    appendLine("connectedClients=${state.connectedClients}")
                    appendLine("allowExternal=${state.allowExternal}")
                    appendLine("localEndpoint=${state.localEndpoint}")
                    if (state.lanEndpoints.isNotEmpty()) {
                        appendLine("lanEndpoints=${state.lanEndpoints.joinToString(",")}")
                    }
                }.trim()
            }
            CallToolResult(content = listOf(TextContent(text)))
        }

        server.addTool(
            name = "keios.get_mcp_logs",
            description = "Get MCP logs from in-app log buffer.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val state = mcpStateProvider?.invoke()
            val text = if (state == null || state.logs.isEmpty()) {
                "No logs."
            } else {
                state.logs.asReversed()
                    .take(80)
                    .joinToString("\n") { "[${it.time}] [${it.level}] ${it.message}" }
            }
            CallToolResult(content = listOf(TextContent(text)))
        }

        server.addTool(
            name = "keios.get_mcp_config_template",
            description = "Get Streamable HTTP client config template JSON.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val state = mcpStateProvider?.invoke()
            val endpoint = state?.localEndpoint ?: "http://localhost:38888/mcp"
            val token = state?.authToken ?: "YOUR_TOKEN"
            val config = buildString {
                appendLine("{")
                appendLine("  \"mcpServers\": {")
                appendLine("    \"KeiOS MCP\": {")
                appendLine("      \"type\": \"streamablehttp\",")
                appendLine("      \"url\": \"$endpoint\",")
                appendLine("      \"headers\": {")
                appendLine("        \"Authorization\": \"Bearer $token\"")
                appendLine("      }")
                appendLine("    }")
                appendLine("  }")
                appendLine("}")
            }.trim()
            CallToolResult(content = listOf(TextContent(config)))
        }

        return server
    }

    private fun decodeRows(raw: String?): List<InfoRow> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val index = line.indexOf('\t')
            if (index <= 0) return@mapNotNull null
            val key = Uri.decode(line.substring(0, index)).trim()
            val value = Uri.decode(line.substring(index + 1)).trim()
            if (key.isBlank() || value.isBlank()) null else InfoRow(key, value)
        }.toList()
    }

    private fun readSystemTopInfoRows(
        maxCount: Int,
        query: String?
    ): List<InfoRow> {
        val kv = com.tencent.mmkv.MMKV.mmkvWithID(SYSTEM_CACHE_KV_ID)
        val allRows = (
            decodeRows(kv.decodeString(KEY_SYSTEM)) +
                decodeRows(kv.decodeString(KEY_SECURE)) +
                decodeRows(kv.decodeString(KEY_GLOBAL)) +
                decodeRows(kv.decodeString(KEY_ANDROID)) +
                decodeRows(kv.decodeString(KEY_JAVA)) +
                decodeRows(kv.decodeString(KEY_LINUX))
            )
            .distinctBy { "${it.key}\u0000${it.value}" }

        val topKeyHints = listOf(
            "long_press", "fbo", "adb", "share_", "voice_", "autofill", "credential", "zygote",
            "dexopt", "dex2oat", "tango", "aod", "vulkan", "opengl", "graphics", "density", "gsm",
            "miui", "version", "build", "security_patch", "lc3", "lea", "usb", "getprop", "env."
        )
        val filtered = allRows.filter { row ->
            topKeyHints.any { hint -> row.key.contains(hint, ignoreCase = true) }
        }

        val queryText = query?.trim().orEmpty()
        val queried = if (queryText.isBlank()) {
            filtered
        } else {
            filtered.filter {
                it.key.contains(queryText, ignoreCase = true) ||
                    it.value.contains(queryText, ignoreCase = true)
            }
        }

        return queried.take(maxCount)
    }
}
