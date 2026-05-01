package os.kei.ui.page.main.github.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubTrackedItemAssetLoadingCard(
    alwaysLatestReleaseDownload: Boolean,
    targetAccent: Color,
    isDark: Boolean
) {
    val stateContainerColor = if (alwaysLatestReleaseDownload) {
        GitHubStatusPalette.tonedSurface(
            targetAccent,
            isDark = isDark
        ).copy(alpha = if (isDark) 0.62f else 0.34f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
    }
    AppSurfaceCard(
        containerColor = stateContainerColor,
        borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CardLayoutRhythm.cardHorizontalPadding,
                    vertical = CardLayoutRhythm.cardVerticalPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            LiquidCircularProgressBar(
                size = 18.dp,
                strokeWidth = 2.dp,
                activeColor = MiuixTheme.colorScheme.primary,
                inactiveColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricCardTextGap)
            ) {
                Text(
                    text = stringResource(R.string.github_asset_loading_title),
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight
                )
                Text(
                    text = stringResource(R.string.github_asset_loading_summary),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight
                )
            }
        }
    }
}

@Composable
internal fun GitHubTrackedItemAssetErrorCard(
    assetError: String,
    isDark: Boolean
) {
    AppSurfaceCard(
        containerColor = GitHubStatusPalette.tonedSurface(
            GitHubStatusPalette.Error,
            isDark = isDark
        ).copy(alpha = if (isDark) 0.84f else 0.96f),
        borderColor = GitHubStatusPalette.Error.copy(alpha = if (isDark) 0.34f else 0.22f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CardLayoutRhythm.cardHorizontalPadding,
                    vertical = CardLayoutRhythm.cardVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.compactSectionGap)
        ) {
            Text(
                text = stringResource(R.string.github_asset_error_title),
                color = GitHubStatusPalette.Error,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight
            )
            Text(
                text = assetError,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight
            )
        }
    }
}
