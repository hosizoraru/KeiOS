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
fun GuideVoiceLanguageCard(
    headers: List<String>,
    selectedHeader: String,
    backdrop: Backdrop?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleHeaders = headers
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { listOf("日配", "中配") }
    val voiceHeaderCopyPayload = remember(visibleHeaders, selectedHeader) {
        val current = selectedHeader.trim().ifBlank { visibleHeaders.firstOrNull().orEmpty() }
        buildGuideCopyPayload("配音", current.ifBlank { visibleHeaders.joinToString(" / ") })
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
                    .guideCopyable(voiceHeaderCopyPayload)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "配音",
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.widthIn(min = 34.dp)
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleHeaders.forEach { header ->
                        val selected = header.equals(selectedHeader.trim(), ignoreCase = true)
                        GlassTextButton(
                            backdrop = backdrop,
                            text = header,
                            textColor = if (selected) Color(0xFF2563EB) else MiuixTheme.colorScheme.onBackgroundVariant,
                            containerColor = if (selected) Color(0x443B82F6) else null,
                            variant = GlassVariant.Compact,
                            onClick = { onSelected(header) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuideVoiceEntryCard(
    entry: BaGuideVoiceEntry,
    languageHeaders: List<String>,
    backdrop: Backdrop?,
    playbackUrl: String,
    isPlaying: Boolean,
    playProgress: Float,
    onTogglePlay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceLines = buildVoiceLinePairsForCard(entry, languageHeaders)
    val entryCopyPayload = remember(entry.section, entry.title, voiceLines) {
        buildGuideVoiceEntryCopyPayload(
            section = entry.section,
            title = entry.title,
            voiceLines = voiceLines
        )
    }
    val normalizedPlaybackUrl = normalizeGuideMediaSource(playbackUrl)
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
                    .guideCopyable(entryCopyPayload)
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
                            variant = GlassVariant.Compact,
                            onClick = {}
                        )
                    }
                    Text(
                        text = entry.title.ifBlank { "语音条目" },
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (normalizedPlaybackUrl.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPlaying) {
                                CircularProgressIndicator(
                                    progress = playProgress.coerceIn(0f, 1f),
                                    size = 18.dp,
                                    strokeWidth = 2.dp,
                                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                        foregroundColor = Color(0xFF3B82F6),
                                        backgroundColor = Color(0x553B82F6)
                                    )
                                )
                            }
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "",
                                leadingIcon = if (isPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                                textColor = Color(0xFF3B82F6),
                                variant = GlassVariant.Compact,
                                onClick = { onTogglePlay(normalizedPlaybackUrl) }
                            )
                        }
                    }
                }

                voiceLines.forEach { (label, text) ->
                    val lineCopyPayload = remember(label, text) {
                        buildGuideCopyPayload(label, text)
                    }
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val labelMaxWidth = (maxWidth * 0.28f).coerceIn(52.dp, 92.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .guideCopyable(lineCopyPayload),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = label,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                modifier = Modifier.widthIn(max = labelMaxWidth),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                            Text(
                                text = text,
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun canonicalVoiceLineLabelInCard(raw: String): String {
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

internal fun voiceLinePriorityForCard(label: String): Int {
    return when (canonicalVoiceLineLabelInCard(label)) {
        "日配" -> 0
        "中配" -> 1
        "官翻" -> 2
        "韩配" -> 3
        else -> 4
    }
}

internal fun buildVoiceLinePairsForCard(
    entry: BaGuideVoiceEntry,
    fallbackHeaders: List<String>
): List<Pair<String, String>> {
    val explicitPairs = if (
        entry.lineHeaders.size == entry.lines.size &&
        entry.lineHeaders.any { it.trim().isNotBlank() }
    ) {
        entry.lineHeaders.zip(entry.lines)
    } else {
        emptyList()
    }
    val rawPairs = if (explicitPairs.isNotEmpty()) {
        explicitPairs
    } else {
        entry.lines.mapIndexed { index, line ->
            val label = fallbackHeaders.getOrNull(index).orEmpty().ifBlank { "台词${index + 1}" }
            label to line
        }
    }
    return rawPairs.withIndex()
        .mapNotNull { indexed ->
            val label = indexed.value.first.trim()
            val text = indexed.value.second.trim()
            if (text.isBlank()) return@mapNotNull null
            val normalizedLabel = canonicalVoiceLineLabelInCard(label).ifBlank {
                label.ifBlank { "台词${indexed.index + 1}" }
            }
            Triple(normalizedLabel, text, indexed.index)
        }
        .sortedWith(
            compareBy<Triple<String, String, Int>> { item ->
                voiceLinePriorityForCard(item.first)
            }.thenBy { item ->
                item.third
            }
        )
        .map { item -> item.first to item.second }
        .ifEmpty { listOf("台词" to "暂无台词文本") }
}
