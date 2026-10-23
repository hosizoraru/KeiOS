@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import os.kei.R
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.appPageEdgePaddingStart
import os.kei.ui.page.main.widget.chrome.appPageEdgePaddingEnd
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmUndoBlock
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmPlaybackCoordinator
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmPlaybackUiState
import os.kei.ui.page.main.student.catalog.component.BaGuideBgmQueueMode
import os.kei.ui.page.main.student.catalog.component.bgm.BaGuideBgmAlbumContent
import os.kei.ui.page.main.student.catalog.component.resolveStudentArtworkImageUrl
import os.kei.ui.page.main.student.catalog.state.BaGuideFavoriteBgmListDerivedState
import os.kei.ui.page.main.student.catalog.state.BaGuideFavoriteBgmOfflineCacheUiState
import os.kei.ui.page.main.student.section.gallery.formatAudioDuration
import os.kei.ui.page.main.widget.chrome.appPageColumnCount
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop

@Composable
internal fun BaGuideFavoriteBgmMusicContent(
    catalog: BaGuideCatalogBundle,
    favorites: List<GuideBgmFavoriteItem>,
    derivedState: BaGuideFavoriteBgmListDerivedState,
    offlineCacheState: BaGuideFavoriteBgmOfflineCacheUiState,
    playbackCoordinator: BaGuideBgmPlaybackCoordinator,
    playbackState: BaGuideBgmPlaybackUiState,
    volumeControlVisible: Boolean,
    lastAudibleVolume: Float,
    accent: Color,
    bottomBarScrollConnection: NestedScrollConnection,
    topPadding: Dp,
    bottomPadding: Dp,
    isPageActive: Boolean,
    onSliderInteractionChanged: (Boolean) -> Unit,
    onVolumeControlVisibleChange: (Boolean) -> Unit,
    onLastAudibleVolumeChange: (Float) -> Unit,
    onScrollBoundsChange: (canScrollBackward: Boolean, canScrollForward: Boolean) -> Unit,
    onRemoveBgmFavorite: (String) -> Unit,
    /**
     * The favourite this list just removed, if it is still offering it back.
     *
     * Only this list carries the offer. Removing on the student BGM tab empties a heart and leaves the row,
     * so tapping again restores it; here the row leaves the only screen that lists it.
     */
    pendingUndoFavorite: GuideBgmFavoriteItem?,
    onUndoRemoveBgmFavorite: () -> Unit,
    onRequestOfflineCache: (List<GuideBgmFavoriteItem>, Boolean, Boolean) -> Unit,
    onToggleFavoriteCache: (GuideBgmFavoriteItem, List<GuideBgmFavoriteItem>) -> Unit,
    onRequestVisibleImages: (List<String>) -> Unit,
    onOpenGuide: (String) -> Unit,
    onRequestGuideDetailTab: (String, GuideBottomTab) -> Unit,
) {
    val contentBackdrop = LocalLiquidParentBackdrop.current
    val listState = rememberLazyListState()
    // The queue's own scroll, because on a tablet it is a pane beside the album rather than the rest of
    // one list -- scrolling the track list must not drag the artwork off the top of the window.
    val trackListState = rememberLazyListState()
    val columnCount = appPageColumnCount()
    // Split, the album pane does not scroll at all, so the queue is the only thing the chrome can watch.
    val paneStates =
        if (columnCount >= 2) listOf(trackListState) else listOf(listState)
    val lifecycleOwner = LocalLifecycleOwner.current
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    val requestVisibleImages by rememberUpdatedState(onRequestVisibleImages)
    val playbackRuntimeState by remember(playbackCoordinator, isPageActive) {
        if (isPageActive) {
            playbackCoordinator.runtimeStateFlow
                .map { runtime ->
                    BaGuideFavoriteBgmPlaybackRuntimeUiState(
                        isPlaying = runtime.isPlaying,
                        volume = runtime.volume,
                        durationMs = runtime.durationMs,
                    )
                }
                .distinctUntilChanged()
        } else {
            emptyFlow()
        }
    }.collectAsStateWithLifecycle(
        initialValue =
            playbackCoordinator.runtimeState.let { runtime ->
                BaGuideFavoriteBgmPlaybackRuntimeUiState(
                    isPlaying = runtime.isPlaying,
                    volume = runtime.volume,
                    durationMs = runtime.durationMs,
                )
            },
    )
    val selectedAudioUrl = playbackState.selectedAudioUrl
    val displayedTracks =
        remember(
            derivedState.tracks,
            selectedAudioUrl,
            playbackRuntimeState.durationMs,
        ) {
            val durationMs = playbackRuntimeState.durationMs
            if (durationMs <= 0L || selectedAudioUrl.isBlank()) {
                derivedState.tracks
            } else {
                derivedState.tracks.map { track ->
                    if (track.id == selectedAudioUrl) {
                        track.copy(durationLabel = formatAudioDuration(durationMs))
                    } else {
                        track
                    }
                }
            }
        }
    val displayedFavorites = derivedState.displayedFavorites
    val favoritesByTrackId = derivedState.favoritesByTrackId
    val favoriteOfflineCacheState =
        rememberBaGuideFavoriteBgmOfflineCacheState(
            uiState = offlineCacheState,
            onToggleFavoriteCache = { favorite ->
                onToggleFavoriteCache(favorite, displayedFavorites)
            },
        )
    LaunchedEffect(displayedFavorites, isPageActive) {
        onRequestOfflineCache(displayedFavorites, isPageActive, false)
    }
    LaunchedEffect(playbackCoordinator, displayedFavorites, isPageActive) {
        if (isPageActive) {
            playbackCoordinator.updateFavoriteQueue(displayedFavorites)
        }
    }
    LaunchedEffect(paneStates, isPageActive, snapshotFlowManager) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlowManager
            .snapshotFlow {
                // Either pane: the chrome expands for content that cannot scroll at all, and an album
                // pane that fits beside a queue that does not is not that.
                paneStates.any { state -> state.canScrollBackward } to
                    paneStates.any { state -> state.canScrollForward }
            }.distinctUntilChanged()
            .collect { (canScrollBackward, canScrollForward) ->
                onScrollBoundsChange(canScrollBackward, canScrollForward)
            }
    }
    val selectedFavorite =
        remember(
            playbackState.selectedAudioUrl,
            playbackState.queue,
            playbackState.favorites,
            displayedFavorites,
        ) {
            displayedFavorites.firstOrNull { it.audioUrl == playbackState.selectedAudioUrl }
                ?: playbackState.selectedFavorite
                ?: displayedFavorites.firstOrNull()
    }
    DisposableEffect(lifecycleOwner, selectedFavorite?.audioUrl, playbackCoordinator) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && !playbackCoordinator.keepsPlaybackAfterPageStop) {
                    selectedFavorite?.let { favorite ->
                        playbackCoordinator.pause(favorite)
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val sectionTitle =
        selectedFavorite
            ?.studentTitle
            ?.ifBlank { stringResource(R.string.ba_catalog_bgm_track_fallback) }
            ?: stringResource(R.string.ba_catalog_bgm_empty_title)
    val sectionMeta =
        if (selectedFavorite != null) {
            ""
        } else {
            stringResource(
                R.string.ba_catalog_bgm_library_summary,
                favorites.size,
                favoriteOfflineCacheState.offlineAudioUrls.size,
            )
        }
    val selectedArtworkImageUrl =
        remember(selectedFavorite, catalog) {
            selectedFavorite
                ?.resolveStudentArtworkImageUrl(catalog)
                .orEmpty()
        }

    LaunchedEffect(selectedArtworkImageUrl, selectedFavorite) {
        requestVisibleImages(
            buildBaGuidePlaybackForegroundImageUrls(
                artworkImageUrl = selectedArtworkImageUrl,
                playbackFavorite = selectedFavorite,
            ),
        )
    }

    fun playFavorite(
        favorite: GuideBgmFavoriteItem,
        restart: Boolean = false,
    ) {
        playbackCoordinator.play(favorite, restart = restart)
    }

    LaunchedEffect(selectedFavorite?.audioUrl, playbackCoordinator.queueMode, isPageActive) {
        if (!isPageActive) return@LaunchedEffect
        playbackCoordinator.prepareSelected()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BaGuideBgmAlbumContent(
            accent = accent,
            tracks = displayedTracks,
            currentTrackId = selectedFavorite?.audioUrl.orEmpty(),
            isPlaying = playbackRuntimeState.isPlaying,
            repeatEnabled = playbackState.queueMode == BaGuideBgmQueueMode.SingleLoop,
            playbackVolume = playbackRuntimeState.volume,
            volumeControlVisible = volumeControlVisible,
            lastAudibleVolume = lastAudibleVolume,
            isTrackFavorite = { id -> favoritesByTrackId.containsKey(id) },
            onRepeatClick = { playbackCoordinator.toggleQueueMode() },
            onPlayPauseClick = {
                val favorite =
                    selectedFavorite ?: displayedFavorites.firstOrNull()
                        ?: return@BaGuideBgmAlbumContent
                playbackCoordinator.toggle(favorite)
            },
            onVolumeChange = { volume ->
                selectedFavorite?.let { favorite ->
                    playbackCoordinator.updateVolume(
                        favorite,
                        volume,
                    )
                }
            },
            onVolumeChangeFinished = { volume ->
                selectedFavorite?.let { favorite ->
                    playbackCoordinator.updateVolume(
                        favorite,
                        volume,
                    )
                }
            },
            onVolumeControlVisibleChange = onVolumeControlVisibleChange,
            onLastAudibleVolumeChange = onLastAudibleVolumeChange,
            onSliderInteractionChanged = onSliderInteractionChanged,
            onTrackClick = { id ->
                favoritesByTrackId[id]?.let { favorite ->
                    playFavorite(favorite, restart = id == playbackState.selectedAudioUrl)
                }
            },
            onTrackFavoriteClick = { id ->
                onRemoveBgmFavorite(id)
                if (playbackState.selectedAudioUrl == id) playbackCoordinator.select("")
            },
            onTrackOfflineClick = { id ->
                favoritesByTrackId[id]?.let(favoriteOfflineCacheState.onToggleFavoriteCache)
            },
            onTrackShareClick = { track ->
                favoritesByTrackId[track.id]?.let { favorite ->
                    onRequestGuideDetailTab(favorite.sourceUrl, GuideBottomTab.Gallery)
                    onOpenGuide(favorite.sourceUrl)
                }
            },
            isTrackOfflineSaved = { id ->
                id in favoriteOfflineCacheState.offlineAudioUrls
            },
            sectionTitle = sectionTitle,
            sectionMeta = sectionMeta,
            sectionFooterTitle = stringResource(R.string.ba_catalog_tab_bgm),
            offlineTrackCount = favoriteOfflineCacheState.offlineAudioUrls.size,
            showFooter = false,
            listState = listState,
            trackListState = trackListState,
            columnCount = columnCount,
            collapseProgress = 0f,
            bottomBarScrollConnection = bottomBarScrollConnection,
            userScrollEnabled = true,
            topPadding = topPadding,
            bottomPadding = bottomPadding,
            contentBackdrop = contentBackdrop,
            artworkImageUrl =
                selectedArtworkImageUrl,
            showAlbumTitle = false,
            promoteSectionTitle = true,
            modifier = Modifier.fillMaxSize(),
        )

        // Anchored to the bottom edge rather than inserted as a list item: the row it replaces has just
        // been removed, so putting the offer in the list would move the content under the finger that
        // removed it.
        AnimatedVisibility(
            visible = pendingUndoFavorite != null,
            enter = appFloatingEnter(),
            exit = appFloatingExit(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = appPageEdgePaddingStart(),
                        end = appPageEdgePaddingEnd(),
                        bottom = bottomPadding,
                    ),
        ) {
            // Held across the exit animation, so the card does not blank out mid-fade when the offer
            // expires or is taken.
            val shown = remember { mutableStateOf(pendingUndoFavorite) }
            if (pendingUndoFavorite != null) shown.value = pendingUndoFavorite
            shown.value?.let { removed ->
                BaGuideBgmUndoBlock(
                    removedFavorite = removed,
                    accent = accent,
                    onUndo = onUndoRemoveBgmFavorite,
                )
            }
        }
    }
}

private data class BaGuideFavoriteBgmPlaybackRuntimeUiState(
    val isPlaying: Boolean,
    val volume: Float,
    val durationMs: Long,
)
