package com.example.keios.mcp

import com.example.keios.ui.utils.ShizukuApiUtils
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject

class LocalMcpService(
    private val shizukuApiUtils: ShizukuApiUtils,
    private val appVersionName: String,
    private val appVersionCode: Long
) {
    @Volatile
    private var cachedServer: Server? = null

    fun getOrCreateServer(): Server {
        cachedServer?.let { return it }
        val created = createServer()
        cachedServer = created
        return created
    }

    fun listLocalToolNames(): List<String> {
        return listOf(
            "keios.get_shizuku_status",
            "keios.get_app_version"
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
            name = "keios.get_shizuku_status",
            description = "Get current Shizuku status from KeiOS app.",
            inputSchema = ToolSchema(
                properties = buildJsonObject { }
            )
        ) {
            CallToolResult(content = listOf(TextContent(shizukuApiUtils.currentStatus())))
        }

        server.addTool(
            name = "keios.get_app_version",
            description = "Get KeiOS app version name and version code.",
            inputSchema = ToolSchema(
                properties = buildJsonObject { }
            )
        ) {
            val text = "versionName=$appVersionName, versionCode=$appVersionCode"
            CallToolResult(content = listOf(TextContent(text)))
        }

        return server
    }
}
