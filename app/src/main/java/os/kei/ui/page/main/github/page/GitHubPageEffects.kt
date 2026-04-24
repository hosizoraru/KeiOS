package os.kei.ui.page.main.github.page

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyListState
import os.kei.R
import os.kei.core.system.AppPackageChangedEvents
import os.kei.feature.github.data.local.GitHubTrackStoreSignals
import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import kotlinx.coroutines.delay

private const val GITHUB_PAGE_ACTIVE_SYNC_DELAY_MS = 120L

@Composable
internal fun BindGitHubPageEffects(
    context: Context,
    listState: LazyListState,
    scrollToTopSignal: Int,
    isPageWarmActive: Boolean,
    isPageDataActive: Boolean,
    state: GitHubPageState,
    actions: GitHubPageActions,
    installedOnlineShareTargets: List<OnlineShareTargetOption>,
    onLaunchAppListPermission: (Intent) -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit
) {
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }

    LaunchedEffect(installedOnlineShareTargets) {
        actions.handleInstalledOnlineShareTargetsChanged(installedOnlineShareTargets)
    }

    LaunchedEffect(isPageWarmActive) {
        if (!isPageWarmActive) return@LaunchedEffect
        val currentSignalVersion = GitHubTrackStoreSignals.version.value
        if (!state.hasInitialized) {
            state.hasInitialized = true
            actions.initializeWarmSnapshot()
        } else if (currentSignalVersion > state.lastTrackStoreSignalVersion) {
            actions.syncTrackSnapshotFromStore(forceRefreshApps = false)
        }
        state.lastTrackStoreSignalVersion = currentSignalVersion
    }

    LaunchedEffect(isPageDataActive) {
        if (!isPageDataActive) return@LaunchedEffect
        delay(GITHUB_PAGE_ACTIVE_SYNC_DELAY_MS)
        if (!state.hasInitialized) {
            state.hasInitialized = true
            actions.initializeWarmSnapshot()
        }
        if (!state.hasActiveInitialized) {
            state.hasActiveInitialized = true
            actions.initializePageActiveWork()
        } else {
            actions.syncLocalAppStateOnPageActive()
        }
        actions.trimExpiredPendingShareImportTrack()
    }

    LaunchedEffect(isPageWarmActive) {
        if (!isPageWarmActive) return@LaunchedEffect
        GitHubTrackStoreSignals.version.collect { version ->
            if (version <= 0L) return@collect
            if (version <= state.lastTrackStoreSignalVersion) return@collect
            state.lastTrackStoreSignalVersion = version
            if (!state.hasInitialized) return@collect
            actions.syncTrackSnapshotFromStore(forceRefreshApps = isPageDataActive)
        }
    }

    LaunchedEffect(scrollToTopSignal, isPageDataActive) {
        if (isPageDataActive && scrollToTopSignal > 0) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(state.appListLoaded, state.appList) {
        if (state.appListLoaded && state.appList.isEmpty() && !state.hasAutoRequestedPermission) {
            state.hasAutoRequestedPermission = true
            val intent = GitHubVersionUtils.buildAppListPermissionIntent(context)
            if (intent != null) {
                onLaunchAppListPermission(intent)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_open_permission_page_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    LaunchedEffect(isPageDataActive) {
        if (!isPageDataActive) return@LaunchedEffect
        AppPackageChangedEvents.events.collect { event ->
            actions.handlePackageChangedEvent(event)
        }
    }
}
