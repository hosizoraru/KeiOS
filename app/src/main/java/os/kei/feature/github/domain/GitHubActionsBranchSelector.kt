package os.kei.feature.github.domain

import os.kei.feature.github.model.GitHubActionsBranchOption
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactSignal
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactsSnapshot
import java.util.Locale

object GitHubActionsBranchSelector {
    fun buildOptions(
        defaultBranch: String,
        workflow: GitHubActionsWorkflow,
        signal: GitHubActionsWorkflowArtifactSignal? = null,
        snapshot: GitHubActionsWorkflowArtifactsSnapshot? = null
    ): List<GitHubActionsBranchOption> {
        val stats = collectBranchStats(
            defaultBranch = defaultBranch,
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )
        val recommended = recommendBranch(
            defaultBranch = defaultBranch,
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )
        val normalizedRecommended = normalize(recommended)
        return stats.values
            .map { branch ->
                GitHubActionsBranchOption(
                    name = branch.name,
                    runCount = branch.runCount,
                    artifactCount = branch.artifactCount,
                    defaultBranch = branch.defaultBranch,
                    recommended = normalize(branch.name) == normalizedRecommended
                )
            }
            .sortedWith(
                compareByDescending<GitHubActionsBranchOption> { it.recommended }
                    .thenByDescending { it.defaultBranch }
                    .thenByDescending { it.artifactCount }
                    .thenByDescending { it.runCount }
                    .thenByDescending { branchPriority(it.name, defaultBranch) }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
    }

    fun recommendBranch(
        defaultBranch: String,
        workflow: GitHubActionsWorkflow,
        signal: GitHubActionsWorkflowArtifactSignal? = null,
        snapshot: GitHubActionsWorkflowArtifactsSnapshot? = null
    ): String {
        val normalizedDefault = normalize(defaultBranch)
        val stats = collectBranchStats(
            defaultBranch = defaultBranch,
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )
        val hasEvidence = signal != null || snapshot != null
        if (!hasEvidence) {
            return defaultBranch.trim().ifBlank { stats.values.firstOrNull()?.name.orEmpty() }
        }

        val defaultStats = stats[normalizedDefault]
        if (normalizedDefault.isNotBlank() && (defaultStats?.artifactCount ?: 0) > 0) {
            return defaultStats?.name ?: defaultBranch.trim()
        }

        val artifactBranch = stats.values
            .filter { it.artifactCount > 0 }
            .maxWithOrNull(
                compareBy<BranchStats> { it.artifactCount }
                    .thenBy { branchPriority(it.name, defaultBranch) }
                    .thenBy { it.runCount }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
        if (artifactBranch != null) {
            return artifactBranch.name
        }

        val signalRecommended = signal?.recommendedRunBranch.orEmpty().trim()
        if (signalRecommended.isNotBlank() && stats.containsKey(normalize(signalRecommended))) {
            return stats.getValue(normalize(signalRecommended)).name
        }

        val defaultRunCount = defaultStats?.runCount ?: 0
        if (normalizedDefault.isNotBlank() && defaultRunCount > 0) {
            val strongerBranch = stats.values
                .filter { normalize(it.name) != normalizedDefault && it.runCount > defaultRunCount }
                .maxWithOrNull(
                    compareBy<BranchStats> { it.runCount }
                        .thenBy { branchPriority(it.name, defaultBranch) }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )
            return strongerBranch?.name ?: (defaultStats?.name ?: defaultBranch.trim())
        }

        val activeBranch = stats.values
            .filter { it.runCount > 0 }
            .maxWithOrNull(
                compareBy<BranchStats> { it.runCount }
                    .thenBy { branchPriority(it.name, defaultBranch) }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
        return activeBranch?.name
            ?: defaultBranch.trim().ifBlank { stats.values.firstOrNull()?.name.orEmpty() }
    }

    private fun collectBranchStats(
        defaultBranch: String,
        workflow: GitHubActionsWorkflow,
        signal: GitHubActionsWorkflowArtifactSignal?,
        snapshot: GitHubActionsWorkflowArtifactsSnapshot?
    ): LinkedHashMap<String, BranchStats> {
        val stats = linkedMapOf<String, BranchStats>()
        fun add(
            branch: String,
            runCount: Int = 0,
            artifactCount: Int = 0,
            defaultBranchFlag: Boolean = false
        ) {
            val name = branch.trim()
            if (name.isBlank()) return
            val key = normalize(name)
            val current = stats[key]
            if (current == null) {
                stats[key] = BranchStats(
                    name = name,
                    runCount = runCount.coerceAtLeast(0),
                    artifactCount = artifactCount.coerceAtLeast(0),
                    defaultBranch = defaultBranchFlag
                )
            } else {
                current.runCount = maxOf(current.runCount, runCount.coerceAtLeast(0))
                current.artifactCount = maxOf(current.artifactCount, artifactCount.coerceAtLeast(0))
                current.defaultBranch = current.defaultBranch || defaultBranchFlag
            }
        }

        add(defaultBranch, defaultBranchFlag = true)
        signal?.branchRunCounts.orEmpty().forEach { (branch, count) ->
            add(branch, runCount = count)
        }
        signal?.branchArtifactCounts.orEmpty().forEach { (branch, count) ->
            add(branch, artifactCount = count)
        }
        signal?.recommendedRunBranch.orEmpty().takeIf { it.isNotBlank() }?.let { branch ->
            add(branch)
        }
        snapshot?.runs.orEmpty()
            .map { runArtifacts ->
                runArtifacts.run.headBranch.trim() to
                    runArtifacts.artifacts.count { artifact -> !artifact.expired }
            }
            .filter { (branch, _) -> branch.isNotBlank() }
            .groupBy({ it.first }, { it.second })
            .forEach { (branch, artifactCounts) ->
                add(
                    branch = branch,
                    runCount = artifactCounts.size,
                    artifactCount = artifactCounts.sum()
                )
            }
        if (GitHubActionsWorkflowSelector.inspectWorkflow(workflow).nightlyLike) {
            add("dev")
            add("develop")
        }
        return stats
    }

    private fun branchPriority(branch: String, defaultBranch: String): Int {
        val normalized = normalize(branch)
        return when {
            normalized.isBlank() -> 0
            normalized == normalize(defaultBranch) -> 100
            normalized in commonMainlineBranches -> 88
            normalized in commonDevBranches -> 78
            normalized.startsWith("release/") ||
                normalized.startsWith("releases/") ||
                normalized.startsWith("stable/") ||
                normalized.startsWith("hotfix/") -> 70
            normalized.startsWith("renovate/") ||
                normalized.startsWith("dependabot/") -> 20
            "/" in normalized -> 36
            else -> 48
        }
    }

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)

    private data class BranchStats(
        val name: String,
        var runCount: Int = 0,
        var artifactCount: Int = 0,
        var defaultBranch: Boolean = false
    )

    private val commonMainlineBranches = setOf("main", "master", "trunk")
    private val commonDevBranches = setOf("dev", "develop", "development", "nightly", "preview")
}
