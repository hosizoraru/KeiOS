package os.kei.ui.page.main.debug

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugBgmMusicUiStateTest {
    @Test
    fun `previous wraps to the end of the queue`() {
        val state = DebugBgmMusicUiState(sampleTracks)

        state.playTrack("track-a")
        state.playPrevious()

        assertEquals("track-c", state.currentTrack.id)
        assertTrue(state.isPlaying)
    }

    @Test
    fun `playback completion keeps current track when repeat is enabled`() {
        val state = DebugBgmMusicUiState(sampleTracks)

        state.playTrack("track-a")
        state.toggleRepeat()
        state.advanceQueueFromPlayback()

        assertEquals("track-a", state.currentTrack.id)
        assertTrue(state.isPlaying)
    }

    @Test
    fun `track favorite and offline states toggle independently`() {
        val state = DebugBgmMusicUiState(sampleTracks)

        state.toggleTrackFavorite("track-a")
        state.toggleTrackOffline("track-a")

        assertFalse(state.isTrackFavorite("track-a"))
        assertTrue(state.isTrackOfflineSaved("track-a"))
        assertEquals(2, state.favoriteTrackCount)
        assertEquals(1, state.offlineTrackCount)
    }

    @Test
    fun `dock selection closes transient search chrome`() {
        val state = DebugBgmMusicUiState(sampleTracks)

        state.toggleSearch()
        state.updateSearchQuery("Beta")
        state.selectDockKey(DebugBgmDockKeys.Radio)

        assertEquals(DebugBgmDockKeys.Radio, state.selectedDockKey)
        assertFalse(state.searchVisible)
        assertEquals("Beta", state.searchQuery)
    }
}

private val sampleTracks = listOf(
    DebugBgmTrack(
        id = "track-a",
        title = "Alpha",
        subtitle = "Student BGM",
        durationLabel = "01:00",
        searchAlias = "alpha"
    ),
    DebugBgmTrack(
        id = "track-b",
        title = "Beta",
        subtitle = "Student BGM",
        durationLabel = "02:00",
        searchAlias = "beta"
    ),
    DebugBgmTrack(
        id = "track-c",
        title = "Gamma",
        subtitle = "Student BGM",
        durationLabel = "03:00",
        searchAlias = "gamma"
    )
)
