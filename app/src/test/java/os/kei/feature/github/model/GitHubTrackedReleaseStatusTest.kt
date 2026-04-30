package os.kei.feature.github.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubTrackedReleaseStatusTest {
    @Test
    fun `status parser accepts stable and legacy cache messages`() {
        assertEquals(
            GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable,
            GitHubTrackedReleaseStatus.fromMessage("github.status.prerelease_update_available")
        )
        assertEquals(
            GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable,
            GitHubTrackedReleaseStatus.fromMessage("\u9884\u53d1\u6709\u66f4\u65b0")
        )
        assertEquals(
            GitHubTrackedReleaseStatus.Failed,
            GitHubTrackedReleaseStatus.fromMessage("\u68c0\u67e5\u5931\u8d25: timeout")
        )
        assertTrue(
            GitHubTrackedReleaseStatus.isOnlyPreReleasesHint(
                "\u8be5\u9879\u76ee\u6682\u65f6\u53ef\u80fd\u53ea\u6709\u9884\u53d1\u884c\u7248"
            )
        )
    }

    @Test
    fun `failure detail localizes stable legacy and English prefixes`() {
        assertEquals(
            "Check failed: timeout",
            GitHubTrackedReleaseStatus.localizedFailureDetail("github.status.failed: timeout", "Check failed")
        )
        assertEquals(
            "Check failed: timeout",
            GitHubTrackedReleaseStatus.localizedFailureDetail("\u68c0\u67e5\u5931\u8d25: timeout", "Check failed")
        )
        assertEquals(
            "\u68c0\u67e5\u5931\u8d25: timeout",
            GitHubTrackedReleaseStatus.localizedFailureDetail("Check failed: timeout", "\u68c0\u67e5\u5931\u8d25")
        )
    }
}
