package com.example.keios.feature.github.data.remote

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubTrackedRepoFixtureGroupsTest {
    private val fixtures = GitHubTrackedRepoFixtures.parityCorpus

    @Test
    fun `tracked repos can be sliced into human review friendly groups`() {
        val prereleaseOnly = fixtures.filter { it.atomStableRawTag == "0.0.8" && it.id == "monogram-android/monogram" }
        val prereleaseAhead = fixtures.filter { it.atomPreRawTag != null }
        val placeholderSensitive = fixtures.filter { it.id == "YumeLira/YumeBox" }
        val nonAppArtifactSensitive = fixtures.filter { it.id == "CeuiLiSA/Pixiv-Shaft" }
        val datePrefixed = fixtures.filter { it.id == "AChep/keyguard-app" || it.id == "NEORUAA/WeType_UI_Enhanced" }

        assertEquals(listOf("monogram-android/monogram"), prereleaseOnly.map { it.id })
        assertTrue(prereleaseAhead.map { it.id }.containsAll(listOf(
            "Itosang/BatteryRecorder",
            "FrancoGiudans/Capsulyric",
            "JanYoStudio/WhatAnime",
            "badnng/Hyper-pick-up-code"
        )))
        assertEquals(listOf("YumeLira/YumeBox"), placeholderSensitive.map { it.id })
        assertEquals(listOf("CeuiLiSA/Pixiv-Shaft"), nonAppArtifactSensitive.map { it.id })
        assertTrue(datePrefixed.map { it.id }.containsAll(listOf(
            "AChep/keyguard-app",
            "NEORUAA/WeType_UI_Enhanced"
        )))
    }
}
