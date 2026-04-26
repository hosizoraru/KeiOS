package os.kei.ui.page.main.github

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.data.local.AppIconCache
import os.kei.feature.github.model.InstalledAppItem
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.sheet.SheetSurfaceCard
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubSelectedAppCard(
    selectedApp: InstalledAppItem
) {
    SheetSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = GitHubStatusPalette.tonedSurface(
            GitHubStatusPalette.Update,
            isDark = isSystemInDarkTheme()
        ),
        borderColor = GitHubStatusPalette.Update.copy(alpha = 0.28f),
        verticalSpacing = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = selectedApp.packageName, size = 38.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = selectedApp.label,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedApp.packageName,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                label = stringResource(R.string.github_strategy_status_selected),
                color = GitHubStatusPalette.Update
            )
        }
    }
}

@Composable
internal fun GitHubAppCandidateRow(
    app: InstalledAppItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val accent = if (selected) GitHubStatusPalette.Update else MiuixTheme.colorScheme.primary
    SheetSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (selected) {
            GitHubStatusPalette.tonedSurface(GitHubStatusPalette.Update, isDark)
        } else {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.48f)
        },
        borderColor = if (selected) {
            GitHubStatusPalette.Update.copy(alpha = 0.3f)
        } else {
            MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.12f)
        },
        verticalSpacing = 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = app.packageName, size = 32.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = app.label,
                    color = accent,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                StatusPill(
                    label = stringResource(R.string.github_strategy_status_current),
                    color = GitHubStatusPalette.Update
                )
            }
        }
    }
}

@Composable
internal fun AppIcon(
    packageName: String,
    size: Dp
) {
    val normalizedPackageName = packageName.trim()
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(
        initialValue = AppIconCache.get(normalizedPackageName),
        normalizedPackageName
    ) {
        if (normalizedPackageName.isBlank()) return@produceState
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                AppIconCache.getOrLoad(context, normalizedPackageName)
            }
        }
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = imageBitmap,
            contentDescription = normalizedPackageName,
            modifier = Modifier
                .width(size)
                .height(size)
                .clip(ContinuousCapsule)
        )
    } else {
        Box(
            modifier = Modifier
                .width(size)
                .height(size)
                .clip(ContinuousCapsule),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.github_strategy_app_fallback),
                color = MiuixTheme.colorScheme.primary,
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight
            )
        }
    }
}
