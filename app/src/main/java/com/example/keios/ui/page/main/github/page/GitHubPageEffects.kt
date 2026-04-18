package com.example.keios.ui.page.main

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyListState
import com.example.keios.R
import com.example.keios.core.system.AppPackageChangedEvents
import com.example.keios.feature.github.data.local.GitHubTrackStoreSignals
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.ui.page.main.github.query.OnlineShareTargetOption

@Composable
internal fun BindGitHubPageEffects(
    context: Context,
    listState: LazyListState,
    scrollToTopSignal: Int,
    isPageActive: Boolean,
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

    LaunchedEffect(isPageActive) {
        if (!isPageActive) return@LaunchedEffect
        val currentSignalVersion = GitHubTrackStoreSignals.version.value
        if (!state.hasInitialized) {
            state.hasInitialized = true
            actions.initializePage()
        } else if (currentSignalVersion > state.lastTrackStoreSignalVersion) {
            actions.syncTrackSnapshotFromStore(forceRefreshApps = true)
        }
        state.lastTrackStoreSignalVersion = currentSignalVersion
        actions.trimExpiredPendingShareImportTrack()
    }

    LaunchedEffect(isPageActive) {
        if (!isPageActive) return@LaunchedEffect
        GitHubTrackStoreSignals.version.collect { version ->
            if (version <= 0L) return@collect
            if (version <= state.lastTrackStoreSignalVersion) return@collect
            state.lastTrackStoreSignalVersion = version
            if (!state.hasInitialized) return@collect
            actions.syncTrackSnapshotFromStore(forceRefreshApps = true)
        }
    }

    LaunchedEffect(scrollToTopSignal, isPageActive) {
        if (isPageActive && scrollToTopSignal > 0) {
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

    LaunchedEffect(isPageActive) {
        if (!isPageActive) return@LaunchedEffect
        AppPackageChangedEvents.events.collect { event ->
            actions.handlePackageChangedEvent(event)
        }
    }
}
