package os.kei.ui.page.main.github.share

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import os.kei.R
import os.kei.core.background.AppBackgroundScheduler
import os.kei.core.download.AppPrivateDownloadManager
import os.kei.core.intent.SafeExternalIntents
import os.kei.core.system.AppPackageChangedEvent
import os.kei.core.system.AppPackageChangedEvents
import os.kei.feature.github.data.local.GitHubPendingShareImportTrackRecord
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.local.GitHubTrackStoreSignals
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.data.remote.GitHubReleaseAssetRepository
import os.kei.feature.github.data.remote.GitHubShareImportResolver
import os.kei.feature.github.domain.GitHubReleaseCheckService
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.query.systemDownloadManagerOption
import os.kei.ui.page.main.github.state.toCacheEntry
import os.kei.ui.page.main.github.state.toUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val shareImportTrackMaxAgeMs = 25 * 60 * 1000L
internal const val shareImportTrackUpdateToleranceMs = 2 * 60 * 1000L
internal const val shareImportMinHandleIntervalMs = 1200L

internal val shareImportAttachActions = setOf(
    Intent.ACTION_PACKAGE_ADDED,
    Intent.ACTION_PACKAGE_REPLACED,
    Intent.ACTION_PACKAGE_CHANGED
)

internal data class ShareImportInstalledPackageSnapshot(
    val packageName: String,
    val appLabel: String,
    val lastUpdateTimeMs: Long,
    val firstInstallTimeMs: Long
)

internal sealed interface ShareImportDeliveryResult {
    data class Success(val toastResId: Int) : ShareImportDeliveryResult
    data class Failure(val toastResId: Int) : ShareImportDeliveryResult
}

internal sealed interface ShareImportAttachResult {
    data class Added(val appLabel: String) : ShareImportAttachResult
    data object Duplicate : ShareImportAttachResult
    data class Failed(val message: String) : ShareImportAttachResult
}
internal suspend fun sendAssetToConfiguredChannel(
    context: Context,
    lookupConfig: GitHubLookupConfig,
    asset: GitHubReleaseAssetFile
): ShareImportDeliveryResult {
    val resolvedUrl = SafeExternalIntents.httpsExternalUrlOrNull(
        resolvePreferredAssetUrl(lookupConfig, asset)
    ) ?: return ShareImportDeliveryResult.Failure(R.string.github_toast_open_downloader_failed)
    val onlineSharePackage = lookupConfig.onlineShareTargetPackage.trim()
    if (onlineSharePackage.isNotBlank()) {
        val intent = SafeExternalIntents.textShareIntent(
            text = resolvedUrl,
            subject = asset.name,
            targetPackage = onlineSharePackage,
            extras = mapOf(
                "channel" to "Online",
                "extra_channel" to "Online",
                "online_channel" to true
            )
        )
        return runCatching {
            context.startActivity(intent)
            ShareImportDeliveryResult.Success(R.string.github_toast_downloader_selected)
        }.getOrElse {
            ShareImportDeliveryResult.Failure(R.string.github_toast_share_link_failed)
        }
    }

    val preferredPackage = lookupConfig.preferredDownloaderPackage.trim()
    val systemDmPackage = systemDownloadManagerOption(context).packageName
    if (preferredPackage == systemDmPackage) {
        return runCatching {
            enqueueWithSystemDownloadManager(context, resolvedUrl, asset.name)
            ShareImportDeliveryResult.Success(R.string.github_toast_downloader_system_builtin)
        }.getOrElse {
            ShareImportDeliveryResult.Failure(R.string.github_toast_open_downloader_failed)
        }
    }
    if (preferredPackage.isBlank()) {
        return if (SafeExternalIntents.startBrowsableUrl(context, resolvedUrl)) {
            ShareImportDeliveryResult.Success(R.string.github_toast_downloader_system_default)
        } else {
            ShareImportDeliveryResult.Failure(R.string.github_toast_open_downloader_failed)
        }
    }

    return runCatching {
        require(SafeExternalIntents.startBrowsableUrl(context, resolvedUrl, preferredPackage))
        ShareImportDeliveryResult.Success(R.string.github_toast_downloader_selected)
    }.recoverCatching {
        require(SafeExternalIntents.startBrowsableUrl(context, resolvedUrl))
        ShareImportDeliveryResult.Success(R.string.github_toast_downloader_fallback_system)
    }.getOrElse {
        ShareImportDeliveryResult.Failure(R.string.github_toast_open_downloader_failed)
    }
}

internal suspend fun resolvePreferredAssetUrl(
    lookupConfig: GitHubLookupConfig,
    asset: GitHubReleaseAssetFile
): String {
    val token = lookupConfig.apiToken.trim()
    val preferApiAsset = lookupConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken
    return withContext(Dispatchers.IO) {
        GitHubReleaseAssetRepository.resolvePreferredDownloadUrl(
            asset = asset,
            useApiAssetUrl = preferApiAsset,
            apiToken = token
        ).getOrElse { asset.downloadUrl }
    }
}

internal fun enqueueWithSystemDownloadManager(
    context: Context,
    url: String,
    fileName: String
) {
    AppPrivateDownloadManager.enqueueHttpsDownload(
        context = context,
        url = url,
        fileName = fileName
    )
}

internal suspend fun attachCandidateToTracked(
    context: Context,
    candidate: GitHubPendingShareImportAttachCandidate,
    prefetchLatestCheck: Boolean = true
): ShareImportAttachResult {
    return withContext(Dispatchers.IO) {
        val trackedItems = GitHubTrackStore.load().toMutableList()
        val candidateId = "${candidate.owner}/${candidate.repo}|${candidate.packageName}"
        if (trackedItems.any { it.id == candidateId }) {
            return@withContext ShareImportAttachResult.Duplicate
        }

        val trackedItem = GitHubTrackedApp(
            repoUrl = candidate.projectUrl,
            owner = candidate.owner,
            repo = candidate.repo,
            packageName = candidate.packageName,
            appLabel = candidate.appLabel.ifBlank { candidate.packageName }
        )
        trackedItems.add(trackedItem)
        GitHubTrackStore.save(trackedItems)
        saveTrackedFirstInstallAtFallback(candidate)
        saveTrackedAddedAtFallback(trackedItem.id, candidate.detectedAtMillis)
        AppBackgroundScheduler.scheduleGitHubRefresh(context)

        if (prefetchLatestCheck) {
            runCatching {
                val refreshedUi = GitHubReleaseCheckService.evaluateTrackedApp(context, trackedItem).toUi()
                val (cache, _) = GitHubTrackStore.loadCheckCache()
                val updatedCache = cache.toMutableMap().apply {
                    put(trackedItem.id, refreshedUi.toCacheEntry())
                }
                GitHubTrackStore.saveCheckCache(updatedCache, System.currentTimeMillis())
            }
        }
        GitHubTrackStoreSignals.requestTrackRefresh(trackedItem.id)

        ShareImportAttachResult.Added(trackedItem.appLabel.ifBlank { trackedItem.packageName })
    }
}

internal fun saveTrackedFirstInstallAtFallback(candidate: GitHubPendingShareImportAttachCandidate) {
    val packageName = candidate.packageName.trim()
    if (packageName.isBlank()) return
    val firstInstallAtMillis = candidate.firstInstallTimeMs
        .takeIf { it > 0L }
        ?: candidate.detectedAtMillis
    if (firstInstallAtMillis <= 0L) return

    val existing = GitHubTrackStore.loadTrackedFirstInstallAtByPackage().toMutableMap()
    val current = existing[packageName]
    if (current == null || current <= 0L || firstInstallAtMillis < current) {
        existing[packageName] = firstInstallAtMillis
        GitHubTrackStore.saveTrackedFirstInstallAtByPackage(existing)
    }
}

internal fun saveTrackedAddedAtFallback(
    trackId: String,
    detectedAtMillis: Long
) {
    val normalizedTrackId = trackId.trim()
    if (normalizedTrackId.isBlank()) return
    val addedAtMillis = detectedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
    val existing = GitHubTrackStore.loadTrackedAddedAtById().toMutableMap()
    val current = existing[normalizedTrackId]
    if (current == null || current <= 0L || addedAtMillis < current) {
        existing[normalizedTrackId] = addedAtMillis
        GitHubTrackStore.saveTrackedAddedAtById(existing)
    }
}

internal suspend fun loadInstalledPackageSnapshot(
    context: Context,
    packageName: String
): ShareImportInstalledPackageSnapshot? {
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val info = runCatching {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }.recoverCatching {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }.getOrNull() ?: return@withContext null

        val label = runCatching {
            val appInfo = info.applicationInfo ?: pm.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
            pm.getApplicationLabel(appInfo).toString().trim()
        }.recoverCatching {
            @Suppress("DEPRECATION")
            val appInfo = info.applicationInfo ?: pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString().trim()
        }.getOrDefault("")

        ShareImportInstalledPackageSnapshot(
            packageName = packageName,
            appLabel = label,
            lastUpdateTimeMs = info.lastUpdateTime,
            firstInstallTimeMs = info.firstInstallTime
        )
    }
}

internal fun findRecentInstalledCandidateForPendingTrack(
    context: Context,
    pendingTrack: GitHubPendingShareImportTrackRecord
): ShareImportInstalledPackageSnapshot? {
    val pm = context.packageManager
    val installed = runCatching {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
    }.recoverCatching {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(0)
    }.getOrDefault(emptyList())

    val candidates = installed.asSequence()
        .filter { info ->
            val packageName = info.packageName.trim()
            packageName.isNotBlank() &&
                packageName != context.packageName &&
                pm.getLaunchIntentForPackage(packageName) != null
        }
        .mapNotNull { info ->
            val packageName = info.packageName.trim()
            val updateTime = info.lastUpdateTime
            if (updateTime <= 0L) return@mapNotNull null
            if (updateTime < (pendingTrack.armedAtMillis - shareImportTrackUpdateToleranceMs)) {
                return@mapNotNull null
            }
            val appLabel = runCatching {
                val appInfo = info.applicationInfo ?: pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
                pm.getApplicationLabel(appInfo).toString().trim()
            }.recoverCatching {
                @Suppress("DEPRECATION")
                val appInfo = info.applicationInfo ?: pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString().trim()
            }.getOrDefault("")

            ShareImportInstalledPackageSnapshot(
                packageName = packageName,
                appLabel = appLabel,
                lastUpdateTimeMs = updateTime,
                firstInstallTimeMs = info.firstInstallTime
            )
        }
        .toList()

    return selectRecentInstalledCandidateForPendingTrack(
        pendingTrack = pendingTrack,
        candidates = candidates
    )
}

internal fun selectRecentInstalledCandidateForPendingTrack(
    pendingTrack: GitHubPendingShareImportTrackRecord,
    candidates: List<ShareImportInstalledPackageSnapshot>
): ShareImportInstalledPackageSnapshot? {
    val eligible = candidates.asSequence()
        .filter { it.packageName.isNotBlank() }
        // Polling reconciliation must only pick packages updated after this share was armed.
        .filter { it.lastUpdateTimeMs >= pendingTrack.armedAtMillis }
        .sortedByDescending { it.lastUpdateTimeMs }
        .take(3)
        .toList()

    if (eligible.isEmpty()) return null
    if (eligible.size >= 2) {
        val first = eligible[0]
        val second = eligible[1]
        val updateGap = (first.lastUpdateTimeMs - second.lastUpdateTimeMs).coerceAtLeast(0L)
        if (updateGap < 90_000L) {
            return null
        }
    }
    return eligible.first()
}

internal fun isShareImportAttachEventValid(
    event: AppPackageChangedEvent,
    armedAtMillis: Long,
    packageLastUpdateTimeMs: Long
): Boolean {
    if (event.action == Intent.ACTION_PACKAGE_ADDED || event.action == Intent.ACTION_PACKAGE_REPLACED) {
        return true
    }
    if (event.action != Intent.ACTION_PACKAGE_CHANGED) return false
    if (packageLastUpdateTimeMs <= 0L) return false
    return packageLastUpdateTimeMs >= (armedAtMillis - shareImportTrackUpdateToleranceMs)
}

internal fun toast(
    context: Context,
    resId: Int,
    vararg args: Any
) {
    Toast.makeText(context, context.getString(resId, *args), Toast.LENGTH_SHORT).show()
}

internal fun Throwable.shouldSuppressShareImportFailureToast(): Boolean {
    if (this is CancellationException) return true
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth < 6) {
        val message = current.message.orEmpty()
        val className = current.javaClass.name
        if (
            message.contains("left the composition", ignoreCase = true) ||
            className.contains("LeftComposition", ignoreCase = true)
        ) {
            return true
        }
        current = current.cause
        depth += 1
    }
    return false
}
