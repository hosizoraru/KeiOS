package com.example.keios.feature.github.data.remote

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubTrackedRepoDeviceSnapshotTest {
    @Test
    fun `device snapshot resource is available for regression tests`() {
        val items = GitHubTrackedRepoDeviceSnapshot.items

        assertEquals(25, items.size)
        assertTrue(items.any { it.id == "monogram-android/monogram" })
        assertTrue(items.any { it.id == "CeuiLiSA/Pixiv-Shaft" })
        assertTrue(items.any { it.id == "jay3-yy/BiliPai" })
    }
}
