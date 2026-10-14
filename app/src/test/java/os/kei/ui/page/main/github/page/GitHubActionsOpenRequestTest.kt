package os.kei.ui.page.main.github.page

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubActionsOpenRequestTest {
    @Test
    fun `owner activation and replay cannot close the newly opened actions request`() {
        var consumedNavigation = 5
        var consumedActions = 2
        var visible = true
        var dismissCount = 0
        var openCount = 0
        for (ownerActive in listOf(false, true, true, true)) {
            if (shouldDismissGitHubActionsForNavigation(6, consumedNavigation)) {
                consumedNavigation = 6
                visible = false
                dismissCount++
            }
            if (canConsumeGitHubActionsOpenRequest(3, consumedActions, ownerActive, true, "owner/repo|app")) {
                consumedActions = 3
                visible = true
                openCount++
            }
        }

        assertTrue(visible)
        assertEquals(1, dismissCount)
        assertEquals(1, openCount)
    }

    @Test
    fun `notification and history requests each open once even when counters match`() {
        var consumed = 0
        val opened = mutableListOf<Int>()
        for (token in listOf(1, 1, -1, -1, 2, 2, -2, -2)) {
            if (canConsumeGitHubActionsOpenRequest(token, consumed, true, true, "owner/repo|app")) {
                opened += token
                consumed = token
            }
        }

        assertEquals(listOf(1, -1, 2, -2), opened)
    }

    @Test
    fun `pending request waits for its visible owner and content`() {
        assertFalse(canConsumeGitHubActionsOpenRequest(1, 0, false, true, "owner/repo|app"))
        assertFalse(canConsumeGitHubActionsOpenRequest(1, 0, true, false, "owner/repo|app"))
        assertTrue(canConsumeGitHubActionsOpenRequest(1, 0, true, true, "owner/repo|app"))
    }

    @Test
    fun `cleared and absent requests cannot reopen an old sheet`() {
        assertFalse(canConsumeGitHubActionsOpenRequest(1, 0, true, true, null))
        assertFalse(canConsumeGitHubActionsOpenRequest(0, 0, true, true, "owner/repo|app"))
    }
}
