package os.kei.feature.github.domain

import org.junit.Test
import os.kei.feature.github.model.GitHubRepoTarget
import os.kei.feature.github.model.GitHubRepositoryReleaseSnapshot
import os.kei.feature.github.model.GitHubStrategyLoadTrace
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubStrategyBenchmarkServiceTest {
    @Test
    fun `benchmark runs strategies concurrently`() {
        val activeLoads = AtomicInteger(0)
        val maxActiveLoads = AtomicInteger(0)
        val firstColdWave = CountDownLatch(2)
        val runners = listOf(
            benchmarkRunner("atom", activeLoads, maxActiveLoads, firstColdWave),
            benchmarkRunner("api", activeLoads, maxActiveLoads, firstColdWave)
        )

        val report = GitHubStrategyBenchmarkService.compareTargetsWithRunners(
            targets = listOf(GitHubRepoTarget("demo", "app")),
            runners = runners,
            maxConcurrency = 2
        )

        assertEquals(listOf("atom", "api"), report.results.map { it.strategyId })
        assertTrue(maxActiveLoads.get() >= 2)
    }

    @Test
    fun `benchmark runs targets concurrently within each strategy`() {
        val activeLoads = AtomicInteger(0)
        val maxActiveLoads = AtomicInteger(0)
        val firstColdWave = CountDownLatch(2)
        val runner = benchmarkRunner("atom", activeLoads, maxActiveLoads, firstColdWave)

        val report = GitHubStrategyBenchmarkService.compareTargetsWithRunners(
            targets = listOf(
                GitHubRepoTarget("demo", "app"),
                GitHubRepoTarget("demo", "lib")
            ),
            runners = listOf(runner),
            maxConcurrency = 2
        )

        assertEquals(2, report.results.single().coldSamples.size)
        assertTrue(maxActiveLoads.get() >= 2)
    }

    private fun benchmarkRunner(
        strategyId: String,
        activeLoads: AtomicInteger,
        maxActiveLoads: AtomicInteger,
        firstColdWave: CountDownLatch
    ): GitHubStrategyBenchmarkRunner {
        return GitHubStrategyBenchmarkRunner(
            strategyId = strategyId,
            displayName = strategyId,
            clearCaches = {},
            load = {
                val active = activeLoads.incrementAndGet()
                updateMax(maxActiveLoads, active)
                firstColdWave.countDown()
                firstColdWave.await(500, TimeUnit.MILLISECONDS)
                activeLoads.decrementAndGet()
                GitHubStrategyLoadTrace<GitHubRepositoryReleaseSnapshot>(
                    result = Result.failure(IllegalStateException("demo")),
                    fromCache = false,
                    elapsedMs = 1L
                )
            }
        )
    }

    private fun updateMax(maxActiveLoads: AtomicInteger, active: Int) {
        while (true) {
            val current = maxActiveLoads.get()
            if (active <= current || maxActiveLoads.compareAndSet(current, active)) return
        }
    }
}
