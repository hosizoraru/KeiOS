package os.kei.mcp.server

import android.content.Context
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import os.kei.core.system.ShizukuApiUtils

class LocalMcpService(
    appContext: Context,
    shizukuApiUtils: ShizukuApiUtils,
    appVersionName: String,
    appVersionCode: Long,
    appPackageName: String,
    appLabel: String
) {
    @Volatile
    private var cachedServer: Server? = null

    @Volatile
    private var mcpStateProvider: (() -> McpServerUiState)? = null

    private val environment = McpToolEnvironment(
        appContext = appContext,
        shizukuApiUtils = shizukuApiUtils,
        appVersionName = appVersionName,
        appVersionCode = appVersionCode,
        appPackageName = appPackageName,
        appLabel = appLabel,
        stateProvider = { mcpStateProvider?.invoke() }
    )
    private val runtimeTools = McpRuntimeTools(environment)
    private val skillContent = McpSkillContent(environment, runtimeTools::buildRuntimeConfigJson)
    private val homeTools = McpHomeTools(environment)
    private val systemOsTools = McpSystemOsTools(environment)
    private val githubTools = McpGitHubTools(environment)
    private val baTools = McpBaTools(environment)

    private val serverInstructions: String by lazy {
        skillContent.buildServerInstructions()
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
        return skillContent.loadSkillMarkdown()
    }

    fun listLocalTools(): List<McpToolMeta> {
        return McpToolCatalog.all
    }

    private fun createServer(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "keios-local-mcp",
                version = environment.appVersionName
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

        runtimeTools.register(server)
        skillContent.registerClawGuideTool(server)
        homeTools.register(server)
        systemOsTools.register(server)
        githubTools.register(server)
        baTools.register(server)
        skillContent.registerResources(server)
        skillContent.registerPrompt(server)
        return server
    }
}
