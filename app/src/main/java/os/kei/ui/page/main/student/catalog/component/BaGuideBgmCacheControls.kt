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
    accent: Color,
    onCacheAll: () -> Unit
) {
    val title = if (batchCaching) {
        stringResource(
            R.string.ba_catalog_bgm_cache_batch_progress,
            batchDone.coerceAtLeast(0),
            batchTotal.coerceAtLeast(0)
        )
    } else {
        stringResource(
            R.string.ba_catalog_bgm_cache_summary,
            cachedCount.coerceAtLeast(0),
            favoriteCount.coerceAtLeast(0),
            formatBgmCacheBytes(cacheBytes)
        )
    }
    val actionText = stringResource(R.string.ba_catalog_bgm_action_cache_all)
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
            }
            GlassTextButton(
                backdrop = null,
                text = actionText,
                leadingIcon = if (batchCaching) appLucideRefreshIcon() else appLucideDownloadIcon(),
                onClick = onCacheAll,
                enabled = favoriteCount > 0 && cachedCount < favoriteCount && !batchCaching,
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
