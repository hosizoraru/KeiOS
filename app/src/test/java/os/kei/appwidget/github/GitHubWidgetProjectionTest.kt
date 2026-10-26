package os.kei.appwidget.github

import kotlin.test.assertEquals
import org.junit.Test
import os.kei.feature.github.data.local.GitHubTrackSnapshot
import os.kei.feature.github.model.GitHubCheckCacheEntry
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedIgnoreMode
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.buildGitHubReleaseIgnoreKey
import os.kei.feature.github.model.checkSourceSignature
import os.kei.feature.github.model.forTrackedItem

class GitHubWidgetProjectionTest {
    private val config = GitHubLookupConfig()
    private fun item(name: String) = GitHubTrackedApp(
        repoUrl = "https://github.com/test/$name",
        owner = "test", repo = name, packageName = "", appLabel = name,
    )

    private fun entry(item: GitHubTrackedApp, update: Boolean? = false) = GitHubCheckCacheEntry(
        hasUpdate = update,
        checkedAtMillis = 100L,
        sourceConfigSignature = item.checkSourceSignature(config.forTrackedItem(item)),
    )

    @Test
    fun `stable and prerelease updates count each project once`() {
        val items = listOf(item("Both"), item("Pre"), item("Current"))
        val result = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = items, lookupConfig = config,
                checkCache = mapOf(
                    items[0].id to entry(items[0], true).copy(hasPreReleaseUpdate = true),
                    items[1].id to entry(items[1]).copy(hasPreReleaseUpdate = true),
                    items[2].id to entry(items[2]),
                ),
            ),
        )
        assertEquals(3, result.trackedCount)
        assertEquals(2, result.updateCount)
        assertEquals(listOf("Both", "Pre"), result.pendingNames)
    }

    @Test
    fun `invalid cache and failed checks never become an all current result`() {
        val items = listOf(item("Changed"), item("Failed"), item("Missing"))
        val result = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = items, lookupConfig = config,
                checkCache = mapOf(
                    items[0].id to entry(items[0], true).copy(sourceConfigSignature = "obsolete"),
                    items[1].id to entry(items[1]).copy(
                        message = GitHubTrackedReleaseStatus.Failed.failureMessage("offline"),
                    ),
                ),
            ),
        )
        assertEquals(0, result.updateCount)
        assertEquals(2, result.uncheckedCount)
        assertEquals(1, result.failedCount)
        assertEquals(100L, result.lastCheckedAtMillis)
    }

    @Test
    fun `paused tracks and removed cache records do not leak update names`() {
        val paused = item("Paused").copy(ignoreMode = GitHubTrackedIgnoreMode.AllVersions)
        val removed = item("Removed")
        val result = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = listOf(paused), lookupConfig = config,
                checkCache = mapOf(paused.id to entry(paused, true), removed.id to entry(removed, true)),
            ),
        )
        assertEquals(1, result.ignoredCount)
        assertEquals(0, result.uncheckedCount)
        assertEquals(0, result.updateCount)
        assertEquals(emptyList(), result.pendingNames)
    }

    @Test
    fun `new ignore settings apply to a previously cached update`() {
        val ignored = item("Ignored").copy(
            ignoreMode = GitHubTrackedIgnoreMode.CurrentStable,
            ignoredStableReleaseKey = buildGitHubReleaseIgnoreKey(
                displayVersion = "2.0", rawTag = "v2.0", rawName = "2.0",
            ),
        )
        val cached = entry(ignored, true).copy(
            latestTag = "2.0", latestStableRawTag = "v2.0", latestStableName = "2.0",
        )
        fun project(check: GitHubCheckCacheEntry) = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = listOf(ignored), lookupConfig = config, checkCache = mapOf(ignored.id to check),
            ),
        )
        assertEquals(0, project(cached).updateCount)
        assertEquals(1, project(cached.copy(hasPreReleaseUpdate = true)).updateCount)
        assertEquals(
            1,
            project(cached.copy(latestTag = "3.0", latestStableRawTag = "v3.0", latestStableName = "3.0")).updateCount,
        )
    }

    @Test
    fun `displayed versions follow the eligible release rather than an ignored stable release`() {
        val both = item("Both")
        val ignored = item("Ignored").copy(
            ignoreMode = GitHubTrackedIgnoreMode.CurrentStable,
            ignoredStableReleaseKey = buildGitHubReleaseIgnoreKey(
                displayVersion = "2.0", rawTag = "v2.0", rawName = "2.0",
            ),
        )
        val result = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = listOf(both, ignored), lookupConfig = config,
                checkCache = listOf(both, ignored).associate { tracked ->
                    tracked.id to entry(tracked, true).copy(
                        latestTag = "2.0", latestStableRawTag = "v2.0", latestStableName = "2.0",
                        hasPreReleaseUpdate = true, latestPreRawTag = "v3.0-rc1",
                    )
                },
            ),
        )
        assertEquals(
            listOf(GitHubWidgetPendingUpdate("Both", "v2.0"), GitHubWidgetPendingUpdate("Ignored", "v3.0-rc1", true)),
            result.pendingUpdates,
        )
    }

    @Test
    fun `snapshot has bounded names and uses check time rather than publish or refresh time`() {
        val items = (1..20).map { item("N".repeat(150) + it) }
        val result = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = items, lookupConfig = config,
                checkCache = items.associate { it.id to entry(it, true) },
                lastRefreshMs = 999999L,
            ),
        )
        assertEquals(20, result.updateCount)
        assertEquals(6, result.pendingNames.size)
        assertEquals(List(6) { 80 }, result.pendingNames.map(String::length))
        assertEquals(100L, result.lastCheckedAtMillis)
    }

    @Test
    fun `large layouts prioritize updates and failed checks ahead of already checked projects`() {
        val current = (1..10).map { item("Current$it") }
        val paused = item("Paused").copy(ignoreMode = GitHubTrackedIgnoreMode.AllVersions)
        val missing = item("Missing")
        val failed = item("Failed")
        val update = item("Update")
        val result = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = current + listOf(paused, missing, failed, update), lookupConfig = config,
                checkCache = current.associate { it.id to entry(it) } + mapOf(
                    failed.id to entry(failed).copy(message = GitHubTrackedReleaseStatus.Failed.failureMessage("offline")),
                    update.id to entry(update, true).copy(latestStableRawTag = "v2.0"),
                ),
            ),
        )
        assertEquals(listOf("Update", "Failed", "Missing", "Current1", "Current2", "Current3"), result.projects.map { it.name })
        assertEquals(10, result.noUpdateCount)
        assertEquals(12, result.checkedCount)
        assertEquals(1, result.ignoredCount)
        assertEquals(1, result.failedCount)
        assertEquals(1, result.uncheckedCount)
    }

    @Test
    fun `authoritative empty snapshot clears previously displayed updates`() {
        assertEquals(
            GitHubWidgetSnapshot(available = true),
            projectGitHubWidget(GitHubTrackSnapshot()),
        )
    }

    @Test
    fun `project details keep installed and candidate versions separate and improve package name fallbacks`() {
        val tracked = item("ShadowQUIC").copy(packageName = "fr.husi.plugin.shadowquic", appLabel = "fr.husi.plugin.shadowquic")
        val cached = entry(tracked, true).copy(localVersion = "1.0", latestStableRawTag = "v2.0")
        val result = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = listOf(tracked), lookupConfig = config, checkCache = mapOf(tracked.id to cached),
            ),
        )
        assertEquals(GitHubWidgetProject("ShadowQUIC", GitHubWidgetProjectStatus.Update, "v2.0", localVersion = "1.0"), result.projects.single())
        val invalid = projectGitHubWidget(
            GitHubTrackSnapshot(
                items = listOf(tracked), lookupConfig = config,
                checkCache = mapOf(tracked.id to cached.copy(sourceConfigSignature = "obsolete")),
            ),
        )
        assertEquals("", invalid.projects.single().localVersion)
        assertEquals("", invalid.projects.single().version)
    }
}
