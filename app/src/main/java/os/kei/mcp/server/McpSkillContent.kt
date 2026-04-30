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
            appendLine("## Step 1 - Import MCP Config")
            appendLine("```json")
            appendLine(configJson)
            appendLine("```")
            appendLine()
            appendLine("## Step 2 - Load Skill")
            appendLine("- Read resource $SKILL_RESOURCE_URI")
            appendLine("- Read overview $SKILL_OVERVIEW_URI")
            appendLine("- Read per-tool help from template $SKILL_TOOL_TEMPLATE_URI")
            appendLine()
            appendLine("## Step 3 - Register Skill in Claw")
            appendLine("- Suggested name: KeiOS MCP Skill")
            appendLine("- Save the SKILL.md content below as the Claw Skill")
            appendLine("- After initialization, call keios.health.ping and keios.mcp.runtime.status first")
            appendLine()
            appendLine("## SKILL.md")
            appendLine("```markdown")
            appendLine(skillMarkdown)
            appendLine("```")
        }.trim()
    }
}
