package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideUndoIcon
import os.kei.ui.page.main.os.appLucideVolume2Icon
import os.kei.ui.page.main.os.appLucideVolumeOffIcon
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.component.GuideLiquidCard
import os.kei.ui.page.main.student.guideLocalizedLabel
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownActionItem
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownColumn
import os.kei.ui.page.main.widget.glass.LiquidMusicProgressSlider
import os.kei.ui.page.main.widget.glass.LiquidVolumeSlider
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.page.main.widget.sheet.capturePopupAnchor
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideBgmPlaybackSeekBar(
    progress: Float,
    enabled: Boolean,
    accent: Color,
    contentDescription: String,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {}
) {
    val sliderBackdrop = rememberLayerBackdrop()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(sliderBackdrop)
        )
        LiquidMusicProgressSlider(
            value = { progress.coerceIn(0f, 1f) },
            onValueChange = { value -> onSeekChanged(value.coerceIn(0f, 1f)) },
            onValueChangeFinished = { onSeekFinished() },
            onInteractionChanged = onInteractionChanged,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = sliderBackdrop,
            enabled = enabled,
            activeColor = accent,
            inactiveColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f),
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun BaGuideBgmVolumeRow(
    volume: Float,
    contentDescription: String,
    valueText: String,
    onVolumeChanged: (Float) -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {}
) {
    val neutralTint = MiuixTheme.colorScheme.onBackgroundVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (volume <= 0.01f) {
                appLucideVolumeOffIcon()
            } else {
                appLucideVolume2Icon()
            },
            contentDescription = null,
            tint = neutralTint,
            modifier = Modifier.size(18.dp)
        )
        BaGuideBgmVolumeSlider(
            volume = volume,
            contentDescription = contentDescription,
            onVolumeChanged = onVolumeChanged,
            onInteractionChanged = onInteractionChanged,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            color = neutralTint,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 1
        )
    }
}

@Composable
private fun BaGuideBgmVolumeSlider(
    volume: Float,
    contentDescription: String,
    onVolumeChanged: (Float) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderBackdrop = rememberLayerBackdrop()
    val activeColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f)
    Box(
        modifier = modifier
            .height(44.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(sliderBackdrop)
        )
        LiquidVolumeSlider(
            value = { volume.coerceIn(0f, 1f) },
            onValueChange = { value -> onVolumeChanged(value.coerceIn(0f, 1f)) },
            onInteractionChanged = onInteractionChanged,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = sliderBackdrop,
            activeColor = activeColor,
            inactiveColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 3.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun BaGuideBgmFavoriteCard(
    favorite: GuideBgmFavoriteItem,
    selected: Boolean,
    cached: Boolean,
    caching: Boolean,
    accent: Color,
    onOpenGuide: () -> Unit,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onCache: () -> Unit,
    onRemove: () -> Unit
) {
    val unknownStudent = stringResource(R.string.ba_catalog_bgm_student_unknown)
    val studentTitle = favorite.studentTitle.ifBlank { unknownStudent }
    val cacheReadyLabel = stringResource(R.string.ba_catalog_bgm_cache_ready)
    val cacheContentDescription = stringResource(
        if (caching) R.string.ba_catalog_bgm_action_cache_busy else R.string.ba_catalog_bgm_action_cache
    )
    val deleteContentDescription = stringResource(R.string.ba_catalog_bgm_action_delete)
    val playContentDescription = stringResource(R.string.ba_catalog_bgm_action_play)
    val openGalleryContentDescription = stringResource(R.string.ba_catalog_bgm_action_open_gallery)
    val moreContentDescription = stringResource(R.string.ba_catalog_bgm_action_more)
    val currentLabel = stringResource(R.string.ba_catalog_bgm_status_current)
    val detailTitle = favorite.title.takeIf { isMeaningfulBgmFavoriteDetail(it, studentTitle) }
    val detailNote = favorite.note.takeIf { note ->
        isMeaningfulBgmFavoriteDetail(note, studentTitle) &&
            (detailTitle?.let { !sameBgmFavoriteDetail(note, it) } ?: true)
    }
    val trackSubtitle = (detailTitle ?: detailNote)?.let { guideLocalizedLabel(it) }
    val cardShape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) accent.copy(alpha = 0.38f) else MiuixTheme.colorScheme.outline.copy(alpha = 0.16f)
    val listActionTint = MiuixTheme.colorScheme.onBackgroundVariant
    val listActionContainer = MiuixTheme.colorScheme.surfaceContainer
    var actionExpanded by remember { mutableStateOf(false) }
    var actionAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    GuideLiquidCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = cardShape
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
                onLongClick = onRemove
        ),
        cornerRadius = 14.dp,
        surfaceColor = if (selected) accent.copy(alpha = 0.11f) else MiuixTheme.colorScheme.surface.copy(alpha = 0.58f),
        isInteractive = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                BaGuideCatalogEntryAvatar(
                    imageUrl = favorite.studentImageUrl.ifBlank { favorite.imageUrl },
                    fallbackRes = R.drawable.ba_tab_bgm,
                    size = 48.dp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = studentTitle,
                        modifier = Modifier.weight(1f),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.CompactTitle.fontSize,
                        lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                        fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selected) {
                        StatusPill(
                            label = currentLabel,
                            color = accent,
                            size = AppStatusPillSize.Compact
                        )
                    }
                    if (cached) {
                        StatusPill(
                            label = cacheReadyLabel,
                            color = Color(0xFF22C55E),
                            size = AppStatusPillSize.Compact
                        )
                    }
                }
                trackSubtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            GlassIconButton(
                backdrop = null,
                icon = appLucidePlayIcon(),
                contentDescription = playContentDescription,
                onClick = onPlay,
                width = 38.dp,
                height = 38.dp,
                variant = GlassVariant.Compact,
                iconTint = listActionTint,
                containerColor = listActionContainer
            )
            Box(
                modifier = Modifier.capturePopupAnchor { actionAnchorBounds = it },
                contentAlignment = Alignment.Center
            ) {
                GlassIconButton(
                    backdrop = null,
                    icon = appLucideMoreIcon(),
                    contentDescription = moreContentDescription,
                    onClick = { actionExpanded = true },
                    width = 32.dp,
                    height = 32.dp,
                    variant = GlassVariant.Compact,
                    iconTint = listActionTint,
                    containerColor = listActionContainer
                )
                if (actionExpanded) {
                    SnapshotWindowListPopup(
                        show = true,
                        alignment = PopupPositionProvider.Align.BottomEnd,
                        anchorBounds = actionAnchorBounds,
                        placement = SnapshotPopupPlacement.ButtonEnd,
                        enableWindowDim = false,
                        onDismissRequest = { actionExpanded = false }
                    ) {
                        LiquidGlassDropdownColumn {
                            BaGuideBgmFavoriteActionItem(
                                text = openGalleryContentDescription,
                                index = 0,
                                optionSize = 3,
                                onClick = {
                                    actionExpanded = false
                                    onOpenGuide()
                                }
                            )
                            BaGuideBgmFavoriteActionItem(
                                text = cacheContentDescription,
                                index = 1,
                                optionSize = 3,
                                onClick = {
                                    actionExpanded = false
                                    onCache()
                                }
                            )
                            BaGuideBgmFavoriteActionItem(
                                text = deleteContentDescription,
                                index = 2,
                                optionSize = 3,
                                variant = GlassVariant.SheetDangerAction,
                                onClick = {
                                    actionExpanded = false
                                    onRemove()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BaGuideBgmFavoriteActionItem(
    text: String,
    index: Int,
    optionSize: Int,
    onClick: () -> Unit,
    variant: GlassVariant = GlassVariant.SheetAction
) {
    LiquidGlassDropdownActionItem(
        text = text,
        onClick = onClick,
        index = index,
        optionSize = optionSize,
        variant = variant
    )
}

@Composable
internal fun BaGuideBgmUndoBlock(
    removedFavorite: GuideBgmFavoriteItem,
    accent: Color,
    onUndo: () -> Unit
) {
    val studentName = removedFavorite.studentTitle.ifBlank {
        stringResource(R.string.ba_catalog_bgm_student_unknown)
    }
    val bgmTitle = guideLocalizedLabel(
        removedFavorite.title.ifBlank { removedFavorite.note },
        R.string.ba_catalog_bgm_track_fallback
    )
    val message = stringResource(
        R.string.ba_catalog_bgm_removed_message,
        studentName,
        bgmTitle
    )
    val undoText = stringResource(R.string.ba_catalog_bgm_action_undo)
    GuideLiquidCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        surfaceColor = Color(0x223B82F6),
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardLayoutRhythm.cardContentPadding),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            GlassTextButton(
                backdrop = null,
                text = undoText,
                leadingIcon = appLucideUndoIcon(),
                onClick = onUndo,
                textColor = accent,
                variant = GlassVariant.Compact,
                textMaxLines = 1
            )
        }
    }
}

private fun isMeaningfulBgmFavoriteDetail(
    raw: String,
    studentTitle: String
): Boolean {
    if (raw.isBlank()) return false
    if (sameBgmFavoriteDetail(raw, studentTitle)) return false
    val compact = raw.bgmFavoriteDetailKey()
    return compact != "bgm" &&
        compact != "回忆大厅" &&
        compact != "回忆大厅bgm"
}

private fun sameBgmFavoriteDetail(
    first: String,
    second: String
): Boolean {
    return first.bgmFavoriteDetailKey() == second.bgmFavoriteDetailKey()
}

private fun String.bgmFavoriteDetailKey(): String {
    return replace(Regex("\\s+"), "")
        .trim()
        .lowercase()
}
