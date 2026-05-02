package os.kei.ui.page.main.student.section

import os.kei.ui.page.main.widget.glass.GlassVariant
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.R
import os.kei.ui.page.main.student.GuideRemoteIcon
import os.kei.ui.page.main.student.GuideSkillCardModel
import os.kei.ui.page.main.student.component.GuideLiquidCard
import os.kei.ui.page.main.student.guideLocalizedLabel
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.support.CopyModeSelectionContainer
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.LiquidSurface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GuideSkillCardItem(
    card: GuideSkillCardModel,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    var showLevelPopup by remember(card.id) { mutableStateOf(false) }
    var levelPopupAnchorBounds by remember(card.id) { mutableStateOf<IntRect?>(null) }
    var skillTitleRowHeightPx by remember(card.id) { mutableStateOf(0) }
    val levelOptions = card.levelOptions
    var selectedLevel by rememberSaveable(card.id) { mutableStateOf(card.defaultLevel) }
    var typeCapsuleHeightPx by remember(card.id, selectedLevel) { mutableStateOf(0) }
    var typeStateBlockHeightPx by remember(card.id, selectedLevel) { mutableStateOf(0) }
    var typeSubRowHeightPx by remember(card.id, selectedLevel) { mutableStateOf(0) }
    var skillNameLineCount by remember(card.id, selectedLevel) { mutableStateOf(1) }
    var descriptionLineCount by remember(card.id, selectedLevel) { mutableStateOf(1) }
    val density = LocalDensity.current

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
    val parsedSkillType = remember(card.type) { parseGuideSkillTypeMeta(card.type) }
    val fallbackSkillLabel = stringResource(R.string.guide_label_skill)
    val rawDisplaySkillType = parsedSkillType.baseType
    val displaySkillType = if (rawDisplaySkillType.isBlank()) {
        fallbackSkillLabel
    } else {
        guideLocalizedLabel(rawDisplaySkillType)
    }
    val displaySkillName = card.name.ifBlank { stringResource(R.string.guide_skill_unnamed) }
    val skillTypeStateTags = parsedSkillType.stateTags
    val skillTypeVariantBadge = parsedSkillType.variantIndex?.let(::toGuideCircledNumber)
    val hasTypeStateBlock = skillTypeStateTags.isNotEmpty()
    val hasTypeSubRow = skillTypeVariantBadge != null || levelOptions.isNotEmpty()
    val isExSkill = remember(card.type) { card.type.contains("EX", ignoreCase = true) }
    val hasSkillMetaColumn = remember(rawDisplaySkillType, skillCost, levelOptions, skillTypeVariantBadge, skillTypeStateTags) {
        rawDisplaySkillType.isNotBlank() ||
            skillCost.isNotBlank() ||
            levelOptions.isNotEmpty() ||
            skillTypeVariantBadge != null ||
            skillTypeStateTags.isNotEmpty()
    }
    val metaColumnShouldTopAlign = descriptionLineCount >= 3
    val skillNameTooLong = skillNameLineCount > 1
    val typeAlignToTitleOffset = if (
        rawDisplaySkillType.isNotBlank() &&
        !skillNameTooLong &&
        skillTitleRowHeightPx > 0 &&
        typeCapsuleHeightPx > 0
    ) {
        with(density) { ((skillTitleRowHeightPx - typeCapsuleHeightPx).coerceAtLeast(0) / 2).toDp() }
    } else {
        0.dp
    }
    val descriptionTopOffsetDp = with(density) { skillTitleRowHeightPx.toDp() } + 8.dp
    val stateBlockHeightDp = if (hasTypeStateBlock) {
        if (typeStateBlockHeightPx > 0) {
            with(density) { typeStateBlockHeightPx.toDp() }
        } else {
            26.dp
        }
    } else {
        0.dp
    }
    val subRowHeightDp = if (hasTypeSubRow) {
        if (typeSubRowHeightPx > 0) {
            with(density) { typeSubRowHeightPx.toDp() }
        } else {
            30.dp
        }
    } else {
        0.dp
    }
    val occupiedBeforeCostDp = when {
        rawDisplaySkillType.isNotBlank() -> {
            val typeBottomSpacing = when {
                hasTypeStateBlock && hasTypeSubRow -> 4.dp + stateBlockHeightDp + 4.dp + subRowHeightDp + 4.dp
                hasTypeStateBlock -> 4.dp + stateBlockHeightDp + 4.dp
                hasTypeSubRow -> 4.dp + subRowHeightDp + 4.dp
                else -> 4.dp
            }
            typeAlignToTitleOffset + with(density) { typeCapsuleHeightPx.toDp() } + typeBottomSpacing
        }
        hasTypeStateBlock && hasTypeSubRow -> stateBlockHeightDp + 4.dp + subRowHeightDp + 4.dp
        hasTypeStateBlock -> stateBlockHeightDp + 4.dp
        hasTypeSubRow -> subRowHeightDp + 4.dp
        else -> 0.dp
    }
    val costAlignToDescriptionOffset = if (metaColumnShouldTopAlign) {
        (descriptionTopOffsetDp - occupiedBeforeCostDp).coerceAtLeast(0.dp)
    } else {
        0.dp
    }
    val statusLabel = stringResource(R.string.guide_label_status)
    val emptySkillDescription = stringResource(R.string.guide_skill_description_empty)
    val skillCopyPayload = remember(
        displaySkillName,
        displaySkillType,
        displayLevel,
        skillCost,
        skillDesc,
        skillTypeStateTags,
        skillTypeVariantBadge,
        fallbackSkillLabel,
        statusLabel
    ) {
        buildGuideSkillCopyPayload(
            name = displaySkillName,
            skillType = displaySkillType,
            level = displayLevel,
            cost = skillCost,
            desc = skillDesc,
            stateTags = skillTypeStateTags,
            variantBadge = skillTypeVariantBadge,
            fallbackSkill = fallbackSkillLabel,
            statusLabel = statusLabel
        )
    }

    GuideLiquidCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = Color(0x223B82F6),
        onClick = {}
    ) {
        CopyModeSelectionContainer {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideCopyable(skillCopyPayload)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { skillTitleRowHeightPx = it.height },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (card.iconUrl.isNotBlank()) {
                            GuideRemoteIcon(
                                imageUrl = card.iconUrl,
                                modifier = Modifier.alignBy { it.measuredHeight / 2 },
                                iconWidth = 34.dp,
                                iconHeight = 34.dp
                            )
                        }
                        Text(
                            text = displaySkillName,
                            modifier = Modifier
                                .weight(1f)
                                .alignBy { it.measuredHeight / 2 },
                            color = MiuixTheme.colorScheme.onBackground,
                            maxLines = if (isExSkill) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { layoutResult ->
                                val safeLineCount = layoutResult.lineCount.coerceAtLeast(1)
                                if (skillNameLineCount != safeLineCount) {
                                    skillNameLineCount = safeLineCount
                                }
                            }
                        )
                    }
                    GuideSkillDescriptionText(
                        description = skillDesc.ifBlank { emptySkillDescription },
                        glossaryIcons = card.glossaryIcons,
                        descriptionIcons = card.descriptionIconsFor(selectedLevel),
                        onLineCountChanged = { lineCount ->
                            val safeLineCount = lineCount.coerceAtLeast(1)
                            if (descriptionLineCount != safeLineCount) {
                                descriptionLineCount = safeLineCount
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (hasSkillMetaColumn) {
                    Column(
                        modifier = Modifier.widthIn(min = 68.dp, max = 90.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (rawDisplaySkillType.isNotBlank()) {
                            if (typeAlignToTitleOffset > 0.dp) {
                                Spacer(modifier = Modifier.height(typeAlignToTitleOffset))
                            }
                            Box(modifier = Modifier.onSizeChanged { typeCapsuleHeightPx = it.height }) {
                                AppLiquidTextButton(
                                    backdrop = backdrop,
                                    text = displaySkillType,
                                    enabled = false,
                                    textColor = Color(0xFF3B82F6),
                                    variant = GlassVariant.Compact,
                                    minHeight = 30.dp,
                                    horizontalPadding = 10.dp,
                                    verticalPadding = 6.dp,
                                    onClick = {}
                                )
                            }
                        }
                        if (hasTypeStateBlock) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { typeStateBlockHeightPx = it.height },
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                skillTypeStateTags.forEach { stateTag ->
                                    GuideSkillStateTagButton(
                                        label = stateTag,
                                        backdrop = backdrop
                                    )
                                }
                            }
                        }
                        if (hasTypeSubRow) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { typeSubRowHeightPx = it.height },
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (skillTypeVariantBadge != null) {
                                    GuideSkillVariantBadge(
                                        label = skillTypeVariantBadge
                                    )
                                }
                                if (levelOptions.isNotEmpty()) {
                                    AppDropdownSelector(
                                        selectedText = displayLevel,
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
                                        variant = GlassVariant.Compact,
                                        minHeight = 30.dp,
                                        horizontalPadding = 10.dp,
                                        verticalPadding = 6.dp
                                    )
                                }
                            }
                        }
                        if (costAlignToDescriptionOffset > 0.dp && skillCost.isNotBlank()) {
                            Spacer(modifier = Modifier.height(costAlignToDescriptionOffset))
                        }
                        if (skillCost.isNotBlank()) {
                            AppLiquidTextButton(
                                backdrop = backdrop,
                                text = "COST:$skillCost",
                                enabled = false,
                                textColor = Color(0xFF3B82F6),
                                variant = GlassVariant.Compact,
                                minHeight = 30.dp,
                                horizontalPadding = 10.dp,
                                verticalPadding = 6.dp,
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GuideSkillVariantBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    val badgeBackdrop = rememberLayerBackdrop()
    Box(
        modifier = modifier.size(26.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(badgeBackdrop)
        )
        LiquidSurface(
            backdrop = badgeBackdrop,
            modifier = Modifier.matchParentSize(),
            shape = RoundedRectangle(999.dp),
            isInteractive = false,
            surfaceColor = Color(0x223B82F6),
            blurRadius = 2.dp,
            lensRadius = 10.dp,
            shadow = false,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color(0xFF3B82F6),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun GuideSkillStateTagButton(
    label: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    AppLiquidTextButton(
        backdrop = backdrop,
        text = label,
        enabled = false,
        textColor = Color(0xFF3B82F6),
        variant = GlassVariant.Compact,
        minHeight = 26.dp,
        horizontalPadding = 8.dp,
        verticalPadding = 5.dp,
        modifier = modifier,
        onClick = {}
    )
}
