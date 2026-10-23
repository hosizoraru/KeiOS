package os.kei.ui.page.main.github.page

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import os.kei.feature.github.model.FdroidAppSearchCandidate
import os.kei.feature.github.model.FdroidAppSearchFailure
import os.kei.feature.github.model.FdroidAppSearchRepoReport
import os.kei.feature.github.model.FdroidAntiFeaturePolicy
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.FdroidTrustPolicy
import os.kei.feature.github.model.FdroidVersionSelectionMode
import os.kei.feature.github.model.GitHubPackageRepositoryScanCandidate
import os.kei.feature.github.model.GitHubTrackedActionsUpdateIntervalMode
import os.kei.feature.github.model.GitHubTrackedIgnoreMode
import os.kei.feature.github.model.GitHubTrackedPreciseApkVersionMode
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.GitHubTrackedUpdateIntervalMode
import os.kei.feature.github.model.InstalledAppItem

@Stable
internal class GitHubTrackEditorPageStateHolder(private val onInputChanged: () -> Unit = {}) {
    private fun <T> durableStateOf(initial: T): ReadWriteProperty<Any?, T> {
        val state = mutableStateOf(initial)
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                if (state.value == value) return
                state.value = value
                onInputChanged()
            }
        }
    }

    var repoUrlInput by durableStateOf("")
    var packageNameInput by durableStateOf("")
    var repoScanCandidates by mutableStateOf<List<GitHubPackageRepositoryScanCandidate>>(emptyList())
    var appSearch by mutableStateOf("")
    var addTrackAppPickerFirstVisibleItemIndex by mutableIntStateOf(0)
    var addTrackAppPickerFirstVisibleItemScrollOffset by mutableIntStateOf(0)
    var pickerExpanded by mutableStateOf(false)
    var preferPreReleaseInput by durableStateOf(false)
    var alwaysShowLatestReleaseDownloadButtonInput by durableStateOf(false)
    var checkActionsUpdatesInput by durableStateOf(false)
    var externalBuildUntilReleaseInput by durableStateOf(false)
    var updateIntervalModeInput by durableStateOf(GitHubTrackedUpdateIntervalMode.FollowGlobal)
    var actionsUpdateIntervalModeInput by durableStateOf(
        GitHubTrackedActionsUpdateIntervalMode.FollowGlobal,
    )
    var preciseApkVersionModeInput by durableStateOf(GitHubTrackedPreciseApkVersionMode.FollowGlobal)
    var ignoreModeInput by durableStateOf(GitHubTrackedIgnoreMode.None)
    var ignoredStableReleaseKeyInput by durableStateOf("")
    var ignoredPreReleaseKeyInput by durableStateOf("")
    var fdroidVersionSelectionModeInput by durableStateOf(FdroidVersionSelectionMode.SuggestedVersionCode)
    var fdroidVersionNameRegexInput by durableStateOf("")
    var fdroidApkNameRegexInput by durableStateOf("")
    var fdroidTrustPolicyInput by durableStateOf(FdroidTrustPolicy.TrackOnlyWarn)
    var fdroidAntiFeaturePolicyInput by durableStateOf(FdroidAntiFeaturePolicy.ShowAndWarn)
    var fdroidRepoScopeIdInput by durableStateOf(FdroidRepositoryPresets.COMMON_ID)
    var fdroidAppSearchQueryInput by durableStateOf("")
    var fdroidAppSearchCandidates by mutableStateOf<List<FdroidAppSearchCandidate>>(emptyList())
    var fdroidAppSearchFailures by mutableStateOf<List<FdroidAppSearchFailure>>(emptyList())
    var fdroidAppSearchFailuresExpanded by mutableStateOf(false)
    var fdroidAppSearchRepoReports by mutableStateOf<List<FdroidAppSearchRepoReport>>(emptyList())
    var fdroidSelectedCandidate by durableStateOf<FdroidAppSearchCandidate?>(null)
    var fdroidAppSearchRunning by mutableStateOf(false)
    var trackSourceModeInput by durableStateOf(GitHubTrackedSourceMode.GitHubRepository)
    var repoUrlScanRunning by mutableStateOf(false)
    var packageNameScanRunning by mutableStateOf(false)
    var selectedApp by mutableStateOf<InstalledAppItem?>(null)
    var appList by mutableStateOf<List<InstalledAppItem>>(emptyList())
    var appListLoaded by mutableStateOf(false)
    var appListRefreshing by mutableStateOf(false)
    var sourceModeDropdownExpanded by mutableStateOf(false)
    var updateIntervalDropdownExpanded by mutableStateOf(false)
    var actionsIntervalDropdownExpanded by mutableStateOf(false)
    var preciseModeDropdownExpanded by mutableStateOf(false)
    var ignoreModeDropdownExpanded by mutableStateOf(false)
    var fdroidVersionSelectionDropdownExpanded by mutableStateOf(false)
    var fdroidTrustPolicyDropdownExpanded by mutableStateOf(false)
    var fdroidAntiFeaturePolicyDropdownExpanded by mutableStateOf(false)
    var fdroidRepoScopeDropdownExpanded by mutableStateOf(false)

    fun reset() {
        repoUrlInput = ""
        packageNameInput = ""
        repoScanCandidates = emptyList()
        selectedApp = null
        appSearch = ""
        pickerExpanded = false
        preferPreReleaseInput = false
        alwaysShowLatestReleaseDownloadButtonInput = false
        checkActionsUpdatesInput = false
        externalBuildUntilReleaseInput = false
        updateIntervalModeInput = GitHubTrackedUpdateIntervalMode.FollowGlobal
        actionsUpdateIntervalModeInput = GitHubTrackedActionsUpdateIntervalMode.FollowGlobal
        preciseApkVersionModeInput = GitHubTrackedPreciseApkVersionMode.FollowGlobal
        ignoreModeInput = GitHubTrackedIgnoreMode.None
        ignoredStableReleaseKeyInput = ""
        ignoredPreReleaseKeyInput = ""
        fdroidVersionSelectionModeInput = FdroidVersionSelectionMode.SuggestedVersionCode
        fdroidVersionNameRegexInput = ""
        fdroidApkNameRegexInput = ""
        fdroidTrustPolicyInput = FdroidTrustPolicy.TrackOnlyWarn
        fdroidAntiFeaturePolicyInput = FdroidAntiFeaturePolicy.ShowAndWarn
        fdroidRepoScopeIdInput = FdroidRepositoryPresets.COMMON_ID
        fdroidAppSearchQueryInput = ""
        fdroidAppSearchCandidates = emptyList()
        fdroidAppSearchFailures = emptyList()
        fdroidAppSearchFailuresExpanded = false
        fdroidAppSearchRepoReports = emptyList()
        fdroidSelectedCandidate = null
        fdroidAppSearchRunning = false
        trackSourceModeInput = GitHubTrackedSourceMode.GitHubRepository
        repoUrlScanRunning = false
        packageNameScanRunning = false
        resetDropdownState()
    }

    fun resetDropdownState() {
        sourceModeDropdownExpanded = false
        updateIntervalDropdownExpanded = false
        actionsIntervalDropdownExpanded = false
        preciseModeDropdownExpanded = false
        ignoreModeDropdownExpanded = false
        fdroidVersionSelectionDropdownExpanded = false
        fdroidTrustPolicyDropdownExpanded = false
        fdroidAntiFeaturePolicyDropdownExpanded = false
        fdroidRepoScopeDropdownExpanded = false
    }
}
