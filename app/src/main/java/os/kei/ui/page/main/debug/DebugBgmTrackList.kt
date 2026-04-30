package os.kei.ui.page.main.debug

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidDropdownColumn
import os.kei.ui.page.main.widget.glass.LiquidDropdownItem
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.page.main.widget.sheet.capturePopupAnchor
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmTrackList(
    tracks: List<DebugBgmTrack>,
    currentTrackId: String,
    isPlaying: Boolean,
    accent: Color,
    isTrackFavorite: (String) -> Boolean,
    onTrackClick: (String) -> Unit,
    onTrackFavoriteClick: (String) -> Unit,
) {
    if (tracks.isEmpty()) {
        DebugBgmEmptyTrackResult(accent = accent)
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        tracks.forEachIndexed { index, track ->
            val active = track.id == currentTrackId
            DebugBgmTrackRow(
                index = index,
                track = track,
                active = active,
                isPlaying = isPlaying,
                favorite = isTrackFavorite(track.id),
                accent = accent,
                onClick = { onTrackClick(track.id) },
                onFavoriteClick = { onTrackFavoriteClick(track.id) }
            )
            if (index < tracks.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 30.dp, end = 32.dp)
                        .height(1.dp)
                        .background(MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f))
                )
            }
        }
    }
}

@Composable
private fun DebugBgmEmptyTrackResult(accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = appLucideMusicIcon(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = stringResource(R.string.debug_component_lab_search_empty_title),
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.debug_component_lab_search_empty_subtitle),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DebugBgmTrackRow(
    index: Int,
    track: DebugBgmTrack,
    active: Boolean,
    isPlaying: Boolean,
    favorite: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    var moreExpanded by remember(track.id) { mutableStateOf(false) }
    var moreAnchorBounds by remember(track.id) { mutableStateOf<IntRect?>(null) }
    val rowShape = RoundedCornerShape(14.dp)
    val statusText = if (active) {
        stringResource(
            if (isPlaying) {
                R.string.debug_component_lab_track_status_now_playing
            } else {
                R.string.debug_component_lab_track_status_paused
            }
        )
    } else {
        track.subtitle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = "${track.title}, $statusText" }
            .clip(rowShape)
            .background(
                color = if (active) accent.copy(alpha = 0.08f) else Color.Transparent,
                shape = rowShape
            )
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DebugBgmTrackIndex(
            index = index,
            active = active,
            isPlaying = isPlaying,
            accent = accent
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                color = if (active) accent else MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = track.durationLabel,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 1,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End
        )
        Box(
            modifier = Modifier.capturePopupAnchor { moreAnchorBounds = it },
            contentAlignment = Alignment.Center
        ) {
            DebugBgmInlineIcon(
                icon = appLucideMoreIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_more),
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
                size = 40.dp,
                iconSize = 22.dp,
                onClick = { moreExpanded = true }
            )
            DebugBgmTrackMorePopup(
                show = moreExpanded,
                anchorBounds = moreAnchorBounds,
                favorite = favorite,
                onDismissRequest = { moreExpanded = false },
                onPlayClick = {
                    moreExpanded = false
                    onClick()
                },
                onFavoriteClick = {
                    moreExpanded = false
                    onFavoriteClick()
                }
            )
        }
    }
}

@Composable
private fun DebugBgmTrackMorePopup(
    show: Boolean,
    anchorBounds: IntRect?,
    favorite: Boolean,
    onDismissRequest: () -> Unit,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    if (!show) return
    SnapshotWindowListPopup(
        show = true,
        alignment = PopupPositionProvider.Align.BottomEnd,
        anchorBounds = anchorBounds?.asTrackMenuAnchor(),
        placement = SnapshotPopupPlacement.ButtonEnd,
        enableWindowDim = false,
        onDismissRequest = onDismissRequest
    ) {
        LiquidDropdownColumn {
            DebugBgmTrackMenuItem(
                text = stringResource(R.string.debug_component_lab_action_play),
                index = 0,
                optionSize = DebugBgmTrackMenuItemCount,
                onClick = onPlayClick
            )
            DebugBgmTrackMenuItem(
                text = stringResource(R.string.debug_component_lab_action_favorite),
                selected = favorite,
                index = 1,
                optionSize = DebugBgmTrackMenuItemCount,
                onClick = onFavoriteClick
            )
            DebugBgmTrackMenuItem(
                text = stringResource(R.string.debug_component_lab_action_share),
                index = 2,
                optionSize = DebugBgmTrackMenuItemCount,
                onClick = onDismissRequest
            )
        }
    }
}

@Composable
private fun DebugBgmTrackMenuItem(
    text: String,
    index: Int,
    optionSize: Int,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    LiquidDropdownItem(
        text = text,
        selected = selected,
        onClick = onClick,
        index = index,
        optionSize = optionSize
    )
}

private fun IntRect.asTrackMenuAnchor(): IntRect {
    return IntRect(
        left = left,
        top = top,
        right = right,
        bottom = Int.MAX_VALUE / 4
    )
}

@Composable
private fun DebugBgmTrackIndex(
    index: Int,
    active: Boolean,
    isPlaying: Boolean,
    accent: Color
) {
    Box(
        modifier = Modifier.width(22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            DebugBgmPlayingBars(
                accent = accent,
                animated = isPlaying
            )
        } else {
            Text(
                text = (index + 1).toString(),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DebugBgmPlayingBars(
    accent: Color,
    animated: Boolean
) {
    val transition = rememberInfiniteTransition(label = "debug_bgm_playing_bars")
    val firstHeight by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "debug_bgm_playing_bar_first"
    )
    val secondHeight by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 640),
            repeatMode = RepeatMode.Reverse
        ),
        label = "debug_bgm_playing_bar_second"
    )
    val thirdHeight by transition.animateFloat(
        initialValue = 0.56f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 580),
            repeatMode = RepeatMode.Reverse
        ),
        label = "debug_bgm_playing_bar_third"
    )

    Row(
        modifier = Modifier
            .width(18.dp)
            .height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        DebugBgmPlayingBar(accent = accent, heightFraction = if (animated) firstHeight else 0.56f)
        DebugBgmPlayingBar(accent = accent, heightFraction = if (animated) secondHeight else 0.56f)
        DebugBgmPlayingBar(accent = accent, heightFraction = if (animated) thirdHeight else 0.56f)
    }
}

@Composable
private fun DebugBgmPlayingBar(
    accent: Color,
    heightFraction: Float
) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight(heightFraction.coerceIn(0.32f, 1f))
            .clip(ContinuousCapsule)
            .background(accent)
    )
}

@Composable
internal fun DebugBgmSearchPanel(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    GlassSearchField(
        value = query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.debug_component_lab_search_placeholder),
        backdrop = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        textColor = MiuixTheme.colorScheme.onBackground,
        variant = GlassVariant.SheetInput,
        horizontalPadding = 18.dp,
        verticalPadding = 0.dp,
        focusRequester = focusRequester
    )
}

private const val DebugBgmTrackMenuItemCount = 3
