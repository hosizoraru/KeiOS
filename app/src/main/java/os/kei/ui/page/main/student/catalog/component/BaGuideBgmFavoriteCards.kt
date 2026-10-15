@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideUndoIcon
import os.kei.ui.page.main.os.appLucideVolume2Icon
import os.kei.ui.page.main.os.appLucideVolumeOffIcon
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.component.GuideLiquidCard
import os.kei.ui.page.main.student.guideLocalizedLabel
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidMusicProgressSlider
import os.kei.ui.page.main.widget.glass.LiquidVolumeSlider
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun BaGuideBgmPlaybackSeekBar(
    progress: () -> Float,
    enabled: Boolean,
    accent: Color,
    contentDescription: String,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {},
) {
    val sliderBackdrop = LocalLiquidParentBackdrop.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp),
    ) {
        LiquidMusicProgressSlider(
            value = { progress().coerceIn(0f, 1f) },
            onValueChange = { value -> onSeekChanged(value.coerceIn(0f, 1f)) },
            onValueChangeFinished = { onSeekFinished() },
            onInteractionChanged = onInteractionChanged,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = sliderBackdrop,
            enabled = enabled,
            contentDescription = contentDescription,
            activeColor = accent,
            inactiveColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f),
            modifier =
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 4.dp),
        )
    }
}

@Composable
internal fun BaGuideBgmVolumeRow(
    volume: Float,
    contentDescription: String,
    valueText: String,
    onVolumeChanged: (Float) -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {},
) {
    val neutralTint = MiuixTheme.colorScheme.onBackgroundVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
                if (volume <= 0.01f) {
                    appLucideVolumeOffIcon()
                } else {
                    appLucideVolume2Icon()
                },
            contentDescription = null,
            tint = neutralTint,
            modifier = Modifier.size(18.dp),
        )
        BaGuideBgmVolumeSlider(
            volume = volume,
            contentDescription = contentDescription,
            onVolumeChanged = onVolumeChanged,
            onInteractionChanged = onInteractionChanged,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valueText,
            color = neutralTint,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 1,
        )
    }
}

@Composable
private fun BaGuideBgmVolumeSlider(
    volume: Float,
    contentDescription: String,
    onVolumeChanged: (Float) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderBackdrop = LocalLiquidParentBackdrop.current
    val activeColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f)
    Box(
        modifier =
            modifier
                .height(48.dp),
    ) {
        LiquidVolumeSlider(
            value = { volume.coerceIn(0f, 1f) },
            onValueChange = { value -> onVolumeChanged(value.coerceIn(0f, 1f)) },
            onInteractionChanged = onInteractionChanged,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = sliderBackdrop,
            contentDescription = contentDescription,
            activeColor = activeColor,
            inactiveColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
            modifier =
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 3.dp),
        )
    }
}

@Composable
internal fun BaGuideBgmUndoBlock(
    removedFavorite: GuideBgmFavoriteItem,
    accent: Color,
    onUndo: () -> Unit,
) {
    val studentName =
        removedFavorite.studentTitle.ifBlank {
            stringResource(R.string.ba_catalog_bgm_student_unknown)
        }
    val bgmTitle =
        guideLocalizedLabel(
            removedFavorite.title.ifBlank { removedFavorite.note },
            R.string.ba_catalog_bgm_track_fallback,
        )
    val message =
        stringResource(
            R.string.ba_catalog_bgm_removed_message,
            studentName,
            bgmTitle,
        )
    val undoText = stringResource(R.string.ba_catalog_bgm_action_undo)
    GuideLiquidCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        surfaceColor = Color(0x223B82F6),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(CardLayoutRhythm.cardContentPadding),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            AppStandaloneLiquidTextButton(
                text = undoText,
                leadingIcon = appLucideUndoIcon(),
                onClick = onUndo,
                textColor = accent,
                variant = GlassVariant.Compact,
                textMaxLines = 1,
            )
        }
    }
}
