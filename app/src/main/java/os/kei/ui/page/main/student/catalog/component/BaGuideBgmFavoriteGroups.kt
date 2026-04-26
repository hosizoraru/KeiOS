package os.kei.ui.page.main.student.catalog.component

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.student.BaStudentGuideStore
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBgmFavoritePlaybackSnapshot
import os.kei.ui.page.main.student.buildProfileMetaItems
import os.kei.ui.page.main.student.catalog.BaGuideCatalogStore
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.fetch.extractGuideContentIdFromUrl
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class BaGuideBgmFavoriteGroupMode(@param:StringRes val labelRes: Int) {
    All(R.string.ba_catalog_bgm_group_all),
    Academy(R.string.ba_catalog_bgm_group_school),
    RoleType(R.string.ba_catalog_bgm_group_role_type),
    RecentPlayed(R.string.ba_catalog_bgm_group_recent_played)
}

internal data class BaGuideBgmFavoriteGroupLabels(
    val all: String,
    val student: String,
    val npcSatellite: String,
    val unknownAcademy: String,
    val unknownType: String,
    val played: String,
    val unplayed: String
)

internal data class BaGuideBgmFavoriteGroup(
    val key: String,
    val label: String,
    val favorites: List<GuideBgmFavoriteItem>
)

@Composable
internal fun rememberBaGuideBgmFavoriteGroupLabels(): BaGuideBgmFavoriteGroupLabels {
    return BaGuideBgmFavoriteGroupLabels(
        all = stringResource(R.string.ba_catalog_bgm_group_all),
        student = stringResource(R.string.ba_catalog_tab_student),
        npcSatellite = stringResource(R.string.ba_catalog_tab_npc_satellite),
        unknownAcademy = stringResource(R.string.ba_catalog_bgm_group_unknown_school),
        unknownType = stringResource(R.string.ba_catalog_bgm_group_unknown_type),
        played = stringResource(R.string.ba_catalog_bgm_group_played),
        unplayed = stringResource(R.string.ba_catalog_bgm_group_unplayed)
    )
}

@Composable
internal fun BaGuideBgmGroupModeRow(
    groupMode: BaGuideBgmFavoriteGroupMode,
    accent: Color,
    onGroupModeChange: (BaGuideBgmFavoriteGroupMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BaGuideBgmFavoriteGroupMode.entries.forEach { mode ->
            val selected = mode == groupMode
            GlassTextButton(
                backdrop = null,
                text = stringResource(mode.labelRes),
                onClick = { onGroupModeChange(mode) },
                textColor = if (selected) accent else Color(0xFF64748B),
                containerColor = if (selected) accent else null,
                variant = GlassVariant.Compact,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun BaGuideBgmFavoriteGroupHeader(
    label: String,
    count: Int,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        StatusPill(
            label = count.coerceAtLeast(0).toString(),
            color = accent,
            size = AppStatusPillSize.Compact
        )
    }
}

internal fun groupBgmFavorites(
    favorites: List<GuideBgmFavoriteItem>,
    groupMode: BaGuideBgmFavoriteGroupMode,
    playbackSnapshot: GuideBgmFavoritePlaybackSnapshot,
    labels: BaGuideBgmFavoriteGroupLabels
): List<BaGuideBgmFavoriteGroup> {
    if (favorites.isEmpty()) return emptyList()
    return when (groupMode) {
        BaGuideBgmFavoriteGroupMode.All -> listOf(
            BaGuideBgmFavoriteGroup(
                key = "all",
                label = labels.all,
                favorites = favorites
            )
        )
        BaGuideBgmFavoriteGroupMode.Academy -> favorites
            .groupBy { favorite -> bgmFavoriteAcademy(favorite).ifBlank { labels.unknownAcademy } }
            .toSortedMap(compareBy<String> { it == labels.unknownAcademy }.thenBy { it })
            .map { (academy, items) ->
                BaGuideBgmFavoriteGroup(
                    key = "academy-$academy",
                    label = academy,
                    favorites = items
                )
            }
        BaGuideBgmFavoriteGroupMode.RoleType -> {
            val orderedLabels = listOf(labels.student, labels.npcSatellite, labels.unknownType)
            favorites
                .groupBy { favorite ->
                    when (bgmFavoriteCatalogTab(favorite)) {
                        BaGuideCatalogTab.Student -> labels.student
                        BaGuideCatalogTab.NpcSatellite -> labels.npcSatellite
                        null -> labels.unknownType
                    }
                }
                .toSortedMap(compareBy<String> { orderedLabels.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE })
                .map { (type, items) ->
                    BaGuideBgmFavoriteGroup(
                        key = "type-$type",
                        label = type,
                        favorites = items
                    )
                }
        }
        BaGuideBgmFavoriteGroupMode.RecentPlayed -> {
            val orderedLabels = listOf(labels.played, labels.unplayed)
            favorites
                .groupBy { favorite ->
                    val playedAt = playbackSnapshot.progressFor(favorite.audioUrl)?.lastPlayedAtMs ?: 0L
                    if (playedAt > 0L) labels.played else labels.unplayed
                }
                .toSortedMap(compareBy<String> { orderedLabels.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE })
                .map { (status, items) ->
                    BaGuideBgmFavoriteGroup(
                        key = "played-$status",
                        label = status,
                        favorites = items
                    )
                }
        }
    }
}

internal fun bgmFavoriteAcademy(favorite: GuideBgmFavoriteItem): String {
    val info = BaStudentGuideStore.loadInfo(favorite.sourceUrl) ?: return ""
    return info.buildProfileMetaItems()
        .firstOrNull { item -> item.title == "学院" }
        ?.value
        ?.trim()
        ?.takeIf { it.isNotBlank() && it != "-" }
        .orEmpty()
}

internal fun bgmFavoriteCatalogTab(favorite: GuideBgmFavoriteItem): BaGuideCatalogTab? {
    val contentId = extractGuideContentIdFromUrl(favorite.sourceUrl) ?: return null
    if (contentId <= 0L) return null
    val bundle = BaGuideCatalogStore.loadBundle() ?: return null
    return BaGuideCatalogTab.entries.firstOrNull { tab ->
        bundle.entries(tab).any { entry -> entry.contentId == contentId }
    }
}
