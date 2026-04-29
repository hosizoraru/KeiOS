package os.kei.feature.github.domain

import org.junit.Test
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsArtifactKind
import os.kei.feature.github.model.GitHubActionsArtifactPlatform
import os.kei.feature.github.model.GitHubActionsArtifactSelectionOptions
import os.kei.feature.github.model.GitHubActionsDownloadRecord
import os.kei.feature.github.model.GitHubReleaseChannel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubActionsArtifactSelectorTest {
    @Test
    fun `name inspector detects android artifact traits`() {
        val traits = GitHubActionsArtifactSelector.inspectName("app-arm64-v8a-release.apk")

        assertEquals(GitHubActionsArtifactKind.AndroidPackage, traits.kind)
        assertEquals(GitHubActionsArtifactPlatform.Android, traits.platform)
        assertEquals("apk", traits.extension)
        assertEquals("arm64-v8a", traits.abi)
        assertEquals(GitHubReleaseChannel.STABLE, traits.channel)
        assertTrue(traits.releaseLike)
        assertTrue(traits.androidLike)
    }

    @Test
    fun `libchecker flavor artifacts stay selectable while mappings stay hidden`() {
        val matches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = listOf(
                artifact("mappings", size = 1000L),
                artifact("Market", size = 45_000_000L),
                artifact("Foss", size = 44_000_000L)
            )
        )

        assertEquals(listOf("Market", "Foss"), matches.map { it.artifact.name })
        assertEquals(GitHubActionsArtifactPlatform.Android, matches.first().traits.platform)
        assertTrue(matches.first().traits.flavors.contains("market"))
        assertTrue(matches[1].traits.flavors.contains("foss"))
    }

    @Test
    fun `multiplatform artifacts default to android artifact only`() {
        val matches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = listOf(
                artifact("Updater-darwin-arm64-dmg.dmg"),
                artifact("Updater-android-universal-apk.apk"),
                artifact("Updater-linux-x64-bin"),
                artifact("Updater-windows-x64-exe"),
                artifact("Updater-linux-arm64-bin")
            )
        )

        assertEquals(listOf("Updater-android-universal-apk.apk"), matches.map { it.artifact.name })
        assertTrue(matches.single().traits.universalLike)
    }

    @Test
    fun `online offline variants are detected as android flavors`() {
        val matches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = listOf(
                artifact("app-offline-Unstable-release.apk"),
                artifact("app-online-Unstable-release.apk"),
                artifact("app-offline-Preview-debug.apk")
            )
        )

        assertEquals(
            listOf(
                "app-offline-Unstable-release.apk",
                "app-online-Unstable-release.apk",
                "app-offline-Preview-debug.apk"
            ),
            matches.map { it.artifact.name }
        )
        assertEquals(GitHubReleaseChannel.DEV, matches.first().traits.channel)
        assertTrue(matches.first().traits.flavors.contains("offline"))
        assertEquals(GitHubReleaseChannel.PREVIEW, matches.last().traits.channel)
    }

    @Test
    fun `selector ranks preferred arm64 release before universal and debug artifacts`() {
        val matches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = listOf(
                artifact("app-universal-release.zip", size = 32_000_000L),
                artifact("app-x86-debug.apk", size = 20_000_000L),
                artifact("app-arm64-v8a-release.apk", size = 24_000_000L),
                artifact("mapping.txt", size = 1000L)
            ),
            options = GitHubActionsArtifactSelectionOptions(
                preferredAbis = listOf("arm64-v8a")
            )
        )

        assertEquals(
            listOf("app-arm64-v8a-release.apk", "app-universal-release.zip", "app-x86-debug.apk"),
            matches.map { it.artifact.name }
        )
        assertTrue(matches.first().reasons.contains("arm64-v8a"))
        assertFalse(matches.any { it.artifact.name == "mapping.txt" })
    }

    @Test
    fun `aggressive abi filtering hides explicit non preferred abi artifacts`() {
        val matches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = listOf(
                artifact("app-arm64-v8a-release.apk"),
                artifact("app-x86_64-release.apk"),
                artifact("app-universal-release.zip")
            ),
            options = GitHubActionsArtifactSelectionOptions(
                preferredAbis = listOf("arm64-v8a"),
                aggressiveAbiFiltering = true
            )
        )

        assertEquals(
            listOf("app-arm64-v8a-release.apk", "app-universal-release.zip"),
            matches.map { it.artifact.name }
        )
    }

    @Test
    fun `selector supports query and regex gates`() {
        val matches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = listOf(
                artifact("KeiOS-release-arm64-v8a.apk"),
                artifact("KeiOS-debug-arm64-v8a.apk"),
                artifact("KeiOS-release-x86.apk")
            ),
            options = GitHubActionsArtifactSelectionOptions(
                query = "KeiOS release",
                includeRegex = Regex("arm64", RegexOption.IGNORE_CASE),
                excludeRegex = Regex("debug", RegexOption.IGNORE_CASE)
            )
        )

        assertEquals(listOf("KeiOS-release-arm64-v8a.apk"), matches.map { it.artifact.name })
        assertTrue(matches.single().reasons.contains("query"))
    }

    @Test
    fun `expired artifacts stay hidden by default`() {
        val matches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = listOf(
                artifact("app-release.apk", expired = true),
                artifact("app-debug.apk", expired = false)
            )
        )

        assertEquals(listOf("app-debug.apk"), matches.map { it.artifact.name })
    }

    @Test
    fun `selector boosts and exposes last downloaded artifact`() {
        val downloaded = artifact("app-universal-release.apk", id = 20L)
        val fresh = artifact("app-arm64-v8a-release.apk", id = 21L)

        val matches = GitHubActionsArtifactSelector.selectDisplayArtifacts(
            artifacts = listOf(fresh, downloaded),
            options = GitHubActionsArtifactSelectionOptions(
                preferredAbis = listOf("arm64-v8a"),
                downloadHistory = listOf(
                    GitHubActionsDownloadRecord(
                        owner = "demo",
                        repo = "app",
                        artifactId = 20L,
                        artifactName = "app-universal-release.apk",
                        downloadedAtMillis = System.currentTimeMillis()
                    )
                )
            )
        )

        assertEquals("app-universal-release.apk", matches.first().artifact.name)
        assertTrue(matches.first().reasons.contains("last-downloaded"))
        assertEquals(20L, matches.first().lastDownload?.artifactId)
    }

    private fun artifact(
        name: String,
        size: Long = 10_000_000L,
        expired: Boolean = false,
        id: Long = name.hashCode().toLong()
    ): GitHubActionsArtifact {
        return GitHubActionsArtifact(
            id = id,
            name = name,
            sizeBytes = size,
            expired = expired
        )
    }
}
