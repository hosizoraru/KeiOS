package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideChevronLeftIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideSquareArrowUpIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmAlbumTopBar(
    accent: Color,
    titleProgress: Float,
    onClose: () -> Unit,
    onShareClick: () -> Unit,
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
            DebugBgmTopBackAction(
                accent = accent,
                onClose = onClose,
                backdrop = backdrop
            )
            DebugBgmTopActionCapsule(
                accent = accent,
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
private fun DebugBgmTopBackAction(
    accent: Color,
    onClose: () -> Unit,
    backdrop: Backdrop? = null
) {
    val closeContentDescription = stringResource(R.string.common_close)
    DebugBgmGlassIcon(
        icon = appLucideChevronLeftIcon(),
        contentDescription = closeContentDescription,
        accent = accent,
        size = AppChromeTokens.liquidActionBarSingleWidth,
        iconSize = DebugBgmTopBarBackIconSize,
        backdrop = backdrop,
        onClick = onClose
    )
}

@Composable
private fun DebugBgmTopActionCapsule(
    accent: Color,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    backdrop: Backdrop? = null
) {
    val shareContentDescription = stringResource(R.string.debug_component_lab_action_share)
    val downloadContentDescription = stringResource(R.string.debug_component_lab_action_download)
    val moreContentDescription = stringResource(R.string.debug_component_lab_action_more)

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
            DebugBgmTopActionButton(
                icon = appLucideSquareArrowUpIcon(),
                contentDescription = shareContentDescription,
                accent = accent,
                onClick = onShareClick
            )
            DebugBgmTopActionButton(
                icon = appLucideDownloadIcon(),
                contentDescription = downloadContentDescription,
                accent = accent,
                onClick = onDownloadClick
            )
            DebugBgmTopActionButton(
                icon = appLucideMoreIcon(),
                contentDescription = moreContentDescription,
                accent = accent
            )
        }
    }
}

@Composable
private fun DebugBgmTopActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    accent: Color,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by appMotionFloatState(
        targetValue = if (pressed) 1f else 0f,
        durationMillis = DebugBgmTopActionPressMotionMs,
        label = "debug_bgm_top_action_press"
    )
    val baseTint = MiuixTheme.colorScheme.onBackground
    val iconTint = lerp(baseTint, accent, pressProgress * 0.72f)
    Box(
        modifier = Modifier
            .width(DebugBgmTopBarActionSlotSize)
            .height(AppChromeTokens.liquidActionBarInnerHeight)
            .graphicsLayer {
                scaleX = 1f + 0.035f * pressProgress
                scaleY = 1f - 0.045f * pressProgress
            }
            .clip(ContinuousCapsule)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = pressProgress * 0.22f }
                .clip(ContinuousCapsule)
                .background(accent)
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = 1f + 0.035f * pressProgress
                    scaleY = 1f + 0.035f * pressProgress
                }
        )
    }
}

private val DebugBgmTopBarBackIconSize = 28.dp
private val DebugBgmTopBarTitleGap = 12.dp
private val DebugBgmTopBarActionSlotSize = AppChromeTokens.liquidActionBarItemStep
private const val DebugBgmTopActionPressMotionMs = 130
