package com.example.keios.ui.page.main.student

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.ba.helper.GameKeeFetchHelper
import com.example.keios.R
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun GuideRemoteImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageHeight: androidx.compose.ui.unit.Dp = 220.dp
) {
    val target = remember(imageUrl) { normalizeGuideUrl(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching { GameKeeFetchHelper.fetchImage(target) }.getOrNull()
        }
    }
    val rendered = bitmap ?: return
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(RoundedCornerShape(14.dp))
    )
}

@Composable
fun GuideRemoteIcon(
    imageUrl: String,
    modifier: Modifier = Modifier,
    iconWidth: androidx.compose.ui.unit.Dp = 20.dp,
    iconHeight: androidx.compose.ui.unit.Dp = iconWidth
) {
    val target = remember(imageUrl) { normalizeGuideUrl(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching { GameKeeFetchHelper.fetchImage(target) }.getOrNull()
        }
    }
    val rendered = bitmap ?: return
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(iconWidth)
            .height(iconHeight)
    )
}

@Composable
fun GuideRowsSection(
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
        val key = row.key.ifBlank { "信息" }
        val hasImage = row.imageUrl.isNotBlank()
        val value = row.value
            .takeIf { it.isNotBlank() && it != "图片" }
            ?: if (hasImage) "见下图" else "-"
        MiuixInfoItem(key, value)
        if (hasImage) {
            Spacer(modifier = Modifier.height(6.dp))
            GuideRemoteImage(
                imageUrl = row.imageUrl,
                imageHeight = imageHeight
            )
        }
        if (index < visibleRows.lastIndex) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun GuideGallerySection(
    items: List<BaGuideGalleryItem>,
    emptyText: String
) {
    if (items.isEmpty()) {
        Text(emptyText, color = MiuixTheme.colorScheme.onBackgroundVariant)
        return
    }
    val visibleItems = items.distinctBy { it.imageUrl }.take(24)
    visibleItems.forEachIndexed { index, item ->
        if (item.title.isNotBlank()) {
            Text(item.title, color = MiuixTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(6.dp))
        }
        GuideRemoteImage(
            imageUrl = item.imageUrl,
            imageHeight = 220.dp
        )
        if (index < visibleItems.lastIndex) {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun GuideProfileMetaLine(item: BaGuideMetaItem) {
    val isPosition = item.title == "位置"
    val summary = if (isPosition) "" else item.value.ifBlank { "-" }
    val titleSlotWidth = 70.dp
    val iconSlotWidth = 34.dp
    val iconSlotHeight = 24.dp
    val iconWidth = if (isPosition) 30.dp else 20.dp
    val iconHeight = 20.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.width(titleSlotWidth),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .width(iconSlotWidth)
                .height(iconSlotHeight),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = item.imageUrl,
                    iconWidth = iconWidth,
                    iconHeight = iconHeight
                )
            }
        }
        if (!isPosition) {
            Text(
                text = summary,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Spacer(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun GuideCombatMetaTile(
    item: BaGuideMetaItem,
    modifier: Modifier = Modifier
) {
    val value = item.value.ifBlank { "-" }
    val adaptiveWide = item.title.contains("战术") || item.title == "武器类型"
    val titleWidth = 112.dp
    val iconWidth = if (adaptiveWide) 28.dp else 18.dp
    val iconHeight = if (adaptiveWide) 18.dp else 18.dp
    val extraIconWidth = 30.dp
    val extraIconHeight = 18.dp
    val iconSlotWidth = 30.dp
    val iconSlotHeight = 22.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.36f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.width(titleWidth),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .width(iconSlotWidth)
                .height(iconSlotHeight),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = item.imageUrl,
                    iconWidth = iconWidth,
                    iconHeight = iconHeight
                )
            }
        }
        if (item.extraImageUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .width(iconSlotWidth)
                    .height(iconSlotHeight),
                contentAlignment = Alignment.Center
            ) {
                GuideRemoteIcon(
                    imageUrl = item.extraImageUrl,
                    iconWidth = extraIconWidth,
                    iconHeight = extraIconHeight
                )
            }
        }
    }
}

@Composable
fun GuideSkillCardItem(
    card: GuideSkillCardModel,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    var showLevelPopup by remember(card.id) { mutableStateOf(false) }
    val levelOptions = card.levelOptions
    var selectedLevel by rememberSaveable(card.id) { mutableStateOf(card.defaultLevel) }

    LaunchedEffect(card.id, card.defaultLevel, levelOptions) {
        if (levelOptions.isEmpty()) {
            selectedLevel = card.defaultLevel
        } else if (selectedLevel !in levelOptions) {
            selectedLevel = card.defaultLevel
        }
    }

    val skillDesc = card.descriptionFor(selectedLevel)
    val skillCost = card.costFor(selectedLevel)
    val displayLevel = selectedLevel.ifBlank { card.defaultLevel }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (card.iconUrl.isNotBlank()) {
                        GuideRemoteIcon(
                            imageUrl = card.iconUrl,
                            iconWidth = 34.dp,
                            iconHeight = 34.dp
                        )
                    }
                    Text(
                        text = card.name,
                        modifier = Modifier.weight(1f),
                        color = MiuixTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (card.type.isNotBlank()) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = card.type,
                            enabled = false,
                            textColor = Color(0xFF3B82F6),
                            bottomBarStyle = true,
                            onClick = {}
                        )
                    }
                    if (skillCost.isNotBlank()) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = "COST: $skillCost",
                            enabled = false,
                            textColor = Color(0xFF3B82F6),
                            bottomBarStyle = true,
                            onClick = {}
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GuideSkillDescriptionText(
                    description = skillDesc.ifBlank { "暂未解析到技能描述。" },
                    glossaryIcons = card.glossaryIcons,
                    descriptionIcons = card.descriptionIconsFor(selectedLevel),
                    modifier = Modifier.weight(1f)
                )
                if (levelOptions.isNotEmpty()) {
                    Box {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = displayLevel,
                            bottomBarStyle = true,
                            onClick = { showLevelPopup = !showLevelPopup }
                        )
                        if (showLevelPopup) {
                            WindowListPopup(
                                show = showLevelPopup,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                onDismissRequest = { showLevelPopup = false },
                                enableWindowDim = false
                            ) {
                                ListPopupColumn {
                                    levelOptions.forEachIndexed { index, option ->
                                        DropdownImpl(
                                            text = option,
                                            optionSize = levelOptions.size,
                                            isSelected = selectedLevel == option,
                                            index = index,
                                            onSelectedIndexChange = { selected ->
                                                selectedLevel = levelOptions[selected]
                                                showLevelPopup = false
                                            }
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
}

@Composable
fun GuideVoiceLanguageCard(
    headers: List<String>,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    val visibleHeaders = headers
        .filterNot { header ->
            val value = header.lowercase()
            value.contains("韩") || value.contains("kr") || value.contains("korean")
        }
        .ifEmpty { listOf("日配", "中配") }
        .take(2)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "配音",
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
            visibleHeaders.forEach { header ->
                GlassTextButton(
                    backdrop = backdrop,
                    text = header,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun GuideVoiceEntryCard(
    entry: BaGuideVoiceEntry,
    languageHeaders: List<String>,
    backdrop: Backdrop?,
    isPlaying: Boolean,
    onTogglePlay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = if (languageHeaders.isNotEmpty()) {
        languageHeaders
    } else {
        listOf("日配", "中配")
    }
    val jpLabel = labels.getOrNull(0) ?: "日配"
    val cnLabel = labels.getOrNull(1) ?: "中配"
    val jpText = entry.lines.getOrNull(0).orEmpty().trim()
    val cnText = entry.lines.getOrNull(1).orEmpty().trim()
    Card(
        modifier = modifier.fillMaxWidth(),
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
                if (entry.section.isNotBlank()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = entry.section,
                        enabled = false,
                        textColor = Color(0xFF3B82F6),
                        bottomBarStyle = true,
                        onClick = {}
                    )
                }
                Text(
                    text = entry.title.ifBlank { "语音条目" },
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (entry.audioUrl.isNotBlank()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = if (isPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                        textColor = Color(0xFF3B82F6),
                        bottomBarStyle = true,
                        onClick = { onTogglePlay(entry.audioUrl) }
                    )
                }
            }

            val compareLines = listOf(jpLabel to jpText, cnLabel to cnText)
            compareLines.forEach { (label, line) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = label,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.width(58.dp)
                    )
                    Text(
                        text = line.ifBlank { "暂无台词文本" },
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun GuideWeaponCardItem(
    card: GuideWeaponCardModel,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    val levelOptions = remember(card.statHeaders) { card.statHeaders.filter { it.isNotBlank() } }
    val defaultLevel = remember(levelOptions) { levelOptions.lastOrNull().orEmpty() }
    var showLevelPopup by remember(card.name, card.imageUrl) { mutableStateOf(false) }
    var selectedLevel by rememberSaveable(card.name, card.imageUrl) { mutableStateOf(defaultLevel) }

    LaunchedEffect(levelOptions, defaultLevel) {
        if (levelOptions.isEmpty()) {
            selectedLevel = ""
        } else if (selectedLevel !in levelOptions) {
            selectedLevel = defaultLevel
        }
    }

    fun levelValue(row: GuideWeaponStatRow): String {
        if (row.values.isEmpty()) return "-"
        if (levelOptions.isEmpty()) return row.values.joinToString(" / ")
        val index = levelOptions.indexOf(selectedLevel).coerceAtLeast(0)
        return row.values.getOrNull(index) ?: row.values.last()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.name.ifBlank { "专属武器" },
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = "专武",
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }

            Text(
                text = card.description.ifBlank { "暂无专武描述。" },
                color = MiuixTheme.colorScheme.onBackground,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            if (card.imageUrl.isNotBlank()) {
                GuideRemoteImage(
                    imageUrl = card.imageUrl,
                    imageHeight = 132.dp
                )
            }

            if (card.statRows.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "专武数值",
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (levelOptions.isNotEmpty()) {
                            Box {
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = selectedLevel,
                                    bottomBarStyle = true,
                                    onClick = { showLevelPopup = !showLevelPopup }
                                )
                                if (showLevelPopup) {
                                    WindowListPopup(
                                        show = showLevelPopup,
                                        alignment = PopupPositionProvider.Align.BottomEnd,
                                        onDismissRequest = { showLevelPopup = false },
                                        enableWindowDim = false
                                    ) {
                                        ListPopupColumn {
                                            levelOptions.forEachIndexed { idx, option ->
                                                DropdownImpl(
                                                    text = option,
                                                    optionSize = levelOptions.size,
                                                    isSelected = selectedLevel == option,
                                                    index = idx,
                                                    onSelectedIndexChange = { selected ->
                                                        selectedLevel = levelOptions[selected]
                                                        showLevelPopup = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    card.statRows.forEach { stat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stat.title,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                text = levelValue(stat),
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (card.starEffects.isNotEmpty()) {
                card.starEffects.forEachIndexed { index, effect ->
                    GuideWeaponStarEffectItem(
                        effect = effect,
                        glossaryIcons = card.glossaryIcons,
                        backdrop = backdrop
                    )
                    if (index < card.starEffects.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideWeaponStarEffectItem(
    effect: GuideWeaponStarEffect,
    glossaryIcons: Map<String, String>,
    backdrop: Backdrop?
) {
    var showLevelPopup by remember(effect.id) { mutableStateOf(false) }
    val levelOptions = effect.levelOptions
    var selectedLevel by rememberSaveable(effect.id) { mutableStateOf(effect.defaultLevel) }

    LaunchedEffect(effect.id, effect.defaultLevel, levelOptions) {
        if (levelOptions.isEmpty()) {
            selectedLevel = effect.defaultLevel
        } else if (selectedLevel !in levelOptions) {
            selectedLevel = effect.defaultLevel
        }
    }

    val desc = effect.descriptionFor(selectedLevel).trim()

    if (effect.starLabel == "★2") {
        GuideWeaponTwoStarEffectItem(
            effect = effect,
            desc = desc,
            glossaryIcons = glossaryIcons,
            backdrop = backdrop,
            levelOptions = levelOptions,
            selectedLevel = selectedLevel,
            showLevelPopup = showLevelPopup,
            onTogglePopup = { showLevelPopup = !showLevelPopup },
            onDismissPopup = { showLevelPopup = false },
            onLevelSelected = { selected ->
                selectedLevel = levelOptions[selected]
                showLevelPopup = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.28f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuideWeaponStarBadgeRow(effect.starLabel, iconSize = 18.dp)
            if (effect.roleTag.isNotBlank()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = effect.roleTag,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }
            Text(
                text = effect.name.ifBlank { "效果" },
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (effect.iconUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = effect.iconUrl,
                    iconWidth = 20.dp,
                    iconHeight = 20.dp
                )
            }
            GuideEffectLevelPicker(
                backdrop = backdrop,
                levelOptions = levelOptions,
                selectedLevel = selectedLevel,
                showLevelPopup = showLevelPopup,
                onTogglePopup = { showLevelPopup = !showLevelPopup },
                onDismissPopup = { showLevelPopup = false },
                onLevelSelected = { selected ->
                    selectedLevel = levelOptions[selected]
                    showLevelPopup = false
                }
            )
        }

        if (desc.isNotBlank()) {
            GuideSkillDescriptionText(
                description = desc,
                glossaryIcons = glossaryIcons,
                descriptionIcons = effect.descriptionIconsFor(selectedLevel)
            )
        }
    }
}

@Composable
private fun GuideWeaponTwoStarEffectItem(
    effect: GuideWeaponStarEffect,
    desc: String,
    glossaryIcons: Map<String, String>,
    backdrop: Backdrop?,
    levelOptions: List<String>,
    selectedLevel: String,
    showLevelPopup: Boolean,
    onTogglePopup: () -> Unit,
    onDismissPopup: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.34f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuideWeaponStarBadgeRow(effect.starLabel, iconSize = 19.dp)
            if (effect.iconUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = effect.iconUrl,
                    iconWidth = 20.dp,
                    iconHeight = 20.dp
                )
            }
            Text(
                text = effect.name.ifBlank { "辅助技能强化" },
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (effect.roleTag.isNotBlank()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = effect.roleTag,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GuideSkillDescriptionText(
                description = desc.ifBlank { "暂无效果描述。" },
                glossaryIcons = glossaryIcons,
                descriptionIcons = effect.descriptionIconsFor(selectedLevel),
                modifier = Modifier.weight(1f)
            )
            GuideEffectLevelPicker(
                backdrop = backdrop,
                levelOptions = levelOptions,
                selectedLevel = selectedLevel,
                showLevelPopup = showLevelPopup,
                onTogglePopup = onTogglePopup,
                onDismissPopup = onDismissPopup,
                onLevelSelected = onLevelSelected
            )
        }
    }
}

@Composable
private fun GuideEffectLevelPicker(
    backdrop: Backdrop?,
    levelOptions: List<String>,
    selectedLevel: String,
    showLevelPopup: Boolean,
    onTogglePopup: () -> Unit,
    onDismissPopup: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    if (levelOptions.isEmpty()) return
    Box {
        GlassTextButton(
            backdrop = backdrop,
            text = selectedLevel,
            bottomBarStyle = true,
            onClick = onTogglePopup
        )
        if (showLevelPopup) {
            WindowListPopup(
                show = showLevelPopup,
                alignment = PopupPositionProvider.Align.BottomEnd,
                onDismissRequest = onDismissPopup,
                enableWindowDim = false
            ) {
                ListPopupColumn {
                    levelOptions.forEachIndexed { idx, option ->
                        DropdownImpl(
                            text = option,
                            optionSize = levelOptions.size,
                            isSelected = selectedLevel == option,
                            index = idx,
                            onSelectedIndexChange = onLevelSelected
                        )
                    }
                }
            }
        }
    }
}

private data class SkillDescriptionRichText(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

@Composable
private fun GuideSkillDescriptionText(
    description: String,
    glossaryIcons: Map<String, String>,
    descriptionIcons: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val textColor = MiuixTheme.colorScheme.onBackground
    val richText = remember(description, glossaryIcons, descriptionIcons, textColor) {
        buildSkillDescriptionRichText(
            description = description,
            glossaryIcons = glossaryIcons,
            leadingIcons = descriptionIcons,
            numberColor = Color(0xFFD84A40)
        )
    }
    BasicText(
        text = richText.text,
        inlineContent = richText.inlineContent,
        style = TextStyle(
            color = textColor,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        modifier = modifier
    )
}

private fun buildSkillDescriptionRichText(
    description: String,
    glossaryIcons: Map<String, String>,
    leadingIcons: List<String>,
    numberColor: Color
): SkillDescriptionRichText {
    if (description.isBlank()) {
        return SkillDescriptionRichText(AnnotatedString(""), emptyMap())
    }

    val numberRegex = Regex("""(?<![A-Za-z])[-+]?\d+(?:\.\d+)?%?""")
    val glossary = glossaryIcons
        .filter { (label, icon) -> label.isNotBlank() && icon.isNotBlank() }
        .entries
        .sortedByDescending { it.key.length }

    val inlineContent = linkedMapOf<String, InlineTextContent>()
    var inlineCounter = 0
    val normalizedDescription = normalizeGlossaryToken(description)
    val fuzzyLeadingIcons = glossary
        .asSequence()
        .filter { entry ->
            val label = entry.key
            if (description.contains(label)) return@filter false
            val normalizedLabel = normalizeGlossaryToken(label)
            normalizedLabel.isNotBlank() && normalizedDescription.contains(normalizedLabel)
        }
        .map { it.value }
        .distinct()
        .take(6)
        .toList()
    val prefixIcons = (leadingIcons + fuzzyLeadingIcons)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)
    val text = buildAnnotatedString {
        prefixIcons
            .forEach { iconUrl ->
                val inlineId = "skill_icon_prefix_$inlineCounter"
                inlineCounter += 1
                inlineContent[inlineId] = InlineTextContent(
                    Placeholder(
                        width = 15.sp,
                        height = 15.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 15.dp,
                        iconHeight = 15.dp
                    )
                }
                appendInlineContent(inlineId, "[图标]")
                append(" ")
            }
        var index = 0
        while (index < description.length) {
            val glossaryMatch = glossary
                .mapNotNull { entry ->
                    val start = description.indexOf(entry.key, index)
                    if (start < 0) null else Triple(start, entry.key, entry.value)
                }
                .minByOrNull { it.first }
            val numberMatch = numberRegex.find(description, index)

            val nextIsGlossary = when {
                glossaryMatch == null -> false
                numberMatch == null -> true
                else -> glossaryMatch.first <= numberMatch.range.first
            }

            if (glossaryMatch == null && numberMatch == null) {
                append(description.substring(index))
                break
            }

            if (nextIsGlossary) {
                val (start, label, iconUrl) = glossaryMatch ?: continue
                if (start > index) {
                    append(description.substring(index, start))
                }
                val inlineId = "skill_icon_$inlineCounter"
                inlineCounter += 1
                inlineContent[inlineId] = InlineTextContent(
                    Placeholder(
                        width = 15.sp,
                        height = 15.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 15.dp,
                        iconHeight = 15.dp
                    )
                }
                appendInlineContent(inlineId, "[图标]")
                append(label)
                index = start + label.length
            } else {
                val number = numberMatch ?: continue
                if (number.range.first > index) {
                    append(description.substring(index, number.range.first))
                }
                withStyle(SpanStyle(color = numberColor, fontWeight = FontWeight.SemiBold)) {
                    append(number.value)
                }
                index = number.range.last + 1
            }
        }
    }
    return SkillDescriptionRichText(text, inlineContent)
}

private fun normalizeGlossaryToken(raw: String): String {
    return raw
        .replace(Regex("""[\s\u3000]"""), "")
        .replace(Regex("""[，。、“”‘’：:；;（）()【】\[\]《》<>·•\-—_+*/\\|!?！？]"""), "")
        .lowercase()
}

@Composable
private fun GuideWeaponStarBadgeRow(
    starLabel: String,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val count = parseWeaponStarCount(starLabel)
    if (count <= 0) {
        Text(
            text = starLabel,
            color = Color(0xFFEC4899)
        )
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count.coerceAtMost(5)) {
            Image(
                painter = painterResource(R.drawable.ba_weapon_star_badge),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

private fun parseWeaponStarCount(starLabel: String): Int {
    return Regex("""(\d{1,2})""")
        .find(starLabel)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: 0
}

fun showLoadingText(loading: Boolean, hasInfo: Boolean): Boolean {
    return loading && hasInfo
}
