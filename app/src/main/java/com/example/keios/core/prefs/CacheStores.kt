package com.example.keios.core.prefs

import android.content.Context
import com.example.keios.feature.github.data.local.AppIconCache
import com.example.keios.feature.github.data.local.GitHubReleaseAssetCacheStore
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubReleaseStrategyRegistry
import com.example.keios.core.system.AppBuildEnv
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.ba.BaCalendarPoolImageCache
import com.example.keios.ui.page.main.ba.BASettingsStore
import com.example.keios.ui.page.main.OsCardVisibilityStore
import com.example.keios.ui.page.main.OsInfoCache
import com.example.keios.ui.page.main.OsSectionCard
import com.example.keios.ui.page.main.OsUiStateStore
import com.example.keios.ui.page.main.student.BaGuideTempMediaCache
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.student.clearGameKeeMediaPlaybackCache
import com.example.keios.ui.page.main.student.loadGameKeeMediaCacheDiagnostics
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogStore
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.student.catalog.clearBaGuideCatalogCache
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
            appIconSummary(),
            baMediaPlaybackSummary(context),
            baTempMediaSummary(context),
            debugUiDumpSummary(context),
            mcpSummary(context)
        )
        return listOf(buildOverview(entries)) + entries
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

    private fun buildOverview(entries: List<CacheEntrySummary>): CacheEntrySummary {
        val cacheBytes = entries.sumOf(CacheEntrySummary::cacheBytes)
        val configBytes = entries.sumOf(CacheEntrySummary::configBytes)
        val diskBytes = entries.sumOf(CacheEntrySummary::diskBytes)
        val memoryBytes = entries.sumOf(CacheEntrySummary::memoryBytes)
        val updatedAtMs = entries.maxOfOrNull(CacheEntrySummary::updatedAtMs)?.takeIf { it > 0L } ?: 0L
        val clearedAtMs = entries.maxOfOrNull(CacheEntrySummary::clearedAtMs)?.takeIf { it > 0L } ?: 0L
        return CacheEntrySummary(
            id = "cache_overview",
            title = "总缓存概览",
            summary = "跨页面缓存与配置占用总览",
            detail = "缓存估算 ${formatBytes(cacheBytes)} · 配置估算 ${formatBytes(configBytes)} · 合计 ${formatBytes(cacheBytes + configBytes)}",
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "磁盘占用 ${formatBytes(diskBytes)} · 内存占用 ${formatBytes(memoryBytes)}",
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
        val detail = buildString {
            append("跟踪 ${snapshot.items.size} 项")
            append(" · 检查缓存 ${snapshot.checkCache.size} 项")
            append(" · 资产缓存 ${assetCacheCount} 项")
            if (snapshot.lastRefreshMs > 0L) append(" · 已有刷新记录") else append(" · 暂无刷新记录")
        }
        return CacheEntrySummary(
            id = "github",
            title = "主页 / GitHub",
            summary = "版本检查结果与 GitHub 追踪配置",
            detail = detail,
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "缓存估算 ${formatBytes(GitHubTrackStore.cacheBytesEstimated())} · 配置估算 ${formatBytes(GitHubTrackStore.configBytesEstimated())} · MMKV 已用 ${formatBytes(GitHubTrackStore.actualDataBytes())} · 图标内存 ${formatBytes(iconMemory)}",
            clearLabel = "清理",
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
        val detail = buildString {
            append("当前服区 ${snapshot.serverIndex}")
            append(" · 日程缓存 ")
            append(if (calendar.raw.isNotBlank()) "已缓存" else "空")
            append(" · 池子缓存 ")
            append(if (pool.raw.isNotBlank()) "已缓存" else "空")
            append(" · 图片 $mediaFiles 张")
        }
        return CacheEntrySummary(
            id = "ba_calendar",
            title = "BA 页面",
            summary = "活动日程、卡池缓存与页面状态",
            detail = detail,
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "缓存估算 ${formatBytes(mergedCacheBytes)} · 配置估算 ${formatBytes(BASettingsStore.configBytesEstimated())} · MMKV 已用 ${formatBytes(BASettingsStore.actualDataBytes())} · 媒体磁盘 ${formatBytes(mediaBytes)}",
            clearLabel = "清理",
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
            title = "BA 图鉴页",
            summary = "角色图鉴详情与图鉴总览列表缓存",
            detail = "详情缓存 $detailCount 条 · 总览 实装 $studentCount 条 / NPC及卫星 $npcSatelliteCount 条",
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "缓存估算 ${formatBytes(cacheBytes)} · 配置估算 ${formatBytes(configBytes)} · MMKV 已用 ${formatBytes(diskBytes)}",
            clearLabel = "清理",
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
            title = "系统页面",
            summary = "各类系统表与属性缓存",
            detail = "显示卡片 $visible 张 · 已缓存分区 $cachedSections 个",
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "缓存估算 ${formatBytes(cacheBytes)} · 配置估算 ${formatBytes(configBytes)} · MMKV 已用 ${formatBytes(OsInfoCache.actualDataBytes() + OsUiStateStore.actualDataBytes())}",
            clearLabel = "清理",
            cacheBytes = cacheBytes,
            configBytes = configBytes,
            diskBytes = OsInfoCache.actualDataBytes() + OsUiStateStore.actualDataBytes(),
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun appIconSummary(): CacheEntrySummary {
        val memoryBytes = AppIconCache.estimatedMemoryBytes()
        val updatedAtMs = AppIconCache.lastUpdatedAtMs()
        val clearedAtMs = CacheEventStore.loadClearedAt("app_icon")
        return CacheEntrySummary(
            id = "app_icon",
            title = "GitHub 图标",
            summary = "GitHub 页面与相关列表的图标内存缓存",
            detail = "当前内存命中 ${AppIconCache.size()} 项",
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "缓存估算 ${formatBytes(memoryBytes)} · 配置估算 0 B · 内存占用 ${formatBytes(memoryBytes)}",
            clearLabel = "清理",
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
            title = "图鉴媒体",
            summary = "图鉴页临时图片与媒体文件",
            detail = "文件 $fileCount 个",
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "缓存估算 ${formatBytes(diskBytes)} · 配置估算 0 B · 磁盘占用 ${formatBytes(diskBytes)}",
            clearLabel = "清理",
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
        val detail = buildString {
            append("文件 ${diagnostics.fileCount} 个")
            append(" · 清理运行 ${diagnostics.cleanupRunCount} 次")
            append(" · 最近命中 ${diagnostics.lastRemovedResourceCount} 项")
            append(" · 最近释放 ${formatBytes(diagnostics.lastRemovedBytes)}")
        }
        return CacheEntrySummary(
            id = "ba_media_playback",
            title = "图鉴播放缓存",
            summary = "语音台词与媒体播放的 ExoPlayer 缓存",
            detail = detail,
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "缓存估算 ${formatBytes(diagnostics.diskBytes)} · 配置估算 0 B · 磁盘占用 ${formatBytes(diagnostics.diskBytes)} · 累计扫描 ${diagnostics.scannedResourceCount} 项 · 累计命中 ${diagnostics.removedResourceCount} 项/${diagnostics.removedSpanCount} 片段/${formatBytes(diagnostics.removedBytes)}",
            clearLabel = "清理",
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
            title = "调试 UI Dump",
            summary = "uiautomator dump 重定向输出目录",
            detail = "环境 ${AppBuildEnv.displayName} · 文件 ${stats.fileCount} 个",
            activity = formatActivity(stats.latestModifiedAtMs, clearedAtMs),
            storage = "缓存估算 ${formatBytes(stats.totalBytes)} · 配置估算 0 B · 磁盘占用 ${formatBytes(stats.totalBytes)} · 目录 ${targetDir.absolutePath}",
            clearLabel = "清理",
            cacheBytes = stats.totalBytes,
            configBytes = 0L,
            diskBytes = stats.totalBytes,
            updatedAtMs = stats.latestModifiedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun mcpSummary(context: Context): CacheEntrySummary {
        val snapshot = McpServerManager.loadSavedCacheSummary()
        val updatedAtMs = mmkvLastModified(context, "mcp_server_prefs")
        val clearedAtMs = CacheEventStore.loadClearedAt("mcp_prefs")
        return CacheEntrySummary(
            id = "mcp_prefs",
            title = "MCP 页面",
            summary = "服务名称、端口、鉴权 token 等本地持久项",
            detail = snapshot,
            activity = formatActivity(updatedAtMs, clearedAtMs),
            storage = "缓存估算 0 B · 配置估算 ${formatBytes(McpServerManager.configBytesEstimated())} · MMKV 已用 ${formatBytes(McpServerManager.actualDataBytes())}",
            clearLabel = "重置",
            cacheBytes = 0L,
            configBytes = McpServerManager.configBytesEstimated(),
            diskBytes = McpServerManager.actualDataBytes(),
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs
        )
    }

    private fun formatActivity(updatedAtMs: Long, clearedAtMs: Long): String {
        val updated = formatTimestamp(updatedAtMs)
        val cleared = formatTimestamp(clearedAtMs)
        return "更新：$updated · 清理：$cleared"
    }

    private fun formatTimestamp(epochMs: Long): String {
        if (epochMs <= 0L) return "未记录"
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
