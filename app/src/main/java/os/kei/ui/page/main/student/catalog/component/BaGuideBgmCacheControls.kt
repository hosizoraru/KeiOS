package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

@Composable
internal fun BaGuideBgmCacheControls(
    favoriteCount: Int,
    cachedCount: Int,
    cacheBytes: Long,
    batchCaching: Boolean,
    batchDone: Int,
    batchTotal: Int,
    batchFailedCount: Int,
    accent: Color,
    onCacheAll: () -> Unit,
    onRetryFailed: () -> Unit
) {
    val title = if (batchCaching) {
        stringResource(
            R.string.ba_catalog_bgm_cache_batch_progress,
            batchDone.coerceAtLeast(0),
            batchTotal.coerceAtLeast(0)
        )
    } else if (batchFailedCount > 0) {
        stringResource(
            R.string.ba_catalog_bgm_cache_batch_failed_summary,
            batchFailedCount.coerceAtLeast(0)
        )
    } else {
        stringResource(
            R.string.ba_catalog_bgm_cache_summary,
            cachedCount.coerceAtLeast(0),
            favoriteCount.coerceAtLeast(0),
            formatBgmCacheBytes(cacheBytes)
        )
    }
    val progress = if (batchTotal > 0) {
        batchDone.toFloat() / batchTotal.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    val retryMode = !batchCaching && batchFailedCount > 0
    val actionText = stringResource(
        if (retryMode) {
            R.string.ba_catalog_bgm_action_retry_failed
        } else {
            R.string.ba_catalog_bgm_action_cache_all
        }
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardLayoutRhythm.cardContentPadding),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.ba_catalog_bgm_cache_scope_hint),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (batchCaching) {
                    top.yukonga.miuix.kmp.basic.LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        height = 4.dp,
                        colors = top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
                            .progressIndicatorColors(
                                foregroundColor = accent,
                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                            )
                    )
                }
            }
            GlassTextButton(
                backdrop = null,
                text = actionText,
                leadingIcon = if (batchCaching || retryMode) appLucideRefreshIcon() else appLucideDownloadIcon(),
                onClick = if (retryMode) onRetryFailed else onCacheAll,
                enabled = !batchCaching &&
                    (
                        retryMode ||
                            (favoriteCount > 0 && cachedCount < favoriteCount)
                        ),
                textColor = accent,
                containerColor = accent,
                variant = GlassVariant.Compact,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatBgmCacheBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        safe >= gb -> String.format(Locale.US, "%.2f GB", safe / gb)
        safe >= mb -> String.format(Locale.US, "%.2f MB", safe / mb)
        safe >= kb -> String.format(Locale.US, "%.2f KB", safe / kb)
        else -> "${bytes.coerceAtLeast(0L)} B"
    }
}
