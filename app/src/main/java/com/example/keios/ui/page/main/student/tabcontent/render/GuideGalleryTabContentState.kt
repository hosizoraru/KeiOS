package com.example.keios.ui.page.main.student.tabcontent.render

import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.BaStudentGuideInfo
import com.example.keios.ui.page.main.student.expressionGalleryOrder
import com.example.keios.ui.page.main.student.hasRenderableGalleryMedia
import com.example.keios.ui.page.main.student.isChocolateGalleryItem
import com.example.keios.ui.page.main.student.isExpressionGalleryItem
import com.example.keios.ui.page.main.student.isInteractiveFurnitureGalleryItem
import com.example.keios.ui.page.main.student.isMemoryHallFileGalleryItem
import com.example.keios.ui.page.main.student.isMemoryHallGalleryItem
import com.example.keios.ui.page.main.student.isOfficialIntroGalleryItem
import com.example.keios.ui.page.main.student.isPreviewVideoCategoryGalleryItem
import com.example.keios.ui.page.main.student.isPreviewVideoGalleryItem
import com.example.keios.ui.page.main.student.isRenderableGalleryAudioUrl
import com.example.keios.ui.page.main.student.isRenderableGalleryImageUrl
import com.example.keios.ui.page.main.student.normalizeGalleryTitle
import com.example.keios.ui.page.main.student.profileRowsForDisplay
import com.example.keios.ui.page.main.student.tabcontent.profile.isGalleryRelatedProfileLinkRow
import com.example.keios.ui.page.main.student.tabcontent.profile.normalizeProfileFieldKey

internal data class GuideGalleryTabResolvedState(
    val previewVideoGroups: List<Pair<String, List<BaGuideGalleryItem>>>,
    val memoryHallVideoGroup: Pair<String, List<BaGuideGalleryItem>>?,
    val pvAndRoleVideoGroups: List<Pair<String, List<BaGuideGalleryItem>>>,
    val otherTrailingVideoGroups: List<Pair<String, List<BaGuideGalleryItem>>>,
    val displayGalleryItems: List<BaGuideGalleryItem>,
    val expressionItems: List<BaGuideGalleryItem>,
    val galleryRelatedLinkRows: List<BaGuideRow>,
    val memoryHallPreview: String,
    val memoryUnlockLevel: String,
    val firstExpressionIndex: Int,
    val firstMemoryHallIndex: Int,
    val lastOfficialIntroIndex: Int
) {
    val hasRenderableContent: Boolean
        get() = displayGalleryItems.isNotEmpty() || previewVideoGroups.isNotEmpty()
}

internal fun resolveGuideGalleryTabState(guide: BaStudentGuideInfo): GuideGalleryTabResolvedState {
    val galleryItems = if (guide.galleryItems.isNotEmpty()) {
        guide.galleryItems
            .filter(::hasRenderableGalleryMedia)
            .distinctBy {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                "${it.mediaType}|$media"
            }
    } else {
        listOfNotNull(
            guide.imageUrl.takeIf { it.isNotBlank() }?.let {
                BaGuideGalleryItem(
                    title = "立绘",
                    imageUrl = it,
                    mediaType = "image",
                    mediaUrl = it
                ).takeIf(::hasRenderableGalleryMedia)
            }
        )
    }

    val cleanedGalleryItems = galleryItems.filterNot(::isMemoryHallFileGalleryItem)
    val galleryRelatedLinkRows = guide.profileRowsForDisplay()
        .filter(::isGalleryRelatedProfileLinkRow)
        .distinctBy { row ->
            "${normalizeProfileFieldKey(row.key)}|${row.value.trim()}"
        }
        .take(10)
    val memoryHallPreview = cleanedGalleryItems
        .firstOrNull {
            isMemoryHallGalleryItem(it) && isRenderableGalleryImageUrl(it.imageUrl)
        }
        ?.imageUrl
        .orEmpty()

    val previewVideoGroups = run {
        val orderedCategories = cleanedGalleryItems
            .asSequence()
            .mapNotNull { item ->
                if (!isPreviewVideoGalleryItem(item)) return@mapNotNull null
                val normalized = normalizeGalleryTitle(item.title)
                when {
                    normalized.startsWith("回忆大厅视频") -> "回忆大厅视频"
                    normalized.startsWith("PV") -> "PV"
                    normalized.startsWith("角色演示") -> "角色演示"
                    else -> null
                }
            }
            .distinct()
            .toList()

        orderedCategories.mapNotNull { category ->
            val categoryFallbackPreview =
                if (category == "回忆大厅视频" && isRenderableGalleryImageUrl(memoryHallPreview)) {
                    memoryHallPreview
                } else {
                    ""
                }
            val categoryItems = cleanedGalleryItems
                .asSequence()
                .filter(::isPreviewVideoGalleryItem)
                .filter { item ->
                    val normalized = normalizeGalleryTitle(item.title)
                    when (category) {
                        "回忆大厅视频" -> normalized.startsWith("回忆大厅视频")
                        "PV" -> normalized.startsWith("PV")
                        "角色演示" -> normalized.startsWith("角色演示")
                        else -> false
                    }
                }
                .mapNotNull { item ->
                    val currentPreview = item.imageUrl
                    val preview = when {
                        isRenderableGalleryImageUrl(currentPreview) -> currentPreview
                        categoryFallbackPreview.isNotBlank() -> categoryFallbackPreview
                        else -> ""
                    }
                    if (preview.isBlank()) {
                        null
                    } else if (preview == currentPreview) {
                        item
                    } else {
                        item.copy(imageUrl = preview)
                    }
                }
                .toList()
            categoryItems.takeIf { it.isNotEmpty() }?.let { category to it }
        }
    }

    val memoryHallVideoGroup = previewVideoGroups.firstOrNull { it.first == "回忆大厅视频" }
    val trailingVideoGroups = previewVideoGroups.filterNot { it.first == "回忆大厅视频" }
    val pvAndRoleVideoGroups = trailingVideoGroups.filter { (title, _) ->
        title == "PV" || title == "角色演示"
    }
    val otherTrailingVideoGroups = trailingVideoGroups.filterNot { (title, _) ->
        title == "PV" || title == "角色演示"
    }

    val displayGalleryItems = cleanedGalleryItems
        .filterNot(::isPreviewVideoCategoryGalleryItem)
        .filterNot(::isChocolateGalleryItem)
        .filterNot(::isInteractiveFurnitureGalleryItem)
        .filter { item ->
            when (item.mediaType.lowercase()) {
                "audio" -> isRenderableGalleryAudioUrl(item.mediaUrl)
                else -> isRenderableGalleryImageUrl(item.imageUrl)
            }
        }

    val memoryUnlockLevel = cleanedGalleryItems
        .asSequence()
        .map { it.memoryUnlockLevel }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .ifBlank {
            val fallback = guide.profileRows
                .firstOrNull { it.key.trim() == "回忆大厅解锁等级" }
                ?.value
                .orEmpty()
            Regex("""\d+""").find(fallback)?.value.orEmpty().ifBlank { fallback }
        }

    val expressionItems = displayGalleryItems
        .withIndex()
        .filter { isExpressionGalleryItem(it.value) }
        .sortedBy { indexed ->
            expressionGalleryOrder(indexed.value.title, indexed.index + 1)
        }
        .map { it.value }

    return GuideGalleryTabResolvedState(
        previewVideoGroups = previewVideoGroups,
        memoryHallVideoGroup = memoryHallVideoGroup,
        pvAndRoleVideoGroups = pvAndRoleVideoGroups,
        otherTrailingVideoGroups = otherTrailingVideoGroups,
        displayGalleryItems = displayGalleryItems,
        expressionItems = expressionItems,
        galleryRelatedLinkRows = galleryRelatedLinkRows,
        memoryHallPreview = memoryHallPreview,
        memoryUnlockLevel = memoryUnlockLevel,
        firstExpressionIndex = displayGalleryItems.indexOfFirst(::isExpressionGalleryItem),
        firstMemoryHallIndex = displayGalleryItems.indexOfFirst(::isMemoryHallGalleryItem),
        lastOfficialIntroIndex = displayGalleryItems.indexOfLast(::isOfficialIntroGalleryItem)
    )
}
