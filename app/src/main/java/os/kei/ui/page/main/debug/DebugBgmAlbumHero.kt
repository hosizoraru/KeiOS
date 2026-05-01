package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.R
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRepeatIcon
import os.kei.ui.page.main.os.appLucideSkipBackIcon
import os.kei.ui.page.main.os.appLucideSkipForwardIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmAlbumHero(
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
    DebugBgmGlassCapsule(
        modifier = Modifier
            .height(52.dp)
            .padding(horizontal = 0.dp),
        accent = accent,
        horizontalPadding = 24.dp,
        verticalPadding = 0.dp,
        selected = isPlaying,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val contentTint = if (isPlaying) {
                accent.copy(alpha = 0.98f)
            } else {
                MiuixTheme.colorScheme.onBackground
            }
            Icon(
                imageVector = if (isPlaying) appLucidePauseIcon() else appLucidePlayIcon(),
                contentDescription = null,
                tint = contentTint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(
                    if (isPlaying) R.string.debug_component_lab_action_pause else R.string.debug_component_lab_action_play
                ),
                color = contentTint,
                fontSize = AppTypographyTokens.CardHeader.fontSize,
                lineHeight = AppTypographyTokens.CardHeader.lineHeight,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
