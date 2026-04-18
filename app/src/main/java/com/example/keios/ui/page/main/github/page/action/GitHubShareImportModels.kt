package com.example.keios.ui.page.main

import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile

internal data class GitHubShareImportPreview(
    val sourceUrl: String,
    val projectUrl: String,
    val owner: String,
    val repo: String,
    val releaseTag: String,
    val releaseUrl: String,
    val strategyLabel: String,
    val assets: List<GitHubReleaseAssetFile>,
    val preferredAssetName: String = ""
) {
    val defaultSelectedIndex: Int
        get() {
            if (assets.isEmpty()) return -1
            val preferred = preferredAssetName.trim()
            if (preferred.isBlank()) return 0
            val index = assets.indexOfFirst { asset ->
                asset.name.equals(preferred, ignoreCase = true)
            }
            return if (index >= 0) index else 0
        }
}

internal data class GitHubPendingShareImportTrack(
    val projectUrl: String,
    val owner: String,
    val repo: String,
    val armedAtMillis: Long = System.currentTimeMillis()
)
