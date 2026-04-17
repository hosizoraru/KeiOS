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
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogStore
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
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
                                    val useNpcPortraitTopCrop = remember(guide.sourceUrl, guide.syncedAtMs) {
                                        isNpcSatelliteGuideSource(guide.sourceUrl)
                                    }
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
                                                        imageHeight = 152.dp,
                                                        cropAlignment = if (useNpcPortraitTopCrop) {
                                                            Alignment.TopCenter
                                                        } else {
                                                            Alignment.Center
                                                        }
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
                                                    modifier = Modifier.guideTabCopyable(
                                                        buildGuideTabCopyPayload("相关同名角色", hint)
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
                                                            .guideTabCopyable(
                                                                buildGuideTabCopyPayload(
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
                                                    onLongClick = rememberGuideTabCopyAction(
                                                        buildGuideTabCopyPayload(title, value)
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
