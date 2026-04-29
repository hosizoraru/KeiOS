package os.kei.feature.github.domain

import os.kei.feature.github.data.remote.GitHubApiTokenReleaseStrategy
import os.kei.feature.github.data.remote.GitHubAtomReleaseStrategy
import os.kei.feature.github.model.GitHubRepoTarget
import os.kei.feature.github.model.GitHubStrategyBenchmarkReport
import os.kei.feature.github.model.GitHubStrategyBenchmarkResult
import os.kei.feature.github.model.GitHubStrategyBenchmarkSample
import os.kei.feature.github.model.GitHubStrategyLoadTrace
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubRepositoryReleaseSnapshot
import java.util.concurrent.Callable
import java.util.concurrent.Executors

object GitHubStrategyBenchmarkService {
    private const val DEFAULT_TARGET_LIMIT = 6
    private const val DEFAULT_BENCHMARK_CONCURRENCY = 4

    fun buildTargets(
        trackedItems: List<GitHubTrackedApp>,
        limit: Int = DEFAULT_TARGET_LIMIT
    ): List<GitHubRepoTarget> {
        return trackedItems
            .map { GitHubRepoTarget(owner = it.owner, repo = it.repo) }
            .distinctBy { it.id }
            .take(limit)
    }

    fun compareTargets(
        targets: List<GitHubRepoTarget>,
        apiToken: String = ""
    ): GitHubStrategyBenchmarkReport {
        val distinctTargets = targets
            .distinctBy { it.id }
        if (distinctTargets.isEmpty()) {
            return GitHubStrategyBenchmarkReport(
                targets = emptyList(),
                results = emptyList()
            )
        }

        val apiStrategy = GitHubApiTokenReleaseStrategy(apiToken = apiToken.trim())
        val runners = listOf(
            GitHubStrategyBenchmarkRunner(
                strategyId = GitHubAtomReleaseStrategy.id,
                displayName = "Atom",
                clearCaches = { GitHubAtomReleaseStrategy.clearCaches() },
                load = { target -> GitHubAtomReleaseStrategy.loadSnapshotTrace(target.owner, target.repo) }
            ),
            GitHubStrategyBenchmarkRunner(
                strategyId = apiStrategy.id,
                displayName = "API",
                clearCaches = { apiStrategy.clearCaches() },
                load = { target -> apiStrategy.loadSnapshotTrace(target.owner, target.repo) }
            )
        )

        return compareTargetsWithRunners(
            targets = distinctTargets,
            runners = runners
        )
    }

    internal fun compareTargetsWithRunners(
        targets: List<GitHubRepoTarget>,
        runners: List<GitHubStrategyBenchmarkRunner>,
        maxConcurrency: Int = DEFAULT_BENCHMARK_CONCURRENCY
    ): GitHubStrategyBenchmarkReport {
        if (targets.isEmpty() || runners.isEmpty()) {
            return GitHubStrategyBenchmarkReport(
                targets = targets,
                results = emptyList()
            )
        }
        return GitHubStrategyBenchmarkReport(
            targets = targets,
            results = runConcurrently(
                items = runners,
                maxConcurrency = maxConcurrency.coerceAtLeast(1)
            ) { runner ->
                runner.run(
                    targets = targets,
                    maxConcurrency = maxConcurrency
                )
            }
        )
    }

    private fun GitHubStrategyBenchmarkRunner.run(
        targets: List<GitHubRepoTarget>,
        maxConcurrency: Int
    ): GitHubStrategyBenchmarkResult {
        clearCaches()
        val coldSamples = loadSamples(
            targets = targets,
            maxConcurrency = maxConcurrency
        )
        val warmSamples = loadSamples(
            targets = targets,
            maxConcurrency = maxConcurrency
        )
        val authMode = coldSamples.firstOrNull()?.authMode ?: warmSamples.firstOrNull()?.authMode

        return GitHubStrategyBenchmarkResult(
            strategyId = strategyId,
            displayName = displayName,
            authMode = authMode,
            coldSamples = coldSamples.map { it.sample },
            warmSamples = warmSamples.map { it.sample }
        )
    }

    private fun GitHubStrategyBenchmarkRunner.loadSamples(
        targets: List<GitHubRepoTarget>,
        maxConcurrency: Int
    ): List<SampleEnvelope> {
        return runConcurrently(
            items = targets,
            maxConcurrency = maxConcurrency
        ) { target ->
            load(target).toSample(target)
        }
    }

    private fun <T, R> runConcurrently(
        items: List<T>,
        maxConcurrency: Int,
        block: (T) -> R
    ): List<R> {
        if (items.isEmpty()) return emptyList()
        val concurrency = items.size.coerceAtMost(maxConcurrency.coerceAtLeast(1))
        if (concurrency <= 1) return items.map(block)
        val executor = Executors.newFixedThreadPool(concurrency)
        return try {
            executor.invokeAll(items.map { item -> Callable { block(item) } })
                .map { future -> future.get() }
        } finally {
            executor.shutdownNow()
        }
    }

    private data class SampleEnvelope(
        val sample: GitHubStrategyBenchmarkSample,
        val authMode: os.kei.feature.github.model.GitHubApiAuthMode?
    )

    private fun GitHubStrategyLoadTrace<GitHubRepositoryReleaseSnapshot>.toSample(
        target: GitHubRepoTarget
    ): SampleEnvelope {
        val snapshot = result.getOrNull()
        val errorMessage = result.exceptionOrNull()?.message.orEmpty()
        return SampleEnvelope(
            sample = GitHubStrategyBenchmarkSample(
                target = target,
                success = result.isSuccess,
                fromCache = fromCache,
                elapsedMs = elapsedMs,
                message = errorMessage,
                stableTag = snapshot?.takeIf { it.hasStableRelease }?.latestStable?.rawTag.orEmpty(),
                preReleaseTag = snapshot?.latestPreRelease?.rawTag.orEmpty()
            ),
            authMode = authMode
        )
    }
}

internal data class GitHubStrategyBenchmarkRunner(
    val strategyId: String,
    val displayName: String,
    val clearCaches: () -> Unit,
    val load: (GitHubRepoTarget) -> GitHubStrategyLoadTrace<GitHubRepositoryReleaseSnapshot>
)
