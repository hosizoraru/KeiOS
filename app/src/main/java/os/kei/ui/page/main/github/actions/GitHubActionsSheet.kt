package os.kei.ui.page.main.github.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.ui.page.main.github.page.GitHubPageState
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet

@Composable
internal fun GitHubActionsSheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    state: GitHubPageState,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit,
    onSelectWorkflow: (Long) -> Unit,
    onSelectRun: (Long) -> Unit,
    onLoadMoreRuns: () -> Unit,
    onWorkflowsExpandedChange: (Boolean) -> Unit,
    onRunsExpandedChange: (Boolean) -> Unit,
    onArtifactsExpandedChange: (Boolean) -> Unit,
    onRefreshRun: (Long) -> Unit,
    onDownloadArtifact: (Long, Long) -> Unit,
    onShareArtifact: (Long, Long) -> Unit,
    onOpenRun: () -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.github_actions_sheet_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideRefreshIcon(),
                contentDescription = stringResource(R.string.github_actions_sheet_cd_refresh),
                onClick = onRefresh
            )
        }
    ) {
        GitHubActionsSheetContent(
            state = state,
            backdrop = backdrop,
            onSelectWorkflow = onSelectWorkflow,
            onSelectRun = onSelectRun,
            onLoadMoreRuns = onLoadMoreRuns,
            onWorkflowsExpandedChange = onWorkflowsExpandedChange,
            onRunsExpandedChange = onRunsExpandedChange,
            onArtifactsExpandedChange = onArtifactsExpandedChange,
            onRefreshRun = onRefreshRun,
            onDownloadArtifact = onDownloadArtifact,
            onShareArtifact = onShareArtifact,
            onOpenRun = onOpenRun
        )
    }
}
