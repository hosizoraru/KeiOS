package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.LiquidActionItem
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmAlbumTopBar(
    accent: Color,
    titleProgress: Float,
    onClose: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    backdrop: Backdrop,
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
            DebugBgmTopBackAction(
                accent = accent,
                onClose = onClose
            )
            DebugBgmTopActionCapsule(
                onShareClick = onShareClick,
                onDownloadClick = onDownloadClick,
                backdrop = backdrop
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = DebugBgmTopBarVisualSize + DebugBgmTopBarTitleGap,
                    end = DebugBgmTopActionBarWidth + DebugBgmTopBarTitleGap
                )
                .fillMaxWidth()
                .height(DebugBgmTopBarVisualSize)
                .graphicsLayer { alpha = titleProgress },
            contentAlignment = Alignment.Center
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
private fun DebugBgmTopBackAction(
    accent: Color,
    onClose: () -> Unit
) {
    val closeContentDescription = stringResource(R.string.common_close)
    DebugBgmGlassIcon(
        icon = appLucideChevronLeftIcon(),
        contentDescription = closeContentDescription,
        accent = accent,
        size = DebugBgmTopBarVisualSize,
        iconSize = DebugBgmTopBarBackIconSize,
        surfaceAlphaScale = DebugBgmTopBarSurfaceAlphaScale,
        highlightAlphaScale = DebugBgmTopBarHighlightAlphaScale,
        lensScale = DebugBgmTopBarLensScale,
        onClick = onClose
    )
}

@Composable
private fun DebugBgmTopActionCapsule(
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    backdrop: Backdrop
) {
    val shareContentDescription = stringResource(R.string.debug_component_lab_action_share)
    val downloadContentDescription = stringResource(R.string.debug_component_lab_action_download)
    val moreContentDescription = stringResource(R.string.debug_component_lab_action_more)
    val shareIcon = appLucideSquareArrowUpIcon()
    val downloadIcon = appLucideDownloadIcon()
    val moreIcon = appLucideMoreIcon()
    val actionItems = remember(
        shareContentDescription,
        downloadContentDescription,
        moreContentDescription,
        shareIcon,
        downloadIcon,
        moreIcon,
        onShareClick,
        onDownloadClick
    ) {
        listOf(
            LiquidActionItem(
                icon = shareIcon,
                contentDescription = shareContentDescription,
                onClick = onShareClick
            ),
            LiquidActionItem(
                icon = downloadIcon,
                contentDescription = downloadContentDescription,
                onClick = onDownloadClick
            ),
            LiquidActionItem(
                icon = moreIcon,
                contentDescription = moreContentDescription,
                onClick = {}
            )
        )
    }
    LiquidActionBar(
        modifier = Modifier
            .height(DebugBgmTopBarVisualSize),
        backdrop = backdrop,
        items = actionItems,
        selectedIndex = actionItems.lastIndex
    )
}

private val DebugBgmTopBarVisualSize = AppChromeTokens.liquidActionBarOuterHeight
private val DebugBgmTopActionBarWidth = AppChromeTokens.liquidActionBarMinWidth
private val DebugBgmTopBarBackIconSize = 27.dp
private val DebugBgmTopBarTitleGap = 12.dp
private const val DebugBgmTopBarSurfaceAlphaScale = 0.62f
private const val DebugBgmTopBarHighlightAlphaScale = 1.18f
private const val DebugBgmTopBarLensScale = 1.12f
