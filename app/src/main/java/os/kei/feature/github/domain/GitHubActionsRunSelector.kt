package os.kei.feature.github.domain

import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsRunBranchTrust
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubActionsRunSelectionOptions
import os.kei.feature.github.model.GitHubActionsRunTraits
import os.kei.feature.github.model.GitHubActionsWorkflowKind
import os.kei.feature.github.model.GitHubActionsWorkflowTraits
import java.util.Locale

object GitHubActionsRunSelector {
    fun inspectRun(
        runArtifacts: GitHubActionsRunArtifacts,
        defaultBranch: String = "",
        workflowTraits: GitHubActionsWorkflowTraits? = null
    ): GitHubActionsRunTraits {
        val run = runArtifacts.run
        val normalizedBranch = normalize(run.headBranch)
        val normalizedDefaultBranch = normalize(defaultBranch)
        val normalizedEvent = normalize(run.event)
        val normalizedStatus = normalize(run.status)
        val normalizedConclusion = normalize(run.conclusion)
        val defaultBranchMatched = normalizedDefaultBranch.isNotBlank() &&
            normalizedBranch == normalizedDefaultBranch
        val releaseTag = looksLikeReleaseTag(normalizedBranch)
        val releaseBranch = looksLikeReleaseBranch(normalizedBranch)
        val mainlineBranch = normalizedDefaultBranch.isBlank() && normalizedBranch in commonMainlineBranches
        val pullRequestByBranchOrEvent = normalizedEvent in pullRequestEvents ||
            normalizedBranch.startsWith("refs/pull/") ||
            normalizedBranch.startsWith("pull/")
        val pullRequestByAssociation = run.pullRequestCount > 0 &&
            !defaultBranchMatched &&
            !mainlineBranch &&
            !releaseTag &&
            normalizedEvent !in trustedBranchEvents
        val pullRequestLike = pullRequestByBranchOrEvent || pullRequestByAssociation
        val forkLike = run.headRepositoryFork ||
            (
                run.repositoryFullName.isNotBlank() &&
                    run.headRepositoryFullName.isNotBlank() &&
                    !run.repositoryFullName.equals(run.headRepositoryFullName, ignoreCase = true)
                )
        val branchTrust = when {
            pullRequestLike -> GitHubActionsRunBranchTrust.PullRequest
            defaultBranchMatched -> GitHubActionsRunBranchTrust.DefaultBranch
            releaseTag -> GitHubActionsRunBranchTrust.ReleaseTag
            mainlineBranch -> GitHubActionsRunBranchTrust.MainlineBranch
            releaseBranch -> GitHubActionsRunBranchTrust.ReleaseBranch
            normalizedBranch.isNotBlank() -> GitHubActionsRunBranchTrust.FeatureBranch
            else -> GitHubActionsRunBranchTrust.Unknown
        }
        val completed = normalizedStatus == "completed"
        val successful = normalizedConclusion == "success"
        val inProgress = normalizedStatus in inProgressStatuses
        val releaseWorkflow = workflowTraits?.kind == GitHubActionsWorkflowKind.Release ||
            workflowTraits?.releaseLike == true
        val trustedBranch = when (branchTrust) {
            GitHubActionsRunBranchTrust.DefaultBranch,
            GitHubActionsRunBranchTrust.MainlineBranch -> true
            GitHubActionsRunBranchTrust.ReleaseTag -> releaseWorkflow || normalizedEvent in releaseEvents
            GitHubActionsRunBranchTrust.ReleaseBranch -> releaseWorkflow
            GitHubActionsRunBranchTrust.PullRequest,
            GitHubActionsRunBranchTrust.FeatureBranch,
            GitHubActionsRunBranchTrust.Unknown -> false
        }
        return GitHubActionsRunTraits(
            normalizedBranch = normalizedBranch,
            normalizedEvent = normalizedEvent,
            normalizedStatus = normalizedStatus,
            normalizedConclusion = normalizedConclusion,
            branchTrust = branchTrust,
            completed = completed,
            successful = successful,
            inProgress = inProgress,
            pullRequestLike = pullRequestLike,
            forkLike = forkLike,
            defaultBranch = defaultBranchMatched,
            releaseTag = releaseTag,
            releaseBranch = releaseBranch,
            safeForRecommendation = completed && successful && trustedBranch && !pullRequestLike && !forkLike
        )
    }

    fun selectRuns(
        runs: List<GitHubActionsRunArtifacts>,
        options: GitHubActionsRunSelectionOptions = GitHubActionsRunSelectionOptions(),
        workflowTraits: GitHubActionsWorkflowTraits? = null
    ): List<GitHubActionsRunMatch> {
        return runs
            .asSequence()
            .mapNotNull { runArtifacts -> matchRun(runArtifacts, options, workflowTraits) }
            .sortedWith(
                compareByDescending<GitHubActionsRunMatch> { it.score }
                    .thenByDescending { it.runArtifacts.run.createdAtMillis ?: Long.MIN_VALUE }
                    .thenByDescending { it.runArtifacts.run.id }
            )
            .toList()
    }

    fun matchRun(
        runArtifacts: GitHubActionsRunArtifacts,
        options: GitHubActionsRunSelectionOptions = GitHubActionsRunSelectionOptions(),
        workflowTraits: GitHubActionsWorkflowTraits? = null
    ): GitHubActionsRunMatch? {
        val traits = inspectRun(
            runArtifacts = runArtifacts,
            defaultBranch = options.defaultBranch,
            workflowTraits = workflowTraits
        )
        if (!options.includePullRequests && traits.pullRequestLike) return null
        if (!options.includeNonDefaultBranches && !traits.defaultBranch && !traits.releaseTag) return null
        if (!options.includeUnsuccessful && traits.completed && !traits.successful) return null

        val downloadHistory = options.downloadHistory + options.artifactOptions.downloadHistory
        val artifactMatches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = runArtifacts.artifacts,
            options = options.artifactOptions.copy(downloadHistory = downloadHistory)
        )
        if (options.requireArtifacts && artifactMatches.isEmpty()) return null
        if (options.requireAndroidArtifacts && artifactMatches.none { it.traits.androidLike }) return null

        val normalizedPreferredBranches = options.preferredBranches
            .map(::normalize)
            .filter { it.isNotBlank() }
            .toSet()
        val normalizedPreferredEvents = options.preferredEvents
            .map(::normalize)
            .filter { it.isNotBlank() }
            .toSet()
        val reasons = mutableListOf<String>()
        var score = 0

        if (traits.completed) {
            score += 32
            reasons += "completed"
        } else if (traits.inProgress) {
            score -= 16
            reasons += "in-progress"
        }
        if (traits.successful) {
            score += 40
            reasons += "success"
        }
        when (traits.branchTrust) {
            GitHubActionsRunBranchTrust.DefaultBranch -> {
                score += 48
                reasons += "default-branch"
            }
            GitHubActionsRunBranchTrust.ReleaseTag -> {
                score += 42
                reasons += "release-tag"
            }
            GitHubActionsRunBranchTrust.MainlineBranch -> {
                score += 30
                reasons += "mainline-branch"
            }
            GitHubActionsRunBranchTrust.ReleaseBranch -> {
                score += 20
                reasons += "release-branch"
            }
            GitHubActionsRunBranchTrust.PullRequest -> {
                score -= 80
                reasons += "pull-request"
            }
            GitHubActionsRunBranchTrust.FeatureBranch -> {
                score -= 24
                reasons += "feature-branch"
            }
            GitHubActionsRunBranchTrust.Unknown -> score -= 8
        }
        if (traits.forkLike) {
            score -= 28
            reasons += "fork"
        }
        if (traits.normalizedBranch in normalizedPreferredBranches) {
            score += 26
            reasons += "preferred-branch"
        }
        if (traits.normalizedEvent in normalizedPreferredEvents) {
            score += 18
            reasons += traits.normalizedEvent
        } else if (traits.normalizedEvent in secondaryEvents) {
            score += 6
            reasons += traits.normalizedEvent
        } else if (traits.normalizedEvent in pullRequestEvents) {
            score -= 34
        }
        if (traits.safeForRecommendation) {
            score += 24
            reasons += "safe"
        }
        val lastDownload = GitHubActionsDownloadHistoryMatcher.latestForRun(
            run = runArtifacts.run,
            history = downloadHistory
        )
        if (lastDownload != null) {
            score += 68
            score += GitHubActionsDownloadHistoryMatcher.recencyScore(lastDownload)
            reasons += "last-downloaded"
        }

        val bestArtifactScore = artifactMatches.maxOfOrNull { it.score } ?: 0
        val androidArtifactCount = artifactMatches.count { it.traits.androidLike }
        score += bestArtifactScore.coerceAtMost(140)
        score += androidArtifactCount.coerceAtMost(8) * 3
        if (artifactMatches.any { it.traits.debugLike }) {
            score -= 4
        }

        return GitHubActionsRunMatch(
            runArtifacts = runArtifacts,
            traits = traits,
            artifactMatches = artifactMatches,
            score = score,
            lastDownload = lastDownload,
            reasons = reasons
        )
    }

    private fun looksLikeReleaseTag(branch: String): Boolean {
        val value = branch.removePrefix("refs/tags/")
        return releaseTagRegex.matches(value)
    }

    private fun looksLikeReleaseBranch(branch: String): Boolean {
        return branch.startsWith("release/") ||
            branch.startsWith("releases/") ||
            branch.startsWith("stable/") ||
            branch.startsWith("hotfix/")
    }

    private fun normalize(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private val releaseTagRegex = Regex(
        """v?\d+(?:\.\d+){1,3}(?:[-+._][0-9a-z][0-9a-z._-]*)?""",
        RegexOption.IGNORE_CASE
    )
    private val commonMainlineBranches = setOf("main", "master", "trunk")
    private val pullRequestEvents = setOf("pull_request", "pull_request_target")
    private val releaseEvents = setOf("release", "workflow_dispatch", "push")
    private val trustedBranchEvents = setOf("push", "workflow_dispatch", "release", "schedule")
    private val secondaryEvents = setOf("schedule", "repository_dispatch", "workflow_run", "merge_group")
    private val inProgressStatuses = setOf("queued", "requested", "waiting", "pending", "in_progress")
}
