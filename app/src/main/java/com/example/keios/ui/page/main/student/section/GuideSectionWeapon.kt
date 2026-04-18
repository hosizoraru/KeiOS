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
import androidx.compose.animation.core.Animatable
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
import com.example.keios.ui.page.main.widget.appMotionFloatState
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
import com.example.keios.ui.page.main.widget.AppDropdownSelector
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
import java.util.concurrent.ConcurrentHashMap

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
                                val valueMaxLines = adaptiveValueMaxLines(valueText, valueCharBudget)

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
internal fun GuidePressableMediaSurface(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by appMotionFloatState(
        targetValue = if (pressed) 0.994f else 1f,
        durationMillis = 120,
        label = "guide_media_press_scale"
    )
    val pressOverlayAlpha by appMotionFloatState(
        targetValue = if (pressed) 0.065f else 0f,
        durationMillis = 130,
        label = "guide_media_press_overlay"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        content()
        if (pressOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E3A8A).copy(alpha = pressOverlayAlpha))
            )
        }
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

@Composable
internal fun GuideEffectLevelPicker(
    backdrop: Backdrop?,
    levelOptions: List<String>,
    selectedLevel: String,
    showLevelPopup: Boolean,
    onTogglePopup: () -> Unit,
    onDismissPopup: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    if (levelOptions.isEmpty()) return
    var levelPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    AppDropdownSelector(
        selectedText = selectedLevel,
        options = levelOptions,
        selectedIndex = levelOptions.indexOf(selectedLevel).coerceAtLeast(0),
        expanded = showLevelPopup,
        anchorBounds = levelPopupAnchorBounds,
        onExpandedChange = { expanded ->
            if (expanded) onTogglePopup() else onDismissPopup()
        },
        onSelectedIndexChange = onLevelSelected,
        onAnchorBoundsChange = { levelPopupAnchorBounds = it },
        backdrop = backdrop,
        variant = GlassVariant.Compact
    )
}

internal data class SkillDescriptionRichText(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

@Composable
internal fun GuideSkillDescriptionText(
    description: String,
    glossaryIcons: Map<String, String>,
    descriptionIcons: List<String> = emptyList(),
    onLineCountChanged: ((Int) -> Unit)? = null,
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
        onTextLayout = { layoutResult ->
            onLineCountChanged?.invoke(layoutResult.lineCount)
        },
        style = TextStyle(
            color = textColor,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        modifier = modifier
    )
}

internal fun buildSkillDescriptionRichText(
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

internal fun normalizeGlossaryToken(raw: String): String {
    return raw
        .replace(Regex("""[\s\u3000]"""), "")
        .replace(Regex("""[，。、“”‘’：:；;（）()【】\[\]《》<>·•\-—_+*/\\|!?！？]"""), "")
        .lowercase()
}

@Composable
internal fun GuideWeaponStarBadgeRow(
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

internal fun parseWeaponStarCount(starLabel: String): Int {
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
