package os.kei.ui.page.main.student.catalog.state

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.ui.page.main.student.BaGuideDataClock
import os.kei.ui.page.main.student.BaGuideSystemDataClock
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntryFilterAttributes
import os.kei.ui.page.main.student.catalog.BaGuideCatalogFilterDefinition
import os.kei.ui.page.main.student.catalog.BaGuideCatalogFilterOption
import os.kei.ui.page.main.student.catalog.BaGuideCatalogRefreshMode
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmFavoriteSortMode
import os.kei.ui.page.main.student.catalog.isBaGuideCatalogCacheExpired
import os.kei.ui.page.main.student.catalog.testBgmFavorite
import os.kei.ui.page.main.student.catalog.testCatalogEntry
import os.kei.ui.page.main.student.fetch.extractGuideContentIdFromUrl
import kotlin.test.assertEquals

class BaGuideCatalogRepositoryTest {
    @Test
    fun `refresh failure keeps complete cached bundle`() =
        runBlocking {
            val cached = catalogBundle("缓存学生")
            val result =
                repository(
                    cached = cached,
                    complete = { it === cached },
                    expired = { _, _, _ -> true },
                    intervalHours = 1,
                ).load(manualRefresh = true)

            assertEquals(cached, result.catalog)
            assertEquals("保留缓存", result.error)
        }

    @Test
    fun `refresh failure keeps partial cached entries`() =
        runBlocking {
            val cached = catalogBundle("旧缓存")
            val result =
                repository(
                    cached = cached,
                    complete = { false },
                    expired = { _, _, _ -> true },
                    intervalHours = 1,
                ).load()

            assertEquals(cached, result.catalog)
            assertEquals("保留缓存", result.error)
        }

    @Test
    fun `fresh complete cache returns without network fetch`() =
        runBlocking {
            val cached = catalogBundle("新缓存")
            var fetchCalled = false
            var observedNowMs = 0L
            val result =
                repository(
                    cached = cached,
                    fetcher = { _, _, _, _, _ ->
                        fetchCalled = true
                        BaGuideCatalogBundle.EMPTY
                    },
                    complete = { it === cached },
                    expired = { _, _, nowMs ->
                        observedNowMs = nowMs
                        false
                    },
                    clock = BaGuideDataClock { 88_000L },
                ).load()

            assertEquals(cached, result.catalog)
            assertEquals(null, result.error)
            assertEquals(false, fetchCalled)
            assertEquals(88_000L, observedNowMs)
        }

    @Test
    fun `expired complete cache fetches incremental refresh before full cadence`() =
        runBlocking {
            val syncedAtMs = 1_000L
            val cached =
                catalogBundle("增量缓存").copy(
                    syncedAtMs = syncedAtMs,
                    fullSyncedAtMs = syncedAtMs,
                )
            val fetched = catalogBundle("增量结果")
            var observedMode: BaGuideCatalogRefreshMode? = null
            val result =
                repository(
                    cached = cached,
                    fetcher = { _, _, _, _, refreshMode ->
                        observedMode = refreshMode
                        fetched
                    },
                    complete = { true },
                    clock = BaGuideDataClock { syncedAtMs + 13L * 60L * 60L * 1000L },
                ).load()

            assertEquals(fetched, result.catalog)
            assertEquals(BaGuideCatalogRefreshMode.Incremental, observedMode)
        }

    /** Also the only test that a manual refresh bypasses a fresh, complete cache. */
    @Test
    fun `manual refresh uses full refresh mode`() =
        runBlocking {
            val cached = catalogBundle("手动刷新缓存")
            val fetched = catalogBundle("手动刷新结果")
            var observedMode: BaGuideCatalogRefreshMode? = null
            val result =
                repository(
                    cached = cached,
                    fetcher = { _, _, _, _, refreshMode ->
                        observedMode = refreshMode
                        fetched
                    },
                    complete = { true },
                    intervalHours = 24,
                    clock = BaGuideDataClock { cached.syncedAtMs + 60_000L },
                ).load(manualRefresh = true)

            assertEquals(fetched, result.catalog)
            assertEquals(BaGuideCatalogRefreshMode.Full, observedMode)
        }

    @Test
    fun `catalog list derivation filters current tab and pins favorites`() =
        runBlocking {
            val starDefinition =
                BaGuideCatalogFilterDefinition(
                    id = 68,
                    name = "星级",
                    type = 0,
                    options =
                        listOf(
                            BaGuideCatalogFilterOption(176, "三星"),
                            BaGuideCatalogFilterOption(70, "二星"),
                        ),
                )
            val favorite =
                catalogEntry(
                    name = "阿露",
                    tab = BaGuideCatalogTab.Student,
                    order = 2,
                    attributes = BaGuideCatalogEntryFilterAttributes(optionIdsByFilterId = mapOf(68 to setOf(176))),
                )
            val regular =
                catalogEntry(
                    name = "日富美",
                    tab = BaGuideCatalogTab.Student,
                    order = 1,
                    attributes = BaGuideCatalogEntryFilterAttributes(optionIdsByFilterId = mapOf(68 to setOf(176))),
                )
            val filteredOut =
                catalogEntry(
                    name = "晴",
                    tab = BaGuideCatalogTab.Student,
                    order = 3,
                    attributes = BaGuideCatalogEntryFilterAttributes(optionIdsByFilterId = mapOf(68 to setOf(70))),
                )
            val bundle =
                BaGuideCatalogBundle(
                    entriesByTab =
                        mapOf(
                            BaGuideCatalogTab.Student to listOf(regular, favorite, filteredOut),
                            BaGuideCatalogTab.NpcSatellite to emptyList(),
                        ),
                    syncedAtMs = 2_000L,
                    filterDefinitionsByTab = mapOf(BaGuideCatalogTab.Student to listOf(starDefinition)),
                )
            val repository = BaGuideCatalogRepository(parseDispatcher = Dispatchers.Unconfined)

            val result =
                repository.deriveCatalogListState(
                    BaGuideCatalogListInput(
                        catalog = bundle,
                        tab = BaGuideCatalogTab.Student,
                        sortMode = BaGuideCatalogSortMode.Default,
                        favoriteCatalogEntries = mapOf(favorite.contentId to 1L),
                        selectedFilterOptions = mapOf(68 to setOf(176), 9999 to setOf(1)),
                        searchQuery = "",
                    ),
                )

            assertEquals(1, result.activeFilterCount)
            assertEquals(listOf("阿露", "日富美"), result.filteredEntries.map { it.name })
            assertEquals(false, result.deriving)
        }

    @Test
    fun `student bgm list derivation pins favorite guide entries`() =
        runBlocking {
            val regular =
                catalogEntry(
                    name = "日富美",
                    tab = BaGuideCatalogTab.Student,
                    order = 1,
                )
            val favorite =
                catalogEntry(
                    name = "阿露",
                    tab = BaGuideCatalogTab.Student,
                    order = 2,
                )
            val bundle =
                BaGuideCatalogBundle(
                    entriesByTab =
                        mapOf(
                            BaGuideCatalogTab.Student to listOf(regular, favorite),
                            BaGuideCatalogTab.NpcSatellite to emptyList(),
                        ),
                    syncedAtMs = 3_000L,
                )
            val repository = BaGuideCatalogRepository(parseDispatcher = Dispatchers.Unconfined)

            val result =
                repository.deriveStudentBgmListState(
                    BaGuideStudentBgmListInput(
                        catalog = bundle,
                        favorites = listOf(bgmFavorite(sourceUrl = favorite.detailUrl)),
                        searchQuery = "",
                    ),
                )

            assertEquals(listOf("阿露", "日富美"), result.filteredEntries.map { it.name })
            assertEquals(listOf("日富美", "阿露"), result.allStudentEntries.map { it.name })
            assertEquals(listOf(favorite.detailUrl), result.favoriteByNormalizedSourceUrl.keys.toList())
            assertEquals(listOf("https://example.com/audio.ogg"), result.favoriteAudioUrls.toList())
            assertEquals(false, result.deriving)
        }

    @Test
    fun `memory lobby list derivation filters students by search and pins favorites`() =
        runBlocking {
            val favorite =
                catalogEntry(
                    name = "阿露",
                    tab = BaGuideCatalogTab.Student,
                    order = 3,
                )
            val regular =
                catalogEntry(
                    name = "日富美",
                    tab = BaGuideCatalogTab.Student,
                    order = 1,
                )
            val unmatched =
                catalogEntry(
                    name = "小春",
                    tab = BaGuideCatalogTab.Student,
                    order = 2,
                )
            val npc =
                catalogEntry(
                    name = "阿露 NPC",
                    tab = BaGuideCatalogTab.NpcSatellite,
                    order = 0,
                )
            val bundle =
                BaGuideCatalogBundle(
                    entriesByTab =
                        mapOf(
                            BaGuideCatalogTab.Student to listOf(regular, unmatched, favorite),
                            BaGuideCatalogTab.NpcSatellite to listOf(npc),
                        ),
                    syncedAtMs = 5_000L,
                )
            val repository = BaGuideCatalogRepository(parseDispatcher = Dispatchers.Unconfined)

            val result =
                repository.deriveMemoryLobbyListState(
                    BaGuideMemoryLobbyListInput(
                        catalog = bundle,
                        favoriteCatalogEntries = mapOf(favorite.contentId to 1L),
                        searchQuery = "阿露",
                    ),
                )

            assertEquals(listOf("阿露"), result.filteredEntries.map { it.name })
            assertEquals(listOf("日富美", "小春", "阿露"), result.allStudentEntries.map { it.name })
            assertEquals(false, result.deriving)

            val pinnedResult =
                repository.deriveMemoryLobbyListState(
                    BaGuideMemoryLobbyListInput(
                        catalog = bundle,
                        favoriteCatalogEntries = mapOf(favorite.contentId to 1L),
                        searchQuery = "",
                    ),
                )

            assertEquals(listOf("阿露", "日富美", "小春"), pinnedResult.filteredEntries.map { it.name })
        }

    @Test
    fun `favorite bgm list derivation sorts off composable path`() =
        runBlocking {
            val older =
                bgmFavorite(
                    sourceUrl = "https://www.gamekee.com/ba/1.html",
                    audioUrl = "https://example.com/old.ogg",
                    title = "Old Track",
                    studentTitle = "日富美",
                    favoritedAtMs = 1L,
                )
            val newer =
                bgmFavorite(
                    sourceUrl = "https://www.gamekee.com/ba/2.html",
                    audioUrl = "https://example.com/new.ogg",
                    title = "New Track",
                    studentTitle = "阿露",
                    favoritedAtMs = 2L,
                )
            val repository = BaGuideCatalogRepository(parseDispatcher = Dispatchers.Unconfined)

            val result =
                repository.deriveFavoriteBgmListState(
                    BaGuideFavoriteBgmListInput(
                        catalog = BaGuideCatalogBundle.EMPTY,
                        favorites = listOf(older, newer),
                        searchQuery = "",
                        sortMode = BaGuideBgmFavoriteSortMode.Recent,
                    ),
                )

            assertEquals(listOf("New Track", "Old Track"), result.displayedFavorites.map { it.title })
            assertEquals(listOf(newer.audioUrl, older.audioUrl), result.tracks.map { it.id })
            assertEquals(newer, result.favoritesByTrackId[newer.audioUrl])
            assertEquals(older, result.favoritesByTrackId[older.audioUrl])
            assertEquals(false, result.deriving)
        }

    @Test
    fun `favorite bgm list derivation fills missing artwork from catalog`() =
        runBlocking {
            val entry =
                catalogEntry(
                    name = "阿露",
                    tab = BaGuideCatalogTab.Student,
                    order = 1,
                )
            val bundle =
                BaGuideCatalogBundle(
                    entriesByTab =
                        mapOf(
                            BaGuideCatalogTab.Student to listOf(entry),
                            BaGuideCatalogTab.NpcSatellite to emptyList(),
                        ),
                    syncedAtMs = 4_000L,
                )
            val favorite =
                bgmFavorite(
                    sourceUrl = entry.detailUrl,
                    title = "Theme",
                    studentTitle = "",
                )
            assertEquals(entry.contentId, extractGuideContentIdFromUrl(favorite.sourceUrl))
            val repository = BaGuideCatalogRepository(parseDispatcher = Dispatchers.Unconfined)

            val result =
                repository.deriveFavoriteBgmListState(
                    BaGuideFavoriteBgmListInput(
                        catalog = bundle,
                        favorites = listOf(favorite),
                        searchQuery = "",
                        sortMode = BaGuideBgmFavoriteSortMode.Recent,
                    ),
                )

            val displayedFavorite = result.displayedFavorites.single()
            assertEquals(entry.name, displayedFavorite.studentTitle)
            assertEquals(entry.iconUrl, displayedFavorite.studentImageUrl)
            assertEquals(entry.iconUrl, displayedFavorite.imageUrl)
            assertEquals(displayedFavorite, result.favoritesByTrackId.getValue(favorite.audioUrl))
        }

    private fun repository(
        cached: BaGuideCatalogBundle?,
        fetcher: suspend (Boolean, CoroutineDispatcher, CoroutineDispatcher, BaGuideDataClock, BaGuideCatalogRefreshMode) -> BaGuideCatalogBundle =
            { _, _, _, _, _ -> error("blocked") },
        complete: (BaGuideCatalogBundle?) -> Boolean,
        expired: (BaGuideCatalogBundle?, Int, Long) -> Boolean = ::isBaGuideCatalogCacheExpired,
        intervalHours: Int = 12,
        clock: BaGuideDataClock = BaGuideSystemDataClock,
    ) = BaGuideCatalogRepository(
        ioDispatcher = Dispatchers.Unconfined,
        refreshIntervalLoader = { intervalHours },
        cachedBundleLoader = { cached },
        catalogFetcher = fetcher,
        completeChecker = complete,
        expiredChecker = expired,
        clock = clock,
    )

    private suspend fun BaGuideCatalogRepository.load(manualRefresh: Boolean = false) =
        loadCatalog(
            context = null,
            currentCatalog = BaGuideCatalogBundle.EMPTY,
            manualRefresh = manualRefresh,
            loadFailedText = "加载失败",
            refreshFailedKeepCacheText = "保留缓存",
        )

    private fun catalogBundle(name: String): BaGuideCatalogBundle =
        BaGuideCatalogBundle(
            entriesByTab =
                mapOf(
                    BaGuideCatalogTab.Student to listOf(catalogEntry(name, BaGuideCatalogTab.Student)),
                    BaGuideCatalogTab.NpcSatellite to
                        listOf(
                            catalogEntry("卫星", BaGuideCatalogTab.NpcSatellite),
                        ),
                ),
            syncedAtMs = 1_000L,
        )

    private fun catalogEntry(
        name: String,
        tab: BaGuideCatalogTab,
        order: Int = 0,
        attributes: BaGuideCatalogEntryFilterAttributes = BaGuideCatalogEntryFilterAttributes.EMPTY,
    ): BaGuideCatalogEntry {
        val hash = name.hashCode().let { if (it < 0) -it else it }
        val contentId = hash + order + 1L
        return testCatalogEntry(
            contentId = contentId,
            name = name,
            tab = tab,
            iconUrl = "https://example.com/icon.png",
            order = order,
            createdAtSec = 1L,
            detailUrl = "https://www.gamekee.com/ba/tj/$contentId.html",
            filterAttributes = attributes,
        )
    }

    private fun bgmFavorite(
        sourceUrl: String,
        audioUrl: String = "https://example.com/audio.ogg",
        title: String = "BGM",
        studentTitle: String = "学生",
        favoritedAtMs: Long = 1L,
    ): GuideBgmFavoriteItem = testBgmFavorite(audioUrl, sourceUrl, title, studentTitle, favoritedAtMs = favoritedAtMs)
}
