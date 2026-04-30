package os.kei.feature.github.domain

import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.feature.github.model.GitHubAtomFeed
import os.kei.feature.github.model.GitHubAtomReleaseEntry
import os.kei.feature.github.model.GitHubReleaseChannel
import os.kei.feature.github.model.GitHubReleaseSignalSource
import os.kei.feature.github.model.GitHubReleaseVersionSignals
import os.kei.feature.github.model.GitHubRepositoryReleaseSnapshot
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.GitHubVersionCandidateSource
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubReleaseCheckServiceTest {
    @Test
    fun `preferred prerelease recommends newer prerelease for BatteryRecorder style repo`() {
        val item = trackedApp(preferPreRelease = true)
        val snapshot = snapshot(
            stable = signal("v1.4.4-release"),
            preRelease = signal("v1.4.7-prerelease3"),
            entries = listOf(entry("v1.4.7-prerelease3"), entry("v1.4.4-release"), entry("v1.4.2-release"))
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "1.4.2-release",
            localVersionCode = 551L,
            snapshot = snapshot
        )

        assertEquals(GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable, result.status)
        assertTrue(result.hasUpdate == true)
        assertTrue(result.hasPreReleaseUpdate)
        assertTrue(result.recommendsPreRelease)
        assertEquals("v1.4.7-prerelease3", result.preRelease?.rawTag)
    }

    @Test
    fun `global prerelease checking keeps stable recommendation when item does not prefer prerelease`() {
        val item = trackedApp(preferPreRelease = false)
        val snapshot = snapshot(
            stable = signal("v1.4.4-release"),
            preRelease = signal("v1.4.7-prerelease3"),
            entries = listOf(entry("v1.4.7-prerelease3"), entry("v1.4.4-release"), entry("v1.4.2-release"))
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "1.4.2-release",
            localVersionCode = 551L,
            snapshot = snapshot,
            checkAllTrackedPreReleases = true
        )

        assertEquals(GitHubTrackedReleaseStatus.UpdateAvailable, result.status)
        assertTrue(result.hasUpdate == true)
        assertTrue(result.hasPreReleaseUpdate)
        assertFalse(result.recommendsPreRelease)
        assertEquals("v1.4.7-prerelease3", result.preReleaseInfo)
    }

    @Test
    fun `newer prerelease becomes optional when stable is already latest and item does not prefer prerelease`() {
        val item = trackedApp(preferPreRelease = false)
        val snapshot = snapshot(
            stable = signal("v1.4.4-release"),
            preRelease = signal("v1.4.7-prerelease3"),
            entries = listOf(entry("v1.4.7-prerelease3"), entry("v1.4.4-release"))
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "1.4.4-release",
            localVersionCode = 554L,
            snapshot = snapshot,
            checkAllTrackedPreReleases = true
        )

        assertEquals(GitHubTrackedReleaseStatus.PreReleaseOptional, result.status)
        assertFalse(result.hasUpdate == true)
        assertTrue(result.hasPreReleaseUpdate)
        assertFalse(result.recommendsPreRelease)
    }

    @Test
    fun `local prerelease older than stable prefers stable update for ImageToolbox style repo`() {
        val item = trackedApp(preferPreRelease = true)
        val snapshot = snapshot(
            stable = signal("3.8.0"),
            preRelease = signal("3.8.0-rc04"),
            entries = listOf(entry("3.8.0"), entry("3.8.0-rc04"))
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "3.8.0-rc04",
            localVersionCode = 224L,
            snapshot = snapshot
        )

        assertEquals(GitHubTrackedReleaseStatus.UpdateAvailable, result.status)
        assertTrue(result.hasUpdate == true)
        assertFalse(result.hasPreReleaseUpdate)
        assertFalse(result.recommendsPreRelease)
        assertTrue(result.isPreReleaseInstalled)
        assertEquals("3.8.0-rc04", result.preReleaseInfo)
    }

    @Test
    fun `unmatched local prerelease still gets prerelease update through channel inference for Capsulyric`() {
        val item = trackedApp(preferPreRelease = false)
        val snapshot = snapshot(
            stable = signal("Version.1.3.Fix2_C359", updatedAtMillis = 1_743_790_000_000L),
            preRelease = signal("Version.26.4.Alpha2_C384", updatedAtMillis = 1_744_137_000_000L),
            entries = listOf(
                entry("Version.26.4.Alpha2_C384"),
                entry("Canary.Version_C384", title = "Canary Build Version.26.4.Canary_C384"),
                entry("Version.1.3.Fix2_C359")
            )
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "Version.26.4.Canary_C378",
            localVersionCode = 378L,
            snapshot = snapshot,
            checkAllTrackedPreReleases = true
        )

        assertEquals(GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable, result.status)
        assertTrue(result.hasUpdate == true)
        assertTrue(result.hasPreReleaseUpdate)
        assertTrue(result.recommendsPreRelease)
        assertTrue(result.isPreReleaseInstalled)
        assertEquals("Version.26.4.Alpha2_C384", result.preRelease?.rawTag)
    }

    @Test
    fun `older prerelease remains visible for animeko but does not override stable recommendation`() {
        val item = trackedApp(preferPreRelease = true)
        val snapshot = snapshot(
            stable = signal("v5.4.3", title = "5.4.3", updatedAtMillis = 1_744_426_950_000L),
            preRelease = signal("v5.4.0-beta05", title = "5.4.0-beta05", updatedAtMillis = 1_742_888_753_000L),
            entries = listOf(entry("v5.4.3", title = "5.4.3"), entry("v5.4.0-beta05", title = "5.4.0-beta05"))
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "5.4.0",
            localVersionCode = 50400L,
            snapshot = snapshot
        )

        assertEquals(GitHubTrackedReleaseStatus.UpdateAvailable, result.status)
        assertTrue(result.hasUpdate == true)
        assertFalse(result.hasPreReleaseUpdate)
        assertFalse(result.recommendsPreRelease)
        assertEquals("5.4.0-beta05", result.preReleaseInfo)
    }

    @Test
    fun `dev prerelease newer than stable is recommended when prerelease is preferred`() {
        val item = trackedApp(preferPreRelease = true)
        val snapshot = snapshot(
            stable = signal("v26.4.3.C01"),
            preRelease = signal("v26.4.9.C01-Dev"),
            entries = listOf(entry("v26.4.9.C01-Dev"), entry("v26.4.3.C01"))
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "26.4.3.C01",
            localVersionCode = 2026040301L,
            snapshot = snapshot
        )

        assertEquals(GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable, result.status)
        assertTrue(result.hasUpdate == true)
        assertTrue(result.hasPreReleaseUpdate)
        assertTrue(result.recommendsPreRelease)
        assertEquals("v26.4.9.C01-Dev", result.preRelease?.rawTag)
    }

    @Test
    fun `prerelease only repository keeps prerelease visible through hint when prerelease checking is off`() {
        val item = trackedApp(preferPreRelease = false)
        val snapshot = snapshot(
            stable = signal("0.0.8", title = "v0.0.8"),
            preRelease = signal("0.0.8", title = "v0.0.8"),
            entries = listOf(entry("0.0.8", title = "v0.0.8")),
            hasStableRelease = false
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "unknown",
            localVersionCode = -1L,
            snapshot = snapshot
        )

        assertFalse(result.hasStableRelease)
        assertEquals(GitHubTrackedReleaseStatus.ONLY_PRERELEASES_HINT_MESSAGE, result.releaseHint)
        assertFalse(result.showPreReleaseInfo)
        assertEquals("0.0.8", result.preRelease?.rawTag)
        assertEquals(null, result.stableRelease)
    }

    @Test
    fun `current stable signal overrides historical prerelease tagging for matched local build`() {
        val item = trackedApp(preferPreRelease = true)
        val promotedStableEntry = entry("v1.0.0-rc1").copy(
            channel = GitHubReleaseChannel.RC,
            isLikelyPreRelease = true
        )
        val snapshot = snapshot(
            stable = signal("v1.0.0-rc1"),
            preRelease = null,
            entries = listOf(promotedStableEntry),
            hasStableRelease = true
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "1.0.0-rc1",
            localVersionCode = 10001L,
            snapshot = snapshot
        )

        assertFalse(result.isPreReleaseInstalled)
        assertEquals(GitHubTrackedReleaseStatus.UpToDate, result.status)
    }

    @Test
    fun `release notes mentioning previous app version still reports self update`() {
        val item = trackedApp(preferPreRelease = false)
        val contentPreview = "这是从 v1.1.0 到 v1.2.0 的功能更新"
        val stable = signal(
            tag = "v1.2.0",
            title = "KeiOS v1.2.0",
            contentPreview = contentPreview
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "1.1.0",
            localVersionCode = 10_100_000L,
            snapshot = snapshot(
                stable = stable,
                entries = listOf(
                    entry(
                        tag = "v1.2.0",
                        title = "KeiOS v1.2.0",
                        contentPreview = contentPreview
                    ),
                    entry(tag = "v1.1.0", title = "KeiOS v1.1.0")
                )
            )
        )

        assertEquals(GitHubTrackedReleaseStatus.UpdateAvailable, result.status)
        assertEquals(true, result.hasUpdate)
        assertEquals("v1.2.0", result.stableRelease?.rawTag)
    }

    @Test
    fun `local prerelease newer than both stable and stale prerelease does not surface remote update`() {
        val item = trackedApp(preferPreRelease = false)
        val snapshot = snapshot(
            stable = signal("11.1.0-release-2026031101"),
            preRelease = signal("10.9.0-alpha03-2025070901"),
            entries = listOf(
                entry("11.1.0-release-2026031101"),
                entry("10.9.0-alpha03-2025070901")
            )
        )

        val result = GitHubReleaseCheckService.evaluateSnapshot(
            item = item,
            localVersion = "11.2.0-alpha01",
            localVersionCode = 11020001L,
            snapshot = snapshot
        )

        assertTrue(result.isPreReleaseInstalled)
        assertEquals(false, result.hasUpdate)
        assertFalse(result.hasPreReleaseUpdate)
        assertFalse(result.recommendsPreRelease)
        assertEquals("10.9.0-alpha03-2025070901", result.preReleaseInfo)
    }

    private fun trackedApp(preferPreRelease: Boolean): GitHubTrackedApp {
        return GitHubTrackedApp(
            repoUrl = "https://github.com/demo/app",
            owner = "demo",
            repo = "app",
            packageName = "demo.app",
            appLabel = "Demo",
            preferPreRelease = preferPreRelease
        )
    }

    private fun snapshot(
        stable: GitHubReleaseVersionSignals,
        preRelease: GitHubReleaseVersionSignals? = null,
        entries: List<GitHubAtomReleaseEntry>,
        hasStableRelease: Boolean = true
    ): GitHubRepositoryReleaseSnapshot {
        return GitHubRepositoryReleaseSnapshot(
            strategyId = "github_api_token",
            feed = GitHubAtomFeed(
                title = "demo/app releases",
                feedUrl = "https://github.com/demo/app/releases",
                entries = entries
            ),
            latestStable = stable,
            hasStableRelease = hasStableRelease,
            latestPreRelease = preRelease
        )
    }

    private fun signal(
        tag: String,
        title: String = tag,
        updatedAtMillis: Long? = null,
        contentPreview: String = ""
    ): GitHubReleaseVersionSignals {
        return GitHubReleaseVersionSignals(
            displayVersion = title,
            rawTag = tag,
            rawName = title,
            link = GitHubVersionUtils.buildReleaseTagUrl("demo", "app", tag),
            updatedAtMillis = updatedAtMillis,
            versionCandidates = GitHubVersionUtils.buildVersionCandidates(
                GitHubVersionCandidateSource.Tag to tag,
                GitHubVersionCandidateSource.Title to title,
                GitHubVersionCandidateSource.Content to contentPreview
            ),
            source = GitHubReleaseSignalSource.GitHubApi,
            channel = GitHubVersionUtils.classifyVersionChannel(tag) ?: GitHubReleaseChannel.UNKNOWN
        )
    }

    private fun entry(
        tag: String,
        title: String = tag,
        contentPreview: String = ""
    ): GitHubAtomReleaseEntry {
        val channel = GitHubVersionUtils.classifyVersionChannel(tag) ?: GitHubReleaseChannel.UNKNOWN
        return GitHubAtomReleaseEntry(
            tag = tag,
            title = title,
            link = GitHubVersionUtils.buildReleaseTagUrl("demo", "app", tag),
            versionCandidates = GitHubVersionUtils.buildVersionCandidates(
                GitHubVersionCandidateSource.Tag to tag,
                GitHubVersionCandidateSource.Title to title,
                GitHubVersionCandidateSource.Content to contentPreview
            ),
            channel = channel,
            isLikelyPreRelease = channel.isPreRelease
        )
    }
}
