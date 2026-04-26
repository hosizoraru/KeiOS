package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideRepeatIcon
import os.kei.ui.page.main.os.appLucideRepeatOneIcon
import os.kei.ui.page.main.os.appLucideSkipBackIcon
import os.kei.ui.page.main.os.appLucideSkipForwardIcon
import os.kei.ui.page.main.os.appLucideTrashIcon
import os.kei.ui.page.main.os.appLucideUndoIcon
import os.kei.ui.page.main.student.BaGuideGalleryItem
import os.kei.ui.page.main.student.GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.section.GuideGalleryCardItem
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideBgmQueueCard(
    favorite: GuideBgmFavoriteItem,
    queueIndex: Int,
    queueSize: Int,
    queueMode: BaGuideBgmQueueMode,
    cached: Boolean,
    accent: Color,
    audioAutoPlayRequestToken: Int,
    mediaUrlResolver: (String) -> String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleQueueMode: () -> Unit,
    onPlaybackEnded: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val modeText = stringResource(queueMode.labelRes)
    val previousContentDescription = stringResource(R.string.ba_catalog_bgm_action_previous)
    val nextContentDescription = stringResource(R.string.ba_catalog_bgm_action_next)
    val openGalleryText = stringResource(R.string.ba_catalog_bgm_action_open_gallery)
    val queueTitle = stringResource(R.string.ba_catalog_bgm_queue_title)
    val positionText = stringResource(
        R.string.ba_catalog_bgm_queue_position,
        queueIndex + 1,
        queueSize
    )
    val cacheReadyLabel = stringResource(R.string.ba_catalog_bgm_cache_ready)
    val cardShape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.42f),
                shape = cardShape
            ),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = accent.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardLayoutRhythm.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = queueTitle,
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = AppTypographyTokens.CompactTitle.fontSize,
                            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatusPill(
                            label = positionText,
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
                GlassIconButton(
                    backdrop = null,
                    icon = appLucideSkipBackIcon(),
                    contentDescription = previousContentDescription,
                    onClick = onPrevious,
                    width = 34.dp,
                    height = 34.dp,
                    variant = GlassVariant.Compact,
                    iconTint = accent,
                    containerColor = accent
                )
                GlassIconButton(
                    backdrop = null,
                    icon = appLucideSkipForwardIcon(),
                    contentDescription = nextContentDescription,
                    onClick = onNext,
                    width = 34.dp,
                    height = 34.dp,
                    variant = GlassVariant.Compact,
                    iconTint = accent,
                    containerColor = accent
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTextButton(
                    backdrop = null,
                    text = modeText,
                    leadingIcon = if (queueMode == BaGuideBgmQueueMode.SingleLoop) {
                        appLucideRepeatOneIcon()
                    } else {
                        appLucideRepeatIcon()
                    },
                    onClick = onToggleQueueMode,
                    textColor = if (queueMode == BaGuideBgmQueueMode.SingleLoop) Color(0xFF22C55E) else accent,
                    variant = GlassVariant.Compact,
                    textMaxLines = 1,
                    textOverflow = TextOverflow.Ellipsis
                )
                GlassTextButton(
                    backdrop = null,
                    text = openGalleryText,
                    leadingIcon = appLucideExternalLinkIcon(),
                    onClick = onOpenGuide,
                    textColor = accent,
                    variant = GlassVariant.Compact,
                    horizontalPadding = 10.dp,
                    textMaxLines = 1
                )
            }
            GuideGalleryCardItem(
                item = favorite.toGalleryItem(),
                backdrop = null,
                onOpenMedia = {},
                onSaveMedia = { _, _ -> },
                audioLoopScopeKey = GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY,
                mediaUrlResolver = mediaUrlResolver,
                embedded = true,
                showMediaTypeLabel = false,
                showSaveAction = false,
                showBgmFavoriteAction = false,
                showAudioLoopAction = false,
                audioLoopEnabledOverride = queueMode == BaGuideBgmQueueMode.SingleLoop,
                audioAutoPlayRequestToken = audioAutoPlayRequestToken,
                onAudioPlaybackEnded = onPlaybackEnded
            )
        }
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
    val detailTitle = favorite.title.takeIf { isMeaningfulBgmFavoriteDetail(it, studentTitle) }
    val detailNote = favorite.note.takeIf { note ->
        isMeaningfulBgmFavoriteDetail(note, studentTitle) &&
            (detailTitle?.let { !sameBgmFavoriteDetail(note, it) } ?: true)
    }
    val hasDetails = detailTitle != null || detailNote != null
    val cardShape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) accent.copy(alpha = 0.42f) else MiuixTheme.colorScheme.outline.copy(alpha = 0.18f)
    Card(
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
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = if (selected) accent.copy(alpha = 0.12f) else Color(0x223B82F6)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardLayoutRhythm.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BaGuideCatalogEntryAvatar(
                        imageUrl = favorite.studentImageUrl.ifBlank { favorite.imageUrl },
                        fallbackRes = R.drawable.ba_tab_bgm
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = studentTitle,
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.CompactTitle.fontSize,
                        lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                        fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cached) {
                            StatusPill(
                                label = cacheReadyLabel,
                                color = Color(0xFF22C55E),
                                size = AppStatusPillSize.Compact
                            )
                        }
                    }
                }
                GlassIconButton(
                    backdrop = null,
                    icon = appLucidePlayIcon(),
                    contentDescription = playContentDescription,
                    onClick = onPlay,
                    width = 44.dp,
                    height = 44.dp,
                    variant = GlassVariant.Compact,
                    iconTint = accent,
                    containerColor = accent
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasDetails) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        detailTitle?.let { title ->
                            Text(
                                text = title,
                                color = MiuixTheme.colorScheme.onBackground,
                                fontSize = AppTypographyTokens.CardHeader.fontSize,
                                lineHeight = AppTypographyTokens.CardHeader.lineHeight,
                                fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        detailNote?.let { note ->
                            Text(
                                text = note,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                fontSize = AppTypographyTokens.Supporting.fontSize,
                                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f))
                }
                GlassIconButton(
                    backdrop = null,
                    icon = appLucideExternalLinkIcon(),
                    contentDescription = openGalleryContentDescription,
                    onClick = onOpenGuide,
                    width = 34.dp,
                    height = 34.dp,
                    variant = GlassVariant.Compact,
                    iconTint = accent,
                    containerColor = accent
                )
                GlassIconButton(
                    backdrop = null,
                    icon = if (caching) appLucideRefreshIcon() else appLucideDownloadIcon(),
                    contentDescription = cacheContentDescription,
                    onClick = onCache,
                    width = 34.dp,
                    height = 34.dp,
                    variant = GlassVariant.Compact,
                    iconTint = if (cached) Color(0xFF22C55E) else accent,
                    containerColor = if (cached) Color(0xFF22C55E) else accent
                )
                GlassIconButton(
                    backdrop = null,
                    icon = appLucideTrashIcon(),
                    contentDescription = deleteContentDescription,
                    onClick = onRemove,
                    width = 34.dp,
                    height = 34.dp,
                    variant = GlassVariant.Compact,
                    iconTint = Color(0xFFEF4444),
                    containerColor = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
internal fun BaGuideBgmUndoBlock(
    removedFavorite: GuideBgmFavoriteItem,
    accent: Color,
    onUndo: () -> Unit
) {
    val message = stringResource(
        R.string.ba_catalog_bgm_removed_message,
        removedFavorite.title
    )
    val undoText = stringResource(R.string.ba_catalog_bgm_action_undo)
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6)
        )
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

private fun GuideBgmFavoriteItem.toGalleryItem(): BaGuideGalleryItem {
    val studentNote = studentTitle
    val displayNote = listOf(studentNote, note)
        .filter { it.isNotBlank() }
        .joinToString(" / ")
    return BaGuideGalleryItem(
        title = title,
        imageUrl = imageUrl.ifBlank { studentImageUrl },
        mediaType = "audio",
        mediaUrl = audioUrl,
        note = displayNote
    )
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
