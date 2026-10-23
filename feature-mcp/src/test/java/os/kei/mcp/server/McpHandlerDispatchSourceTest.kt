package os.kei.mcp.server

import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The MCP server must have exactly one place where handler concurrency is capped.
 *
 * kotlin-sdk 0.15.0 stopped processing inbound messages serially on the transport read loop and began
 * dispatching handlers concurrently onto `ProtocolOptions.handlerCoroutineContext`. Both halves of
 * that have to be honoured here, and each one is silent when it is wrong:
 *
 * 1. **The context has to be pinned.** Left at its `Dispatchers.Default` default, every MCP tool —
 *    overwhelmingly network and disk work — runs on the CPU pool.
 * 2. **Tools must not hop again after arriving.** Each profile used to carry
 *    `AppDispatchers.mcpServer` and `executeMcpTextTool` hopped onto it unconditionally, which was
 *    correct while the SDK was serial and became a second, *narrower* serialization point once it was
 *    not: four threads shared by the 30s network and 60s deep-scan profiles, with 4-second cache
 *    reads queued behind them.
 *
 * The service wiring stays guarded here; [McpToolExecutionTest] checks the actual execution context.
 */
class McpHandlerDispatchSourceTest {
    @Test
    fun theServerPinsItsHandlerCoroutineContext() {
        val source = sourceFile(LOCAL_SERVICE)

        assertTrue(
            "handlerCoroutineContext = AppDispatchers.mcpServer" in source,
            "$LOCAL_SERVICE must pin handlerCoroutineContext, or every MCP tool runs on the CPU pool",
        )
    }

    /**
     * Progress is opt-in from both sides, and the long profiles are the ones that need it.
     */
    @Test
    fun theLongProfilesReportProgress() {
        val reporting =
            McpToolExecutionProfile.entries
                .filter { profile -> profile.progressInterval != null }
                .toSet()

        assertTrue(
            reporting == setOf(McpToolExecutionProfile.Network, McpToolExecutionProfile.DeepScan),
            "The 30s and 60s profiles are the ones worth reporting on. Found: $reporting",
        )
        reporting.forEach { profile ->
            assertTrue(
                profile.progressInterval!! < profile.timeout,
                "${profile.name} would time out before emitting a single progress notification",
            )
        }
    }

}

private const val LOCAL_SERVICE = "feature-mcp/src/main/java/os/kei/mcp/server/LocalMcpService.kt"

private fun repositoryRoot(): File {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val root =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .firstOrNull { directory -> File(directory, "settings.gradle.kts").isFile }
    return requireNotNull(root) { "Unable to locate the repository root from $workingDirectory" }
}

private fun sourceFile(relativePath: String): String =
    File(repositoryRoot(), relativePath).let { file ->
        require(file.isFile) { "Unable to locate $relativePath" }
        file.readText()
    }
