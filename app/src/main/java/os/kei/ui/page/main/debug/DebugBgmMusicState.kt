package os.kei.ui.page.main.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import os.kei.R

@Stable
internal class DebugBgmMusicUiState(
    internal val tracks: List<DebugBgmTrack>
) {
    private val fallbackTrackId = tracks.getOrNull(2)?.id ?: tracks.firstOrNull()?.id.orEmpty()

    var currentTrackId by mutableStateOf(fallbackTrackId)
        private set
    var isPlaying by mutableStateOf(true)
        private set
    var repeatEnabled by mutableStateOf(false)
        private set
    var searchVisible by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var selectedDockKey by mutableStateOf(DebugBgmDockKeys.Library)
        private set

    private var favoriteTrackIds by mutableStateOf(tracks.map { it.id }.toSet())
    private var offlineTrackIds by mutableStateOf(emptySet<String>())

    val currentTrack: DebugBgmTrack
        get() = tracks.firstOrNull { it.id == currentTrackId }
            ?: tracks.firstOrNull()
            ?: DebugBgmTrack.Empty

    val visibleTracks: List<DebugBgmTrack>
        get() = tracks.filter { track -> track.matchesSearch(searchQuery) }

    val currentTrackOfflineSaved: Boolean
        get() = currentTrackId in offlineTrackIds

    fun togglePlayPause() {
        isPlaying = !isPlaying
    }

    fun playNext() {
        if (tracks.isEmpty()) return
        val currentIndex = tracks.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
        currentTrackId = tracks[(currentIndex + 1) % tracks.size].id
        isPlaying = true
    }

    fun playTrack(trackId: String) {
        if (tracks.none { it.id == trackId }) return
        currentTrackId = trackId
        isPlaying = true
        closeSearch()
    }

    fun toggleRepeat() {
        repeatEnabled = !repeatEnabled
    }

    fun toggleCurrentTrackOffline() {
        offlineTrackIds = offlineTrackIds.toggle(currentTrackId)
    }

    fun toggleTrackFavorite(trackId: String) {
        favoriteTrackIds = favoriteTrackIds.toggle(trackId)
    }

    fun isTrackFavorite(trackId: String): Boolean = trackId in favoriteTrackIds

    fun selectDockKey(key: String) {
        selectedDockKey = key
        searchVisible = false
    }

    fun toggleSearch() {
        searchVisible = !searchVisible
    }

    fun closeSearch() {
        searchVisible = false
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }
}

@Composable
internal fun rememberDebugBgmMusicUiState(): DebugBgmMusicUiState {
    val tracks = rememberDebugBgmTracks()
    return remember(tracks) { DebugBgmMusicUiState(tracks) }
}

@Composable
private fun rememberDebugBgmTracks(): List<DebugBgmTrack> {
    val track1Title = stringResource(R.string.debug_component_lab_track_1_title)
    val track1Subtitle = stringResource(R.string.debug_component_lab_track_1_subtitle)
    val track1Duration = stringResource(R.string.debug_component_lab_track_1_duration)
    val track1Alias = stringResource(R.string.debug_component_lab_track_1_alias)
    val track2Title = stringResource(R.string.debug_component_lab_track_2_title)
    val track2Subtitle = stringResource(R.string.debug_component_lab_track_2_subtitle)
    val track2Duration = stringResource(R.string.debug_component_lab_track_2_duration)
    val track2Alias = stringResource(R.string.debug_component_lab_track_2_alias)
    val track3Title = stringResource(R.string.debug_component_lab_track_3_title)
    val track3Subtitle = stringResource(R.string.debug_component_lab_track_3_subtitle)
    val track3Duration = stringResource(R.string.debug_component_lab_track_3_duration)
    val track3Alias = stringResource(R.string.debug_component_lab_track_3_alias)
    val track4Title = stringResource(R.string.debug_component_lab_track_4_title)
    val track4Subtitle = stringResource(R.string.debug_component_lab_track_4_subtitle)
    val track4Duration = stringResource(R.string.debug_component_lab_track_4_duration)
    val track4Alias = stringResource(R.string.debug_component_lab_track_4_alias)
    return remember(
        track1Title,
        track1Subtitle,
        track1Duration,
        track1Alias,
        track2Title,
        track2Subtitle,
        track2Duration,
        track2Alias,
        track3Title,
        track3Subtitle,
        track3Duration,
        track3Alias,
        track4Title,
        track4Subtitle,
        track4Duration,
        track4Alias
    ) {
        listOf(
            DebugBgmTrack(
                id = DebugBgmTrackIds.Hoshino,
                title = track1Title,
                subtitle = track1Subtitle,
                durationLabel = track1Duration,
                searchAlias = track1Alias
            ),
            DebugBgmTrack(
                id = DebugBgmTrackIds.Yuuka,
                title = track2Title,
                subtitle = track2Subtitle,
                durationLabel = track2Duration,
                searchAlias = track2Alias
            ),
            DebugBgmTrack(
                id = DebugBgmTrackIds.Shiroko,
                title = track3Title,
                subtitle = track3Subtitle,
                durationLabel = track3Duration,
                searchAlias = track3Alias
            ),
            DebugBgmTrack(
                id = DebugBgmTrackIds.Momoi,
                title = track4Title,
                subtitle = track4Subtitle,
                durationLabel = track4Duration,
                searchAlias = track4Alias
            )
        )
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    if (value.isBlank()) return this
    return if (value in this) this - value else this + value
}

private fun DebugBgmTrack.matchesSearch(query: String): Boolean {
    val tokens = query
        .trim()
        .split(DebugBgmSearchTokenSplitRegex)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val fields = listOf(title, subtitle, durationLabel, searchAlias)
    return tokens.all { token ->
        fields.any { field -> field.contains(token, ignoreCase = true) }
    }
}

private val DebugBgmSearchTokenSplitRegex = Regex("""\s+""")

internal data class DebugBgmTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationLabel: String,
    val searchAlias: String
) {
    companion object {
        val Empty = DebugBgmTrack(
            id = "",
            title = "",
            subtitle = "",
            durationLabel = "",
            searchAlias = ""
        )
    }
}

private object DebugBgmTrackIds {
    const val Hoshino = "hoshino"
    const val Yuuka = "yuuka"
    const val Shiroko = "shiroko"
    const val Momoi = "momoi"
}
