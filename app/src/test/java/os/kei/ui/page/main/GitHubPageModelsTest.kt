package os.kei.ui.page.main

import androidx.compose.ui.graphics.Color
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.formatReleaseValue
import os.kei.ui.page.main.github.statusActionUrl
import os.kei.ui.page.main.github.statusColor
import org.junit.Test
import kotlin.test.assertEquals

class GitHubPageModelsTest {
    @Test
    fun `format release value keeps tag when release name is placeholder`() {
        assertEquals("Release · 0.9.100", formatReleaseValue("Release", "0.9.100"))
        assertEquals("随风而行 · v0.5.1", formatReleaseValue("随风而行", "v0.5.1"))
    }

    @Test
    fun `format release value avoids duplicate tag when name and tag match`() {
        assertEquals("v0.0.8", formatReleaseValue("v0.0.8", "v0.0.8"))
        assertEquals("3.8.0", formatReleaseValue("3.8.0", "3.8.0"))
    }

    @Test
    fun `format release value prefers compact tag when name carries project label`() {
        assertEquals("1.1.0", formatReleaseValue("KeiOS v1.1.0", "v1.1.0"))
    }

    @Test
    fun `status action url only exists for real remote update targets`() {
        val preTrackedNoRemoteUpdate = VersionCheckUi(
            hasUpdate = false,
            isPreRelease = true,
            latestPreRawTag = "10.9.0-alpha03-2025070901",
            latestPreUrl = "https://github.com/demo/app/releases/tag/10.9.0-alpha03-2025070901"
        )
        val stableUpdate = VersionCheckUi(
            hasUpdate = true,
            latestStableRawTag = "v1.2.3"
        )
        val preUpdate = VersionCheckUi(
            hasPreReleaseUpdate = true,
            latestPreRawTag = "v1.3.0-beta1"
        )

        assertEquals("", preTrackedNoRemoteUpdate.statusActionUrl("demo", "app"))
        assertEquals(
            "https://github.com/demo/app/releases/tag/v1.2.3",
            stableUpdate.statusActionUrl("demo", "app")
        )
        assertEquals(
            "https://github.com/demo/app/releases/tag/v1.3.0-beta1",
            preUpdate.statusActionUrl("demo", "app")
        )
    }

    @Test
    fun `local prerelease without remote update keeps prerelease status color`() {
        val state = VersionCheckUi(
            hasUpdate = false,
            isPreRelease = true
        )

        assertEquals(Color(0xFFF59E0B), state.statusColor(Color.Gray))
    }
}
