package com.example.keios.ui.page.main.student.tabcontent.render

import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.BaStudentGuideInfo
import com.example.keios.ui.page.main.student.hasRenderableGalleryMedia
import com.example.keios.ui.page.main.student.isChocolateGalleryItem
import com.example.keios.ui.page.main.student.isInteractiveFurnitureGalleryItem
import com.example.keios.ui.page.main.student.profileRowsForDisplay
import com.example.keios.ui.page.main.student.shouldHideMovedHeaderRow
import com.example.keios.ui.page.main.student.tabcontent.isGrowthTitleVoiceRow
import com.example.keios.ui.page.main.student.tabcontent.isVoicePlaceholderRow
import com.example.keios.ui.page.main.student.tabcontent.profile.GiftPreferenceItem
import com.example.keios.ui.page.main.student.tabcontent.profile.SameNameRoleItem
import com.example.keios.ui.page.main.student.tabcontent.profile.buildGiftPreferenceItems
import com.example.keios.ui.page.main.student.tabcontent.profile.buildProfileCardRows
import com.example.keios.ui.page.main.student.tabcontent.profile.buildSameNameRoleItems
import com.example.keios.ui.page.main.student.tabcontent.profile.extractSameNameRoleHint
import com.example.keios.ui.page.main.student.tabcontent.profile.isGiftPreferenceProfileRow
import com.example.keios.ui.page.main.student.tabcontent.profile.isGalleryRelatedProfileLinkRow
import com.example.keios.ui.page.main.student.tabcontent.profile.isProfileSectionHeaderRow
import com.example.keios.ui.page.main.student.tabcontent.profile.isSameNameRoleRow
import com.example.keios.ui.page.main.student.tabcontent.profile.isSkillMigratedProfileRow
import com.example.keios.ui.page.main.student.tabcontent.profile.isStructuredProfileCardRow
import com.example.keios.ui.page.main.student.tabcontent.profile.normalizeProfileFieldKey
import com.example.keios.ui.page.main.student.tabcontent.profile.profileHobbyFieldSpecs
import com.example.keios.ui.page.main.student.tabcontent.profile.profileNicknameFieldSpecs
import com.example.keios.ui.page.main.student.tabcontent.profile.profileStudentInfoFieldSpecs
import com.example.keios.ui.page.main.student.tabcontent.profile.sortGalleryItemsByTitleNumbers
import com.example.keios.ui.page.main.student.tabcontent.profile.sortProfileRowsByKeyNumbers

internal data class GuideProfileTabHeaderState(
    val nicknameRows: List<BaGuideRow>,
    val studentInfoRows: List<BaGuideRow>,
    val hobbyRows: List<BaGuideRow>,
    val giftPreferenceItems: List<GiftPreferenceItem>,
    val chocolateInfoRows: List<BaGuideRow>,
    val furnitureInfoRows: List<BaGuideRow>,
    val normalProfileRows: List<BaGuideRow>,
    val sameNameRoleItems: List<SameNameRoleItem>,
    val sameNameRoleHint: String,
    val chocolateGalleryItems: List<BaGuideGalleryItem>,
    val furnitureGalleryItems: List<BaGuideGalleryItem>
)

internal fun buildGuideProfileTabHeaderState(
    guide: BaStudentGuideInfo
): GuideProfileTabHeaderState {
    val profileRowsBase = guide.profileRowsForDisplay()
        .filterNot(::shouldHideMovedHeaderRow)
        .filterNot(::isGrowthTitleVoiceRow)
        .filterNot(::isVoicePlaceholderRow)
        .filterNot(::isProfileSectionHeaderRow)
        .filterNot(::isGalleryRelatedProfileLinkRow)
    val sameNameRoleRows = profileRowsBase.filter(::isSameNameRoleRow)
    val sameNameRoleItems = buildSameNameRoleItems(sameNameRoleRows)
    val sameNameRoleHint = sameNameRoleRows.firstNotNullOfOrNull { row ->
        extractSameNameRoleHint(row)
    }.orEmpty()
    val hasTopDataHeader = profileRowsBase.any { row ->
        normalizeProfileFieldKey(row.key) == normalizeProfileFieldKey("顶级数据")
    }
    val hasInitialDataHeader = profileRowsBase.any { row ->
        normalizeProfileFieldKey(row.key) == normalizeProfileFieldKey("初始数据")
    }
    val allProfileRows = profileRowsBase.filterNot { row ->
        isSkillMigratedProfileRow(
            row = row,
            hasTopDataHeader = hasTopDataHeader,
            hasInitialDataHeader = hasInitialDataHeader
        ) || isSameNameRoleRow(row)
    }
    val nicknameRows = buildProfileCardRows(
        rows = allProfileRows,
        specs = profileNicknameFieldSpecs
    )
    val studentInfoRows = buildProfileCardRows(
        rows = allProfileRows,
        specs = profileStudentInfoFieldSpecs
    )
    val hobbyRows = buildProfileCardRows(
        rows = allProfileRows,
        specs = profileHobbyFieldSpecs
    )
    val giftPreferenceRows = allProfileRows
        .filter(::isGiftPreferenceProfileRow)
        .let(::sortProfileRowsByKeyNumbers)
    val giftPreferenceItems = buildGiftPreferenceItems(giftPreferenceRows)
    val chocolateInfoRows = allProfileRows.filter { row ->
        val key = row.key.trim()
        key.contains("巧克力", ignoreCase = true)
    }.let(::sortProfileRowsByKeyNumbers)
    val furnitureInfoRows = allProfileRows.filter { row ->
        val key = row.key.trim()
        key.contains("互动家具", ignoreCase = true)
    }.let(::sortProfileRowsByKeyNumbers)
    val normalProfileRows = allProfileRows.filterNot { row ->
        val key = row.key.trim()
        key.contains("巧克力", ignoreCase = true) ||
            key.contains("互动家具", ignoreCase = true) ||
            isGiftPreferenceProfileRow(row) ||
            isStructuredProfileCardRow(row)
    }
    val chocolateGalleryItems = guide.galleryItems
        .filter(::isChocolateGalleryItem)
        .filter(::hasRenderableGalleryMedia)
        .distinctBy {
            val media = it.mediaUrl.ifBlank { it.imageUrl }
            "${it.mediaType}|$media"
        }
        .let(::sortGalleryItemsByTitleNumbers)
    val furnitureGalleryItems = guide.galleryItems
        .filter(::isInteractiveFurnitureGalleryItem)
        .filter(::hasRenderableGalleryMedia)
        .distinctBy {
            val media = it.mediaUrl.ifBlank { it.imageUrl }
            "${it.mediaType}|$media"
        }
        .let(::sortGalleryItemsByTitleNumbers)
    return GuideProfileTabHeaderState(
        nicknameRows = nicknameRows,
        studentInfoRows = studentInfoRows,
        hobbyRows = hobbyRows,
        giftPreferenceItems = giftPreferenceItems,
        chocolateInfoRows = chocolateInfoRows,
        furnitureInfoRows = furnitureInfoRows,
        normalProfileRows = normalProfileRows,
        sameNameRoleItems = sameNameRoleItems,
        sameNameRoleHint = sameNameRoleHint,
        chocolateGalleryItems = chocolateGalleryItems,
        furnitureGalleryItems = furnitureGalleryItems
    )
}
