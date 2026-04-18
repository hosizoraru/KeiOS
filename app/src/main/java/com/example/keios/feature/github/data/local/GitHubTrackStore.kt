package com.example.keios.feature.github.data.local

import com.example.keios.feature.github.model.GitHubCheckCacheEntry
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject

data class GitHubTrackSnapshot(
    val items: List<GitHubTrackedApp> = emptyList(),
    val checkCache: Map<String, GitHubCheckCacheEntry> = emptyMap(),
    val lastRefreshMs: Long = 0L,
    val refreshIntervalHours: Int = 3,
    val lookupConfig: GitHubLookupConfig = GitHubLookupConfig(),
    val pendingShareImportTrack: GitHubPendingShareImportTrackRecord? = null,
    val trackedFirstInstallAtByPackage: Map<String, Long> = emptyMap()
)

data class GitHubTrackedItemsImportPayload(
    val items: List<GitHubTrackedApp> = emptyList(),
    val sourceCount: Int = 0,
    val invalidCount: Int = 0,
    val duplicateCount: Int = 0
)

data class GitHubPendingShareImportTrackRecord(
    val projectUrl: String,
    val owner: String,
    val repo: String,
    val releaseTag: String = "",
    val assetName: String = "",
    val armedAtMillis: Long
)

object GitHubTrackStore {
    private const val TRACK_EXPORT_FORMAT = "keios.github.tracked/v1"
    private const val KV_ID = "github_track_store"
    private const val KEY_ITEMS = "tracked_items"
    private const val KEY_CHECK_CACHE = "tracked_check_cache"
    private const val KEY_LAST_REFRESH_MS = "last_full_refresh_ms"
    private const val KEY_REFRESH_INTERVAL_HOURS = "refresh_interval_hours"
    private const val KEY_LOOKUP_STRATEGY = "lookup_strategy"
    private const val KEY_GITHUB_API_TOKEN = "github_api_token"
    private const val KEY_CHECK_ALL_TRACKED_PRE_RELEASES = "check_all_tracked_pre_releases"
    private const val KEY_AGGRESSIVE_APK_FILTERING = "github_aggressive_apk_filtering"
    private const val KEY_SHARE_IMPORT_LINKAGE_ENABLED = "github_share_import_linkage_enabled"
    private const val KEY_ONLINE_SHARE_TARGET_PACKAGE = "github_online_share_target_package"
    private const val KEY_PREFERRED_DOWNLOADER_PACKAGE = "github_preferred_downloader_package"
    private const val KEY_PENDING_SHARE_IMPORT_TRACK = "github_pending_share_import_track"
    private const val KEY_TRACKED_FIRST_INSTALL_AT_BY_PACKAGE = "github_tracked_first_install_at_by_package"

    @Volatile
    private var didAutoRefreshInSession: Boolean = false
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    private fun kv(): MMKV = store

    fun load(): List<GitHubTrackedApp> {
        val raw = kv().decodeString(KEY_ITEMS).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseTrackedItem(obj)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(items: List<GitHubTrackedApp>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(trackedItemToJson(item))
        }
        kv().encode(KEY_ITEMS, array.toString())
    }

    fun loadPendingShareImportTrack(): GitHubPendingShareImportTrackRecord? {
        val raw = kv().decodeString(KEY_PENDING_SHARE_IMPORT_TRACK).orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val projectUrl = obj.optString("projectUrl").trim()
            val owner = obj.optString("owner").trim()
            val repo = obj.optString("repo").trim()
            val armedAtMillis = obj.optLong("armedAtMillis", 0L)
            if (projectUrl.isBlank() || owner.isBlank() || repo.isBlank() || armedAtMillis <= 0L) {
                return@runCatching null
            }
            GitHubPendingShareImportTrackRecord(
                projectUrl = projectUrl,
                owner = owner,
                repo = repo,
                releaseTag = obj.optString("releaseTag").trim(),
                assetName = obj.optString("assetName").trim(),
                armedAtMillis = armedAtMillis
            )
        }.getOrNull()
    }

    fun savePendingShareImportTrack(record: GitHubPendingShareImportTrackRecord?) {
        if (record == null) {
            kv().removeValueForKey(KEY_PENDING_SHARE_IMPORT_TRACK)
            return
        }
        val payload = JSONObject()
            .put("projectUrl", record.projectUrl)
            .put("owner", record.owner)
            .put("repo", record.repo)
            .put("releaseTag", record.releaseTag)
            .put("assetName", record.assetName)
            .put("armedAtMillis", record.armedAtMillis)
        kv().encode(KEY_PENDING_SHARE_IMPORT_TRACK, payload.toString())
    }

    fun loadTrackedFirstInstallAtByPackage(): Map<String, Long> {
        val raw = kv().decodeString(KEY_TRACKED_FIRST_INSTALL_AT_BY_PACKAGE).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val packageName = keys.next().trim()
                    if (packageName.isBlank()) continue
                    val firstInstallAtMillis = obj.optLong(packageName, -1L)
                    if (firstInstallAtMillis > 0L) {
                        put(packageName, firstInstallAtMillis)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveTrackedFirstInstallAtByPackage(values: Map<String, Long>) {
        if (values.isEmpty()) {
            kv().removeValueForKey(KEY_TRACKED_FIRST_INSTALL_AT_BY_PACKAGE)
            return
        }
        val payload = JSONObject()
        values.forEach { (packageName, firstInstallAtMillis) ->
            val normalizedPackageName = packageName.trim()
            if (normalizedPackageName.isBlank() || firstInstallAtMillis <= 0L) return@forEach
            payload.put(normalizedPackageName, firstInstallAtMillis)
        }
        if (payload.length() == 0) {
            kv().removeValueForKey(KEY_TRACKED_FIRST_INSTALL_AT_BY_PACKAGE)
            return
        }
        kv().encode(KEY_TRACKED_FIRST_INSTALL_AT_BY_PACKAGE, payload.toString())
    }

    fun buildTrackedItemsExportJson(
        items: List<GitHubTrackedApp>,
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(trackedItemToJson(item))
        }
        return JSONObject()
            .put("format", TRACK_EXPORT_FORMAT)
            .put("exportedAtMillis", exportedAtMillis)
            .put("itemCount", items.size)
            .put("items", array)
            .toString(2)
    }

    fun parseTrackedItemsImport(raw: String): GitHubTrackedItemsImportPayload {
        val normalized = raw.trim()
        if (normalized.isBlank()) {
            return GitHubTrackedItemsImportPayload()
        }
        val rootArray = when (normalized.firstOrNull()) {
            '{' -> {
                val root = JSONObject(normalized)
                when {
                    root.has("items") -> root.optJSONArray("items")
                    root.has("trackedItems") -> root.optJSONArray("trackedItems")
                    else -> null
                } ?: JSONArray()
            }
            '[' -> JSONArray(normalized)
            else -> throw IllegalArgumentException("unsupported github track import format")
        }
        val deduplicated = linkedMapOf<String, GitHubTrackedApp>()
        var invalidCount = 0
        var duplicateCount = 0
        for (index in 0 until rootArray.length()) {
            val itemObject = rootArray.optJSONObject(index)
            val item = itemObject?.let(::parseTrackedItem)
            if (item == null) {
                invalidCount += 1
                continue
            }
            if (deduplicated.containsKey(item.id)) {
                duplicateCount += 1
            }
            deduplicated[item.id] = item
        }
        return GitHubTrackedItemsImportPayload(
            items = deduplicated.values.toList(),
            sourceCount = rootArray.length(),
            invalidCount = invalidCount,
            duplicateCount = duplicateCount
        )
    }

    fun loadCheckCache(): Pair<Map<String, GitHubCheckCacheEntry>, Long> {
        val raw = kv().decodeString(KEY_CHECK_CACHE).orEmpty()
        val ts = kv().decodeLong(KEY_LAST_REFRESH_MS, 0L)
        if (raw.isBlank()) return emptyMap<String, GitHubCheckCacheEntry>() to ts
        val map = runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val item = obj.optJSONObject(id) ?: continue
                    put(
                        id,
                        GitHubCheckCacheEntry(
                            loading = false,
                            localVersion = item.optString("localVersion"),
                            localVersionCode = item.optLong("localVersionCode", -1L),
                            latestTag = item.optString("latestTag"),
                            latestStableName = item.optString("latestStableName").ifBlank {
                                item.optString("latestTag")
                            },
                            latestStableRawTag = item.optString("latestStableRawTag"),
                            latestStableUrl = item.optString("latestStableUrl"),
                            latestStableUpdatedAtMillis = item.optLong("latestStableUpdatedAtMillis", -1L),
                            latestPreName = item.optString("latestPreName").ifBlank {
                                item.optString("preReleaseInfo")
                            },
                            latestPreRawTag = item.optString("latestPreRawTag"),
                            latestPreUrl = item.optString("latestPreUrl"),
                            latestPreUpdatedAtMillis = item.optLong("latestPreUpdatedAtMillis", -1L),
                            hasStableRelease = if (item.has("hasStableRelease")) {
                                item.optBoolean("hasStableRelease", true)
                            } else {
                                item.optString("latestStableRawTag").isNotBlank() || item.optString("latestTag").isNotBlank()
                            },
                            hasUpdate = if (item.has("hasUpdate")) item.optBoolean("hasUpdate") else null,
                            message = item.optString("message"),
                            isPreRelease = item.optBoolean("isPreRelease", false),
                            preReleaseInfo = item.optString("preReleaseInfo"),
                            showPreReleaseInfo = item.optBoolean("showPreReleaseInfo", false),
                            hasPreReleaseUpdate = item.optBoolean("hasPreReleaseUpdate", false),
                            recommendsPreRelease = item.optBoolean("recommendsPreRelease", false),
                            releaseHint = item.optString("releaseHint"),
                            sourceStrategyId = item.optString("sourceStrategyId")
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
        return map to ts
    }

    fun loadSnapshot(): GitHubTrackSnapshot {
        val lookupConfig = loadLookupConfig()
        val (checkCache, lastRefreshMs) = loadCheckCache()
        return GitHubTrackSnapshot(
            items = load(),
            checkCache = checkCache,
            lastRefreshMs = lastRefreshMs,
            refreshIntervalHours = loadRefreshIntervalHours(),
            lookupConfig = lookupConfig,
            pendingShareImportTrack = loadPendingShareImportTrack(),
            trackedFirstInstallAtByPackage = loadTrackedFirstInstallAtByPackage()
        )
    }

    fun saveCheckCache(states: Map<String, GitHubCheckCacheEntry>, lastRefreshMs: Long) {
        val obj = JSONObject()
        states.forEach { (id, state) ->
            obj.put(
                id,
                JSONObject()
                    .put("localVersion", state.localVersion)
                    .put("localVersionCode", state.localVersionCode)
                    .put("latestTag", state.latestTag)
                    .put("latestStableName", state.latestStableName)
                    .put("latestStableRawTag", state.latestStableRawTag)
                    .put("latestStableUrl", state.latestStableUrl)
                    .put("latestStableUpdatedAtMillis", state.latestStableUpdatedAtMillis)
                    .put("latestPreName", state.latestPreName)
                    .put("latestPreRawTag", state.latestPreRawTag)
                    .put("latestPreUrl", state.latestPreUrl)
                    .put("latestPreUpdatedAtMillis", state.latestPreUpdatedAtMillis)
                    .put("hasStableRelease", state.hasStableRelease)
                    .put("hasUpdate", state.hasUpdate)
                    .put("message", state.message)
                    .put("isPreRelease", state.isPreRelease)
                    .put("preReleaseInfo", state.preReleaseInfo)
                    .put("showPreReleaseInfo", state.showPreReleaseInfo)
                    .put("hasPreReleaseUpdate", state.hasPreReleaseUpdate)
                    .put("recommendsPreRelease", state.recommendsPreRelease)
                    .put("releaseHint", state.releaseHint)
                    .put("sourceStrategyId", state.sourceStrategyId)
            )
        }
        kv().encode(KEY_CHECK_CACHE, obj.toString())
        kv().encode(KEY_LAST_REFRESH_MS, lastRefreshMs)
    }

    fun clearCheckCache() {
        val store = kv()
        store.removeValueForKey(KEY_CHECK_CACHE)
        store.removeValueForKey(KEY_LAST_REFRESH_MS)
        store.trim()
    }

    fun cachedCheckCount(): Int = loadCheckCache().first.size

    fun storageFootprintBytes(): Long = kv().totalSize()

    fun actualDataBytes(): Long = kv().actualSize()

    fun cacheBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        val cacheJsonBytes = snapshot.checkCache.values.sumOf { state ->
            listOf(
                state.localVersion,
                state.latestTag,
                state.latestStableName,
                state.latestStableRawTag,
                state.latestStableUrl,
                state.latestPreName,
                state.latestPreRawTag,
                state.latestPreUrl,
                state.message,
                state.preReleaseInfo,
                state.releaseHint,
                state.sourceStrategyId
            ).sumOf { it.length.toLong() * 2 } + 64L
        }
        return cacheJsonBytes + 16L
    }

    fun configBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        val trackedBytes = snapshot.items.sumOf { item ->
            (item.repoUrl.length + item.owner.length + item.repo.length + item.packageName.length + item.appLabel.length)
                .toLong() * 2 + 32L
        }
        val prefsBytes = snapshot.lookupConfig.apiToken.length.toLong() * 2 + 96L
        return trackedBytes + prefsBytes
    }

    fun shouldAutoRefreshOnceInSession(): Boolean = !didAutoRefreshInSession

    fun markAutoRefreshDoneInSession() {
        didAutoRefreshInSession = true
    }

    fun loadRefreshIntervalHours(defaultValue: Int = 3): Int {
        val value = kv().decodeInt(KEY_REFRESH_INTERVAL_HOURS, defaultValue)
        return if (value in setOf(1, 3, 6, 12)) value else defaultValue
    }

    fun saveRefreshIntervalHours(hours: Int) {
        if (hours in setOf(1, 3, 6, 12)) {
            kv().encode(KEY_REFRESH_INTERVAL_HOURS, hours)
        }
    }

    fun loadLookupConfig(): GitHubLookupConfig {
        return GitHubLookupConfig(
            selectedStrategy = GitHubLookupStrategyOption.fromStorageId(
                kv().decodeString(KEY_LOOKUP_STRATEGY).orEmpty()
            ),
            apiToken = kv().decodeString(KEY_GITHUB_API_TOKEN).orEmpty().trim(),
            checkAllTrackedPreReleases = kv().decodeBool(KEY_CHECK_ALL_TRACKED_PRE_RELEASES, false),
            aggressiveApkFiltering = kv().decodeBool(KEY_AGGRESSIVE_APK_FILTERING, false),
            shareImportLinkageEnabled = kv().decodeBool(KEY_SHARE_IMPORT_LINKAGE_ENABLED, false),
            onlineShareTargetPackage = kv().decodeString(KEY_ONLINE_SHARE_TARGET_PACKAGE).orEmpty().trim(),
            preferredDownloaderPackage = kv().decodeString(KEY_PREFERRED_DOWNLOADER_PACKAGE).orEmpty().trim()
        )
    }

    fun saveLookupConfig(config: GitHubLookupConfig) {
        kv().encode(KEY_LOOKUP_STRATEGY, config.selectedStrategy.storageId)
        kv().encode(KEY_GITHUB_API_TOKEN, config.apiToken.trim())
        kv().encode(KEY_CHECK_ALL_TRACKED_PRE_RELEASES, config.checkAllTrackedPreReleases)
        kv().encode(KEY_AGGRESSIVE_APK_FILTERING, config.aggressiveApkFiltering)
        kv().encode(KEY_SHARE_IMPORT_LINKAGE_ENABLED, config.shareImportLinkageEnabled)
        kv().encode(KEY_ONLINE_SHARE_TARGET_PACKAGE, config.onlineShareTargetPackage.trim())
        kv().encode(KEY_PREFERRED_DOWNLOADER_PACKAGE, config.preferredDownloaderPackage.trim())
    }

    private fun parseTrackedItem(obj: JSONObject): GitHubTrackedApp? {
        val repoUrl = obj.optString("repoUrl").trim()
        val owner = obj.optString("owner").trim()
        val repo = obj.optString("repo").trim()
        val packageName = obj.optString("packageName").trim()
        val appLabel = obj.optString("appLabel").trim()
        if (repoUrl.isBlank() || owner.isBlank() || repo.isBlank() || packageName.isBlank()) {
            return null
        }
        return GitHubTrackedApp(
            repoUrl = repoUrl,
            owner = owner,
            repo = repo,
            packageName = packageName,
            appLabel = appLabel.ifBlank { packageName },
            preferPreRelease = when {
                obj.has("preferPreRelease") -> obj.optBoolean("preferPreRelease", false)
                obj.has("checkPreRelease") -> obj.optBoolean("checkPreRelease", false)
                else -> false
            },
            alwaysShowLatestReleaseDownloadButton = when {
                obj.has("alwaysShowLatestReleaseDownloadButton") ->
                    obj.optBoolean("alwaysShowLatestReleaseDownloadButton", false)
                obj.has("alwaysShowLatestReleaseDownload") ->
                    obj.optBoolean("alwaysShowLatestReleaseDownload", false)
                else -> false
            }
        )
    }

    private fun trackedItemToJson(item: GitHubTrackedApp): JSONObject {
        return JSONObject()
            .put("repoUrl", item.repoUrl)
            .put("owner", item.owner)
            .put("repo", item.repo)
            .put("packageName", item.packageName)
            .put("appLabel", item.appLabel)
            .put("preferPreRelease", item.preferPreRelease)
            .put("checkPreRelease", item.preferPreRelease)
            .put("alwaysShowLatestReleaseDownloadButton", item.alwaysShowLatestReleaseDownloadButton)
            .put("alwaysShowLatestReleaseDownload", item.alwaysShowLatestReleaseDownloadButton)
    }
}
