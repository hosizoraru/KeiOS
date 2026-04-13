package com.example.keios.feature.github.data.remote

import com.example.keios.feature.github.model.GitHubVersionCandidateSource
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitHubVersionUtilsTest {
    @Test
    fun `stable release outranks same-base rc from ImageToolbox style versions`() {
        val compare = GitHubVersionUtils.compareVersionToCandidates(
            localVersion = "3.8.0-rc04",
            candidates = listOf("3.8.0")
        )
        assertEquals(-1, compare)
    }

    @Test
    fun `canary build is older than alpha build for Capsulyric style versions`() {
        val compare = GitHubVersionUtils.compareVersionToCandidates(
            localVersion = "Version.26.4.Canary_C378",
            candidates = listOf("Version.26.4.Alpha2_C384")
        )
        assertEquals(-1, compare)
    }

    @Test
    fun `newer alpha still outranks older stable for SaltPlayer style versions`() {
        val compare = GitHubVersionUtils.compareVersionToCandidates(
            localVersion = "11.2.0-alpha01",
            candidates = listOf("11.1.0")
        )
        assertEquals(1, compare)
    }

    @Test
    fun `pre-release keyword without dash is recognized`() {
        val channel = GitHubVersionUtils.classifyVersionChannel("v1.4.6-prerelease2")
        assertEquals(com.example.keios.feature.github.model.GitHubReleaseChannel.PREVIEW, channel)
    }

    @Test
    fun `nightly token is recognized for WhatAnime style builds`() {
        val channel = GitHubVersionUtils.classifyVersionChannel("1.9.0.n488.nightly")
        assertEquals(com.example.keios.feature.github.model.GitHubReleaseChannel.DEV, channel)
    }

    @Test
    fun `keyguard style semantic title candidate beats raw date tag`() {
        val candidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "r20260410",
            GitHubVersionCandidateSource.Title to "Release v2.8.0-20260410"
        )
        val compare = GitHubVersionUtils.compareVersionToStructuredCandidates(
            localVersion = "2.7.0",
            candidates = candidates
        )
        assertEquals(-1, compare)
    }

    @Test
    fun `candidate normalization keeps tracked project formats`() {
        val candidates = GitHubVersionUtils.normalizeVersionCandidates("Version.26.4.Canary_C378")
        assertTrue(candidates.any { it.contains("26.4") })
        assertTrue(candidates.any { it.contains("canary") || it.contains("dev") })
    }

    @Test
    fun `structured candidate comparison prefers exact semantic match over content noise`() {
        val left = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "v0.19.7"
        )
        val right = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "v0.19.9",
            GitHubVersionCandidateSource.Content to "release notes for v0.19.7 and older hotfix"
        )
        val compare = GitHubVersionUtils.compareStructuredCandidateSets(left, right)
        assertEquals(-1, compare)
    }

    @Test
    fun `date prefixed release names still expose semantic version candidate`() {
        val candidates = GitHubVersionUtils.normalizeVersionCandidates("260412_1.22")
        assertTrue("1.22".lowercase() in candidates)
        val compare = GitHubVersionUtils.compareVersionToCandidates(
            localVersion = "1.22",
            candidates = listOf("260412_1.22")
        )
        assertEquals(0, compare)
    }

    @Test
    fun `structured comparison returns exact match when title carries current version`() {
        val candidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "r20260410",
            GitHubVersionCandidateSource.Title to "Release v2.7.0-20260320"
        )
        val compare = GitHubVersionUtils.compareVersionToStructuredCandidates("2.7.0", candidates)
        assertNotNull(compare)
        assertEquals(0, compare)
    }

    @Test
    fun `meaningful prerelease candidates keep date based rc tags but reject branch names`() {
        val dateRcCandidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "v20260410-rc1"
        )
        val branchCandidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "dev-fix-access-denied-error-1"
        )

        assertTrue(GitHubVersionUtils.hasMeaningfulPreReleaseVersionCandidates(dateRcCandidates))
        assertEquals(false, GitHubVersionUtils.hasMeaningfulPreReleaseVersionCandidates(branchCandidates))
    }

    @Test
    fun `same release detection uses candidate overlap instead of loose semantic equality`() {
        val stableCandidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "Version.1.3.Fix2_C359"
        )
        val preCandidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "Version.26.4.Alpha2_C384"
        )
        val duplicateCandidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "v0.0.8",
            GitHubVersionCandidateSource.Title to "0.0.8"
        )

        assertEquals(false, GitHubVersionUtils.referToSameReleaseVersion(preCandidates, stableCandidates))
        assertTrue(GitHubVersionUtils.referToSameReleaseVersion(duplicateCandidates, duplicateCandidates))
    }

    @Test
    fun `same release detection ignores content noise from changelog ranges`() {
        val stableCandidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "3.8.0"
        )
        val preCandidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to "3.8.0-rc04",
            GitHubVersionCandidateSource.Content to "Full Changelog 3.8.0-rc03...3.8.0-rc04"
        )

        assertEquals(false, GitHubVersionUtils.referToSameReleaseVersion(preCandidates, stableCandidates))
    }
}
