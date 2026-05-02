package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideCatalogEntryCard(
    entry: BaGuideCatalogEntry,
    isFavorite: Boolean,
    loadAvatarImage: Boolean = true,
    onOpenGuide: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    val uiState = rememberBaGuideCatalogEntryCardUiState(entry = entry, isFavorite = isFavorite)
    val copyAction = rememberBaGuideCatalogEntryCopyAction(copyPayload = uiState.copyPayload)
    AppSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = uiState.containerColor,
        borderColor = uiState.borderColor,
        onClick = { onOpenGuide(entry.detailUrl) },
        onLongClick = copyAction
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CardLayoutRhythm.cardHorizontalPadding,
                    vertical = CardLayoutRhythm.cardVerticalPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                BaGuideCatalogEntryAvatar(
                    imageUrl = entry.iconUrl,
                    fallbackRes = entry.tab.iconRes,
                    loadEnabled = loadAvatarImage
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowTextGap)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = entry.name,
                            modifier = Modifier.weight(1f),
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = AppTypographyTokens.CompactTitle.fontSize,
                            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatusPill(
                            label = "ID ${entry.contentId}",
                            color = MiuixTheme.colorScheme.primary,
                            size = AppStatusPillSize.Compact
                        )
                    }
                    if (entry.aliasDisplay.isNotBlank()) {
                        Text(
                            text = entry.aliasDisplay,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = AppTypographyTokens.Supporting.fontSize,
                            lineHeight = AppTypographyTokens.Supporting.lineHeight,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            AppStandaloneLiquidIconButton(
                icon = MiuixIcons.Regular.FavoritesFill,
                contentDescription = uiState.favoriteContentDescription,
                onClick = { onToggleFavorite(entry.contentId) },
                width = 34.dp,
                height = 34.dp,
                variant = GlassVariant.Floating,
                iconTint = uiState.favoriteActionColor,
                containerColor = uiState.favoriteActionColor
            )
        }
    }
}
