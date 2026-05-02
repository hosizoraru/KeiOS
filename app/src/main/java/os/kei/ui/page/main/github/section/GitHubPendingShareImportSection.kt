package os.kei.ui.page.main.github.section

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.ui.page.main.github.GitHubCompactInfoRow
import os.kei.ui.page.main.github.share.GitHubPendingShareImportTrack
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.LiquidDialogActionButton
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubPendingShareImportCard(
    pending: GitHubPendingShareImportTrack,
    repoOverlapCount: Int,
    onCancel: () -> Unit
) {
    val nowMillis = System.currentTimeMillis()
    val ageMinutes = ((nowMillis - pending.armedAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
    AppSurfaceCard(
        containerColor = GitHubStatusPalette.tonedSurface(
            GitHubStatusPalette.Active,
            isDark = isSystemInDarkTheme()
        ).copy(alpha = 0.26f),
        borderColor = GitHubStatusPalette.Active.copy(alpha = 0.24f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CardLayoutRhythm.cardHorizontalPadding,
                    vertical = CardLayoutRhythm.cardVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.github_share_import_pending_title),
                    color = GitHubStatusPalette.Active,
                    fontSize = AppTypographyTokens.CompactTitle.fontSize,
                    lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                    fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = stringResource(
                        R.string.github_share_import_pending_armed_minutes,
                        ageMinutes.coerceAtLeast(0)
                    ),
                    color = GitHubStatusPalette.Active,
                    size = AppStatusPillSize.Compact
                )
            }
            GitHubCompactInfoRow(
                label = stringResource(R.string.github_share_import_pending_label_target),
                value = "${pending.owner}/${pending.repo}",
                valueColor = MiuixTheme.colorScheme.onBackground
            )
            if (pending.releaseTag.isNotBlank()) {
                GitHubCompactInfoRow(
                    label = stringResource(R.string.github_share_import_pending_label_release),
                    value = pending.releaseTag,
                    valueColor = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            if (pending.assetName.isNotBlank()) {
                GitHubCompactInfoRow(
                    label = stringResource(R.string.github_share_import_pending_label_asset),
                    value = pending.assetName,
                    valueColor = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            if (repoOverlapCount > 0) {
                Text(
                    text = stringResource(
                        R.string.github_share_import_pending_repo_overlap_hint,
                        repoOverlapCount
                    ),
                    color = GitHubStatusPalette.PreRelease,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LiquidDialogActionButton(
                    text = stringResource(R.string.github_share_import_pending_action_cancel),
                    onClick = onCancel
                )
            }
        }
    }
}
