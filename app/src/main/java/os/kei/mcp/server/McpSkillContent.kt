package os.kei.mcp.server

import android.os.Build
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.Locale

internal class McpSkillContent(
    private val environment: McpToolEnvironment,
    private val runtimeConfigBuilder: (McpServerUiState?, String, String, String) -> String
) {
    private val appVersionName: String get() = environment.appVersionName
    private val appVersionCode: Long get() = environment.appVersionCode
    private val appPackageName: String get() = environment.appPackageName
    private val appLabel: String get() = environment.appLabel

    fun registerClawGuideTool(server: Server) {
        val locale = currentLocale()
        server.addTool(
            name = "keios.mcp.claw.skill.guide",
            description = McpToolCatalog.descriptionFor("keios.mcp.claw.skill.guide", locale),
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("mode", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("endpoint", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("serverName", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val state = environment.currentState()
            val mode = argString(request.arguments?.get("mode"))
            val endpoint = argString(request.arguments?.get("endpoint")).trim()
            val serverName = argString(request.arguments?.get("serverName")).trim()
            callText(
                buildClawSkillGuideText(
                    state = state,
                    mode = mode,
                    endpointOverride = endpoint,
                    serverNameOverride = serverName
                )
            )
        }
    }

    fun registerResources(server: Server) {
        val locale = currentLocale()
        server.addResource(
            uri = SKILL_RESOURCE_URI,
            name = "keios-mcp-skill",
            description = if (isSimplifiedChinese(locale)) "KeiOS MCP Skill 指南" else "KeiOS MCP skill guide",
            mimeType = MIME_MARKDOWN
        ) { _ ->
            callResource(uri = SKILL_RESOURCE_URI, mimeType = MIME_MARKDOWN, text = loadSkillMarkdown())
        }

        server.addResource(
            uri = SKILL_OVERVIEW_URI,
            name = "keios-mcp-skill-overview",
            description = if (isSimplifiedChinese(locale)) "MCP Skill 快速总览" else "Quick MCP skill overview",
            mimeType = MIME_TEXT
        ) { _ ->
            callResource(uri = SKILL_OVERVIEW_URI, mimeType = MIME_TEXT, text = buildSkillOverview())
        }

        server.addResourceTemplate(
            uriTemplate = SKILL_TOOL_TEMPLATE_URI,
            name = "keios-mcp-tool-help",
            description = if (isSimplifiedChinese(locale)) "KeiOS MCP 单工具帮助" else "Tool-level help for KeiOS MCP",
            mimeType = MIME_MARKDOWN
        ) { _, params ->
            val tool = params["tool"].orEmpty()
            callResource(
                uri = SKILL_TOOL_TEMPLATE_URI.replace("{tool}", tool),
                mimeType = MIME_MARKDOWN,
                text = buildToolHelp(tool)
            )
        }

        server.addResource(
            uri = CONFIG_RESOURCE_URI,
            name = "keios-mcp-config-default",
            description = if (isSimplifiedChinese(locale)) {
                "默认 MCP 配置包 JSON（auto mode）"
            } else {
                "Default MCP config package JSON (auto mode)"
            },
            mimeType = MIME_JSON
        ) { _ ->
            val state = environment.currentState()
            callResource(
                uri = CONFIG_RESOURCE_URI,
                mimeType = MIME_JSON,
                text = runtimeConfigBuilder(state, "auto", "", "")
            )
        }

        server.addResourceTemplate(
            uriTemplate = CONFIG_TEMPLATE_URI,
            name = "keios-mcp-config-template",
            description = if (isSimplifiedChinese(locale)) {
                "按 mode 生成 MCP 配置包 JSON（auto/local/lan）"
            } else {
                "MCP config package JSON by mode (auto/local/lan)"
            },
            mimeType = MIME_JSON
        ) { _, params ->
            val state = environment.currentState()
            val mode = normalizeMcpConfigMode(params["mode"].orEmpty())
            callResource(
                uri = CONFIG_TEMPLATE_URI.replace("{mode}", mode),
                mimeType = MIME_JSON,
                text = runtimeConfigBuilder(state, mode, "", "")
            )
        }
    }

    fun registerPrompt(server: Server) {
        val locale = currentLocale()
        server.addPrompt(
            name = BOOTSTRAP_PROMPT,
            description = if (isSimplifiedChinese(locale)) {
                "用于初始化 KeiOS MCP 工具使用方式的启动 Prompt。"
            } else {
                "Bootstrap prompt for using KeiOS MCP tools."
            },
            arguments = listOf(
                PromptArgument(
                    name = "task",
                    description = if (isSimplifiedChinese(locale)) {
                        "当前用户目标，例如检查 BA 缓存或巡检 GitHub 更新"
                    } else {
                        "Current user goal such as check BA cache or inspect GitHub updates"
                    },
                    required = false,
                    title = if (isSimplifiedChinese(locale)) "任务" else "Task"
                )
            )
        ) { request ->
            val task = request.arguments?.get("task").orEmpty().trim()
            val promptText = buildBootstrapPromptText(task = task, locale = currentLocale())

            GetPromptResult(
                description = if (isSimplifiedChinese(currentLocale())) {
                    "KeiOS MCP 启动 Prompt"
                } else {
                    "KeiOS MCP bootstrap prompt"
                },
                messages = listOf(
                    PromptMessage(
                        role = Role.User,
                        content = TextContent(promptText)
                    )
                )
            )
        }
    }

    fun buildServerInstructions(): String {
        val locale = currentLocale()
        if (isSimplifiedChinese(locale)) {
            return buildString {
                appendLine("KeiOS 本地 MCP 服务")
                appendLine("- 先调用 keios.health.ping，再调用 keios.mcp.runtime.status。")
                appendLine("- 执行任务前先读取快速总览 $SKILL_OVERVIEW_URI。")
                appendLine("- 缺少任务上下文时使用 $BOOTSTRAP_PROMPT。")
                appendLine("- 使用 keios.mcp.runtime.config 或资源 $CONFIG_RESOURCE_URI / $CONFIG_TEMPLATE_URI 生成导入 JSON。")
                appendLine("- Home 总览使用 keios.home.overview.snapshot。")
                appendLine("- OS 巡检使用 keios.os.cards.snapshot / keios.os.activity.cards / keios.os.shell.cards。")
                appendLine("- GitHub 分享导入使用 keios.github.share.parse / keios.github.share.resolve。")
                appendLine("- 完整 Skill 文档资源：$SKILL_RESOURCE_URI")
                appendLine("- 单工具帮助模板：$SKILL_TOOL_TEMPLATE_URI")
            }.trim()
        }
        return buildString {
            appendLine("KeiOS local MCP server")
            appendLine("- Start with keios.health.ping, then keios.mcp.runtime.status.")
            appendLine("- Read quick overview from $SKILL_OVERVIEW_URI before task execution.")
            appendLine("- Use $BOOTSTRAP_PROMPT when task context is missing.")
            appendLine("- Use keios.mcp.runtime.config or resources $CONFIG_RESOURCE_URI / $CONFIG_TEMPLATE_URI for import JSON.")
            appendLine("- Home overview can use keios.home.overview.snapshot.")
            appendLine("- OS diagnostics can use keios.os.cards.snapshot / keios.os.activity.cards / keios.os.shell.cards.")
            appendLine("- GitHub shared-link intake can use keios.github.share.parse / keios.github.share.resolve.")
            appendLine("- Full skill doc resource: $SKILL_RESOURCE_URI")
            appendLine("- Tool help template resource: $SKILL_TOOL_TEMPLATE_URI")
        }.trim()
    }

    private fun buildBootstrapPromptText(task: String, locale: Locale): String {
        if (isSimplifiedChinese(locale)) {
            return buildString {
                appendLine("你正在连接 KeiOS 本地 MCP 服务。")
                appendLine("按以下顺序初始化：")
                appendLine("1) keios.health.ping")
                appendLine("2) keios.mcp.runtime.status")
                appendLine("3) keios.mcp.runtime.config(mode=auto)")
                appendLine("4) 读取资源 $SKILL_OVERVIEW_URI")
                appendLine("5) 按任务读取 $SKILL_RESOURCE_URI 或模板 $SKILL_TOOL_TEMPLATE_URI")
                appendLine("6) 需要导入配置时读取 $CONFIG_RESOURCE_URI 或模板 $CONFIG_TEMPLATE_URI")
                appendLine()
                appendLine("常用工具分组：")
                appendLine("- 运行排障：keios.mcp.runtime.status / keios.mcp.runtime.logs / keios.shizuku.status")
                appendLine("- Home 总览：keios.home.overview.snapshot")
                appendLine("- OS 页面：keios.os.cards.snapshot / keios.os.activity.cards / keios.os.shell.cards")
                appendLine("- 系统参数：keios.system.topinfo.query")
                appendLine("- GitHub 跟踪与分享：keios.github.tracked.snapshot / list / check / share.parse / share.resolve")
                appendLine("- Blue Archive 缓存与媒体：keios.ba.snapshot / keios.ba.guide.cache.inspect / keios.ba.guide.media.list / keios.ba.guide.bgm.favorites")
                appendLine("- 导入导出：keios.github.tracked.export / keios.github.tracked.import / keios.os.cards.export / keios.os.cards.import")
                appendLine("- 缓存清理：keios.github.tracked.cache.clear / keios.ba.cache.clear")
                if (task.isNotBlank()) {
                    appendLine()
                    appendLine("当前任务：$task")
                    appendLine("先给出不超过 4 步的工具调用计划，再执行。")
                }
            }.trim()
        }
        return buildString {
            appendLine("You are connected to the local KeiOS MCP server.")
            appendLine("Initialize in this order:")
            appendLine("1) keios.health.ping")
            appendLine("2) keios.mcp.runtime.status")
            appendLine("3) keios.mcp.runtime.config(mode=auto)")
            appendLine("4) Read resource $SKILL_OVERVIEW_URI")
            appendLine("5) Read $SKILL_RESOURCE_URI or template $SKILL_TOOL_TEMPLATE_URI for the task")
            appendLine("6) Read $CONFIG_RESOURCE_URI or template $CONFIG_TEMPLATE_URI when import config is needed")
            appendLine()
            appendLine("Common tool groups:")
            appendLine("- Runtime diagnostics: keios.mcp.runtime.status / keios.mcp.runtime.logs / keios.shizuku.status")
            appendLine("- Home overview: keios.home.overview.snapshot")
            appendLine("- OS page: keios.os.cards.snapshot / keios.os.activity.cards / keios.os.shell.cards")
            appendLine("- System values: keios.system.topinfo.query")
            appendLine("- GitHub tracking and share import: keios.github.tracked.snapshot / list / check / share.parse / share.resolve")
            appendLine("- Blue Archive cache and media: keios.ba.snapshot / keios.ba.guide.cache.inspect / keios.ba.guide.media.list / keios.ba.guide.bgm.favorites")
            appendLine("- Import and export: keios.github.tracked.export / keios.github.tracked.import / keios.os.cards.export / keios.os.cards.import")
            appendLine("- Cache cleanup: keios.github.tracked.cache.clear / keios.ba.cache.clear")
            if (task.isNotBlank()) {
                appendLine()
                appendLine("Current task: $task")
                appendLine("Give a tool-call plan of up to 4 steps before executing.")
            }
        }.trim()
    }

    fun loadSkillMarkdown(): String {
        val locale = currentLocale()
        return runCatching {
            environment.appContext.assets.open(localizedSkillAssetPath(locale))
                .bufferedReader()
                .use { it.readText() }
        }.map { template ->
            renderSkillTemplate(template, locale)
        }.getOrElse {
            buildFallbackSkillMarkdown(locale)
        }
    }

    private fun renderSkillTemplate(template: String, locale: Locale): String {
        val state = environment.currentState()
        val appVersion = "$appVersionName ($appVersionCode)"
        val serverName = state?.serverName ?: "KeiOS MCP"
        val localEndpoint = state?.localEndpoint ?: DEFAULT_ENDPOINT
        val lanEndpoints = state?.lanEndpoints?.takeIf { it.isNotEmpty() }?.joinToString(" | ") ?: "N/A"
        val toolList = McpToolCatalog.forLocale(locale).joinToString("\n") { meta ->
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

    private fun buildFallbackSkillMarkdown(locale: Locale): String {
        if (isSimplifiedChinese(locale)) {
            return buildString {
                appendLine("# KeiOS MCP Skill")
                appendLine()
                appendLine("## 起步流程")
                appendLine("1. keios.health.ping")
                appendLine("2. keios.mcp.runtime.status")
                appendLine("3. keios.mcp.runtime.config")
                appendLine("4. 读取资源：$CONFIG_RESOURCE_URI")
                appendLine()
                appendLine("## 工具分组")
                McpToolCatalog.forLocale(locale).forEach { meta ->
                    appendLine("- ${meta.name}: ${meta.description}")
                }
            }.trim()
        }
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
            McpToolCatalog.forLocale(locale).forEach { meta ->
                appendLine("- ${meta.name}: ${meta.description}")
            }
        }.trim()
    }

    private fun buildSkillOverview(): String {
        val state = environment.currentState()
        val locale = currentLocale()
        return buildString {
            appendLine("skillResource=$SKILL_RESOURCE_URI")
            appendLine("skillOverviewResource=$SKILL_OVERVIEW_URI")
            appendLine("skillToolTemplate=$SKILL_TOOL_TEMPLATE_URI")
            appendLine("bootstrapPrompt=$BOOTSTRAP_PROMPT")
            appendLine("language=${locale.toLanguageTag()}")
            appendLine("configResource=$CONFIG_RESOURCE_URI")
            appendLine("configTemplate=$CONFIG_TEMPLATE_URI")
            appendLine("recommendedConfigTool=keios.mcp.runtime.config")
            appendLine("localEndpoint=${state?.localEndpoint ?: DEFAULT_ENDPOINT}")
            if (state?.lanEndpoints?.isNotEmpty() == true) {
                appendLine("lanEndpoints=${state.lanEndpoints.joinToString(",")}")
            }
            appendLine("homeTools=${McpToolCatalog.homeToolNames.joinToString(",")}")
            appendLine("runtimeTools=${McpToolCatalog.runtimeToolNames.joinToString(",")}")
            appendLine("osTools=${McpToolCatalog.osToolNames.joinToString(",")}")
            appendLine("systemTools=${McpToolCatalog.systemToolNames.joinToString(",")}")
            appendLine("githubTools=${McpToolCatalog.githubToolNames.joinToString(",")}")
            appendLine("baTools=${McpToolCatalog.baToolNames.joinToString(",")}")
            appendLine("defaultFlow=keios.health.ping->keios.mcp.runtime.status->keios.mcp.runtime.config")
            appendLine("toolCount=${McpToolCatalog.all.size}")
            appendLine("tools=${McpToolCatalog.all.joinToString(",") { it.name }}")
        }.trim()
    }

    private fun buildToolHelp(tool: String): String {
        val locale = currentLocale()
        val normalized = tool.trim().lowercase(Locale.ROOT)
        val localizedTools = McpToolCatalog.forLocale(locale)
        val hit = localizedTools.firstOrNull { it.name.lowercase(Locale.ROOT) == normalized }
        if (hit == null) {
            return buildString {
                appendLine(if (isSimplifiedChinese(locale)) "# 未知工具" else "# Unknown Tool")
                appendLine("tool=$tool")
                appendLine("available=${McpToolCatalog.all.joinToString(",") { it.name }}")
            }.trim()
        }

        return buildString {
            appendLine("# ${hit.name}")
            appendLine()
            appendLine(hit.description)
            appendLine()
            appendLine(if (isSimplifiedChinese(locale)) "## 建议用法" else "## Suggested Usage")
            appendToolUsage(hit.name, locale)
        }.trim()
    }

    private fun StringBuilder.appendToolUsage(name: String, locale: Locale) {
        if (isSimplifiedChinese(locale)) {
            appendChineseToolUsage(name)
            return
        }
        when (name) {
            "keios.health.ping" -> {
                appendLine("- Use this as the first call to verify connectivity.")
                appendLine("- The return value is always pong.")
            }

            "keios.app.info", "keios.app.version", "keios.shizuku.status" -> {
                appendLine("- Use this for environment confirmation.")
                appendLine("- Output is key=value text that can be copied directly into reports.")
            }

            "keios.mcp.runtime.status" -> {
                appendLine("- Check running, connectedClients, and localEndpoint first.")
                appendLine("- Call keios.mcp.runtime.logs when an error is present.")
            }

            "keios.mcp.runtime.logs" -> {
                appendLine("- Start with limit=30, then increase it for deeper diagnostics.")
                appendLine("- Logs are returned in reverse chronological order.")
            }

            "keios.mcp.runtime.config" -> {
                appendLine("- Use mode=auto by default.")
                appendLine("- Use mode=local for clients on the same device.")
                appendLine("- Use mode=lan for cross-device debugging.")
                appendLine("- Pass endpoint to target a temporary endpoint.")
            }

            "keios.mcp.claw.skill.guide" -> {
                appendLine("- Generates a complete Claw onboarding guide.")
                appendLine("- Output includes importable JSON, Skill resource URIs, and full SKILL.md text.")
                appendLine("- Start with mode=auto, then switch to local or lan for targeted debugging.")
            }

            "keios.home.overview.snapshot" -> {
                appendLine("- Reads the data source for the three Home overview cards.")
                appendLine("- Use it to spot key MCP, GitHub, and Blue Archive state changes first.")
            }

            "keios.system.topinfo.query" -> {
                appendLine("- Leave query empty to return high-signal values.")
                appendLine("- Use limit to control output size.")
            }

            "keios.os.cards.snapshot" -> {
                appendLine("- Reads the overall OS page state as a first snapshot.")
                appendLine("- Visibility, expansion state, cache size, and estimates are returned together.")
            }

            "keios.os.activity.cards" -> {
                appendLine("- Use query to filter Activity cards by title, package, or class name.")
                appendLine("- Use onlyVisible=true to inspect currently visible Activity cards.")
                appendLine("- Use limit to control the number of returned entries.")
            }

            "keios.os.shell.cards" -> {
                appendLine("- Returns shell card metadata and commands by default.")
                appendLine("- includeOutput=true appends a run-output summary.")
                appendLine("- Combine onlyVisible=true with query for precise filtering.")
            }

            "keios.os.cards.export" -> {
                appendLine("- target=activity exports Activity cards.")
                appendLine("- target=shell exports shell cards.")
                appendLine("- target=all generates an activity/shell bundle.")
            }

            "keios.os.cards.import" -> {
                appendLine("- target must be activity or shell.")
                appendLine("- apply=false returns an import preview.")
                appendLine("- apply=true merges data into the local card store.")
            }

            "keios.github.tracked.snapshot", "keios.github.tracked.list", "keios.github.tracked.summary" -> {
                appendLine("- Start with snapshot, then drill into list or summary.")
                appendLine("- repoFilter accepts owner/repo, package name, or app label.")
            }

            "keios.github.tracked.export" -> {
                appendLine("- Exports the current GitHub tracking list as JSON.")
                appendLine("- Use repoFilter to export a subset.")
            }

            "keios.github.tracked.import" -> {
                appendLine("- apply=false returns added, updated, and unchanged counts.")
                appendLine("- apply=true merges tracking items and clears check cache.")
            }

            "keios.github.tracked.check" -> {
                appendLine("- Use onlyUpdates=true for a quick update-focused pass.")
                appendLine("- repoFilter can filter by owner/repo, package name, or app label.")
            }

            "keios.github.tracked.cache.clear" -> {
                appendLine("- Run tracked.check again after clearing to get fresh state.")
                appendLine("- Release asset cache is cleared at the same time.")
            }

            "keios.github.share.parse" -> {
                appendLine("- Parses GitHub links from shared text.")
                appendLine("- Supports repo, releases, latest, tag, and download asset links.")
            }

            "keios.github.share.resolve" -> {
                appendLine("- Resolves a shared link and fetches APK asset candidates online.")
                appendLine("- Uses the current GitHub strategy config and fallback strategy.")
            }

            "keios.github.share.pending" -> {
                appendLine("- clear=false inspects pre-install tracking state.")
                appendLine("- clear=true clears the pending install-link record.")
            }

            "keios.ba.snapshot", "keios.ba.calendar.cache", "keios.ba.pool.cache", "keios.ba.guide.catalog.cache", "keios.ba.guide.cache.overview" -> {
                appendLine("- Start with ba.snapshot for global state.")
                appendLine("- Then drill into calendar, recruitment banner, or Student Guide cache tools.")
            }

            "keios.ba.guide.cache.inspect" -> {
                appendLine("- Leave url empty to use the current Student Guide URL.")
                appendLine("- includeSections=true outputs section statistics.")
                appendLine("- refreshIntervalHours overrides the current freshness window.")
            }

            "keios.ba.guide.media.list" -> {
                appendLine("- Lists gallery and voice media from Student Guide detail cache.")
                appendLine("- kind accepts all, gallery, voice, image, video, or audio.")
            }

            "keios.ba.guide.bgm.favorites" -> {
                appendLine("- action=list reads BGM favorites.")
                appendLine("- action=export exports favorites as JSON.")
                appendLine("- action=import combines with apply to preview or merge imports.")
            }

            "keios.ba.cache.clear" -> {
                appendLine("- scope=all clears Blue Archive and GitHub cache data together.")
                appendLine("- scope=ba_guide_url also requires url.")
            }

            else -> {
                appendLine("- Call directly and parse key=value output.")
            }
        }
    }

    private fun StringBuilder.appendChineseToolUsage(name: String) {
        when (name) {
            "keios.health.ping" -> {
                appendLine("- 作为第一条调用，用来确认 MCP 通道连通。")
                appendLine("- 返回值固定为 pong。")
            }

            "keios.app.info", "keios.app.version", "keios.shizuku.status" -> {
                appendLine("- 用于确认当前 App、版本与 Shizuku 环境。")
                appendLine("- 输出是 key=value 文本，适合直接放入报告。")
            }

            "keios.mcp.runtime.status" -> {
                appendLine("- 先看 running、connectedClients 与 localEndpoint。")
                appendLine("- 出现错误后继续调用 keios.mcp.runtime.logs。")
            }

            "keios.mcp.runtime.logs" -> {
                appendLine("- 先用 limit=30，排障时再扩大范围。")
                appendLine("- 日志按时间倒序返回。")
            }

            "keios.mcp.runtime.config" -> {
                appendLine("- 默认使用 mode=auto。")
                appendLine("- 同设备客户端使用 mode=local。")
                appendLine("- 跨设备调试使用 mode=lan。")
                appendLine("- 需要临时目标时传 endpoint 覆盖。")
            }

            "keios.mcp.claw.skill.guide" -> {
                appendLine("- 直接生成 Claw 接入引导。")
                appendLine("- 输出包含可导入 JSON、Skill 资源 URI 与完整 SKILL.md。")
                appendLine("- 先用 mode=auto，定向调试时切换 local 或 lan。")
            }

            "keios.home.overview.snapshot" -> {
                appendLine("- 读取 Home 三张总览卡片的数据源。")
                appendLine("- 适合先判断 MCP、GitHub、Blue Archive 是否有关键状态变化。")
            }

            "keios.system.topinfo.query" -> {
                appendLine("- query 留空时返回高价值系统参数。")
                appendLine("- 使用 limit 控制输出规模。")
            }

            "keios.os.cards.snapshot" -> {
                appendLine("- 读取 OS 页面整体状态，适合作为第一步快照。")
                appendLine("- 可见卡片、展开态、缓存体积和估算值会一起返回。")
            }

            "keios.os.activity.cards" -> {
                appendLine("- 用 query 按标题、包名或类名筛选 Activity card。")
                appendLine("- onlyVisible=true 只检查当前显示中的 Activity card。")
                appendLine("- 用 limit 控制返回条目数量。")
            }

            "keios.os.shell.cards" -> {
                appendLine("- 默认返回 shell card 元信息和命令。")
                appendLine("- includeOutput=true 会追加运行输出摘要。")
                appendLine("- onlyVisible=true 可配合 query 精确筛选。")
            }

            "keios.os.cards.export" -> {
                appendLine("- target=activity 导出 Activity card。")
                appendLine("- target=shell 导出 shell card。")
                appendLine("- target=all 生成 activity/shell bundle。")
            }

            "keios.os.cards.import" -> {
                appendLine("- target 使用 activity 或 shell。")
                appendLine("- apply=false 返回导入预览。")
                appendLine("- apply=true 合并写入本地 card store。")
            }

            "keios.github.tracked.snapshot", "keios.github.tracked.list", "keios.github.tracked.summary" -> {
                appendLine("- 先用 snapshot 获取总览，再用 list 或 summary 下钻。")
                appendLine("- repoFilter 支持 owner/repo、包名或应用名。")
            }

            "keios.github.tracked.export" -> {
                appendLine("- 导出当前 GitHub 跟踪列表 JSON。")
                appendLine("- 用 repoFilter 导出子集。")
            }

            "keios.github.tracked.import" -> {
                appendLine("- apply=false 返回新增、更新与不变数量。")
                appendLine("- apply=true 合并跟踪项并清理检查缓存。")
            }

            "keios.github.tracked.check" -> {
                appendLine("- 用 onlyUpdates=true 快速筛出有更新的项目。")
                appendLine("- repoFilter 可按 owner/repo、包名或应用名过滤。")
            }

            "keios.github.tracked.cache.clear" -> {
                appendLine("- 清理后再执行 tracked.check 获取新状态。")
                appendLine("- release asset 缓存会同步清理。")
            }

            "keios.github.share.parse" -> {
                appendLine("- 解析分享文本中的 GitHub 链接。")
                appendLine("- 支持 repo、releases、latest、tag 和 download asset 链接。")
            }

            "keios.github.share.resolve" -> {
                appendLine("- 解析分享链接并在线获取 APK asset 候选。")
                appendLine("- 使用当前 GitHub 策略配置和 fallback 策略。")
            }

            "keios.github.share.pending" -> {
                appendLine("- clear=false 查看安装前跟踪状态。")
                appendLine("- clear=true 清除待关联安装记录。")
            }

            "keios.ba.snapshot", "keios.ba.calendar.cache", "keios.ba.pool.cache", "keios.ba.guide.catalog.cache", "keios.ba.guide.cache.overview" -> {
                appendLine("- 先用 ba.snapshot 获取全局状态。")
                appendLine("- 再进入活动日历、招募卡池或学生图鉴缓存工具。")
            }

            "keios.ba.guide.cache.inspect" -> {
                appendLine("- url 留空时使用当前学生图鉴 URL。")
                appendLine("- includeSections=true 输出分区统计。")
                appendLine("- refreshIntervalHours 覆盖当前新鲜度判定窗口。")
            }

            "keios.ba.guide.media.list" -> {
                appendLine("- 从学生图鉴详情缓存列出影画鉴赏与语音媒体。")
                appendLine("- kind 可用 all、gallery、voice、image、video、audio。")
            }

            "keios.ba.guide.bgm.favorites" -> {
                appendLine("- action=list 读取 BGM 收藏。")
                appendLine("- action=export 导出收藏 JSON。")
                appendLine("- action=import 配合 apply 预览或合并导入。")
            }

            "keios.ba.cache.clear" -> {
                appendLine("- scope=all 同时清理 Blue Archive 与 GitHub 缓存。")
                appendLine("- scope=ba_guide_url 需要同时传入 url。")
            }

            else -> {
                appendLine("- 直接调用并解析 key=value 输出。")
            }
        }
    }

    private fun buildClawSkillGuideText(
        state: McpServerUiState?,
        mode: String,
        endpointOverride: String,
        serverNameOverride: String
    ): String {
        val locale = currentLocale()
        val fixedMode = normalizeMcpConfigMode(mode)
        val configJson = runtimeConfigBuilder(state, fixedMode, endpointOverride, serverNameOverride)
        val skillMarkdown = loadSkillMarkdown()
        return buildString {
            appendLine("target=claw")
            appendLine("mode=$fixedMode")
            appendLine("skillResource=$SKILL_RESOURCE_URI")
            appendLine("skillOverviewResource=$SKILL_OVERVIEW_URI")
            appendLine("skillToolTemplate=$SKILL_TOOL_TEMPLATE_URI")
            appendLine("bootstrapPrompt=$BOOTSTRAP_PROMPT")
            appendLine("configTool=keios.mcp.runtime.config")
            appendLine("defaultFlow=keios.health.ping->keios.mcp.runtime.status->keios.mcp.runtime.config")
            appendLine()
            appendLine(if (isSimplifiedChinese(locale)) "## Step 1 - 导入 MCP 配置" else "## Step 1 - Import MCP Config")
            appendLine("```json")
            appendLine(configJson)
            appendLine("```")
            appendLine()
            appendLine(if (isSimplifiedChinese(locale)) "## Step 2 - 载入 Skill" else "## Step 2 - Load Skill")
            if (isSimplifiedChinese(locale)) {
                appendLine("- 读取资源 $SKILL_RESOURCE_URI")
                appendLine("- 读取摘要 $SKILL_OVERVIEW_URI")
                appendLine("- 按工具读取帮助模板 $SKILL_TOOL_TEMPLATE_URI")
            } else {
                appendLine("- Read resource $SKILL_RESOURCE_URI")
                appendLine("- Read overview $SKILL_OVERVIEW_URI")
                appendLine("- Read per-tool help from template $SKILL_TOOL_TEMPLATE_URI")
            }
            appendLine()
            appendLine(if (isSimplifiedChinese(locale)) "## Step 3 - 在 Claw 注册 Skill" else "## Step 3 - Register Skill in Claw")
            if (isSimplifiedChinese(locale)) {
                appendLine("- 建议名称：KeiOS MCP Skill")
                appendLine("- 将下方 SKILL.md 内容保存到 Claw Skill")
                appendLine("- 初始化后先执行 keios.health.ping 与 keios.mcp.runtime.status")
            } else {
                appendLine("- Suggested name: KeiOS MCP Skill")
                appendLine("- Save the SKILL.md content below as the Claw Skill")
                appendLine("- After initialization, call keios.health.ping and keios.mcp.runtime.status first")
            }
            appendLine()
            appendLine("## SKILL.md")
            appendLine("```markdown")
            appendLine(skillMarkdown)
            appendLine("```")
        }.trim()
    }

    private fun localizedSkillAssetPath(locale: Locale): String {
        return if (isSimplifiedChinese(locale)) {
            "mcp/SKILL.zh-CN.md"
        } else {
            "mcp/SKILL.md"
        }
    }

    private fun currentLocale(): Locale {
        val configuration = environment.appContext.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0] ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            configuration.locale ?: Locale.getDefault()
        }
    }

    private fun isSimplifiedChinese(locale: Locale): Boolean {
        return locale.language.equals("zh", ignoreCase = true)
    }
}
