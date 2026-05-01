package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideChevronLeftIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideSquareArrowUpIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmAlbumTopBar(
    accent: Color,
    titleProgress: Float,
    onClose: () -> Unit,
    onShareClick: () -> Unit,
    offlineSaved: Boolean,
    onDownloadClick: () -> Unit,
    backdrop: Backdrop? = null,
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
                backdrop = backdrop,
                onClick = onClose
            )
            DebugBgmTopActionCapsule(
                accent = accent,
                offlineSaved = offlineSaved,
                onShareClick = onShareClick,
                onDownloadClick = onDownloadClick,
                backdrop = backdrop
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
            verticalPadding = 8.dp,
            backdrop = backdrop
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
    onDownloadClick: () -> Unit,
    backdrop: Backdrop? = null
) {
    DebugBgmGlassCapsule(
        accent = accent,
        modifier = Modifier
            .width(AppChromeTokens.liquidActionBarMinWidth)
            .height(AppChromeTokens.liquidActionBarOuterHeight),
        horizontalPadding = AppChromeTokens.liquidActionBarHorizontalPadding,
        verticalPadding = AppChromeTokens.liquidActionBarHorizontalPadding,
        backdrop = backdrop
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

private val DebugBgmTopBarBackIconSize = 28.dp
private val DebugBgmTopBarTitleGap = 12.dp
private val DebugBgmTopBarActionSlotSize = AppChromeTokens.liquidActionBarItemStep
private val DebugBgmTopBarActionIconSize = 24.dp
