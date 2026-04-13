package com.example.keios.feature.github.data.remote

import com.example.keios.feature.github.model.GitHubReleaseChannel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubAtomHeuristicsTest {
    @Test
    fun `content preview keeps semantic version lines and removes asset download noise`() {
        val html = """
            <div>NVIM v0.13.0-dev-151+gfa22a78d2a</div>
            <div>Build type: RelWithDebInfo</div>
            <div>Download <strong>nvim-win64.zip</strong> and <strong>nvim-win64.msi</strong></div>
            <div>curl -LO https://github.com/neovim/neovim/releases/download/nightly/nvim-linux-x86_64.tar.gz</div>
            <div>Release notes</div>
        """.trimIndent()

        val plainText = GitHubAtomHeuristics.htmlToPlainText(html)
        val preview = GitHubAtomHeuristics.buildContentPreview(plainText)

        assertTrue("v0.13.0-dev-151" in preview)
        assertFalse("nvim-win64.zip" in preview)
        assertFalse("curl -LO" in preview)
    }

    @Test
    fun `channel detection supports tracked project variants`() {
        assertEquals(
            GitHubReleaseChannel.DEV,
            GitHubAtomHeuristics.detectReleaseChannel(
                tag = "Version.26.4.Canary_C378",
                title = "Capsulyric Canary",
                contentPreview = ""
            )
        )
        assertEquals(
            GitHubReleaseChannel.PREVIEW,
            GitHubAtomHeuristics.detectReleaseChannel(
                tag = "v1.4.6-prerelease2",
                title = "BatteryRecorder pre-release",
                contentPreview = ""
            )
        )
        assertEquals(
            GitHubReleaseChannel.DEV,
            GitHubAtomHeuristics.detectReleaseChannel(
                tag = "1.9.0.n488.nightly",
                title = "WhatAnime nightly",
                contentPreview = ""
            )
        )
    }

    @Test
    fun `channel detection does not let changelog prerelease noise override stable tag`() {
        assertEquals(
            GitHubReleaseChannel.STABLE,
            GitHubAtomHeuristics.detectReleaseChannel(
                tag = "3.8.0",
                title = "3.8.0",
                contentPreview = "Full Changelog 3.7.2-alpha02...3.8.0"
            )
        )
    }

    @Test
    fun `content preview keeps release note headings for generic titles`() {
        val content = """
            Release notes
            Version 2.8.0-20260410
            Fixed several regressions
            Windows package keyguard-app-x64.zip
        """.trimIndent()

        val preview = GitHubAtomHeuristics.buildContentPreview(content)

        assertTrue("Version 2.8.0-20260410" in preview)
        assertTrue("Release notes" in preview)
        assertFalse("keyguard-app-x64.zip" in preview)
    }
}
