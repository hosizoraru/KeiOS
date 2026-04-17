package com.example.keios.ui.page.main.student

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.keios.R
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.CopyModeSelectionContainer
import com.example.keios.ui.page.main.widget.buildTextCopyPayload
import com.example.keios.ui.page.main.widget.copyModeAwareRow
import com.example.keios.ui.page.main.widget.rememberLightTextCopyAction
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val GUIDE_SIMULATE_CACHE_MAX_SIZE = 96
private val guideSimulateDataCache = object : LinkedHashMap<String, GuideSimulateData>(
    GUIDE_SIMULATE_CACHE_MAX_SIZE,
    0.75f,
    true
) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GuideSimulateData>?): Boolean {
        return size > GUIDE_SIMULATE_CACHE_MAX_SIZE
    }
}

private fun buildGuideCopyPayload(key: String, value: String): String {
    return buildTextCopyPayload(key, value)
}

@Composable
private fun rememberGuideCopyAction(copyPayload: String): () -> Unit {
    val quickCopyAction = rememberLightTextCopyAction(copyPayload)
    return remember(quickCopyAction) {
        { quickCopyAction?.invoke() }
    }
}

private fun Modifier.guideCopyable(
    copyPayload: String,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    this.copyModeAwareRow(
        copyPayload = copyPayload,
        onClick = onClick
    )
}

internal fun LazyListScope.renderBaStudentGuideTabContent(
    activeBottomTab: GuideBottomTab,
    info: BaStudentGuideInfo?,
    error: String?,
    backdrop: LayerBackdrop,
    accent: Color,
    context: Context,
    sourceUrl: String,
    galleryCacheRevision: Int,
    playingVoiceUrl: String,
    isVoicePlaying: Boolean,
    voicePlayProgress: Float,
    selectedVoiceLanguage: String,
    onOpenExternal: (String) -> Unit,
    onOpenGuide: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit,
    onToggleVoicePlayback: (String) -> Unit,
    onSelectedVoiceLanguageChange: (String) -> Unit
) {
                when (activeBottomTab) {
                    GuideBottomTab.Archive -> {
                        item {
                            val guide = info
                            val profileItems = remember(
                                guide?.sourceUrl,
                                guide?.syncedAtMs
                            ) {
                                guide?.buildProfileMetaItems().orEmpty()
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = Color(0x223B82F6),
                                    contentColor = MiuixTheme.colorScheme.onBackground
                                ),
                                onClick = {}
                            ) {
                                if (guide != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(modifier = Modifier.width(112.dp)) {
                                                if (guide.imageUrl.isNotBlank()) {
                                                    GuideRemoteImage(
                                                        imageUrl = guide.imageUrl,
                                                        imageHeight = 152.dp
                                                    )
                                                } else {
                                                    Text(
                                                        text = "暂无图片",
                                                        color = MiuixTheme.colorScheme.onBackgroundVariant
                                                    )
                                                }
                                            }
                                            val rarityItem = profileItems.firstOrNull { meta ->
                                                meta.title == "稀有度" || meta.title == "星级"
                                            }
                                            val academyItem = profileItems.firstOrNull { meta ->
                                                meta.title == "学院"
                                            }
                                            val clubItem = profileItems.firstOrNull { meta ->
                                                meta.title == "所属社团" || meta.title == "社团"
                                            }
                                            val alignedItems = listOfNotNull(rarityItem, academyItem, clubItem)
                                            val extraItems = profileItems.filterNot { item ->
                                                alignedItems.any { it.title == item.title && it.value == item.value }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(152.dp)
                                            ) {
                                                rarityItem?.let { item ->
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .fillMaxWidth()
                                                    ) {
                                                        GuideProfileMetaLine(item)
                                                    }
                                                }
                                                academyItem?.let { item ->
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.CenterStart)
                                                            .fillMaxWidth()
                                                    ) {
                                                        GuideProfileMetaLine(item)
                                                    }
                                                }
                                                clubItem?.let { item ->
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomStart)
                                                            .fillMaxWidth()
                                                    ) {
                                                        GuideProfileMetaLine(item)
                                                    }
                                                }
                                                if (alignedItems.isEmpty()) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        profileItems.forEach { item ->
                                                            GuideProfileMetaLine(item)
                                                        }
                                                    }
                                                } else if (extraItems.isNotEmpty()) {
                                                    Column(
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .fillMaxWidth()
                                                            .padding(top = 2.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        extraItems.forEach { item ->
                                                            GuideProfileMetaLine(item)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(10.dp)) }
                        item {
                            val guide = info
                            val combatItems = remember(
                                guide?.sourceUrl,
                                guide?.syncedAtMs
                            ) {
                                guide?.buildCombatMetaItems().orEmpty()
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = Color(0x223B82F6),
                                    contentColor = MiuixTheme.colorScheme.onBackground
                                ),
                                onClick = {}
                            ) {
                                if (guide != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        combatItems.forEach { item ->
                                            GuideCombatMetaTile(
                                                item = item,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GuideBottomTab.Skills -> {
                        val guide = info
                        if (guide == null) {
                            item {
                                FrostedBlock(
                                    backdrop = backdrop,
                                    title = activeBottomTab.label,
                                    subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                    accent = accent,
                                    content = {
                                        error?.takeIf { it.isNotBlank() }?.let {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = it,
                                                color = MiuixTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
                            val skillCards = guide.skillCardsForDisplay()
                            val weaponCard = guide.weaponCardForDisplay()

                            if (!error.isNullOrBlank()) {
                                item {
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
                                                text = error.orEmpty(),
                                                color = MiuixTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (skillCards.isNotEmpty()) {
                                skillCards.forEachIndexed { index, card ->
                                    item {
                                        GuideSkillCardItem(
                                            card = card,
                                            backdrop = backdrop
                                        )
                                    }
                                    if (index < skillCards.lastIndex) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                }
                            } else {
                                item {
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
                                                text = "暂未解析到结构化技能卡数据。",
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        }
                                    }
                                }
                            }

                            weaponCard?.let { weapon ->
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                                item {
                                    GuideWeaponCardItem(
                                        card = weapon,
                                        backdrop = backdrop
                                    )
                                }
                            }
                        }
                    }

                    GuideBottomTab.Profile -> {
                        val guide = info
                        if (guide == null) {
                            item {
                                FrostedBlock(
                                    backdrop = backdrop,
                                    title = activeBottomTab.label,
                                    subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                    accent = accent,
                                    content = {
                                        error?.takeIf { it.isNotBlank() }?.let {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = it,
                                                color = MiuixTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
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

                            if (!error.isNullOrBlank()) {
                                item {
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
                                            Text(
                                                text = error.orEmpty(),
                                                color = MiuixTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (nicknameRows.isNotEmpty()) {
                                item {
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
                                                title = "学生昵称"
                                            )
                                            GuideProfileInfoRows(rows = nicknameRows) { row ->
                                                GuideProfileInfoItem(
                                                    key = row.key.ifBlank { "信息" },
                                                    value = row.value.ifBlank { "-" }
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (studentInfoRows.isNotEmpty()) {
                                item {
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
                                                title = "学生信息"
                                            )
                                            GuideProfileInfoRows(rows = studentInfoRows) { row ->
                                                val normalizedKey = normalizeProfileFieldKey(row.key)
                                                if (normalizedKey == profileRoleReferenceFieldKey) {
                                                    val externalLink = remember(row.value) {
                                                        extractProfileExternalLink(row.value)
                                                    }
                                                    val resolvedTitle by produceState(
                                                        initialValue = if (externalLink.isNotBlank()) {
                                                            profileLinkTitleCache[externalLink].orEmpty()
                                                        } else {
                                                            ""
                                                        },
                                                        key1 = externalLink
                                                    ) {
                                                        value = if (externalLink.isBlank()) {
                                                            ""
                                                        } else {
                                                            withContext(Dispatchers.IO) {
                                                                resolveProfileLinkTitle(externalLink)
                                                            }
                                                        }
                                                    }
                                                    val displayValue = when {
                                                        externalLink.isBlank() -> row.value.ifBlank { "-" }
                                                        resolvedTitle.isNotBlank() -> resolvedTitle
                                                        else -> fallbackProfileLinkTitle(externalLink)
                                                    }
                                                    GuideProfileInfoItem(
                                                        key = row.key.ifBlank { "信息" },
                                                        value = displayValue,
                                                        onClick = externalLink.takeIf { it.isNotBlank() }?.let { link ->
                                                            { onOpenExternal(link) }
                                                        },
                                                        valueColor = if (externalLink.isNotBlank()) {
                                                            Color(0xFF5FA8FF)
                                                        } else {
                                                            null
                                                        },
                                                        preferCapsule = false
                                                    )
                                                } else {
                                                    GuideProfileInfoItem(
                                                        key = row.key.ifBlank { "信息" },
                                                        value = row.value.ifBlank { "-" }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (hobbyRows.isNotEmpty()) {
                                item {
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
                                                title = "学生爱好"
                                            )
                                            GuideProfileInfoRows(rows = hobbyRows) { row ->
                                                GuideProfileInfoItem(
                                                    key = row.key.ifBlank { "信息" },
                                                    value = row.value.ifBlank { "-" },
                                                    preferCapsule = false
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (giftPreferenceItems.isNotEmpty()) {
                                item {
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
                                                title = "礼物偏好"
                                            )
                                            GuideGiftPreferenceGrid(
                                                items = giftPreferenceItems
                                            )
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            item {
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
                                            title = "相关同名角色"
                                        )
                                        sameNameRoleHint.takeIf { it.isNotBlank() }?.let { hint ->
                                            CopyModeSelectionContainer {
                                                Text(
                                                    text = hint,
                                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                                    modifier = Modifier.guideCopyable(
                                                        buildGuideCopyPayload("相关同名角色", hint)
                                                    ),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (sameNameRoleItems.isEmpty()) {
                                            Text(
                                                text = "暂无同名角色条目。",
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        } else {
                                            sameNameRoleItems.forEachIndexed { index, role ->
                                                if (index > 0) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                                val roleCopyPayload = buildString {
                                                    append(role.name.ifBlank { "同名角色" })
                                                    role.linkUrl.trim().takeIf { it.isNotBlank() }?.let { link ->
                                                        append('\n')
                                                        append(link)
                                                    }
                                                }
                                                CopyModeSelectionContainer {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .guideCopyable(
                                                                buildGuideCopyPayload(
                                                                    "同名角色",
                                                                    roleCopyPayload
                                                                )
                                                            ),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                    val previewImage = role.imageUrl.trim()
                                                    if (previewImage.isNotBlank()) {
                                                        GuideRemoteIcon(
                                                            imageUrl = previewImage,
                                                            iconWidth = 68.dp,
                                                            iconHeight = 68.dp
                                                        )
                                                    }
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val link = role.linkUrl.trim()
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = role.name.ifBlank { "同名角色" },
                                                                color = MiuixTheme.colorScheme.onBackground,
                                                                modifier = Modifier.weight(1f),
                                                                maxLines = Int.MAX_VALUE,
                                                                overflow = TextOverflow.Clip
                                                            )
                                                            if (link.isNotBlank()) {
                                                                GlassTextButton(
                                                                    backdrop = backdrop,
                                                                    text = "图鉴",
                                                                    textColor = Color(0xFF3B82F6),
                                                                    variant = GlassVariant.Compact,
                                                                    onClick = { onOpenGuide(link) }
                                                                )
                                                            }
                                                        }
                                                        if (link.isBlank()) {
                                                            Text(
                                                                text = "暂无可跳转链接",
                                                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(10.dp)) }

                            if (normalProfileRows.isNotEmpty()) {
                                item {
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
                                            GuideProfileRowsSection(
                                                rows = normalProfileRows,
                                                emptyText = "暂未解析到学生档案数据。"
                                            )
                                        }
                                    }
                                }
                            } else if (
                                nicknameRows.isEmpty() &&
                                studentInfoRows.isEmpty() &&
                                hobbyRows.isEmpty() &&
                                giftPreferenceItems.isEmpty()
                            ) {
                                item {
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
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "暂未解析到学生档案数据。",
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        }
                                    }
                                }
                            }

                            if (chocolateInfoRows.isNotEmpty() || chocolateGalleryItems.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                                item {
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
                                                title = "巧克力"
                                            )
                                            GuideProfileInfoRows(rows = chocolateInfoRows) { row ->
                                                val value = row.value.ifBlank { "-" }
                                                GuideProfileInfoItem(
                                                    key = row.key.ifBlank { "信息" },
                                                    value = value
                                                )
                                            }
                                            chocolateGalleryItems.forEachIndexed { index, chocolateItem ->
                                                if (chocolateInfoRows.isNotEmpty() || index > 0) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                }
                                                GuideGalleryCardItem(
                                                    item = chocolateItem,
                                                    backdrop = backdrop,
                                                    onOpenMedia = onOpenExternal,
                                                    onSaveMedia = onSaveMedia,
                                                    audioLoopScopeKey = sourceUrl,
                                                    mediaUrlResolver = { raw ->
                                                        galleryCacheRevision.let {
                                                            BaGuideTempMediaCache.resolveCachedUrl(
                                                                context = context,
                                                                sourceUrl = sourceUrl,
                                                                rawUrl = raw
                                                            )
                                                        }
                                                    },
                                                    embedded = true,
                                                    showMediaTypeLabel = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (furnitureInfoRows.isNotEmpty() || furnitureGalleryItems.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                                item {
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
                                                title = "互动家具"
                                            )
                                            GuideProfileInfoRows(rows = furnitureInfoRows) { row ->
                                                val value = row.value.ifBlank { "-" }
                                                GuideProfileInfoItem(
                                                    key = row.key.ifBlank { "信息" },
                                                    value = value,
                                                    preferCapsule = false
                                                )
                                            }
                                            furnitureGalleryItems.forEachIndexed { index, furnitureItem ->
                                                if (furnitureInfoRows.isNotEmpty() || index > 0) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                }
                                                GuideGalleryCardItem(
                                                    item = furnitureItem,
                                                    backdrop = backdrop,
                                                    onOpenMedia = onOpenExternal,
                                                    onSaveMedia = onSaveMedia,
                                                    audioLoopScopeKey = sourceUrl,
                                                    mediaUrlResolver = { raw ->
                                                        galleryCacheRevision.let {
                                                            BaGuideTempMediaCache.resolveCachedUrl(
                                                                context = context,
                                                                sourceUrl = sourceUrl,
                                                                rawUrl = raw
                                                            )
                                                        }
                                                    },
                                                    embedded = true,
                                                    showMediaTypeLabel = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GuideBottomTab.Voice -> {
                        val guide = info
                        if (guide == null) {
                            item {
                                FrostedBlock(
                                    backdrop = backdrop,
                                    title = activeBottomTab.label,
                                    subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                    accent = accent
                                )
                            }
                        } else {
                            val structuredVoiceEntries = guide.voiceEntries.filter { entry ->
                                entry.lines.any { line -> line.trim().isNotBlank() }
                            }
                            val migratedVoiceEntries = buildGrowthTitleVoiceEntries(
                                guide.profileRowsForDisplay()
                                    .filter(::isGrowthTitleVoiceRow)
                            )
                            val voiceEntries = (structuredVoiceEntries + migratedVoiceEntries)
                                .distinctBy { entry ->
                                    listOf(
                                        entry.section.trim(),
                                        entry.title.trim(),
                                        entry.lineHeaders.joinToString("|") { it.trim() },
                                        entry.lines.joinToString("|") { it.trim() },
                                        entry.audioUrls.joinToString("|") { normalizeGuideUrl(it) },
                                        normalizeGuideUrl(entry.audioUrl)
                                    ).joinToString("|")
                                }
                            val voiceCvByLanguage = buildVoiceCvDisplayMap(guide)
                            val voiceLanguageHeaders = buildVoiceLanguageHeadersForDisplay(
                                headers = guide.voiceLanguageHeaders,
                                entries = voiceEntries
                            )
                            val dubbingHeaders = buildDubbingHeadersForVoiceCard(
                                headers = voiceLanguageHeaders,
                                entries = voiceEntries
                            )
                            val selectedDubbingHeader = dubbingHeaders.firstOrNull { header ->
                                header.equals(selectedVoiceLanguage.trim(), ignoreCase = true)
                            } ?: dubbingHeaders.firstOrNull().orEmpty()

                            if (voiceCvByLanguage.isNotEmpty()) {
                                item {
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
                                            voiceCvByLanguage.forEach { (label, value) ->
                                                val title = if (label.contains("配")) "$label CV" else label
                                                MiuixInfoItem(
                                                    key = title,
                                                    value = value,
                                                    onLongClick = rememberGuideCopyAction(
                                                        buildGuideCopyPayload(title, value)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (voiceEntries.isNotEmpty() && dubbingHeaders.isNotEmpty()) {
                                item {
                                    GuideVoiceLanguageCard(
                                        headers = dubbingHeaders,
                                        selectedHeader = selectedDubbingHeader,
                                        backdrop = backdrop,
                                        onSelected = onSelectedVoiceLanguageChange
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (!error.isNullOrBlank()) {
                                item {
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
                                                text = error.orEmpty(),
                                                color = MiuixTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (voiceEntries.isNotEmpty()) {
                                voiceEntries.forEachIndexed { index, entry ->
                                    val playbackUrl = resolveVoicePlaybackUrl(
                                        entry = entry,
                                        headers = voiceLanguageHeaders,
                                        selectedHeader = selectedDubbingHeader
                                    )
                                    val directPlaybackUrl = normalizeGuidePlaybackSource(playbackUrl)
                                    val resolvedCachedPlaybackUrl = galleryCacheRevision.let {
                                        BaGuideTempMediaCache.resolveCachedUrl(
                                            context = context,
                                            sourceUrl = sourceUrl,
                                            rawUrl = directPlaybackUrl
                                        )
                                    }
                                    val normalizedPlaybackUrl = normalizeGuidePlaybackSource(resolvedCachedPlaybackUrl)
                                    val isCurrentPlayback = normalizedPlaybackUrl.isNotBlank() &&
                                        isVoicePlaying &&
                                        (normalizedPlaybackUrl == playingVoiceUrl || directPlaybackUrl == playingVoiceUrl)
                                    item {
                                        GuideVoiceEntryCard(
                                            entry = entry,
                                            languageHeaders = voiceLanguageHeaders,
                                            backdrop = backdrop,
                                            playbackUrl = normalizedPlaybackUrl,
                                            isPlaying = isCurrentPlayback,
                                            playProgress = if (isCurrentPlayback) {
                                                voicePlayProgress
                                            } else {
                                                0f
                                            },
                                            onTogglePlay = onToggleVoicePlayback
                                        )
                                    }
                                    if (index < voiceEntries.lastIndex) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                }
                            } else {
                                item {
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
                                                text = "暂未解析到结构化语音台词，点击右上角刷新后重试。",
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GuideBottomTab.Gallery -> {
                        val guide = info
                        if (guide == null) {
                            item {
                                FrostedBlock(
                                    backdrop = backdrop,
                                    title = activeBottomTab.label,
                                    subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                    accent = accent
                                )
                            }
                        } else {
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
                            // 影画条目若没有可用封面图，则不渲染对应卡片，避免出现空壳卡片。
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
                            val firstExpressionIndex = displayGalleryItems.indexOfFirst(::isExpressionGalleryItem)
                            val firstMemoryHallIndex = displayGalleryItems.indexOfFirst(::isMemoryHallGalleryItem)
                            val lastOfficialIntroIndex = displayGalleryItems.indexOfLast(::isOfficialIntroGalleryItem)

                            if (!error.isNullOrBlank()) {
                                item {
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
                                                text = error.orEmpty(),
                                                color = MiuixTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (displayGalleryItems.isNotEmpty() || previewVideoGroups.isNotEmpty()) {
                                var renderedCount = 0
                                var insertedUnlockLevel = false
                                var insertedMemoryHallVideoNearGallery = false
                                var insertedPvRoleAfterOfficial = false
                                var insertedGalleryRelatedLinks = false
                                displayGalleryItems.forEachIndexed { index, item ->
                                    val isExpression = isExpressionGalleryItem(item)
                                    if (isExpression && index != firstExpressionIndex) {
                                        return@forEachIndexed
                                    }
                                    if (renderedCount > 0) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }

                                    // 将“回忆大厅解锁等级”展示在立绘组和回忆大厅之间。
                                    if (!insertedUnlockLevel &&
                                        memoryUnlockLevel.isNotBlank() &&
                                        index == firstMemoryHallIndex
                                    ) {
                                        item {
                                            GuideGalleryUnlockLevelCardItem(
                                                level = memoryUnlockLevel,
                                                backdrop = backdrop
                                            )
                                        }
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                        insertedUnlockLevel = true
                                    }

                                    item {
                                        if (isExpression && expressionItems.isNotEmpty()) {
                                            GuideGalleryExpressionCardItem(
                                                title = "角色表情",
                                                items = expressionItems,
                                                backdrop = backdrop,
                                                onOpenMedia = onOpenExternal,
                                                onSaveMedia = onSaveMedia,
                                                mediaUrlResolver = { raw ->
                                                    galleryCacheRevision.let {
                                                        BaGuideTempMediaCache.resolveCachedUrl(
                                                            context = context,
                                                            sourceUrl = sourceUrl,
                                                            rawUrl = raw
                                                        )
                                                    }
                                                }
                                            )
                                        } else {
                                            GuideGalleryCardItem(
                                                item = item,
                                                backdrop = backdrop,
                                                onOpenMedia = onOpenExternal,
                                                onSaveMedia = onSaveMedia,
                                                audioLoopScopeKey = sourceUrl,
                                                mediaUrlResolver = { raw ->
                                                    galleryCacheRevision.let {
                                                        BaGuideTempMediaCache.resolveCachedUrl(
                                                            context = context,
                                                            sourceUrl = sourceUrl,
                                                            rawUrl = raw
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    renderedCount += 1

                                    if (!insertedPvRoleAfterOfficial &&
                                        pvAndRoleVideoGroups.isNotEmpty() &&
                                        index == lastOfficialIntroIndex
                                    ) {
                                        pvAndRoleVideoGroups.forEach { (title, items) ->
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
                                                    mediaUrlResolver = { raw ->
                                                        galleryCacheRevision.let {
                                                            BaGuideTempMediaCache.resolveCachedUrl(
                                                                context = context,
                                                                sourceUrl = sourceUrl,
                                                                rawUrl = raw
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                            renderedCount += 1
                                        }
                                        insertedPvRoleAfterOfficial = true

                                        if (!insertedGalleryRelatedLinks && galleryRelatedLinkRows.isNotEmpty()) {
                                            if (renderedCount > 0) {
                                                item { Spacer(modifier = Modifier.height(10.dp)) }
                                            }
                                            item {
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
                                                            rows = galleryRelatedLinkRows,
                                                            onOpenExternal = onOpenExternal
                                                        )
                                                    }
                                                }
                                            }
                                            renderedCount += 1
                                            insertedGalleryRelatedLinks = true
                                        }
                                    }

                                    // 将“回忆大厅视频”紧贴“回忆大厅”条目展示。
                                    if (!insertedMemoryHallVideoNearGallery &&
                                        memoryHallVideoGroup != null &&
                                        index == firstMemoryHallIndex
                                    ) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                        item {
                                            GuideGalleryVideoGroupCardItem(
                                                title = memoryHallVideoGroup.first,
                                                items = memoryHallVideoGroup.second,
                                                previewFallbackUrl = memoryHallPreview,
                                                backdrop = backdrop,
                                                onOpenMedia = onOpenExternal,
                                                onSaveMedia = onSaveMedia,
                                                mediaUrlResolver = { raw ->
                                                    galleryCacheRevision.let {
                                                        BaGuideTempMediaCache.resolveCachedUrl(
                                                            context = context,
                                                            sourceUrl = sourceUrl,
                                                            rawUrl = raw
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        renderedCount += 1
                                        insertedMemoryHallVideoNearGallery = true
                                    }
                                }

                                if (!insertedUnlockLevel &&
                                    memoryUnlockLevel.isNotBlank() &&
                                    memoryHallVideoGroup != null
                                ) {
                                    if (renderedCount > 0) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                    item {
                                        GuideGalleryUnlockLevelCardItem(
                                            level = memoryUnlockLevel,
                                            backdrop = backdrop
                                        )
                                    }
                                    insertedUnlockLevel = true
                                    renderedCount += 1
                                }

                                if (!insertedMemoryHallVideoNearGallery && memoryHallVideoGroup != null) {
                                    if (renderedCount > 0) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                    item {
                                        GuideGalleryVideoGroupCardItem(
                                            title = memoryHallVideoGroup.first,
                                            items = memoryHallVideoGroup.second,
                                            previewFallbackUrl = memoryHallPreview,
                                            backdrop = backdrop,
                                            onOpenMedia = onOpenExternal,
                                            onSaveMedia = onSaveMedia,
                                            mediaUrlResolver = { raw ->
                                                galleryCacheRevision.let {
                                                    BaGuideTempMediaCache.resolveCachedUrl(
                                                        context = context,
                                                        sourceUrl = sourceUrl,
                                                        rawUrl = raw
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    renderedCount += 1
                                }

                                if (!insertedPvRoleAfterOfficial) {
                                    pvAndRoleVideoGroups.forEach { (title, items) ->
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
                                                mediaUrlResolver = { raw ->
                                                    galleryCacheRevision.let {
                                                        BaGuideTempMediaCache.resolveCachedUrl(
                                                            context = context,
                                                            sourceUrl = sourceUrl,
                                                            rawUrl = raw
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        renderedCount += 1
                                    }
                                    insertedPvRoleAfterOfficial = pvAndRoleVideoGroups.isNotEmpty()
                                    if (!insertedGalleryRelatedLinks && galleryRelatedLinkRows.isNotEmpty()) {
                                        if (renderedCount > 0) {
                                            item { Spacer(modifier = Modifier.height(10.dp)) }
                                        }
                                        item {
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
                                                        rows = galleryRelatedLinkRows,
                                                        onOpenExternal = onOpenExternal
                                                    )
                                                }
                                            }
                                        }
                                        renderedCount += 1
                                        insertedGalleryRelatedLinks = true
                                    }
                                }

                                otherTrailingVideoGroups.forEach { (title, items) ->
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
                                            mediaUrlResolver = { raw ->
                                                galleryCacheRevision.let {
                                                    BaGuideTempMediaCache.resolveCachedUrl(
                                                        context = context,
                                                        sourceUrl = sourceUrl,
                                                        rawUrl = raw
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    renderedCount += 1
                                }

                                if (!insertedGalleryRelatedLinks && galleryRelatedLinkRows.isNotEmpty()) {
                                    if (renderedCount > 0) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                    item {
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
                                                    rows = galleryRelatedLinkRows,
                                                    onOpenExternal = onOpenExternal
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
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
                            }
                        }
                    }

                    GuideBottomTab.Simulate -> {
                        val guide = info
                        if (guide == null) {
                            item {
                                FrostedBlock(
                                    backdrop = backdrop,
                                    title = activeBottomTab.label,
                                    subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                    accent = accent
                                )
                            }
                        } else {
                            val simulateRows = guide.simulateRowsForDisplay()
                            val simulateData = buildGuideSimulateData(simulateRows)

                            if (!error.isNullOrBlank()) {
                                item {
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
                                                text = error.orEmpty(),
                                                color = MiuixTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            val hasAnySectionData = simulateData.initialRows.isNotEmpty() ||
                                simulateData.maxRows.isNotEmpty() ||
                                simulateData.weaponRows.isNotEmpty() ||
                                simulateData.equipmentRows.isNotEmpty() ||
                                simulateData.favorRows.isNotEmpty() ||
                                simulateData.unlockRows.isNotEmpty() ||
                                simulateData.bondRows.isNotEmpty()

                            if (hasAnySectionData) {
                                item {
                                    GuideSimulateAbilityCard(
                                        data = simulateData,
                                        backdrop = backdrop
                                    )
                                }

                                val sectionCards = listOf(
                                    Triple("专武", simulateData.weaponRows, simulateData.weaponHint),
                                    Triple("装备", simulateData.equipmentRows, simulateData.equipmentHint),
                                    Triple("爱用品", simulateData.favorRows, simulateData.favorHint),
                                    Triple("能力解放", simulateData.unlockRows, simulateData.unlockHint),
                                    Triple("羁绊等级奖励", simulateData.bondRows, simulateData.bondHint)
                                )

                                sectionCards.forEach { (title, rows, hint) ->
                                    item { Spacer(modifier = Modifier.height(10.dp)) }
                                    item {
                                        when (title) {
                                            "专武" -> GuideSimulateWeaponCard(
                                                title = title,
                                                rows = rows,
                                                hint = hint,
                                                backdrop = backdrop
                                            )

                                            "装备" -> GuideSimulateEquipmentCard(
                                                title = title,
                                                rows = rows,
                                                hint = hint,
                                                backdrop = backdrop
                                            )

                                            "能力解放" -> GuideSimulateUnlockCard(
                                                title = title,
                                                rows = rows,
                                                hint = hint,
                                                backdrop = backdrop
                                            )

                                            "羁绊等级奖励" -> GuideSimulateBondCard(
                                                title = title,
                                                rows = rows,
                                                hint = hint,
                                                backdrop = backdrop
                                            )

                                            else -> GuideSimulateSectionCard(
                                                title = title,
                                                rows = rows,
                                                hint = hint,
                                                backdrop = backdrop
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
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
                                                text = "暂未解析到养成模拟数据，点击右上角刷新后重试。",
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
}

private data class GuideSimulateData(
    val initialHint: String = "",
    val initialRows: List<BaGuideRow> = emptyList(),
    val maxHint: String = "",
    val maxRows: List<BaGuideRow> = emptyList(),
    val weaponHint: String = "",
    val weaponRows: List<BaGuideRow> = emptyList(),
    val equipmentHint: String = "",
    val equipmentRows: List<BaGuideRow> = emptyList(),
    val favorHint: String = "",
    val favorRows: List<BaGuideRow> = emptyList(),
    val unlockHint: String = "",
    val unlockRows: List<BaGuideRow> = emptyList(),
    val bondHint: String = "",
    val bondRows: List<BaGuideRow> = emptyList()
)

private fun sanitizeSimulateFavorRows(rows: List<BaGuideRow>): List<BaGuideRow> {
    if (rows.isEmpty()) return emptyList()
    return rows
        .filterNot { row ->
            row.key.isBlank() &&
                row.value.isBlank() &&
                row.imageUrl.isBlank() &&
                row.imageUrls.isEmpty()
        }
        .filterNot { row ->
            val normalizedKey = normalizeProfileFieldKey(row.key)
            val hasMedia = row.imageUrl.isNotBlank() || row.imageUrls.isNotEmpty()
            val isTierMetaKey = Regex(
                """^t\d+(效果|所需升级材料|技能图标)$""",
                RegexOption.IGNORE_CASE
            ).matches(normalizedKey)
            isTierMetaKey && row.value.isBlank() && !hasMedia
        }
        .filterNot { row ->
            val normalizedKey = normalizeProfileFieldKey(row.key)
            val normalizedValue = normalizeProfileFieldKey(row.value)
            val hasMedia = row.imageUrl.isNotBlank() || row.imageUrls.isNotEmpty()
            val hasNumericValue = Regex("""\d""").containsMatchIn(row.value)
            val isBrokenStatPair =
                isLikelySimulateStatLabel(row.key) &&
                    isLikelySimulateStatLabel(row.value) &&
                    !hasNumericValue &&
                    !hasMedia
            val isTierLabel = Regex("""^T\d+$""", RegexOption.IGNORE_CASE).matches(normalizedKey)
            val isTierOnlyPlaceholder =
                isTierLabel &&
                    normalizedValue.isBlank() &&
                    !hasMedia
            isBrokenStatPair || isTierOnlyPlaceholder
        }
        .distinctBy { row ->
            val packedImages = row.imageUrls.joinToString("|")
            "${normalizeProfileFieldKey(row.key)}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
        }
}

private fun sanitizeSimulateBondRows(rows: List<BaGuideRow>): List<BaGuideRow> {
    if (rows.isEmpty()) return emptyList()
    return rows.filterNot { row ->
        val normalizedKey = normalizeProfileFieldKey(row.key)
        Regex("""^羁绊角色\d+$""").matches(normalizedKey) &&
            row.value.isBlank() &&
            row.imageUrl.isBlank() &&
            row.imageUrls.isEmpty()
    }
}

@Composable
private fun GuideSimulateAbilityCard(
    data: GuideSimulateData,
    backdrop: LayerBackdrop
) {
    var selectedAbility by rememberSaveable(
        data.initialRows.size,
        data.maxRows.size
    ) { mutableStateOf("最大培养") }
    val selectedRows = if (selectedAbility == "最大培养") data.maxRows else data.initialRows
    val selectedHint = if (selectedAbility == "最大培养") data.maxHint else data.initialHint
    val initialValueByKey = remember(data.initialRows) {
        linkedMapOf<String, String>().apply {
            data.initialRows.forEach { row ->
                val key = normalizeProfileFieldKey(row.key)
                if (key.isBlank() || row.value.isBlank()) return@forEach
                putIfAbsent(key, row.value.trim())
            }
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "角色能力",
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
                listOf("初始能力", "最大培养").forEach { option ->
                    val selected = selectedAbility == option
                    GlassTextButton(
                        backdrop = backdrop,
                        text = option,
                        textColor = if (selected) Color(0xFF2563EB) else MiuixTheme.colorScheme.onBackgroundVariant,
                        containerColor = if (selected) Color(0x443B82F6) else null,
                        variant = GlassVariant.Compact,
                        onClick = { selectedAbility = option }
                    )
                }
            }
            selectedHint.takeIf { it.isNotBlank() }?.let { hint ->
                Text(
                    text = hint.trim('*').trim(),
                    color = Color(0xFF60A5FA),
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.guideCopyable(
                        buildGuideCopyPayload("角色能力说明", hint)
                    )
                )
            }
            if (selectedRows.isNotEmpty()) {
                selectedRows.forEach { row ->
                    val deltaText = if (selectedAbility == "最大培养") {
                        buildSimulateMaxDeltaText(
                            maxValue = row.value,
                            initialValue = initialValueByKey[normalizeProfileFieldKey(row.key)]
                        )
                    } else {
                        ""
                    }
                    GuideSimulateRowItem(
                        row = row,
                        backdrop = backdrop,
                        valueDelta = deltaText
                    )
                }
            } else {
                Text(
                    text = "暂无能力数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

@Composable
private fun GuideSimulateSectionCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val levelRowIndex = if (title == "能力解放") {
        rows.indexOfFirst { row ->
            Regex("""^\d+级$""").matches(normalizeProfileFieldKey(row.key))
        }
    } else {
        -1
    }
    val levelCapsule = when {
        levelRowIndex >= 0 -> rows[levelRowIndex].key.trim()
        else -> extractSimulateLevelCapsule(hint)
    }
    val displayRows = if (levelRowIndex >= 0) {
        rows.filterIndexed { index, _ -> index != levelRowIndex }
    } else {
        rows
    }
    val displayCapsule = if (title == "爱用品" && displayRows.isEmpty()) {
        ""
    } else {
        levelCapsule
    }

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
            GuideSimulateCardTitleRow(
                title = title,
                capsule = displayCapsule,
                backdrop = backdrop
            )
            if (displayRows.isNotEmpty()) {
                displayRows.forEach { row ->
                    GuideSimulateRowItem(
                        row = row,
                        backdrop = backdrop
                    )
                }
            } else {
                Text(
                    text = if (title == "爱用品") "此学生暂未佩戴爱用品。" else "暂无${title}数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

private data class SimulateEquipmentGroup(
    val slotLabel: String,
    val itemName: String,
    val tierText: String,
    val iconUrl: String,
    val statRows: List<BaGuideRow>
)

private fun parseSimulateEquipmentGhostMediaUrl(row: BaGuideRow): String {
    val key = row.key.trim()
    if (key.isBlank()) return ""
    if (row.value.trim().isNotBlank()) return ""
    val looksLikeUrl = key.startsWith("//") ||
        key.startsWith("http://", ignoreCase = true) ||
        key.startsWith("https://", ignoreCase = true)
    if (!looksLikeUrl) return ""
    val isMediaUrl = Regex(
        """(?i)\.(png|jpe?g|webp|gif|bmp|mp4|webm|mkv|mov|mp3|ogg|wav|m4a)(\?.*)?$"""
    ).containsMatchIn(key)
    if (!isMediaUrl) return ""
    return normalizeGuideUrl(key)
}

private fun isSimulateEquipmentGhostMediaRow(row: BaGuideRow): Boolean {
    return parseSimulateEquipmentGhostMediaUrl(row).isNotBlank()
}

private fun buildSimulateEquipmentGroups(rows: List<BaGuideRow>): List<SimulateEquipmentGroup> {
    if (rows.isEmpty()) return emptyList()

    val groups = mutableListOf<SimulateEquipmentGroup>()
    var currentSlot = ""
    var currentItemName = ""
    var currentTierText = ""
    var currentIcon = ""
    val currentStats = mutableListOf<BaGuideRow>()

    fun commitGroup() {
        if (currentSlot.isBlank() && currentItemName.isBlank() && currentStats.isEmpty()) return
        groups += SimulateEquipmentGroup(
            slotLabel = currentSlot.ifBlank { "装备" },
            itemName = currentItemName,
            tierText = currentTierText,
            iconUrl = currentIcon,
            statRows = currentStats.toList()
        )
        currentSlot = ""
        currentItemName = ""
        currentTierText = ""
        currentIcon = ""
        currentStats.clear()
    }

    rows.forEach { row ->
        val key = row.key.trim()
        val normalizedKey = normalizeProfileFieldKey(key)
        val rowIcon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }

        if (Regex("""^\d+号装备$""").matches(normalizedKey)) {
            commitGroup()
            currentSlot = key
            if (rowIcon.isNotBlank()) {
                currentIcon = rowIcon
            }
            return@forEach
        }

        val ghostMediaUrl = parseSimulateEquipmentGhostMediaUrl(row)
        if (ghostMediaUrl.isNotBlank()) {
            currentIcon = ghostMediaUrl
            return@forEach
        }

        val isMetaRow = !isLikelySimulateStatLabel(key) && !isSimulateSubHeader(key)
        if (currentItemName.isBlank() && isMetaRow) {
            currentItemName = key
            currentTierText = row.value.trim()
            if (currentIcon.isBlank() && rowIcon.isNotBlank()) {
                currentIcon = rowIcon
            }
            return@forEach
        }

        if (key.isBlank() && row.value.isBlank()) return@forEach
        currentStats += row.copy(imageUrl = "", imageUrls = emptyList())
    }
    commitGroup()

    return groups
}

@Composable
private fun GuideSimulateEquipmentCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val groups = buildSimulateEquipmentGroups(rows)
    val hintCapsule = extractSimulateLevelCapsule(hint)

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
            GuideSimulateCardTitleRow(
                title = title,
                capsule = hintCapsule,
                backdrop = backdrop
            )

            if (groups.isNotEmpty()) {
                groups.forEach { group ->
                    val groupCopyPayload = buildGuideCopyPayload(
                        group.slotLabel.ifBlank { "装备" },
                        listOf(group.itemName.trim(), group.tierText.trim())
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "-" }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .guideCopyable(groupCopyPayload),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(0.52f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (group.iconUrl.isNotBlank()) {
                                GuideRemoteIcon(
                                    imageUrl = group.iconUrl,
                                    iconWidth = 40.dp,
                                    iconHeight = 40.dp
                                )
                            }
                            group.itemName.takeIf { it.isNotBlank() }?.let { name ->
                                Text(
                                    text = name,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.weight(0.48f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GuideSimulateInlineCapsule(
                                text = group.slotLabel,
                                backdrop = backdrop
                            )
                        }
                    }

                    group.statRows.forEach { row ->
                        GuideSimulateRowItem(
                            row = row,
                            backdrop = backdrop
                        )
                    }
                }
            } else if (rows.isNotEmpty()) {
                rows.forEach { row ->
                    GuideSimulateRowItem(
                        row = row,
                        backdrop = backdrop
                    )
                }
            } else {
                Text(
                    text = "暂无装备数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

private data class SimulateBondGroup(
    val roleLabel: String,
    val iconUrl: String,
    val statRows: List<BaGuideRow>
)

private data class SimulateUnlockViewData(
    val levelCapsule: String,
    val rows: List<BaGuideRow>
)

private fun buildSimulateUnlockViewData(
    rows: List<BaGuideRow>,
    hint: String
): SimulateUnlockViewData {
    if (rows.isEmpty()) {
        return SimulateUnlockViewData(
            levelCapsule = extractSimulateLevelCapsule(hint),
            rows = emptyList()
        )
    }

    val levelRowIndex = rows.indexOfFirst { row ->
        Regex("""^\d+级$""").matches(normalizeProfileFieldKey(row.key))
    }
    val capsule = when {
        levelRowIndex >= 0 -> rows[levelRowIndex].key.trim()
        else -> extractSimulateLevelCapsule(hint)
    }
    val contentRows = if (levelRowIndex >= 0) {
        rows.filterIndexed { index, _ -> index != levelRowIndex }
    } else {
        rows
    }
    return SimulateUnlockViewData(
        levelCapsule = capsule,
        rows = contentRows
    )
}

@Composable
private fun GuideSimulateUnlockCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val viewData = buildSimulateUnlockViewData(rows, hint)

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
            GuideSimulateCardTitleRow(
                title = title,
                capsule = viewData.levelCapsule,
                backdrop = backdrop
            )

            if (viewData.rows.isNotEmpty()) {
                viewData.rows.forEach { row ->
                    val iconUrl = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
                    if (iconUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GuideRemoteIcon(
                                imageUrl = iconUrl,
                                iconWidth = 78.dp,
                                iconHeight = 62.dp
                            )
                            GuideSimulateRowItem(
                                row = row.copy(imageUrl = "", imageUrls = emptyList()),
                                backdrop = backdrop
                            )
                        }
                    } else {
                        GuideSimulateRowItem(
                            row = row,
                            backdrop = backdrop
                        )
                    }
                }
            } else {
                Text(
                    text = "暂无能力解放数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

private fun buildSimulateBondGroups(rows: List<BaGuideRow>): List<SimulateBondGroup> {
    if (rows.isEmpty()) return emptyList()

    val groups = mutableListOf<SimulateBondGroup>()
    var currentRole = ""
    var currentIcon = ""
    val currentRows = mutableListOf<BaGuideRow>()

    fun commitGroup() {
        if (currentRole.isBlank() && currentRows.isEmpty()) return
        groups += SimulateBondGroup(
            roleLabel = currentRole.ifBlank { "羁绊角色" },
            iconUrl = currentIcon,
            statRows = currentRows.toList()
        )
        currentRole = ""
        currentIcon = ""
        currentRows.clear()
    }

    rows.forEach { row ->
        val key = row.key.trim()
        val normalizedKey = normalizeProfileFieldKey(key)
        val rowIcon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }

        if (Regex("""^羁绊角色\d+$""").matches(normalizedKey)) {
            commitGroup()
            currentRole = key
            currentIcon = rowIcon
            return@forEach
        }

        if (key.isBlank() && row.value.isBlank()) return@forEach
        currentRows += row.copy(
            imageUrl = "",
            imageUrls = emptyList()
        )
    }
    commitGroup()

    return groups
}

@Composable
private fun GuideSimulateBondCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val groups = buildSimulateBondGroups(rows)
    val levelCapsule = extractSimulateLevelCapsule(hint)

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
            GuideSimulateCardTitleRow(
                title = title,
                capsule = levelCapsule,
                backdrop = backdrop
            )

            if (groups.isNotEmpty()) {
                groups.forEach { group ->
                    val groupCopyPayload = buildGuideCopyPayload(
                        "羁绊角色",
                        group.roleLabel.ifBlank { "羁绊角色" }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .guideCopyable(groupCopyPayload),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (group.iconUrl.isNotBlank()) {
                            Box(modifier = Modifier.width(112.dp)) {
                                GuideRemoteImage(
                                    imageUrl = group.iconUrl,
                                    imageHeight = 86.dp
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = group.roleLabel,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            group.statRows.forEach { stat ->
                                GuideSimulateRowItem(
                                    row = stat,
                                    backdrop = backdrop
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "暂无羁绊等级奖励数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

private data class SimulateWeaponViewData(
    val imageUrl: String,
    val statRows: List<BaGuideRow>
)

private fun buildSimulateWeaponViewData(rows: List<BaGuideRow>): SimulateWeaponViewData {
    if (rows.isEmpty()) return SimulateWeaponViewData("", emptyList())
    val imageUrl = rows.firstNotNullOfOrNull { row ->
        row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }.takeIf { it.isNotBlank() }
    }.orEmpty()
    val statRows = rows
        .filter { row ->
            val key = row.key.trim()
            val value = row.value.trim()
            key.isNotBlank() || value.isNotBlank()
        }
        .map { row ->
            val rowIcon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
            if (imageUrl.isNotBlank() && rowIcon == imageUrl) {
                row.copy(imageUrl = "", imageUrls = emptyList())
            } else {
                row
            }
        }
    return SimulateWeaponViewData(
        imageUrl = imageUrl,
        statRows = statRows
    )
}

@Composable
private fun GuideSimulateWeaponCard(
    title: String,
    rows: List<BaGuideRow>,
    hint: String,
    backdrop: LayerBackdrop
) {
    val viewData = buildSimulateWeaponViewData(rows)
    val levelCapsule = extractSimulateLevelCapsule(hint)

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
            GuideSimulateCardTitleRow(
                title = title,
                capsule = levelCapsule,
                backdrop = backdrop
            )

            if (viewData.imageUrl.isNotBlank() || viewData.statRows.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (viewData.imageUrl.isNotBlank()) {
                        Box(modifier = Modifier.width(112.dp)) {
                            GuideRemoteImage(
                                imageUrl = viewData.imageUrl,
                                imageHeight = 72.dp
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewData.statRows.forEach { row ->
                            GuideSimulateRowItem(
                                row = row,
                                backdrop = backdrop
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "暂无专武数据。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}

private fun extractSimulateLevelCapsule(rawHint: String): String {
    val hint = rawHint.trim().trim('*').trim()
    if (hint.isBlank()) return ""

    Regex("""(?i)Lv\s*\d+""")
        .find(hint)
        ?.value
        ?.replace(" ", "")
        ?.let { raw ->
            val digits = Regex("""\d+""").find(raw)?.value.orEmpty()
            if (digits.isNotBlank()) return "Lv$digits"
        }

    Regex("""(?i)T\d+""")
        .find(hint)
        ?.value
        ?.replace(" ", "")
        ?.uppercase()
        ?.let { return it }

    Regex("""\d+级""")
        .find(hint)
        ?.value
        ?.let { return it }

    return ""
}

@Composable
private fun GuideSimulateCardTitleRow(
    title: String,
    capsule: String,
    backdrop: LayerBackdrop
) {
    val copyPayload = remember(title, capsule) {
        buildGuideCopyPayload(title, capsule.ifBlank { "-" })
    }
    CopyModeSelectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .guideCopyable(copyPayload),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            capsule.takeIf { it.isNotBlank() }?.let { label ->
                GuideSimulateInlineCapsule(
                    text = label,
                    backdrop = backdrop
                )
            }
        }
    }
}

@Composable
private fun GuideSimulateInlineCapsule(
    text: String,
    backdrop: LayerBackdrop
) {
    GlassTextButton(
        backdrop = backdrop,
        text = text,
        enabled = false,
        textColor = Color(0xFF60A5FA),
        variant = GlassVariant.Compact,
        onClick = {}
    )
}

@Composable
private fun GuideSimulateRowItem(
    row: BaGuideRow,
    backdrop: LayerBackdrop,
    valueDelta: String = ""
) {
    val key = row.key.trim().ifBlank { "信息" }
    val value = row.value.trim()
    val rowCopyAction = rememberGuideCopyAction(buildGuideCopyPayload(key, value.ifBlank { "-" }))
    val iconUrl = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
    val statGlyph = simulateStatGlyphForKey(key)
    if (isSimulateSubHeader(key)) {
        CopyModeSelectionContainer {
            Row(
                modifier = Modifier.copyModeAwareRow(
                    copyPayload = buildGuideCopyPayload(key, value.ifBlank { "-" }),
                    onLongClick = rowCopyAction
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconUrl.isNotBlank()) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 24.dp,
                        iconHeight = 24.dp
                    )
                }
                GlassTextButton(
                    backdrop = backdrop,
                    text = key,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {}
                )
            }
        }
        return
    }

    val valueColor = when {
        value.contains("%") -> Color(0xFF5FA8FF)
        value.matches(Regex("""(?i)^T\d+.*$""")) -> Color(0xFF5FA8FF)
        value.matches(Regex("""(?i)^Lv\d+.*$""")) -> Color(0xFF5FA8FF)
        key.contains("COST", ignoreCase = true) -> Color(0xFF5FA8FF)
        else -> MiuixTheme.colorScheme.onBackground
    }

    CopyModeSelectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .copyModeAwareRow(
                    copyPayload = buildGuideCopyPayload(key, value.ifBlank { "-" }),
                    onLongClick = rowCopyAction
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(0.45f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconUrl.isNotBlank()) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 24.dp,
                        iconHeight = 24.dp
                    )
                } else if (statGlyph != null) {
                    Text(
                        text = statGlyph,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.width(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = key,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.weight(0.55f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { "-" },
                    color = valueColor,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                valueDelta.takeIf { it.isNotBlank() }?.let { delta ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = delta,
                        color = Color(0xFFE3B547),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

private fun isSimulateSubHeader(key: String): Boolean {
    val normalized = normalizeProfileFieldKey(key)
    if (Regex("""^\d+号装备$""").matches(normalized)) return true
    if (Regex("""^羁绊角色\d+$""").matches(normalized)) return true
    if (Regex("""^\d+级$""").matches(normalized)) return true
    return false
}

private fun simulateStatGlyphForKey(raw: String): String? {
    val key = normalizeProfileFieldKey(raw)
    return when (key) {
        normalizeProfileFieldKey("攻击力") -> "✢"
        normalizeProfileFieldKey("防御力") -> "⛨"
        normalizeProfileFieldKey("生命值") -> "♥"
        normalizeProfileFieldKey("治愈力") -> "✚"
        normalizeProfileFieldKey("命中值") -> "◎"
        normalizeProfileFieldKey("闪避值") -> "◌"
        normalizeProfileFieldKey("暴击值") -> "✶"
        normalizeProfileFieldKey("暴击伤害") -> "✹"
        normalizeProfileFieldKey("稳定值") -> "≋"
        normalizeProfileFieldKey("射程") -> "➚"
        normalizeProfileFieldKey("群控强化力") -> "⬆"
        normalizeProfileFieldKey("群控抵抗力") -> "⬡"
        normalizeProfileFieldKey("装弹数") -> "☰"
        normalizeProfileFieldKey("防御无视值") -> "⊘"
        normalizeProfileFieldKey("受恢复率") -> "⟳"
        normalizeProfileFieldKey("COST恢复力") -> "⌛"
        normalizeProfileFieldKey("暴击抵抗值") -> "⛯"
        normalizeProfileFieldKey("暴伤抵抗率"),
        normalizeProfileFieldKey("暴击伤害抵抗率") -> "✺"
        else -> null
    }
}

private fun buildSimulateMaxDeltaText(
    maxValue: String,
    initialValue: String?
): String {
    val maxText = maxValue.trim()
    val initialText = initialValue?.trim().orEmpty()
    if (maxText.isBlank() || initialText.isBlank()) return ""
    if (Regex("""\([+-]\d+(\.\d+)?\)""").containsMatchIn(maxText)) return ""

    val maxNumber = extractComparableNumber(maxText) ?: return ""
    val initialNumber = extractComparableNumber(initialText) ?: return ""
    val diff = maxNumber - initialNumber
    if (abs(diff) < 0.0001) return ""

    val sign = if (diff > 0) "+" else "-"
    val absDiff = abs(diff)
    val deltaText = if (abs(absDiff - absDiff.toLong().toDouble()) < 0.0001) {
        absDiff.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", absDiff).trimEnd('0').trimEnd('.')
    }
    return "($sign$deltaText)"
}

private fun extractComparableNumber(raw: String): Double? {
    val normalized = raw.replace(",", "").trim()
    val numberText = Regex("""-?\d+(\.\d+)?""")
        .find(normalized)
        ?.value
        .orEmpty()
    return numberText.toDoubleOrNull()
}

private fun buildGuideSimulateCacheKey(rows: List<BaGuideRow>): String {
    var hash = 17
    rows.forEach { row ->
        hash = 31 * hash + normalizeProfileFieldKey(row.key).hashCode()
        hash = 31 * hash + row.value.trim().hashCode()
        hash = 31 * hash + row.imageUrl.trim().hashCode()
        row.imageUrls.forEach { image ->
            hash = 31 * hash + image.trim().hashCode()
        }
    }
    return "${rows.size}|$hash"
}

private fun buildGuideSimulateData(rows: List<BaGuideRow>): GuideSimulateData {
    if (rows.isEmpty()) return GuideSimulateData()
    val cacheKey = buildGuideSimulateCacheKey(rows)
    synchronized(guideSimulateDataCache) {
        guideSimulateDataCache[cacheKey]?.let { return it }
    }
    val sections = linkedMapOf<String, MutableList<BaGuideRow>>()
    val hints = mutableMapOf<String, String>()
    var currentSection = ""

    rows.forEach { row ->
        val header = resolveSimulateSectionName(row.key)
        if (header != null) {
            currentSection = header
            sections.getOrPut(header) { mutableListOf() }
            val hint = row.value.trim().trim('*').trim()
            if (hint.isNotBlank()) {
                hints[header] = hint
            }
            return@forEach
        }
        if (currentSection.isBlank()) return@forEach

        val cleaned = row.copy(
            key = row.key.trim(),
            value = row.value.trim(),
            imageUrl = row.imageUrl.trim(),
            imageUrls = row.imageUrls.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        )
        if (
            cleaned.key.isBlank() &&
            cleaned.value.isBlank() &&
            cleaned.imageUrl.isBlank() &&
            cleaned.imageUrls.isEmpty()
        ) return@forEach
        sections.getOrPut(currentSection) { mutableListOf() } += cleaned
    }

    val computed = GuideSimulateData(
        initialHint = hints["初始数据"].orEmpty(),
        initialRows = expandSimulateRows(sections["初始数据"].orEmpty()),
        maxHint = hints["顶级数据"].orEmpty(),
        maxRows = expandSimulateRows(sections["顶级数据"].orEmpty()),
        weaponHint = hints["专武"].orEmpty(),
        weaponRows = expandSimulateRows(sections["专武"].orEmpty()),
        equipmentHint = hints["装备"].orEmpty(),
        equipmentRows = expandSimulateRows(sections["装备"].orEmpty()),
        favorHint = hints["爱用品"].orEmpty(),
        favorRows = sanitizeSimulateFavorRows(
            expandSimulateRows(sections["爱用品"].orEmpty())
        ),
        unlockHint = hints["能力解放"].orEmpty(),
        unlockRows = expandSimulateRows(sections["能力解放"].orEmpty()),
        bondHint = hints["羁绊等级奖励"].orEmpty(),
        bondRows = sanitizeSimulateBondRows(
            expandSimulateRows(sections["羁绊等级奖励"].orEmpty())
        )
    )
    synchronized(guideSimulateDataCache) {
        guideSimulateDataCache[cacheKey] = computed
    }
    return computed
}

private fun resolveSimulateSectionName(rawKey: String): String? {
    val normalized = normalizeProfileFieldKey(rawKey)
    return when {
        normalized == normalizeProfileFieldKey("初始数据") -> "初始数据"
        normalized == normalizeProfileFieldKey("顶级数据") -> "顶级数据"
        normalized == normalizeProfileFieldKey("专武") -> "专武"
        normalized == normalizeProfileFieldKey("装备") -> "装备"
        normalized == normalizeProfileFieldKey("爱用品") -> "爱用品"
        normalized == normalizeProfileFieldKey("能力解放") -> "能力解放"
        normalized == normalizeProfileFieldKey("羁绊等级奖励") -> "羁绊等级奖励"
        else -> null
    }
}

private fun isLikelySimulateStatLabel(raw: String): Boolean {
    val normalized = normalizeProfileFieldKey(raw)
    if (normalized in normalizedTopDataStatKeys) return true
    val extraStatKeys = setOf(
        "暴伤抵抗率",
        "暴击抵抗值",
        "暴伤抵抗值"
    ).map(::normalizeProfileFieldKey).toSet()
    if (normalized in extraStatKeys) return true
    return normalized.endsWith("值") || normalized.endsWith("率")
}

private fun expandSimulateRows(rows: List<BaGuideRow>): List<BaGuideRow> {
    if (rows.isEmpty()) return emptyList()
    val expanded = mutableListOf<BaGuideRow>()
    rows.forEach { row ->
        val key = row.key.trim()
        val value = row.value.trim()
        val icon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
        val images = row.imageUrls.ifEmpty { listOfNotNull(icon.takeIf { it.isNotBlank() }) }
        if (key.isBlank() && value.isBlank() && images.isEmpty()) return@forEach

        if (value.isBlank()) {
            if (key.isNotBlank() || icon.isNotBlank()) {
                expanded += BaGuideRow(
                    key = key.ifBlank { "信息" },
                    value = "",
                    imageUrl = icon,
                    imageUrls = images
                )
            }
            return@forEach
        }

        val tokens = splitGuideCompositeValues(value)
        if (tokens.isEmpty()) {
            expanded += BaGuideRow(
                key = key.ifBlank { "信息" },
                value = value,
                imageUrl = icon,
                imageUrls = images
            )
            return@forEach
        }

        val firstTokenLooksLikeStat = isLikelySimulateStatLabel(tokens.first())
        var index = 0
        if (!firstTokenLooksLikeStat) {
            expanded += BaGuideRow(
                key = key.ifBlank { "等级" },
                value = tokens.first().trim(),
                imageUrl = icon,
                imageUrls = images
            )
            index = 1
        } else if (key.isNotBlank() && !isLikelySimulateStatLabel(key) && !isSimulateSubHeader(key)) {
            expanded += BaGuideRow(
                key = key,
                value = "",
                imageUrl = icon,
                imageUrls = images
            )
        } else if (icon.isNotBlank() && key.isNotBlank()) {
            expanded += BaGuideRow(
                key = key,
                value = "",
                imageUrl = icon,
                imageUrls = images
            )
        }

        var pairIndex = 0
        while (index + 1 < tokens.size) {
            val statKey = tokens[index].trim()
            val statValue = tokens[index + 1].trim()
            if (statKey.isNotBlank() && statValue.isNotBlank()) {
                val pairIcon = if (images.size > 1) images.getOrNull(pairIndex).orEmpty() else ""
                expanded += BaGuideRow(
                    key = statKey,
                    value = statValue,
                    imageUrl = pairIcon,
                    imageUrls = listOfNotNull(pairIcon.takeIf { it.isNotBlank() })
                )
            }
            pairIndex += 1
            index += 2
        }
    }

    return expanded.distinctBy { row ->
        val packedImages = row.imageUrls.joinToString("|")
        "${normalizeProfileFieldKey(row.key)}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
    }
}

@Composable
private fun GuideProfileSectionHeader(
    title: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GuideProfileInfoRows(
    rows: List<BaGuideRow>,
    rowContent: @Composable (BaGuideRow) -> Unit
) {
    rows.forEach { row ->
        rowContent(row)
    }
}

@Composable
private fun GuideProfileInfoItem(
    key: String,
    value: String,
    onClick: (() -> Unit)? = null,
    valueColor: Color? = null,
    preferCapsule: Boolean = true
) {
    val displayKey = key.ifBlank { "信息" }
    val displayValue = value.ifBlank { "-" }
    val rowCopyAction = rememberGuideCopyAction(buildGuideCopyPayload(displayKey, displayValue))
    val showCapsule = preferCapsule && shouldUseProfileValueCapsule(displayKey, displayValue, onClick)
    CopyModeSelectionContainer {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .copyModeAwareRow(
                    copyPayload = buildGuideCopyPayload(displayKey, displayValue),
                    onClick = onClick,
                    onLongClick = rowCopyAction
                )
        ) {
            val keyMaxWidth = adaptiveProfileKeyMaxWidth(
                key = displayKey,
                value = displayValue,
                containerWidth = maxWidth
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = displayKey,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.widthIn(min = 52.dp, max = keyMaxWidth),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (showCapsule) {
                        GuideProfileValueCapsule(
                            label = displayValue,
                            tint = valueColor ?: Color(0xFF5FA8FF),
                            onClick = onClick,
                            onLongClick = rowCopyAction
                        )
                    } else {
                        Text(
                            text = displayValue,
                            color = valueColor ?: MiuixTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Medium,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideProfileValueCapsule(
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.copyModeAwareRow(
            copyPayload = buildGuideCopyPayload("", label),
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .clip(ContinuousCapsule)
            .then(clickModifier)
            .background(tint.copy(alpha = if (isDark) 0.20f else 0.16f))
            .border(
                width = 0.8.dp,
                color = tint.copy(alpha = if (isDark) 0.42f else 0.46f),
                shape = ContinuousCapsule
            )
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isDark) tint else tint.copy(alpha = 0.92f),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GuideProfileRowsSection(
    rows: List<BaGuideRow>,
    emptyText: String,
    imageHeight: androidx.compose.ui.unit.Dp = 96.dp
) {
    if (rows.isEmpty()) {
        Text(emptyText, color = MiuixTheme.colorScheme.onBackgroundVariant)
        return
    }
    val visibleRows = rows
        .take(120)
        .mapNotNull { row ->
            val cleanedValue = sanitizeProfileFieldValue(row.key, row.value)
            val isPlaceholderValue = isProfileValuePlaceholder(cleanedValue)
            val hasImage = row.imageUrl.isNotBlank() || row.imageUrls.any { it.isNotBlank() }
            val shouldDropRow =
                (isProfileInstructionPlaceholder(row.value) && isPlaceholderValue) ||
                    (isPlaceholderValue && !hasImage)
            if (shouldDropRow) {
                null
            } else {
                row.copy(value = cleanedValue)
            }
        }
    if (visibleRows.isEmpty()) {
        Text(emptyText, color = MiuixTheme.colorScheme.onBackgroundVariant)
        return
    }
    visibleRows.forEachIndexed { index, row ->
        val hasImage = row.imageUrl.isNotBlank()
        val value = row.value
            .takeIf { it.isNotBlank() && it != "图片" }
            ?: if (hasImage) "见下图" else "-"
        GuideProfileInfoItem(
            key = row.key.ifBlank { "信息" },
            value = value,
            preferCapsule = false
        )
        if (hasImage) {
            Spacer(modifier = Modifier.height(6.dp))
            GuideRemoteImage(
                imageUrl = row.imageUrl,
                imageHeight = imageHeight
            )
        }
        if (index < visibleRows.lastIndex) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun GuideGalleryRelatedLinkRows(
    rows: List<BaGuideRow>,
    onOpenExternal: (String) -> Unit
) {
    if (rows.isEmpty()) {
        Text(
            text = "暂无影画相关链接。",
            color = MiuixTheme.colorScheme.onBackgroundVariant
        )
        return
    }

    rows.forEachIndexed { index, row ->
        val links = extractGuideWebLinks(row.value)
        if (links.isEmpty()) return@forEachIndexed
        val noteText = stripGuideWebLinks(row.value)
        val keyText = row.key.ifBlank { "影画链接" }
        val rowCopyPayload = buildGuideCopyPayload(
            key = keyText,
            value = buildString {
                if (noteText.isNotBlank()) {
                    append(noteText)
                }
                if (links.isNotEmpty()) {
                    if (isNotEmpty()) append('\n')
                    append(links.joinToString("\n"))
                }
            }.ifBlank { "-" }
        )
        val rowCopyAction = rememberGuideCopyAction(rowCopyPayload)

        CopyModeSelectionContainer {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
            ) {
                val keyMaxWidth = adaptiveProfileKeyMaxWidth(
                    key = keyText,
                    value = links.first(),
                    containerWidth = maxWidth
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .copyModeAwareRow(
                            copyPayload = rowCopyPayload,
                            onLongClick = rowCopyAction
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = keyText,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.widthIn(min = 52.dp, max = keyMaxWidth),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (noteText.isNotBlank()) {
                            Text(
                                text = noteText,
                                color = MiuixTheme.colorScheme.onBackground,
                                textAlign = TextAlign.End,
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Clip
                            )
                        }
                        links.forEach { link ->
                            val linkCopyAction = rememberGuideCopyAction(buildGuideCopyPayload(keyText, link))
                            Text(
                                text = link,
                                color = Color(0xFF3B82F6),
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.copyModeAwareRow(
                                    copyPayload = buildGuideCopyPayload(keyText, link),
                                    onClick = { onOpenExternal(link) },
                                    onLongClick = linkCopyAction
                                )
                            )
                        }
                    }
                }
            }
        }
        if (index < rows.lastIndex) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideGiftPreferenceGrid(
    items: List<GiftPreferenceItem>
) {
    if (items.isEmpty()) {
        Text(
            text = "暂无礼物偏好条目。",
            color = MiuixTheme.colorScheme.onBackgroundVariant
        )
        return
    }
    val isDark = isSystemInDarkTheme()
    val horizontalSpacing = 4.dp
    val minCardWidth = 78.dp
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= (minCardWidth * 3 + horizontalSpacing * 2) -> 3
            maxWidth >= (minCardWidth * 2 + horizontalSpacing) -> 2
            else -> 1
        }
        val cardWidth = ((maxWidth - horizontalSpacing * (columns - 1)) / columns)
            .coerceAtLeast(72.dp)
        val giftBoxHeight = (cardWidth * 0.66f).coerceIn(56.dp, 76.dp)
        val giftIconWidth = (cardWidth + 4.dp).coerceIn(74.dp, 122.dp)
        val giftIconHeight = (giftBoxHeight + 2.dp).coerceAtLeast(48.dp)
        val emojiIconSize = (cardWidth * 0.16f).coerceIn(13.dp, 18.dp)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .width(cardWidth)
                        .guideCopyable(buildGuideCopyPayload("礼物", item.label)),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(giftBoxHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x163B82F6))
                            .border(
                                width = 0.8.dp,
                                color = Color(0x243B82F6),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        GuideRemoteIcon(
                            imageUrl = item.giftImageUrl,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-3).dp),
                            iconWidth = giftIconWidth,
                            iconHeight = giftIconHeight
                        )
                        if (item.emojiImageUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 3.dp, end = 3.dp)
                                    .clip(ContinuousCapsule)
                                    .background(
                                        if (isDark) Color(0x663B82F6) else Color(0xCCEFF6FF)
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = if (isDark) Color(0x553B82F6) else Color(0x553BA8FF),
                                        shape = ContinuousCapsule
                                    )
                                    .padding(horizontal = 3.dp, vertical = 3.dp)
                            ) {
                                GuideRemoteIcon(
                                    imageUrl = item.emojiImageUrl,
                                    iconWidth = emojiIconSize,
                                    iconHeight = emojiIconSize
                                )
                            }
                        }
                    }
                    Text(
                        text = item.label,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun extractOrderedNumbers(raw: String): List<Int> {
    if (raw.isBlank()) return emptyList()
    return Regex("""\d+""")
        .findAll(raw)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()
}

private fun sortKeyNumbers(raw: String): Pair<Int, Int> {
    val numbers = extractOrderedNumbers(raw)
    if (numbers.isEmpty()) return -1 to -1
    val first = numbers.getOrElse(0) { -1 }
    val second = numbers.getOrElse(1) { -1 }
    return first to second
}

private fun sortProfileRowsByKeyNumbers(rows: List<BaGuideRow>): List<BaGuideRow> {
    return rows.sortedWith(
        compareBy<BaGuideRow>(
            { sortKeyNumbers(it.key).first },
            { sortKeyNumbers(it.key).second },
            { normalizeProfileFieldKey(it.key) }
        )
    )
}

private fun sortGalleryItemsByTitleNumbers(items: List<BaGuideGalleryItem>): List<BaGuideGalleryItem> {
    return items.sortedWith(
        compareBy<BaGuideGalleryItem>(
            { sortKeyNumbers(normalizeGalleryTitle(it.title)).first },
            { sortKeyNumbers(normalizeGalleryTitle(it.title)).second },
            { normalizeGalleryTitle(it.title) }
        )
    )
}

private data class ProfileFieldSpec(
    val title: String,
    val aliases: List<String>,
    val hideWhenEmpty: Boolean = false
)

private data class SameNameRoleItem(
    val name: String,
    val linkUrl: String,
    val imageUrl: String
)

private data class GiftPreferenceItem(
    val label: String,
    val giftImageUrl: String,
    val emojiImageUrl: String
)

private val profileNicknameFieldSpecs = listOf(
    ProfileFieldSpec("角色名称", listOf("角色名称")),
    ProfileFieldSpec("全名", listOf("全名")),
    ProfileFieldSpec("假名注音", listOf("假名注音", "假名注明")),
    ProfileFieldSpec("繁中译名", listOf("繁中译名")),
    ProfileFieldSpec("简中译名", listOf("简中译名"))
)

private val profileStudentInfoFieldSpecs = listOf(
    ProfileFieldSpec("年龄", listOf("年龄")),
    ProfileFieldSpec("生日", listOf("生日")),
    ProfileFieldSpec("身高", listOf("身高")),
    ProfileFieldSpec("画师", listOf("画师", "原画师")),
    ProfileFieldSpec("实装日期", listOf("实装日期", "首次登场日期")),
    ProfileFieldSpec("声优", listOf("声优")),
    ProfileFieldSpec("角色考据", listOf("角色考据"), hideWhenEmpty = true),
    ProfileFieldSpec("设计", listOf("设计", "设计师"), hideWhenEmpty = true)
)

private val profileHobbyFieldSpecs = listOf(
    ProfileFieldSpec("兴趣爱好", listOf("兴趣爱好")),
    ProfileFieldSpec("个人简介", listOf("个人简介")),
    ProfileFieldSpec("MomoTalk状态消息", listOf("MomoTalk状态消息", "Momotalk状态消息")),
    ProfileFieldSpec("MomoTalk解锁等级", listOf("MomoTalk解锁等级", "Momotalk解锁等级"), hideWhenEmpty = true)
)

private val profileStructuredFieldSpecs = profileNicknameFieldSpecs + profileStudentInfoFieldSpecs + profileHobbyFieldSpecs

private fun normalizeProfileFieldKey(raw: String): String {
    return raw
        .replace(" ", "")
        .replace("　", "")
        .replace("（", "(")
        .replace("）", ")")
        .trim()
        .lowercase()
}

private val profileRoleReferenceFieldKey = normalizeProfileFieldKey("角色考据")
private val relatedSameNameRoleHeaderKey = normalizeProfileFieldKey("相关同名角色")
private val sameNameRoleNameRowKey = normalizeProfileFieldKey("同名角色名称")
private val giftPreferenceRowPrefixKey = normalizeProfileFieldKey("礼物偏好礼物")
private val profileSectionHeaderKeys = setOf("介绍", "学生信息", "信息")
    .map(::normalizeProfileFieldKey)
    .toSet()
private val profileCapsuleFieldKeys = setOf(
    "角色名称", "年龄", "生日", "身高",
    "实装日期", "MomoTalk解锁等级",
    "繁中译名", "简中译名", "假名注音", "假名注明"
).map(::normalizeProfileFieldKey).toSet()
private val profileLongTextFieldKeys = setOf(
    "全名", "个人简介", "兴趣爱好", "MomoTalk状态消息"
).map(::normalizeProfileFieldKey).toSet()
private val profileLinkTitleCache = ConcurrentHashMap<String, String>()

private fun shouldUseProfileValueCapsule(
    key: String,
    value: String,
    onClick: (() -> Unit)?
): Boolean {
    if (onClick != null) return false
    if (isProfileValuePlaceholder(value)) return false
    val normalizedKey = normalizeProfileFieldKey(key)
    if (value.length > 12 || value.contains('\n')) return false
    if (value.contains("http", ignoreCase = true)) return false
    if (value.contains("/") || value.contains(" / ")) return false
    if (value.contains("：") || value.contains(":")) return false
    if (normalizedKey in profileLongTextFieldKeys) return false
    if (normalizedKey in profileCapsuleFieldKeys) return true
    return value.length <= 8 && !value.contains(" ")
}

private fun adaptiveProfileKeyMaxWidth(
    key: String,
    value: String,
    containerWidth: Dp
): Dp {
    val keyLength = key.trim().length
    val baseWidth = when {
        keyLength >= 12 -> 148.dp
        keyLength >= 10 -> 138.dp
        keyLength >= 8 -> 124.dp
        keyLength >= 6 -> 110.dp
        else -> 94.dp
    }
    val valuePenalty = when {
        value.length >= 64 -> 24.dp
        value.length >= 40 -> 16.dp
        value.length >= 24 -> 8.dp
        else -> 0.dp
    }
    val preferred = (baseWidth - valuePenalty).coerceAtLeast(84.dp)
    val containerLimit = (containerWidth * 0.48f).coerceAtLeast(84.dp)
    return preferred.coerceAtMost(containerLimit)
}

private fun isSameNameRoleRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key == relatedSameNameRoleHeaderKey || key == sameNameRoleNameRowKey
}

private fun isGiftPreferenceProfileRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key.startsWith(giftPreferenceRowPrefixKey)
}

private fun buildGiftPreferenceItems(rows: List<BaGuideRow>): List<GiftPreferenceItem> {
    if (rows.isEmpty()) return emptyList()
    return rows.mapIndexedNotNull { index, row ->
        val normalizedImages = buildList {
            add(row.imageUrl.trim())
            addAll(row.imageUrls.map { it.trim() })
        }.filter { candidate ->
            isRenderableGalleryImageUrl(candidate)
        }.distinct()
        val giftImage = normalizedImages.firstOrNull().orEmpty()
        if (giftImage.isBlank()) return@mapIndexedNotNull null
        val emojiImage = normalizedImages.firstOrNull { candidate ->
            candidate != giftImage
        }.orEmpty()
        val fallbackIndex = extractOrderedNumbers(row.key).firstOrNull() ?: (index + 1)
        val label = row.value
            .trim()
            .takeIf { it.isNotBlank() && !isProfileValuePlaceholder(it) }
            ?: "礼物$fallbackIndex"
        GiftPreferenceItem(
            label = label,
            giftImageUrl = giftImage,
            emojiImageUrl = emojiImage
        )
    }.distinctBy { item ->
        "${item.giftImageUrl}|${item.emojiImageUrl}|${item.label.trim()}"
    }
}

private fun splitRoleRowTokens(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .split(Regex("""\s*(?:/|／|\||｜|\n)\s*"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private val sameNameGuideEmbeddedLinkRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
private val sameNameGuidePathPattern = Regex("""^/(?:ba/tj/\d+(?:\.html)?|ba/\d+(?:\.html)?|v1/content/detail/\d+)$""")

private fun sanitizeSameNameLinkToken(raw: String): String {
    return raw.trim().trimEnd(')', ']', '}', ',', '。', '，', ';', '；')
}

private fun extractSameNameGuideLink(raw: String): String {
    val source = raw.trim()
    if (source.isBlank()) return ""

    val directCandidates = buildList {
        val cleaned = sanitizeSameNameLinkToken(source)
        if (cleaned.startsWith("http://", ignoreCase = true) || cleaned.startsWith("https://", ignoreCase = true)) {
            add(cleaned)
        } else if (cleaned.startsWith("www.", ignoreCase = true)) {
            add("https://$cleaned")
        } else if (cleaned.matches(Regex("""^\d{4,}$"""))) {
            add("https://www.gamekee.com/ba/tj/$cleaned.html")
        } else if (cleaned.startsWith("/") && sameNameGuidePathPattern.matches(cleaned)) {
            add(normalizeGuideUrl(cleaned))
        }
        addAll(
            sameNameGuideEmbeddedLinkRegex.findAll(source).map { match ->
                sanitizeSameNameLinkToken(match.value)
            }
        )
    }.distinct()

    for (candidate in directCandidates) {
        val normalized = normalizeGuideUrl(candidate)
        if (normalized.isBlank()) continue
        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: continue
        val host = uri.host.orEmpty().lowercase()
        val path = uri.path.orEmpty()
        val hostAccepted = host == "www.gamekee.com" || host == "gamekee.com"
        val pathAccepted = sameNameGuidePathPattern.matches(path)
        if (!hostAccepted || !pathAccepted) continue
        val contentId = extractGuideContentIdFromUrl(normalized) ?: continue
        if (contentId <= 0L) continue
        return "https://www.gamekee.com/ba/tj/$contentId.html"
    }
    return ""
}

private val sameNameRoleHintKeywords = listOf(
    "暂无同名角色",
    "未填写",
    "占位",
    "说明",
    "备注",
    "复制",
    "不用写",
    "暂时没",
    "待补充"
)

private fun isSameNameRoleHintText(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    val compact = value
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
    if (compact.length >= 20) return true
    return sameNameRoleHintKeywords.any { keyword ->
        compact.contains(keyword.lowercase())
    }
}

private fun extractSameNameRoleHint(row: BaGuideRow): String? {
    if (normalizeProfileFieldKey(row.key) != relatedSameNameRoleHeaderKey) return null
    val rawValue = row.value.trim().trim('*')
    if (rawValue.isBlank()) return null
    if (isProfileValuePlaceholder(rawValue)) return null
    val hasLink = extractProfileExternalLink(rawValue).isNotBlank()
    val hasImage = buildList {
        add(row.imageUrl.trim())
        addAll(row.imageUrls.map { it.trim() })
    }.any { candidate ->
        candidate.isNotBlank() && isRenderableGalleryImageUrl(candidate)
    }
    if (hasLink || hasImage) return null
    if (!isSameNameRoleHintText(rawValue)) return null
    return rawValue
}

private fun buildSameNameRoleItems(rows: List<BaGuideRow>): List<SameNameRoleItem> {
    if (rows.isEmpty()) return emptyList()
    val items = rows.mapNotNull { row ->
        val normalizedKey = normalizeProfileFieldKey(row.key)
        if (normalizedKey != sameNameRoleNameRowKey && normalizedKey != relatedSameNameRoleHeaderKey) {
            return@mapNotNull null
        }
        val tokens = splitRoleRowTokens(row.value)
        val link = sequence<String> {
            tokens.forEach { yield(it) }
            yield(row.value)
        }.map { token -> extractSameNameGuideLink(token) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val name = tokens.firstOrNull { token ->
            token.isNotBlank() &&
                !isProfileValuePlaceholder(token) &&
                extractProfileExternalLink(token).isBlank() &&
                !isSameNameRoleHintText(token)
        }.orEmpty()
        val image = (row.imageUrls + row.imageUrl)
            .firstOrNull { candidate -> isRenderableGalleryImageUrl(candidate) }
            .orEmpty()
        if (name.isBlank() && link.isBlank() && image.isBlank()) {
            return@mapNotNull null
        }
        if (name.isBlank() && link.isBlank() && isSameNameRoleHintText(row.value)) {
            return@mapNotNull null
        }
        val fallbackName = when {
            link.isNotBlank() -> fallbackProfileLinkTitle(link)
            else -> "同名角色"
        }
        SameNameRoleItem(
            name = name.ifBlank { fallbackName },
            linkUrl = link,
            imageUrl = image
        )
    }

    return items.distinctBy { item ->
        "${item.name.trim()}|${item.linkUrl.trim()}|${item.imageUrl.trim()}"
    }
}

private fun isProfileSectionHeaderRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key in profileSectionHeaderKeys
}

private val galleryRelatedProfileLinkKeyTokens = listOf(
    "影画相关链接",
    "相关链接",
    "来源链接",
    "个人账号主页",
    "账号主页",
    "个人主页",
    "主页链接",
    "主页"
).map(::normalizeProfileFieldKey)

private fun isGalleryRelatedProfileLinkRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    if (key.isBlank()) return false
    val hasGalleryLinkKey = galleryRelatedProfileLinkKeyTokens.any { token ->
        token.isNotBlank() && key.contains(token)
    }
    if (!hasGalleryLinkKey) return false
    val linkSource = buildString {
        append(row.value)
        append(' ')
        append(row.key)
    }
    return containsGuideWebLink(linkSource)
}

private fun extractProfileExternalLink(raw: String): String {
    val source = raw.trim()
    if (source.isBlank()) return ""

    val direct = when {
        source.startsWith("http://", ignoreCase = true) ||
            source.startsWith("https://", ignoreCase = true) -> source
        source.startsWith("www.", ignoreCase = true) -> "https://$source"
        source.startsWith("/") -> normalizeGuideUrl(source)
        else -> ""
    }
    if (direct.isNotBlank()) {
        val scheme = runCatching { Uri.parse(direct).scheme.orEmpty() }.getOrDefault("")
        if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) {
            return direct
        }
    }

    val embedded = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
        .find(source)
        ?.value
        .orEmpty()
        .trimEnd(')', ']', '}', ',', '。', '，')
    if (embedded.isBlank()) return ""

    val embeddedScheme = runCatching { Uri.parse(embedded).scheme.orEmpty() }.getOrDefault("")
    return if (embeddedScheme.equals("http", ignoreCase = true) || embeddedScheme.equals("https", ignoreCase = true)) {
        embedded
    } else {
        ""
    }
}

private fun resolveProfileLinkTitle(url: String): String {
    if (url.isBlank()) return ""
    profileLinkTitleCache[url]?.let { return it }
    val title = runCatching { fetchProfileLinkTitle(url) }.getOrDefault("")
    profileLinkTitleCache[url] = title
    return title
}

private fun fetchProfileLinkTitle(url: String): String {
    if (url.isBlank()) return ""
    val html = GameKeeFetchHelper.fetchHtml(
        pathOrUrl = url,
        refererPath = "/ba/"
    )
    if (html.isBlank()) return ""

    val ogTitle = Regex(
        pattern = """<meta\s+[^>]*(?:property|name)\s*=\s*["']og:title["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(html)?.groupValues?.getOrNull(1).orEmpty()

    val titleTag = Regex(
        pattern = """<title[^>]*>(.*?)</title>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(html)?.groupValues?.getOrNull(1).orEmpty()

    return cleanProfileLinkTitle(
        if (ogTitle.isNotBlank()) ogTitle else titleTag
    )
}

private fun cleanProfileLinkTitle(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun fallbackProfileLinkTitle(url: String): String {
    val uri = runCatching { Uri.parse(url) }.getOrNull()
    val lastSegment = uri?.lastPathSegment?.trim().orEmpty()
    if (lastSegment.isNotBlank()) return lastSegment
    val host = uri?.host?.trim().orEmpty()
    if (host.isNotBlank()) return host
    return "打开链接"
}

private fun isProfileValuePlaceholder(value: String): Boolean {
    val normalized = value.trim()
    val compact = normalized
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
    if (normalized.isBlank()) return true
    if (compact.matches(Regex("""^[\\/|｜／,，;；:：._\-—~·*]+$"""))) return true
    return normalized == "-" ||
        normalized == "—" ||
        normalized == "--" ||
        normalized == "暂无" ||
        normalized == "无" ||
        compact == "n" ||
        compact == "null" ||
        compact == "undefined" ||
        compact == "nan"
}

private val profileInstructionNoteRegex = Regex("""(?:<-|←)?\s*(?:这个|这里|此处|这条)?\s*不用写""")

private fun stripProfileInstructionNotes(raw: String): String {
    if (raw.isBlank()) return ""
    if (!profileInstructionNoteRegex.containsMatchIn(raw)) return raw.trim()

    val segments = raw
        .split(Regex("""\s*(?:/|／|\||｜|,|，|\n)\s*"""))
        .map { segment ->
            profileInstructionNoteRegex
                .replace(segment, "")
                .trim()
                .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
                .trim()
        }
        .filter { it.isNotBlank() }
    if (segments.isNotEmpty()) {
        return segments.joinToString(" / ").trim()
    }
    return profileInstructionNoteRegex
        .replace(raw, "")
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
}

private fun isProfileInstructionPlaceholder(value: String): Boolean {
    if (value.isBlank()) return false
    val normalized = value.trim()
    if (!profileInstructionNoteRegex.containsMatchIn(normalized)) return false
    val stripped = stripProfileInstructionNotes(normalized)
    return isProfileValuePlaceholder(stripped)
}

private fun stripProfileCopyHint(raw: String): String {
    if (raw.isBlank()) return ""
    val hintRegex = Regex("""(?:<-|←)?\s*大部分时候可以去别的图鉴复制""")
    if (!hintRegex.containsMatchIn(raw)) return raw.trim()

    val segments = raw
        .split(Regex("""\s*(?:/|／|\||｜|,|，|\n)\s*"""))
        .map { it.trim() }
        .filter { part ->
            part.isNotBlank() && !hintRegex.containsMatchIn(part)
        }
    if (segments.isNotEmpty()) {
        return segments.joinToString(" / ").trim()
    }
    return raw
        .replace(hintRegex, "")
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
}

private val profileInlineNoteStripFieldKeys = setOf(
    "角色考据",
    "设计",
    "MomoTalk解锁等级",
    "Momotalk解锁等级"
).map(::normalizeProfileFieldKey).toSet()

private fun sanitizeProfileFieldValue(key: String, value: String): String {
    if (value.isBlank()) return ""
    val normalizedKey = normalizeProfileFieldKey(key)
    var cleaned = value.trim()
    if (normalizedKey == normalizeProfileFieldKey("声优")) {
        cleaned = stripProfileCopyHint(cleaned)
    }
    if (normalizedKey in profileInlineNoteStripFieldKeys) {
        cleaned = stripGuideInlineNotes(cleaned)
    }
    cleaned = stripProfileInstructionNotes(cleaned)
    cleaned = cleaned
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
    return cleaned
}

private fun isProfileRowAliasMatch(row: BaGuideRow, aliases: List<String>): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    if (key.isBlank()) return false
    return aliases.any { alias ->
        key == normalizeProfileFieldKey(alias)
    }
}

private fun buildProfileCardRows(rows: List<BaGuideRow>, specs: List<ProfileFieldSpec>): List<BaGuideRow> {
    return buildList {
        specs.forEach { spec ->
            val matched = rows.firstOrNull { row ->
                isProfileRowAliasMatch(row, spec.aliases)
            } ?: return@forEach
            val normalizedValue = sanitizeProfileFieldValue(spec.title, matched.value)
            if (isProfileInstructionPlaceholder(matched.value) && isProfileValuePlaceholder(normalizedValue)) {
                return@forEach
            }
            if (spec.hideWhenEmpty && isProfileValuePlaceholder(normalizedValue)) {
                return@forEach
            }
            add(matched.copy(key = spec.title, value = normalizedValue))
        }
    }
}

private fun isStructuredProfileCardRow(row: BaGuideRow): Boolean {
    return profileStructuredFieldSpecs.any { spec ->
        isProfileRowAliasMatch(row, spec.aliases)
    }
}

private val topDataStatKeys = setOf(
    "攻击力", "防御力", "生命值", "治愈力",
    "命中值", "闪避值", "暴击值", "暴击伤害",
    "稳定值", "射程", "群控强化力", "群控抵抗力",
    "装弹数", "防御无视值", "受恢复率", "COST恢复力"
)
private val normalizedTopDataStatKeys = topDataStatKeys.map(::normalizeProfileFieldKey).toSet()

private fun splitGuideCompositeValues(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .replace("／", "/")
        .replace("|", "/")
        .replace("｜", "/")
        .split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() && it != "-" && it != "—" }
}

private fun isSkillMigratedProfileRow(
    row: BaGuideRow,
    hasTopDataHeader: Boolean,
    hasInitialDataHeader: Boolean
): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    val value = normalizeProfileFieldKey(row.value)
    if (Regex("""^附加属性\d+$""").matches(key)) return true
    if (key == normalizeProfileFieldKey("初始数据")) return true
    if (key == normalizeProfileFieldKey("顶级数据")) return true
    if (key == normalizeProfileFieldKey("25级")) return true
    if (Regex("""^t\d+$""", RegexOption.IGNORE_CASE).matches(key)) return true
    if (Regex("""^t\d+(效果|所需升级材料|技能图标)$""", RegexOption.IGNORE_CASE).matches(key)) return true
    if (
        isLikelySimulateStatLabel(row.key) &&
        isLikelySimulateStatLabel(row.value) &&
        !Regex("""\d""").containsMatchIn(value)
    ) {
        return true
    }
    if ((hasTopDataHeader || hasInitialDataHeader) && key in normalizedTopDataStatKeys) return true
    return false
}

private fun canonicalVoiceLanguageForDisplay(raw: String): String {
    val normalized = raw
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
        .trim()
    if (normalized.isBlank()) return ""
    return when {
        normalized.contains("官翻") || normalized.contains("官方翻译") || normalized.contains("官方中文") || normalized.contains("官中") -> "官翻"
        normalized.contains("韩") || normalized.contains("kr") || normalized.contains("kor") || normalized.contains("korean") -> "韩配"
        normalized.contains("中") || normalized.contains("cn") || normalized.contains("国语") || normalized.contains("国配") || normalized.contains("中文") -> "中配"
        normalized.contains("日") || normalized.contains("jp") || normalized.contains("jpn") || normalized.contains("日本") -> "日配"
        else -> raw.trim()
    }
}

private fun defaultVoiceLanguageHeaderForDisplay(index: Int): String {
    return when (index) {
        0 -> "日配"
        1 -> "中配"
        2 -> "韩配"
        else -> "语言${index + 1}"
    }
}

private fun buildVoiceCvDisplayMap(info: BaStudentGuideInfo): Map<String, String> {
    val merged = linkedMapOf<String, String>()
    info.voiceCvByLanguage.forEach { (rawKey, rawValue) ->
        val key = canonicalVoiceLanguageForDisplay(rawKey)
        val value = rawValue.trim()
        if (
            key.isNotBlank() &&
            value.isNotBlank() &&
            !isOfficialTranslationHeader(key) &&
            merged[key].isNullOrBlank()
        ) {
            merged[key] = value
        }
    }
    val jp = info.voiceCvJp.trim()
    if (jp.isNotBlank() && merged["日配"].isNullOrBlank()) {
        merged["日配"] = jp
    }
    val cn = info.voiceCvCn.trim()
    if (cn.isNotBlank() && merged["中配"].isNullOrBlank()) {
        merged["中配"] = cn
    }

    val ordered = linkedMapOf<String, String>()
    listOf("日配", "中配", "韩配").forEach { label ->
        merged[label]?.takeIf { it.isNotBlank() }?.let { value ->
            ordered[label] = value
        }
    }
    merged.forEach { (key, value) ->
        if (key !in ordered && value.isNotBlank()) {
            ordered[key] = value
        }
    }
    return ordered
}

private fun buildVoiceLanguageHeadersForDisplay(
    headers: List<String>,
    entries: List<BaGuideVoiceEntry>
): List<String> {
    val merged = mutableListOf<String>()
    headers.forEach { raw ->
        val normalized = canonicalVoiceLanguageForDisplay(raw)
        if (normalized.isBlank()) return@forEach
        if (isOfficialTranslationHeader(normalized)) return@forEach
        if (normalized !in merged) {
            merged += normalized
        }
    }
    val maxAudioCount = entries.maxOfOrNull { entry ->
        maxOf(entry.audioUrls.size, if (entry.audioUrl.trim().isNotBlank()) 1 else 0)
    } ?: 0
    while (merged.size < maxAudioCount) {
        val fallback = defaultVoiceLanguageHeaderForDisplay(merged.size)
        merged += fallback
    }
    return merged.mapIndexed { index, header ->
        header.ifBlank { defaultVoiceLanguageHeaderForDisplay(index) }
    }
}

private fun isOfficialTranslationHeader(header: String): Boolean {
    return canonicalVoiceLanguageForDisplay(header) == "官翻"
}

private fun hasVoiceAudioAtIndex(entries: List<BaGuideVoiceEntry>, index: Int): Boolean {
    return entries.any { entry ->
        val directAudio = entry.audioUrls.getOrNull(index).orEmpty().trim()
        directAudio.isNotBlank() ||
            (index == 0 && entry.audioUrls.isEmpty() && entry.audioUrl.trim().isNotBlank())
    }
}

private fun buildDubbingHeadersForVoiceCard(
    headers: List<String>,
    entries: List<BaGuideVoiceEntry>
): List<String> {
    if (headers.isEmpty()) return emptyList()
    return headers.mapIndexedNotNull { index, header ->
        val normalized = canonicalVoiceLanguageForDisplay(header)
        if (normalized.isBlank()) return@mapIndexedNotNull null
        if (isOfficialTranslationHeader(normalized)) return@mapIndexedNotNull null
        if (!hasVoiceAudioAtIndex(entries, index)) return@mapIndexedNotNull null
        normalized
    }.distinct()
}

private fun resolveVoicePlaybackUrl(
    entry: BaGuideVoiceEntry,
    headers: List<String>,
    selectedHeader: String
): String {
    val normalizedSelected = canonicalVoiceLanguageForDisplay(selectedHeader)
    if (headers.isNotEmpty() && normalizedSelected.isNotBlank()) {
        val selectedIndex = headers.indexOfFirst { header ->
            canonicalVoiceLanguageForDisplay(header) == normalizedSelected
        }
        if (selectedIndex >= 0) {
            val selectedAudio = normalizeGuideUrl(entry.audioUrls.getOrNull(selectedIndex).orEmpty())
            if (selectedAudio.isNotBlank()) {
                return selectedAudio
            }
        }
    }
    val fallbackAudio = entry.audioUrls
        .asSequence()
        .map(::normalizeGuideUrl)
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
    if (fallbackAudio.isNotBlank()) return fallbackAudio
    return normalizeGuideUrl(entry.audioUrl)
}

private fun normalizeGuidePlaybackSource(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val scheme = runCatching { Uri.parse(value).scheme.orEmpty() }.getOrDefault("")
    return if (scheme.equals("file", ignoreCase = true)) {
        value
    } else {
        normalizeGuideUrl(value)
    }
}

private fun isGrowthTitleVoiceRow(row: BaGuideRow): Boolean {
    fun normalize(text: String): String = text.replace(" ", "").lowercase()
    val key = normalize(row.key)
    val value = normalize(row.value)
    fun matches(text: String): Boolean {
        if (text.isBlank()) return false
        return (text.contains("成长") && text.contains("title")) ||
            text.contains("成长标题") ||
            text.contains("growthtitle") ||
            text.contains("growth_title")
    }
    return matches(key) || matches(value)
}

private fun isVoicePlaceholderRow(row: BaGuideRow): Boolean {
    val merged = listOf(row.key.trim(), row.value.trim()).joinToString(" ").replace(" ", "")
    return Regex("""被CC\d+""").containsMatchIn(merged)
}

private fun buildGrowthTitleVoiceEntries(rows: List<BaGuideRow>): List<BaGuideVoiceEntry> {
    return rows.mapIndexedNotNull { index, row ->
        val lines = parseGrowthTitleVoiceLines(row.value)
        val jp = lines.getOrNull(0).orEmpty().trim()
        val cn = lines.getOrNull(1).orEmpty().trim()
        if (jp.isBlank() && cn.isBlank()) return@mapIndexedNotNull null
        BaGuideVoiceEntry(
            section = "成长",
            title = row.key.trim().ifBlank { "成长台词 ${index + 1}" },
            lines = listOf(jp, cn),
            audioUrl = ""
        )
    }
}

private fun parseGrowthTitleVoiceLines(raw: String): List<String> {
    val normalized = raw.trim()
    if (normalized.isBlank()) return listOf("", "")
    val lineBreakParts = normalized
        .split('\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (lineBreakParts.size >= 2) {
        return listOf(lineBreakParts[0], lineBreakParts[1])
    }
    val slashParts = normalized
        .split(Regex("""\s*(?:/|／|\|)\s*"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return when {
        slashParts.size >= 2 -> listOf(slashParts[0], slashParts[1])
        slashParts.size == 1 -> listOf(slashParts[0], "")
        else -> listOf("", "")
    }
}
