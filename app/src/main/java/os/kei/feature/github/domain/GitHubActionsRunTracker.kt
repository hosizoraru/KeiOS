package os.kei.feature.github.domain

import os.kei.feature.github.model.GitHubActionsRunTrackingPlan
import os.kei.feature.github.model.GitHubActionsRunWatchState
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import java.util.Locale

object GitHubActionsRunTracker {
    fun buildTrackingPlan(
        run: GitHubActionsWorkflowRun,
        nowMillis: Long = System.currentTimeMillis()
    ): GitHubActionsRunTrackingPlan {
        val status = run.status.trim().lowercase(Locale.ROOT)
        val conclusion = run.conclusion.trim().lowercase(Locale.ROOT)
        val state = when {
            status in queuedStatuses -> GitHubActionsRunWatchState.Queued
            status == "in_progress" -> GitHubActionsRunWatchState.Running
            status == "completed" && conclusion == "success" -> GitHubActionsRunWatchState.Completed
            status == "completed" -> GitHubActionsRunWatchState.Failed
            status.isBlank() -> GitHubActionsRunWatchState.Unknown
            else -> GitHubActionsRunWatchState.Unknown
        }
        val startedAt = run.runStartedAtMillis ?: run.createdAtMillis ?: nowMillis
        val ageMillis = (nowMillis - startedAt).coerceAtLeast(0L)
        val pollable = state == GitHubActionsRunWatchState.Queued ||
            state == GitHubActionsRunWatchState.Running ||
            state == GitHubActionsRunWatchState.Unknown
        val nextDelay = when (state) {
            GitHubActionsRunWatchState.Queued -> 15_000L
            GitHubActionsRunWatchState.Running -> 20_000L
            GitHubActionsRunWatchState.Unknown -> 30_000L
            GitHubActionsRunWatchState.Completed,
            GitHubActionsRunWatchState.Failed -> 0L
        }
        val staleAfter = when (state) {
            GitHubActionsRunWatchState.Queued -> 45L * minuteMillis
            GitHubActionsRunWatchState.Running -> 3L * hourMillis
            GitHubActionsRunWatchState.Unknown -> 30L * minuteMillis
            GitHubActionsRunWatchState.Completed,
            GitHubActionsRunWatchState.Failed -> 0L
        }
        val reason = when {
            !pollable -> "finished"
            staleAfter > 0L && ageMillis >= staleAfter -> "stale"
            state == GitHubActionsRunWatchState.Queued -> "queued"
            state == GitHubActionsRunWatchState.Running -> "running"
            else -> "unknown-status"
        }
        return GitHubActionsRunTrackingPlan(
            runId = run.id,
            state = state,
            status = run.status,
            conclusion = run.conclusion,
            pollable = pollable && reason != "stale",
            nextPollDelayMillis = nextDelay,
            staleAfterMillis = staleAfter,
            reason = reason
        )
    }

    private val queuedStatuses = setOf("queued", "requested", "waiting", "pending")
    private const val minuteMillis = 60L * 1000L
    private const val hourMillis = 60L * minuteMillis
}
