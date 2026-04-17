package com.example.keios.ui.page.main.student

import com.example.keios.ui.page.main.widget.GlassVariant
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.keios.R
import com.example.keios.ui.page.main.ba.BASettingsStore
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.CopyModeSelectionContainer
import com.example.keios.ui.page.main.widget.buildTextCopyPayload
import com.example.keios.ui.page.main.widget.copyModeAwareRow
import com.example.keios.ui.page.main.widget.rememberLightTextCopyAction
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.ContinuousTransformType
import com.github.panpf.zoomimage.zoom.GestureType
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import java.util.concurrent.ConcurrentHashMap

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
    val displaySkillType = parsedSkillType.baseType
    val skillTypeStateTags = parsedSkillType.stateTags
    val skillTypeVariantBadge = parsedSkillType.variantIndex?.let(::toGuideCircledNumber)
    val hasTypeStateBlock = skillTypeStateTags.isNotEmpty()
    val hasTypeSubRow = skillTypeVariantBadge != null || levelOptions.isNotEmpty()
    val isExSkill = remember(card.type) { card.type.contains("EX", ignoreCase = true) }
    val hasSkillMetaColumn = remember(displaySkillType, skillCost, levelOptions, skillTypeVariantBadge, skillTypeStateTags) {
        displaySkillType.isNotBlank() ||
            skillCost.isNotBlank() ||
            levelOptions.isNotEmpty() ||
            skillTypeVariantBadge != null ||
            skillTypeStateTags.isNotEmpty()
    }
    val metaColumnShouldTopAlign = descriptionLineCount >= 3
    val skillNameTooLong = skillNameLineCount > 1
    val typeAlignToTitleOffset = if (
        displaySkillType.isNotBlank() &&
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
        displaySkillType.isNotBlank() -> {
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
    val skillCopyPayload = remember(
        card.name,
        displaySkillType,
        displayLevel,
        skillCost,
        skillDesc,
        skillTypeStateTags,
        skillTypeVariantBadge
    ) {
        buildGuideSkillCopyPayload(
            name = card.name,
            skillType = displaySkillType,
            level = displayLevel,
            cost = skillCost,
            desc = skillDesc,
            stateTags = skillTypeStateTags,
            variantBadge = skillTypeVariantBadge
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
                            text = card.name,
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
                        description = skillDesc.ifBlank { "暂未解析到技能描述。" },
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
                        if (displaySkillType.isNotBlank()) {
                            if (typeAlignToTitleOffset > 0.dp) {
                                Spacer(modifier = Modifier.height(typeAlignToTitleOffset))
                            }
                            Box(modifier = Modifier.onSizeChanged { typeCapsuleHeightPx = it.height }) {
                                GlassTextButton(
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
                                    Box(
                                        modifier = Modifier.capturePopupAnchor { levelPopupAnchorBounds = it }
                                    ) {
                                        GlassTextButton(
                                            backdrop = backdrop,
                                            text = displayLevel,
                                            variant = GlassVariant.Compact,
                                            minHeight = 30.dp,
                                            horizontalPadding = 10.dp,
                                            verticalPadding = 6.dp,
                                            onClick = { showLevelPopup = !showLevelPopup }
                                        )
                                        if (showLevelPopup) {
                                            SnapshotWindowListPopup(
                                                show = showLevelPopup,
                                                alignment = PopupPositionProvider.Align.BottomEnd,
                                                anchorBounds = levelPopupAnchorBounds,
                                                placement = SnapshotPopupPlacement.ButtonEnd,
                                                onDismissRequest = { showLevelPopup = false },
                                                enableWindowDim = false
                                            ) {
                                                LiquidDropdownColumn {
                                                    levelOptions.forEachIndexed { index, option ->
                                                        LiquidDropdownImpl(
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
                        if (costAlignToDescriptionOffset > 0.dp && skillCost.isNotBlank()) {
                            Spacer(modifier = Modifier.height(costAlignToDescriptionOffset))
                        }
                        if (skillCost.isNotBlank()) {
                            GlassTextButton(
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

internal data class GuideSkillTypeMeta(
    val baseType: String,
    val variantIndex: Int? = null,
    val stateTags: List<String> = emptyList()
)

internal fun parseGuideSkillTypeMeta(raw: String): GuideSkillTypeMeta {
    val cleaned = sanitizeGuideSkillLabelForDisplay(raw).trim()
    if (cleaned.isBlank()) return GuideSkillTypeMeta(baseType = "")

    var variantIndex: Int? = null
    val stateTags = mutableListOf<String>()
    val stateCandidates = guideSkillTypeBracketPattern
        .findAll(cleaned)
        .map { it.groupValues.getOrElse(1) { "" }.trim() }
        .filter { it.isNotBlank() }
        .toList()

    stateCandidates.forEach { candidate ->
        val tokens = candidate
            .split(guideSkillTypeStateSplitPattern)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(candidate) }
        tokens.forEach { token ->
            val tokenMeta = parseGuideSkillTypeToken(token)
            if (variantIndex == null && tokenMeta.variantIndex != null) {
                variantIndex = tokenMeta.variantIndex
            }
            val tag = normalizeGuideSkillStateTag(tokenMeta.base.ifBlank { token.trim() })
            if (tag.isNotBlank()) {
                stateTags += tag
            }
        }
    }

    val baseCandidate = guideSkillTypeBracketPattern
        .replace(cleaned, "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '-', '_', '/', '／', '|', '｜')
        .trim()
    val baseMeta = parseGuideSkillTypeToken(baseCandidate.ifBlank { cleaned })
    if (variantIndex == null) {
        variantIndex = baseMeta.variantIndex
    }

    return GuideSkillTypeMeta(
        baseType = baseMeta.base.ifBlank { cleaned },
        variantIndex = variantIndex,
        stateTags = stateTags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    )
}

internal data class GuideSkillTypeTokenMeta(
    val base: String,
    val variantIndex: Int? = null
)

internal fun normalizeGuideSkillStateTag(raw: String): String {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return ""
    val compact = cleaned.replace(" ", "").replace("　", "")
    return if (
        compact.startsWith("对") &&
        compact.endsWith("使用") &&
        compact.length > 3
    ) {
        compact.removeSuffix("使用")
    } else {
        cleaned
    }
}

internal fun parseGuideSkillTypeToken(raw: String): GuideSkillTypeTokenMeta {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return GuideSkillTypeTokenMeta(base = "")

    val circledMatch = guideSkillTypeCircledSuffixPattern.matchEntire(cleaned)
    if (circledMatch != null) {
        val base = circledMatch.groupValues[1].trim()
        val circled = circledMatch.groupValues[2]
        val index = guideCircledNumbers.indexOf(circled).takeIf { it >= 0 }?.plus(1)
        return GuideSkillTypeTokenMeta(
            base = if (base.isBlank()) cleaned else base,
            variantIndex = index
        )
    }

    val numericMatch = guideSkillTypeNumericSuffixPattern.matchEntire(cleaned)
    if (numericMatch != null) {
        val base = numericMatch.groupValues[1].trim()
        val index = numericMatch.groupValues[2].toIntOrNull()?.takeIf { it > 0 }
        if (index != null) {
            return GuideSkillTypeTokenMeta(
                base = if (base.isBlank()) cleaned else base,
                variantIndex = index
            )
        }
    }

    return GuideSkillTypeTokenMeta(base = cleaned)
}

internal fun toGuideCircledNumber(index: Int): String {
    return guideCircledNumbers.getOrNull(index - 1) ?: index.toString()
}

@Composable
internal fun GuideSkillVariantBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color(0x223B82F6))
            .border(width = 1.dp, color = Color(0x663B82F6), shape = CircleShape),
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

@Composable
internal fun GuideSkillStateTagButton(
    label: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    GlassTextButton(
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
