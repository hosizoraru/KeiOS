package os.kei.feature.github.data.local

import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import os.kei.core.json.KeiJson
import os.kei.core.json.encodeCompact
import os.kei.core.json.hasNonNull
import os.kei.core.json.jsonObjectOrNull
import os.kei.core.json.jsonPrimitiveOrNull
import os.kei.core.json.optArray
import os.kei.core.json.optBoolean
import os.kei.core.json.optInt
import os.kei.core.json.optLong
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonArrayOrNull
import os.kei.core.json.parseJsonElementOrNull
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.prefs.KeiMmkv
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.GITHUB_FDROID_DEFAULT_REFRESH_INTERVAL_HOURS
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.feature.github.model.GitHubCheckCacheEntry
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubProfileDepth
import os.kei.feature.github.model.GitHubReleaseDecisionNote
import os.kei.feature.github.model.GitHubReleaseNotesMode
import os.kei.feature.github.model.GitHubRepositoryProfileSnapshot
import os.kei.feature.github.model.GitHubShareImportFlowMode
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedLocalAppType
import os.kei.feature.github.model.GitHubTrackedPreciseApkVersionMode
import os.kei.feature.github.model.GitHubTrackedUpdateIntervalMode
import os.kei.feature.github.model.defaultKeiOsTrackedApp
import os.kei.feature.github.model.defaultRepositoryProfilePurpose
import os.kei.feature.github.model.githubProfileSourceSignature
import os.kei.feature.github.model.isDirectApkTrack
import os.kei.feature.github.model.isFdroidRepositoryTrack
import os.kei.feature.github.model.isGitHubRepositoryTrack
import os.kei.feature.github.model.isGitRepositoryTrack
import os.kei.feature.github.model.requiredCapabilities
import os.kei.feature.github.model.resolvedRefreshTimestamp
import os.kei.feature.github.model.withSourceModeConstraints

data class GitHubTrackSnapshot(
    val items: List<GitHubTrackedApp> = emptyList(),
    val checkCache: Map<String, GitHubCheckCacheEntry> = emptyMap(),
    val profileCache: Map<String, GitHubRepositoryProfileSnapshot> = emptyMap(),
    val lastRefreshMs: Long = 0L,
    val refreshIntervalHours: Int = 3,
    val lookupConfig: GitHubLookupConfig = GitHubLookupConfig(),
    val pendingShareImportTrack: GitHubPendingShareImportTrackRecord? = null,
    val trackedFirstInstallAtByPackage: Map<String, Long> = emptyMap(),
    val trackedAddedAtById: Map<String, Long> = emptyMap(),
    val trackedModifiedAtById: Map<String, Long> = emptyMap()
)

data class GitHubTrackedItemsImportPayload(
    val items: List<GitHubTrackedApp> = emptyList(),
    val sourceCount: Int = 0,
    val invalidCount: Int = 0,
    val duplicateCount: Int = 0,
    val schemaVersion: Int = 0,
    val format: String = "",
    val exportedAtMillis: Long = 0L
)

data class GitHubTrackedItemsOptionCounts(
    val preferPreReleaseCount: Int = 0,
    val latestReleaseDownloadCount: Int = 0,
    val actionsUpdateCount: Int = 0,
    val updateIntervalOverrideCount: Int = 0,
    val preciseApkVersionOverrideCount: Int = 0,
    val archivedOrForkCount: Int = 0
)

data class GitHubTrackedItemsSourceCounts(
    val githubRepositoryCount: Int = 0,
    val gitRepositoryCount: Int = 0,
    val directApkCount: Int = 0,
    val fdroidRepositoryCount: Int = 0
)

data class GitHubPendingShareImportTrackRecord(
    val projectUrl: String,
    val owner: String,
    val repo: String,
    val releaseTag: String = "",
    val assetName: String = "",
    val packageName: String = "",
    val versionName: String = "",
    val targetDisplayName: String = "",
    val armedAtMillis: Long
)

data class GitHubAppPickerPreferences(
    val includeUserApps: Boolean = true,
    val includeSystemApps: Boolean = false,
    val includeTrackedApps: Boolean = false,
    val sortModeId: String = "name",
    val sortDirectionId: String = "ascending",
    val includeDisabledOrFrozenApps: Boolean = false,
)

object GitHubTrackStore {
    private const val TRACK_EXPORT_SCHEMA_VERSION = 4
    private const val TRACK_EXPORT_FORMAT = "keios.github.tracked/v4"
    private const val KV_ID = "github_track_store"
    private const val KEY_ITEMS = "tracked_items"
    private const val KEY_CHECK_CACHE = "tracked_check_cache"
    private const val KEY_PROFILE_CACHE = "tracked_profile_cache"
    private const val KEY_LAST_REFRESH_MS = "last_full_refresh_ms"
    private val checkCacheLock = Any()
    private const val KEY_HIDE_TAG_ONLY_RELEASES = "hide_tag_only_releases"
    private const val KEY_REFRESH_INTERVAL_HOURS = "refresh_interval_hours"
    private const val KEY_LOOKUP_STRATEGY = "lookup_strategy"
    private const val KEY_ACTIONS_LOOKUP_STRATEGY = "actions_lookup_strategy"
    private const val KEY_GITHUB_API_TOKEN = "github_api_token"
    private const val KEY_CHECK_ALL_TRACKED_PRE_RELEASES = "check_all_tracked_pre_releases"
    private const val KEY_CHECK_ALL_DIRECT_APK_PRE_RELEASES =
        "github_check_all_direct_apk_pre_releases"
    private const val KEY_AGGRESSIVE_APK_FILTERING = "github_aggressive_apk_filtering"
    private const val KEY_PRECISE_APK_VERSION_ENABLED = "github_precise_apk_version_enabled"
    private const val KEY_SCAN_SYSTEM_APPS_BY_DEFAULT = "github_scan_system_apps_by_default"
    private const val KEY_PROFILE_DEPTH = "github_profile_depth"
    private const val KEY_SHARE_IMPORT_FLOW_MODE = "github_share_import_flow_mode"
    private const val KEY_APP_MANAGED_SHARE_INSTALL_ENABLED =
        "github_app_managed_share_install_enabled"
    private const val KEY_FOREGROUND_MANAGED_DOWNLOAD_BOOST_ENABLED =
        "github_foreground_managed_download_boost_enabled"
    private const val KEY_ONLINE_SHARE_TARGET_PACKAGE = "github_online_share_target_package"
    private const val KEY_PREFERRED_DOWNLOADER_PACKAGE = "github_preferred_downloader_package"
    private const val KEY_DECISION_ASSIST_ENABLED = "github_decision_assist_enabled"
    private const val KEY_REPOSITORY_HEALTH_CARD_ENABLED = "github_repository_health_card_enabled"
    private const val KEY_APK_TRUST_CHECK_ENABLED = "github_apk_trust_check_enabled"
    private const val KEY_RELEASE_NOTES_MODE = "github_release_notes_mode"
    private const val KEY_FDROID_COMMON_REPO_IDS = "github_fdroid_common_repo_ids"
    private const val KEY_PENDING_SHARE_IMPORT_TRACK = "github_pending_share_import_track"
    private const val KEY_TRACKED_FIRST_INSTALL_AT_BY_PACKAGE = "github_tracked_first_install_at_by_package"
    private const val KEY_TRACKED_ADDED_AT_BY_ID = "github_tracked_added_at_by_id"
    private const val KEY_TRACKED_MODIFIED_AT_BY_ID = "github_tracked_modified_at_by_id"
    private const val KEY_APP_PICKER_INCLUDE_USER_APPS = "github_app_picker_include_user_apps"
    private const val KEY_APP_PICKER_INCLUDE_SYSTEM_APPS = "github_app_picker_include_system_apps"
    private const val KEY_APP_PICKER_INCLUDE_TRACKED_APPS = "github_app_picker_include_tracked_apps"
    private const val KEY_APP_PICKER_INCLUDE_DISABLED_OR_FROZEN_APPS = "github_app_picker_include_disabled_or_frozen_apps"
    private const val KEY_APP_PICKER_SORT_MODE = "github_app_picker_sort_mode"
    private const val KEY_APP_PICKER_SORT_DIRECTION = "github_app_picker_sort_direction"

    @Volatile
    private var didAutoRefreshInSession: Boolean = false
    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    private fun kv(): MMKV = store

    fun load(): List<GitHubTrackedApp> {
        val store = kv()
        if (!store.containsKey(KEY_ITEMS)) {
            return seedDefaultTrackedItems()
        }
        val raw = store.decodeString(KEY_ITEMS).orEmpty()
        if (raw.isBlank()) return emptyList()
        val array = raw.parseJsonArrayOrNull() ?: return emptyList()
        val parsedItems = buildList {
            array.forEach { element ->
                val obj = element.jsonObjectOrNull() ?: return@forEach
                parseTrackedItem(obj)?.let(::add)
            }
        }
        val normalizedItems = normalizeTrackedItems(parsedItems)
        if (normalizedItems != parsedItems) {
            saveNormalizedItemsIfRawUnchanged(raw, normalizedItems)
        }
        return normalizedItems
    }

    private fun seedDefaultTrackedItems(): List<GitHubTrackedApp> {
        val defaults = listOf(defaultKeiOsTrackedApp())
        save(defaults)
        return defaults
    }

    fun save(items: List<GitHubTrackedApp>) {
        val normalizedItems = normalizeTrackedItems(items)
        saveNormalizedItems(normalizedItems)
    }

    private fun saveNormalizedItemsIfRawUnchanged(
        expectedRaw: String,
        normalizedItems: List<GitHubTrackedApp>
    ) {
        val store = kv()
        if (store.decodeString(KEY_ITEMS).orEmpty() != expectedRaw) return
        saveNormalizedItems(normalizedItems)
    }

    private fun saveNormalizedItems(normalizedItems: List<GitHubTrackedApp>) {
        val array = buildJsonArray {
            normalizedItems.forEach { item ->
                add(trackedItemToJson(item))
            }
        }
        kv().encode(KEY_ITEMS, array.encodeCompact())
    }

    fun loadPendingShareImportTrack(): GitHubPendingShareImportTrackRecord? {
        val raw = kv().decodeString(KEY_PENDING_SHARE_IMPORT_TRACK).orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val obj = raw.parseJsonObjectOrNull() ?: return@runCatching null
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
                packageName = obj.optString("packageName").trim(),
                versionName = obj.optString("versionName").trim(),
                targetDisplayName = obj.optString("targetDisplayName").trim(),
                armedAtMillis = armedAtMillis
            )
        }.getOrNull()
    }

    fun savePendingShareImportTrack(record: GitHubPendingShareImportTrackRecord?) {
        if (record == null) {
            kv().removeValueForKey(KEY_PENDING_SHARE_IMPORT_TRACK)
            return
        }
        val payload = buildJsonObject {
            put("projectUrl", record.projectUrl)
            put("owner", record.owner)
            put("repo", record.repo)
            put("releaseTag", record.releaseTag)
            put("assetName", record.assetName)
            put("packageName", record.packageName)
            put("versionName", record.versionName)
            put("targetDisplayName", record.targetDisplayName)
            put("armedAtMillis", record.armedAtMillis)
        }
        kv().encode(KEY_PENDING_SHARE_IMPORT_TRACK, payload.encodeCompact())
    }

    fun loadAppPickerPreferences(): GitHubAppPickerPreferences {
        val store = kv()
        return GitHubAppPickerPreferences(
            includeUserApps = store.decodeBool(KEY_APP_PICKER_INCLUDE_USER_APPS, true),
            includeSystemApps = store.decodeBool(KEY_APP_PICKER_INCLUDE_SYSTEM_APPS, false),
            includeTrackedApps = store.decodeBool(KEY_APP_PICKER_INCLUDE_TRACKED_APPS, false),
            includeDisabledOrFrozenApps = store.decodeBool(KEY_APP_PICKER_INCLUDE_DISABLED_OR_FROZEN_APPS, false),
            sortModeId = store.decodeString(KEY_APP_PICKER_SORT_MODE, "name").orEmpty(),
            sortDirectionId = store.decodeString(
                KEY_APP_PICKER_SORT_DIRECTION,
                "ascending"
            ).orEmpty()
        )
    }

    fun saveAppPickerPreferences(preferences: GitHubAppPickerPreferences) {
        val store = kv()
        store.encode(KEY_APP_PICKER_INCLUDE_USER_APPS, preferences.includeUserApps)
        store.encode(KEY_APP_PICKER_INCLUDE_SYSTEM_APPS, preferences.includeSystemApps)
        store.encode(KEY_APP_PICKER_INCLUDE_TRACKED_APPS, preferences.includeTrackedApps)
        store.encode(KEY_APP_PICKER_INCLUDE_DISABLED_OR_FROZEN_APPS, preferences.includeDisabledOrFrozenApps)
        store.encode(KEY_APP_PICKER_SORT_MODE, preferences.sortModeId)
        store.encode(KEY_APP_PICKER_SORT_DIRECTION, preferences.sortDirectionId)
    }

    fun loadTrackedFirstInstallAtByPackage(): Map<String, Long> {
        return loadPositiveLongMap(KEY_TRACKED_FIRST_INSTALL_AT_BY_PACKAGE)
    }

    fun saveTrackedFirstInstallAtByPackage(values: Map<String, Long>) {
        savePositiveLongMap(KEY_TRACKED_FIRST_INSTALL_AT_BY_PACKAGE, values)
    }

    fun loadTrackedAddedAtById(): Map<String, Long> {
        return loadPositiveLongMap(KEY_TRACKED_ADDED_AT_BY_ID)
    }

    fun saveTrackedAddedAtById(values: Map<String, Long>) {
        savePositiveLongMap(KEY_TRACKED_ADDED_AT_BY_ID, values)
    }

    fun loadTrackedModifiedAtById(): Map<String, Long> {
        return loadPositiveLongMap(KEY_TRACKED_MODIFIED_AT_BY_ID)
    }

    fun saveTrackedModifiedAtById(values: Map<String, Long>) {
        savePositiveLongMap(KEY_TRACKED_MODIFIED_AT_BY_ID, values)
    }

    private fun loadPositiveLongMap(key: String): Map<String, Long> {
        val raw = kv().decodeString(key).orEmpty()
        if (raw.isBlank()) return emptyMap()
        val obj = raw.parseJsonObjectOrNull() ?: return emptyMap()
        return buildMap {
            obj.forEach { (rawName, element) ->
                val name = rawName.trim()
                if (name.isBlank()) return@forEach
                val value = element.jsonPrimitiveOrNull()?.longOrNull ?: return@forEach
                if (value > 0L) {
                    put(name, value)
                }
            }
        }
    }

    private fun savePositiveLongMap(key: String, values: Map<String, Long>) {
        if (values.isEmpty()) {
            kv().removeValueForKey(key)
            return
        }
        val payload = buildJsonObject {
            values.forEach { (rawName, rawValue) ->
                val name = rawName.trim()
                if (name.isBlank() || rawValue <= 0L) return@forEach
                put(name, rawValue)
            }
        }
        if (payload.isEmpty()) {
            kv().removeValueForKey(key)
            return
        }
        kv().encode(key, payload.encodeCompact())
    }

    fun buildTrackedItemsExportJson(
        items: List<GitHubTrackedApp>,
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val normalizedItems = normalizeTrackedItems(items)
        val array = buildJsonArray {
            normalizedItems.forEach { item ->
                add(trackedItemToJson(item))
            }
        }
        val optionCounts = calculateTrackedItemsOptionCounts(normalizedItems)
        val sourceCounts = calculateTrackedItemsSourceCounts(normalizedItems)
        val payload = buildJsonObject {
            put("format", TRACK_EXPORT_FORMAT)
            put("schemaVersion", TRACK_EXPORT_SCHEMA_VERSION)
            put("schemaFeatures", trackedItemsExportSchemaFeatures())
            put("exportedAtMillis", exportedAtMillis)
            put("itemCount", normalizedItems.size)
            put("sourceCounts", sourceCounts.toJson())
            put("optionCounts", optionCounts.toJson())
            put("items", array)
        }
        return KeiJson.pretty.encodeToString(JsonObject.serializer(), payload)
    }

    private fun trackedItemsExportSchemaFeatures(): JsonObject =
        buildJsonObject {
            put("fdroidRepositorySource", true)
            put("fdroidDefaultUpdateIntervalHours", GITHUB_FDROID_DEFAULT_REFRESH_INTERVAL_HOURS)
            put("fdroidCustomUpdateIntervalMaxHours", 24)
            put("externalBuildUntilRelease", true)
            put("webDavMergeCompatible", true)
        }

    fun parseTrackedItemsImport(raw: String): GitHubTrackedItemsImportPayload {
        val normalized = raw.trim()
        if (normalized.isBlank()) {
            return GitHubTrackedItemsImportPayload()
        }
        var schemaVersion = 0
        var format = ""
        var exportedAtMillis = 0L
        val rootArray = when (val rootElement = normalized.parseJsonElementOrNull()) {
            is JsonObject -> {
                val root = rootElement
                schemaVersion = root.optInt("schemaVersion", 0)
                format = root.optString("format").trim()
                exportedAtMillis = root.optLong("exportedAtMillis", 0L)
                when {
                    root.hasNonNull("items") -> root.optArray("items")
                    root.hasNonNull("trackedItems") -> root.optArray("trackedItems")
                    else -> null
                } ?: JsonArray(emptyList())
            }
            is JsonArray -> rootElement
            else -> throw IllegalArgumentException("unsupported github track import format")
        }
        val parsedItems = mutableListOf<GitHubTrackedApp>()
        val seenIds = linkedSetOf<String>()
        var invalidCount = 0
        var duplicateCount = 0
        rootArray.forEach { element ->
            val itemObject = element.jsonObjectOrNull()
            val item = itemObject?.let(::parseTrackedItem)
            if (item == null) {
                invalidCount += 1
                return@forEach
            }
            if (!seenIds.add(item.id)) {
                duplicateCount += 1
            }
            parsedItems += item
        }
        return GitHubTrackedItemsImportPayload(
            items = normalizeTrackedItems(parsedItems),
            sourceCount = rootArray.size,
            invalidCount = invalidCount,
            duplicateCount = duplicateCount,
            schemaVersion = schemaVersion,
            format = format,
            exportedAtMillis = exportedAtMillis
        )
    }

    fun calculateTrackedItemsOptionCounts(
        items: List<GitHubTrackedApp>
    ): GitHubTrackedItemsOptionCounts {
        val normalizedItems = normalizeTrackedItems(items)
        return GitHubTrackedItemsOptionCounts(
            preferPreReleaseCount = normalizedItems.count { it.preferPreRelease },
            latestReleaseDownloadCount = normalizedItems.count {
                it.alwaysShowLatestReleaseDownloadButton
            },
            actionsUpdateCount = normalizedItems.count { it.checkActionsUpdates },
            updateIntervalOverrideCount = normalizedItems.count {
                it.updateIntervalMode != GitHubTrackedUpdateIntervalMode.FollowGlobal
            },
            preciseApkVersionOverrideCount = normalizedItems.count {
                it.preciseApkVersionMode != GitHubTrackedPreciseApkVersionMode.FollowGlobal
            },
            archivedOrForkCount = normalizedItems.count { it.repositoryArchived || it.repositoryFork }
        )
    }

    fun calculateTrackedItemsSourceCounts(
        items: List<GitHubTrackedApp>
    ): GitHubTrackedItemsSourceCounts {
        val normalizedItems = normalizeTrackedItems(items)
        return GitHubTrackedItemsSourceCounts(
            githubRepositoryCount = normalizedItems.count { it.isGitHubRepositoryTrack() },
            gitRepositoryCount = normalizedItems.count { it.isGitRepositoryTrack() },
            directApkCount = normalizedItems.count { it.isDirectApkTrack() },
            fdroidRepositoryCount = normalizedItems.count { it.isFdroidRepositoryTrack() }
        )
    }

    fun loadCheckCache(): Pair<Map<String, GitHubCheckCacheEntry>, Long> =
        synchronized(checkCacheLock) {
            loadCheckCacheLocked(includeRepositoryProfiles = true)
        }

    private fun loadCheckCacheLocked(
        includeRepositoryProfiles: Boolean,
    ): Pair<Map<String, GitHubCheckCacheEntry>, Long> {
        val raw = kv().decodeString(KEY_CHECK_CACHE).orEmpty()
        val ts = kv().decodeLong(KEY_LAST_REFRESH_MS, 0L)
        val profileCache =
            if (includeRepositoryProfiles) {
                loadProfileCache()
            } else {
                emptyMap()
            }
        if (raw.isBlank()) return emptyMap<String, GitHubCheckCacheEntry>() to 0L
        val map =
            raw.parseJsonObjectOrNull()
                ?.let { obj ->
                    buildMap {
                        obj.forEach { (id, element) ->
                            val item = element.jsonObjectOrNull() ?: return@forEach
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
                                    latestStableAuthorAvatarUrl = item.optString(
                                        "latestStableAuthorAvatarUrl"
                                    ),
                                    latestStableUpdatedAtMillis = item.optLong(
                                        "latestStableUpdatedAtMillis",
                                        -1L
                                    ),
                                    latestPreName = item.optString("latestPreName").ifBlank {
                                        item.optString("preReleaseInfo")
                                    },
                                    latestPreRawTag = item.optString("latestPreRawTag"),
                                    latestPreUrl = item.optString("latestPreUrl"),
                                    latestPreAuthorAvatarUrl = item.optString(
                                        "latestPreAuthorAvatarUrl"
                                    ),
                                    latestPreUpdatedAtMillis = item.optLong(
                                        "latestPreUpdatedAtMillis",
                                        -1L
                                    ),
                                    hasStableRelease =
                                        if (item.hasNonNull("hasStableRelease")) {
                                            item.optBoolean("hasStableRelease", true)
                                        } else {
                                            item.optString("latestStableRawTag").isNotBlank() ||
                                                item.optString("latestTag").isNotBlank()
                                        },
                                    hasUpdate =
                                        if (item.hasNonNull("hasUpdate")) {
                                            item.optBoolean("hasUpdate")
                                        } else {
                                            null
                                        },
                                    message = item.optString("message"),
                                    isPreRelease = item.optBoolean("isPreRelease", false),
                                    preReleaseInfo = item.optString("preReleaseInfo"),
                                    showPreReleaseInfo = item.optBoolean(
                                        "showPreReleaseInfo",
                                        false
                                    ),
                                    hasPreReleaseUpdate = item.optBoolean(
                                        "hasPreReleaseUpdate",
                                        false
                                    ),
                                    recommendsPreRelease = item.optBoolean(
                                        "recommendsPreRelease",
                                        false
                                    ),
                                    releaseHint = item.optString("releaseHint"),
                                    repositoryArchived = item.optBoolean(
                                        "repositoryArchived",
                                        false
                                    ),
                                    repositoryFork = item.optBoolean("repositoryFork", false),
                                    repositoryPushedAtMillis = item.optLong(
                                        "repositoryPushedAtMillis",
                                        -1L
                                    ),
                                    upstreamFullName = item.optString("upstreamFullName"),
                                    upstreamArchived = item.optBoolean("upstreamArchived", false),
                                    upstreamPushedAtMillis = item.optLong(
                                        "upstreamPushedAtMillis",
                                        -1L
                                    ),
                                    repositoryProfile =
                                        if (includeRepositoryProfiles) {
                                            parseGitHubRepositoryProfileSnapshot(
                                                item.optObject("repositoryProfile")
                                            ) ?: profileCache[id]
                                        } else {
                                            null
                                        },
                                    sourceStrategyId = item.optString("sourceStrategyId"),
                                    sourceConfigSignature = item.optString(
                                        "sourceConfigSignature"
                                    ),
                                    latestStableApkVersion = parseRemoteApkVersionInfo(
                                        item.optObject("latestStableApkVersion")
                                    ),
                                    latestPreApkVersion = parseRemoteApkVersionInfo(
                                        item.optObject("latestPreApkVersion")
                                    ),
                                    directApkRemoteHealth = parseDirectApkRemoteHealth(
                                        item.optString("directApkRemoteHealth")
                                    ),
                                    directApkRemoteHealthMessage = item.optString(
                                        "directApkRemoteHealthMessage"
                                    ),
                                    directApkRemoteCheckedAtMillis = item.optLong(
                                        "directApkRemoteCheckedAtMillis",
                                        -1L
                                    ),
                                    checkedAtMillis =
                                        if (item.hasNonNull("checkedAtMillis")) {
                                            item.optLong("checkedAtMillis", -1L)
                                        } else {
                                            ts
                                        },
                                    decisionNote = releaseDecisionNoteFromJson(
                                        item.optObject("decision")
                                    )
                                )
                            )
                        }
                    }
                } ?: emptyMap()
        return map to map.resolvedRefreshTimestamp(ts)
    }

    fun loadProfileCache(): Map<String, GitHubRepositoryProfileSnapshot> {
        val raw = kv().decodeString(KEY_PROFILE_CACHE).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.parseJsonObjectOrNull()
            ?.let { obj ->
                buildMap {
                    obj.forEach { (id, element) ->
                        val profile = parseGitHubRepositoryProfileSnapshot(element.jsonObjectOrNull())
                            ?: return@forEach
                        put(id, profile)
                    }
                }
            } ?: emptyMap()
    }

    fun loadSnapshot(): GitHubTrackSnapshot {
        val lookupConfig = loadLookupConfig()
        val (checkCache, lastRefreshMs) = loadCheckCache()
        val profilePurpose = lookupConfig.defaultRepositoryProfilePurpose()
        val requiredProfileCapabilities = profilePurpose.requiredCapabilities(
            lookupConfig.profileDepth
        )
        val activeProfileSignature = lookupConfig.githubProfileSourceSignature(
            requiredProfileCapabilities
        )
        val activeProfileCache = loadProfileCache().filterValues { profile ->
            profile.isFreshFor(
                activeSourceConfigSignature = activeProfileSignature,
                requiredCapabilities = requiredProfileCapabilities
            )
        }
        val mergedCheckCache = checkCache.mapValues { (id, entry) ->
            val freshProfile = entry.repositoryProfile
                ?.takeIf {
                    it.isFreshFor(
                        activeSourceConfigSignature = activeProfileSignature,
                        requiredCapabilities = requiredProfileCapabilities
                    )
                }
                ?: activeProfileCache[id]
            entry.copy(repositoryProfile = freshProfile)
        }
        return GitHubTrackSnapshot(
            items = load(),
            checkCache = mergedCheckCache,
            profileCache = activeProfileCache,
            lastRefreshMs = lastRefreshMs,
            refreshIntervalHours = loadRefreshIntervalHours(),
            lookupConfig = lookupConfig,
            pendingShareImportTrack = loadPendingShareImportTrack(),
            trackedFirstInstallAtByPackage = loadTrackedFirstInstallAtByPackage(),
            trackedAddedAtById = loadTrackedAddedAtById(),
            trackedModifiedAtById = loadTrackedModifiedAtById()
        )
    }

    fun loadLocalSummarySnapshot(): GitHubTrackSnapshot {
        val lookupConfig = loadLookupConfig()
        val (checkCache, lastRefreshMs) =
            synchronized(checkCacheLock) {
                loadCheckCacheLocked(includeRepositoryProfiles = false)
            }
        return GitHubTrackSnapshot(
            items = load(),
            checkCache = checkCache,
            lastRefreshMs = lastRefreshMs,
            refreshIntervalHours = loadRefreshIntervalHours(),
            lookupConfig = lookupConfig,
            pendingShareImportTrack = loadPendingShareImportTrack(),
        )
    }

    fun saveCheckCache(states: Map<String, GitHubCheckCacheEntry>, lastRefreshMs: Long): Long =
        synchronized(checkCacheLock) {
            saveCheckCacheLocked(states, lastRefreshMs)
        }

    fun mergeCheckCache(
        entries: Map<String, GitHubCheckCacheEntry>,
        refreshTimestamp: Long
    ): Long =
        synchronized(checkCacheLock) {
            if (entries.isEmpty()) {
                loadCheckCacheLocked(includeRepositoryProfiles = true).second
            } else {
                val (currentCache, currentRefreshTimestamp) =
                    loadCheckCacheLocked(includeRepositoryProfiles = true)
                saveCheckCacheLocked(
                    states = currentCache + entries,
                    lastRefreshMs = currentRefreshTimestamp.takeIf { it > 0L }
                        ?: refreshTimestamp,
                )
            }
        }

    fun markAllTrackedChecksDue(): Int =
        synchronized(checkCacheLock) {
            val trackedItems = load()
            if (trackedItems.isEmpty()) return@synchronized 0
            val (currentCache, currentRefreshTimestamp) =
                loadCheckCacheLocked(includeRepositoryProfiles = true)
            val expiredCache =
                buildDueCheckCache(
                    trackedIds = trackedItems.map { item -> item.id },
                    currentCache = currentCache,
                )
            saveCheckCacheLocked(
                states = expiredCache,
                lastRefreshMs = currentRefreshTimestamp,
            )
            trackedItems.size
        }

    fun removeCheckCacheEntries(trackIds: Set<String>, refreshTimestamp: Long = 0L): Long =
        synchronized(checkCacheLock) {
            if (trackIds.isEmpty()) {
                loadCheckCacheLocked(includeRepositoryProfiles = true).second
            } else {
                val (currentCache, currentRefreshTimestamp) =
                    loadCheckCacheLocked(includeRepositoryProfiles = true)
                val nextCache = currentCache.filterKeys { it !in trackIds }
                if (nextCache.size == currentCache.size) {
                    currentRefreshTimestamp
                } else {
                    saveCheckCacheLocked(
                        states = nextCache,
                        lastRefreshMs = currentRefreshTimestamp.takeIf { it > 0L }
                            ?: refreshTimestamp,
                    )
                }
            }
        }

    private fun saveCheckCacheLocked(
        states: Map<String, GitHubCheckCacheEntry>,
        lastRefreshMs: Long,
    ): Long {
        if (states.isEmpty()) {
            val store = kv()
            store.removeValueForKey(KEY_CHECK_CACHE)
            store.removeValueForKey(KEY_PROFILE_CACHE)
            store.removeValueForKey(KEY_LAST_REFRESH_MS)
            return 0L
        }
        val resolvedLastRefreshMs = states.resolvedRefreshTimestamp(lastRefreshMs)
        val obj = buildJsonObject {
            states.forEach { (id, state) ->
                put(
                    id,
                    buildJsonObject {
                        put("localVersion", state.localVersion)
                        put("localVersionCode", state.localVersionCode)
                        put("latestTag", state.latestTag)
                        put("latestStableName", state.latestStableName)
                        put("latestStableRawTag", state.latestStableRawTag)
                        put("latestStableUrl", state.latestStableUrl)
                        put("latestStableAuthorAvatarUrl", state.latestStableAuthorAvatarUrl)
                        put("latestStableUpdatedAtMillis", state.latestStableUpdatedAtMillis)
                        put("latestPreName", state.latestPreName)
                        put("latestPreRawTag", state.latestPreRawTag)
                        put("latestPreUrl", state.latestPreUrl)
                        put("latestPreAuthorAvatarUrl", state.latestPreAuthorAvatarUrl)
                        put("latestPreUpdatedAtMillis", state.latestPreUpdatedAtMillis)
                        put("hasStableRelease", state.hasStableRelease)
                        state.hasUpdate?.let { put("hasUpdate", it) }
                        put("message", state.message)
                        put("isPreRelease", state.isPreRelease)
                        put("preReleaseInfo", state.preReleaseInfo)
                        put("showPreReleaseInfo", state.showPreReleaseInfo)
                        put("hasPreReleaseUpdate", state.hasPreReleaseUpdate)
                        put("recommendsPreRelease", state.recommendsPreRelease)
                        put("releaseHint", state.releaseHint)
                        put("repositoryArchived", state.repositoryArchived)
                        put("repositoryFork", state.repositoryFork)
                        put("repositoryPushedAtMillis", state.repositoryPushedAtMillis)
                        put("upstreamFullName", state.upstreamFullName)
                        put("upstreamArchived", state.upstreamArchived)
                        put("upstreamPushedAtMillis", state.upstreamPushedAtMillis)
                        state.repositoryProfile?.toCacheJson()?.let { put("repositoryProfile", it) }
                        put("sourceStrategyId", state.sourceStrategyId)
                        put("sourceConfigSignature", state.sourceConfigSignature)
                        remoteApkVersionInfoToJson(state.latestStableApkVersion)
                            ?.let { put("latestStableApkVersion", it) }
                        remoteApkVersionInfoToJson(state.latestPreApkVersion)
                            ?.let { put("latestPreApkVersion", it) }
                        put("directApkRemoteHealth", state.directApkRemoteHealth.name)
                        put("directApkRemoteHealthMessage", state.directApkRemoteHealthMessage)
                        put("directApkRemoteCheckedAtMillis", state.directApkRemoteCheckedAtMillis)
                        put("checkedAtMillis", state.checkedAtMillis)
                        releaseDecisionNoteToJson(state.decisionNote)?.let { put("decision", it) }
                    }
                )
            }
        }
        val profileObj = buildJsonObject {
            states.forEach { (id, state) ->
                state.repositoryProfile?.let { profile ->
                    put(id, profile.toCacheJson())
                }
            }
        }
        kv().encode(KEY_CHECK_CACHE, obj.encodeCompact())
        if (profileObj.isNotEmpty()) {
            kv().encode(KEY_PROFILE_CACHE, profileObj.encodeCompact())
        } else {
            kv().removeValueForKey(KEY_PROFILE_CACHE)
        }
        kv().encode(KEY_LAST_REFRESH_MS, resolvedLastRefreshMs)
        return resolvedLastRefreshMs
    }

    fun clearCheckCache() = synchronized(checkCacheLock) {
        val store = kv()
        store.removeValueForKey(KEY_CHECK_CACHE)
        store.removeValueForKey(KEY_PROFILE_CACHE)
        store.removeValueForKey(KEY_LAST_REFRESH_MS)
        store.trim()
    }

    fun cachedCheckCount(): Int = loadCheckCache().first.size

    fun storageFootprintBytes(): Long = kv().totalSize()

    fun actualDataBytes(): Long = kv().actualSize()

    fun cacheBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        if (snapshot.checkCache.isEmpty() && snapshot.profileCache.isEmpty()) return 0L
        val cacheJsonBytes = snapshot.checkCache.values.sumOf { state ->
            listOf(
                state.localVersion,
                state.latestTag,
                state.latestStableName,
                state.latestStableRawTag,
                state.latestStableUrl,
                state.latestStableAuthorAvatarUrl,
                state.latestPreName,
                state.latestPreRawTag,
                state.latestPreUrl,
                state.latestPreAuthorAvatarUrl,
                state.message,
                state.preReleaseInfo,
                state.releaseHint,
                state.sourceStrategyId,
                state.sourceConfigSignature,
                state.latestStableApkVersion?.versionLabel().orEmpty(),
                state.latestStableApkVersion?.releaseLabel().orEmpty(),
                state.latestPreApkVersion?.versionLabel().orEmpty(),
                state.latestPreApkVersion?.releaseLabel().orEmpty(),
                state.repositoryProfile?.identity?.fullName?.value.orEmpty(),
                state.repositoryProfile?.sourceConfigSignature.orEmpty()
            ).sumOf { it.length.toLong() * 2 } + 64L
        }
        val profileJsonBytes = snapshot.profileCache.values.sumOf { profile ->
            profile.toCacheJson().encodeCompact().length.toLong() * 2
        }
        return cacheJsonBytes + profileJsonBytes + 16L
    }

    fun configBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        val trackedBytes = snapshot.items.sumOf { item ->
            (item.repoUrl.length + item.owner.length + item.repo.length + item.packageName.length + item.appLabel.length)
                .toLong() * 2 + 32L
        }
        val prefsBytes =
            (snapshot.lookupConfig.apiToken.length +
                snapshot.lookupConfig.normalizedFdroidCommonRepoIds.sumOf { id -> id.length })
                .toLong() * 2 + 128L
        return trackedBytes + prefsBytes
    }

    fun shouldAutoRefreshOnceInSession(): Boolean = !didAutoRefreshInSession

    fun markAutoRefreshDoneInSession() {
        didAutoRefreshInSession = true
    }

    /**
     * Whether the release list hides entries that are only tags.
     *
     * GitHub's `releases.atom` carries a repository's *tags* as well as its releases, so a tag pushed
     * without a release shows up in the feed with no notes and no files — KeiOS's own v1.11.6 through
     * v1.11.9 are exactly that, and none of them appear on the repository's Releases page. The API
     * returns only real releases, so this changes nothing in that mode.
     */
    fun loadHideTagOnlyReleases(defaultValue: Boolean = true): Boolean =
        kv().decodeBool(KEY_HIDE_TAG_ONLY_RELEASES, defaultValue)

    fun saveHideTagOnlyReleases(hide: Boolean) {
        kv().encode(KEY_HIDE_TAG_ONLY_RELEASES, hide)
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
            actionsStrategy = GitHubActionsLookupStrategyOption.fromStorageId(
                kv().decodeString(KEY_ACTIONS_LOOKUP_STRATEGY).orEmpty()
            ),
            apiToken = kv().decodeString(KEY_GITHUB_API_TOKEN).orEmpty().trim(),
            checkAllTrackedPreReleases = kv().decodeBool(KEY_CHECK_ALL_TRACKED_PRE_RELEASES, false),
            checkAllDirectApkPreReleases = kv().decodeBool(
                KEY_CHECK_ALL_DIRECT_APK_PRE_RELEASES,
                false
            ),
            aggressiveApkFiltering = kv().decodeBool(KEY_AGGRESSIVE_APK_FILTERING, false),
            preciseApkVersionEnabled = kv().decodeBool(KEY_PRECISE_APK_VERSION_ENABLED, false),
            scanSystemAppsByDefault = kv().decodeBool(KEY_SCAN_SYSTEM_APPS_BY_DEFAULT, false),
            profileDepth = GitHubProfileDepth.fromStorageId(
                kv().decodeString(KEY_PROFILE_DEPTH).orEmpty()
            ),
            shareImportFlowMode = GitHubShareImportFlowMode.fromStorageId(
                kv().decodeString(KEY_SHARE_IMPORT_FLOW_MODE).orEmpty()
            ),
            appManagedShareInstallEnabled = kv().decodeBool(
                KEY_APP_MANAGED_SHARE_INSTALL_ENABLED,
                false
            ),
            foregroundManagedDownloadBoostEnabled = kv().decodeBool(
                KEY_FOREGROUND_MANAGED_DOWNLOAD_BOOST_ENABLED,
                false
            ),
            onlineShareTargetPackage = kv().decodeString(KEY_ONLINE_SHARE_TARGET_PACKAGE).orEmpty().trim(),
            preferredDownloaderPackage = kv().decodeString(KEY_PREFERRED_DOWNLOADER_PACKAGE)
                .orEmpty().trim(),
            decisionAssistEnabled = kv().decodeBool(KEY_DECISION_ASSIST_ENABLED, false),
            repositoryHealthCardEnabled = kv().decodeBool(
                KEY_REPOSITORY_HEALTH_CARD_ENABLED,
                false
            ),
            apkTrustCheckEnabled = kv().decodeBool(KEY_APK_TRUST_CHECK_ENABLED, false),
            releaseNotesMode = GitHubReleaseNotesMode.fromStorageId(
                kv().decodeString(KEY_RELEASE_NOTES_MODE).orEmpty()
            ),
            fdroidCommonRepoIds = loadFdroidCommonRepoIds()
        )
    }

    fun saveLookupConfig(config: GitHubLookupConfig) {
        kv().encode(KEY_LOOKUP_STRATEGY, config.selectedStrategy.storageId)
        kv().encode(KEY_ACTIONS_LOOKUP_STRATEGY, config.actionsStrategy.storageId)
        kv().encode(KEY_GITHUB_API_TOKEN, config.apiToken.trim())
        kv().encode(KEY_CHECK_ALL_TRACKED_PRE_RELEASES, config.checkAllTrackedPreReleases)
        kv().encode(
            KEY_CHECK_ALL_DIRECT_APK_PRE_RELEASES,
            config.checkAllDirectApkPreReleases
        )
        kv().encode(KEY_AGGRESSIVE_APK_FILTERING, config.aggressiveApkFiltering)
        kv().encode(KEY_PRECISE_APK_VERSION_ENABLED, config.preciseApkVersionEnabled)
        kv().encode(KEY_SCAN_SYSTEM_APPS_BY_DEFAULT, config.scanSystemAppsByDefault)
        kv().encode(KEY_PROFILE_DEPTH, config.profileDepth.storageId)
        kv().encode(KEY_SHARE_IMPORT_FLOW_MODE, config.shareImportFlowMode.storageId)
        kv().encode(
            KEY_APP_MANAGED_SHARE_INSTALL_ENABLED,
            config.appManagedShareInstallEnabled
        )
        kv().encode(
            KEY_FOREGROUND_MANAGED_DOWNLOAD_BOOST_ENABLED,
            config.foregroundManagedDownloadBoostEnabled
        )
        kv().encode(KEY_ONLINE_SHARE_TARGET_PACKAGE, config.onlineShareTargetPackage.trim())
        kv().encode(KEY_PREFERRED_DOWNLOADER_PACKAGE, config.preferredDownloaderPackage.trim())
        kv().encode(KEY_DECISION_ASSIST_ENABLED, config.decisionAssistEnabled)
        kv().encode(KEY_REPOSITORY_HEALTH_CARD_ENABLED, config.repositoryHealthCardEnabled)
        kv().encode(KEY_APK_TRUST_CHECK_ENABLED, config.apkTrustCheckEnabled)
        kv().encode(KEY_RELEASE_NOTES_MODE, config.releaseNotesMode.storageId)
        kv().encode(
            KEY_FDROID_COMMON_REPO_IDS,
            config.normalizedFdroidCommonRepoIds.joinToString(",")
        )
    }

    private fun loadFdroidCommonRepoIds(): List<String> {
        val raw = kv().decodeString(KEY_FDROID_COMMON_REPO_IDS).orEmpty()
        val ids =
            raw
                .split(',')
                .map { id -> id.trim() }
                .filter { id -> id.isNotBlank() }
        return FdroidRepositoryPresets.normalizedCommonSearchRepoIds(ids)
    }

}

internal fun buildDueCheckCache(
    trackedIds: List<String>,
    currentCache: Map<String, GitHubCheckCacheEntry>,
): Map<String, GitHubCheckCacheEntry> =
    trackedIds.associateWith { trackId ->
        (currentCache[trackId] ?: GitHubCheckCacheEntry()).copy(checkedAtMillis = 1L)
    }

internal fun normalizeTrackedItems(
    items: List<GitHubTrackedApp>
): List<GitHubTrackedApp> {
    if (items.isEmpty()) return emptyList()
    val byId = LinkedHashMap<String, GitHubTrackedApp>(items.size)
    items.forEach { rawItem ->
        val item = rawItem.withSourceModeConstraints()
        val previous = byId[item.id]
        byId[item.id] = previous?.mergeDuplicate(item) ?: item
    }
    return byId.values.toList()
}

private fun GitHubTrackedApp.mergeDuplicate(
    incoming: GitHubTrackedApp
): GitHubTrackedApp {
    return if (incoming.localAppType == GitHubTrackedLocalAppType.Unknown) {
        incoming.copy(localAppType = localAppType)
    } else {
        incoming
    }
}
