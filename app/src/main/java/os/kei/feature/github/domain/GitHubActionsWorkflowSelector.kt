package os.kei.feature.github.domain

import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactSignal
import os.kei.feature.github.model.GitHubActionsWorkflowKind
import os.kei.feature.github.model.GitHubActionsWorkflowMatch
import os.kei.feature.github.model.GitHubActionsWorkflowSelectionOptions
import os.kei.feature.github.model.GitHubActionsWorkflowTraits
import os.kei.feature.github.model.GitHubActionsRunBranchTrust
import os.kei.feature.github.model.GitHubActionsRunSelectionOptions
import java.util.Locale

object GitHubActionsWorkflowSelector {
    fun inspectWorkflow(workflow: GitHubActionsWorkflow): GitHubActionsWorkflowTraits {
        val normalizedName = workflow.name.trim().lowercase(Locale.ROOT)
        val normalizedPath = workflow.path.trim().lowercase(Locale.ROOT)
        val fileName = normalizedPath.substringAfterLast('/')
        val searchable = "$normalizedName $normalizedPath"
        val dynamic = normalizedPath.startsWith("dynamic/")
        val androidLike = containsAny(searchable, "android", "apk", "gradle", "kmp", "app")
        val buildLike = containsAny(searchable, "build", "artifact", "ci", "assemble")
        val releaseLike = containsAny(searchable, "release", "publish")
        val nightlyLike = containsAny(searchable, "nightly", "preview", "dev", "alpha", "unstable")
        val maintenanceLike = dynamic ||
            containsAny(searchable, "dependabot", "copilot", "codeql", "crowdin", "translation", "dependency")
        val documentationLike = containsAny(searchable, "docs", "documentation", "pages")
        val qualityLike = containsAny(searchable, "lint", "test", "check", "codeql")
        val kind = when {
            maintenanceLike && containsAny(searchable, "translation", "crowdin") -> GitHubActionsWorkflowKind.Localization
            maintenanceLike && containsAny(searchable, "dependabot", "dependency") -> GitHubActionsWorkflowKind.Dependency
            maintenanceLike -> GitHubActionsWorkflowKind.Automation
            documentationLike -> GitHubActionsWorkflowKind.Documentation
            qualityLike && !buildLike -> GitHubActionsWorkflowKind.Quality
            androidLike && buildLike -> GitHubActionsWorkflowKind.AndroidBuild
            releaseLike -> GitHubActionsWorkflowKind.Release
            nightlyLike -> GitHubActionsWorkflowKind.Nightly
            buildLike -> GitHubActionsWorkflowKind.Ci
            else -> GitHubActionsWorkflowKind.Unknown
        }
        return GitHubActionsWorkflowTraits(
            normalizedName = normalizedName,
            normalizedPath = normalizedPath,
            fileName = fileName,
            kind = kind,
            active = workflow.state.equals("active", ignoreCase = true),
            dynamic = dynamic,
            androidLike = androidLike,
            buildLike = buildLike,
            releaseLike = releaseLike,
            nightlyLike = nightlyLike,
            maintenanceLike = maintenanceLike || documentationLike || qualityLike
        )
    }

    fun buildArtifactSignal(
        workflow: GitHubActionsWorkflow,
        runs: List<GitHubActionsRunArtifacts>,
        defaultBranch: String = ""
    ): GitHubActionsWorkflowArtifactSignal {
        val workflowTraits = inspectWorkflow(workflow)
        val artifacts = runs.flatMap { it.artifacts }
        val androidArtifactCount = artifacts.count { artifact ->
            GitHubActionsArtifactSelector.inspectName(artifact.name).androidLike
        }
        val runTraits = runs.map { runArtifacts ->
            GitHubActionsRunSelector.inspectRun(
                runArtifacts = runArtifacts,
                defaultBranch = defaultBranch,
                workflowTraits = workflowTraits
            )
        }
        val runMatches = GitHubActionsRunSelector.selectRuns(
            runs = runs,
            workflowTraits = workflowTraits,
            options = GitHubActionsRunSelectionOptions(defaultBranch = defaultBranch)
        )
        val recommendedRun = runMatches.firstOrNull { it.traits.safeForRecommendation }
        return GitHubActionsWorkflowArtifactSignal(
            workflowId = workflow.id,
            recentRunCount = runs.size,
            successfulRunCount = runs.count { it.run.conclusion.equals("success", ignoreCase = true) },
            artifactCount = artifacts.size,
            nonExpiredArtifactCount = artifacts.count { !it.expired },
            androidArtifactCount = androidArtifactCount,
            trustedRunCount = runTraits.count { it.safeForRecommendation },
            defaultBranchRunCount = runTraits.count { it.branchTrust == GitHubActionsRunBranchTrust.DefaultBranch },
            releaseTagRunCount = runTraits.count { it.branchTrust == GitHubActionsRunBranchTrust.ReleaseTag },
            pullRequestRunCount = runTraits.count { it.pullRequestLike },
            recommendedRunId = recommendedRun?.runArtifacts?.run?.id,
            recommendedRunBranch = recommendedRun?.runArtifacts?.run?.headBranch.orEmpty(),
            recommendedRunEvent = recommendedRun?.runArtifacts?.run?.event.orEmpty(),
            recommendedArtifactCount = recommendedRun?.artifactMatches?.size ?: 0,
            branchRunCounts = runs
                .map { it.run.headBranch.trim() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount(),
            artifactNames = artifacts.map { it.name }.distinct()
        )
    }

    fun selectWorkflows(
        workflows: List<GitHubActionsWorkflow>,
        artifactSignals: Map<Long, GitHubActionsWorkflowArtifactSignal> = emptyMap(),
        options: GitHubActionsWorkflowSelectionOptions = GitHubActionsWorkflowSelectionOptions()
    ): List<GitHubActionsWorkflowMatch> {
        return workflows
            .asSequence()
            .mapNotNull { workflow -> matchWorkflow(workflow, artifactSignals[workflow.id], options) }
            .sortedWith(
                compareByDescending<GitHubActionsWorkflowMatch> { it.score }
                    .thenBy { it.workflow.name.lowercase(Locale.ROOT) }
                    .thenBy { it.workflow.path.lowercase(Locale.ROOT) }
            )
            .toList()
    }

    fun matchWorkflow(
        workflow: GitHubActionsWorkflow,
        signal: GitHubActionsWorkflowArtifactSignal? = null,
        options: GitHubActionsWorkflowSelectionOptions = GitHubActionsWorkflowSelectionOptions()
    ): GitHubActionsWorkflowMatch? {
        val traits = inspectWorkflow(workflow)
        if (!options.includeDisabled && !traits.active) return null
        if (options.requireArtifacts && (signal?.nonExpiredArtifactCount ?: 0) <= 0) return null
        if (!matchesQuery("${traits.normalizedName} ${traits.normalizedPath}", options.query)) return null

        val normalizedPreferredPaths = options.preferredWorkflowPaths
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .toSet()
        val preferred = workflow.id in options.preferredWorkflowIds ||
            traits.normalizedPath in normalizedPreferredPaths ||
            traits.fileName in normalizedPreferredPaths
        val reasons = mutableListOf<String>()
        var score = 0
        if (preferred) {
            score += 80
            reasons += "preferred"
        }
        if (traits.active) score += 12
        when (traits.kind) {
            GitHubActionsWorkflowKind.AndroidBuild -> {
                score += 50
                reasons += "android-build"
            }
            GitHubActionsWorkflowKind.Release -> {
                score += 38
                reasons += "release"
            }
            GitHubActionsWorkflowKind.Nightly -> {
                score += 34
                reasons += "nightly"
            }
            GitHubActionsWorkflowKind.Ci -> {
                score += 28
                reasons += "ci"
            }
            GitHubActionsWorkflowKind.Quality -> score -= 24
            GitHubActionsWorkflowKind.Localization -> score -= 30
            GitHubActionsWorkflowKind.Dependency -> score -= 34
            GitHubActionsWorkflowKind.Documentation -> score -= 26
            GitHubActionsWorkflowKind.Automation -> score -= 32
            GitHubActionsWorkflowKind.Unknown -> Unit
        }
        if (traits.releaseLike) score += 12
        if (traits.nightlyLike) score += 8
        if (traits.buildLike && traits.nightlyLike) {
            score += 20
            reasons += "nightly-build"
        }
        if (traits.maintenanceLike) score -= 20
        val lastDownload = GitHubActionsDownloadHistoryMatcher.latestForWorkflow(
            workflow = workflow,
            history = options.downloadHistory
        )
        if (lastDownload != null) {
            score += 38
            score += GitHubActionsDownloadHistoryMatcher.recencyScore(lastDownload)
            reasons += "last-downloaded"
        }

        signal?.let {
            if (it.recentRunCount > 0) score += 4
            if (it.successfulRunCount > 0) score += 8
            if (it.nonExpiredArtifactCount > 0) {
                score += 32
                reasons += "artifacts"
            }
            if (it.androidArtifactCount > 0) {
                score += 24
                reasons += "android-artifacts"
            }
            if (it.recommendedRunId != null) {
                score += 28
                reasons += "recommended-run"
            }
            if (it.trustedRunCount > 0) {
                score += 18
                reasons += "trusted-run"
            }
            if (it.defaultBranchRunCount > 0) {
                score += 16
                reasons += "default-branch"
            }
            if (it.releaseTagRunCount > 0 && traits.releaseLike) {
                score += 12
                reasons += "release-tag"
            }
            if (it.pullRequestRunCount > 0 && it.trustedRunCount == 0) {
                score -= 18
                reasons += "pull-request-only"
            }
            if (it.artifactCount > it.nonExpiredArtifactCount) {
                score -= 4
                reasons += "expired-artifacts"
            }
        }

        return GitHubActionsWorkflowMatch(
            workflow = workflow,
            traits = traits,
            signal = signal,
            score = score,
            lastDownload = lastDownload,
            reasons = reasons
        )
    }

    private fun matchesQuery(value: String, query: String): Boolean {
        val tokens = query
            .trim()
            .lowercase(Locale.ROOT)
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true
        return tokens.all { value.contains(it) }
    }

    private fun containsAny(value: String, vararg needles: String): Boolean {
        return needles.any { value.contains(it, ignoreCase = true) }
    }
}
