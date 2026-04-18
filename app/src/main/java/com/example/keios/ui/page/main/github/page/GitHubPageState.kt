package com.example.keios.ui.page.main

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetBundle
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.feature.github.model.InstalledAppItem
import kotlinx.coroutines.Job

@Stable
internal class GitHubPageState(
    private val searchBarHideThresholdPx: Float
) {
    var trackedSearch by mutableStateOf("")
    var repoUrlInput by mutableStateOf("")
    var appSearch by mutableStateOf("")
    var pickerExpanded by mutableStateOf(false)
    var showAddSheet by mutableStateOf(false)
    var showStrategySheet by mutableStateOf(false)
    var showCheckLogicSheet by mutableStateOf(false)
    var showDownloaderPopup by mutableStateOf(false)
    var editingTrackedItem by mutableStateOf<GitHubTrackedApp?>(null)
    var preferPreReleaseInput by mutableStateOf(false)
    var alwaysShowLatestReleaseDownloadButtonInput by mutableStateOf(false)
    var selectedApp by mutableStateOf<InstalledAppItem?>(null)
    var appList by mutableStateOf<List<InstalledAppItem>>(emptyList())
    var appListLoaded by mutableStateOf(false)
    var hasAutoRequestedPermission by mutableStateOf(false)
    var hasInitialized by mutableStateOf(false)
    var lastTrackStoreSignalVersion by mutableStateOf(0L)
    var showSortPopup by mutableStateOf(false)
    var showCheckLogicIntervalPopup by mutableStateOf(false)
    var showOnlineShareTargetPopup by mutableStateOf(false)
    var checkLogicIntervalPopupAnchorBounds by mutableStateOf<IntRect?>(null)
    var downloaderPopupAnchorBounds by mutableStateOf<IntRect?>(null)
    var onlineShareTargetPopupAnchorBounds by mutableStateOf<IntRect?>(null)
    var pendingTrackImportPreview by mutableStateOf<GitHubTrackImportPreview?>(null)
    var pendingShareImportPreview by mutableStateOf<GitHubShareImportPreview?>(null)
    var pendingShareImportTrack by mutableStateOf<GitHubPendingShareImportTrack?>(null)
    var pendingShareImportAttachCandidate by mutableStateOf<GitHubPendingShareImportAttachCandidate?>(null)
    var shareImportResolving by mutableStateOf(false)
    var sortMode by mutableStateOf(GitHubSortMode.UpdateFirst)
    var pendingDeleteItem by mutableStateOf<GitHubTrackedApp?>(null)
    var overviewRefreshState by mutableStateOf(OverviewRefreshState.Idle)
    var lastRefreshMs by mutableStateOf(0L)
    var refreshIntervalHours by mutableStateOf(3)
    var refreshProgress by mutableStateOf(0f)
    var lookupConfig by mutableStateOf(GitHubLookupConfig())
    var selectedStrategyInput by mutableStateOf(lookupConfig.selectedStrategy)
    var githubApiTokenInput by mutableStateOf("")
    var checkAllTrackedPreReleasesInput by mutableStateOf(false)
    var aggressiveApkFilteringInput by mutableStateOf(false)
    var shareImportLinkageEnabledInput by mutableStateOf(false)
    var onlineShareTargetPackageInput by mutableStateOf("")
    var preferredDownloaderPackageInput by mutableStateOf("")
    var refreshIntervalHoursInput by mutableStateOf(refreshIntervalHours)
    var showApiTokenPlainText by mutableStateOf(false)
    var strategyBenchmarkRunning by mutableStateOf(false)
    var strategyBenchmarkError by mutableStateOf<String?>(null)
    var strategyBenchmarkReport by mutableStateOf<com.example.keios.feature.github.model.GitHubStrategyBenchmarkReport?>(null)
    var credentialCheckRunning by mutableStateOf(false)
    var credentialCheckError by mutableStateOf<String?>(null)
    var credentialCheckStatus by mutableStateOf<com.example.keios.feature.github.model.GitHubApiCredentialStatus?>(null)
    var recommendedTokenGuideExpanded by mutableStateOf(false)
    var assetSourceSignature by mutableStateOf("")
    var refreshAllJob by mutableStateOf<Job?>(null)
    var deleteInProgress by mutableStateOf(false)
    var showFloatingAddButton by mutableStateOf(true)
    var showSearchBar by mutableStateOf(true)
    var searchBarHideOffsetPx by mutableStateOf(0f)

    val trackedItems = mutableStateListOf<GitHubTrackedApp>()
    val checkStates = mutableStateMapOf<String, VersionCheckUi>()
    val apkAssetBundles = mutableStateMapOf<String, GitHubReleaseAssetBundle>()
    val apkAssetLoading = mutableStateMapOf<String, Boolean>()
    val apkAssetErrors = mutableStateMapOf<String, String>()
    val apkAssetExpanded = mutableStateMapOf<String, Boolean>()
    val apkAssetIncludeAll = mutableStateMapOf<String, Boolean>()
    val trackedCardExpanded = mutableStateMapOf<String, Boolean>()
    val trackedFirstInstallAtByPackage = mutableStateMapOf<String, Long>()

    val addButtonScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -1f) {
                showFloatingAddButton = false
                if (showSearchBar) {
                    searchBarHideOffsetPx =
                        (searchBarHideOffsetPx + (-available.y)).coerceAtMost(searchBarHideThresholdPx)
                    if (searchBarHideOffsetPx >= searchBarHideThresholdPx) {
                        showSearchBar = false
                        searchBarHideOffsetPx = 0f
                    }
                }
            }
            if (available.y > 1f) {
                showFloatingAddButton = true
                showSearchBar = true
                searchBarHideOffsetPx = 0f
            }
            return Offset.Zero
        }
    }

    fun activeStrategyId(): String = lookupConfig.selectedStrategy.storageId

    fun buildAssetSourceSignature(config: GitHubLookupConfig = lookupConfig): String {
        return listOf(
            "asset-v2",
            config.selectedStrategy.storageId,
            config.apiToken.isNotBlank().toString(),
            config.aggressiveApkFiltering.toString()
        ).joinToString("|")
    }

    fun matchesAssetSourceSignature(bundle: GitHubReleaseAssetBundle): Boolean {
        return bundle.sourceConfigSignature.isNotBlank() &&
            bundle.sourceConfigSignature == buildAssetSourceSignature()
    }

    fun clearAllAssetUiState() {
        apkAssetBundles.clear()
        apkAssetLoading.clear()
        apkAssetErrors.clear()
        apkAssetExpanded.clear()
        apkAssetIncludeAll.clear()
    }

    fun clearAssetRuntimeState(itemId: String) {
        apkAssetExpanded.remove(itemId)
        apkAssetLoading.remove(itemId)
        apkAssetErrors.remove(itemId)
        apkAssetBundles.remove(itemId)
    }

    fun clearAssetUiState(itemId: String) {
        clearAssetRuntimeState(itemId)
        apkAssetIncludeAll.remove(itemId)
    }

    fun retainTrackedUiState(validItemIds: Set<String>) {
        trackedCardExpanded.keys.retainAll(validItemIds)
        apkAssetExpanded.keys.retainAll(validItemIds)
        apkAssetIncludeAll.keys.retainAll(validItemIds)
        apkAssetLoading.keys.retainAll(validItemIds)
        apkAssetErrors.keys.retainAll(validItemIds)
        apkAssetBundles.keys.retainAll(validItemIds)
    }

    fun recordTrackedFirstInstallAt(packageName: String, firstInstallAtMillis: Long) {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isBlank()) return
        if (firstInstallAtMillis <= 0L) return
        val current = trackedFirstInstallAtByPackage[normalizedPackageName]
        if (current == null || current <= 0L || firstInstallAtMillis < current) {
            trackedFirstInstallAtByPackage[normalizedPackageName] = firstInstallAtMillis
        }
    }

    fun retainTrackedFirstInstallAtByTrackedItems() {
        val trackedPackages = trackedItems
            .map { it.packageName.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        trackedFirstInstallAtByPackage.keys.retainAll(trackedPackages)
    }

    fun dismissStrategySheet() {
        showStrategySheet = false
        showApiTokenPlainText = false
        credentialCheckRunning = false
        recommendedTokenGuideExpanded = false
    }

    fun dismissCheckLogicSheet() {
        showCheckLogicIntervalPopup = false
        showDownloaderPopup = false
        showOnlineShareTargetPopup = false
        pendingTrackImportPreview = null
        showCheckLogicSheet = false
    }

    fun dismissTrackImportPreview() {
        pendingTrackImportPreview = null
    }

    fun dismissShareImportPreview() {
        pendingShareImportPreview = null
    }

    fun resetTrackEditor() {
        editingTrackedItem = null
        repoUrlInput = ""
        selectedApp = null
        appSearch = ""
        pickerExpanded = false
        preferPreReleaseInput = false
        alwaysShowLatestReleaseDownloadButtonInput = false
    }

    fun dismissTrackSheet() {
        showAddSheet = false
        resetTrackEditor()
    }
}

@Composable
internal fun rememberGitHubPageState(): GitHubPageState {
    val density = LocalDensity.current
    val searchBarHideThresholdPx = remember(density) {
        with(density) { 28.dp.toPx() }
    }
    return remember(searchBarHideThresholdPx) {
        GitHubPageState(searchBarHideThresholdPx)
    }
}
