package com.example.keios.ui.page.main.student

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    onToggleVoicePlayback: (String) -> Unit,
    onSelectedVoiceLanguageChange: (String) -> Unit
) {
                when (activeBottomTab) {
                    GuideBottomTab.Archive -> {
                        item {
                            val guide = info
                            val profileItems = guide?.buildProfileMetaItems().orEmpty()
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
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(152.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                profileItems.forEach { item ->
                                                    GuideProfileMetaLine(item)
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
                            val combatItems = guide?.buildCombatMetaItems().orEmpty()
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
                            val topDataRows = buildTopDataRowsForSkillPage(guide)

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

                            if (topDataRows.isNotEmpty()) {
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
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "顶级数据",
                                                color = MiuixTheme.colorScheme.onBackground
                                            )
                                            GuideRowsSection(
                                                rows = topDataRows,
                                                emptyText = "暂无顶级数据。"
                                            )
                                        }
                                    }
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
                            val sameNameRoleRows = profileRowsBase.filter(::isSameNameRoleRow)
                            val sameNameRoleItems = buildSameNameRoleItems(sameNameRoleRows)
                            val sameNameRoleHint = sameNameRoleRows.firstNotNullOfOrNull { row ->
                                if (normalizeProfileFieldKey(row.key) != relatedSameNameRoleHeaderKey) return@firstNotNullOfOrNull null
                                row.value
                                    .trim()
                                    .trim('*')
                                    .takeIf { it.isNotBlank() }
                            }.orEmpty()
                            val hasTopDataHeader = profileRowsBase.any { row ->
                                normalizeProfileFieldKey(row.key) == normalizeProfileFieldKey("顶级数据")
                            }
                            val allProfileRows = profileRowsBase.filterNot { row ->
                                isSkillMigratedProfileRow(row, hasTopDataHeader) || isSameNameRoleRow(row)
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
                                            Text(
                                                text = hint,
                                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
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
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
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
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
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
                                                MiuixInfoItem(title, value)
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
                                    val normalizedPlaybackUrl = normalizeGuideUrl(playbackUrl)
                                    item {
                                        GuideVoiceEntryCard(
                                            entry = entry,
                                            languageHeaders = voiceLanguageHeaders,
                                            backdrop = backdrop,
                                            playbackUrl = normalizedPlaybackUrl,
                                            isPlaying = normalizedPlaybackUrl.isNotBlank() &&
                                                normalizedPlaybackUrl == playingVoiceUrl &&
                                                isVoicePlaying,
                                            playProgress = if (normalizedPlaybackUrl.isNotBlank() && normalizedPlaybackUrl == playingVoiceUrl) {
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

                    else -> {
                        item {
                            FrostedBlock(
                                backdrop = backdrop,
                                title = activeBottomTab.label,
                                subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                accent = accent,
                                content = {
                                    error?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            color = MiuixTheme.colorScheme.error,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    val guide = info
                                    if (guide != null) {
                                        val profileRows = guide.profileRowsForDisplay()
                                        val visibleProfileRows = profileRows.filterNot { shouldHideMovedHeaderRow(it) }
                                        val growthRows = guide.growthRowsForDisplay()
                                        val activeGuideTab = activeBottomTab.guideTab

                                        Text(
                                            text = guide.summary.ifBlank { guide.description },
                                            color = MiuixTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        when (activeGuideTab) {
                                            GuideTab.Profile -> {
                                                GuideRowsSection(
                                                    rows = visibleProfileRows,
                                                    emptyText = "暂未解析到学生档案数据。"
                                                )
                                            }

                                            GuideTab.Voice -> {
                                                GuideRowsSection(
                                                    rows = guide.voiceRows,
                                                    emptyText = "语音台词解析中，当前版本先完善其他栏目。"
                                                )
                                            }

                                            GuideTab.Simulate -> {
                                                GuideRowsSection(
                                                    rows = growthRows,
                                                    emptyText = "暂未解析到养成模拟数据。"
                                                )
                                            }

                                            else -> {}
                                        }
                                    }
                                }
                            )
                        }
                    }
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
    val showCapsule = preferCapsule && shouldUseProfileValueCapsule(displayKey, displayValue, onClick)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
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
                        onClick = onClick
                    )
                } else {
                    val clickableModifier = if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                    Text(
                        text = displayValue,
                        color = valueColor ?: MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(clickableModifier),
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

@Composable
private fun GuideProfileValueCapsule(
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
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
    val visibleRows = rows.take(120)
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
                    modifier = Modifier.width(cardWidth),
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
    ProfileFieldSpec("实装日期", listOf("实装日期")),
    ProfileFieldSpec("声优", listOf("声优")),
    ProfileFieldSpec("角色考据", listOf("角色考据"), hideWhenEmpty = true),
    ProfileFieldSpec("设计", listOf("设计", "设计师"), hideWhenEmpty = true)
)

private val profileHobbyFieldSpecs = listOf(
    ProfileFieldSpec("兴趣爱好", listOf("兴趣爱好")),
    ProfileFieldSpec("个人简介", listOf("个人简介")),
    ProfileFieldSpec("MomoTalk状态消息", listOf("MomoTalk状态消息", "Momotalk状态消息")),
    ProfileFieldSpec("MomoTalk解锁等级", listOf("MomoTalk解锁等级", "Momotalk解锁等级"))
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
        .split(Regex("""\s+/\s+"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun buildSameNameRoleItems(rows: List<BaGuideRow>): List<SameNameRoleItem> {
    if (rows.isEmpty()) return emptyList()
    val items = rows.mapNotNull { row ->
        if (normalizeProfileFieldKey(row.key) != sameNameRoleNameRowKey) return@mapNotNull null
        val tokens = splitRoleRowTokens(row.value)
        val link = sequence<String> {
            tokens.forEach { yield(it) }
            yield(row.value)
        }.map { token -> extractProfileExternalLink(token) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val name = tokens.firstOrNull { token ->
            token.isNotBlank() && extractProfileExternalLink(token).isBlank()
        }.orEmpty()
        val image = (row.imageUrls + row.imageUrl)
            .firstOrNull { candidate -> isRenderableGalleryImageUrl(candidate) }
            .orEmpty()
        if (name.isBlank() && link.isBlank() && image.isBlank()) {
            return@mapNotNull null
        }
        SameNameRoleItem(
            name = name.ifBlank { fallbackProfileLinkTitle(link) },
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
    if (normalized.isBlank()) return true
    return normalized == "-" ||
        normalized == "—" ||
        normalized == "--" ||
        normalized == "暂无" ||
        normalized == "无"
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
            if (spec.hideWhenEmpty && isProfileValuePlaceholder(matched.value)) {
                return@forEach
            }
            add(matched.copy(key = spec.title))
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

private fun isSkillMigratedProfileRow(row: BaGuideRow, hasTopDataHeader: Boolean): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    if (Regex("""^附加属性\d+$""").matches(key)) return true
    if (key == normalizeProfileFieldKey("顶级数据")) return true
    if (key == normalizeProfileFieldKey("25级")) return true
    if (hasTopDataHeader && key in normalizedTopDataStatKeys) return true
    return false
}

private fun buildTopDataRowsForSkillPage(info: BaStudentGuideInfo): List<BaGuideRow> {
    val sourceRows = buildList {
        addAll(info.skillRowsForDisplay())
        addAll(info.growthRowsForDisplay())
        addAll(info.profileRowsForDisplay())
    }
    if (sourceRows.isEmpty()) return emptyList()

    val result = mutableListOf<BaGuideRow>()
    var inTopDataBlock = false
    sourceRows.forEach { row ->
        val key = row.key.trim()
        val value = row.value.trim()
        val normalizedKey = normalizeProfileFieldKey(key)
        if (normalizedKey == normalizeProfileFieldKey("顶级数据")) {
            inTopDataBlock = true
            if (value.isNotBlank()) {
                result += BaGuideRow(
                    key = "说明",
                    value = value.trim('*')
                )
            }
            return@forEach
        }

        if (inTopDataBlock && (
                normalizedKey == normalizeProfileFieldKey("专武") ||
                    normalizedKey == normalizeProfileFieldKey("装备") ||
                    normalizedKey == normalizeProfileFieldKey("爱用品") ||
                    normalizedKey == normalizeProfileFieldKey("能力解放") ||
                    normalizedKey.contains(normalizeProfileFieldKey("羁绊等级奖励"))
                )
        ) {
            inTopDataBlock = false
        }
        if (!inTopDataBlock) return@forEach

        if (normalizedKey in normalizedTopDataStatKeys && value.isNotBlank()) {
            val tokens = splitGuideCompositeValues(value)
            if (tokens.isEmpty()) return@forEach

            val firstTokenLooksLikeKey = normalizeProfileFieldKey(tokens.first()) in normalizedTopDataStatKeys
            var index = if (firstTokenLooksLikeKey) 0 else 1
            if (!firstTokenLooksLikeKey) {
                result += BaGuideRow(
                    key = key.ifBlank { "数据" },
                    value = tokens.first()
                )
            }
            while (index + 1 < tokens.size) {
                val statKey = tokens[index].trim()
                val statValue = tokens[index + 1].trim()
                if (statKey.isNotBlank() && statValue.isNotBlank()) {
                    result += BaGuideRow(
                        key = statKey,
                        value = statValue
                    )
                }
                index += 2
            }
        }
    }

    return result.distinctBy { row ->
        "${normalizeProfileFieldKey(row.key)}|${row.value.trim()}"
    }
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
