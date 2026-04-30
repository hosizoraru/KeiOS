package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.component.floatingtabbar.rememberFloatingTabBarScrollConnection
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import os.kei.ui.page.main.os.appLucideShuffleIcon
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
        contentTopPadding = 108.dp,
        contentBottomPadding = 224.dp,
        topBarModifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        bottomBarModifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
    bottomBarModifier: Modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    onClose: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val listState = rememberLazyListState()
    val bottomBarScrollConnection = rememberFloatingTabBarScrollConnection(scrollThreshold = 56.dp)
    val collapseProgress by remember {
        derivedStateOf {
            val indexOffset = listState.firstVisibleItemIndex * 240f
            ((indexOffset + listState.firstVisibleItemScrollOffset) / 260f).coerceIn(0f, 1f)
        }
    }
    var selectedDockKey by remember { mutableStateOf(DebugBgmDockKeys.Library) }
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

    Box(
        modifier = panelModifier
    ) {
        DebugBgmAlbumContent(
            accent = accent,
            listState = listState,
            collapseProgress = collapseProgress,
            bottomBarScrollConnection = bottomBarScrollConnection,
            topPadding = contentTopPadding,
            bottomPadding = contentBottomPadding
        )
        DebugBgmAlbumTopBar(
            accent = accent,
            collapseProgress = collapseProgress,
            onClose = onClose,
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
        DebugBgmFloatingBottomChrome(
            accent = accent,
            scrollConnection = bottomBarScrollConnection,
            selectedDockKey = selectedDockKey,
            onSelectedDockKeyChange = { selectedDockKey = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(bottomBarModifier)
        )
    }
}

@Composable
private fun DebugBgmAlbumContent(
    accent: Color,
    listState: LazyListState,
    collapseProgress: Float,
    bottomBarScrollConnection: NestedScrollConnection,
    topPadding: Dp,
    bottomPadding: Dp
) {
    val tracks = rememberDebugBgmTracks()
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
                collapseProgress = collapseProgress
            )
        }
        item {
            DebugBgmAlbumDescription(accent = accent)
        }
        item {
            DebugBgmTrackList(
                tracks = tracks,
                accent = accent
            )
        }
        item {
            DebugBgmAlbumFooter()
        }
    }
}

@Composable
private fun DebugBgmAlbumTopBar(
    accent: Color,
    collapseProgress: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleAlpha = ((collapseProgress - 0.88f) / 0.12f).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebugBgmGlassIcon(
                icon = appLucideBackIcon(),
                contentDescription = stringResource(R.string.common_close),
                accent = accent,
                size = 46.dp,
                onClick = onClose
            )
            DebugBgmTopActionCapsule(accent = accent)
        }
        DebugBgmGlassCapsule(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 184.dp)
                .graphicsLayer { alpha = titleAlpha },
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
private fun DebugBgmTopActionCapsule(accent: Color) {
    DebugBgmGlassCapsule(
        accent = accent,
        horizontalPadding = 4.dp,
        verticalPadding = 3.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebugBgmInlineIcon(
                icon = appLucideShareIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_share),
                tint = MiuixTheme.colorScheme.onBackground
            )
            DebugBgmInlineIcon(
                icon = appLucideMoreIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_more),
                tint = MiuixTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun DebugBgmAlbumHero(
    accent: Color,
    collapseProgress: Float
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
                text = stringResource(R.string.debug_component_lab_album_artist),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.SectionTitle.fontSize,
                lineHeight = AppTypographyTokens.SectionTitle.lineHeight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.debug_component_lab_album_meta),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DebugBgmAlbumPrimaryActions(accent = accent)
    }
}

@Composable
private fun DebugBgmAlbumArtwork(accent: Color) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.74f)
            .height(206.dp)
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
private fun DebugBgmAlbumPrimaryActions(accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DebugBgmRoundAction(
            icon = appLucideShuffleIcon(),
            contentDescription = stringResource(R.string.debug_component_lab_action_shuffle),
            accent = accent
        )
        DebugBgmPlayAction(accent = accent)
        DebugBgmRoundAction(
            icon = appLucideDownloadIcon(),
            contentDescription = stringResource(R.string.debug_component_lab_action_download),
            accent = accent
        )
    }
}

@Composable
private fun DebugBgmRoundAction(
    icon: ImageVector,
    contentDescription: String,
    accent: Color
) {
    DebugBgmGlassIcon(
        icon = icon,
        contentDescription = contentDescription,
        accent = accent,
        size = 52.dp,
        iconSize = 24.dp
    )
}

@Composable
private fun DebugBgmPlayAction(accent: Color) {
    Row(
        modifier = Modifier
            .height(52.dp)
            .clip(ContinuousCapsule)
            .background(Color.Black)
            .clickable(onClick = {})
            .padding(horizontal = 34.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = appLucidePlayIcon(),
            contentDescription = null,
            tint = accent.copy(alpha = 0.96f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.debug_component_lab_action_play),
            color = Color.White,
            fontSize = AppTypographyTokens.CardHeader.fontSize,
            lineHeight = AppTypographyTokens.CardHeader.lineHeight,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun DebugBgmAlbumDescription(accent: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.debug_component_lab_album_description),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.18f))
        )
    }
}

@Composable
private fun DebugBgmTrackList(
    tracks: List<DebugBgmTrack>,
    accent: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        tracks.forEachIndexed { index, track ->
            DebugBgmTrackRow(
                index = index,
                track = track,
                accent = accent
            )
            if (index < tracks.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp)
                        .height(1.dp)
                        .background(MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f))
                )
            }
        }
    }
}

@Composable
private fun DebugBgmTrackRow(
    index: Int,
    track: DebugBgmTrack,
    accent: Color
) {
    val playing = index == 2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { contentDescription = track.title }
            .clickable(onClick = {}),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (playing) "•" else (index + 1).toString(),
            color = if (playing) accent else MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.widthIn(min = 28.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.title,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.subtitle,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DebugBgmInlineIcon(
            icon = appLucideMoreIcon(),
            contentDescription = stringResource(R.string.debug_component_lab_action_more),
            tint = MiuixTheme.colorScheme.onBackgroundVariant
        )
    }
}

@Composable
private fun DebugBgmAlbumFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.debug_component_lab_album_footer_date),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
        Text(
            text = stringResource(R.string.debug_component_lab_album_footer_count),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
    }
}

@Composable
private fun rememberDebugBgmTracks(): List<DebugBgmTrack> {
    val track1Title = stringResource(R.string.debug_component_lab_track_1_title)
    val track1Subtitle = stringResource(R.string.debug_component_lab_track_1_subtitle)
    val track2Title = stringResource(R.string.debug_component_lab_track_2_title)
    val track2Subtitle = stringResource(R.string.debug_component_lab_track_2_subtitle)
    val track3Title = stringResource(R.string.debug_component_lab_track_3_title)
    val track3Subtitle = stringResource(R.string.debug_component_lab_track_3_subtitle)
    val track4Title = stringResource(R.string.debug_component_lab_track_4_title)
    val track4Subtitle = stringResource(R.string.debug_component_lab_track_4_subtitle)
    return remember(
        track1Title,
        track1Subtitle,
        track2Title,
        track2Subtitle,
        track3Title,
        track3Subtitle,
        track4Title,
        track4Subtitle
    ) {
        listOf(
            DebugBgmTrack(track1Title, track1Subtitle),
            DebugBgmTrack(track2Title, track2Subtitle),
            DebugBgmTrack(track3Title, track3Subtitle),
            DebugBgmTrack(track4Title, track4Subtitle)
        )
    }
}

private data class DebugBgmTrack(
    val title: String,
    val subtitle: String
)
