package os.kei.mcp.server

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import os.kei.core.system.ShizukuApiUtils

internal class McpRuntimeTools(
    private val environment: McpToolEnvironment
) {
    private val appVersionName: String get() = environment.appVersionName
    private val appVersionCode: Long get() = environment.appVersionCode
    private val appPackageName: String get() = environment.appPackageName
    private val appLabel: String get() = environment.appLabel

    fun register(server: Server) {
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
            callText(environment.shizukuApiUtils.currentStatus())
        }

        server.addTool(
            name = "keios.mcp.runtime.status",
            description = "Get MCP runtime status from app state.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildRuntimeStatusText(environment.currentState()))
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
            callText(buildRuntimeLogsText(environment.currentState(), limit))
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
            val state = environment.currentState()
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
    }

    fun buildRuntimeConfigJson(
        state: McpServerUiState?,
        mode: String,
        endpointOverride: String,
        serverNameOverride: String
    ): String {
        val fixedMode = normalizeMcpConfigMode(mode)
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
}
