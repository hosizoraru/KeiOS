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
internal fun adaptiveValueMaxLines(value: String, lineCharBudget: Int): Int {
    val normalized = value.trim()
    if (normalized.isBlank()) return 1
    if (normalized.contains('\n')) return 2
    val budget = lineCharBudget.coerceIn(10, 52)
    val breakable = normalized.any {
        it == ' ' || it == '/' || it == '-' || it == ',' || it == '，' || it == '、' || it == ':' || it == '：'
    }
    val veryLong = normalized.length > budget + budget / 2
    val longAndBreakable = breakable && normalized.length > budget
    return if (veryLong || longAndBreakable) 2 else 1
}

internal fun buildGuideCopyPayload(key: String, value: String): String {
    return buildTextCopyPayload(key, value)
}

internal fun buildGuideSkillCopyPayload(
    name: String,
    skillType: String,
    level: String,
    cost: String,
    desc: String,
    stateTags: List<String>,
    variantBadge: String?
): String {
    val headerParts = buildList {
        add(name.ifBlank { "技能" })
        skillType.trim().takeIf { it.isNotBlank() }?.let(::add)
        variantBadge?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        level.trim().takeIf { it.isNotBlank() }?.let(::add)
        cost.trim().takeIf { it.isNotBlank() }?.let { add("COST:$it") }
    }
    val stateText = stateTags
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" / ")
    return buildString {
        append(headerParts.joinToString(" · ").ifBlank { "技能" })
        if (stateText.isNotBlank()) {
            append('\n')
            append("状态：")
            append(stateText)
        }
        if (desc.isNotBlank()) {
            append('\n')
            append(desc.trim())
        }
    }
}

internal fun buildGuideVoiceEntryCopyPayload(
    section: String,
    title: String,
    voiceLines: List<Pair<String, String>>
): String {
    val header = buildString {
        append(section.trim().ifBlank { "语音" })
        append(" · ")
        append(title.trim().ifBlank { "语音条目" })
    }
    val lines = voiceLines
        .mapNotNull { (label, text) ->
            val lineText = text.trim()
            if (lineText.isBlank()) null else "${label.trim().ifBlank { "台词" }}：$lineText"
        }
    if (lines.isEmpty()) return header
    return buildString {
        append(header)
        append('\n')
        append(lines.joinToString("\n"))
    }
}

internal fun buildGuideWeaponCopyPayload(
    name: String,
    level: String,
    desc: String
): String {
    return buildString {
        append(name.trim().ifBlank { "专属武器" })
        level.trim().takeIf { it.isNotBlank() }?.let {
            append(" · ")
            append(it)
        }
        if (desc.isNotBlank()) {
            append('\n')
            append(desc.trim())
        }
    }
}

@Composable
internal fun rememberGuideCopyAction(copyPayload: String): () -> Unit {
    val quickCopyAction = rememberLightTextCopyAction(copyPayload)
    return remember(quickCopyAction) {
        { quickCopyAction?.invoke() }
    }
}

internal fun Modifier.guideCopyable(
    copyPayload: String,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    this.copyModeAwareRow(
        copyPayload = copyPayload,
        onClick = onClick
    )
}

@Composable
fun GuideProfileMetaLine(item: BaGuideMetaItem) {
    val isPosition = item.title == "位置"
    val isRarity = item.title == "稀有度"
    val inlineTitleIcon = isRarity || item.title == "学院"
    val summary = if (isPosition) "" else item.value.ifBlank { "-" }
    val rowCopyAction = rememberGuideCopyAction(buildGuideCopyPayload(item.title, summary))
    val iconSlotWidth = 34.dp
    val iconSlotHeight = 24.dp
    val iconWidth = when {
        isRarity -> 30.dp
        isPosition -> 30.dp
        else -> 20.dp
    }
    val iconHeight = when {
        isRarity -> 24.dp
        else -> 20.dp
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val titleMaxWidth = if (inlineTitleIcon) {
            (maxWidth * 0.34f).coerceIn(68.dp, 136.dp)
        } else {
            (maxWidth * 0.42f).coerceIn(80.dp, 176.dp)
        }

        CopyModeSelectionContainer {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .copyModeAwareRow(
                        copyPayload = buildGuideCopyPayload(item.title, summary),
                        onLongClick = rowCopyAction
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (inlineTitleIcon) {
                    Row(
                        modifier = Modifier.widthIn(max = titleMaxWidth),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        if (item.imageUrl.isNotBlank()) {
                            GuideRemoteIcon(
                                imageUrl = item.imageUrl,
                                iconWidth = iconWidth,
                                iconHeight = iconHeight
                            )
                        }
                    }
                } else {
                    val hasLeadingIcon = item.imageUrl.isNotBlank()
                    Row(
                        modifier = Modifier.widthIn(max = titleMaxWidth),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        if (hasLeadingIcon) {
                            Box(
                                modifier = Modifier
                                    .width(iconSlotWidth)
                                    .height(iconSlotHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                GuideRemoteIcon(
                                    imageUrl = item.imageUrl,
                                    iconWidth = iconWidth,
                                    iconHeight = iconHeight
                                )
                            }
                        }
                    }
                }
                if (!isPosition) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = summary,
                            color = MiuixTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Clip
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GuideCombatMetaTile(
    item: BaGuideMetaItem,
    modifier: Modifier = Modifier
) {
    val value = item.value.ifBlank { "-" }
    val rowCopyAction = rememberGuideCopyAction(buildGuideCopyPayload(item.title, value))
    val adaptiveWide = item.title.contains("战术") || item.title == "武器类型"
    val iconWidth = if (adaptiveWide) 28.dp else 18.dp
    val iconHeight = if (adaptiveWide) 18.dp else 18.dp
    val extraIconWidth = 30.dp
    val extraIconHeight = 18.dp
    val iconSlotWidth = 30.dp
    val iconSlotHeight = 22.dp
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .copyModeAwareRow(
                copyPayload = buildGuideCopyPayload(item.title, value),
                onLongClick = rowCopyAction
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.36f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val trailingSlotsWidth = if (item.extraImageUrl.isNotBlank()) iconSlotWidth * 2 else iconSlotWidth
        val titleMaxWidth = ((maxWidth - trailingSlotsWidth) * if (adaptiveWide) 0.52f else 0.46f)
            .coerceIn(86.dp, 180.dp)
        val valueCharBudget = ((maxWidth - titleMaxWidth - trailingSlotsWidth).value / 7.2f)
            .toInt()
            .coerceAtLeast(10)
        val valueMaxLines = adaptiveValueMaxLines(value, valueCharBudget)

        CopyModeSelectionContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.widthIn(max = titleMaxWidth),
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = value,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = valueMaxLines,
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
    }
}
