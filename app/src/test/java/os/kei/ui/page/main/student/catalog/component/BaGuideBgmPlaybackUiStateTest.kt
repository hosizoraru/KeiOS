package os.kei.ui.page.main.student.catalog.component

import org.junit.Test
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.testBgmFavorite
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class BaGuideBgmPlaybackUiStateTest {
    @Test
    fun `resolved selection prefers queue item`() {
        val queueFavorite = favorite("same-url", "queue")
        val state =
            BaGuideBgmPlaybackUiState(
                favorites = listOf(favorite("same-url", "favorite")),
                queue = listOf(queueFavorite),
                selectedAudioUrl = "same-url",
            ).withResolvedSelection()

        assertEquals(queueFavorite, state.selectedFavorite)
        assertEquals(queueFavorite, state.selectedQueueFavorite)
    }

    @Test
    fun `resolved selection falls back to first available favorite`() {
        val fallbackFavorite = favorite("fallback-url", "fallback")
        val state =
            BaGuideBgmPlaybackUiState(
                favorites = listOf(fallbackFavorite),
                selectedAudioUrl = "missing-url",
            ).withResolvedSelection()

        assertEquals(fallbackFavorite, state.selectedFavorite)
        assertNull(state.selectedQueueFavorite)
    }

    @Test
    fun `retained playing queue acquires late favorite artwork without changing order`() {
        val selected = favorite("same-url", "queue").copy(studentImageUrl = " ", imageUrl = "")
        val saved = selected.copy(studentImageUrl = "https://example.com/student.png")
        val state = BaGuideBgmPlaybackUiState(
            favorites = listOf(saved),
            queue = listOf(favorite("other", "other"), selected),
            selectedAudioUrl = "same-url",
        ).withResolvedSelection()
        assertEquals(listOf("other", "same-url"), state.queue.map { it.audioUrl })
        assertEquals("same-url", state.selectedAudioUrl)
        assertEquals(saved.studentImageUrl, state.selectedQueueFavorite?.studentImageUrl)
        assertEquals("queue", state.selectedFavorite?.title)
    }

    @Test
    fun `shared audio cannot borrow artwork from a different explicit student identity`() {
        val selected = favorite("same-url", "same name").copy(
            sourceUrl = "https://www.gamekee.com/ba/tj/1.html",
        )
        val other = selected.copy(
            sourceUrl = "https://www.gamekee.com/ba/tj/2.html",
            studentImageUrl = "https://example.com/other-student.png",
        )
        val state = BaGuideBgmPlaybackUiState(
            favorites = listOf(other),
            queue = listOf(selected),
            selectedAudioUrl = "same-url",
        ).withResolvedSelection()
        assertEquals(selected, state.selectedQueueFavorite)
        assertEquals(selected, state.selectedFavorite)
    }

    @Test
    fun `native session restores an unfavorited track and queue to an empty player`() {
        val first = favorite("first-url", "First")
        val playing = favorite("playing-url", "Playing").copy(
            studentImageUrl = "https://example.com/playing.png",
        )
        val state = BaGuideBgmPlaybackUiState().withNativeSession(
            BaGuideBgmNativeSessionSnapshot(
                queue = listOf(first, playing),
                selectedAudioUrl = playing.audioUrl,
                queueMode = BaGuideBgmQueueMode.SingleLoop,
            ),
            restoreQueue = true,
        )

        assertEquals(playing, state.selectedFavorite)
        assertEquals(playing, state.selectedQueueFavorite)
        assertEquals(playing.audioUrl, state.selectedAudioUrl)
        assertEquals(listOf(first, playing), state.queue)
        assertEquals(BaGuideBgmQueueMode.SingleLoop, state.queueMode)
        assertEquals(emptyList(), state.favorites)
    }

    @Test
    fun `surviving session takes precedence over a different saved favorite`() {
        val saved = favorite("favorite-url", "Saved")
        val playing = favorite("playing-url", "Playing")
        val state = BaGuideBgmPlaybackUiState(
            favorites = listOf(saved),
            queue = listOf(saved),
            selectedAudioUrl = saved.audioUrl,
        ).withResolvedSelection().withNativeSession(
            BaGuideBgmNativeSessionSnapshot(
                queue = listOf(playing),
                selectedAudioUrl = playing.audioUrl,
                queueMode = BaGuideBgmQueueMode.Continuous,
            ),
            restoreQueue = true,
        )

        assertEquals(playing, state.selectedFavorite)
        assertEquals(listOf(saved), state.favorites)
        assertEquals(listOf(playing), state.queue)
    }

    @Test
    fun `native next track outside the visible list retains its title and artwork`() {
        val visible = favorite("visible-url", "Visible")
        val playing = favorite("playing-url", "Playing").copy(
            studentImageUrl = "https://example.com/playing.png",
        )
        val state = BaGuideBgmPlaybackUiState(
            queue = listOf(visible),
            selectedAudioUrl = visible.audioUrl,
        ).withResolvedSelection().withNativeSession(
            BaGuideBgmNativeSessionSnapshot(
                queue = listOf(visible, playing),
                selectedAudioUrl = playing.audioUrl,
                queueMode = BaGuideBgmQueueMode.Continuous,
            ),
        )

        assertEquals(listOf(playing, visible), state.queue)
        assertEquals(playing, state.selectedFavorite)
    }

    @Test
    fun `an incomplete native snapshot does not replace an existing selection`() {
        val existing = favorite("existing-url", "Existing")
        val state = BaGuideBgmPlaybackUiState(
            queue = listOf(existing),
            selectedAudioUrl = existing.audioUrl,
        ).withResolvedSelection()

        assertSame(
            state,
            state.withNativeSession(
                BaGuideBgmNativeSessionSnapshot(
                    queue = emptyList(),
                    selectedAudioUrl = "missing-url",
                    queueMode = BaGuideBgmQueueMode.SingleLoop,
                ),
                restoreQueue = true,
            ),
        )
    }

    private fun favorite(
        audioUrl: String,
        title: String,
    ): GuideBgmFavoriteItem = testBgmFavorite(audioUrl = audioUrl, title = title, studentTitle = title)
}
