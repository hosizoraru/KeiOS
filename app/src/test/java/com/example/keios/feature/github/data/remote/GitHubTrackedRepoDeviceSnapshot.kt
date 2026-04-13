package com.example.keios.feature.github.data.remote

import org.json.JSONArray

internal data class GitHubTrackedRepoSnapshotItem(
    val repoUrl: String,
    val owner: String,
    val repo: String,
    val packageName: String,
    val appLabel: String,
    val checkPreRelease: Boolean
) {
    val id: String = "$owner/$repo"
}

internal object GitHubTrackedRepoDeviceSnapshot {
    private const val RESOURCE_PATH = "/github/tracked_repos_device_snapshot.json"

    val items: List<GitHubTrackedRepoSnapshotItem> by lazy {
        val raw = checkNotNull(javaClass.getResourceAsStream(RESOURCE_PATH)) {
            "Missing test resource: $RESOURCE_PATH"
        }.bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    GitHubTrackedRepoSnapshotItem(
                        repoUrl = item.getString("repoUrl"),
                        owner = item.getString("owner"),
                        repo = item.getString("repo"),
                        packageName = item.getString("packageName"),
                        appLabel = item.optString("appLabel"),
                        checkPreRelease = item.optBoolean("checkPreRelease", false)
                    )
                )
            }
        }
    }
}
