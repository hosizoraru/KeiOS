package com.example.keios.ui.page.main.student

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    onOpenExternal: (String) -> Unit,
    onToggleVoicePlayback: (String) -> Unit
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
                            val allProfileRows = guide.profileRowsForDisplay()
                                .filterNot(::shouldHideMovedHeaderRow)
                                .filterNot(::isGrowthTitleVoiceRow)
                            val chocolateInfoRows = allProfileRows.filter { row ->
                                val key = row.key.trim()
                                key.contains("巧克力", ignoreCase = true)
                            }
                            val furnitureInfoRows = allProfileRows.filter { row ->
                                val key = row.key.trim()
                                key.contains("互动家具", ignoreCase = true)
                            }
                            val normalProfileRows = allProfileRows.filterNot { row ->
                                val key = row.key.trim()
                                key.contains("巧克力", ignoreCase = true) ||
                                    key.contains("互动家具", ignoreCase = true)
                            }
                            val chocolateGalleryItems = guide.galleryItems
                                .filter(::isChocolateGalleryItem)
                                .filter(::hasRenderableGalleryMedia)
                                .distinctBy {
                                    val media = it.mediaUrl.ifBlank { it.imageUrl }
                                    "${it.mediaType}|$media"
                                }
                            val furnitureGalleryItems = guide.galleryItems
                                .filter(::isInteractiveFurnitureGalleryItem)
                                .filter(::hasRenderableGalleryMedia)
                                .distinctBy {
                                    val media = it.mediaUrl.ifBlank { it.imageUrl }
                                    "${it.mediaType}|$media"
                                }
                                .sortedBy { item ->
                                    Regex("""(\d+)(?!.*\d)""").find(item.title)?.groupValues?.getOrNull(1)?.toIntOrNull()
                                        ?: Int.MAX_VALUE
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
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            GuideRowsSection(
                                                rows = normalProfileRows,
                                                emptyText = "暂未解析到学生档案数据。"
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
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "巧克力",
                                                color = MiuixTheme.colorScheme.onBackground
                                            )
                                            chocolateInfoRows.forEach { row ->
                                                val value = row.value.ifBlank { "-" }
                                                MiuixInfoItem(
                                                    key = row.key.ifBlank { "信息" },
                                                    value = value
                                                )
                                            }
                                        }
                                    }
                                }

                                chocolateGalleryItems.forEach { chocolateItem ->
                                    item { Spacer(modifier = Modifier.height(10.dp)) }
                                    item {
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
                                            }
                                        )
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
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "互动家具",
                                                color = MiuixTheme.colorScheme.onBackground
                                            )
                                            furnitureInfoRows.forEach { row ->
                                                val value = row.value.ifBlank { "-" }
                                                MiuixInfoItem(
                                                    key = row.key.ifBlank { "信息" },
                                                    value = value
                                                )
                                            }
                                        }
                                    }
                                }

                                furnitureGalleryItems.forEach { furnitureItem ->
                                    item { Spacer(modifier = Modifier.height(10.dp)) }
                                    item {
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
                                            }
                                        )
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
                                val jp = entry.lines.getOrNull(0).orEmpty().trim()
                                val cn = entry.lines.getOrNull(1).orEmpty().trim()
                                jp.isNotBlank() || cn.isNotBlank()
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
                                        entry.lines.joinToString("|") { it.trim() },
                                        normalizeGuideUrl(entry.audioUrl)
                                    ).joinToString("|")
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
                                    item {
                                        GuideVoiceEntryCard(
                                            entry = entry,
                                            languageHeaders = guide.voiceLanguageHeaders,
                                            backdrop = backdrop,
                                            isPlaying = normalizeGuideUrl(entry.audioUrl) == playingVoiceUrl && isVoicePlaying,
                                            playProgress = if (normalizeGuideUrl(entry.audioUrl) == playingVoiceUrl) {
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
