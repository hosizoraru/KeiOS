package os.kei.mcp.server

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import os.kei.feature.github.data.local.GitHubReleaseAssetCacheStore
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.cafeDailyCapacity
import os.kei.ui.page.main.ba.support.cafeHourlyGain
import os.kei.ui.page.main.ba.support.cafeStorageCap
import os.kei.ui.page.main.ba.support.calculateApFullAtMs
import os.kei.ui.page.main.ba.support.calculateApNextPointAtMs
import os.kei.ui.page.main.ba.support.calculateInviteTicketAvailableMs
import os.kei.ui.page.main.ba.support.calculateNextHeadpatAvailableMs
import os.kei.ui.page.main.ba.support.decodeBaCalendarEntries
import os.kei.ui.page.main.ba.support.decodeBaPoolEntries
import os.kei.ui.page.main.ba.support.displayAp
import os.kei.ui.page.main.ba.support.fractionalApPart
import os.kei.ui.page.main.ba.support.gameKeeServerId
import os.kei.ui.page.main.student.BaStudentGuideStore
import os.kei.ui.page.main.student.GuideBgmFavoriteStore
import os.kei.ui.page.main.student.catalog.BaGuideCatalogStore
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.clearBaGuideCatalogCache
import os.kei.ui.page.main.student.catalog.isBaGuideCatalogBundleComplete
import os.kei.ui.page.main.student.catalog.isBaGuideCatalogCacheExpired
import os.kei.ui.page.main.student.catalog.loadCachedBaGuideCatalogBundle
import os.kei.ui.page.main.student.fetch.normalizeGuideUrl
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

internal class McpBaTools(
    private val environment: McpToolEnvironment
) {
    private val appContext get() = environment.appContext

    fun register(server: Server) {
        server.addTool(
            name = "keios.ba.snapshot",
            description = "Get BA page snapshot (AP, cafe, refresh interval).",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildBaSnapshotText())
        }

        server.addTool(
            name = "keios.ba.calendar.cache",
            description = "Inspect BA calendar cache. Args: serverIndex(optional), includeEntries(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("serverIndex", buildJsonObject { put("type", JsonPrimitive("integer")) })
                    put("includeEntries", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val serverIndexArg = argIntOrNull(request.arguments?.get("serverIndex"))
            val includeEntries = argBoolean(request.arguments?.get("includeEntries"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_ENTRY_LIMIT).coerceIn(1, MAX_ENTRY_LIMIT)
            callText(
                buildBaCalendarCacheText(
                    requestedServerIndex = serverIndexArg,
                    includeEntries = includeEntries,
                    limit = limit
                )
            )
        }

        server.addTool(
            name = "keios.ba.pool.cache",
            description = "Inspect BA pool cache. Args: serverIndex(optional), includeEntries(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("serverIndex", buildJsonObject { put("type", JsonPrimitive("integer")) })
                    put("includeEntries", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val serverIndexArg = argIntOrNull(request.arguments?.get("serverIndex"))
            val includeEntries = argBoolean(request.arguments?.get("includeEntries"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_ENTRY_LIMIT).coerceIn(1, MAX_ENTRY_LIMIT)
            callText(
                buildBaPoolCacheText(
                    requestedServerIndex = serverIndexArg,
                    includeEntries = includeEntries,
                    limit = limit
                )
            )
        }

        server.addTool(
            name = "keios.ba.guide.catalog.cache",
            description = "Inspect BA guide catalog cache. Args: tab(all|student|npc), includeEntries(optional), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("tab", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("includeEntries", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val tab = argString(request.arguments?.get("tab")).trim()
            val includeEntries = argBoolean(request.arguments?.get("includeEntries"), false)
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_ENTRY_LIMIT).coerceIn(1, MAX_ENTRY_LIMIT)
            callText(buildGuideCatalogCacheText(tab = tab, includeEntries = includeEntries, limit = limit))
        }

        server.addTool(
            name = "keios.ba.guide.cache.overview",
            description = "Get BA guide detail cache overview.",
            inputSchema = ToolSchema(properties = buildJsonObject { })
        ) { _ ->
            callText(buildGuideCacheOverviewText())
        }

        server.addTool(
            name = "keios.ba.guide.cache.inspect",
            description = "Inspect BA guide detail cache by URL. Args: url(optional), includeSections(optional), refreshIntervalHours(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("url", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("includeSections", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                    put("refreshIntervalHours", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val url = argString(request.arguments?.get("url")).trim()
            val includeSections = argBoolean(request.arguments?.get("includeSections"), false)
            val refreshHours = argInt(
                request.arguments?.get("refreshIntervalHours"),
                BASettingsStore.loadCalendarRefreshIntervalHours()
            ).coerceAtLeast(1)
            callText(
                buildGuideCacheInspectText(
                    url = url,
                    includeSections = includeSections,
                    refreshIntervalHours = refreshHours
                )
            )
        }

        server.addTool(
            name = "keios.ba.guide.media.list",
            description = "List BA guide gallery and voice media from detail cache. Args: url(optional), kind(all|gallery|voice|image|video|audio), limit(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("url", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("kind", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                }
            )
        ) { request ->
            val url = argString(request.arguments?.get("url")).trim()
            val kind = argString(request.arguments?.get("kind")).trim()
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_ENTRY_LIMIT).coerceIn(1, MAX_ENTRY_LIMIT)
            callText(buildGuideMediaListText(url = url, kind = kind, limit = limit))
        }

        server.addTool(
            name = "keios.ba.guide.bgm.favorites",
            description = "List/export/import BA guide BGM favorites. Args: action(list|export|import), query(optional), limit(optional), json(optional), apply(optional).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("action", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("query", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("limit", buildJsonObject { put("type", JsonPrimitive("integer")) })
                    put("json", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("apply", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                }
            )
        ) { request ->
            val action = argString(request.arguments?.get("action")).trim()
            val query = argString(request.arguments?.get("query")).trim()
            val limit = argInt(request.arguments?.get("limit"), DEFAULT_ENTRY_LIMIT).coerceIn(1, MAX_ENTRY_LIMIT)
            val rawJson = argString(request.arguments?.get("json"))
            val apply = argBoolean(request.arguments?.get("apply"), false)
            callText(
                buildGuideBgmFavoritesText(
                    action = action,
                    query = query,
                    limit = limit,
                    rawJson = rawJson,
                    apply = apply
                )
            )
        }

        server.addTool(
            name = "keios.ba.cache.clear",
            description = "Clear BA/GitHub caches. Args: scope(optional), url(optional for ba_guide_url).",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("scope", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("url", buildJsonObject { put("type", JsonPrimitive("string")) })
                }
            )
        ) { request ->
            val scope = argString(request.arguments?.get("scope"))
            val url = argString(request.arguments?.get("url"))
            callText(buildCacheClearText(scope = scope, url = url))
        }
    }

    private fun resolveServerIndex(requestedServerIndex: Int?): Int {
        return requestedServerIndex?.coerceIn(0, 2) ?: BASettingsStore.loadSnapshot().serverIndex
    }

    private fun buildBaSnapshotText(nowMs: Long = System.currentTimeMillis()): String {
        val snapshot = BASettingsStore.loadSnapshot()
        val serverIndex = snapshot.serverIndex.coerceIn(0, 2)
        val displayedAp = displayAp(snapshot.apCurrent)
        val apFraction = fractionalApPart(snapshot.apCurrent)
        val apPercent = if (snapshot.apLimit <= 0) {
            0.0
        } else {
            (snapshot.apCurrent / snapshot.apLimit.toDouble() * 100.0).coerceIn(0.0, 999.0)
        }
        val apFullAtMs = calculateApFullAtMs(
            apLimit = snapshot.apLimit,
            apCurrent = snapshot.apCurrent,
            apRegenBaseMs = snapshot.apRegenBaseMs,
            nowMs = nowMs
        )
        val apNextPointAtMs = calculateApNextPointAtMs(
            apLimit = snapshot.apLimit,
            apCurrent = snapshot.apCurrent,
            apRegenBaseMs = snapshot.apRegenBaseMs,
            nowMs = nowMs
        )
        val headpatReadyAtMs = calculateNextHeadpatAvailableMs(
            lastHeadpatMs = snapshot.coffeeHeadpatMs,
            serverIndex = serverIndex
        )
        val invite1ReadyAtMs = calculateInviteTicketAvailableMs(snapshot.coffeeInvite1UsedMs)
        val invite2ReadyAtMs = calculateInviteTicketAvailableMs(snapshot.coffeeInvite2UsedMs)
        val cafeLevel = snapshot.cafeLevel.coerceIn(1, 10)
        val cafeCap = cafeStorageCap(cafeLevel)
        val cafeStored = snapshot.cafeStoredAp.coerceIn(0.0, cafeCap)
        val cafePercent = if (cafeCap <= 0.0) 0.0 else (cafeStored / cafeCap * 100.0).coerceIn(0.0, 100.0)

        return buildString {
            appendLine("serverIndex=$serverIndex")
            appendLine("gameKeeServerId=${gameKeeServerId(serverIndex)}")
            appendLine("calendarRefreshIntervalHours=${snapshot.calendarRefreshIntervalHours}")
            appendLine("apCurrent=$displayedAp")
            appendLine("apCurrentExact=${snapshot.apCurrent}")
            appendLine("apFraction=${(apFraction * 1000.0).roundToInt() / 1000.0}")
            appendLine("apLimit=${snapshot.apLimit}")
            appendLine("apPercent=${(apPercent * 10.0).roundToInt() / 10.0}")
            appendLine("apRegenBaseMs=${snapshot.apRegenBaseMs}")
            appendLine("apSyncMs=${snapshot.apSyncMs}")
            appendLine("apNextPointAtMs=$apNextPointAtMs")
            appendLine("apFullAtMs=$apFullAtMs")
            appendLine("apNotifyEnabled=${snapshot.apNotifyEnabled}")
            appendLine("apNotifyThreshold=${snapshot.apNotifyThreshold}")
            appendLine("apLastNotifiedLevel=${snapshot.apLastNotifiedLevel}")
            appendLine("cafeLevel=$cafeLevel")
            appendLine("cafeHourlyGain=${(cafeHourlyGain(cafeLevel) * 100.0).roundToInt() / 100.0}")
            appendLine("cafeDailyCapacity=${cafeDailyCapacity(cafeLevel)}")
            appendLine("cafeStorageCap=$cafeCap")
            appendLine("cafeStoredAp=${(cafeStored * 100.0).roundToInt() / 100.0}")
            appendLine("cafeStoredPercent=${(cafePercent * 10.0).roundToInt() / 10.0}")
            appendLine("coffeeHeadpatLastMs=${snapshot.coffeeHeadpatMs}")
            appendLine("coffeeHeadpatReadyAtMs=$headpatReadyAtMs")
            appendLine("coffeeInvite1LastMs=${snapshot.coffeeInvite1UsedMs}")
            appendLine("coffeeInvite1ReadyAtMs=$invite1ReadyAtMs")
            appendLine("coffeeInvite2LastMs=${snapshot.coffeeInvite2UsedMs}")
            appendLine("coffeeInvite2ReadyAtMs=$invite2ReadyAtMs")
            appendLine("showEndedPools=${snapshot.showEndedPools}")
            appendLine("showEndedActivities=${snapshot.showEndedActivities}")
            appendLine("showCalendarPoolImages=${snapshot.showCalendarPoolImages}")
            appendLine("idNickname=${snapshot.idNickname}")
            appendLine("idFriendCode=${snapshot.idFriendCode}")
        }.trim()
    }

    private fun buildBaCalendarCacheText(
        requestedServerIndex: Int?,
        includeEntries: Boolean,
        limit: Int
    ): String {
        val nowMs = System.currentTimeMillis()
        val refreshHours = BASettingsStore.loadCalendarRefreshIntervalHours()
        val refreshIntervalMs = refreshHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val serverIndex = resolveServerIndex(requestedServerIndex)
        val snapshot = BASettingsStore.loadCalendarCacheSnapshot(serverIndex)
        val entries = runCatching { decodeBaCalendarEntries(snapshot.raw, nowMs) }.getOrElse { emptyList() }
        val expired = snapshot.syncMs <= 0L || (nowMs - snapshot.syncMs).coerceAtLeast(0L) >= refreshIntervalMs

        return buildString {
            appendLine("serverIndex=$serverIndex")
            appendLine("gameKeeServerId=${gameKeeServerId(serverIndex)}")
            appendLine("refreshIntervalHours=$refreshHours")
            appendLine("cacheVersion=${snapshot.version}")
            appendLine("cachePresent=${snapshot.raw.isNotBlank()}")
            appendLine("cacheRawChars=${snapshot.raw.length}")
            appendLine("cacheSyncMs=${snapshot.syncMs}")
            appendLine("cacheExpired=$expired")
            appendLine("entryCount=${entries.size}")
            if (includeEntries && entries.isNotEmpty()) {
                entries.take(limit).forEachIndexed { index, entry ->
                    appendLine(
                        "entry[$index]=id:${entry.id} | kind:${entry.kindName} | running:${entry.isRunning} | title:${entry.title} | beginAtMs:${entry.beginAtMs} | endAtMs:${entry.endAtMs} | link:${entry.linkUrl}"
                    )
                }
            }
        }.trim()
    }

    private fun buildBaPoolCacheText(
        requestedServerIndex: Int?,
        includeEntries: Boolean,
        limit: Int
    ): String {
        val nowMs = System.currentTimeMillis()
        val refreshHours = BASettingsStore.loadCalendarRefreshIntervalHours()
        val refreshIntervalMs = refreshHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val serverIndex = resolveServerIndex(requestedServerIndex)
        val snapshot = BASettingsStore.loadPoolCacheSnapshot(serverIndex)
        val entries = runCatching { decodeBaPoolEntries(snapshot.raw, nowMs) }.getOrElse { emptyList() }
        val expired = snapshot.syncMs <= 0L || (nowMs - snapshot.syncMs).coerceAtLeast(0L) >= refreshIntervalMs

        return buildString {
            appendLine("serverIndex=$serverIndex")
            appendLine("gameKeeServerId=${gameKeeServerId(serverIndex)}")
            appendLine("refreshIntervalHours=$refreshHours")
            appendLine("cacheVersion=${snapshot.version}")
            appendLine("cachePresent=${snapshot.raw.isNotBlank()}")
            appendLine("cacheRawChars=${snapshot.raw.length}")
            appendLine("cacheSyncMs=${snapshot.syncMs}")
            appendLine("cacheExpired=$expired")
            appendLine("entryCount=${entries.size}")
            if (includeEntries && entries.isNotEmpty()) {
                entries.take(limit).forEachIndexed { index, entry ->
                    appendLine(
                        "entry[$index]=id:${entry.id} | tag:${entry.tagName} | running:${entry.isRunning} | name:${entry.name} | startAtMs:${entry.startAtMs} | endAtMs:${entry.endAtMs} | link:${entry.linkUrl}"
                    )
                }
            }
        }.trim()
    }

    private fun parseCatalogTab(raw: String): BaGuideCatalogTab? {
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "", "all" -> null
            "student", "students", "实装学生" -> BaGuideCatalogTab.Student
            "npc", "npcs", "npc_satellite", "npc-satellite", "npc及卫星" -> BaGuideCatalogTab.NpcSatellite
            else -> null
        }
    }

    private fun buildGuideCatalogCacheText(
        tab: String,
        includeEntries: Boolean,
        limit: Int
    ): String {
        val filterTab = parseCatalogTab(tab)
        val bundle = loadCachedBaGuideCatalogBundle()
        val counts = BaGuideCatalogStore.cachedEntryCounts()
        val refreshHours = BASettingsStore.loadCalendarRefreshIntervalHours()
        val complete = isBaGuideCatalogBundleComplete(bundle)
        val expired = isBaGuideCatalogCacheExpired(bundle, refreshHours)
        val latestSyncedAtMs = bundle?.syncedAtMs ?: BaGuideCatalogStore.latestSyncedAtMs()

        val selectedEntries = buildList {
            if (bundle != null) {
                if (filterTab == null) {
                    BaGuideCatalogTab.entries.forEach { currentTab ->
                        addAll(bundle.entries(currentTab))
                    }
                } else {
                    addAll(bundle.entries(filterTab))
                }
            }
        }

        return buildString {
            appendLine("bundlePresent=${bundle != null}")
            appendLine("bundleComplete=$complete")
            appendLine("bundleExpired=$expired")
            appendLine("refreshIntervalHours=$refreshHours")
            appendLine("latestSyncedAtMs=$latestSyncedAtMs")
            appendLine("totalCachedEntryCount=${BaGuideCatalogStore.cachedEntryCount()}")
            appendLine("studentCount=${counts[BaGuideCatalogTab.Student] ?: 0}")
            appendLine("npcSatelliteCount=${counts[BaGuideCatalogTab.NpcSatellite] ?: 0}")
            appendLine("actualDataBytes=${BaGuideCatalogStore.actualDataBytes()}")
            appendLine("cacheBytesEstimated=${BaGuideCatalogStore.cacheBytesEstimated()}")
            appendLine("configBytesEstimated=${BaGuideCatalogStore.configBytesEstimated()}")
            appendLine("selectedTab=${filterTab?.name ?: "ALL"}")
            appendLine("selectedEntryCount=${selectedEntries.size}")
            if (includeEntries && selectedEntries.isNotEmpty()) {
                selectedEntries.take(limit).forEachIndexed { index, entry ->
                    appendLine(
                        "entry[$index]=tab:${entry.tab.name} | contentId:${entry.contentId} | name:${entry.name} | alias:${entry.aliasDisplay} | createdAtSec:${entry.createdAtSec} | detailUrl:${entry.detailUrl}"
                    )
                }
            }
        }.trim()
    }

    private fun buildGuideCacheOverviewText(): String {
        return buildString {
            appendLine("currentUrl=${BaStudentGuideStore.loadCurrentUrl()}")
            appendLine("cachedEntryCount=${BaStudentGuideStore.cachedEntryCount()}")
            appendLine("latestSyncedAtMs=${BaStudentGuideStore.latestSyncedAtMs()}")
            appendLine("storageFootprintBytes=${BaStudentGuideStore.storageFootprintBytes()}")
            appendLine("actualDataBytes=${BaStudentGuideStore.actualDataBytes()}")
            appendLine("cacheBytesEstimated=${BaStudentGuideStore.cacheBytesEstimated()}")
            appendLine("configBytesEstimated=${BaStudentGuideStore.configBytesEstimated()}")
        }.trim()
    }

    private fun extractGuideContentId(url: String): Long {
        val text = url.trim()
        if (text.isBlank()) return 0L
        val patterns = listOf(
            Regex("/v1/content/detail/(\\d+)"),
            Regex("/tj/(\\d+)\\.html"),
            Regex("/(\\d+)\\.html")
        )
        patterns.forEach { regex ->
            val hit = regex.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (hit != null && hit > 0L) return hit
        }
        return 0L
    }

    private fun buildGuideCacheInspectText(
        url: String,
        includeSections: Boolean,
        refreshIntervalHours: Int
    ): String {
        val target = normalizeGuideUrl(url).ifBlank { BaStudentGuideStore.loadCurrentUrl() }
        if (target.isBlank()) {
            return "hasTarget=false\nmessage=No target URL. Pass url argument or open a student guide page first."
        }
        val snapshot = BaStudentGuideStore.loadInfoSnapshot(target)
        val expired = BaStudentGuideStore.isCacheExpired(
            snapshot = snapshot,
            refreshIntervalHours = refreshIntervalHours
        )
        val info = snapshot.info

        return buildString {
            appendLine("hasTarget=true")
            appendLine("targetUrl=$target")
            appendLine("contentId=${extractGuideContentId(target)}")
            appendLine("hasCache=${snapshot.hasCache}")
            appendLine("isComplete=${snapshot.isComplete}")
            appendLine("isExpired=$expired")
            appendLine("refreshIntervalHours=$refreshIntervalHours")
            appendLine("syncedAtMs=${snapshot.syncedAtMs}")
            appendLine("infoPresent=${info != null}")
            if (info != null) {
                appendLine("title=${info.title}")
                appendLine("subtitle=${info.subtitle}")
                appendLine("summary=${info.summary}")
                appendLine("statsCount=${info.stats.size}")
                appendLine("skillRowsCount=${info.skillRows.size}")
                appendLine("profileRowsCount=${info.profileRows.size}")
                appendLine("galleryItemsCount=${info.galleryItems.size}")
                appendLine("growthRowsCount=${info.growthRows.size}")
                appendLine("simulateRowsCount=${info.simulateRows.size}")
                appendLine("voiceRowsCount=${info.voiceRows.size}")
                appendLine("voiceEntriesCount=${info.voiceEntries.size}")
                appendLine("voiceHeadersCount=${info.voiceLanguageHeaders.size}")
                appendLine("voiceCvLangCount=${info.voiceCvByLanguage.size}")
                if (includeSections) {
                    appendLine("voiceCvByLanguage=${info.voiceCvByLanguage.entries.joinToString(" | ") { "${it.key}:${it.value}" }}")
                    appendLine("tabSkillIconUrl=${info.tabSkillIconUrl}")
                    appendLine("tabProfileIconUrl=${info.tabProfileIconUrl}")
                    appendLine("tabVoiceIconUrl=${info.tabVoiceIconUrl}")
                    appendLine("tabGalleryIconUrl=${info.tabGalleryIconUrl}")
                    appendLine("tabSimulateIconUrl=${info.tabSimulateIconUrl}")
                }
            }
        }.trim()
    }

    private fun buildGuideMediaListText(
        url: String,
        kind: String,
        limit: Int
    ): String {
        val target = normalizeGuideUrl(url).ifBlank { BaStudentGuideStore.loadCurrentUrl() }
        if (target.isBlank()) {
            return "hasTarget=false\nmessage=No target URL. Pass url argument or open a student guide page first."
        }
        val snapshot = BaStudentGuideStore.loadInfoSnapshot(target)
        val info = snapshot.info ?: return buildString {
            appendLine("hasTarget=true")
            appendLine("targetUrl=$target")
            appendLine("hasCache=${snapshot.hasCache}")
            appendLine("infoPresent=false")
        }.trim()

        val normalizedKind = normalizeGuideMediaKind(kind)
        val galleryRows = info.galleryItems.filter { item ->
            when (normalizedKind) {
                "gallery", "all" -> true
                "image" -> item.mediaType.equals("image", ignoreCase = true)
                "video" -> item.mediaType.equals("video", ignoreCase = true)
                else -> false
            }
        }
        val voiceRows = info.voiceEntries.filter { entry ->
            normalizedKind == "all" || normalizedKind == "voice" || normalizedKind == "audio"
        }

        var emitted = 0
        return buildString {
            appendLine("hasTarget=true")
            appendLine("targetUrl=$target")
            appendLine("title=${info.title}")
            appendLine("galleryCount=${info.galleryItems.size}")
            appendLine("voiceEntryCount=${info.voiceEntries.size}")
            appendLine("kind=$normalizedKind")
            appendLine("limit=$limit")
            galleryRows.forEachIndexed { index, item ->
                if (emitted >= limit) return@forEachIndexed
                appendLine(
                    "gallery[$index]=title:${item.title} | type:${item.mediaType} | image:${item.imageUrl} | media:${item.mediaUrl} | unlock:${item.memoryUnlockLevel} | note:${item.note}"
                )
                emitted += 1
            }
            voiceRows.forEachIndexed { index, entry ->
                if (emitted >= limit) return@forEachIndexed
                val audioUrls = (entry.audioUrls + entry.audioUrl).map { it.trim() }.filter { it.isNotBlank() }.distinct()
                appendLine(
                    "voice[$index]=section:${entry.section} | title:${entry.title} | audioCount:${audioUrls.size} | audio:${audioUrls.firstOrNull().orEmpty()}"
                )
                emitted += 1
            }
            appendLine("returned=$emitted")
        }.trim()
    }

    private fun normalizeGuideMediaKind(kind: String): String {
        return when (kind.trim().lowercase(Locale.ROOT)) {
            "gallery", "image", "video", "voice", "audio" -> kind.trim().lowercase(Locale.ROOT)
            else -> "all"
        }
    }

    private fun buildGuideBgmFavoritesText(
        action: String,
        query: String,
        limit: Int,
        rawJson: String,
        apply: Boolean
    ): String {
        return when (action.trim().lowercase(Locale.ROOT)) {
            "export" -> GuideBgmFavoriteStore.buildFavoritesExportJson()
            "import" -> buildGuideBgmFavoritesImportText(rawJson = rawJson, apply = apply)
            else -> buildGuideBgmFavoritesListText(query = query, limit = limit)
        }
    }

    private fun buildGuideBgmFavoritesListText(query: String, limit: Int): String {
        val favorites = GuideBgmFavoriteStore.favoritesSnapshot()
        val key = query.trim().lowercase(Locale.ROOT)
        val filtered = if (key.isBlank()) {
            favorites
        } else {
            favorites.filter { favorite ->
                favorite.audioUrl.lowercase(Locale.ROOT).contains(key) ||
                    favorite.title.lowercase(Locale.ROOT).contains(key) ||
                    favorite.studentTitle.lowercase(Locale.ROOT).contains(key) ||
                    favorite.sourceUrl.lowercase(Locale.ROOT).contains(key) ||
                    favorite.note.lowercase(Locale.ROOT).contains(key)
            }
        }
        val rows = filtered.take(limit)
        return buildString {
            appendLine("total=${favorites.size}")
            appendLine("matched=${filtered.size}")
            appendLine("returned=${rows.size}")
            appendLine("query=${query.ifBlank { "(none)" }}")
            rows.forEachIndexed { index, favorite ->
                appendLine(
                    "favorite[$index]=title:${favorite.title} | student:${favorite.studentTitle} | audio:${favorite.audioUrl} | source:${favorite.sourceUrl} | favoritedAtMs:${favorite.favoritedAtMs}"
                )
            }
        }.trim()
    }

    private fun buildGuideBgmFavoritesImportText(rawJson: String, apply: Boolean): String {
        val importedUrls = parseBgmFavoriteAudioUrls(rawJson)
        val existingUrls = GuideBgmFavoriteStore.favoritesSnapshot().map { it.audioUrl }.toSet()
        val addedCount = importedUrls.count { it !in existingUrls }
        val updatedCount = importedUrls.count { it in existingUrls }
        val result = if (apply) GuideBgmFavoriteStore.importFavoritesJsonMerged(rawJson) else null
        return buildString {
            appendLine("apply=$apply")
            appendLine("importedCount=${result?.importedCount ?: importedUrls.size}")
            appendLine("newCount=${result?.addedCount ?: addedCount}")
            appendLine("updatedCount=${result?.updatedCount ?: updatedCount}")
            appendLine("applied=${result != null}")
        }.trim()
    }

    private fun parseBgmFavoriteAudioUrls(rawJson: String): List<String> {
        val trimmed = rawJson.trim()
        if (trimmed.isBlank()) return emptyList()
        return runCatching {
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val root = JSONObject(trimmed)
                root.optJSONArray("favorites")
                    ?: root.optJSONArray("bgmFavorites")
                    ?: JSONArray()
            }
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val audioUrl = item.optString("audioUrl").trim()
                    if (audioUrl.isNotBlank()) add(audioUrl)
                }
            }.distinct()
        }.getOrDefault(emptyList())
    }

    private fun normalizeCacheClearScope(raw: String): String {
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "ba_calendar_pool", "calendar_pool", "ba-calendar-pool" -> "ba_calendar_pool"
            "ba_guide_catalog", "guide_catalog", "ba-guide-catalog" -> "ba_guide_catalog"
            "ba_guide_all", "guide_all", "ba-guide-all" -> "ba_guide_all"
            "ba_guide_url", "guide_url", "ba-guide-url" -> "ba_guide_url"
            "github_check", "github", "github_cache", "github-check" -> "github_check"
            else -> "all"
        }
    }

    private fun buildCacheClearText(scope: String, url: String): String {
        val normalizedScope = normalizeCacheClearScope(scope)
        val cleared = mutableListOf<String>()
        var message = "ok"

        when (normalizedScope) {
            "ba_calendar_pool" -> {
                BASettingsStore.clearCalendarAndPoolCaches()
                cleared += "ba_calendar_pool"
            }

            "ba_guide_catalog" -> {
                clearBaGuideCatalogCache(appContext)
                cleared += "ba_guide_catalog"
            }

            "ba_guide_all" -> {
                BaStudentGuideStore.clearAllCachedInfo()
                cleared += "ba_guide_all"
            }

            "ba_guide_url" -> {
                val target = normalizeGuideUrl(url)
                if (target.isBlank()) {
                    message = "url_required_for_ba_guide_url"
                } else {
                    BaStudentGuideStore.clearCachedInfo(target)
                    cleared += "ba_guide_url:$target"
                }
            }

            "github_check" -> {
                GitHubTrackStore.clearCheckCache()
                GitHubReleaseAssetCacheStore.clearAll()
                cleared += "github_check"
            }

            else -> {
                BASettingsStore.clearCalendarAndPoolCaches()
                BaStudentGuideStore.clearAllCachedInfo()
                clearBaGuideCatalogCache(appContext)
                GitHubTrackStore.clearCheckCache()
                GitHubReleaseAssetCacheStore.clearAll()
                cleared += "ba_calendar_pool"
                cleared += "ba_guide_all"
                cleared += "ba_guide_catalog"
                cleared += "github_check"
            }
        }

        return buildString {
            appendLine("scope=$normalizedScope")
            appendLine("message=$message")
            appendLine("cleared=${if (cleared.isEmpty()) "none" else cleared.joinToString(",")}")
        }.trim()
    }
}
