package os.kei.ui.page.main.student.tabcontent.render

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.student.BaGuideRow
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.isExpressionGalleryItem
import os.kei.ui.page.main.student.section.GuideGalleryCardItem
import os.kei.ui.page.main.student.section.gallery.GuideGalleryExpressionCardItem
import os.kei.ui.page.main.student.section.gallery.GuideGalleryUnlockLevelCardItem
import os.kei.ui.page.main.student.section.gallery.GuideGalleryVideoGroupCardItem
import os.kei.ui.page.main.student.tabcontent.profile.GuideGalleryRelatedLinkRows
import os.kei.ui.page.main.student.tabcontent.profile.GuideProfileSectionHeader
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.renderGuideGalleryStateContent(
    state: GuideGalleryTabResolvedState,
    error: String?,
    backdrop: LayerBackdrop,
    context: Context,
    sourceUrl: String,
    studentTitle: String,
    studentImageUrl: String,
    galleryCacheRevision: Int,
    onOpenExternal: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit,
    onSaveMediaPack: (items: List<Pair<String, String>>, packTitle: String) -> Unit
) {
    if (!error.isNullOrBlank()) {
        item { GuideGalleryErrorCard(error = error) }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (!state.hasRenderableContent) {
        item { GuideGalleryEmptyCard() }
        return
    }

    val mediaUrlResolver: (String) -> String = { raw ->
        galleryCacheRevision.let {
            BaGuideTempMediaCache.resolveCachedUrl(
                context = context,
                sourceUrl = sourceUrl,
                rawUrl = raw
            )
        }
    }

    var renderedCount = 0
    var insertedUnlockLevel = false
    var insertedMemoryHallVideoNearGallery = false
    var insertedPvRoleAfterOfficial = false
    var insertedGalleryRelatedLinks = false

    state.displayGalleryItems.forEachIndexed { index, item ->
        val isExpression = isExpressionGalleryItem(item)
        if (isExpression && index != state.firstExpressionIndex) {
            return@forEachIndexed
        }
        if (renderedCount > 0) {
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        if (!insertedUnlockLevel &&
            state.memoryUnlockLevel.isNotBlank() &&
            index == state.firstMemoryHallIndex
        ) {
            item {
                GuideGalleryUnlockLevelCardItem(
                    level = state.memoryUnlockLevel,
                    backdrop = backdrop
                )
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            insertedUnlockLevel = true
        }

        item {
            if (isExpression && state.expressionItems.isNotEmpty()) {
                GuideGalleryExpressionCardItem(
                    title = "角色表情",
                    items = state.expressionItems,
                    backdrop = backdrop,
                    onOpenMedia = onOpenExternal,
                    onSaveMedia = onSaveMedia,
                    onSaveMediaPack = onSaveMediaPack,
                    mediaUrlResolver = mediaUrlResolver
                )
            } else {
                GuideGalleryCardItem(
                    item = item,
                    backdrop = backdrop,
                    onOpenMedia = onOpenExternal,
                    onSaveMedia = onSaveMedia,
                    audioLoopScopeKey = sourceUrl,
                    mediaUrlResolver = mediaUrlResolver,
                    bgmFavoriteStudentTitle = studentTitle,
                    bgmFavoriteStudentImageUrl = studentImageUrl,
                    bgmFavoriteSourceUrl = sourceUrl
                )
            }
        }
        renderedCount += 1

        if (!insertedPvRoleAfterOfficial &&
            state.pvAndRoleVideoGroups.isNotEmpty() &&
            index == state.lastOfficialIntroIndex
        ) {
            state.pvAndRoleVideoGroups.forEach { (title, items) ->
                if (renderedCount > 0) {
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                }
                item {
                    GuideGalleryVideoGroupCardItem(
                        title = title,
                        items = items,
                        previewFallbackUrl = "",
                        backdrop = backdrop,
                        onOpenMedia = onOpenExternal,
                        onSaveMedia = onSaveMedia,
                        mediaUrlResolver = mediaUrlResolver
                    )
                }
                renderedCount += 1
            }
            insertedPvRoleAfterOfficial = true

            if (!insertedGalleryRelatedLinks && state.galleryRelatedLinkRows.isNotEmpty()) {
                if (renderedCount > 0) {
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                }
                item {
                    GuideGalleryRelatedLinksCard(
                        rows = state.galleryRelatedLinkRows,
                        onOpenExternal = onOpenExternal
                    )
                }
                renderedCount += 1
                insertedGalleryRelatedLinks = true
            }
        }

        if (!insertedMemoryHallVideoNearGallery &&
            state.memoryHallVideoGroup != null &&
            index == state.firstMemoryHallIndex
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                GuideGalleryVideoGroupCardItem(
                    title = state.memoryHallVideoGroup.first,
                    items = state.memoryHallVideoGroup.second,
                    previewFallbackUrl = state.memoryHallPreview,
                    backdrop = backdrop,
                    onOpenMedia = onOpenExternal,
                    onSaveMedia = onSaveMedia,
                    mediaUrlResolver = mediaUrlResolver
                )
            }
            renderedCount += 1
            insertedMemoryHallVideoNearGallery = true
        }
    }

    if (!insertedUnlockLevel &&
        state.memoryUnlockLevel.isNotBlank() &&
        state.memoryHallVideoGroup != null
    ) {
        if (renderedCount > 0) {
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
        item {
            GuideGalleryUnlockLevelCardItem(
                level = state.memoryUnlockLevel,
                backdrop = backdrop
            )
        }
        insertedUnlockLevel = true
        renderedCount += 1
    }

    if (!insertedMemoryHallVideoNearGallery && state.memoryHallVideoGroup != null) {
        if (renderedCount > 0) {
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
        item {
            GuideGalleryVideoGroupCardItem(
                title = state.memoryHallVideoGroup.first,
                items = state.memoryHallVideoGroup.second,
                previewFallbackUrl = state.memoryHallPreview,
                backdrop = backdrop,
                onOpenMedia = onOpenExternal,
                onSaveMedia = onSaveMedia,
                mediaUrlResolver = mediaUrlResolver
            )
        }
        renderedCount += 1
    }

    if (!insertedPvRoleAfterOfficial) {
        state.pvAndRoleVideoGroups.forEach { (title, items) ->
            if (renderedCount > 0) {
                item { Spacer(modifier = Modifier.height(10.dp)) }
            }
            item {
                GuideGalleryVideoGroupCardItem(
                    title = title,
                    items = items,
                    previewFallbackUrl = "",
                    backdrop = backdrop,
                    onOpenMedia = onOpenExternal,
                    onSaveMedia = onSaveMedia,
                    mediaUrlResolver = mediaUrlResolver
                )
            }
            renderedCount += 1
        }
        insertedPvRoleAfterOfficial = state.pvAndRoleVideoGroups.isNotEmpty()
        if (!insertedGalleryRelatedLinks && state.galleryRelatedLinkRows.isNotEmpty()) {
            if (renderedCount > 0) {
                item { Spacer(modifier = Modifier.height(10.dp)) }
            }
            item {
                GuideGalleryRelatedLinksCard(
                    rows = state.galleryRelatedLinkRows,
                    onOpenExternal = onOpenExternal
                )
            }
            renderedCount += 1
            insertedGalleryRelatedLinks = true
        }
    }

    state.otherTrailingVideoGroups.forEach { (title, items) ->
        if (renderedCount > 0) {
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
        item {
            GuideGalleryVideoGroupCardItem(
                title = title,
                items = items,
                previewFallbackUrl = "",
                backdrop = backdrop,
                onOpenMedia = onOpenExternal,
                onSaveMedia = onSaveMedia,
                mediaUrlResolver = mediaUrlResolver
            )
        }
        renderedCount += 1
    }

    if (!insertedGalleryRelatedLinks && state.galleryRelatedLinkRows.isNotEmpty()) {
        if (renderedCount > 0) {
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
        item {
            GuideGalleryRelatedLinksCard(
                rows = state.galleryRelatedLinkRows,
                onOpenExternal = onOpenExternal
            )
        }
    }
}

@Composable
private fun GuideGalleryErrorCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = error,
                color = MiuixTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GuideGalleryEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "暂未解析到影画鉴赏内容，点击右上角刷新后重试。",
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
        }
    }
}

@Composable
private fun GuideGalleryRelatedLinksCard(
    rows: List<BaGuideRow>,
    onOpenExternal: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GuideProfileSectionHeader(
                title = "影画相关链接"
            )
            GuideGalleryRelatedLinkRows(
                rows = rows,
                onOpenExternal = onOpenExternal
            )
        }
    }
}
