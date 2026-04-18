package com.example.keios.feature.github.data.remote

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

class GitHubShareIntentParserTest {
    @Test
    fun `extract first github url trims trailing punctuation`() {
        val text = "看看这个链接：https://github.com/open-ani/animeko/releases。"
        val url = GitHubShareIntentParser.extractFirstGitHubUrl(text)
        assertEquals("https://github.com/open-ani/animeko/releases", url)
    }

    @Test
    fun `parse repo link to project target`() {
        val parsed = GitHubShareIntentParser.parseSharedReleaseLink(
            "https://github.com/open-ani/animeko"
        )
        assertNotNull(parsed)
        assertEquals(GitHubSharedUrlType.Repo, parsed.type)
        assertEquals("open-ani", parsed.owner)
        assertEquals("animeko", parsed.repo)
        assertEquals("https://github.com/open-ani/animeko", parsed.projectUrl)
    }

    @Test
    fun `parse releases page link`() {
        val parsed = GitHubShareIntentParser.parseSharedReleaseLink(
            "https://github.com/open-ani/animeko/releases"
        )
        assertNotNull(parsed)
        assertEquals(GitHubSharedUrlType.Releases, parsed.type)
        assertEquals("open-ani", parsed.owner)
        assertEquals("animeko", parsed.repo)
    }

    @Test
    fun `parse release tag link with decoded tag`() {
        val parsed = GitHubShareIntentParser.parseSharedReleaseLink(
            "https://github.com/open-ani/animeko/releases/tag/v5.5.0-alpha02"
        )
        assertNotNull(parsed)
        assertEquals(GitHubSharedUrlType.ReleaseTag, parsed.type)
        assertEquals("v5.5.0-alpha02", parsed.releaseTag)
    }

    @Test
    fun `parse release download link with tag and asset`() {
        val parsed = GitHubShareIntentParser.parseSharedReleaseLink(
            "https://github.com/open-ani/animeko/releases/download/v5.5.0-alpha02/ani-5.5.0-alpha02-arm64-v8a.apk"
        )
        assertNotNull(parsed)
        assertEquals(GitHubSharedUrlType.ReleaseDownloadAsset, parsed.type)
        assertEquals("v5.5.0-alpha02", parsed.releaseTag)
        assertEquals("ani-5.5.0-alpha02-arm64-v8a.apk", parsed.assetName)
    }

    @Test
    fun `non github text returns null`() {
        val parsed = GitHubShareIntentParser.parseSharedReleaseLink("https://example.com/demo")
        assertNull(parsed)
    }
}
