package os.kei.mcp.server

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import os.kei.feature.github.data.local.GitHubReleaseAssetCacheStore
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.feature.github.domain.GitHubReleaseCheckService
import os.kei.feature.github.model.GitHubTrackedApp
import java.util.Locale

internal class McpGitHubTools(
    private val environment: McpToolEnvironment
) {
    private data class GitHubCheckRow(
        val item: GitHubTrackedApp,
        val localVersion: String,
        val stableVersion: String,
        val preReleaseVersion: String,
        val status: String,
        val hasUpdate: Boolean
    )

    private val appContext get() = environment.appContext

    fun register(server: Server) {
        server.addTool(
            name = "keios.github.tracked.snapshot",
            description = "Get GitHub tracked settings and cache snapshot.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildGitHubTrackedSnapshotText())
        }

        server.addTool(
            name = "keios.github.tracked.list",
            description = "List tracked repos. Args: repoFilter(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("repoFilter", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val repoFilter = argString(request.arguments?.get("repoFilter")).trim()
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TRACK_LIMIT).coerceIn(1, MAX_TRACK_LIMIT)
            callText(buildGitHubTrackedListText(repoFilter = repoFilter, limit = limit))
        }

        server.addTool(
            name = "keios.github.tracked.check",
            description = "Online check tracked repo updates. Args: repoFilter(optional), onlyUpdates(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("repoFilter", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("onlyUpdates", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val repoFilter = argString(request.arguments?.get("repoFilter")).trim()
            val onlyUpdates = argBoolean(request.arguments?.get("onlyUpdates"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_TRACK_LIMIT).coerceIn(1, MAX_TRACK_LIMIT)
            val rows = checkTrackedGitHub(repoFilter)
                .let { data -> if (onlyUpdates) data.filter { it.hasUpdate } else data }
                .take(limit)

            val text = if (rows.isEmpty()) {
                if (onlyUpdates) "No tracked repos with updates." else "No tracked repos matched."
            } else {
                rows.joinToString("\n") { row ->
                    val repo = "${row.item.owner}/${row.item.repo}"
                    val pre = if (row.preReleaseVersion.isNotBlank()) " | pre=${row.preReleaseVersion}" else ""
                    "$repo | local=${row.localVersion} | stable=${row.stableVersion}$pre | status=${row.status} | update=${row.hasUpdate}"
                }
            }
            callText(text)
        }

        server.addTool(
            name = "keios.github.tracked.summary",
            description = "Get tracked summary. Args: mode(cache|network), repoFilter(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("mode", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("repoFilter", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val mode = argString(request.arguments?.get("mode")).trim().lowercase(Locale.ROOT)
            val repoFilter = argString(request.arguments?.get("repoFilter")).trim()
            callText(
                if (mode == "network") {
                    buildGitHubTrackedSummaryFromNetwork(repoFilter)
                } else {
                    buildGitHubTrackedSummaryFromCache(repoFilter)
                }
            )
        }

        server.addTool(
            name = "keios.github.tracked.cache.clear",
            description = "Clear GitHub tracked check cache.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            GitHubTrackStore.clearCheckCache()
            GitHubReleaseAssetCacheStore.clearAll()
            callText("cleared=github_check_cache")
        }
    }

    private fun buildGitHubTrackedSnapshotText(): String {
        val snapshot = GitHubTrackStore.loadSnapshot()
        val cachedUpdateCount = snapshot.checkCache.values.count { it.hasUpdate == true }
        val cachedFailedCount = snapshot.checkCache.values.count {
            it.message.contains("失败", ignoreCase = true) || it.message.contains("failed", ignoreCase = true)
        }
        return buildString {
            appendLine("trackedCount=${snapshot.items.size}")
            appendLine("cachedCheckCount=${snapshot.checkCache.size}")
            appendLine("cachedHasUpdateCount=$cachedUpdateCount")
            appendLine("cachedFailedCount=$cachedFailedCount")
            appendLine("lastRefreshMs=${snapshot.lastRefreshMs}")
            appendLine("refreshIntervalHours=${snapshot.refreshIntervalHours}")
            appendLine("lookupStrategy=${snapshot.lookupConfig.selectedStrategy.storageId}")
            appendLine("apiTokenConfigured=${snapshot.lookupConfig.apiToken.isNotBlank()}")
            appendLine("checkAllTrackedPreReleases=${snapshot.lookupConfig.checkAllTrackedPreReleases}")
            appendLine("aggressiveApkFiltering=${snapshot.lookupConfig.aggressiveApkFiltering}")
        }.trim()
    }

    private fun filterTrackedItems(items: List<GitHubTrackedApp>, repoFilter: String): List<GitHubTrackedApp> {
        if (repoFilter.isBlank()) return items
        return items.filter {
            "${it.owner}/${it.repo}".contains(repoFilter, ignoreCase = true) ||
                it.packageName.contains(repoFilter, ignoreCase = true) ||
                it.appLabel.contains(repoFilter, ignoreCase = true)
        }
    }

    private fun buildGitHubTrackedListText(repoFilter: String, limit: Int): String {
        val items = filterTrackedItems(GitHubTrackStore.load(), repoFilter).take(limit)
        if (items.isEmpty()) return "No tracked GitHub apps."
        return items.joinToString("\n") { item ->
            "${item.owner}/${item.repo} | label=${item.appLabel} | package=${item.packageName} | preferPreRelease=${item.preferPreRelease}"
        }
    }

    private fun buildGitHubTrackedSummaryFromCache(repoFilter: String): String {
        val snapshot = GitHubTrackStore.loadSnapshot()
        val tracked = filterTrackedItems(snapshot.items, repoFilter)
        if (tracked.isEmpty()) {
            return "mode=cache\ntracked=0\nmatched=0"
        }

        val ids = tracked.map { it.id }.toSet()
        val cacheHit = snapshot.checkCache.filterKeys { key -> key in ids }
        val hasUpdate = cacheHit.count { it.value.hasUpdate == true }
        val unknown = cacheHit.count { it.value.hasUpdate == null }
        val preRelease = cacheHit.count { it.value.showPreReleaseInfo || it.value.hasPreReleaseUpdate || it.value.recommendsPreRelease }

        return buildString {
            appendLine("mode=cache")
            appendLine("tracked=${snapshot.items.size}")
            appendLine("matched=${tracked.size}")
            appendLine("cacheHit=${cacheHit.size}")
            appendLine("hasUpdate=$hasUpdate")
            appendLine("unknown=$unknown")
            appendLine("preReleaseState=$preRelease")
            appendLine("lastRefreshMs=${snapshot.lastRefreshMs}")
            cacheHit.entries
                .sortedByDescending { it.value.hasUpdate == true }
                .take(20)
                .forEach { (id, entry) ->
                    appendLine("$id=${entry.message}")
                }
        }.trim()
    }

    private fun buildGitHubTrackedSummaryFromNetwork(repoFilter: String): String {
        val rows = checkTrackedGitHub(repoFilter)
        val hasUpdate = rows.count { it.hasUpdate }
        val preRelease = rows.count { it.preReleaseVersion.isNotBlank() || it.status.contains("预", ignoreCase = true) }
        return buildString {
            appendLine("mode=network")
            appendLine("matched=${rows.size}")
            appendLine("hasUpdate=$hasUpdate")
            appendLine("preReleaseState=$preRelease")
            rows.sortedByDescending { it.hasUpdate }
                .take(20)
                .forEach { row ->
                    appendLine("${row.item.owner}/${row.item.repo}=${row.status}")
                }
        }.trim()
    }

    private fun checkTrackedGitHub(repoFilter: String): List<GitHubCheckRow> {
        val items = GitHubTrackStore.load()
        val filtered = filterTrackedItems(items, repoFilter)
        return filtered.map { item ->
            runCatching { evaluateTrackedApp(item) }.getOrElse { err ->
                GitHubCheckRow(
                    item = item,
                    localVersion = runCatching {
                        GitHubVersionUtils.localVersionName(appContext, item.packageName)
                    }.getOrDefault("unknown"),
                    stableVersion = "unknown",
                    preReleaseVersion = "",
                    status = "检查失败: ${err.message ?: "unknown"}",
                    hasUpdate = false
                )
            }
        }
    }

    private fun evaluateTrackedApp(item: GitHubTrackedApp): GitHubCheckRow {
        val check = GitHubReleaseCheckService.evaluateTrackedApp(appContext, item)
        return GitHubCheckRow(
            item = item,
            localVersion = check.localVersion,
            stableVersion = check.stableRelease?.displayVersion.orEmpty().ifBlank { "unknown" },
            preReleaseVersion = check.preReleaseInfo,
            status = check.message,
            hasUpdate = check.hasUpdate == true
        )
    }
}
