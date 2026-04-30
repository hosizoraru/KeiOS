package os.kei.ui.page.main.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.R
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmMusicPage(onClose: () -> Unit) {
    DebugBgmLiquidMusicPreview(
        accent = MiuixTheme.colorScheme.primary,
        modifier = Modifier.fillMaxSize(),
        panelShape = RoundedCornerShape(0.dp),
        showBorder = false,
        contentTopPadding = 136.dp,
        contentBottomPadding = 224.dp,
        topBarModifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        bottomBarModifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        onClose = onClose
    )
}

@Composable
internal fun DebugBgmLiquidMusicPreview(
    accent: Color,
    modifier: Modifier = Modifier.height(620.dp),
    panelShape: Shape = RoundedCornerShape(28.dp),
    showBorder: Boolean = true,
    contentTopPadding: Dp = 74.dp,
    contentBottomPadding: Dp = 144.dp,
    topBarModifier: Modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
    bottomBarModifier: Modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
    onClose: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val listState = rememberLazyListState()
    val bottomBarScrollConnection = rememberDebugBgmBottomChromeScrollState(scrollThreshold = 56.dp)
    val musicState = rememberDebugBgmMusicUiState()
    val currentTrack = musicState.currentTrack
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.debug_component_lab_share_chooser_title)
    val sectionText = rememberDebugBgmDockSectionText(selectedDockKey = musicState.selectedDockKey)
    val onShareTrack: (DebugBgmTrack) -> Unit = remember(context, shareChooserTitle) {
        { track ->
            context.launchDebugBgmTrackShare(
                chooserTitle = shareChooserTitle,
                shareText = context.getString(R.string.debug_component_lab_share_text, track.title)
            )
        }
    }
    val albumScrollOffset by remember {
        derivedStateOf {
            val indexOffset = listState.firstVisibleItemIndex * 720f
            indexOffset + listState.firstVisibleItemScrollOffset
        }
    }
    val collapseProgress by remember {
        derivedStateOf {
            (albumScrollOffset / 720f).coerceIn(0f, 1f)
        }
    }
    val topBarTitleProgress by remember {
        derivedStateOf {
            ((albumScrollOffset - 620f) / 180f).coerceIn(0f, 1f)
        }
    }
    val displayedTracks = if (musicState.searchVisible) {
        musicState.visibleTracks
    } else {
        musicState.tracks
    }
    val searchPanelBottomPadding = if (bottomBarScrollConnection.isCompact) {
        DebugBgmSearchPanelCompactBottomPadding
    } else {
        DebugBgmSearchPanelExpandedBottomPadding
    }
    val panelBackground = if (isDark) Color(0xFF10141B) else Color(0xFFDCE9FF)
    val panelBorder = if (isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    var panelModifier = modifier
        .fillMaxWidth()
        .clip(panelShape)
        .background(panelBackground)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = if (isDark) 0.28f else 0.18f),
                    Color(0xFFDCE9FF).copy(alpha = if (isDark) 0.10f else 0.74f),
                    panelBackground
                )
            )
        )
    if (showBorder) {
        panelModifier = panelModifier.border(1.dp, panelBorder, panelShape)
    }

    BackHandler(enabled = musicState.searchVisible) {
        musicState.closeSearch()
    }

    Box(
        modifier = panelModifier
    ) {
        DebugBgmAlbumContent(
            accent = accent,
            tracks = displayedTracks,
            currentTrackId = currentTrack.id,
            isPlaying = musicState.isPlaying,
            repeatEnabled = musicState.repeatEnabled,
            offlineSaved = musicState.currentTrackOfflineSaved,
            isTrackFavorite = musicState::isTrackFavorite,
            onRepeatClick = musicState::toggleRepeat,
            onDownloadClick = musicState::toggleCurrentTrackOffline,
            onPreviousClick = musicState::playPrevious,
            onPlayPauseClick = musicState::togglePlayPause,
            onNextClick = musicState::playNext,
            onTrackClick = musicState::playTrack,
            onTrackFavoriteClick = musicState::toggleTrackFavorite,
            onTrackOfflineClick = musicState::toggleTrackOffline,
            onTrackShareClick = onShareTrack,
            isTrackOfflineSaved = musicState::isTrackOfflineSaved,
            sectionTitle = sectionText.heroTitle,
            sectionMeta = sectionText.heroMeta,
            sectionFooterTitle = sectionText.footerTitle,
            offlineTrackCount = musicState.offlineTrackCount,
            listState = listState,
            collapseProgress = collapseProgress,
            bottomBarScrollConnection = bottomBarScrollConnection,
            topPadding = contentTopPadding,
            bottomPadding = contentBottomPadding
        )
        DebugBgmAlbumTopBar(
            accent = accent,
            titleProgress = topBarTitleProgress,
            onClose = onClose,
            onShareClick = { onShareTrack(currentTrack) },
            offlineSaved = musicState.currentTrackOfflineSaved,
            onDownloadClick = musicState::toggleCurrentTrackOffline,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(topBarModifier)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(196.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            panelBackground.copy(alpha = if (isDark) 0.86f else 0.88f),
                            panelBackground.copy(alpha = if (isDark) 0.96f else 0.98f)
                        )
                    )
                )
        )
        AnimatedVisibility(
            visible = musicState.searchVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = DebugBgmSearchPanelHorizontalPadding,
                    end = DebugBgmSearchPanelHorizontalPadding,
                    bottom = searchPanelBottomPadding
                )
        ) {
            DebugBgmSearchPanel(
                query = musicState.searchQuery,
                onQueryChange = musicState::updateSearchQuery
            )
        }
        DebugBgmFloatingBottomChrome(
            accent = accent,
            scrollState = bottomBarScrollConnection,
            currentTrackTitle = currentTrack.title,
            isPlaying = musicState.isPlaying,
            onPlayPauseClick = musicState::togglePlayPause,
            onPreviousClick = musicState::playPrevious,
            onNextClick = musicState::playNext,
            searchVisible = musicState.searchVisible,
            selectedDockKey = musicState.selectedDockKey,
            onSelectedDockKeyChange = musicState::selectDockKey,
            onSearchClick = musicState::toggleSearch,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(bottomBarModifier)
        )
    }
}

private val DebugBgmSearchPanelHorizontalPadding = 34.dp
private val DebugBgmSearchPanelCompactBottomPadding = 108.dp
private val DebugBgmSearchPanelExpandedBottomPadding = 168.dp
