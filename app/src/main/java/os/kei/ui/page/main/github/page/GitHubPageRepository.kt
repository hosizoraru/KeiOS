package os.kei.ui.page.main.github.page

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.InstalledAppItem
import os.kei.ui.page.main.github.GitHubSortMode
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import os.kei.ui.page.main.github.query.queryDownloaderOptions
import os.kei.ui.page.main.github.query.queryOnlineShareTargetOptions
import os.kei.ui.page.main.github.section.GitHubOverviewMetrics
import os.kei.ui.page.main.github.share.GitHubPendingShareImportTrack

private const val pendingShareImportCardVisibleWindowMs = 90_000L

internal data class GitHubPageContentInput(
    val trackedItems: List<GitHubTrackedApp>,
    val trackedSearch: String,
    val sortMode: GitHubSortMode,
    val checkStates: Map<String, VersionCheckUi>,
    val appList: List<InstalledAppItem>,
    val trackedFirstInstallAtByPackage: Map<String, Long>,
    val trackedAddedAtById: Map<String, Long>,
    val pendingShareImportTrack: GitHubPendingShareImportTrack?,
    val nowMillis: Long
)

internal data class GitHubOnlineShareTargetInput(
    val shouldResolve: Boolean,
    val appList: List<InstalledAppItem>
)

internal class GitHubPageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend fun buildContentState(input: GitHubPageContentInput): GitHubPageContentDerivedState {
        return withContext(defaultDispatcher) {
            val filteredTracked = input.trackedItems.filter { item ->
                input.trackedSearch.isBlank() ||
                    item.owner.contains(input.trackedSearch, ignoreCase = true) ||
                    item.repo.contains(input.trackedSearch, ignoreCase = true) ||
                    item.appLabel.contains(input.trackedSearch, ignoreCase = true) ||
                    item.packageName.contains(input.trackedSearch, ignoreCase = true)
            }
            val isSortUpdatable: (GitHubTrackedApp) -> Boolean = { item ->
                item.alwaysShowLatestReleaseDownloadButton || input.checkStates[item.id]?.hasUpdate == true
            }
            val sortedTracked = when (input.sortMode) {
                GitHubSortMode.UpdateFirst -> filteredTracked.sortedWith(
                    compareByDescending<GitHubTrackedApp> { isSortUpdatable(it) }
                        .thenByDescending { input.checkStates[it.id]?.hasPreReleaseUpdate == true }
                        .thenBy { it.appLabel.lowercase() }
                )

                GitHubSortMode.NameAsc -> filteredTracked.sortedBy { it.appLabel.lowercase() }
                GitHubSortMode.PreReleaseFirst -> filteredTracked.sortedWith(
                    compareByDescending<GitHubTrackedApp> {
                        input.checkStates[it.id]?.isPreRelease == true
                    }
                        .thenByDescending { isSortUpdatable(it) }
                        .thenBy { it.appLabel.lowercase() }
                )
            }
            val trackedCount = input.trackedItems.size
            val updatableCount = input.trackedItems.count { input.checkStates[it.id]?.hasUpdate == true }
            val preReleaseCount = input.trackedItems.count { input.checkStates[it.id]?.isPreRelease == true }
            val preReleaseUpdateCount =
                input.trackedItems.count { input.checkStates[it.id]?.hasPreReleaseUpdate == true }
            val failedCount = input.trackedItems.count { input.checkStates[it.id]?.failed == true }
            val stableLatestCount = input.trackedItems.count {
                val itemState = input.checkStates[it.id]
                itemState?.hasUpdate == false && itemState.isPreRelease.not()
            }
            val appLastUpdatedAtByTrackId = buildAppLastUpdatedAtByTrackId(input)
            val pendingShareImportRepoOverlapCount = input.pendingShareImportTrack?.let { pending ->
                input.trackedItems.count { item ->
                    item.owner.equals(pending.owner, ignoreCase = true) &&
                        item.repo.equals(pending.repo, ignoreCase = true)
                }
            } ?: 0
            val showPendingShareImportCard = input.pendingShareImportTrack?.let { pending ->
                val ageMs = (input.nowMillis - pending.armedAtMillis).coerceAtLeast(0L)
                ageMs <= pendingShareImportCardVisibleWindowMs ||
                    pendingShareImportRepoOverlapCount > 0
            } ?: false
            GitHubPageContentDerivedState(
                trackedUi = GitHubPageDerivedState(
                    filteredTracked = filteredTracked,
                    sortedTracked = sortedTracked,
                    overviewMetrics = GitHubOverviewMetrics(
                        trackedCount = trackedCount,
                        updatableCount = updatableCount,
                        stableLatestCount = stableLatestCount,
                        preReleaseCount = preReleaseCount,
                        preReleaseUpdateCount = preReleaseUpdateCount,
                        failedCount = failedCount
                    )
                ),
                appLastUpdatedAtByTrackId = appLastUpdatedAtByTrackId,
                pendingShareImportRepoOverlapCount = pendingShareImportRepoOverlapCount,
                showPendingShareImportCard = showPendingShareImportCard
            )
        }
    }

    suspend fun queryOnlineShareTargets(
        context: Context,
        input: GitHubOnlineShareTargetInput
    ): List<OnlineShareTargetOption> {
        if (!input.shouldResolve) return emptyList()
        return withContext(defaultDispatcher) {
            queryOnlineShareTargetOptions(context, input.appList)
        }
    }

    suspend fun queryDownloaders(context: Context): List<DownloaderOption> {
        return withContext(defaultDispatcher) {
            queryDownloaderOptions(context)
        }
    }

    suspend fun buildTrackedItemsExportJson(
        items: List<GitHubTrackedApp>,
        exportedAtMillis: Long
    ): String {
        return withContext(defaultDispatcher) {
            GitHubTrackStore.buildTrackedItemsExportJson(
                items = items,
                exportedAtMillis = exportedAtMillis
            )
        }
    }

    suspend fun writeText(
        contentResolver: ContentResolver,
        uri: Uri,
        content: String
    ) {
        withContext(ioDispatcher) {
            contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                checkNotNull(writer) { "openOutputStream returned null" }
                writer.write(content)
            }
        }
    }

    suspend fun readText(
        contentResolver: ContentResolver,
        uri: Uri
    ): String {
        return withContext(ioDispatcher) {
            contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                checkNotNull(reader) { "openInputStream returned null" }
                reader.readText()
            }
        }
    }

    private fun buildAppLastUpdatedAtByTrackId(
        input: GitHubPageContentInput
    ): Map<String, Long> {
        val appUpdatedAtByPackage = buildMap {
            input.trackedFirstInstallAtByPackage.forEach { (packageName, firstInstallAtMillis) ->
                if (packageName.isNotBlank() && firstInstallAtMillis > 0L) {
                    put(packageName, firstInstallAtMillis)
                }
            }
            input.appList
                .filter { it.packageName.isNotBlank() && it.lastUpdateTimeMs > 0L }
                .forEach { put(it.packageName, it.lastUpdateTimeMs) }
        }
        return buildMap {
            input.trackedItems.forEach { item ->
                val byPackage = appUpdatedAtByPackage[item.packageName]
                val byTrackId = input.trackedAddedAtById[item.id]
                val updatedAt = byPackage?.takeIf { it > 0L } ?: byTrackId?.takeIf { it > 0L }
                if (updatedAt != null) {
                    put(item.id, updatedAt)
                }
            }
        }
    }
}
