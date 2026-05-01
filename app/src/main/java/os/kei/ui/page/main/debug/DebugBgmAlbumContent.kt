package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun DebugBgmAlbumContent(
    accent: Color,
    tracks: List<DebugBgmTrack>,
    currentTrackId: String,
    isPlaying: Boolean,
    repeatEnabled: Boolean,
    playbackVolume: Float,
    isTrackFavorite: (String) -> Boolean,
    onRepeatClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: (Float) -> Unit,
    onTrackClick: (String) -> Unit,
    onTrackFavoriteClick: (String) -> Unit,
    onTrackOfflineClick: (String) -> Unit,
    onTrackShareClick: (DebugBgmTrack) -> Unit,
    isTrackOfflineSaved: (String) -> Boolean,
    sectionTitle: String,
    sectionMeta: String,
    sectionFooterTitle: String,
    offlineTrackCount: Int,
    listState: LazyListState,
    collapseProgress: Float,
    bottomBarScrollConnection: NestedScrollConnection,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(bottomBarScrollConnection),
        contentPadding = PaddingValues(start = 16.dp, top = topPadding, end = 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DebugBgmAlbumHero(
                accent = accent,
                collapseProgress = collapseProgress,
                repeatEnabled = repeatEnabled,
                isPlaying = isPlaying,
                playbackVolume = playbackVolume,
                sectionTitle = sectionTitle,
                sectionMeta = sectionMeta,
                onRepeatClick = onRepeatClick,
                onPlayPauseClick = onPlayPauseClick,
                onVolumeChange = onVolumeChange,
                onVolumeChangeFinished = onVolumeChangeFinished
            )
        }
        item {
            DebugBgmTrackList(
                tracks = tracks,
                currentTrackId = currentTrackId,
                isPlaying = isPlaying,
                accent = accent,
                isTrackFavorite = isTrackFavorite,
                isTrackOfflineSaved = isTrackOfflineSaved,
                onTrackClick = onTrackClick,
                onTrackFavoriteClick = onTrackFavoriteClick,
                onTrackOfflineClick = onTrackOfflineClick,
                onTrackShareClick = onTrackShareClick
            )
        }
        item {
            DebugBgmAlbumFooter(
                sectionTitle = sectionFooterTitle,
                trackCount = tracks.size,
                offlineTrackCount = offlineTrackCount
            )
        }
    }
}
