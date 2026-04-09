package com.example.keios.mcp

import android.content.Context
import android.net.Uri
import com.example.keios.ui.utils.GitHubTrackStore
import com.example.keios.ui.utils.GitHubTrackedApp
import com.example.keios.ui.utils.GitHubVersionUtils
import com.example.keios.ui.utils.ShizukuApiUtils
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
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
    private val appContext: Context,
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

    private data class GitHubCheckRow(
        val item: GitHubTrackedApp,
        val localVersion: String,
        val stableVersion: String,
        val preReleaseVersion: String,
        val status: String,
        val hasUpdate: Boolean
    )

    companion object {
        private const val SYSTEM_CACHE_KV_ID = "system_info_cache"
        private const val KEY_SYSTEM = "section_system_table"
        private const val KEY_SECURE = "section_secure_table"
        private const val KEY_GLOBAL = "section_global_table"
        private const val KEY_ANDROID = "section_android_properties"
        private const val KEY_JAVA = "section_java_properties"
        private const val KEY_LINUX = "section_linux_environment"

        private const val DEFAULT_TOPINFO_LIMIT = 120
        private const val MAX_TOPINFO_LIMIT = 300
        private const val DEFAULT_LOG_LIMIT = 80
        private const val MAX_LOG_LIMIT = 200

        private const val MIME_MARKDOWN = "text/markdown"
        private const val MIME_TEXT = "text/plain"
        private const val MIME_JSON = "application/json"

        private const val SKILL_RESOURCE_URI = "keios://skill/claw-skill.md"
        private const val SKILL_OVERVIEW_URI = "keios://skill/overview.txt"
        private const val SKILL_TOOL_TEMPLATE_URI = "keios://skill/tool/{tool}"
        private const val CLAW_IMPORT_RESOURCE_URI = "keios://claw/import/auto.json"
        private const val CLAW_IMPORT_TEMPLATE_URI = "keios://claw/import/{mode}.json"
        private const val CLAW_BOOTSTRAP_PROMPT = "keios.claw.bootstrap"
        private const val DEFAULT_ENDPOINT = "http://127.0.0.1:38888/mcp"
    }

    @Volatile
    private var cachedServer: Server? = null

    @Volatile
    private var mcpStateProvider: (() -> McpServerUiState)? = null

    private val serverInstructions: String by lazy {
        buildServerInstructions()
    }

    fun bindMcpStateProvider(provider: () -> McpServerUiState) {
        mcpStateProvider = provider
    }

    fun getOrCreateServer(): Server {
        cachedServer?.let { return it }
        val created = createServer()
        cachedServer = created
        return created
    }

    fun getSkillMarkdownForUi(): String {
        return loadSkillMarkdown()
    }

    fun listLocalTools(): List<McpToolMeta> {
        return listOf(
            McpToolMeta("keios.health.ping", "连通性检测，用于确认 Claw 与 KeiOS MCP 通道正常"),
            McpToolMeta("keios.app.get_info", "读取 KeiOS 应用基本信息（包名、版本、Shizuku API）"),
            McpToolMeta("keios.app.get_version", "读取 KeiOS 应用版本信息"),
            McpToolMeta("keios.shizuku.get_status", "读取 Shizuku 可用性与授权状态"),
            McpToolMeta("keios.system.topinfo.list", "读取系统参数 TopInfo（支持 query、limit 参数）"),
            McpToolMeta("keios.system.topinfo.search", "按关键字检索系统参数 TopInfo"),
            McpToolMeta("keios.mcp.get_status", "读取 MCP 服务运行状态（端口、地址、在线客户端）"),
            McpToolMeta("keios.mcp.get_logs", "读取 MCP 运行日志（支持 limit 参数）"),
            McpToolMeta("keios.mcp.get_client_config", "生成可直接用于客户端接入的 MCP JSON 配置"),
            McpToolMeta("keios.mcp.get_claw_import_package", "生成 Claw 可直接导入的 MCP 配置包（支持 mode 参数）"),
            McpToolMeta("keios.github.tracked.list", "读取 GitHub 跟踪列表"),
            McpToolMeta("keios.github.tracked.check_updates", "检查跟踪仓库更新（支持 repoFilter、onlyUpdates）"),
            McpToolMeta("keios.github.tracked.summary", "读取跟踪仓库更新汇总")
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
                    tools = ServerCapabilities.Tools(listChanged = false),
                    resources = ServerCapabilities.Resources(listChanged = false, subscribe = false),
                    prompts = ServerCapabilities.Prompts(listChanged = false)
                )
            ),
            instructions = serverInstructions
        )

        registerCanonicalTools(server)
        registerSkillResources(server)
        registerClawImportResources(server)
        registerSkillPrompt(server)
        return server
    }

    private fun registerCanonicalTools(server: Server) {
        server.addTool(
            name = "keios.health.ping",
            description = "Health check for MCP connection.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText("pong")
        }

        server.addTool(
            name = "keios.app.get_info",
            description = "Get KeiOS app info.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(
                listOf(
                    "label=$appLabel",
                    "package=$appPackageName",
                    "versionName=$appVersionName",
                    "versionCode=$appVersionCode",
                    "shizukuApi=${ShizukuApiUtils.API_VERSION}"
                ).joinToString("\n")
            )
        }

        server.addTool(
            name = "keios.app.get_version",
            description = "Get KeiOS app version name and version code.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText("versionName=$appVersionName\nversionCode=$appVersionCode")
        }

        server.addTool(
            name = "keios.shizuku.get_status",
            description = "Get current Shizuku status from KeiOS app.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(shizukuApiUtils.currentStatus())
        }

        server.addTool(
            name = "keios.system.topinfo.list",
            description = "List TopInfo from cached system sections. Args: query(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("query", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val query = argString(request.arguments?.get("query"))
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TOPINFO_LIMIT)
                .coerceIn(1, MAX_TOPINFO_LIMIT)
            callText(buildTopInfoText(query = query, limit = limit))
        }

        server.addTool(
            name = "keios.system.topinfo.search",
            description = "Search TopInfo rows. Args: query(required), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("query", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val query = argString(request.arguments?.get("query"))
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TOPINFO_LIMIT)
                .coerceIn(1, MAX_TOPINFO_LIMIT)
            callText(buildTopInfoText(query = query, limit = limit))
        }

        server.addTool(
            name = "keios.mcp.get_status",
            description = "Get MCP runtime status.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val state = mcpStateProvider?.invoke()
            val text = if (state == null) {
                "MCP state unavailable"
            } else {
                buildString {
                    appendLine("running=${state.running}")
                    appendLine("serverName=${state.serverName}")
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
            callText(text)
        }

        server.addTool(
            name = "keios.mcp.get_logs",
            description = "Get MCP in-app logs. Arg: limit(optional, default=80).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_LOG_LIMIT)
                .coerceIn(1, MAX_LOG_LIMIT)
            val state = mcpStateProvider?.invoke()
            val text = if (state == null || state.logs.isEmpty()) {
                "No logs."
            } else {
                state.logs.asReversed()
                    .take(limit)
                    .joinToString("\n") { "[${it.time}] [${it.level}] ${it.message}" }
            }
            callText(text)
        }

        server.addTool(
            name = "keios.mcp.get_client_config",
            description = "Build streamable-http client config JSON. Args: endpointMode(local|lan), endpoint(optional), serverName(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("endpointMode", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("endpoint", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("serverName", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val state = mcpStateProvider?.invoke()
            val endpointMode = argString(request.arguments?.get("endpointMode")).lowercase()
            val endpointOverride = argString(request.arguments?.get("endpoint")).trim()
            val serverNameOverride = argString(request.arguments?.get("serverName")).trim()
            callText(
                buildClientConfigJson(
                    state = state,
                    endpointMode = endpointMode,
                    endpointOverride = endpointOverride,
                    serverNameOverride = serverNameOverride
                )
            )
        }

        server.addTool(
            name = "keios.mcp.get_claw_import_package",
            description = "Generate Claw-ready import package JSON. Args: mode(auto|local|lan), serverName(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("mode", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("serverName", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val state = mcpStateProvider?.invoke()
            val mode = normalizeImportMode(argString(request.arguments?.get("mode")))
            val serverNameOverride = argString(request.arguments?.get("serverName")).trim()
            callText(
                buildClawImportPackageJson(
                    state = state,
                    mode = mode,
                    serverNameOverride = serverNameOverride
                )
            )
        }

        server.addTool(
            name = "keios.github.tracked.list",
            description = "List tracked GitHub repos configured in KeiOS.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val items = GitHubTrackStore.load()
            val text = if (items.isEmpty()) {
                "No tracked GitHub apps."
            } else {
                items.joinToString("\n") { item ->
                    "${item.owner}/${item.repo} | label=${item.appLabel} | package=${item.packageName} | repoUrl=${item.repoUrl}"
                }
            }
            callText(text)
        }

        server.addTool(
            name = "keios.github.tracked.check_updates",
            description = "Check tracked app versions. Args: repoFilter(optional), onlyUpdates(optional bool).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("repoFilter", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("onlyUpdates", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                }
            )
        ) { request ->
            val repoFilter = argString(request.arguments?.get("repoFilter")).trim()
            val onlyUpdates = argBoolean(request.arguments?.get("onlyUpdates"), false)
            val checks = checkTrackedGitHub(repoFilter)
                .let { rows -> if (onlyUpdates) rows.filter { it.hasUpdate } else rows }

            val text = if (checks.isEmpty()) {
                if (onlyUpdates) "No tracked items with updates." else "No tracked items matched."
            } else {
                checks.joinToString("\n") { row ->
                    val repo = "${row.item.owner}/${row.item.repo}"
                    val pre = if (row.preReleaseVersion.isNotBlank()) " | pre=${row.preReleaseVersion}" else ""
                    "$repo | local=${row.localVersion} | stable=${row.stableVersion}$pre | status=${row.status} | update=${row.hasUpdate}"
                }
            }
            callText(text)
        }

        server.addTool(
            name = "keios.github.tracked.summary",
            description = "Get summary for tracked GitHub update states.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            val checks = checkTrackedGitHub(repoFilter = "")
            val total = checks.size
            val updates = checks.count { it.hasUpdate }
            val preRelease = checks.count { it.status.contains("预发", ignoreCase = true) || it.status.contains("pre", ignoreCase = true) }
            val body = buildString {
                appendLine("tracked=$total")
                appendLine("hasUpdate=$updates")
                appendLine("preReleaseState=$preRelease")
                checks.sortedByDescending { it.hasUpdate }.take(20).forEach { row ->
                    appendLine("${row.item.owner}/${row.item.repo}=${row.status}")
                }
            }.trim()
            callText(body)
        }
    }

    private fun registerSkillResources(server: Server) {
        server.addResource(
            uri = SKILL_RESOURCE_URI,
            name = "keios-claw-skill",
            description = "KeiOS MCP skill guide for Claw",
            mimeType = MIME_MARKDOWN
        ) { _ ->
            callResource(uri = SKILL_RESOURCE_URI, mimeType = MIME_MARKDOWN, text = loadSkillMarkdown())
        }

        server.addResource(
            uri = SKILL_OVERVIEW_URI,
            name = "keios-skill-overview",
            description = "Quick overview and import hints for KeiOS skill",
            mimeType = MIME_TEXT
        ) { _ ->
            callResource(uri = SKILL_OVERVIEW_URI, mimeType = MIME_TEXT, text = buildSkillOverview())
        }

        server.addResourceTemplate(
            uriTemplate = SKILL_TOOL_TEMPLATE_URI,
            name = "keios-tool-help",
            description = "Tool-level usage help for KeiOS MCP",
            mimeType = MIME_MARKDOWN
        ) { _, params ->
            val tool = params["tool"].orEmpty()
            val text = buildToolHelp(tool)
            callResource(
                uri = SKILL_TOOL_TEMPLATE_URI.replace("{tool}", tool),
                mimeType = MIME_MARKDOWN,
                text = text
            )
        }
    }

    private fun registerClawImportResources(server: Server) {
        server.addResource(
            uri = CLAW_IMPORT_RESOURCE_URI,
            name = "keios-claw-import-auto",
            description = "Claw import package (auto mode) in JSON",
            mimeType = MIME_JSON
        ) { _ ->
            val state = mcpStateProvider?.invoke()
            callResource(
                uri = CLAW_IMPORT_RESOURCE_URI,
                mimeType = MIME_JSON,
                text = buildClawImportPackageJson(state = state, mode = "auto", serverNameOverride = "")
            )
        }

        server.addResourceTemplate(
            uriTemplate = CLAW_IMPORT_TEMPLATE_URI,
            name = "keios-claw-import-template",
            description = "Claw import package by mode (auto/local/lan)",
            mimeType = MIME_JSON
        ) { _, params ->
            val state = mcpStateProvider?.invoke()
            val mode = normalizeImportMode(params["mode"].orEmpty())
            callResource(
                uri = CLAW_IMPORT_TEMPLATE_URI.replace("{mode}", mode),
                mimeType = MIME_JSON,
                text = buildClawImportPackageJson(state = state, mode = mode, serverNameOverride = "")
            )
        }
    }

    private fun registerSkillPrompt(server: Server) {
        server.addPrompt(
            name = CLAW_BOOTSTRAP_PROMPT,
            description = "Bootstrap prompt for Claw to use KeiOS MCP tools effectively.",
            arguments = listOf(
                PromptArgument(
                    name = "task",
                    description = "Current user goal, such as check updates or inspect system params",
                    required = false,
                    title = "Task"
                )
            )
        ) { request ->
            val task = request.arguments?.get("task").orEmpty().trim()
            val promptText = buildString {
                appendLine("你是 Claw，当前连接的是 KeiOS 本地 MCP 服务。")
                appendLine("先执行以下初始化调用：")
                appendLine("1) keios.health.ping")
                appendLine("2) keios.mcp.get_status")
                appendLine("3) keios.mcp.get_claw_import_package (mode=auto)")
                appendLine("4) 如需文档，读取资源 $SKILL_RESOURCE_URI")
                appendLine("5) 如需可导入 JSON，读取资源 $CLAW_IMPORT_RESOURCE_URI 或模板 $CLAW_IMPORT_TEMPLATE_URI")
                appendLine("\n优先根据任务选择工具：")
                appendLine("- 系统参数：keios.system.topinfo.search")
                appendLine("- GitHub 更新：keios.github.tracked.check_updates")
                appendLine("- 排障：keios.mcp.get_logs + keios.shizuku.get_status")
                if (task.isNotBlank()) {
                    appendLine("\n当前任务：$task")
                    appendLine("请先给出 3 步内的工具调用计划，再执行。")
                }
                appendLine("\n可读取技能文档资源：$SKILL_RESOURCE_URI")
            }.trim()

            GetPromptResult(
                description = "KeiOS bootstrap prompt",
                messages = listOf(
                    PromptMessage(
                        role = Role.User,
                        content = TextContent(promptText)
                    )
                )
            )
        }
    }

    private fun buildServerInstructions(): String {
        return buildString {
            appendLine("KeiOS local MCP server")
            appendLine("- Prefer keios.health.ping before task execution.")
            appendLine("- For connection troubleshooting, always read keios.mcp.get_status and keios.mcp.get_logs.")
            appendLine("- For full skill doc, read resource: $SKILL_RESOURCE_URI")
            appendLine("- For Claw import JSON, read resource: $CLAW_IMPORT_RESOURCE_URI")
            appendLine("- For mode-specific import JSON, read template: $CLAW_IMPORT_TEMPLATE_URI")
            appendLine("- For guided startup prompt, call: $CLAW_BOOTSTRAP_PROMPT")
        }.trim()
    }

    private fun loadSkillMarkdown(): String {
        return runCatching {
            appContext.assets.open("mcp/SKILL.md")
                .bufferedReader()
                .use { it.readText() }
        }.map { template ->
            renderSkillTemplate(template)
        }.getOrElse {
            buildFallbackSkillMarkdown()
        }
    }

    private fun renderSkillTemplate(template: String): String {
        val state = mcpStateProvider?.invoke()
        val appVersion = "$appVersionName ($appVersionCode)"
        val serverName = state?.serverName ?: "KeiOS MCP"
        val localEndpoint = state?.localEndpoint ?: DEFAULT_ENDPOINT
        val lanEndpoints = state?.lanEndpoints?.takeIf { it.isNotEmpty() }?.joinToString(" | ") ?: "N/A"
        val toolList = listLocalTools()
            .joinToString("\n") { meta -> "- `${meta.name}`: ${meta.description}" }

        return template
            .replace("{{APP_LABEL}}", appLabel)
            .replace("{{APP_PACKAGE}}", appPackageName)
            .replace("{{APP_VERSION}}", appVersion)
            .replace("{{SERVER_NAME}}", serverName)
            .replace("{{LOCAL_ENDPOINT}}", localEndpoint)
            .replace("{{LAN_ENDPOINTS}}", lanEndpoints)
            .replace("{{RESOURCE_SKILL_URI}}", SKILL_RESOURCE_URI)
            .replace("{{PROMPT_BOOTSTRAP}}", CLAW_BOOTSTRAP_PROMPT)
            .replace("{{RESOURCE_IMPORT_URI}}", CLAW_IMPORT_RESOURCE_URI)
            .replace("{{RESOURCE_IMPORT_TEMPLATE_URI}}", CLAW_IMPORT_TEMPLATE_URI)
            .replace("{{TOOL_LIST}}", toolList)
    }

    private fun buildFallbackSkillMarkdown(): String {
        return buildString {
            appendLine("# KeiOS MCP Skill")
            appendLine()
            appendLine("## First Steps")
            appendLine("1. keios.health.ping")
            appendLine("2. keios.mcp.get_status")
            appendLine("3. keios.mcp.get_claw_import_package")
            appendLine("4. Read resource: $CLAW_IMPORT_RESOURCE_URI")
            appendLine()
            appendLine("## Tool Groups")
            listLocalTools().forEach {
                appendLine("- ${it.name}: ${it.description}")
            }
        }.trim()
    }

    private fun buildSkillOverview(): String {
        val state = mcpStateProvider?.invoke()
        return buildString {
            appendLine("skillResource=$SKILL_RESOURCE_URI")
            appendLine("skillToolTemplate=$SKILL_TOOL_TEMPLATE_URI")
            appendLine("bootstrapPrompt=$CLAW_BOOTSTRAP_PROMPT")
            appendLine("importResource=$CLAW_IMPORT_RESOURCE_URI")
            appendLine("importTemplate=$CLAW_IMPORT_TEMPLATE_URI")
            appendLine("recommendedImportTool=keios.mcp.get_claw_import_package")
            appendLine("localEndpoint=${state?.localEndpoint ?: DEFAULT_ENDPOINT}")
            if (state?.lanEndpoints?.isNotEmpty() == true) {
                appendLine("lanEndpoints=${state.lanEndpoints.joinToString(",")}")
            }
            appendLine("toolCount=${listLocalTools().size}")
            appendLine("tools=${listLocalTools().joinToString(",") { it.name }}")
        }.trim()
    }

    private fun buildToolHelp(tool: String): String {
        val normalized = tool.trim().lowercase()
        val hit = listLocalTools().firstOrNull { it.name.lowercase() == normalized }
        if (hit == null) {
            return buildString {
                appendLine("# Unknown Tool")
                appendLine("tool=$tool")
                appendLine("available=${listLocalTools().joinToString(",") { it.name }}")
            }.trim()
        }

        return buildString {
            appendLine("# ${hit.name}")
            appendLine()
            appendLine(hit.description)
            appendLine()
            appendLine("## Suggested Usage")
            when (hit.name) {
                "keios.mcp.get_client_config" -> {
                    appendLine("- Generate config first, then copy to client")
                    appendLine("- Use endpointMode=local for same-device clients")
                    appendLine("- Use endpointMode=lan for LAN clients")
                }

                "keios.mcp.get_claw_import_package" -> {
                    appendLine("- Use mode=auto to include local endpoint and LAN endpoint (if available)")
                    appendLine("- Use mode=local for same-device client import")
                    appendLine("- Use mode=lan for cross-device import")
                }

                "keios.system.topinfo.search" -> {
                    appendLine("- Pass query with a focused keyword")
                    appendLine("- Use limit to control output size")
                }

                "keios.github.tracked.check_updates" -> {
                    appendLine("- Start with onlyUpdates=true")
                    appendLine("- Use repoFilter when narrowing scope")
                }

                else -> {
                    appendLine("- Call directly and parse key=value output")
                }
            }
        }.trim()
    }

    private fun buildTopInfoText(query: String, limit: Int): String {
        val rows = readSystemTopInfoRows(maxCount = limit, query = query)
        return if (rows.isEmpty()) {
            if (query.isBlank()) {
                "TopInfo cache empty. Open System page once to build cache."
            } else {
                "No matched TopInfo rows."
            }
        } else {
            rows.joinToString("\n") { "${it.key}=${it.value}" }
        }
    }

    private fun callText(text: String): CallToolResult {
        return CallToolResult(content = listOf(TextContent(text)))
    }

    private fun callResource(uri: String, mimeType: String, text: String): ReadResourceResult {
        return ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    uri = uri,
                    mimeType = mimeType,
                    text = text
                )
            )
        )
    }

    private fun normalizeClientMode(raw: String): String {
        return when (raw.trim().lowercase()) {
            "lan" -> "lan"
            else -> "local"
        }
    }

    private fun normalizeImportMode(raw: String): String {
        return when (raw.trim().lowercase()) {
            "local" -> "local"
            "lan" -> "lan"
            else -> "auto"
        }
    }

    private fun resolveEndpoint(
        state: McpServerUiState?,
        mode: String,
        endpointOverride: String
    ): String {
        if (endpointOverride.isNotBlank()) return endpointOverride
        if (state == null) return DEFAULT_ENDPOINT
        return when (mode) {
            "lan" -> state.lanEndpoints.firstOrNull() ?: state.localEndpoint
            else -> state.localEndpoint
        }
    }

    private fun resolveServerName(state: McpServerUiState?, serverNameOverride: String): String {
        return serverNameOverride.ifBlank { state?.serverName ?: "KeiOS MCP" }
    }

    private fun buildClientConfigJson(
        state: McpServerUiState?,
        endpointMode: String,
        endpointOverride: String,
        serverNameOverride: String
    ): String {
        val mode = normalizeClientMode(endpointMode)
        val endpoint = resolveEndpoint(state = state, mode = mode, endpointOverride = endpointOverride)
        val serverName = resolveServerName(state = state, serverNameOverride = serverNameOverride)
        val token = state?.authToken ?: "YOUR_TOKEN"
        return buildMcpConfigJson(
            servers = listOf(serverName to endpoint),
            token = token
        )
    }

    private fun buildClawImportPackageJson(
        state: McpServerUiState?,
        mode: String,
        serverNameOverride: String
    ): String {
        val fixedMode = normalizeImportMode(mode)
        val serverName = resolveServerName(state = state, serverNameOverride = serverNameOverride)
        val token = state?.authToken ?: "YOUR_TOKEN"
        val localEndpoint = state?.localEndpoint ?: DEFAULT_ENDPOINT
        val lanEndpoint = state?.lanEndpoints?.firstOrNull()

        val servers = when (fixedMode) {
            "local" -> listOf(serverName to localEndpoint)
            "lan" -> listOf(serverName to (lanEndpoint ?: localEndpoint))
            else -> {
                val list = mutableListOf<Pair<String, String>>()
                list += "$serverName Local" to localEndpoint
                if (!lanEndpoint.isNullOrBlank() && lanEndpoint != localEndpoint) {
                    list += "$serverName LAN" to lanEndpoint
                }
                list
            }
        }
        return buildMcpConfigJson(servers = servers, token = token)
    }

    private fun buildMcpConfigJson(
        servers: List<Pair<String, String>>,
        token: String
    ): String {
        val fixedServers = if (servers.isEmpty()) {
            listOf("KeiOS MCP" to DEFAULT_ENDPOINT)
        } else {
            servers
        }
        val fixedToken = jsonEscape(token)

        return buildString {
            appendLine("{")
            appendLine("  \"mcpServers\": {")
            fixedServers.forEachIndexed { index, pair ->
                val name = jsonEscape(pair.first)
                val endpoint = jsonEscape(pair.second.ifBlank { DEFAULT_ENDPOINT })
                appendLine("    \"$name\": {")
                appendLine("      \"type\": \"streamablehttp\",")
                appendLine("      \"url\": \"$endpoint\",")
                appendLine("      \"headers\": {")
                appendLine("        \"Authorization\": \"Bearer $fixedToken\"")
                appendLine("      }")
                append("    }")
                if (index != fixedServers.lastIndex) append(",")
                appendLine()
            }
            appendLine("  }")
            append("}")
        }
    }

    private fun jsonEscape(raw: String): String {
        return raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }

    private fun argString(value: Any?): String {
        return (value as? JsonPrimitive)?.contentOrNull.orEmpty()
    }

    private fun argInt(value: Any?, defaultValue: Int): Int {
        return argString(value).trim().toIntOrNull() ?: defaultValue
    }

    private fun argBoolean(value: Any?, defaultValue: Boolean): Boolean {
        val raw = argString(value).trim().lowercase()
        return when (raw) {
            "1", "true", "yes", "y", "on" -> true
            "0", "false", "no", "n", "off" -> false
            else -> defaultValue
        }
    }

    private fun checkTrackedGitHub(repoFilter: String): List<GitHubCheckRow> {
        val items = GitHubTrackStore.load()
        val filtered = if (repoFilter.isBlank()) {
            items
        } else {
            items.filter {
                "${it.owner}/${it.repo}".contains(repoFilter, ignoreCase = true) ||
                    it.packageName.contains(repoFilter, ignoreCase = true) ||
                    it.appLabel.contains(repoFilter, ignoreCase = true)
            }
        }
        return filtered.map { item ->
            runCatching { evaluateTrackedApp(item) }.getOrElse { err ->
                GitHubCheckRow(
                    item = item,
                    localVersion = runCatching { GitHubVersionUtils.localVersionName(appContext, item.packageName) }.getOrDefault("unknown"),
                    stableVersion = "unknown",
                    preReleaseVersion = "",
                    status = "检查失败: ${err.message ?: "unknown"}",
                    hasUpdate = false
                )
            }
        }
    }

    private fun evaluateTrackedApp(item: GitHubTrackedApp): GitHubCheckRow {
        val local = runCatching {
            GitHubVersionUtils.localVersionName(appContext, item.packageName)
        }.getOrDefault("unknown")
        val atomEntries = GitHubVersionUtils.fetchReleaseEntriesFromAtom(item.owner, item.repo, limit = 30)
            .getOrDefault(emptyList())
        val matchedEntry = atomEntries.firstOrNull {
            GitHubVersionUtils.compareVersionToCandidates(local, it.candidates) == 0
        }
        val latestPreEntry = atomEntries.firstOrNull { it.isLikelyPreRelease }
        val stableResult = GitHubVersionUtils.fetchLatestReleaseSignals(
            owner = item.owner,
            repo = item.repo,
            atomEntriesHint = atomEntries
        )

        return stableResult.fold(
            onSuccess = { stable ->
                val cmp = GitHubVersionUtils.compareVersionToCandidates(local, stable.candidates)
                val latestPreLabel = latestPreEntry?.title?.ifBlank { latestPreEntry.tag }.orEmpty()
                val preVsStable = if (latestPreLabel.isNotBlank()) {
                    GitHubVersionUtils.compareVersionToCandidates(latestPreLabel, stable.candidates)
                } else {
                    null
                }
                val preRelevant = latestPreEntry != null && (preVsStable == null || preVsStable > 0)
                val localIsPre = matchedEntry?.isLikelyPreRelease == true
                val preCmp = if (latestPreEntry != null) {
                    GitHubVersionUtils.compareVersionToCandidates(local, latestPreEntry.candidates)
                } else null
                val hasPreUpdate = localIsPre && preRelevant && (preCmp?.let { it < 0 } == true)
                val stableUpdate = cmp?.let { it < 0 } == true
                val hasUpdate = hasPreUpdate || stableUpdate
                val preReleaseDisplay = latestPreEntry?.title?.ifBlank { latestPreEntry.tag }.orEmpty()
                val status = when {
                    hasPreUpdate -> "预发有更新"
                    stableUpdate -> "发现更新"
                    localIsPre && preRelevant -> "预发行"
                    hasUpdate.not() -> "已是最新"
                    else -> "版本格式无法精确比较"
                }
                GitHubCheckRow(
                    item = item,
                    localVersion = local,
                    stableVersion = stable.displayVersion,
                    preReleaseVersion = when {
                        localIsPre && preRelevant -> matchedEntry.title.ifBlank { matchedEntry.tag }
                        preRelevant -> preReleaseDisplay
                        else -> ""
                    },
                    status = status,
                    hasUpdate = hasUpdate
                )
            },
            onFailure = { err ->
                if (matchedEntry != null) {
                    val localIsPre = matchedEntry.isLikelyPreRelease
                    GitHubCheckRow(
                        item = item,
                        localVersion = local,
                        stableVersion = matchedEntry.title.ifBlank { matchedEntry.tag },
                        preReleaseVersion = if (localIsPre) matchedEntry.title.ifBlank { matchedEntry.tag } else "",
                        status = if (localIsPre) "预发行" else "已匹配发行",
                        hasUpdate = false
                    )
                } else {
                    GitHubCheckRow(
                        item = item,
                        localVersion = local,
                        stableVersion = "unknown",
                        preReleaseVersion = "",
                        status = "检查失败: ${err.message ?: "unknown"}",
                        hasUpdate = false
                    )
                }
            }
        )
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
