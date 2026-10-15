package os.kei.mcp.server

import io.modelcontextprotocol.kotlin.sdk.server.Server
import java.util.Locale

internal class McpDevTools(
    private val environment: McpToolEnvironment,
    private val runtimeConfigBuilder: (
        state: McpServerUiState?,
        mode: String,
        endpointOverride: String,
        serverNameOverride: String,
    ) -> String,
) {
    fun register(server: Server) {
        server.addMcpTextTool(environment, name = "keios.dev.codex.config") { request ->
            val state = environment.currentState()
            val mode = normalizeMcpConfigMode(argString(request.arguments?.get("mode")))
            val endpoint = argString(request.arguments?.get("endpoint")).trim()
            val serverName = argString(request.arguments?.get("serverName")).trim()
            val tokenEnv =
                argString(request.arguments?.get("tokenEnv"))
                    .trim()
                    .ifBlank { DEFAULT_CODEX_TOKEN_ENV }
            buildCodexConfigGuide(
                state = state,
                mode = mode,
                endpointOverride = endpoint,
                serverNameOverride = serverName,
                tokenEnv = tokenEnv,
            )
        }

        server.addMcpTextTool(environment, name = "keios.dev.project.snapshot") { _ ->
            buildProjectSnapshot()
        }

        server.addMcpTextTool(environment, name = "keios.dev.validation.plan") { request ->
            val scope = argString(request.arguments?.get("scope")).trim().lowercase(Locale.ROOT)
            buildValidationPlan(scope)
        }
    }

    private fun buildCodexConfigGuide(
        state: McpServerUiState?,
        mode: String,
        endpointOverride: String,
        serverNameOverride: String,
        tokenEnv: String,
    ): String {
        val fixedServerName =
            serverNameOverride
                .ifBlank {
                    state?.serverName ?: McpServerDefaults.SERVER_NAME
                }.trim()
                .ifBlank { McpServerDefaults.SERVER_NAME }
        val endpoint =
            endpointOverride.ifBlank {
                when (mode) {
                    "lan" -> state?.lanEndpoints?.firstOrNull()
                    else -> state?.localEndpoint
                } ?: DEFAULT_ENDPOINT
            }
        val token = state?.authToken.orEmpty()
        val configJson = runtimeConfigBuilder(state, mode, endpointOverride, serverNameOverride)
        return buildString {
            appendLine("# KeiOS Codex MCP")
            appendLine()
            appendLine("target=codex")
            appendLine("transport=streamable_http")
            appendLine("serverName=$fixedServerName")
            appendLine("endpoint=$endpoint")
            appendLine("tokenEnv=$tokenEnv")
            appendLine("tokenPresent=${token.isNotBlank()}")
            appendLine("recommendedTools=${McpToolCatalog.devToolNames.joinToString(",")}")
            appendLine()
            appendLine("## Codex CLI")
            appendLine("```bash")
            appendLine("export $tokenEnv=${shellSingleQuote(if (token.isBlank()) "YOUR_TOKEN" else token)}")
            appendLine(
                "codex mcp add ${shellSingleQuote(fixedServerName)} " +
                    "--url ${shellSingleQuote(endpoint)} " +
                    "--bearer-token-env-var $tokenEnv",
            )
            appendLine("```")
            appendLine()
            appendLine("## First Calls")
            appendLine("1. keios.health.ping")
            appendLine("2. keios.dev.project.snapshot")
            appendLine("3. keios.dev.validation.plan(scope=quick)")
            appendLine("4. keios.mcp.runtime.status")
            appendLine()
            appendLine("## Generic Streamable HTTP Config")
            appendLine("```json")
            appendLine(configJson)
            appendLine("```")
        }.trim()
    }

    internal fun buildProjectSnapshot(): String =
        buildString {
            appendLine("appLabel=${environment.appLabel}")
            appendLine("package=${environment.appPackageName}")
            appendLine("versionName=${environment.appVersionName}")
            appendLine("versionCode=${environment.appVersionCode}")
            appendLine("language=kotlin")
            appendLine("ui=jetpack_compose")
            appendLine("state=stateflow")
            appendLine("minSdk=35")
            appendLine("mcpTransport=streamable_http")
            appendLine("mcpEndpoint=${environment.currentState()?.localEndpoint ?: DEFAULT_ENDPOINT}")
            appendLine("mcpDevTools=${McpToolCatalog.devToolNames.joinToString(",")}")
            appendLine("keySourceFiles=${DEV_SOURCE_FILES.joinToString(",")}")
            appendLine("keyTestFiles=${DEV_TEST_FILES.joinToString(",")}")
            appendLine("quickCompile=./gradlew --no-parallel :app:compileDebugKotlin")
            appendLine("mcpTests=$MCP_TEST_COMMAND")
            appendLine("diffCheck=git diff --check")
        }.trim()

    internal fun buildValidationPlan(scope: String): String {
        val normalized =
            when (scope) {
                "mcp", "release", "ui", "baseline" -> scope
                else -> "quick"
            }
        val commands =
            when (normalized) {
                "mcp" -> MCP_VALIDATION_COMMANDS
                "release" -> RELEASE_VALIDATION_COMMANDS
                "ui" -> UI_VALIDATION_COMMANDS
                "baseline" -> BASELINE_VALIDATION_COMMANDS
                else -> QUICK_VALIDATION_COMMANDS
            }
        return buildString {
            appendLine("scope=$normalized")
            appendLine("cwd=repository_root")
            appendLine("destructive=false")
            appendLine("commands=")
            commands.forEachIndexed { index, command ->
                appendLine("${index + 1}. $command")
            }
            appendLine("notes=")
            appendLine("- keep local.properties and signing secrets local")
            appendLine("- use --no-parallel for MCP and release checks when diagnosing flaky output")
            appendLine("- prefer emulator validation for UI and runtime behavior")
        }.trim()
    }

    private fun shellSingleQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"

    private companion object {
        const val DEFAULT_CODEX_TOKEN_ENV = "KEIOS_MCP_TOKEN"
        const val MCP_TEST_COMMAND =
            "./gradlew --no-parallel :feature-mcp:testDebugUnitTest --tests \"os.kei.mcp.server.*\" " +
                ":app:testDebugUnitTest --tests \"os.kei.mcp.server.*\""

        val DEV_SOURCE_FILES =
            listOf(
                "feature-mcp/src/main/java/os/kei/mcp/server/LocalMcpService.kt",
                "feature-mcp/src/main/java/os/kei/mcp/server/McpKtorEndpointHost.kt",
                "feature-mcp/src/main/java/os/kei/mcp/server/McpToolCatalog.kt",
                "feature-mcp/src/main/java/os/kei/mcp/server/McpToolExecution.kt",
                "feature-mcp/src/main/assets/mcp/SKILL.md",
            )

        val DEV_TEST_FILES =
            listOf(
                "app/src/test/java/os/kei/mcp/server/McpToolRegistrationTest.kt",
                "feature-mcp/src/test/java/os/kei/mcp/server/McpToolCatalogLocalizationTest.kt",
                "feature-mcp/src/test/java/os/kei/mcp/server/McpKtorEndpointHostTest.kt",
            )

        val QUICK_VALIDATION_COMMANDS =
            listOf(
                "./gradlew --no-parallel :app:compileDebugKotlin",
                "git diff --check",
            )

        val MCP_VALIDATION_COMMANDS =
            listOf(
                MCP_TEST_COMMAND,
                "./gradlew --no-parallel :app:compileDebugKotlin",
                "git diff --check",
            )

        val RELEASE_VALIDATION_COMMANDS =
            listOf(
                "./gradlew --no-parallel :app:assembleRelease",
                "./gradlew --no-parallel :app:assembleBenchmark",
                "git diff --check",
            )

        val UI_VALIDATION_COMMANDS =
            listOf(
                "./gradlew --no-parallel :app:compileDebugKotlin",
                "use AVD or device screenshot checks for touched screens",
                "git diff --check",
            )

        val BASELINE_VALIDATION_COMMANDS =
            listOf(
                "./gradlew --no-parallel :baselineprofile:generateBaselineProfile",
                "./gradlew --no-parallel :app:assembleRelease",
                "git diff --check",
            )
    }
}
