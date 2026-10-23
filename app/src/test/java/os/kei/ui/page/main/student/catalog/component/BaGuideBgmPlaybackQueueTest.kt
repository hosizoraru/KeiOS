package os.kei.ui.page.main.student.catalog.component

import org.junit.Test
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.testBgmFavorite
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BaGuideBgmPlaybackQueueTest {
    @Test
    fun `opening an empty favorite library preserves the native playlist`() {
        val native = nativeSession()

        val queue = resolveBaGuideBgmFavoritePlaybackQueue(emptyList(), native)
        val selection = resolveBaGuideBgmPlaybackQueueSelection(queue, native.selectedAudioUrl)

        assertEquals(native.queue, selection.queue)
        assertEquals("b", selection.selectedAudioUrl)
        assertEquals("c", selectBaGuideBgmPlaybackQueueOffset(queue, "b", 1)?.audioUrl)
    }

    @Test
    fun `other saved favorites cannot replace an unfavorited native playlist`() {
        val native = nativeSession()
        val favorites = listOf(track("saved"))

        assertEquals(native.queue, resolveBaGuideBgmFavoritePlaybackQueue(favorites, native))
    }

    @Test
    fun `a visible favorite still uses the displayed favorite queue`() {
        val favorites = listOf(track("b"), track("saved"))

        assertEquals(favorites, resolveBaGuideBgmFavoritePlaybackQueue(favorites, nativeSession()))
    }

    @Test
    fun `no live native selection leaves favorite queue behavior intact`() {
        val favorites = listOf(track("saved"))

        assertEquals(favorites, resolveBaGuideBgmFavoritePlaybackQueue(favorites, null))
        assertEquals(
            favorites,
            resolveBaGuideBgmFavoritePlaybackQueue(
                favorites,
                nativeSession().copy(selectedAudioUrl = "unknown"),
            ),
        )
    }

    @Test
    fun `queue selection keeps current hidden track when metadata is available`() {
        val selection = resolveBaGuideBgmPlaybackQueueSelection(
            nextQueue = listOf(track("a"), track("b"), track("c")),
            currentSelectedAudioUrl = "outside",
            currentSelectedFavorite = track("outside"),
        )

        assertEquals("outside", selection.selectedAudioUrl)
        assertEquals(listOf("outside", "a", "b", "c"), selection.queue.map { it.audioUrl })
    }

    @Test
    fun `queue selection clamps invalid current track without metadata`() {
        val selection = resolveBaGuideBgmPlaybackQueueSelection(
            nextQueue = listOf(track("a"), track("b"), track("c")),
            currentSelectedAudioUrl = "outside",
        )

        assertEquals("a", selection.selectedAudioUrl)
        assertEquals(listOf("a", "b", "c"), selection.queue.map { it.audioUrl })
    }

    @Test
    fun `offset moves and wraps inside the queue`() {
        val queue = listOf(track("a"), track("b"), track("c"))

        listOf(
            Triple("b", 1, "c"),
            Triple("b", -1, "a"),
            Triple("c", 1, "a"),
            Triple("a", -1, "c"),
        ).forEach { (selected, offset, expected) ->
            assertEquals(
                expected,
                selectBaGuideBgmPlaybackQueueOffset(
                    queue = queue,
                    selectedAudioUrl = selected,
                    offset = offset
                )?.audioUrl,
                "$selected offset $offset",
            )
        }
    }

    @Test
    fun `empty queue keeps selection and disables offset selection`() {
        val selection = resolveBaGuideBgmPlaybackQueueSelection(
            nextQueue = emptyList(),
            currentSelectedAudioUrl = "outside"
        )

        assertEquals("outside", selection.selectedAudioUrl)
        assertNull(
            selectBaGuideBgmPlaybackQueueOffset(
                queue = selection.queue,
                selectedAudioUrl = selection.selectedAudioUrl,
                offset = 1
            )
        )
    }

    @Test
    fun `duplicate queue entries keep first occurrence`() {
        val selection = resolveBaGuideBgmPlaybackQueueSelection(
            nextQueue = listOf(track("a"), track("a"), track("b")),
            currentSelectedAudioUrl = "b"
        )

        assertEquals(listOf("a", "b"), selection.queue.map { it.audioUrl })
        assertEquals("b", selection.selectedAudioUrl)
    }

    private fun track(id: String): GuideBgmFavoriteItem =
        testBgmFavorite(
            audioUrl = id,
            sourceUrl = "https://www.gamekee.com/ba/tj/$id.html",
            title = "Track $id",
            studentTitle = "Student $id",
        )

    private fun nativeSession() = BaGuideBgmNativeSessionSnapshot(
        queue = listOf(track("a"), track("b"), track("c")),
        selectedAudioUrl = "b",
        queueMode = BaGuideBgmQueueMode.Continuous,
    )
}
