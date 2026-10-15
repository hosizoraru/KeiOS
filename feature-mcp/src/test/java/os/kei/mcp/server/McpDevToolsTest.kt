package os.kei.mcp.server

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.core.privilege.PrivilegedShell
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class McpDevToolsTest {
    @Test
    fun developmentGuidePointsToExistingFilesAndCoversTheirTestModules() {
        val tools = McpDevTools(
            environment = McpToolEnvironment(
                appContext = ApplicationProvider.getApplicationContext(),
                privilegedShell = PrivilegedShell(),
                appVersionName = "test",
                appVersionCode = 1L,
                appPackageName = "os.kei.test",
                appLabel = "KeiOS",
                stateProvider = { null },
                toolCallLogger = { _, _, _, _, _ -> },
            ),
            runtimeConfigBuilder = { _, _, _, _ -> "{}" },
        )
        val snapshot = tools.buildProjectSnapshot().lineSequence()
            .filter { '=' in it }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val sourceFiles = snapshot.getValue("keySourceFiles").split(',')
        val testFiles = snapshot.getValue("keyTestFiles").split(',')
        assertTrue(sourceFiles.isNotEmpty() && testFiles.isNotEmpty())
        (sourceFiles + testFiles).forEach { path ->
            assertTrue(File(root, path).isFile, "Development guide points to missing file: $path")
        }

        val command = snapshot.getValue("mcpTests")
        val modules = Regex(":([\\w-]+):testDebugUnitTest").findAll(command)
            .map { it.groupValues[1] }.toSet()
        assertEquals(testFiles.map { it.substringBefore('/') }.toSet(), modules)
        assertTrue(command in tools.buildValidationPlan("mcp"))
    }
}
