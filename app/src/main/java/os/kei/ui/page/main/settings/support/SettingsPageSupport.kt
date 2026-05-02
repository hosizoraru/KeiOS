package os.kei.ui.page.main.settings.support

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.core.prefs.CacheEntrySummary
import os.kei.ui.page.main.widget.core.AppControlRow
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsGroupCard(
    header: String,
    title: String,
    sectionIcon: ImageVector? = null,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    AppFeatureCard(
        title = title,
        subtitle = "",
        eyebrow = header,
        sectionIcon = sectionIcon,
        containerColor = containerColor,
        showIndication = false,
        contentVerticalSpacing = CardLayoutRhythm.denseSectionGap,
        contentPadding = PaddingValues(
            start = CardLayoutRhythm.cardHorizontalPadding,
            end = CardLayoutRhythm.cardHorizontalPadding,
            bottom = CardLayoutRhythm.cardVerticalPadding
        ),
        content = content
    )
}

@Composable
internal fun SettingsActionItem(
    title: String,
    summary: String,
    infoKey: String? = null,
    infoValue: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
    ) {
        AppControlRow(
            title = title,
            summary = summary,
            titleColor = MiuixTheme.colorScheme.onBackground,
            minHeight = 48.dp,
            onClick = onClick,
            trailing = trailing
        )
        if (!infoKey.isNullOrBlank() && !infoValue.isNullOrBlank()) {
            SettingsInfoItem(
                key = infoKey,
                value = infoValue
            )
        }
    }
}

@Composable
internal fun SettingsToggleItem(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    infoKey: String? = null,
    infoValue: String? = null
) {
    SettingsActionItem(
        title = title,
        summary = summary,
        infoKey = infoKey,
        infoValue = infoValue,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            AppSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
internal fun SettingsInfoItem(
    key: String,
    value: String
) {
    AppInfoRow(
        label = key,
        value = value.ifBlank { stringResource(R.string.common_na) },
        labelColor = MiuixTheme.colorScheme.onBackgroundVariant,
        valueColor = MiuixTheme.colorScheme.onBackground,
        labelMinWidth = 64.dp,
        labelMaxWidth = 112.dp,
        horizontalSpacing = CardLayoutRhythm.infoRowGap,
        rowVerticalPadding = CardLayoutRhythm.infoRowVerticalPadding,
        labelMaxLines = 2,
        valueMaxLines = 6,
        valueOverflow = TextOverflow.Ellipsis,
        labelFontSize = AppTypographyTokens.Supporting.fontSize,
        labelLineHeight = AppTypographyTokens.Supporting.lineHeight,
        valueFontSize = AppTypographyTokens.Body.fontSize,
        valueLineHeight = AppTypographyTokens.Body.lineHeight,
        emphasizedValue = false
    )
}

@Composable
internal fun SettingsCacheRow(
    entry: CacheEntrySummary,
    clearing: Boolean,
    onClear: () -> Unit
) {
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f)
    val resetLabel = stringResource(R.string.common_reset)
    val actionColor = if (entry.clearLabel == resetLabel) {
        MiuixTheme.colorScheme.error
    } else {
        MiuixTheme.colorScheme.primary
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
    ) {
        AppControlRow(
            title = entry.title,
            summary = entry.summary,
            titleColor = titleColor,
            minHeight = 48.dp,
            trailing = {
                if (entry.clearLabel.isNotBlank()) {
                    AppStandaloneLiquidTextButton(
                        variant = GlassVariant.Compact,
                        text = if (clearing) stringResource(R.string.common_processing) else entry.clearLabel,
                        textColor = actionColor,
                        containerColor = actionColor,
                        enabled = !clearing,
                        onClick = onClear
                    )
                }
            }
        )
        Text(
            text = entry.detail,
            color = subtitleColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight
        )
        Text(
            text = entry.activity,
            color = subtitleColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight
        )
        Text(
            text = entry.storage,
            color = subtitleColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight
        )
    }
}

internal fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        safe >= gb -> String.format(Locale.US, "%.2f GB", safe / gb)
        safe >= mb -> String.format(Locale.US, "%.2f MB", safe / mb)
        safe >= kb -> String.format(Locale.US, "%.2f KB", safe / kb)
        else -> "${safe.toLong()} B"
    }
}

internal fun formatLogTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return ""
    return runCatching {
        SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
    }.getOrElse { "" }
}

internal fun formatOpacityPercent(alpha: Float): Int {
    return (alpha.coerceIn(0f, 1f) * 100f).roundToInt()
}

internal fun formatMilliseconds(value: Int): Int {
    return value.coerceAtLeast(0)
}

private const val NON_HOME_BACKGROUND_CROP_DIR = "non_home_background"
private const val NON_HOME_BACKGROUND_CROP_FILE_PREFIX = "cropped_non_home_"
private const val NON_HOME_BACKGROUND_CROP_TARGET_SHORT_EDGE = 1440
private const val NON_HOME_BACKGROUND_CROP_MAX_WIDTH = 2560
private const val NON_HOME_BACKGROUND_CROP_MAX_HEIGHT = 4096
internal const val NON_HOME_BACKGROUND_OPACITY_DEFAULT = 0.16f
internal const val NON_HOME_BACKGROUND_OPACITY_MIN = 0.06f
internal const val NON_HOME_BACKGROUND_OPACITY_MAX = 0.40f
internal const val NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD = 0.03f
internal val NON_HOME_BACKGROUND_OPACITY_KEY_POINTS = listOf(
    0.06f,
    0.10f,
    0.13f,
    NON_HOME_BACKGROUND_OPACITY_DEFAULT,
    0.20f,
    0.26f,
    0.33f,
    NON_HOME_BACKGROUND_OPACITY_MAX
)
internal const val SUPER_ISLAND_RESTORE_DELAY_DEFAULT_MS = 100f
internal const val SUPER_ISLAND_RESTORE_DELAY_MIN_MS = 50f
internal const val SUPER_ISLAND_RESTORE_DELAY_MAX_MS = 350f
internal const val SUPER_ISLAND_RESTORE_DELAY_MAGNET_THRESHOLD = 6f
internal val SUPER_ISLAND_RESTORE_DELAY_KEY_POINTS = listOf(
    SUPER_ISLAND_RESTORE_DELAY_MIN_MS,
    75f,
    SUPER_ISLAND_RESTORE_DELAY_DEFAULT_MS,
    125f,
    150f,
    200f,
    250f,
    300f,
    SUPER_ISLAND_RESTORE_DELAY_MAX_MS
)

internal fun createNonHomeBackgroundCropOutputUri(context: Context): Uri {
    val dir = File(context.filesDir, NON_HOME_BACKGROUND_CROP_DIR)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val output = File(
        dir,
        "$NON_HOME_BACKGROUND_CROP_FILE_PREFIX${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        output
    )
}

internal fun resolveNonHomeBackgroundAspectRatio(context: Context): Pair<Float, Float> {
    val metrics = context.resources.displayMetrics
    val widthPx = metrics.widthPixels.coerceAtLeast(1)
    val heightPx = metrics.heightPixels.coerceAtLeast(1)
    return widthPx.toFloat() to heightPx.toFloat()
}

internal fun resolveNonHomeBackgroundCropSize(context: Context): Pair<Int, Int> {
    val metrics = context.resources.displayMetrics
    val widthPx = metrics.widthPixels.coerceAtLeast(1)
    val heightPx = metrics.heightPixels.coerceAtLeast(1)
    val shortEdge = min(widthPx, heightPx).coerceAtLeast(1)
    val upscale = (NON_HOME_BACKGROUND_CROP_TARGET_SHORT_EDGE.toFloat() / shortEdge.toFloat())
        .coerceAtLeast(1f)
    val width = (widthPx * upscale).roundToInt().coerceIn(widthPx, NON_HOME_BACKGROUND_CROP_MAX_WIDTH)
    val height = (heightPx * upscale).roundToInt().coerceIn(heightPx, NON_HOME_BACKGROUND_CROP_MAX_HEIGHT)
    return width to height
}

internal fun deleteManagedNonHomeBackgroundFile(context: Context, uriText: String) {
    if (uriText.isBlank()) return
    val uri = runCatching { uriText.toUri() }.getOrNull() ?: return
    val target = when (uri.scheme) {
        "file" -> File(uri.path ?: return)
        "content" -> resolveManagedNonHomeBackgroundFileByContentUri(context, uri) ?: return
        else -> return
    }
    if (target.name.startsWith(NON_HOME_BACKGROUND_CROP_FILE_PREFIX).not()) return
    if (target.parentFile?.name != NON_HOME_BACKGROUND_CROP_DIR) return
    runCatching { target.delete() }
}

internal fun resolveManagedNonHomeBackgroundFileByContentUri(context: Context, uri: Uri): File? {
    val expectedAuthority = "${context.packageName}.fileprovider"
    if (uri.authority != expectedAuthority) return null
    val fileName = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.startsWith(NON_HOME_BACKGROUND_CROP_FILE_PREFIX) }
        ?: return null
    val dir = File(context.filesDir, NON_HOME_BACKGROUND_CROP_DIR)
    return File(dir, fileName)
}
