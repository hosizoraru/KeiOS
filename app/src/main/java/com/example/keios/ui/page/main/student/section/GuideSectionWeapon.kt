package com.example.keios.ui.page.main.student.section

import com.example.keios.ui.page.main.widget.glass.GlassVariant
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.student.GuideRemoteIcon
import com.example.keios.ui.page.main.student.GuideRemoteImage
import com.example.keios.ui.page.main.student.GuideWeaponCardModel
import com.example.keios.ui.page.main.student.GuideWeaponStarEffect
import com.example.keios.ui.page.main.student.GuideWeaponStatRow
import com.example.keios.ui.page.main.student.section.gallery.GuideImageFullscreenDialog
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.motion.appMotionFloatState
import com.example.keios.ui.page.main.widget.support.CopyModeSelectionContainer
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import com.example.keios.ui.page.main.widget.glass.AppDropdownSelector
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    var showImageFullscreen by remember(card.imageUrl) { mutableStateOf(false) }

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
    val weaponCopyPayload = remember(card.name, selectedLevel, card.description) {
        buildGuideWeaponCopyPayload(
            name = card.name,
            level = selectedLevel,
            desc = card.description
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        CopyModeSelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideCopyable(weaponCopyPayload)
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
                        variant = GlassVariant.Compact,
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
                    GuidePressableMediaSurface(
                        onClick = { showImageFullscreen = true }
                    ) {
                        GuideRemoteImage(
                            imageUrl = card.imageUrl,
                            imageHeight = 132.dp
                        )
                    }
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
                                var levelPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
                                AppDropdownSelector(
                                    selectedText = selectedLevel,
                                    options = levelOptions,
                                    selectedIndex = levelOptions.indexOf(selectedLevel).coerceAtLeast(0),
                                    expanded = showLevelPopup,
                                    anchorBounds = levelPopupAnchorBounds,
                                    onExpandedChange = { showLevelPopup = it },
                                    onSelectedIndexChange = { selected ->
                                        selectedLevel = levelOptions[selected]
                                    },
                                    onAnchorBoundsChange = { levelPopupAnchorBounds = it },
                                    backdrop = backdrop,
                                    variant = GlassVariant.Compact
                                )
                            }
                        }

                        card.statRows.forEach { stat ->
                            val valueText = levelValue(stat)
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val titleMaxWidth = (maxWidth * 0.34f).coerceIn(64.dp, 128.dp)
                                val valueCharBudget = ((maxWidth - titleMaxWidth).value / 7f).toInt().coerceAtLeast(10)
                                val valueMaxLines =
                                    adaptiveValueMaxLines(valueText, valueCharBudget)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .guideCopyable(buildGuideCopyPayload(stat.title, valueText)),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stat.title,
                                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                                        modifier = Modifier.widthIn(max = titleMaxWidth),
                                        maxLines = 2,
                                        overflow = TextOverflow.Clip
                                    )
                                    Text(
                                        text = valueText,
                                        color = MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f),
                                        maxLines = valueMaxLines,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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

    if (showImageFullscreen && card.imageUrl.isNotBlank()) {
        GuideImageFullscreenDialog(
            imageUrl = card.imageUrl,
            onDismiss = { showImageFullscreen = false }
        )
    }
}

@Composable
internal fun GuideWeaponStarEffectItem(
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
    val effectCopyPayload = remember(
        effect.starLabel,
        effect.name,
        effect.roleTag,
        selectedLevel,
        desc
    ) {
        buildString {
            append(effect.starLabel.ifBlank { "★" })
            append(" · ")
            append(effect.name.ifBlank { "效果" })
            effect.roleTag.trim().takeIf { it.isNotBlank() }?.let {
                append('\n')
                append("分类：")
                append(it)
            }
            selectedLevel.trim().takeIf { it.isNotBlank() }?.let {
                append('\n')
                append("等级：")
                append(it)
            }
            if (desc.isNotBlank()) {
                append('\n')
                append(desc)
            }
        }
    }

    if (effect.starLabel == "★2") {
        GuideWeaponTwoStarEffectItem(
            effect = effect,
            desc = desc,
            copyPayload = effectCopyPayload,
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

    CopyModeSelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guideCopyable(effectCopyPayload)
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
                        variant = GlassVariant.Compact,
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
}

@Composable
internal fun GuideWeaponTwoStarEffectItem(
    effect: GuideWeaponStarEffect,
    desc: String,
    copyPayload: String,
    glossaryIcons: Map<String, String>,
    backdrop: Backdrop?,
    levelOptions: List<String>,
    selectedLevel: String,
    showLevelPopup: Boolean,
    onTogglePopup: () -> Unit,
    onDismissPopup: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    CopyModeSelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guideCopyable(copyPayload)
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
                        variant = GlassVariant.Compact,
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
}
