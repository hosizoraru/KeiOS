package com.example.keios.feature.github.data.remote

import com.example.keios.feature.github.model.GitHubReleaseChannel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubTrackedRepoCorpusTest {
    @Test
    fun `tracked corpus keeps prerelease visibility only for forward moving tracks`() {
        val fixtures = GitHubTrackedRepoFixtures.parityCorpus.associateBy { it.id }

        assertEquals("v1.4.7-prerelease3", fixtures.getValue("Itosang/BatteryRecorder").tokenPreRawTag)
        assertEquals("Version.26.4.Alpha2_C384", fixtures.getValue("FrancoGiudans/Capsulyric").tokenPreRawTag)
        assertEquals("1.9.0.n488.nightly", fixtures.getValue("JanYoStudio/WhatAnime").tokenPreRawTag)
        assertEquals("v26.4.9.C01-Dev", fixtures.getValue("badnng/Hyper-pick-up-code").tokenPreRawTag)

        assertNull(fixtures.getValue("T8RIN/ImageToolbox").tokenPreRawTag)
        assertNull(fixtures.getValue("open-ani/animeko").tokenPreRawTag)
        assertNull(fixtures.getValue("anilbeesetti/nextplayer").tokenPreRawTag)
        assertNull(fixtures.getValue("Moriafly/SaltPlayerSource").tokenPreRawTag)
        assertNull(fixtures.getValue("YumeLira/YumeBox").tokenPreRawTag)
    }

    @Test
    fun `wild tracked versions remain comparable after normalization`() {
        assertEquals(
            GitHubReleaseChannel.DEV,
            GitHubVersionUtils.classifyVersionChannel("v26.4.9.C01-Dev")
        )
        assertEquals(
            GitHubReleaseChannel.STABLE,
            GitHubVersionUtils.classifyVersionChannel("260412_1.22")
                ?: GitHubReleaseChannel.STABLE
        )
        assertEquals(
            GitHubReleaseChannel.PREVIEW,
            GitHubVersionUtils.classifyVersionChannel("v1.4.7-prerelease3")
        )
        assertEquals(
            GitHubReleaseChannel.DEV,
            GitHubVersionUtils.classifyVersionChannel("1.9.0.n488.nightly")
        )

        assertTrue(
            GitHubVersionUtils.compareVersionToCandidates(
                localVersion = "26.4.3.C01",
                candidates = listOf("v26.4.9.C01-Dev")
            ) == -1
        )
        assertTrue(
            GitHubVersionUtils.compareVersionToCandidates(
                localVersion = "1.8.8.r471.cff36155",
                candidates = listOf("1.9.0.r488.d07a3e1b")
            ) == -1
        )
        assertTrue(
            GitHubVersionUtils.compareVersionToCandidates(
                localVersion = "1.22",
                candidates = listOf("260412_1.22")
            ) == 0
        )
    }
}


    @Test
    fun `prerelease only tracked repo still has usable effective latest signal`() {
        val fixtures = GitHubTrackedRepoFixtures.parityCorpus.associateBy { it.id }
        val monogram = fixtures.getValue("monogram-android/monogram")

        assertEquals("0.0.8", monogram.atomStableRawTag)
        assertEquals("0.0.8", monogram.tokenStableRawTag)
        assertNull(monogram.atomPreRawTag)
        assertNull(monogram.tokenPreRawTag)
    }

    @Test
    fun `non app artifact release does not hijack pixiv shaft stable signal`() {
        val fixtures = GitHubTrackedRepoFixtures.parityCorpus.associateBy { it.id }
        val shaft = fixtures.getValue("CeuiLiSA/Pixiv-Shaft")

        assertEquals("v4.5.3", shaft.atomStableRawTag)
        assertEquals("v4.5.3", shaft.tokenStableRawTag)
        assertNull(shaft.atomPreRawTag)
        assertNull(shaft.tokenPreRawTag)
    }

    @Test
    fun `recent bilipai stable aligns across both strategies`() {
        val fixtures = GitHubTrackedRepoFixtures.parityCorpus.associateBy { it.id }
        val bilipai = fixtures.getValue("jay3-yy/BiliPai")

        assertEquals("7.8.0", bilipai.atomStableRawTag)
        assertEquals("7.8.0", bilipai.tokenStableRawTag)
        assertNull(bilipai.atomPreRawTag)
        assertNull(bilipai.tokenPreRawTag)
    }
