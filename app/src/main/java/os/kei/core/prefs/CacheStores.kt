package os.kei.core.prefs

import android.content.Context
import os.kei.R
import os.kei.feature.github.data.local.AppIconCache
import os.kei.feature.github.data.local.GitHubReleaseAssetCacheStore
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.remote.GitHubReleaseStrategyRegistry
import os.kei.core.system.AppBuildEnv
import os.kei.mcp.server.McpServerManager
import os.kei.ui.page.main.ba.BaCalendarPoolImageCache
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.os.OsCardVisibilityStore
import os.kei.ui.page.main.os.OsInfoCache
import os.kei.ui.page.main.os.OsSectionCard
import os.kei.ui.page.main.os.OsUiStateStore
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.BaStudentGuideStore
import os.kei.ui.page.main.student.clearGameKeeMediaPlaybackCache
import os.kei.ui.page.main.student.loadGameKeeMediaCacheDiagnostics
import os.kei.ui.page.main.student.catalog.BaGuideCatalogStore
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.clearBaGuideCatalogCache
import com.tencent.mmkv.MMKV
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

internal data class CacheEntrySummary(
    val id: String,
    val title: String,
    val summary: String,
    val detail: String,
    val activity: String,
    val storage: String,
    val clearLabel: String,
    val cacheBytes: Long = 0L,
    val configBytes: Long = 0L,
    val diskBytes: Long = 0L,
    val memoryBytes: Long = 0L,
    val updatedAtMs: Long = 0L,
    val clearedAtMs: Long = 0L
)

internal object CacheStores {
    private const val CACHE_EVENT_KV_ID = "cache_events"

    fun list(context: Context): List<CacheEntrySummary> {
        val entries = listOf(
            githubSummary(context),
            baCalendarSummary(context),
            baStudentGuideSummary(context),
            osSummary(context),
            appIconSummary(context),
            baMediaPlaybackSummary(context),
            baTempMediaSummary(context),
            debugUiDumpSummary(context),
            mcpSummary(context)
        )
        return listOf(buildOverview(context, entries)) + entries
    }

    fun clear(context: Context, id: String) {
        when (id) {
            "github" -> {
                GitHubReleaseStrategyRegistry.clearAllCaches()
                GitHubTrackStore.clearCheckCache()
                GitHubReleaseAssetCacheStore.clearAll()
                AppIconCache.clear()
                CacheEventStore.markCleared("app_icon")
            }
            "ba_calendar" -> {
                BASettingsStore.clearCalendarAndPoolCaches()
                BaCalendarPoolImageCache.clearAll(context)
            }
            "ba_student_guide" -> {
                BaStudentGuideStore.clearAllCachedInfo()
                clearBaGuideCatalogCache(context)
            }
            "os_info" -> OsInfoCache.clearAll()
            "app_icon" -> AppIconCache.clear()
            "ba_media_playback" -> clearGameKeeMediaPlaybackCache(context)
            "ba_temp_media" -> BaGuideTempMediaCache.clearAll(context)
            "debug_ui_dump" -> clearDebugUiDump(context)
            "mcp_prefs" -> McpServerManager.clearSavedCacheOnly()
        }
        if (id != "cache_overview") {
            CacheEventStore.markCleared(id)
        }
    }

    fun clearAll(context: Context) {
        list(context)
            .asSequence()
            .filter { it.id != "cache_overview" && it.clearLabel.isNotBlank() }
            .forEach { entry ->
                clear(context, entry.id)
            }
    }

    private fun buildOverview(context: Context, entries: List<CacheEntrySummary>): CacheEntrySummary {
        val cacheBytes = entries.sumOf(CacheEntrySummary::cacheBytes)
        val configBytes = entries.sumOf(CacheEntrySummary::configBytes)
        val diskBytes = entries.sumOf(CacheEntrySummary::diskBytes)
        val memoryBytes = entries.sumOf(CacheEntrySummary::memoryBytes)
        val updatedAtMs = entries.maxOfOrNull(CacheEntrySummary::updatedAtMs)?.takeIf { it > 0L } ?: 0L
        val clearedAtMs = entries.maxOfOrNull(CacheEntrySummary::clearedAtMs)?.takeIf { it > 0L } ?: 0L
        return CacheEntrySummary(
            id = "cache_overview",
            title = context.getString(R.string.settings_cache_entry_overview_title),
            summary = context.getString(R.string.settings_cache_entry_overview_summary),
            detail = context.getString(
                R.string.settings_cache_entry_overview_detail,
                formatBytes(cacheBytes),
                formatBytes(configBytes),
                formatBytes(cacheBytes + configBytes)
            ),
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_disk_memory,
                formatBytes(diskBytes),
                formatBytes(memoryBytes)
            ),
            clearLabel = "",
            cacheBytes = cacheBytes,
            configBytes = configBytes,
            diskBytes = diskBytes,
            memoryBytes = memoryBytes,
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun githubSummary(context: Context): CacheEntrySummary {
        val snapshot = GitHubTrackStore.loadSnapshot()
        val updatedAtMs = snapshot.lastRefreshMs.takeIf { it > 0L }
            ?: mmkvLastModified(context, "github_track_store")
        val clearedAtMs = CacheEventStore.loadClearedAt("github")
        val iconMemory = AppIconCache.estimatedMemoryBytes()
        val assetCacheCount = GitHubReleaseAssetCacheStore.cachedEntryCount()
        val refreshState = context.getString(
            if (snapshot.lastRefreshMs > 0L) {
                R.string.settings_cache_refresh_record_present
            } else {
                R.string.settings_cache_refresh_record_empty
            }
        )
        val detail = context.getString(
            R.string.settings_cache_entry_github_detail,
            snapshot.items.size,
            snapshot.checkCache.size,
            assetCacheCount,
            refreshState
        )
        return CacheEntrySummary(
            id = "github",
            title = context.getString(R.string.settings_cache_entry_github_title),
            summary = context.getString(R.string.settings_cache_entry_github_summary),
            detail = detail,
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_cache_config_mmkv_icon,
                formatBytes(GitHubTrackStore.cacheBytesEstimated()),
                formatBytes(GitHubTrackStore.configBytesEstimated()),
                formatBytes(GitHubTrackStore.actualDataBytes()),
                formatBytes(iconMemory)
            ),
            clearLabel = context.getString(R.string.common_clear),
            cacheBytes = GitHubTrackStore.cacheBytesEstimated(),
            configBytes = GitHubTrackStore.configBytesEstimated(),
            diskBytes = GitHubTrackStore.actualDataBytes(),
            memoryBytes = iconMemory,
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun baCalendarSummary(context: Context): CacheEntrySummary {
        val snapshot = BASettingsStore.loadSnapshot()
        val calendar = BASettingsStore.loadCalendarCacheSnapshot(snapshot.serverIndex)
        val pool = BASettingsStore.loadPoolCacheSnapshot(snapshot.serverIndex)
        val mediaBytes = BaCalendarPoolImageCache.cacheTotalBytes(context)
        val mediaFiles = BaCalendarPoolImageCache.cacheFileCount(context)
        val mediaUpdatedAtMs = BaCalendarPoolImageCache.latestModifiedAtMs(context)
        val mergedCacheBytes = BASettingsStore.cacheBytesEstimated() + mediaBytes
        val updatedAtMs = maxOf(calendar.syncMs, pool.syncMs, mediaUpdatedAtMs).takeIf { it > 0L }
            ?: mmkvLastModified(context, "ba_page_settings")
        val clearedAtMs = CacheEventStore.loadClearedAt("ba_calendar")
        val cachedState = context.getString(R.string.common_status_cached)
        val emptyState = context.getString(R.string.settings_cache_state_empty)
        val detail = context.getString(
            R.string.settings_cache_entry_ba_page_detail,
            snapshot.serverIndex,
            if (calendar.raw.isNotBlank()) cachedState else emptyState,
            if (pool.raw.isNotBlank()) cachedState else emptyState,
            mediaFiles
        )
        return CacheEntrySummary(
            id = "ba_calendar",
            title = context.getString(R.string.settings_cache_entry_ba_page_title),
            summary = context.getString(R.string.settings_cache_entry_ba_page_summary),
            detail = detail,
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_cache_config_mmkv_media,
                formatBytes(mergedCacheBytes),
                formatBytes(BASettingsStore.configBytesEstimated()),
                formatBytes(BASettingsStore.actualDataBytes()),
                formatBytes(mediaBytes)
            ),
            clearLabel = context.getString(R.string.common_clear),
            cacheBytes = mergedCacheBytes,
            configBytes = BASettingsStore.configBytesEstimated(),
            diskBytes = BASettingsStore.actualDataBytes() + mediaBytes,
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun baStudentGuideSummary(context: Context): CacheEntrySummary {
        val detailCount = BaStudentGuideStore.cachedEntryCount()
        val catalogCounts = BaGuideCatalogStore.cachedEntryCounts()
        val studentCount = catalogCounts[BaGuideCatalogTab.Student] ?: 0
        val npcSatelliteCount = catalogCounts[BaGuideCatalogTab.NpcSatellite] ?: 0
        val cacheBytes = BaStudentGuideStore.cacheBytesEstimated() + BaGuideCatalogStore.cacheBytesEstimated()
        val configBytes = BaStudentGuideStore.configBytesEstimated() + BaGuideCatalogStore.configBytesEstimated()
        val diskBytes = BaStudentGuideStore.actualDataBytes() + BaGuideCatalogStore.actualDataBytes()
        val updatedAtMs = maxOf(
            BaStudentGuideStore.latestSyncedAtMs(),
            BaGuideCatalogStore.latestSyncedAtMs()
        ).takeIf { it > 0L }
            ?: maxOf(
                mmkvLastModified(context, "ba_student_guide"),
                mmkvLastModified(context, "ba_guide_catalog")
            )
        val clearedAtMs = CacheEventStore.loadClearedAt("ba_student_guide")
        return CacheEntrySummary(
            id = "ba_student_guide",
            title = context.getString(R.string.settings_cache_entry_ba_guide_title),
            summary = context.getString(R.string.settings_cache_entry_ba_guide_summary),
            detail = context.getString(
                R.string.settings_cache_entry_ba_guide_detail,
                detailCount,
                studentCount,
                npcSatelliteCount
            ),
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_cache_config_mmkv,
                formatBytes(cacheBytes),
                formatBytes(configBytes),
                formatBytes(diskBytes)
            ),
            clearLabel = context.getString(R.string.common_clear),
            cacheBytes = cacheBytes,
            configBytes = configBytes,
            diskBytes = diskBytes,
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun osSummary(context: Context): CacheEntrySummary {
        val visible = OsCardVisibilityStore.loadVisibleCards().size
        val cachedSections = OsInfoCache.cachedSectionCount(OsSectionCard.entries.toSet())
        val cacheBytes = OsInfoCache.cacheBytesEstimated()
        val configBytes = OsUiStateStore.configBytesEstimated()
        val updatedAtMs = maxOf(
            mmkvLastModified(context, "os_info_cache"),
            mmkvLastModified(context, "system_info_cache"),
            mmkvLastModified(context, "os_ui_state"),
            mmkvLastModified(context, "system_ui_state")
        ).takeIf { it > 0L } ?: 0L
        val clearedAtMs = CacheEventStore.loadClearedAt("os_info")
        return CacheEntrySummary(
            id = "os_info",
            title = context.getString(R.string.settings_cache_entry_os_title),
            summary = context.getString(R.string.settings_cache_entry_os_summary),
            detail = context.getString(R.string.settings_cache_entry_os_detail, visible, cachedSections),
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_cache_config_mmkv,
                formatBytes(cacheBytes),
                formatBytes(configBytes),
                formatBytes(OsInfoCache.actualDataBytes() + OsUiStateStore.actualDataBytes())
            ),
            clearLabel = context.getString(R.string.common_clear),
            cacheBytes = cacheBytes,
            configBytes = configBytes,
            diskBytes = OsInfoCache.actualDataBytes() + OsUiStateStore.actualDataBytes(),
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun appIconSummary(context: Context): CacheEntrySummary {
        val memoryBytes = AppIconCache.estimatedMemoryBytes()
        val updatedAtMs = AppIconCache.lastUpdatedAtMs()
        val clearedAtMs = CacheEventStore.loadClearedAt("app_icon")
        return CacheEntrySummary(
            id = "app_icon",
            title = context.getString(R.string.settings_cache_entry_app_icon_title),
            summary = context.getString(R.string.settings_cache_entry_app_icon_summary),
            detail = context.getString(R.string.settings_cache_entry_app_icon_detail, AppIconCache.size()),
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_cache_config_memory,
                formatBytes(memoryBytes),
                formatBytes(0L),
                formatBytes(memoryBytes)
            ),
            clearLabel = context.getString(R.string.common_clear),
            cacheBytes = memoryBytes,
            configBytes = 0L,
            memoryBytes = memoryBytes,
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun baTempMediaSummary(context: Context): CacheEntrySummary {
        val fileCount = BaGuideTempMediaCache.cacheFileCount(context)
        val diskBytes = BaGuideTempMediaCache.cacheTotalBytes(context)
        val updatedAtMs = BaGuideTempMediaCache.latestModifiedAtMs(context)
        val clearedAtMs = CacheEventStore.loadClearedAt("ba_temp_media")
        return CacheEntrySummary(
            id = "ba_temp_media",
            title = context.getString(R.string.settings_cache_entry_ba_temp_media_title),
            summary = context.getString(R.string.settings_cache_entry_ba_temp_media_summary),
            detail = context.getString(R.string.settings_cache_entry_file_count_detail, fileCount),
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_cache_config_disk,
                formatBytes(diskBytes),
                formatBytes(0L),
                formatBytes(diskBytes)
            ),
            clearLabel = context.getString(R.string.common_clear),
            cacheBytes = diskBytes,
            configBytes = 0L,
            diskBytes = diskBytes,
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun baMediaPlaybackSummary(context: Context): CacheEntrySummary {
        val diagnostics = loadGameKeeMediaCacheDiagnostics(context)
        val updatedAtMs = maxOf(
            diagnostics.latestModifiedAtMs,
            diagnostics.lastCleanupAtMs
        ).takeIf { it > 0L } ?: 0L
        val clearedAtMs = CacheEventStore.loadClearedAt("ba_media_playback")
        val detail = context.getString(
            R.string.settings_cache_entry_ba_playback_detail,
            diagnostics.fileCount,
            diagnostics.cleanupRunCount,
            diagnostics.lastRemovedResourceCount,
            formatBytes(diagnostics.lastRemovedBytes)
        )
        return CacheEntrySummary(
            id = "ba_media_playback",
            title = context.getString(R.string.settings_cache_entry_ba_playback_title),
            summary = context.getString(R.string.settings_cache_entry_ba_playback_summary),
            detail = detail,
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_ba_playback,
                formatBytes(diagnostics.diskBytes),
                formatBytes(0L),
                formatBytes(diagnostics.diskBytes),
                diagnostics.scannedResourceCount,
                diagnostics.removedResourceCount,
                diagnostics.removedSpanCount,
                formatBytes(diagnostics.removedBytes)
            ),
            clearLabel = context.getString(R.string.common_clear),
            cacheBytes = diagnostics.diskBytes,
            configBytes = 0L,
            diskBytes = diagnostics.diskBytes,
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun debugUiDumpSummary(context: Context): CacheEntrySummary {
        val targetDir = AppBuildEnv.uiDumpDirectory(context)
        val stats = collectDirectoryStats(targetDir)
        val clearedAtMs = CacheEventStore.loadClearedAt("debug_ui_dump")
        return CacheEntrySummary(
            id = "debug_ui_dump",
            title = context.getString(R.string.settings_cache_entry_debug_ui_dump_title),
            summary = context.getString(R.string.settings_cache_entry_debug_ui_dump_summary),
            detail = context.getString(
                R.string.settings_cache_entry_debug_ui_dump_detail,
                AppBuildEnv.displayName,
                stats.fileCount
            ),
            activity = formatActivity(context, stats.latestModifiedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_debug_ui_dump,
                formatBytes(stats.totalBytes),
                formatBytes(0L),
                formatBytes(stats.totalBytes),
                targetDir.absolutePath
            ),
            clearLabel = context.getString(R.string.common_clear),
            cacheBytes = stats.totalBytes,
            configBytes = 0L,
            diskBytes = stats.totalBytes,
            updatedAtMs = stats.latestModifiedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun mcpSummary(context: Context): CacheEntrySummary {
        val snapshot = McpServerManager.loadSavedCacheSummary(context)
        val updatedAtMs = mmkvLastModified(context, "mcp_server_prefs")
        val clearedAtMs = CacheEventStore.loadClearedAt("mcp_prefs")
        return CacheEntrySummary(
            id = "mcp_prefs",
            title = context.getString(R.string.settings_cache_entry_mcp_title),
            summary = context.getString(R.string.settings_cache_entry_mcp_summary),
            detail = snapshot,
            activity = formatActivity(context, updatedAtMs, clearedAtMs),
            storage = context.getString(
                R.string.settings_cache_storage_cache_config_mmkv,
                formatBytes(0L),
                formatBytes(McpServerManager.configBytesEstimated()),
                formatBytes(McpServerManager.actualDataBytes())
            ),
            clearLabel = context.getString(R.string.common_reset),
            cacheBytes = 0L,
            configBytes = McpServerManager.configBytesEstimated(),
            diskBytes = McpServerManager.actualDataBytes(),
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun formatActivity(context: Context, updatedAtMs: Long, clearedAtMs: Long): String {
        val updated = formatTimestamp(context, updatedAtMs)
        val cleared = formatTimestamp(context, clearedAtMs)
        return context.getString(R.string.settings_cache_activity, updated, cleared)
    }

    private fun formatTimestamp(context: Context, epochMs: Long): String {
        if (epochMs <= 0L) return context.getString(R.string.settings_cache_no_record)
        return SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
    }

    private fun clearDebugUiDump(context: Context) {
        val dir = AppBuildEnv.uiDumpDirectory(context)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dir.mkdirs()
    }

    private data class DirectoryStats(
        val fileCount: Int,
        val totalBytes: Long,
        val latestModifiedAtMs: Long
    )

    private fun collectDirectoryStats(root: File): DirectoryStats {
        if (!root.exists()) {
            return DirectoryStats(
                fileCount = 0,
                totalBytes = 0L,
                latestModifiedAtMs = 0L
            )
        }
        val queue = ArrayDeque<File>()
        queue.add(root)
        var fileCount = 0
        var totalBytes = 0L
        var latestModifiedAtMs = 0L
        while (queue.isNotEmpty()) {
            val current = queue.removeLast()
            val modifiedAt = current.lastModified()
            if (modifiedAt > latestModifiedAtMs) {
                latestModifiedAtMs = modifiedAt
            }
            if (current.isFile) {
                fileCount += 1
                totalBytes += current.length().coerceAtLeast(0L)
                continue
            }
            current.listFiles().orEmpty().forEach(queue::add)
        }
        return DirectoryStats(
            fileCount = fileCount,
            totalBytes = totalBytes,
            latestModifiedAtMs = latestModifiedAtMs
        )
    }

    private fun mmkvLastModified(context: Context, id: String): Long {
        val root = File(context.filesDir, "mmkv")
        if (!root.exists()) return 0L
        return root.listFiles()
            .orEmpty()
            .filter { file -> file.name == id || file.name.startsWith("$id.") }
            .maxOfOrNull(File::lastModified)
            ?: 0L
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "${bytes} ${units[digitGroups]}"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    private object CacheEventStore {
        private val store: MMKV by lazy { MMKV.mmkvWithID(CACHE_EVENT_KV_ID) }

        fun loadClearedAt(id: String): Long {
            return store.decodeLong("cleared_$id", 0L)
        }

        fun markCleared(id: String, epochMs: Long = System.currentTimeMillis()) {
            store.encode("cleared_$id", epochMs.coerceAtLeast(0L))
        }
    }
}
