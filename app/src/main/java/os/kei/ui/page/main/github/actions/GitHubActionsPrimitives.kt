package os.kei.ui.page.main.github.actions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetSurfaceCard
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsArtifactHintText(
    text: String
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = githubActionsSecondaryTextColor(isSystemInDarkTheme()),
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun GitHubActionsLoadMoreRunsButton(
    backdrop: LayerBackdrop,
    visibleRunLimit: Int,
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        GlassTextButton(
            backdrop = backdrop,
            variant = GlassVariant.SheetAction,
            text = if (loading) {
                stringResource(R.string.common_loading)
            } else {
                stringResource(R.string.github_actions_action_load_more_runs, visibleRunLimit)
            },
            leadingIcon = appLucideRefreshIcon(),
            enabled = !loading,
            textColor = MiuixTheme.colorScheme.primary,
            iconTint = MiuixTheme.colorScheme.primary,
            onClick = onClick,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun GitHubActionsSelectableCard(
    selected: Boolean,
    isDark: Boolean,
    containerColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val resolvedContainerColor = containerColor ?: if (selected) {
        githubActionsNeutralCardColor(isDark, prominent = true)
    } else {
        githubActionsNeutralCardColor(isDark)
    }
    val resolvedBorderColor = borderColor ?: githubActionsNeutralBorderColor(isDark, prominent = selected)
    SheetSurfaceCard(
        containerColor = resolvedContainerColor,
        borderColor = resolvedBorderColor,
        verticalSpacing = 10.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onClick
    ) {
        content()
    }
}

@Composable
internal fun GitHubActionsTitleRow(
    title: String,
    accent: Color,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = accent,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GitHubActionsPillRow(
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = { content() }
    )
}

@Composable
internal fun GitHubActionsLoadingCard(text: String) {
    val isDark = isSystemInDarkTheme()
    SheetSurfaceCard(
        containerColor = githubActionsNeutralCardColor(isDark),
        borderColor = githubActionsNeutralBorderColor(isDark),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                progress = 0f,
                size = 18.dp,
                strokeWidth = 2.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = MiuixTheme.colorScheme.primary,
                    backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                )
            )
            Text(
                text = text,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight
            )
        }
    }
}

@Composable
internal fun GitHubActionsNoticeCard(
    text: String,
    accent: Color,
    isDark: Boolean
) {
    val isError = accent == GitHubStatusPalette.Error
    SheetSurfaceCard(
        containerColor = if (isError) {
            GitHubStatusPalette.tonedSurface(GitHubStatusPalette.Error, isDark).copy(
                alpha = if (isDark) 0.16f else 0.09f
            )
        } else {
            githubActionsNeutralCardColor(isDark)
        },
        borderColor = if (isError) {
            GitHubStatusPalette.Error.copy(alpha = if (isDark) 0.24f else 0.16f)
        } else {
            githubActionsNeutralBorderColor(isDark)
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = if (isError) GitHubStatusPalette.Error else githubActionsSecondaryTextColor(isDark),
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
    }
}
