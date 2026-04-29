package os.kei.feature.github.domain

import org.junit.Test
import os.kei.feature.github.model.GitHubActionsRunWatchState
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubActionsRunTrackerTest {
    @Test
    fun `running workflow run produces pollable tracking plan`() {
        val plan = GitHubActionsRunTracker.buildTrackingPlan(
            run = run(status = "in_progress"),
            nowMillis = 120_000L
        )

        assertEquals(GitHubActionsRunWatchState.Running, plan.state)
        assertTrue(plan.pollable)
        assertEquals(20_000L, plan.nextPollDelayMillis)
        assertEquals("running", plan.reason)
    }

    @Test
    fun `completed workflow run stops polling`() {
        val plan = GitHubActionsRunTracker.buildTrackingPlan(
            run = run(status = "completed", conclusion = "success"),
            nowMillis = 120_000L
        )

        assertEquals(GitHubActionsRunWatchState.Completed, plan.state)
        assertFalse(plan.pollable)
        assertEquals("finished", plan.reason)
    }

    @Test
    fun `old running workflow run becomes stale`() {
        val plan = GitHubActionsRunTracker.buildTrackingPlan(
            run = run(status = "in_progress", startedAt = 0L),
            nowMillis = 4L * 60L * 60L * 1000L
        )

        assertEquals(GitHubActionsRunWatchState.Running, plan.state)
        assertFalse(plan.pollable)
        assertEquals("stale", plan.reason)
    }

    private fun run(
        status: String,
        conclusion: String = "",
        startedAt: Long = 60_000L
    ): GitHubActionsWorkflowRun {
        return GitHubActionsWorkflowRun(
            id = 42L,
            status = status,
            conclusion = conclusion,
            runStartedAtMillis = startedAt,
            createdAtMillis = startedAt
        )
    }
}
