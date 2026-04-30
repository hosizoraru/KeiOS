package os.kei.ui.page.main.debug

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideChevronLeftIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRepeatIcon
import os.kei.ui.page.main.os.appLucideSkipBackIcon
import os.kei.ui.page.main.os.appLucideSkipForwardIcon
import os.kei.ui.page.main.os.appLucideSquareArrowUpIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
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

@Composable
private fun DebugBgmAlbumContent(
    accent: Color,
    tracks: List<DebugBgmTrack>,
    currentTrackId: String,
    isPlaying: Boolean,
    repeatEnabled: Boolean,
    offlineSaved: Boolean,
    isTrackFavorite: (String) -> Boolean,
    onRepeatClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
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
    bottomPadding: Dp
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
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
                offlineSaved = offlineSaved,
                isPlaying = isPlaying,
                sectionTitle = sectionTitle,
                sectionMeta = sectionMeta,
                onRepeatClick = onRepeatClick,
                onDownloadClick = onDownloadClick,
                onPreviousClick = onPreviousClick,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick
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

@Composable
private fun DebugBgmAlbumTopBar(
    accent: Color,
    titleProgress: Float,
    onClose: () -> Unit,
    onShareClick: () -> Unit,
    offlineSaved: Boolean,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppChromeTokens.liquidActionBarOuterHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebugBgmGlassIcon(
                icon = appLucideChevronLeftIcon(),
                contentDescription = stringResource(R.string.common_close),
                accent = accent,
                size = AppChromeTokens.liquidActionBarSingleWidth,
                iconSize = DebugBgmTopBarBackIconSize,
                onClick = onClose
            )
            DebugBgmTopActionCapsule(
                accent = accent,
                offlineSaved = offlineSaved,
                onShareClick = onShareClick,
                onDownloadClick = onDownloadClick
            )
        }
        DebugBgmGlassCapsule(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = AppChromeTokens.liquidActionBarSingleWidth + DebugBgmTopBarTitleGap,
                    end = AppChromeTokens.liquidActionBarMinWidth + DebugBgmTopBarTitleGap
                )
                .fillMaxWidth()
                .graphicsLayer { alpha = titleProgress },
            accent = accent,
            horizontalPadding = 16.dp,
            verticalPadding = 8.dp
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_album_title),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DebugBgmTopActionCapsule(
    accent: Color,
    offlineSaved: Boolean,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    DebugBgmGlassCapsule(
        accent = accent,
        modifier = Modifier
            .width(AppChromeTokens.liquidActionBarMinWidth)
            .height(AppChromeTokens.liquidActionBarOuterHeight),
        horizontalPadding = AppChromeTokens.liquidActionBarHorizontalPadding,
        verticalPadding = AppChromeTokens.liquidActionBarHorizontalPadding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebugBgmInlineIcon(
                icon = appLucideSquareArrowUpIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_share),
                tint = MiuixTheme.colorScheme.onBackground,
                size = DebugBgmTopBarActionSlotSize,
                iconSize = DebugBgmTopBarActionIconSize,
                onClick = onShareClick
            )
            DebugBgmInlineIcon(
                icon = appLucideDownloadIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_download),
                tint = if (offlineSaved) accent else MiuixTheme.colorScheme.onBackground,
                size = DebugBgmTopBarActionSlotSize,
                iconSize = DebugBgmTopBarActionIconSize,
                onClick = onDownloadClick
            )
            DebugBgmInlineIcon(
                icon = appLucideMoreIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_more),
                tint = MiuixTheme.colorScheme.onBackground,
                size = DebugBgmTopBarActionSlotSize,
                iconSize = DebugBgmTopBarActionIconSize
            )
        }
    }
}

@Composable
private fun DebugBgmAlbumHero(
    accent: Color,
    collapseProgress: Float,
    repeatEnabled: Boolean,
    offlineSaved: Boolean,
    isPlaying: Boolean,
    sectionTitle: String,
    sectionMeta: String,
    onRepeatClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = 1f - collapseProgress * 0.04f
                scaleY = 1f - collapseProgress * 0.04f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DebugBgmAlbumArtwork(accent = accent)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_album_title),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = 25.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sectionTitle,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.SectionTitle.fontSize,
                lineHeight = AppTypographyTokens.SectionTitle.lineHeight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sectionMeta,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DebugBgmAlbumPrimaryActions(
            accent = accent,
            repeatEnabled = repeatEnabled,
            offlineSaved = offlineSaved,
            isPlaying = isPlaying,
            onRepeatClick = onRepeatClick,
            onDownloadClick = onDownloadClick,
            onPreviousClick = onPreviousClick,
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick
        )
    }
}

@Composable
private fun DebugBgmAlbumArtwork(accent: Color) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(1f)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF4D6D),
                        Color(0xFFFFC857),
                        Color(0xFF35C2FF),
                        accent,
                        Color(0xFF2ED573)
                    )
                )
            )
            .border(8.dp, Color.White.copy(alpha = 0.78f), shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = appLucideMusicIcon(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun DebugBgmAlbumPrimaryActions(
    accent: Color,
    repeatEnabled: Boolean,
    offlineSaved: Boolean,
    isPlaying: Boolean,
    onRepeatClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DebugBgmRoundAction(
            icon = appLucideRepeatIcon(),
            contentDescription = stringResource(R.string.debug_component_lab_action_repeat),
            accent = accent,
            selected = repeatEnabled,
            onClick = onRepeatClick
        )
        DebugBgmRoundAction(
            icon = appLucideSkipBackIcon(),
            contentDescription = stringResource(R.string.debug_component_lab_action_previous),
            accent = accent,
            onClick = onPreviousClick
        )
        DebugBgmPlayAction(
            accent = accent,
            isPlaying = isPlaying,
            onClick = onPlayPauseClick
        )
        DebugBgmRoundAction(
            icon = appLucideSkipForwardIcon(),
            contentDescription = stringResource(R.string.debug_component_lab_action_next),
            accent = accent,
            onClick = onNextClick
        )
        DebugBgmRoundAction(
            icon = appLucideDownloadIcon(),
            contentDescription = stringResource(R.string.debug_component_lab_action_download),
            accent = accent,
            selected = offlineSaved,
            onClick = onDownloadClick
        )
    }
}

@Composable
private fun DebugBgmRoundAction(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    DebugBgmGlassIcon(
        icon = icon,
        contentDescription = contentDescription,
        accent = accent,
        size = 52.dp,
        iconSize = 24.dp,
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun DebugBgmPlayAction(
    accent: Color,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(52.dp)
            .clip(ContinuousCapsule)
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), ContinuousCapsule)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPlaying) appLucidePauseIcon() else appLucidePlayIcon(),
            contentDescription = null,
            tint = accent.copy(alpha = 0.96f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(
                if (isPlaying) R.string.debug_component_lab_action_pause else R.string.debug_component_lab_action_play
            ),
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.CardHeader.fontSize,
            lineHeight = AppTypographyTokens.CardHeader.lineHeight,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun DebugBgmAlbumFooter(
    sectionTitle: String,
    trackCount: Int,
    offlineTrackCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp, end = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.debug_component_lab_album_footer_date),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
        Text(
            text = stringResource(R.string.debug_component_lab_section_footer_current, sectionTitle),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
        Text(
            text = stringResource(
                R.string.debug_component_lab_section_footer_state,
                trackCount,
                offlineTrackCount
            ),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
    }
}

@Composable
private fun rememberDebugBgmDockSectionText(selectedDockKey: String): DebugBgmDockSectionText {
    val libraryLabel = stringResource(R.string.debug_component_lab_nav_library)
    return when (selectedDockKey) {
        DebugBgmDockKeys.Home -> DebugBgmDockSectionText(
            heroTitle = stringResource(R.string.debug_component_lab_section_home_title),
            heroMeta = stringResource(R.string.debug_component_lab_section_home_meta),
            footerTitle = stringResource(R.string.debug_component_lab_nav_home)
        )
        DebugBgmDockKeys.Discover -> DebugBgmDockSectionText(
            heroTitle = stringResource(R.string.debug_component_lab_section_discover_title),
            heroMeta = stringResource(R.string.debug_component_lab_section_discover_meta),
            footerTitle = stringResource(R.string.debug_component_lab_nav_discover)
        )
        DebugBgmDockKeys.Radio -> DebugBgmDockSectionText(
            heroTitle = stringResource(R.string.debug_component_lab_section_radio_title),
            heroMeta = stringResource(R.string.debug_component_lab_section_radio_meta),
            footerTitle = stringResource(R.string.debug_component_lab_nav_radio)
        )
        else -> DebugBgmDockSectionText(
            heroTitle = stringResource(R.string.debug_component_lab_album_artist),
            heroMeta = stringResource(R.string.debug_component_lab_album_meta),
            footerTitle = libraryLabel
        )
    }
}

private fun Context.launchDebugBgmTrackShare(
    chooserTitle: String,
    shareText: String
) {
    val shareIntent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, shareText)
    startActivity(Intent.createChooser(shareIntent, chooserTitle))
}

private data class DebugBgmDockSectionText(
    val heroTitle: String,
    val heroMeta: String,
    val footerTitle: String
)

private val DebugBgmTopBarBackIconSize = 28.dp
private val DebugBgmTopBarTitleGap = 12.dp
private val DebugBgmTopBarActionSlotSize = AppChromeTokens.liquidActionBarItemStep
private val DebugBgmTopBarActionIconSize = 24.dp
private val DebugBgmSearchPanelHorizontalPadding = 34.dp
private val DebugBgmSearchPanelCompactBottomPadding = 108.dp
private val DebugBgmSearchPanelExpandedBottomPadding = 168.dp
