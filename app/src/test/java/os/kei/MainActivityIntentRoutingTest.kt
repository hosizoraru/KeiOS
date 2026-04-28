package os.kei

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainActivityIntentRoutingTest {
    @Test
    fun `valid github shortcut route is preserved`() {
        val route = MainActivityIntentRouting.sanitize(
            rawTargetBottomPage = MainActivity.TARGET_BOTTOM_PAGE_GITHUB,
            rawMcpServerAction = null,
            rawShortcutAction = MainActivity.SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED
        )

        assertEquals(MainActivity.TARGET_BOTTOM_PAGE_GITHUB, route?.targetBottomPage)
        assertEquals(MainActivity.SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED, route?.shortcutAction)
        assertNull(route?.mcpServerAction)
    }

    @Test
    fun `mismatched shortcut action is dropped`() {
        val route = MainActivityIntentRouting.sanitize(
            rawTargetBottomPage = MainActivity.TARGET_BOTTOM_PAGE_MCP,
            rawMcpServerAction = null,
            rawShortcutAction = MainActivity.SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED
        )

        assertEquals(MainActivity.TARGET_BOTTOM_PAGE_MCP, route?.targetBottomPage)
        assertNull(route?.shortcutAction)
    }

    @Test
    fun `valid mcp action is preserved only on mcp target`() {
        val route = MainActivityIntentRouting.sanitize(
            rawTargetBottomPage = MainActivity.TARGET_BOTTOM_PAGE_MCP,
            rawMcpServerAction = MainActivity.MCP_SERVER_ACTION_TOGGLE,
            rawShortcutAction = null
        )

        assertEquals(MainActivity.TARGET_BOTTOM_PAGE_MCP, route?.targetBottomPage)
        assertEquals(MainActivity.MCP_SERVER_ACTION_TOGGLE, route?.mcpServerAction)
    }

    @Test
    fun `unknown target is rejected`() {
        val route = MainActivityIntentRouting.sanitize(
            rawTargetBottomPage = "Settings",
            rawMcpServerAction = MainActivity.MCP_SERVER_ACTION_TOGGLE,
            rawShortcutAction = MainActivity.SHORTCUT_ACTION_BA_AP_ISLAND
        )

        assertNull(route)
    }
}
