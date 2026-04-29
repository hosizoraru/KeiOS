package os.kei.ui.page.main.github.actions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.page.GitHubPageState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsArtifactsSection(
    state: GitHubPageState,
    selectedRun: GitHubActionsRunMatch?,
    selectedRunArtifactsLoading: Boolean,
    canResolveArtifacts: Boolean,
    isDark: Boolean,
    backdrop: LayerBackdrop,
    onExpandedChange: (Boolean) -> Unit,
    onDownloadArtifact: (Long, Long) -> Unit,
    onShareArtifact: (Long, Long) -> Unit,
    context: Context
) {
    val nightlyLink = state.lookupConfig.actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink
    val emptyArtifactsText = if (nightlyLink) {
        stringResource(R.string.github_actions_empty_artifacts_nightly)
    } else {
        stringResource(R.string.github_actions_empty_artifacts)
    }
    GitHubActionsCollapsibleSection(
        title = stringResource(R.string.github_actions_section_artifacts),
        summary = artifactSectionSummary(selectedRun),
        countLabel = stringResource(
            R.string.github_actions_value_count,
            selectedRun?.artifactMatches?.size ?: 0
        ),
        expanded = state.actionsArtifactsExpanded,
        isDark = isDark,
        onExpandedChange = onExpandedChange
    ) {
        when {
            selectedRun == null -> {
                GitHubActionsNoticeCard(
                    text = emptyArtifactsText,
                    accent = MiuixTheme.colorScheme.onBackgroundVariant,
                    isDark = isDark
                )
            }
            selectedRun.traits.inProgress -> {
                GitHubActionsNoticeCard(
                    text = stringResource(R.string.github_actions_hint_run_in_progress),
                    accent = GitHubStatusPalette.Active,
                    isDark = isDark
                )
            }
            selectedRunArtifactsLoading -> {
                GitHubActionsLoadingCard(
                    text = stringResource(R.string.github_actions_loading_artifacts)
                )
            }
            selectedRun.artifactMatches.isEmpty() -> {
                GitHubActionsNoticeCard(
                    text = emptyArtifactsText,
                    accent = MiuixTheme.colorScheme.onBackgroundVariant,
                    isDark = isDark
                )
            }
            else -> {
                if (!canResolveArtifacts) {
                    GitHubActionsArtifactHintText(
                        text = stringResource(R.string.github_actions_hint_token_required)
                    )
                }
                selectedRun.artifactMatches.forEachIndexed { index, artifactMatch ->
                    GitHubActionsArtifactCard(
                        runMatch = selectedRun,
                        artifactMatch = artifactMatch,
                        recommended = index == 0,
                        hasToken = canResolveArtifacts,
                        downloading = state.actionsArtifactDownloadLoadingId == artifactMatch.artifact.id,
                        sharing = state.actionsArtifactShareLoadingId == artifactMatch.artifact.id,
                        context = context,
                        isDark = isDark,
                        backdrop = backdrop,
                        onDownload = {
                            onDownloadArtifact(
                                selectedRun.runArtifacts.run.id,
                                artifactMatch.artifact.id
                            )
                        },
                        onShare = {
                            onShareArtifact(
                                selectedRun.runArtifacts.run.id,
                                artifactMatch.artifact.id
                            )
                        }
                    )
                }
            }
        }
    }
}
