package os.kei.mcp.server

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.core.privilege.PrivilegedShell
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.coroutines.ContinuationInterceptor

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class McpToolExecutionTest {
    // Virtual time: the CacheRead profile's real 4-second budget would otherwise be spent waiting.
    @Test
    fun timeoutReturnsToolErrorResult() = runTest {
        val logs = mutableListOf<String>()
        val result = executeMcpTextTool(
            environment = testEnvironment(logs),
            name = "test.timeout",
            profile = McpToolExecutionProfile.CacheRead,
            outputContract = McpToolOutputContract.KeyValueText,
            structuredOutputEnabled = false,
            request = CallToolRequest(CallToolRequestParams(name = "test.timeout"))
        ) {
            delay(4_500)
            "late"
        }

        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text
        assertTrue(text.contains("timeout_4000ms"))
        assertTrue(logs.any { it.contains("test.timeout:false") })
    }

    @Test
    fun businessErrorTextReturnsToolErrorResult() = runBlocking {
        val result = executeMcpTextTool(
            environment = testEnvironment(mutableListOf()),
            name = "test.business",
            profile = McpToolExecutionProfile.CacheRead,
            outputContract = McpToolOutputContract.KeyValueText,
            structuredOutputEnabled = false,
            request = CallToolRequest(CallToolRequestParams(name = "test.business"))
        ) {
            "ok=false\nmessage=query_required"
        }

        assertEquals(true, result.isError)
    }

    @Test
    fun businessErrorDetectorCoversKnownToolErrorShapes() {
        assertEquals(true, McpToolBusinessErrors.isBusinessError("target=unknown\nmessage=no_target"))
        assertEquals(true, McpToolBusinessErrors.isBusinessError("hasTarget=false\nmessage=missing"))
        assertEquals(true, McpToolBusinessErrors.isBusinessError("ok=true\nmessage=repoUrls_required"))
        assertEquals(false, McpToolBusinessErrors.isBusinessError("ok=true\nmessage=ready"))
    }

    @Test
    fun ioProfilesKeepTheCallerDispatcherAcrossSuspension() = runTest {
        val callerDispatcher = currentCoroutineContext()[ContinuationInterceptor]
        McpToolExecutionProfile.entries.filterNot { it == McpToolExecutionProfile.Cpu }.forEach { profile ->
            val result = executeMcpTextTool(
                environment = testEnvironment(mutableListOf()),
                name = "test.dispatch.${profile.name}",
                profile = profile,
                outputContract = McpToolOutputContract.KeyValueText,
                structuredOutputEnabled = false,
                request = CallToolRequest(CallToolRequestParams(name = "test.dispatch.${profile.name}")),
            ) {
                assertSame(callerDispatcher, currentCoroutineContext()[ContinuationInterceptor], profile.name)
                delay(1)
                assertSame(callerDispatcher, currentCoroutineContext()[ContinuationInterceptor], profile.name)
                "ok=true"
            }

            assertEquals("ok=true", (result.content.single() as TextContent).text, profile.name)
        }
    }

    @Test
    fun cpuProfileUsesDefaultAndReturnsToTheCallerDispatcher() = runBlocking {
        val callerDispatcher = currentCoroutineContext()[ContinuationInterceptor]
        val result = executeMcpTextTool(
            environment = testEnvironment(mutableListOf()),
            name = "test.dispatch.cpu",
            profile = McpToolExecutionProfile.Cpu,
            outputContract = McpToolOutputContract.KeyValueText,
            structuredOutputEnabled = false,
            request = CallToolRequest(CallToolRequestParams(name = "test.dispatch.cpu")),
        ) {
            assertSame(Dispatchers.Default, currentCoroutineContext()[ContinuationInterceptor])
            "ok=true"
        }

        assertEquals("ok=true", (result.content.single() as TextContent).text)
        assertSame(callerDispatcher, currentCoroutineContext()[ContinuationInterceptor])
    }

    private fun testEnvironment(logs: MutableList<String>): McpToolEnvironment {
        return McpToolEnvironment(
            appContext = ApplicationProvider.getApplicationContext(),
            privilegedShell = PrivilegedShell(),
            appVersionName = "test",
            appVersionCode = 1L,
            appPackageName = "os.kei.test",
            appLabel = "KeiOS",
            stateProvider = { McpServerUiState(authToken = "token") },
            toolCallLogger = { name, _, _, success, _ ->
                logs += "$name:$success"
            }
        )
    }
}
