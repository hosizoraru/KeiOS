package os.kei.ui.page.main.student.catalog.component

import org.junit.Test
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.state.favoriteStudentBgmEntryContentIds
import os.kei.ui.page.main.student.catalog.state.filterAndSortStudentBgmEntries
import os.kei.ui.page.main.student.catalog.state.visibleCatalogEntriesWithFavoriteVisibility
import os.kei.ui.page.main.student.catalog.state.visibleMemoryLobbyEntriesWithFavoriteVisibility
import os.kei.ui.page.main.student.catalog.state.visibleStudentBgmEntriesWithFavoriteVisibility
import os.kei.ui.page.main.student.catalog.testBgmFavorite
import os.kei.ui.page.main.student.catalog.testCatalogEntry
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class BaGuideStudentBgmDisplayedModelTest {
    @Test
    fun `favorite content ids resolve from normalized source urls`() {
        val entries =
            listOf(
                testCatalogEntry(contentId = 1L, name = "Alice", order = 1),
                testCatalogEntry(contentId = 2L, name = "Bob", order = 2),
            )
        val favorites =
            mapOf(
                "https://www.gamekee.com/ba/2" to favorite(sourceUrl = "https://www.gamekee.com/ba/2"),
            )

        val ids = favoriteStudentBgmEntryContentIds(entries, favorites)

        assertEquals(setOf(2L), ids)
    }

    @Test
    fun `filter and sort places favorites first then entry order`() {
        val entries =
            listOf(
                testCatalogEntry(contentId = 1L, name = "Alice", order = 1),
                testCatalogEntry(contentId = 2L, name = "Bob", order = 2),
                testCatalogEntry(contentId = 3L, name = "Carol", order = 3),
            )

        val result =
            filterAndSortStudentBgmEntries(
                entries = entries,
                searchQuery = "",
                favoriteContentIds = setOf(3L),
            )

        assertEquals(listOf(3L, 1L, 2L), result.map { it.contentId })
    }

    @Test
    fun `favorite visibility hides favorites only when asked`() {
        data class Case(
            val label: String,
            val entryIds: List<Long>,
            val favoriteId: Long,
            val visible: (List<BaGuideCatalogEntry>, Long, Boolean) -> List<BaGuideCatalogEntry>,
        )
        listOf(
            Case("student bgm", listOf(3L, 1L, 2L), 3L) { entries, id, hidden ->
                visibleStudentBgmEntriesWithFavoriteVisibility(entries, setOf(id), hidden)
            },
            Case("memory lobby", listOf(2L, 1L, 3L), 2L) { entries, id, hidden ->
                visibleMemoryLobbyEntriesWithFavoriteVisibility(entries, setOf(id), hidden)
            },
            Case("catalog", listOf(2L, 1L, 3L), 2L) { entries, id, hidden ->
                visibleCatalogEntriesWithFavoriteVisibility(entries, mapOf(id to 100L), hidden)
            },
        ).forEach { case ->
            val entries = case.entryIds.map { id -> testCatalogEntry(contentId = id, name = "Student $id", order = id.toInt()) }

            assertEquals(
                case.entryIds - case.favoriteId,
                case.visible(entries, case.favoriteId, true).map { it.contentId },
                "${case.label}: hidden",
            )
            assertEquals(
                case.entryIds,
                case.visible(entries, case.favoriteId, false).map { it.contentId },
                "${case.label}: shown",
            )
        }
    }

    @Test
    fun `displayed model builds row state and favorite flags once`() {
        val entries =
            listOf(
                testCatalogEntry(contentId = 1L, name = "Alice", order = 1),
                testCatalogEntry(contentId = 2L, name = "Bob", order = 2),
            )
        val favorite = favorite(sourceUrl = "https://www.gamekee.com/ba/1")
        val readyFavorite = favorite(sourceUrl = "https://www.gamekee.com/ba/2")

        val model =
            buildBaGuideStudentBgmDisplayedModel(
                displayedEntries = entries,
                lookupStates =
                    mapOf(
                        2L to
                            BaGuideStudentBgmLookupState.Ready(
                                BaGuideStudentBgmResolvedItem(
                                    favorite = readyFavorite,
                                    fromCache = true,
                                ),
                            ),
                    ),
                favoriteByNormalizedSourceUrl =
                    mapOf(
                        "https://www.gamekee.com/ba/1" to favorite,
                    ),
                favoriteAudioUrls = setOf(favorite.audioUrl),
            )

        assertEquals(listOf(1L, 2L), model.contentIds)
        assertEquals(2, model.rows.size)
        assertIs<BaGuideStudentBgmLookupState.Ready>(model.rows[0].displayState)
        assertEquals(favorite.audioUrl, model.rows[0].readyAudioUrl)
        assertEquals(true, model.rows[0].favorite)
        assertEquals(readyFavorite.audioUrl, model.rows[1].readyAudioUrl)
        assertEquals(false, model.rows[1].favorite)
        assertEquals(listOf(favorite.audioUrl, readyFavorite.audioUrl), model.playableFavorites.map { it.audioUrl })
        assertEquals(2, model.resolvedCount)
    }

    @Test
    fun `displayed model fills favorite fallback artwork from entry`() {
        val entry =
            testCatalogEntry(
                contentId = 8L,
                name = "Aris",
                order = 1,
                iconUrl = "https://example.com/aris.png",
            )
        val favorite =
            favorite(sourceUrl = entry.detailUrl)
                .copy(studentTitle = "", studentImageUrl = "", imageUrl = "")

        val model =
            buildBaGuideStudentBgmDisplayedModel(
                displayedEntries = listOf(entry),
                lookupStates = emptyMap(),
                favoriteByNormalizedSourceUrl = mapOf(entry.detailUrl to favorite),
                favoriteAudioUrls = setOf(favorite.audioUrl),
            )

        val playableFavorite = model.playableFavorites.single()
        assertEquals(entry.name, playableFavorite.studentTitle)
        assertEquals(entry.iconUrl, playableFavorite.studentImageUrl)
        assertEquals(entry.iconUrl, playableFavorite.imageUrl)
        assertEquals(playableFavorite.audioUrl, model.rows.single().readyAudioUrl)
    }

    @Test
    fun `ready lookup receives current entry metadata in row and playback queue`() {
        val entry = testCatalogEntry(
            contentId = 8L,
            name = "Aris",
            iconUrl = "https://example.com/aris.png",
        )
        val favorite = favorite(entry.detailUrl).copy(studentTitle = " ", studentImageUrl = "\t", imageUrl = "")
        val ready = BaGuideStudentBgmLookupState.Ready(
            BaGuideStudentBgmResolvedItem(favorite, fromCache = true, fromFavorite = true),
        )

        val model = buildBaGuideStudentBgmDisplayedModel(
            displayedEntries = listOf(entry),
            lookupStates = mapOf(entry.contentId to ready),
            favoriteByNormalizedSourceUrl = emptyMap(),
            favoriteAudioUrls = setOf(favorite.audioUrl),
        )

        val expected = favorite.copy(
            studentTitle = entry.name,
            studentImageUrl = entry.iconUrl,
            imageUrl = entry.iconUrl,
        )
        val rowState = assertIs<BaGuideStudentBgmLookupState.Ready>(model.rows.single().displayState)
        assertEquals(ready.item.copy(favorite = expected), rowState.item)
        assertEquals(listOf(expected), model.playableFavorites)
        assertEquals(true, model.rows.single().favorite)
        assertEquals(
            rowState,
            studentBgmStateWithFavoriteFallback(entry, ready, emptyMap()),
        )
    }

    @Test
    fun `ready lookup with complete artwork keeps existing source and state identity`() {
        val entry = testCatalogEntry(contentId = 8L, name = "Aris", iconUrl = "https://example.com/icon.png")
        val favorite = favorite(entry.detailUrl).copy(
            studentImageUrl = "https://example.com/original.png",
            imageUrl = "https://example.com/gallery.png",
        )
        val ready = BaGuideStudentBgmLookupState.Ready(
            BaGuideStudentBgmResolvedItem(favorite, fromCache = true),
        )

        val model = buildBaGuideStudentBgmDisplayedModel(
            displayedEntries = listOf(entry),
            lookupStates = mapOf(entry.contentId to ready),
            favoriteByNormalizedSourceUrl = emptyMap(),
            favoriteAudioUrls = emptySet(),
        )

        assertSame(ready, model.rows.single().displayState)
        assertEquals(listOf(favorite), model.playableFavorites)
    }

    private fun favorite(sourceUrl: String): GuideBgmFavoriteItem =
        testBgmFavorite(audioUrl = "$sourceUrl/audio.mp3", sourceUrl = sourceUrl, title = "BGM", studentTitle = "Demo", favoritedAtMs = 0L)
}
