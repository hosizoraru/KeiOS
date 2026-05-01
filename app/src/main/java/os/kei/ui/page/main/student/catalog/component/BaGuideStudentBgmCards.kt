package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideHeartIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal sealed interface BaGuideStudentBgmLookupState {
    data object Idle : BaGuideStudentBgmLookupState
    data object Loading : BaGuideStudentBgmLookupState
    data object Missing : BaGuideStudentBgmLookupState
    data class Ready(val item: BaGuideStudentBgmResolvedItem) : BaGuideStudentBgmLookupState
}

internal fun BaGuideStudentBgmLookupState.readyFavoriteOrNull() =
    (this as? BaGuideStudentBgmLookupState.Ready)?.item?.favorite

@Composable
internal fun BaGuideStudentBgmHeader(
    totalCount: Int,
    displayedCount: Int,
    resolvedCount: Int,
    favoriteCount: Int,
    loadingCount: Int,
    searchActive: Boolean,
    accent: Color
) {
    AppSurfaceCard(
        containerColor = Color(0x123B82F6),
        borderColor = accent.copy(alpha = 0.16f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ba_catalog_student_bgm_title),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (searchActive) {
                            stringResource(
                                R.string.ba_catalog_student_bgm_summary_search,
                                displayedCount.coerceAtLeast(0),
                                totalCount.coerceAtLeast(0),
                                resolvedCount.coerceAtLeast(0),
                                favoriteCount.coerceAtLeast(0)
                            )
                        } else {
                            stringResource(
                                R.string.ba_catalog_student_bgm_summary,
                                totalCount.coerceAtLeast(0),
                                resolvedCount.coerceAtLeast(0),
                                favoriteCount.coerceAtLeast(0)
                            )
                        },
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(
                    label = if (loadingCount > 0) {
                        stringResource(R.string.ba_catalog_student_bgm_resolving_count, loadingCount)
                    } else {
                        stringResource(R.string.ba_catalog_student_bgm_ready_count, resolvedCount)
                    },
                    color = accent,
                    size = AppStatusPillSize.Compact
                )
            }
            Text(
                text = stringResource(R.string.ba_catalog_student_bgm_hint),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun BaGuideStudentBgmCard(
    entry: BaGuideCatalogEntry,
    lookupState: BaGuideStudentBgmLookupState,
    selected: Boolean,
    playing: Boolean,
    favorite: Boolean,
    accent: Color,
    onOpenGuide: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isLoading = lookupState == BaGuideStudentBgmLookupState.Loading
    val isMissing = lookupState == BaGuideStudentBgmLookupState.Missing
    val ready = lookupState as? BaGuideStudentBgmLookupState.Ready
    val borderColor = when {
        selected -> accent.copy(alpha = 0.38f)
        favorite -> Color(0xFFEC4899).copy(alpha = 0.34f)
        else -> MiuixTheme.colorScheme.outline.copy(alpha = 0.16f)
    }
    val containerColor = when {
        selected -> accent.copy(alpha = 0.11f)
        favorite -> Color(0xFFEC4899).copy(alpha = 0.08f)
        else -> MiuixTheme.colorScheme.surface.copy(alpha = 0.58f)
    }
    val neutralTint = MiuixTheme.colorScheme.onBackgroundVariant
    val neutralContainer = MiuixTheme.colorScheme.surfaceContainer
    AppSurfaceCard(
        containerColor = containerColor,
        borderColor = borderColor,
        onClick = onPlay,
        onLongClick = onOpenGuide
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
                    imageUrl = entry.iconUrl,
                    fallbackRes = R.drawable.ba_tab_student_bgm,
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
                        text = entry.name,
                        modifier = Modifier.weight(1f),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.CompactTitle.fontSize,
                        lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                        fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (favorite) {
                        StatusPill(
                            label = stringResource(R.string.ba_catalog_student_bgm_status_favorite),
                            color = Color(0xFFEC4899),
                            size = AppStatusPillSize.Compact
                        )
                    }
                }
                Text(
                    text = ready?.item?.favorite?.title?.takeIf { it.isNotBlank() }
                        ?: entry.aliasDisplay.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.ba_catalog_student_bgm_card_subtitle),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = when {
                            isLoading -> stringResource(R.string.ba_catalog_student_bgm_status_resolving)
                            isMissing -> stringResource(R.string.ba_catalog_student_bgm_status_missing)
                            ready?.item?.fromFavorite == true -> stringResource(R.string.ba_catalog_student_bgm_status_from_favorite)
                            ready?.item?.fromCache == true -> stringResource(R.string.ba_catalog_student_bgm_status_cached_detail)
                            ready != null -> stringResource(R.string.ba_catalog_student_bgm_status_ready)
                            else -> stringResource(R.string.ba_catalog_student_bgm_status_idle)
                        },
                        color = when {
                            isMissing -> Color(0xFFEF4444)
                            isLoading -> Color(0xFFF59E0B)
                            ready != null -> Color(0xFF22C55E)
                            else -> accent
                        },
                        size = AppStatusPillSize.Compact
                    )
                    StatusPill(
                        label = "ID ${entry.contentId}",
                        color = MiuixTheme.colorScheme.primary,
                        size = AppStatusPillSize.Compact
                    )
                }
            }
            GlassIconButton(
                backdrop = null,
                icon = if (playing) appLucidePauseIcon() else appLucidePlayIcon(),
                contentDescription = stringResource(
                    if (playing) {
                        R.string.ba_catalog_bgm_action_pause
                    } else {
                        R.string.ba_catalog_student_bgm_action_resolve_play
                    }
                ),
                onClick = onPlay,
                width = 38.dp,
                height = 38.dp,
                variant = GlassVariant.Compact,
                iconTint = if (playing || selected) Color.White else neutralTint,
                containerColor = if (playing || selected) accent else neutralContainer,
                enabled = !isLoading
            )
            GlassIconButton(
                backdrop = null,
                icon = appLucideHeartIcon(),
                contentDescription = stringResource(
                    if (favorite) {
                        R.string.guide_bgm_cd_unfavorite
                    } else {
                        R.string.guide_bgm_cd_favorite
                    }
                ),
                onClick = onToggleFavorite,
                width = 34.dp,
                height = 34.dp,
                variant = GlassVariant.Compact,
                iconTint = if (favorite) Color(0xFFEC4899) else neutralTint,
                containerColor = if (favorite) Color(0x33EC4899) else neutralContainer,
                enabled = !isLoading
            )
            GlassIconButton(
                backdrop = null,
                icon = appLucideExternalLinkIcon(),
                contentDescription = stringResource(R.string.ba_catalog_bgm_action_open_gallery),
                onClick = onOpenGuide,
                width = 34.dp,
                height = 34.dp,
                variant = GlassVariant.Compact,
                iconTint = neutralTint,
                containerColor = neutralContainer
            )
        }
    }
}
