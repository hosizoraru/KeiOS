package os.kei.ui.page.main.github.share

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import os.kei.feature.github.data.local.GitHubPendingShareImportTrackRecord

class GitHubShareImportWindowFlowSupportTest {
    @Test
    fun `reconciliation ignores package updated before current share was armed`() {
        val pending = pendingTrack(armedAtMillis = 10_000L)
        val candidate = selectRecentInstalledCandidateForPendingTrack(
            pendingTrack = pending,
            candidates = listOf(
                installedPackage(
                    packageName = "old.package",
                    lastUpdateTimeMs = 9_999L
                )
            )
        )

        assertNull(candidate)
    }

    @Test
    fun `reconciliation picks package updated after current share was armed`() {
        val pending = pendingTrack(armedAtMillis = 10_000L)
        val candidate = selectRecentInstalledCandidateForPendingTrack(
            pendingTrack = pending,
            candidates = listOf(
                installedPackage(
                    packageName = "old.package",
                    lastUpdateTimeMs = 9_500L
                ),
                installedPackage(
                    packageName = "new.package",
                    lastUpdateTimeMs = 12_000L
                )
            )
        )

        assertEquals("new.package", candidate?.packageName)
    }

    @Test
    fun `reconciliation stays empty when recent packages are ambiguous`() {
        val pending = pendingTrack(armedAtMillis = 10_000L)
        val candidate = selectRecentInstalledCandidateForPendingTrack(
            pendingTrack = pending,
            candidates = listOf(
                installedPackage(
                    packageName = "first.package",
                    lastUpdateTimeMs = 13_000L
                ),
                installedPackage(
                    packageName = "second.package",
                    lastUpdateTimeMs = 12_500L
                )
            )
        )

        assertNull(candidate)
    }

    private fun pendingTrack(armedAtMillis: Long) = GitHubPendingShareImportTrackRecord(
        projectUrl = "https://github.com/asadahimeka/pixiv-viewer-app",
        owner = "asadahimeka",
        repo = "pixiv-viewer-app",
        armedAtMillis = armedAtMillis
    )

    private fun installedPackage(
        packageName: String,
        lastUpdateTimeMs: Long
    ) = ShareImportInstalledPackageSnapshot(
        packageName = packageName,
        appLabel = packageName,
        lastUpdateTimeMs = lastUpdateTimeMs,
        firstInstallTimeMs = lastUpdateTimeMs
    )
}
