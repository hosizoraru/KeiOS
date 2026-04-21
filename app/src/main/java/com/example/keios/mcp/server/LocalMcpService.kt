package com.example.keios.mcp.server

import android.content.Intent
import android.content.Context
import android.net.Uri
import com.example.keios.R
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.feature.github.data.local.GitHubReleaseAssetCacheStore
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.feature.github.domain.GitHubReleaseCheckService
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.ba.support.BASettingsStore
import com.example.keios.ui.page.main.ba.support.calculateApFullAtMs
import com.example.keios.ui.page.main.ba.support.calculateApNextPointAtMs
import com.example.keios.ui.page.main.ba.support.calculateInviteTicketAvailableMs
import com.example.keios.ui.page.main.ba.support.calculateNextHeadpatAvailableMs
import com.example.keios.ui.page.main.ba.support.cafeDailyCapacity
import com.example.keios.ui.page.main.ba.support.cafeHourlyGain
import com.example.keios.ui.page.main.ba.support.cafeStorageCap
import com.example.keios.ui.page.main.ba.support.decodeBaCalendarEntries
import com.example.keios.ui.page.main.ba.support.decodeBaPoolEntries
import com.example.keios.ui.page.main.ba.support.displayAp
import com.example.keios.ui.page.main.ba.support.fractionalApPart
import com.example.keios.ui.page.main.ba.support.gameKeeServerId
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogStore
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.student.catalog.clearBaGuideCatalogCache
import com.example.keios.ui.page.main.student.catalog.isBaGuideCatalogBundleComplete
import com.example.keios.ui.page.main.student.catalog.isBaGuideCatalogCacheExpired
import com.example.keios.ui.page.main.student.catalog.loadCachedBaGuideCatalogBundle
import com.example.keios.ui.page.main.os.OsGoogleSystemServiceConfig
import com.example.keios.ui.page.main.os.OsInfoCache
import com.example.keios.ui.page.main.os.OsSectionCard
import com.example.keios.ui.page.main.os.OsUiStateStore
import com.example.keios.ui.page.main.os.shell.OsShellCommandCard
import com.example.keios.ui.page.main.os.shell.OsShellCommandCardStore
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCard
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import com.tencent.mmkv.MMKV
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
import java.util.Locale
import kotlin.math.roundToInt

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
        private const val OS_CACHE_KV_ID = "os_info_cache"
        private const val LEGACY_SYSTEM_CACHE_KV_ID = "system_info_cache"

        private const val KEY_OS_SYSTEM = "section_os_system_table"
        private const val KEY_OS_SECURE = "section_os_secure_table"
        private const val KEY_OS_GLOBAL = "section_os_global_table"
        private const val KEY_OS_ANDROID = "section_os_android_properties"
        private const val KEY_OS_JAVA = "section_os_java_properties"
        private const val KEY_OS_LINUX = "section_os_linux_environment"

        private const val LEGACY_KEY_SYSTEM = "section_system_table"
        private const val LEGACY_KEY_SECURE = "section_secure_table"
        private const val LEGACY_KEY_GLOBAL = "section_global_table"
        private const val LEGACY_KEY_ANDROID = "section_android_properties"
        private const val LEGACY_KEY_JAVA = "section_java_properties"
        private const val LEGACY_KEY_LINUX = "section_linux_environment"

        private const val DEFAULT_TOPINFO_LIMIT = 120
        private const val MAX_TOPINFO_LIMIT = 300
        private const val DEFAULT_LOG_LIMIT = 80
        private const val MAX_LOG_LIMIT = 200
        private const val DEFAULT_TRACK_LIMIT = 80
        private const val MAX_TRACK_LIMIT = 400
        private const val DEFAULT_ENTRY_LIMIT = 12
        private const val MAX_ENTRY_LIMIT = 200

        private const val MIME_MARKDOWN = "text/markdown"
        private const val MIME_TEXT = "text/plain"
        private const val MIME_JSON = "application/json"

        private const val SKILL_RESOURCE_URI = "keios://skill/keios-mcp.md"
        private const val SKILL_OVERVIEW_URI = "keios://skill/overview.txt"
        private const val SKILL_TOOL_TEMPLATE_URI = "keios://skill/tool/{tool}"
        private const val CONFIG_RESOURCE_URI = "keios://mcp/config/default.json"
        private const val CONFIG_TEMPLATE_URI = "keios://mcp/config/{mode}.json"
        private const val BOOTSTRAP_PROMPT = "keios.mcp.bootstrap"
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
            McpToolMeta("keios.health.ping", "健康探针，返回 pong。"),
            McpToolMeta("keios.app.info", "读取应用元信息（label/package/version/shizukuApi）。"),
            McpToolMeta("keios.app.version", "读取版本信息（versionName/versionCode）。"),
            McpToolMeta("keios.shizuku.status", "读取 Shizuku 当前状态字符串。"),
            McpToolMeta("keios.mcp.runtime.status", "读取 MCP 运行态（endpoint/clientCount/error）。"),
            McpToolMeta("keios.mcp.runtime.logs", "读取 MCP 日志（limit=1..200）。"),
            McpToolMeta("keios.mcp.runtime.config", "生成可导入 MCP JSON（mode=auto|local|lan，支持 endpoint/serverName 覆盖）。"),
            McpToolMeta("keios.system.topinfo.query", "查询系统 TopInfo 缓存（query/limit）。"),
            McpToolMeta("keios.os.cards.snapshot", "读取 OS 页面卡片总览（可见性/展开态/缓存体积/统计）。"),
            McpToolMeta("keios.os.activity.cards", "读取活动 card 列表（query/onlyVisible/limit）。"),
            McpToolMeta("keios.os.shell.cards", "读取 shell card 列表（query/onlyVisible/includeOutput/limit）。"),
            McpToolMeta("keios.github.tracked.snapshot", "读取 GitHub 跟踪配置与缓存快照。"),
            McpToolMeta("keios.github.tracked.list", "读取跟踪仓库列表（repoFilter/limit）。"),
            McpToolMeta("keios.github.tracked.check", "在线检查跟踪仓库更新（repoFilter/onlyUpdates/limit）。"),
            McpToolMeta("keios.github.tracked.summary", "读取跟踪汇总（mode=cache|network，支持 repoFilter）。"),
            McpToolMeta("keios.github.tracked.cache.clear", "清空 GitHub 检查缓存与 Release 资源缓存。"),
            McpToolMeta("keios.ba.snapshot", "读取 BA 核心快照（AP/咖啡厅/通知阈值/刷新间隔）。"),
            McpToolMeta("keios.ba.calendar.cache", "读取 BA 活动日历缓存（serverIndex/includeEntries/limit）。"),
            McpToolMeta("keios.ba.pool.cache", "读取 BA 卡池缓存（serverIndex/includeEntries/limit）。"),
            McpToolMeta("keios.ba.guide.catalog.cache", "读取 BA 图鉴总览缓存（tab/includeEntries/limit）。"),
            McpToolMeta("keios.ba.guide.cache.overview", "读取学生图鉴详情缓存总览。"),
            McpToolMeta("keios.ba.guide.cache.inspect", "按 URL 检查学生图鉴详情缓存（url/includeSections/refreshIntervalHours）。"),
            McpToolMeta("keios.ba.cache.clear", "清理 BA/GitHub 缓存（scope=all|ba_calendar_pool|ba_guide_catalog|ba_guide_all|ba_guide_url|github_check）。")
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
        registerConfigResources(server)
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
            name = "keios.app.info",
            description = "Get KeiOS app base info.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(
                buildString {
                    appendLine("label=$appLabel")
                    appendLine("package=$appPackageName")
                    appendLine("versionName=$appVersionName")
                    appendLine("versionCode=$appVersionCode")
                    appendLine("shizukuApi=${ShizukuApiUtils.API_VERSION}")
                }.trim()
            )
        }

        server.addTool(
            name = "keios.app.version",
            description = "Get KeiOS app version info.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText("versionName=$appVersionName\nversionCode=$appVersionCode")
        }

        server.addTool(
            name = "keios.shizuku.status",
            description = "Get current Shizuku status from KeiOS app.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(shizukuApiUtils.currentStatus())
        }

        server.addTool(
            name = "keios.mcp.runtime.status",
            description = "Get MCP runtime status from app state.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildRuntimeStatusText(mcpStateProvider?.invoke()))
        }

        server.addTool(
            name = "keios.mcp.runtime.logs",
            description = "Get MCP runtime logs. Args: limit(optional, default=80).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_LOG_LIMIT).coerceIn(1, MAX_LOG_LIMIT)
            callText(buildRuntimeLogsText(mcpStateProvider?.invoke(), limit))
        }

        server.addTool(
            name = "keios.mcp.runtime.config",
            description = "Build client config JSON. Args: mode(local|lan|auto), endpoint(optional), serverName(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("mode", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("endpoint", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("serverName", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val state = mcpStateProvider?.invoke()
            val mode = argString(request.arguments?.get("mode"))
            val endpoint = argString(request.arguments?.get("endpoint")).trim()
            val serverName = argString(request.arguments?.get("serverName")).trim()
            callText(
                buildRuntimeConfigJson(
                    state = state,
                    mode = mode,
                    endpointOverride = endpoint,
                    serverNameOverride = serverName
                )
            )
        }

        server.addTool(
            name = "keios.system.topinfo.query",
            description = "Query TopInfo rows from cached system data. Args: query(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("query", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val query = argString(request.arguments?.get("query"))
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TOPINFO_LIMIT).coerceIn(1, MAX_TOPINFO_LIMIT)
            callText(buildTopInfoText(query = query, limit = limit))
        }

        server.addTool(
            name = "keios.os.cards.snapshot",
            description = "Get OS page card snapshot (visibility, expanded state, cache sizes).",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildOsCardsSnapshotText())
        }

        server.addTool(
            name = "keios.os.activity.cards",
            description = "List OS activity cards. Args: query(optional), onlyVisible(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("query", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("onlyVisible", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val query = argString(request.arguments?.get("query")).trim()
            val onlyVisible = argBoolean(request.arguments?.get("onlyVisible"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TRACK_LIMIT).coerceIn(1, MAX_TRACK_LIMIT)
            callText(
                buildOsActivityCardsText(
                    query = query,
                    onlyVisible = onlyVisible,
                    limit = limit
                )
            )
        }

        server.addTool(
            name = "keios.os.shell.cards",
            description = "List OS shell cards. Args: query(optional), onlyVisible(optional), includeOutput(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("query", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("onlyVisible", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("includeOutput", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val query = argString(request.arguments?.get("query")).trim()
            val onlyVisible = argBoolean(request.arguments?.get("onlyVisible"), false)
            val includeOutput = argBoolean(request.arguments?.get("includeOutput"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TRACK_LIMIT).coerceIn(1, MAX_TRACK_LIMIT)
            callText(
                buildOsShellCardsText(
                    query = query,
                    onlyVisible = onlyVisible,
                    includeOutput = includeOutput,
                    limit = limit
                )
            )
        }

        server.addTool(
            name = "keios.github.tracked.snapshot",
            description = "Get GitHub tracked settings and cache snapshot.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildGitHubTrackedSnapshotText())
        }

        server.addTool(
            name = "keios.github.tracked.list",
            description = "List tracked repos. Args: repoFilter(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("repoFilter", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val repoFilter = argString(request.arguments?.get("repoFilter")).trim()
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TRACK_LIMIT).coerceIn(1, MAX_TRACK_LIMIT)
            callText(buildGitHubTrackedListText(repoFilter = repoFilter, limit = limit))
        }

        server.addTool(
            name = "keios.github.tracked.check",
            description = "Online check tracked repo updates. Args: repoFilter(optional), onlyUpdates(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("repoFilter", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("onlyUpdates", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val repoFilter = argString(request.arguments?.get("repoFilter")).trim()
            val onlyUpdates = argBoolean(request.arguments?.get("onlyUpdates"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TRACK_LIMIT).coerceIn(1, MAX_TRACK_LIMIT)
            val rows = checkTrackedGitHub(repoFilter)
                .let { data -> if (onlyUpdates) data.filter { it.hasUpdate } else data }
                .take(limit)

            val text = if (rows.isEmpty()) {
                if (onlyUpdates) "No tracked repos with updates." else "No tracked repos matched."
            } else {
                rows.joinToString("\n") { row ->
                    val repo = "${row.item.owner}/${row.item.repo}"
                    val pre = if (row.preReleaseVersion.isNotBlank()) " | pre=${row.preReleaseVersion}" else ""
                    "$repo | local=${row.localVersion} | stable=${row.stableVersion}$pre | status=${row.status} | update=${row.hasUpdate}"
                }
            }
            callText(text)
        }

        server.addTool(
            name = "keios.github.tracked.summary",
            description = "Get tracked summary. Args: mode(cache|network), repoFilter(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("mode", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("repoFilter", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val mode = argString(request.arguments?.get("mode")).trim().lowercase(Locale.ROOT)
            val repoFilter = argString(request.arguments?.get("repoFilter")).trim()
            callText(
                if (mode == "network") {
                    buildGitHubTrackedSummaryFromNetwork(repoFilter)
                } else {
                    buildGitHubTrackedSummaryFromCache(repoFilter)
                }
            )
        }

        server.addTool(
            name = "keios.github.tracked.cache.clear",
            description = "Clear GitHub tracked check cache.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            GitHubTrackStore.clearCheckCache()
            GitHubReleaseAssetCacheStore.clearAll()
            callText("cleared=github_check_cache")
        }

        server.addTool(
            name = "keios.ba.snapshot",
            description = "Get BA page snapshot (AP, cafe, refresh interval).",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildBaSnapshotText())
        }

        server.addTool(
            name = "keios.ba.calendar.cache",
            description = "Inspect BA calendar cache. Args: serverIndex(optional), includeEntries(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("serverIndex", buildJsonObject { put("type", JsonPrimitive("integer")) })
                    put("includeEntries", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val serverIndexArg = argIntOrNull(request.arguments?.get("serverIndex"))
            val includeEntries = argBoolean(request.arguments?.get("includeEntries"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_ENTRY_LIMIT).coerceIn(1, MAX_ENTRY_LIMIT)
            callText(
                buildBaCalendarCacheText(
                    requestedServerIndex = serverIndexArg,
                    includeEntries = includeEntries,
                    limit = limit
                )
            )
        }

        server.addTool(
            name = "keios.ba.pool.cache",
            description = "Inspect BA pool cache. Args: serverIndex(optional), includeEntries(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("serverIndex", buildJsonObject { put("type", JsonPrimitive("integer")) })
                    put("includeEntries", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val serverIndexArg = argIntOrNull(request.arguments?.get("serverIndex"))
            val includeEntries = argBoolean(request.arguments?.get("includeEntries"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_ENTRY_LIMIT).coerceIn(1, MAX_ENTRY_LIMIT)
            callText(
                buildBaPoolCacheText(
                    requestedServerIndex = serverIndexArg,
                    includeEntries = includeEntries,
                    limit = limit
                )
            )
        }

        server.addTool(
            name = "keios.ba.guide.catalog.cache",
            description = "Inspect BA guide catalog cache. Args: tab(all|student|npc), includeEntries(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("tab", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("includeEntries", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val tab = argString(request.arguments?.get("tab")).trim()
            val includeEntries = argBoolean(request.arguments?.get("includeEntries"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_ENTRY_LIMIT).coerceIn(1, MAX_ENTRY_LIMIT)
            callText(buildGuideCatalogCacheText(tab = tab, includeEntries = includeEntries, limit = limit))
        }

        server.addTool(
            name = "keios.ba.guide.cache.overview",
            description = "Get BA guide detail cache overview.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildGuideCacheOverviewText())
        }

        server.addTool(
            name = "keios.ba.guide.cache.inspect",
            description = "Inspect BA guide detail cache by URL. Args: url(optional), includeSections(optional), refreshIntervalHours(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("url", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("includeSections", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("refreshIntervalHours", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val url = argString(request.arguments?.get("url")).trim()
            val includeSections = argBoolean(request.arguments?.get("includeSections"), false)
            val refreshHours = argInt(
                request.arguments?.get("refreshIntervalHours"),
                BASettingsStore.loadCalendarRefreshIntervalHours()
            ).coerceAtLeast(1)
            callText(
                buildGuideCacheInspectText(
                    url = url,
                    includeSections = includeSections,
                    refreshIntervalHours = refreshHours
                )
            )
        }

        server.addTool(
            name = "keios.ba.cache.clear",
            description = "Clear BA/GitHub caches. Args: scope(optional), url(optional for ba_guide_url).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("scope", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("url", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val scope = argString(request.arguments?.get("scope"))
            val url = argString(request.arguments?.get("url"))
            callText(buildCacheClearText(scope = scope, url = url))
        }
    }

    private fun registerSkillResources(server: Server) {
        server.addResource(
            uri = SKILL_RESOURCE_URI,
            name = "keios-mcp-skill",
            description = "KeiOS MCP skill guide",
            mimeType = MIME_MARKDOWN
        ) { _ ->
            callResource(uri = SKILL_RESOURCE_URI, mimeType = MIME_MARKDOWN, text = loadSkillMarkdown())
        }

        server.addResource(
            uri = SKILL_OVERVIEW_URI,
            name = "keios-mcp-skill-overview",
            description = "Quick MCP skill overview",
            mimeType = MIME_TEXT
        ) { _ ->
            callResource(uri = SKILL_OVERVIEW_URI, mimeType = MIME_TEXT, text = buildSkillOverview())
        }

        server.addResourceTemplate(
            uriTemplate = SKILL_TOOL_TEMPLATE_URI,
            name = "keios-mcp-tool-help",
            description = "Tool-level help for KeiOS MCP",
            mimeType = MIME_MARKDOWN
        ) { _, params ->
            val tool = params["tool"].orEmpty()
            callResource(
                uri = SKILL_TOOL_TEMPLATE_URI.replace("{tool}", tool),
                mimeType = MIME_MARKDOWN,
                text = buildToolHelp(tool)
            )
        }
    }

    private fun registerConfigResources(server: Server) {
        server.addResource(
            uri = CONFIG_RESOURCE_URI,
            name = "keios-mcp-config-default",
            description = "Default MCP config package JSON (auto mode)",
            mimeType = MIME_JSON
        ) { _ ->
            val state = mcpStateProvider?.invoke()
            callResource(
                uri = CONFIG_RESOURCE_URI,
                mimeType = MIME_JSON,
                text = buildRuntimeConfigJson(
                    state = state,
                    mode = "auto",
                    endpointOverride = "",
                    serverNameOverride = ""
                )
            )
        }

        server.addResourceTemplate(
            uriTemplate = CONFIG_TEMPLATE_URI,
            name = "keios-mcp-config-template",
            description = "MCP config package JSON by mode (auto/local/lan)",
            mimeType = MIME_JSON
        ) { _, params ->
            val state = mcpStateProvider?.invoke()
            val mode = normalizeConfigMode(params["mode"].orEmpty())
            callResource(
                uri = CONFIG_TEMPLATE_URI.replace("{mode}", mode),
                mimeType = MIME_JSON,
                text = buildRuntimeConfigJson(
                    state = state,
                    mode = mode,
                    endpointOverride = "",
                    serverNameOverride = ""
                )
            )
        }
    }

    private fun registerSkillPrompt(server: Server) {
        server.addPrompt(
            name = BOOTSTRAP_PROMPT,
            description = "Bootstrap prompt for using KeiOS MCP tools.",
            arguments = listOf(
                PromptArgument(
                    name = "task",
                    description = "Current user goal such as check BA cache or inspect GitHub updates",
                    required = false,
                    title = "Task"
                )
            )
        ) { request ->
            val task = request.arguments?.get("task").orEmpty().trim()
            val promptText = buildString {
                appendLine("你当前连接的是 KeiOS 本地 MCP 服务。")
                appendLine("请按以下顺序初始化：")
                appendLine("1) keios.health.ping")
                appendLine("2) keios.mcp.runtime.status")
                appendLine("3) keios.mcp.runtime.config(mode=auto)")
                appendLine("4) 读取资源 $SKILL_OVERVIEW_URI")
                appendLine("5) 按任务读取资源 $SKILL_RESOURCE_URI 或模板 $SKILL_TOOL_TEMPLATE_URI")
                appendLine("6) 需要导入配置时读取 $CONFIG_RESOURCE_URI 或模板 $CONFIG_TEMPLATE_URI")
                appendLine()
                appendLine("常用工具分组：")
                appendLine("- 运行排障：keios.mcp.runtime.status / keios.mcp.runtime.logs / keios.shizuku.status")
                appendLine("- OS 页面：keios.os.cards.snapshot / keios.os.activity.cards / keios.os.shell.cards")
                appendLine("- 系统参数：keios.system.topinfo.query")
                appendLine("- GitHub 跟踪：keios.github.tracked.snapshot / list / check / summary")
                appendLine("- BA 缓存：keios.ba.snapshot / keios.ba.calendar.cache / keios.ba.pool.cache / keios.ba.guide.cache.inspect")
                appendLine("- 缓存清理：keios.github.tracked.cache.clear / keios.ba.cache.clear")
                if (task.isNotBlank()) {
                    appendLine()
                    appendLine("当前任务：$task")
                    appendLine("先给出不超过 4 步的工具调用计划，再执行。")
                }
            }.trim()

            GetPromptResult(
                description = "KeiOS MCP bootstrap prompt",
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
            appendLine("- Start with keios.health.ping, then keios.mcp.runtime.status.")
            appendLine("- Read quick overview from $SKILL_OVERVIEW_URI before task execution.")
            appendLine("- Use $BOOTSTRAP_PROMPT when task context is missing.")
            appendLine("- Use keios.mcp.runtime.config or resources $CONFIG_RESOURCE_URI / $CONFIG_TEMPLATE_URI for import JSON.")
            appendLine("- OS diagnostics can use keios.os.cards.snapshot / keios.os.activity.cards / keios.os.shell.cards.")
            appendLine("- Full skill doc resource: $SKILL_RESOURCE_URI")
            appendLine("- Tool help template resource: $SKILL_TOOL_TEMPLATE_URI")
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
        val toolList = listLocalTools().joinToString("\n") { meta ->
            "- `${meta.name}`: ${meta.description}"
        }

        return template
            .replace("{{APP_LABEL}}", appLabel)
            .replace("{{APP_PACKAGE}}", appPackageName)
            .replace("{{APP_VERSION}}", appVersion)
            .replace("{{SERVER_NAME}}", serverName)
            .replace("{{LOCAL_ENDPOINT}}", localEndpoint)
            .replace("{{LAN_ENDPOINTS}}", lanEndpoints)
            .replace("{{RESOURCE_SKILL_URI}}", SKILL_RESOURCE_URI)
            .replace("{{RESOURCE_OVERVIEW_URI}}", SKILL_OVERVIEW_URI)
            .replace("{{PROMPT_BOOTSTRAP}}", BOOTSTRAP_PROMPT)
            .replace("{{RESOURCE_CONFIG_URI}}", CONFIG_RESOURCE_URI)
            .replace("{{RESOURCE_CONFIG_TEMPLATE_URI}}", CONFIG_TEMPLATE_URI)
            .replace("{{TOOL_LIST}}", toolList)
    }

    private fun buildFallbackSkillMarkdown(): String {
        return buildString {
            appendLine("# KeiOS MCP Skill")
            appendLine()
            appendLine("## First Steps")
            appendLine("1. keios.health.ping")
            appendLine("2. keios.mcp.runtime.status")
            appendLine("3. keios.mcp.runtime.config")
            appendLine("4. Read resource: $CONFIG_RESOURCE_URI")
            appendLine()
            appendLine("## Tool Groups")
            listLocalTools().forEach { meta ->
                appendLine("- ${meta.name}: ${meta.description}")
            }
        }.trim()
    }

    private fun buildSkillOverview(): String {
        val state = mcpStateProvider?.invoke()
        val runtimeTools = listOf(
            "keios.health.ping",
            "keios.mcp.runtime.status",
            "keios.mcp.runtime.logs",
            "keios.mcp.runtime.config"
        )
        val osTools = listOf(
            "keios.os.cards.snapshot",
            "keios.os.activity.cards",
            "keios.os.shell.cards"
        )
        val systemTools = listOf(
            "keios.system.topinfo.query"
        )
        val githubTools = listOf(
            "keios.github.tracked.snapshot",
            "keios.github.tracked.list",
            "keios.github.tracked.check",
            "keios.github.tracked.summary",
            "keios.github.tracked.cache.clear"
        )
        val baTools = listOf(
            "keios.ba.snapshot",
            "keios.ba.calendar.cache",
            "keios.ba.pool.cache",
            "keios.ba.guide.catalog.cache",
            "keios.ba.guide.cache.overview",
            "keios.ba.guide.cache.inspect",
            "keios.ba.cache.clear"
        )
        return buildString {
            appendLine("skillResource=$SKILL_RESOURCE_URI")
            appendLine("skillOverviewResource=$SKILL_OVERVIEW_URI")
            appendLine("skillToolTemplate=$SKILL_TOOL_TEMPLATE_URI")
            appendLine("bootstrapPrompt=$BOOTSTRAP_PROMPT")
            appendLine("configResource=$CONFIG_RESOURCE_URI")
            appendLine("configTemplate=$CONFIG_TEMPLATE_URI")
            appendLine("recommendedConfigTool=keios.mcp.runtime.config")
            appendLine("localEndpoint=${state?.localEndpoint ?: DEFAULT_ENDPOINT}")
            if (state?.lanEndpoints?.isNotEmpty() == true) {
                appendLine("lanEndpoints=${state.lanEndpoints.joinToString(",")}")
            }
            appendLine("runtimeTools=${runtimeTools.joinToString(",")}")
            appendLine("osTools=${osTools.joinToString(",")}")
            appendLine("systemTools=${systemTools.joinToString(",")}")
            appendLine("githubTools=${githubTools.joinToString(",")}")
            appendLine("baTools=${baTools.joinToString(",")}")
            appendLine("defaultFlow=keios.health.ping->keios.mcp.runtime.status->keios.mcp.runtime.config")
            appendLine("toolCount=${listLocalTools().size}")
            appendLine("tools=${listLocalTools().joinToString(",") { it.name }}")
        }.trim()
    }

    private fun buildToolHelp(tool: String): String {
        val normalized = tool.trim().lowercase(Locale.ROOT)
        val hit = listLocalTools().firstOrNull { it.name.lowercase(Locale.ROOT) == normalized }
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
                "keios.health.ping" -> {
                    appendLine("- 作为第一条调用验证链路连通性。")
                    appendLine("- 返回值固定为 pong。")
                }

                "keios.app.info", "keios.app.version", "keios.shizuku.status" -> {
                    appendLine("- 作为环境确认工具使用。")
                    appendLine("- 输出为 key=value 文本，适合直接记录到报告。")
                }

                "keios.mcp.runtime.status" -> {
                    appendLine("- 先看 running/connectedClients/localEndpoint。")
                    appendLine("- 有异常时继续调用 keios.mcp.runtime.logs。")
                }

                "keios.mcp.runtime.logs" -> {
                    appendLine("- 建议先用 limit=30，排障时再扩大。")
                    appendLine("- 日志按时间倒序输出。")
                }

                "keios.mcp.runtime.config" -> {
                    appendLine("- 默认使用 mode=auto。")
                    appendLine("- 同机客户端优先 mode=local。")
                    appendLine("- 跨设备调试使用 mode=lan。")
                    appendLine("- 需要临时目标时可传 endpoint 覆盖。")
                }

                "keios.system.topinfo.query" -> {
                    appendLine("- query 为空时返回热点参数。")
                    appendLine("- 使用 limit 控制输出规模。")
                }

                "keios.os.cards.snapshot" -> {
                    appendLine("- 读取 OS 页面整体状态，适合先做一次快照。")
                    appendLine("- 可见卡片、展开态、缓存体积和估算值会一次返回。")
                }

                "keios.os.activity.cards" -> {
                    appendLine("- 用 query 按标题/包名/类名筛选活动 card。")
                    appendLine("- onlyVisible=true 只看当前显示中的活动 card。")
                    appendLine("- limit 控制返回条目数量。")
                }

                "keios.os.shell.cards" -> {
                    appendLine("- 默认返回 shell card 元信息和命令。")
                    appendLine("- includeOutput=true 会追加运行输出摘要。")
                    appendLine("- onlyVisible=true 可与 query 组合做精确筛选。")
                }

                "keios.github.tracked.snapshot", "keios.github.tracked.list", "keios.github.tracked.summary" -> {
                    appendLine("- 先用 snapshot 获取总览，再按 list/summary 下钻。")
                    appendLine("- repoFilter 支持 owner/repo、包名、应用名。")
                }

                "keios.github.tracked.check" -> {
                    appendLine("- 先用 onlyUpdates=true 快速筛选。")
                    appendLine("- repoFilter 可按 owner/repo、包名或应用名过滤。")
                }

                "keios.github.tracked.cache.clear" -> {
                    appendLine("- 清理后建议再执行 tracked.check 获取新状态。")
                    appendLine("- 会同时清理 release asset 缓存。")
                }

                "keios.ba.snapshot", "keios.ba.calendar.cache", "keios.ba.pool.cache", "keios.ba.guide.catalog.cache", "keios.ba.guide.cache.overview" -> {
                    appendLine("- 先用 ba.snapshot 获取全局状态。")
                    appendLine("- 再按日历/卡池/图鉴缓存工具下钻。")
                }

                "keios.ba.guide.cache.inspect" -> {
                    appendLine("- url 为空时读取当前图鉴 URL。")
                    appendLine("- includeSections=true 输出分区统计。")
                    appendLine("- refreshIntervalHours 可覆盖当前判定窗口。")
                }

                "keios.ba.cache.clear" -> {
                    appendLine("- scope=all 可一次性清理 BA/GitHub 缓存。")
                    appendLine("- scope=ba_guide_url 需要同时传入 url。")
                }

                else -> {
                    appendLine("- 直接调用并解析 key=value 输出。")
                }
            }
        }.trim()
    }

    private fun buildRuntimeStatusText(state: McpServerUiState?): String {
        if (state == null) return "runtimeState=unavailable"
        return buildString {
            appendLine("running=${state.running}")
            appendLine("serverName=${state.serverName}")
            appendLine("host=${state.host}")
            appendLine("port=${state.port}")
            appendLine("path=${state.endpointPath}")
            appendLine("allowExternal=${state.allowExternal}")
            appendLine("connectedClients=${state.connectedClients}")
            appendLine("localEndpoint=${state.localEndpoint}")
            appendLine("authTokenPresent=${state.authToken.isNotBlank()}")
            if (state.lanEndpoints.isNotEmpty()) {
                appendLine("lanEndpoints=${state.lanEndpoints.joinToString(",")}")
            }
            if (!state.lastError.isNullOrBlank()) {
                appendLine("lastError=${state.lastError}")
            }
        }.trim()
    }

    private fun buildRuntimeLogsText(state: McpServerUiState?, limit: Int): String {
        if (state == null) return "runtimeState=unavailable"
        if (state.logs.isEmpty()) return "No logs."
        return state.logs.asReversed()
            .take(limit)
            .joinToString("\n") { row -> "[${row.time}] [${row.level}] ${row.message}" }
    }

    private fun normalizeConfigMode(raw: String): String {
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "local" -> "local"
            "lan" -> "lan"
            else -> "auto"
        }
    }

    private fun resolveServerName(state: McpServerUiState?, serverNameOverride: String): String {
        return serverNameOverride.ifBlank { state?.serverName ?: "KeiOS MCP" }
    }

    private fun resolveEndpoint(state: McpServerUiState?, mode: String): String {
        if (state == null) return DEFAULT_ENDPOINT
        return when (mode) {
            "lan" -> state.lanEndpoints.firstOrNull() ?: state.localEndpoint
            else -> state.localEndpoint
        }
    }

    private fun buildRuntimeConfigJson(
        state: McpServerUiState?,
        mode: String,
        endpointOverride: String,
        serverNameOverride: String
    ): String {
        val fixedMode = normalizeConfigMode(mode)
        val fixedServerName = resolveServerName(state, serverNameOverride)
        val overrideEndpoint = endpointOverride.trim()
        if (overrideEndpoint.isNotBlank()) {
            return buildMcpConfigJson(
                servers = listOf(
                    McpConfigServerEntry(
                        name = fixedServerName,
                        endpoint = overrideEndpoint,
                        includeJsonContentTypeHeader = shouldIncludeJsonContentTypeHeader(overrideEndpoint, fixedMode)
                    )
                ),
                token = state?.authToken ?: "YOUR_TOKEN"
            )
        }

        val localEndpoint = resolveEndpoint(state = state, mode = "local")
        val lanEndpoint = resolveEndpoint(state = state, mode = "lan")
        val servers = when (fixedMode) {
            "local" -> listOf(
                McpConfigServerEntry(
                    name = fixedServerName,
                    endpoint = localEndpoint,
                    includeJsonContentTypeHeader = false
                )
            )
            "lan" -> listOf(
                McpConfigServerEntry(
                    name = fixedServerName,
                    endpoint = lanEndpoint,
                    includeJsonContentTypeHeader = true
                )
            )
            else -> {
                val list = mutableListOf<McpConfigServerEntry>()
                list += McpConfigServerEntry(
                    name = "$fixedServerName Local",
                    endpoint = localEndpoint,
                    includeJsonContentTypeHeader = false
                )
                if (lanEndpoint != localEndpoint) {
                    list += McpConfigServerEntry(
                        name = "$fixedServerName LAN",
                        endpoint = lanEndpoint,
                        includeJsonContentTypeHeader = true
                    )
                }
                list
            }
        }
        return buildMcpConfigJson(
            servers = servers,
            token = state?.authToken ?: "YOUR_TOKEN"
        )
    }

    private fun buildMcpConfigJson(
        servers: List<McpConfigServerEntry>,
        token: String
    ): String {
        val fixedServers = if (servers.isEmpty()) {
            listOf(
                McpConfigServerEntry(
                    name = "KeiOS MCP",
                    endpoint = DEFAULT_ENDPOINT,
                    includeJsonContentTypeHeader = false
                )
            )
        } else {
            servers
        }
        val fixedToken = jsonEscape(token)

        return buildString {
            appendLine("{")
            appendLine("  \"mcpServers\": {")
            fixedServers.forEachIndexed { index, server ->
                val name = jsonEscape(server.name)
                val endpoint = jsonEscape(server.endpoint.ifBlank { DEFAULT_ENDPOINT })
                appendLine("    \"$name\": {")
                appendLine("      \"type\": \"streamablehttp\",")
                appendLine("      \"url\": \"$endpoint\",")
                appendLine("      \"headers\": {")
                if (server.includeJsonContentTypeHeader) {
                    appendLine("        \"Authorization\": \"Bearer $fixedToken\",")
                    appendLine("        \"Content-Type\": \"application/json\"")
                } else {
                    appendLine("        \"Authorization\": \"Bearer $fixedToken\"")
                }
                appendLine("      }")
                append("    }")
                if (index != fixedServers.lastIndex) append(",")
                appendLine()
            }
            appendLine("  }")
            append("}")
        }
    }

    private data class McpConfigServerEntry(
        val name: String,
        val endpoint: String,
        val includeJsonContentTypeHeader: Boolean
    )

    private fun shouldIncludeJsonContentTypeHeader(endpoint: String, mode: String): Boolean {
        if (mode == "lan") return true
        val host = runCatching { java.net.URI(endpoint).host.orEmpty() }.getOrDefault("")
        if (host.isBlank()) return false
        val lowerHost = host.lowercase()
        return lowerHost != "127.0.0.1" && lowerHost != "localhost"
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

    private fun buildOsGoogleSystemDefaults(): OsGoogleSystemServiceConfig {
        val defaults = OsGoogleSystemServiceConfig(
            title = appContext.getString(R.string.os_section_google_system_service_title),
            subtitle = appContext.getString(R.string.os_google_system_service_default_subtitle),
            appName = appContext.getString(R.string.os_google_system_service_default_app_name),
            intentFlags = appContext.getString(R.string.os_google_system_service_default_intent_flags)
        )
        return defaults.normalized()
    }

    private fun buildOsGoogleSettingsSampleDefaults(
        defaults: OsGoogleSystemServiceConfig
    ): OsGoogleSystemServiceConfig {
        val sample = OsGoogleSystemServiceConfig(
            title = appContext.getString(R.string.os_activity_builtin_google_settings_title),
            subtitle = appContext.getString(R.string.os_activity_builtin_google_settings_subtitle),
            appName = appContext.getString(R.string.os_activity_builtin_google_settings_app_name),
            packageName = appContext.getString(R.string.os_activity_builtin_google_settings_package),
            className = appContext.getString(R.string.os_activity_builtin_google_settings_class),
            intentAction = Intent.ACTION_VIEW,
            intentFlags = appContext.getString(R.string.os_google_system_service_default_intent_flags)
        )
        return sample.normalized(defaults)
    }

    private fun loadOsActivityCards(): List<OsActivityShortcutCard> {
        val defaults = buildOsGoogleSystemDefaults()
        val sampleDefaults = buildOsGoogleSettingsSampleDefaults(defaults)
        return OsActivityShortcutCardStore.loadCards(
            defaults = defaults,
            builtInSampleDefaults = sampleDefaults
        )
    }

    private fun buildOsCardsSnapshotText(): String {
        val uiSnapshot = OsUiStateStore.loadSnapshot()
        val visibleCards = uiSnapshot.visibleCards
        val activityCards = loadOsActivityCards()
        val shellCards = OsShellCommandCardStore.loadCards()
        val shellRunnerVisible = visibleCards.contains(OsSectionCard.SHELL_RUNNER)
        val parameterTotalCount = OsSectionCard.entries.count {
            it != OsSectionCard.GOOGLE_SYSTEM_SERVICE && it != OsSectionCard.SHELL_RUNNER
        }
        val parameterVisibleCount = visibleCards.count {
            it != OsSectionCard.GOOGLE_SYSTEM_SERVICE && it != OsSectionCard.SHELL_RUNNER
        }
        val activityVisibleCount = activityCards.count { it.visible }
        val shellVisibleCount = shellCards.count { it.visible } + if (shellRunnerVisible) 1 else 0
        val shellTotalCount = shellCards.size + 1
        val cachePersisted = OsInfoCache.hasPersistedCache()
        val cachedSectionCount = OsInfoCache.cachedSectionCount(visibleCards)

        return buildString {
            appendLine("visibleCards=${visibleCards.map { it.name }.sorted().joinToString(",")}")
            appendLine("parameterCards=$parameterVisibleCount/$parameterTotalCount")
            appendLine("activityCards=$activityVisibleCount/${activityCards.size}")
            appendLine("shellCards=$shellVisibleCount/$shellTotalCount")
            appendLine("shellRunnerVisible=$shellRunnerVisible")
            appendLine("topInfoExpanded=${uiSnapshot.topInfoExpanded}")
            appendLine("overviewExpanded=${OsUiStateStore.overviewExpanded(true)}")
            appendLine("systemExpanded=${uiSnapshot.systemTableExpanded}")
            appendLine("secureExpanded=${uiSnapshot.secureTableExpanded}")
            appendLine("globalExpanded=${uiSnapshot.globalTableExpanded}")
            appendLine("androidExpanded=${uiSnapshot.androidPropsExpanded}")
            appendLine("javaExpanded=${uiSnapshot.javaPropsExpanded}")
            appendLine("linuxExpanded=${uiSnapshot.linuxEnvExpanded}")
            appendLine("cachePersisted=$cachePersisted")
            appendLine("cachedSectionCount=$cachedSectionCount")
            appendLine("cacheFootprintBytes=${OsInfoCache.storageFootprintBytes()}")
            appendLine("cacheActualDataBytes=${OsInfoCache.actualDataBytes()}")
            appendLine("cacheEstimatedBytes=${OsInfoCache.cacheBytesEstimated()}")
            appendLine("uiStateFootprintBytes=${OsUiStateStore.storageFootprintBytes()}")
            appendLine("uiStateActualDataBytes=${OsUiStateStore.actualDataBytes()}")
            appendLine("uiStateEstimatedBytes=${OsUiStateStore.configBytesEstimated()}")
        }.trim()
    }

    private fun matchesActivityCard(card: OsActivityShortcutCard, query: String): Boolean {
        if (query.isBlank()) return true
        val key = query.lowercase(Locale.ROOT)
        return card.id.lowercase(Locale.ROOT).contains(key) ||
            card.config.title.lowercase(Locale.ROOT).contains(key) ||
            card.config.subtitle.lowercase(Locale.ROOT).contains(key) ||
            card.config.appName.lowercase(Locale.ROOT).contains(key) ||
            card.config.packageName.lowercase(Locale.ROOT).contains(key) ||
            card.config.className.lowercase(Locale.ROOT).contains(key) ||
            card.config.intentAction.lowercase(Locale.ROOT).contains(key) ||
            card.config.intentCategory.lowercase(Locale.ROOT).contains(key)
    }

    private fun buildOsActivityCardsText(
        query: String,
        onlyVisible: Boolean,
        limit: Int
    ): String {
        val cards = loadOsActivityCards()
        val filtered = cards.filter { card ->
            (!onlyVisible || card.visible) && matchesActivityCard(card, query)
        }
        val rows = filtered.take(limit)
        return buildString {
            appendLine("total=${cards.size}")
            appendLine("visibleTotal=${cards.count { it.visible }}")
            appendLine("matched=${filtered.size}")
            appendLine("returned=${rows.size}")
            appendLine("query=${query.ifBlank { "(none)" }}")
            appendLine("onlyVisible=$onlyVisible")
            rows.forEachIndexed { index, card ->
                appendLine(
                    "activity[$index]=id:${card.id} | visible:${card.visible} | builtIn:${card.isBuiltInSample} | title:${card.config.title} | package:${card.config.packageName} | class:${card.config.className} | action:${card.config.intentAction} | extras:${card.config.intentExtras.size}"
                )
            }
        }.trim()
    }

    private fun matchesShellCard(card: OsShellCommandCard, query: String): Boolean {
        if (query.isBlank()) return true
        val key = query.lowercase(Locale.ROOT)
        return card.id.lowercase(Locale.ROOT).contains(key) ||
            card.title.lowercase(Locale.ROOT).contains(key) ||
            card.subtitle.lowercase(Locale.ROOT).contains(key) ||
            card.command.lowercase(Locale.ROOT).contains(key) ||
            card.runOutput.lowercase(Locale.ROOT).contains(key)
    }

    private fun normalizeOutputSnippet(raw: String, maxLen: Int = 180): String {
        val normalized = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (normalized.length <= maxLen) normalized else "${normalized.take(maxLen - 1)}…"
    }

    private fun buildOsShellCardsText(
        query: String,
        onlyVisible: Boolean,
        includeOutput: Boolean,
        limit: Int
    ): String {
        val uiSnapshot = OsUiStateStore.loadSnapshot()
        val shellRunnerTitle = appContext.getString(R.string.os_shell_card_title)
        val shellRunnerSubtitle = appContext.getString(R.string.os_shell_card_subtitle)
        val shellRunnerVisible = uiSnapshot.visibleCards.contains(OsSectionCard.SHELL_RUNNER)

        val matchesShellRunner = query.isBlank() ||
            shellRunnerTitle.contains(query, ignoreCase = true) ||
            shellRunnerSubtitle.contains(query, ignoreCase = true) ||
            "shell-runner".contains(query, ignoreCase = true)
        val includeShellRunner = (!onlyVisible || shellRunnerVisible) && matchesShellRunner

        val cards = OsShellCommandCardStore.loadCards()
        val filteredCards = cards.filter { card ->
            (!onlyVisible || card.visible) && matchesShellCard(card, query)
        }
        val rows = filteredCards.take(limit)
        val totalWithRunner = cards.size + 1
        val visibleWithRunner = cards.count { it.visible } + if (shellRunnerVisible) 1 else 0
        val matchedCount = filteredCards.size + if (includeShellRunner) 1 else 0
        val returnedCount = rows.size + if (includeShellRunner) 1 else 0

        return buildString {
            appendLine("total=$totalWithRunner")
            appendLine("visibleTotal=$visibleWithRunner")
            appendLine("matched=$matchedCount")
            appendLine("returned=$returnedCount")
            appendLine("query=${query.ifBlank { "(none)" }}")
            appendLine("onlyVisible=$onlyVisible")
            appendLine("includeOutput=$includeOutput")
            if (includeShellRunner) {
                appendLine(
                    "shell[runner]=id:shell-runner | visible:$shellRunnerVisible | builtIn:true | title:$shellRunnerTitle | subtitle:$shellRunnerSubtitle"
                )
            }
            rows.forEachIndexed { index, card ->
                val baseLine = buildString {
                    append("shell[$index]=id:${card.id} | visible:${card.visible} | builtIn:false")
                    append(" | title:${card.title} | subtitle:${card.subtitle}")
                    append(" | command:${card.command}")
                    append(" | lastRunAtMs:${card.lastRunAtMillis}")
                    append(" | updatedAtMs:${card.updatedAtMillis}")
                }
                appendLine(baseLine)
                if (includeOutput && card.runOutput.isNotBlank()) {
                    appendLine("shell[$index].output=${normalizeOutputSnippet(card.runOutput)}")
                }
            }
        }.trim()
    }

    private fun buildGitHubTrackedSnapshotText(): String {
        val snapshot = GitHubTrackStore.loadSnapshot()
        val cachedUpdateCount = snapshot.checkCache.values.count { it.hasUpdate == true }
        val cachedFailedCount = snapshot.checkCache.values.count {
            it.message.contains("失败", ignoreCase = true) || it.message.contains("failed", ignoreCase = true)
        }
        return buildString {
            appendLine("trackedCount=${snapshot.items.size}")
            appendLine("cachedCheckCount=${snapshot.checkCache.size}")
            appendLine("cachedHasUpdateCount=$cachedUpdateCount")
            appendLine("cachedFailedCount=$cachedFailedCount")
            appendLine("lastRefreshMs=${snapshot.lastRefreshMs}")
            appendLine("refreshIntervalHours=${snapshot.refreshIntervalHours}")
            appendLine("lookupStrategy=${snapshot.lookupConfig.selectedStrategy.storageId}")
            appendLine("apiTokenConfigured=${snapshot.lookupConfig.apiToken.isNotBlank()}")
            appendLine("checkAllTrackedPreReleases=${snapshot.lookupConfig.checkAllTrackedPreReleases}")
            appendLine("aggressiveApkFiltering=${snapshot.lookupConfig.aggressiveApkFiltering}")
        }.trim()
    }

    private fun filterTrackedItems(items: List<GitHubTrackedApp>, repoFilter: String): List<GitHubTrackedApp> {
        if (repoFilter.isBlank()) return items
        return items.filter {
            "${it.owner}/${it.repo}".contains(repoFilter, ignoreCase = true) ||
                it.packageName.contains(repoFilter, ignoreCase = true) ||
                it.appLabel.contains(repoFilter, ignoreCase = true)
        }
    }

    private fun buildGitHubTrackedListText(repoFilter: String, limit: Int): String {
        val items = filterTrackedItems(GitHubTrackStore.load(), repoFilter).take(limit)
        if (items.isEmpty()) return "No tracked GitHub apps."
        return items.joinToString("\n") { item ->
            "${item.owner}/${item.repo} | label=${item.appLabel} | package=${item.packageName} | preferPreRelease=${item.preferPreRelease}"
        }
    }

    private fun buildGitHubTrackedSummaryFromCache(repoFilter: String): String {
        val snapshot = GitHubTrackStore.loadSnapshot()
        val tracked = filterTrackedItems(snapshot.items, repoFilter)
        if (tracked.isEmpty()) {
            return "mode=cache\ntracked=0\nmatched=0"
        }

        val ids = tracked.map { it.id }.toSet()
        val cacheHit = snapshot.checkCache.filterKeys { key -> key in ids }
        val hasUpdate = cacheHit.count { it.value.hasUpdate == true }
        val unknown = cacheHit.count { it.value.hasUpdate == null }
        val preRelease = cacheHit.count { it.value.showPreReleaseInfo || it.value.hasPreReleaseUpdate || it.value.recommendsPreRelease }

        return buildString {
            appendLine("mode=cache")
            appendLine("tracked=${snapshot.items.size}")
            appendLine("matched=${tracked.size}")
            appendLine("cacheHit=${cacheHit.size}")
            appendLine("hasUpdate=$hasUpdate")
            appendLine("unknown=$unknown")
            appendLine("preReleaseState=$preRelease")
            appendLine("lastRefreshMs=${snapshot.lastRefreshMs}")
            cacheHit.entries
                .sortedByDescending { it.value.hasUpdate == true }
                .take(20)
                .forEach { (id, entry) ->
                    appendLine("$id=${entry.message}")
                }
        }.trim()
    }

    private fun buildGitHubTrackedSummaryFromNetwork(repoFilter: String): String {
        val rows = checkTrackedGitHub(repoFilter)
        val hasUpdate = rows.count { it.hasUpdate }
        val preRelease = rows.count { it.preReleaseVersion.isNotBlank() || it.status.contains("预", ignoreCase = true) }
        return buildString {
            appendLine("mode=network")
            appendLine("matched=${rows.size}")
            appendLine("hasUpdate=$hasUpdate")
            appendLine("preReleaseState=$preRelease")
            rows.sortedByDescending { it.hasUpdate }
                .take(20)
                .forEach { row ->
                    appendLine("${row.item.owner}/${row.item.repo}=${row.status}")
                }
        }.trim()
    }

    private fun resolveServerIndex(requestedServerIndex: Int?): Int {
        return requestedServerIndex?.coerceIn(0, 2) ?: BASettingsStore.loadSnapshot().serverIndex
    }

    private fun buildBaSnapshotText(nowMs: Long = System.currentTimeMillis()): String {
        val snapshot = BASettingsStore.loadSnapshot()
        val serverIndex = snapshot.serverIndex.coerceIn(0, 2)
        val displayedAp = displayAp(snapshot.apCurrent)
        val apFraction = fractionalApPart(snapshot.apCurrent)
        val apPercent = if (snapshot.apLimit <= 0) {
            0.0
        } else {
            (snapshot.apCurrent / snapshot.apLimit.toDouble() * 100.0).coerceIn(0.0, 999.0)
        }
        val apFullAtMs = calculateApFullAtMs(
            apLimit = snapshot.apLimit,
            apCurrent = snapshot.apCurrent,
            apRegenBaseMs = snapshot.apRegenBaseMs,
            nowMs = nowMs
        )
        val apNextPointAtMs = calculateApNextPointAtMs(
            apLimit = snapshot.apLimit,
            apCurrent = snapshot.apCurrent,
            apRegenBaseMs = snapshot.apRegenBaseMs,
            nowMs = nowMs
        )
        val headpatReadyAtMs = calculateNextHeadpatAvailableMs(
            lastHeadpatMs = snapshot.coffeeHeadpatMs,
            serverIndex = serverIndex
        )
        val invite1ReadyAtMs = calculateInviteTicketAvailableMs(snapshot.coffeeInvite1UsedMs)
        val invite2ReadyAtMs = calculateInviteTicketAvailableMs(snapshot.coffeeInvite2UsedMs)
        val cafeLevel = snapshot.cafeLevel.coerceIn(1, 10)
        val cafeCap = cafeStorageCap(cafeLevel)
        val cafeStored = snapshot.cafeStoredAp.coerceIn(0.0, cafeCap)
        val cafePercent = if (cafeCap <= 0.0) 0.0 else (cafeStored / cafeCap * 100.0).coerceIn(0.0, 100.0)

        return buildString {
            appendLine("serverIndex=$serverIndex")
            appendLine("gameKeeServerId=${gameKeeServerId(serverIndex)}")
            appendLine("calendarRefreshIntervalHours=${snapshot.calendarRefreshIntervalHours}")
            appendLine("apCurrent=$displayedAp")
            appendLine("apCurrentExact=${snapshot.apCurrent}")
            appendLine("apFraction=${(apFraction * 1000.0).roundToInt() / 1000.0}")
            appendLine("apLimit=${snapshot.apLimit}")
            appendLine("apPercent=${(apPercent * 10.0).roundToInt() / 10.0}")
            appendLine("apRegenBaseMs=${snapshot.apRegenBaseMs}")
            appendLine("apSyncMs=${snapshot.apSyncMs}")
            appendLine("apNextPointAtMs=$apNextPointAtMs")
            appendLine("apFullAtMs=$apFullAtMs")
            appendLine("apNotifyEnabled=${snapshot.apNotifyEnabled}")
            appendLine("apNotifyThreshold=${snapshot.apNotifyThreshold}")
            appendLine("apLastNotifiedLevel=${snapshot.apLastNotifiedLevel}")
            appendLine("cafeLevel=$cafeLevel")
            appendLine("cafeHourlyGain=${(cafeHourlyGain(cafeLevel) * 100.0).roundToInt() / 100.0}")
            appendLine("cafeDailyCapacity=${cafeDailyCapacity(cafeLevel)}")
            appendLine("cafeStorageCap=$cafeCap")
            appendLine("cafeStoredAp=${(cafeStored * 100.0).roundToInt() / 100.0}")
            appendLine("cafeStoredPercent=${(cafePercent * 10.0).roundToInt() / 10.0}")
            appendLine("coffeeHeadpatLastMs=${snapshot.coffeeHeadpatMs}")
            appendLine("coffeeHeadpatReadyAtMs=$headpatReadyAtMs")
            appendLine("coffeeInvite1LastMs=${snapshot.coffeeInvite1UsedMs}")
            appendLine("coffeeInvite1ReadyAtMs=$invite1ReadyAtMs")
            appendLine("coffeeInvite2LastMs=${snapshot.coffeeInvite2UsedMs}")
            appendLine("coffeeInvite2ReadyAtMs=$invite2ReadyAtMs")
            appendLine("showEndedPools=${snapshot.showEndedPools}")
            appendLine("showEndedActivities=${snapshot.showEndedActivities}")
            appendLine("showCalendarPoolImages=${snapshot.showCalendarPoolImages}")
            appendLine("idNickname=${snapshot.idNickname}")
            appendLine("idFriendCode=${snapshot.idFriendCode}")
        }.trim()
    }

    private fun buildBaCalendarCacheText(
        requestedServerIndex: Int?,
        includeEntries: Boolean,
        limit: Int
    ): String {
        val nowMs = System.currentTimeMillis()
        val refreshHours = BASettingsStore.loadCalendarRefreshIntervalHours()
        val refreshIntervalMs = refreshHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val serverIndex = resolveServerIndex(requestedServerIndex)
        val snapshot = BASettingsStore.loadCalendarCacheSnapshot(serverIndex)
        val entries = runCatching { decodeBaCalendarEntries(snapshot.raw, nowMs) }.getOrElse { emptyList() }
        val expired = snapshot.syncMs <= 0L || (nowMs - snapshot.syncMs).coerceAtLeast(0L) >= refreshIntervalMs

        return buildString {
            appendLine("serverIndex=$serverIndex")
            appendLine("gameKeeServerId=${gameKeeServerId(serverIndex)}")
            appendLine("refreshIntervalHours=$refreshHours")
            appendLine("cacheVersion=${snapshot.version}")
            appendLine("cachePresent=${snapshot.raw.isNotBlank()}")
            appendLine("cacheRawChars=${snapshot.raw.length}")
            appendLine("cacheSyncMs=${snapshot.syncMs}")
            appendLine("cacheExpired=$expired")
            appendLine("entryCount=${entries.size}")
            if (includeEntries && entries.isNotEmpty()) {
                entries.take(limit).forEachIndexed { index, entry ->
                    appendLine(
                        "entry[$index]=id:${entry.id} | kind:${entry.kindName} | running:${entry.isRunning} | title:${entry.title} | beginAtMs:${entry.beginAtMs} | endAtMs:${entry.endAtMs} | link:${entry.linkUrl}"
                    )
                }
            }
        }.trim()
    }

    private fun buildBaPoolCacheText(
        requestedServerIndex: Int?,
        includeEntries: Boolean,
        limit: Int
    ): String {
        val nowMs = System.currentTimeMillis()
        val refreshHours = BASettingsStore.loadCalendarRefreshIntervalHours()
        val refreshIntervalMs = refreshHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val serverIndex = resolveServerIndex(requestedServerIndex)
        val snapshot = BASettingsStore.loadPoolCacheSnapshot(serverIndex)
        val entries = runCatching { decodeBaPoolEntries(snapshot.raw, nowMs) }.getOrElse { emptyList() }
        val expired = snapshot.syncMs <= 0L || (nowMs - snapshot.syncMs).coerceAtLeast(0L) >= refreshIntervalMs

        return buildString {
            appendLine("serverIndex=$serverIndex")
            appendLine("gameKeeServerId=${gameKeeServerId(serverIndex)}")
            appendLine("refreshIntervalHours=$refreshHours")
            appendLine("cacheVersion=${snapshot.version}")
            appendLine("cachePresent=${snapshot.raw.isNotBlank()}")
            appendLine("cacheRawChars=${snapshot.raw.length}")
            appendLine("cacheSyncMs=${snapshot.syncMs}")
            appendLine("cacheExpired=$expired")
            appendLine("entryCount=${entries.size}")
            if (includeEntries && entries.isNotEmpty()) {
                entries.take(limit).forEachIndexed { index, entry ->
                    appendLine(
                        "entry[$index]=id:${entry.id} | tag:${entry.tagName} | running:${entry.isRunning} | name:${entry.name} | startAtMs:${entry.startAtMs} | endAtMs:${entry.endAtMs} | link:${entry.linkUrl}"
                    )
                }
            }
        }.trim()
    }

    private fun parseCatalogTab(raw: String): BaGuideCatalogTab? {
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "", "all" -> null
            "student", "students", "实装学生" -> BaGuideCatalogTab.Student
            "npc", "npcs", "npc_satellite", "npc-satellite", "npc及卫星" -> BaGuideCatalogTab.NpcSatellite
            else -> null
        }
    }

    private fun buildGuideCatalogCacheText(
        tab: String,
        includeEntries: Boolean,
        limit: Int
    ): String {
        val filterTab = parseCatalogTab(tab)
        val bundle = loadCachedBaGuideCatalogBundle()
        val counts = BaGuideCatalogStore.cachedEntryCounts()
        val refreshHours = BASettingsStore.loadCalendarRefreshIntervalHours()
        val complete = isBaGuideCatalogBundleComplete(bundle)
        val expired = isBaGuideCatalogCacheExpired(bundle, refreshHours)
        val latestSyncedAtMs = bundle?.syncedAtMs ?: BaGuideCatalogStore.latestSyncedAtMs()

        val selectedEntries = buildList {
            if (bundle != null) {
                if (filterTab == null) {
                    BaGuideCatalogTab.entries.forEach { currentTab ->
                        addAll(bundle.entries(currentTab))
                    }
                } else {
                    addAll(bundle.entries(filterTab))
                }
            }
        }

        return buildString {
            appendLine("bundlePresent=${bundle != null}")
            appendLine("bundleComplete=$complete")
            appendLine("bundleExpired=$expired")
            appendLine("refreshIntervalHours=$refreshHours")
            appendLine("latestSyncedAtMs=$latestSyncedAtMs")
            appendLine("totalCachedEntryCount=${BaGuideCatalogStore.cachedEntryCount()}")
            appendLine("studentCount=${counts[BaGuideCatalogTab.Student] ?: 0}")
            appendLine("npcSatelliteCount=${counts[BaGuideCatalogTab.NpcSatellite] ?: 0}")
            appendLine("actualDataBytes=${BaGuideCatalogStore.actualDataBytes()}")
            appendLine("cacheBytesEstimated=${BaGuideCatalogStore.cacheBytesEstimated()}")
            appendLine("configBytesEstimated=${BaGuideCatalogStore.configBytesEstimated()}")
            appendLine("selectedTab=${filterTab?.name ?: "ALL"}")
            appendLine("selectedEntryCount=${selectedEntries.size}")
            if (includeEntries && selectedEntries.isNotEmpty()) {
                selectedEntries.take(limit).forEachIndexed { index, entry ->
                    appendLine(
                        "entry[$index]=tab:${entry.tab.name} | contentId:${entry.contentId} | name:${entry.name} | alias:${entry.aliasDisplay} | createdAtSec:${entry.createdAtSec} | detailUrl:${entry.detailUrl}"
                    )
                }
            }
        }.trim()
    }

    private fun buildGuideCacheOverviewText(): String {
        return buildString {
            appendLine("currentUrl=${BaStudentGuideStore.loadCurrentUrl()}")
            appendLine("cachedEntryCount=${BaStudentGuideStore.cachedEntryCount()}")
            appendLine("latestSyncedAtMs=${BaStudentGuideStore.latestSyncedAtMs()}")
            appendLine("storageFootprintBytes=${BaStudentGuideStore.storageFootprintBytes()}")
            appendLine("actualDataBytes=${BaStudentGuideStore.actualDataBytes()}")
            appendLine("cacheBytesEstimated=${BaStudentGuideStore.cacheBytesEstimated()}")
            appendLine("configBytesEstimated=${BaStudentGuideStore.configBytesEstimated()}")
        }.trim()
    }

    private fun extractGuideContentId(url: String): Long {
        val text = url.trim()
        if (text.isBlank()) return 0L
        val patterns = listOf(
            Regex("/v1/content/detail/(\\d+)"),
            Regex("/tj/(\\d+)\\.html"),
            Regex("/(\\d+)\\.html")
        )
        patterns.forEach { regex ->
            val hit = regex.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (hit != null && hit > 0L) return hit
        }
        return 0L
    }

    private fun buildGuideCacheInspectText(
        url: String,
        includeSections: Boolean,
        refreshIntervalHours: Int
    ): String {
        val target = normalizeGuideUrl(url).ifBlank { BaStudentGuideStore.loadCurrentUrl() }
        if (target.isBlank()) {
            return "hasTarget=false\nmessage=No target URL. Pass url argument or open a student guide page first."
        }
        val snapshot = BaStudentGuideStore.loadInfoSnapshot(target)
        val expired = BaStudentGuideStore.isCacheExpired(
            snapshot = snapshot,
            refreshIntervalHours = refreshIntervalHours
        )
        val info = snapshot.info

        return buildString {
            appendLine("hasTarget=true")
            appendLine("targetUrl=$target")
            appendLine("contentId=${extractGuideContentId(target)}")
            appendLine("hasCache=${snapshot.hasCache}")
            appendLine("isComplete=${snapshot.isComplete}")
            appendLine("isExpired=$expired")
            appendLine("refreshIntervalHours=$refreshIntervalHours")
            appendLine("syncedAtMs=${snapshot.syncedAtMs}")
            appendLine("infoPresent=${info != null}")
            if (info != null) {
                appendLine("title=${info.title}")
                appendLine("subtitle=${info.subtitle}")
                appendLine("summary=${info.summary}")
                appendLine("statsCount=${info.stats.size}")
                appendLine("skillRowsCount=${info.skillRows.size}")
                appendLine("profileRowsCount=${info.profileRows.size}")
                appendLine("galleryItemsCount=${info.galleryItems.size}")
                appendLine("growthRowsCount=${info.growthRows.size}")
                appendLine("simulateRowsCount=${info.simulateRows.size}")
                appendLine("voiceRowsCount=${info.voiceRows.size}")
                appendLine("voiceEntriesCount=${info.voiceEntries.size}")
                appendLine("voiceHeadersCount=${info.voiceLanguageHeaders.size}")
                appendLine("voiceCvLangCount=${info.voiceCvByLanguage.size}")
                if (includeSections) {
                    appendLine("voiceCvByLanguage=${info.voiceCvByLanguage.entries.joinToString(" | ") { "${it.key}:${it.value}" }}")
                    appendLine("tabSkillIconUrl=${info.tabSkillIconUrl}")
                    appendLine("tabProfileIconUrl=${info.tabProfileIconUrl}")
                    appendLine("tabVoiceIconUrl=${info.tabVoiceIconUrl}")
                    appendLine("tabGalleryIconUrl=${info.tabGalleryIconUrl}")
                    appendLine("tabSimulateIconUrl=${info.tabSimulateIconUrl}")
                }
            }
        }.trim()
    }

    private fun normalizeCacheClearScope(raw: String): String {
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "ba_calendar_pool", "calendar_pool", "ba-calendar-pool" -> "ba_calendar_pool"
            "ba_guide_catalog", "guide_catalog", "ba-guide-catalog" -> "ba_guide_catalog"
            "ba_guide_all", "guide_all", "ba-guide-all" -> "ba_guide_all"
            "ba_guide_url", "guide_url", "ba-guide-url" -> "ba_guide_url"
            "github_check", "github", "github_cache", "github-check" -> "github_check"
            else -> "all"
        }
    }

    private fun buildCacheClearText(scope: String, url: String): String {
        val normalizedScope = normalizeCacheClearScope(scope)
        val cleared = mutableListOf<String>()
        var message = "ok"

        when (normalizedScope) {
            "ba_calendar_pool" -> {
                BASettingsStore.clearCalendarAndPoolCaches()
                cleared += "ba_calendar_pool"
            }

            "ba_guide_catalog" -> {
                clearBaGuideCatalogCache(appContext)
                cleared += "ba_guide_catalog"
            }

            "ba_guide_all" -> {
                BaStudentGuideStore.clearAllCachedInfo()
                cleared += "ba_guide_all"
            }

            "ba_guide_url" -> {
                val target = normalizeGuideUrl(url)
                if (target.isBlank()) {
                    message = "url_required_for_ba_guide_url"
                } else {
                    BaStudentGuideStore.clearCachedInfo(target)
                    cleared += "ba_guide_url:$target"
                }
            }

            "github_check" -> {
                GitHubTrackStore.clearCheckCache()
                GitHubReleaseAssetCacheStore.clearAll()
                cleared += "github_check"
            }

            else -> {
                BASettingsStore.clearCalendarAndPoolCaches()
                BaStudentGuideStore.clearAllCachedInfo()
                clearBaGuideCatalogCache(appContext)
                GitHubTrackStore.clearCheckCache()
                GitHubReleaseAssetCacheStore.clearAll()
                cleared += "ba_calendar_pool"
                cleared += "ba_guide_all"
                cleared += "ba_guide_catalog"
                cleared += "github_check"
            }
        }

        return buildString {
            appendLine("scope=$normalizedScope")
            appendLine("message=$message")
            appendLine("cleared=${if (cleared.isEmpty()) "none" else cleared.joinToString(",")}")
        }.trim()
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

    private fun argIntOrNull(value: Any?): Int? {
        return argString(value).trim().toIntOrNull()
    }

    private fun argBoolean(value: Any?, defaultValue: Boolean): Boolean {
        val raw = argString(value).trim().lowercase(Locale.ROOT)
        return when (raw) {
            "1", "true", "yes", "y", "on" -> true
            "0", "false", "no", "n", "off" -> false
            else -> defaultValue
        }
    }

    private fun checkTrackedGitHub(repoFilter: String): List<GitHubCheckRow> {
        val items = GitHubTrackStore.load()
        val filtered = filterTrackedItems(items, repoFilter)
        return filtered.map { item ->
            runCatching { evaluateTrackedApp(item) }.getOrElse { err ->
                GitHubCheckRow(
                    item = item,
                    localVersion = runCatching {
                        GitHubVersionUtils.localVersionName(appContext, item.packageName)
                    }.getOrDefault("unknown"),
                    stableVersion = "unknown",
                    preReleaseVersion = "",
                    status = "检查失败: ${err.message ?: "unknown"}",
                    hasUpdate = false
                )
            }
        }
    }

    private fun evaluateTrackedApp(item: GitHubTrackedApp): GitHubCheckRow {
        val check = GitHubReleaseCheckService.evaluateTrackedApp(appContext, item)
        return GitHubCheckRow(
            item = item,
            localVersion = check.localVersion,
            stableVersion = check.stableRelease?.displayVersion.orEmpty().ifBlank { "unknown" },
            preReleaseVersion = check.preReleaseInfo,
            status = check.message,
            hasUpdate = check.hasUpdate == true
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
        val kv = MMKV.mmkvWithID(OS_CACHE_KV_ID)
        val legacyKv = MMKV.mmkvWithID(LEGACY_SYSTEM_CACHE_KV_ID)
        val readRaw: (String, String) -> String? = { newKey, legacyKey ->
            val newRaw = kv.decodeString(newKey)
            if (!newRaw.isNullOrBlank()) newRaw else legacyKv.decodeString(legacyKey)
        }
        val allRows = (
            decodeRows(readRaw(KEY_OS_SYSTEM, LEGACY_KEY_SYSTEM)) +
                decodeRows(readRaw(KEY_OS_SECURE, LEGACY_KEY_SECURE)) +
                decodeRows(readRaw(KEY_OS_GLOBAL, LEGACY_KEY_GLOBAL)) +
                decodeRows(readRaw(KEY_OS_ANDROID, LEGACY_KEY_ANDROID)) +
                decodeRows(readRaw(KEY_OS_JAVA, LEGACY_KEY_JAVA)) +
                decodeRows(readRaw(KEY_OS_LINUX, LEGACY_KEY_LINUX))
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
