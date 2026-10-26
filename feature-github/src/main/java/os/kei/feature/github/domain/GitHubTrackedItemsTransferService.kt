package os.kei.feature.github.domain

import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload
import os.kei.feature.github.data.local.GitHubTrackedItemsOptionCounts
import os.kei.feature.github.data.local.GitHubTrackedItemsSourceCounts
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.data.local.GitHubTrackStoreSignals
import os.kei.feature.github.data.local.GitHubApkManifestInfoCacheStore
import os.kei.feature.github.data.local.GitHubReleaseAssetCacheStore
import os.kei.feature.github.data.local.GitHubStarImportApkVerificationCacheStore
import os.kei.feature.github.data.local.fdroid.FdroidMetadataSidecarStore

data class GitHubTrackedItemsImportPreview(
    val payload: GitHubTrackedItemsImportPayload,
    val fileItemCount: Int,
    val validCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val newCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int,
    val mergedCount: Int,
    val githubRepositoryCount: Int = 0,
    val gitRepositoryCount: Int = 0,
    val directApkCount: Int = 0,
    val fdroidRepositoryCount: Int = 0,
    val preferPreReleaseCount: Int = 0,
    val latestReleaseDownloadCount: Int = 0,
    val actionsUpdateCount: Int = 0,
    val updateIntervalOverrideCount: Int = 0,
    val preciseApkVersionOverrideCount: Int = 0,
) {
    val canImport: Boolean
        get() = validCount > 0
}

object GitHubTrackedItemsTransferService {
    fun replaceFromSync(payload: GitHubTrackedItemsImportPayload, onRefreshNeeded: () -> Unit) {
        require(payload.invalidCount == 0) { "The backup contains invalid tracking entries" }
        val previous = GitHubTrackStore.load()
        GitHubTrackStore.save(payload.items)
        val affectedIds = (previous + payload.items).map { it.id }.toSet()
        GitHubTrackStore.removeCheckCacheEntries(affectedIds)
        affectedIds.forEach(FdroidMetadataSidecarStore::clear)
        GitHubReleaseAssetCacheStore.clearAll()
        GitHubApkManifestInfoCacheStore.clearAll()
        GitHubStarImportApkVerificationCacheStore.clearAll()
        GitHubTrackChangeHistoryService().recordChangesBlocking(
            previousItems = previous,
            nextItems = payload.items,
            source = GitHubTrackChangeHistorySource.Import,
            changedAtMillis = System.currentTimeMillis(),
        )
        GitHubTrackStoreSignals.notifyChanged()
        onRefreshNeeded()
    }

    fun loadItems(): List<GitHubTrackedApp> =
        GitHubTrackStore.load()

    fun parseImport(raw: String): GitHubTrackedItemsImportPayload =
        GitHubTrackStore.parseTrackedItemsImport(raw)

    fun buildExportJson(
        items: List<GitHubTrackedApp>,
        exportedAtMillis: Long = System.currentTimeMillis(),
    ): String =
        GitHubTrackStore.buildTrackedItemsExportJson(
            items = items,
            exportedAtMillis = exportedAtMillis,
        )

    fun calculateOptionCounts(items: List<GitHubTrackedApp>): GitHubTrackedItemsOptionCounts =
        GitHubTrackStore.calculateTrackedItemsOptionCounts(items)

    fun calculateSourceCounts(items: List<GitHubTrackedApp>): GitHubTrackedItemsSourceCounts =
        GitHubTrackStore.calculateTrackedItemsSourceCounts(items)

    fun buildImportPreview(
        payload: GitHubTrackedItemsImportPayload,
        existingItems: List<GitHubTrackedApp>,
    ): GitHubTrackedItemsImportPreview {
        val existingItemsById = existingItems.associateBy { it.id }
        var newCount = 0
        var updatedCount = 0
        var unchangedCount = 0
        payload.items.forEach { item ->
            when (val existingItem = existingItemsById[item.id]) {
                null -> newCount += 1
                item -> unchangedCount += 1
                else -> updatedCount += 1
            }
        }
        val optionCounts = GitHubTrackStore.calculateTrackedItemsOptionCounts(payload.items)
        val sourceCounts = GitHubTrackStore.calculateTrackedItemsSourceCounts(payload.items)
        return GitHubTrackedItemsImportPreview(
            payload = payload,
            fileItemCount = payload.sourceCount,
            validCount = payload.items.size,
            duplicateCount = payload.duplicateCount,
            invalidCount = payload.invalidCount,
            newCount = newCount,
            updatedCount = updatedCount,
            unchangedCount = unchangedCount,
            mergedCount = existingItems.size + newCount,
            githubRepositoryCount = sourceCounts.githubRepositoryCount,
            gitRepositoryCount = sourceCounts.gitRepositoryCount,
            directApkCount = sourceCounts.directApkCount,
            fdroidRepositoryCount = sourceCounts.fdroidRepositoryCount,
            preferPreReleaseCount = optionCounts.preferPreReleaseCount,
            latestReleaseDownloadCount = optionCounts.latestReleaseDownloadCount,
            actionsUpdateCount = optionCounts.actionsUpdateCount,
            updateIntervalOverrideCount = optionCounts.updateIntervalOverrideCount,
            preciseApkVersionOverrideCount = optionCounts.preciseApkVersionOverrideCount,
        )
    }

    fun applyImport(
        payload: GitHubTrackedItemsImportPayload,
        onRefreshNeeded: () -> Unit,
        existingItems: List<GitHubTrackedApp> = GitHubTrackStore.load(),
    ): GitHubTrackedItemsImportApplyResult =
        GitHubTrackedItemsImportApplier.apply(
            payload = payload,
            onRefreshNeeded = onRefreshNeeded,
            existingItems = existingItems,
        )
}
