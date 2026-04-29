package os.kei.mcp.server

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
        server.addTool(
            name = "keios.mcp.claw.skill.guide",
            description = "Build Claw onboarding guide with import JSON + SKILL.md content. Args: mode(local|lan|auto), endpoint(optional), serverName(optional).",
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

        server.addResource(
            uri = CONFIG_RESOURCE_URI,
            name = "keios-mcp-config-default",
            description = "Default MCP config package JSON (auto mode)",
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
            description = "MCP config package JSON by mode (auto/local/lan)",
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
                appendLine("- Home 总览：keios.home.overview.snapshot")
                appendLine("- OS 页面：keios.os.cards.snapshot / keios.os.activity.cards / keios.os.shell.cards")
                appendLine("- 系统参数：keios.system.topinfo.query")
                appendLine("- GitHub 跟踪与分享：keios.github.tracked.snapshot / list / check / share.parse / share.resolve")
                appendLine("- BA 缓存与媒体：keios.ba.snapshot / keios.ba.guide.cache.inspect / keios.ba.guide.media.list / keios.ba.guide.bgm.favorites")
                appendLine("- 导入导出：keios.github.tracked.export / keios.github.tracked.import / keios.os.cards.export / keios.os.cards.import")
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

    fun buildServerInstructions(): String {
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

    fun loadSkillMarkdown(): String {
        return runCatching {
            environment.appContext.assets.open("mcp/SKILL.md")
                .bufferedReader()
                .use { it.readText() }
        }.map { template ->
            renderSkillTemplate(template)
        }.getOrElse {
            buildFallbackSkillMarkdown()
        }
    }

    private fun renderSkillTemplate(template: String): String {
        val state = environment.currentState()
        val appVersion = "$appVersionName ($appVersionCode)"
        val serverName = state?.serverName ?: "KeiOS MCP"
        val localEndpoint = state?.localEndpoint ?: DEFAULT_ENDPOINT
        val lanEndpoints = state?.lanEndpoints?.takeIf { it.isNotEmpty() }?.joinToString(" | ") ?: "N/A"
        val toolList = McpToolCatalog.all.joinToString("\n") { meta ->
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
            McpToolCatalog.all.forEach { meta ->
                appendLine("- ${meta.name}: ${meta.description}")
            }
        }.trim()
    }

    private fun buildSkillOverview(): String {
        val state = environment.currentState()
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
        val normalized = tool.trim().lowercase(Locale.ROOT)
        val hit = McpToolCatalog.all.firstOrNull { it.name.lowercase(Locale.ROOT) == normalized }
        if (hit == null) {
            return buildString {
                appendLine("# Unknown Tool")
                appendLine("tool=$tool")
                appendLine("available=${McpToolCatalog.all.joinToString(",") { it.name }}")
            }.trim()
        }

        return buildString {
            appendLine("# ${hit.name}")
            appendLine()
            appendLine(hit.description)
            appendLine()
            appendLine("## Suggested Usage")
            appendToolUsage(hit.name)
        }.trim()
    }

    private fun StringBuilder.appendToolUsage(name: String) {
        when (name) {
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

            "keios.mcp.claw.skill.guide" -> {
                appendLine("- 直接生成 Claw 接入完整引导。")
                appendLine("- 输出包含可导入 JSON、Skill 资源 URI 和完整 SKILL.md 文本。")
                appendLine("- mode 建议先用 auto，定向调试再切 local 或 lan。")
            }

            "keios.home.overview.snapshot" -> {
                appendLine("- 读取 Home 三张总览卡片的数据源。")
                appendLine("- 适合先判断 MCP、GitHub、BA 是否有关键状态变化。")
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

            "keios.os.cards.export" -> {
                appendLine("- target=activity 导出活动 card。")
                appendLine("- target=shell 导出 shell card。")
                appendLine("- target=all 生成 activity/shell bundle。")
            }

            "keios.os.cards.import" -> {
                appendLine("- target 必须是 activity 或 shell。")
                appendLine("- apply=false 只返回导入预览。")
                appendLine("- apply=true 合并写入本地 card store。")
            }

            "keios.github.tracked.snapshot", "keios.github.tracked.list", "keios.github.tracked.summary" -> {
                appendLine("- 先用 snapshot 获取总览，再按 list/summary 下钻。")
                appendLine("- repoFilter 支持 owner/repo、包名、应用名。")
            }

            "keios.github.tracked.export" -> {
                appendLine("- 导出当前 GitHub 跟踪列表 JSON。")
                appendLine("- repoFilter 可生成子集。")
            }

            "keios.github.tracked.import" -> {
                appendLine("- apply=false 返回新增/更新/不变数量。")
                appendLine("- apply=true 合并跟踪项并清理检查缓存。")
            }

            "keios.github.tracked.check" -> {
                appendLine("- 先用 onlyUpdates=true 快速筛选。")
                appendLine("- repoFilter 可按 owner/repo、包名或应用名过滤。")
            }

            "keios.github.tracked.cache.clear" -> {
                appendLine("- 清理后建议再执行 tracked.check 获取新状态。")
                appendLine("- 会同时清理 release asset 缓存。")
            }

            "keios.github.share.parse" -> {
                appendLine("- 解析分享文本中的 GitHub 链接。")
                appendLine("- 支持 repo、releases、latest、tag 和 download asset。")
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
                appendLine("- 再按日历/卡池/图鉴缓存工具下钻。")
            }

            "keios.ba.guide.cache.inspect" -> {
                appendLine("- url 为空时读取当前图鉴 URL。")
                appendLine("- includeSections=true 输出分区统计。")
                appendLine("- refreshIntervalHours 可覆盖当前判定窗口。")
            }

            "keios.ba.guide.media.list" -> {
                appendLine("- 从学生图鉴详情缓存列出影画鉴赏和语音媒体。")
                appendLine("- kind 可用 all/gallery/voice/image/video/audio。")
            }

            "keios.ba.guide.bgm.favorites" -> {
                appendLine("- action=list 读取 BGM 收藏。")
                appendLine("- action=export 导出收藏 JSON。")
                appendLine("- action=import 配合 apply 预览或合并导入。")
            }

            "keios.ba.cache.clear" -> {
                appendLine("- scope=all 可一次性清理 BA/GitHub 缓存。")
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
            appendLine("## Step 1 · 导入 MCP 配置")
            appendLine("```json")
            appendLine(configJson)
            appendLine("```")
            appendLine()
            appendLine("## Step 2 · 载入 Skill")
            appendLine("- 读取资源 $SKILL_RESOURCE_URI")
            appendLine("- 读取摘要 $SKILL_OVERVIEW_URI")
            appendLine("- 按工具读取模板 $SKILL_TOOL_TEMPLATE_URI")
            appendLine()
            appendLine("## Step 3 · 在 Claw 注册 Skill")
            appendLine("- 建议名称：KeiOS MCP Skill")
            appendLine("- 将下方 SKILL.md 内容保存到 Claw Skill")
            appendLine("- 初始化后先执行 keios.health.ping 与 keios.mcp.runtime.status")
            appendLine()
            appendLine("## SKILL.md")
            appendLine("```markdown")
            appendLine(skillMarkdown)
            appendLine("```")
        }.trim()
    }
}
