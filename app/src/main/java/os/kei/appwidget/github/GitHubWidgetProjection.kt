package os.kei.appwidget.github

import os.kei.feature.github.data.local.GitHubTrackSnapshot
import os.kei.feature.github.model.GitHubCheckCacheEntry
import os.kei.feature.github.model.GitHubTrackedIgnoreMode
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.buildGitHubReleaseIgnoreKey
import os.kei.feature.github.model.forTrackedItem
import os.kei.feature.github.model.githubReleaseIgnoreKeyMatches
import os.kei.feature.github.model.isValidForTrackedItem
import os.kei.feature.github.model.suppressesAllReleaseUpdates

internal fun projectGitHubWidget(snapshot: GitHubTrackSnapshot): GitHubWidgetSnapshot {
    var updates = 0
    var unchecked = 0
    var failed = 0
    var ignored = 0
    var lastChecked = 0L
    val pending = mutableListOf<GitHubWidgetPendingUpdate>()
    val projects = mutableListOf<GitHubWidgetProject>()
    snapshot.items.forEach { item ->
        val displayName = item.appLabel.takeUnless { it.isBlank() || it == item.packageName }
            ?: item.repo.ifBlank { item.appLabel }
        fun record(
            status: GitHubWidgetProjectStatus,
            version: String = "",
            preRelease: Boolean = false,
            localVersion: String = "",
        ) {
            val row = GitHubWidgetProject(
                displayName.filterNot(Char::isISOControl).take(80),
                status, version.filterNot(Char::isISOControl).take(48), preRelease,
                localVersion.filterNot(Char::isISOControl).take(48),
            )
            val index = projects.indexOfFirst { it.status.ordinal > status.ordinal }
            if (index >= 0) projects.add(index, row) else if (projects.size < 6) projects.add(row)
            if (projects.size > 6) projects.removeAt(6)
        }
        if (item.ignoreMode.suppressesAllReleaseUpdates()) {
            ignored++
            record(GitHubWidgetProjectStatus.Paused)
            return@forEach
        }
        val entry = snapshot.checkCache[item.id]?.takeIf {
            it.isValidForTrackedItem(
                item = item,
                lookupConfig = snapshot.lookupConfig.forTrackedItem(item),
                activeStrategyId = snapshot.lookupConfig.selectedStrategy.storageId,
            )
        }
        if (entry == null || entry.loading) {
            unchecked++
            record(GitHubWidgetProjectStatus.Unchecked)
            return@forEach
        }
        lastChecked = maxOf(lastChecked, entry.checkedAtMillis)
        if (GitHubTrackedReleaseStatus.isFailureMessage(entry.message)) {
            failed++
            record(GitHubWidgetProjectStatus.Failed, localVersion = entry.localVersion)
            return@forEach
        }
        val stableIgnored = item.ignoreMode == GitHubTrackedIgnoreMode.CurrentStable &&
            githubReleaseIgnoreKeyMatches(item.ignoredStableReleaseKey, entry.stableIgnoreKey())
        val preIgnored = item.ignoreMode == GitHubTrackedIgnoreMode.CurrentPreRelease &&
            githubReleaseIgnoreKeyMatches(item.ignoredPreReleaseKey, entry.preIgnoreKey())
        val stableUpdate = entry.hasUpdate == true && !stableIgnored
        val preUpdate = entry.hasPreReleaseUpdate && !preIgnored
        if (stableUpdate || preUpdate) {
            updates++
            val preRelease = !stableUpdate && preUpdate
            val version = if (preRelease) {
                entry.latestPreRawTag.ifBlank { entry.preReleaseInfo.ifBlank { entry.latestPreName } }
            } else {
                entry.latestStableRawTag.ifBlank { entry.latestTag.ifBlank { entry.latestStableName } }
            }
            record(GitHubWidgetProjectStatus.Update, version, preRelease, entry.localVersion)
            if (pending.size < 6) {
                pending += GitHubWidgetPendingUpdate(
                    name = displayName.filterNot(Char::isISOControl).take(80),
                    version = version.filterNot(Char::isISOControl).take(48),
                    preRelease = preRelease,
                )
            }
        } else if (entry.hasUpdate == null && !entry.hasPreReleaseUpdate) {
            unchecked++
            record(GitHubWidgetProjectStatus.Unchecked, localVersion = entry.localVersion)
        } else {
            record(GitHubWidgetProjectStatus.NoUpdate, localVersion = entry.localVersion)
        }
    }
    return GitHubWidgetSnapshot(
        available = true,
        trackedCount = snapshot.items.size,
        updateCount = updates,
        uncheckedCount = unchecked,
        failedCount = failed,
        ignoredCount = ignored,
        lastCheckedAtMillis = lastChecked,
        pendingUpdates = pending,
        projects = projects,
    )
}

private fun GitHubCheckCacheEntry.stableIgnoreKey(): String = buildGitHubReleaseIgnoreKey(
    displayVersion = latestStableName.ifBlank { latestTag.ifBlank { latestStableRawTag } },
    rawTag = latestStableRawTag.ifBlank { latestTag },
    rawName = latestStableName,
    link = latestStableUrl,
    preciseApkVersion = latestStableApkVersion,
)

private fun GitHubCheckCacheEntry.preIgnoreKey(): String = buildGitHubReleaseIgnoreKey(
    displayVersion = latestPreName.ifBlank { preReleaseInfo.ifBlank { latestPreRawTag } },
    rawTag = latestPreRawTag.ifBlank { preReleaseInfo },
    rawName = latestPreName,
    link = latestPreUrl,
    preciseApkVersion = latestPreApkVersion,
)
