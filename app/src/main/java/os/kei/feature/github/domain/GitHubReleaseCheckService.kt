package os.kei.feature.github.domain

import android.content.Context
import os.kei.feature.github.data.remote.GitHubApiTokenReleaseStrategy
import os.kei.feature.github.data.remote.GitHubAtomReleaseStrategy
import os.kei.feature.github.data.remote.GitHubReleaseLookupStrategy
import os.kei.feature.github.data.remote.GitHubReleaseStrategyRegistry
import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubReleaseChannel
import os.kei.feature.github.model.GitHubRepositoryReleaseSnapshot
import os.kei.feature.github.model.GitHubCheckCacheEntry
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedReleaseCheck
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import java.io.IOException

object GitHubReleaseCheckService {
    private const val transientRetryCount = 1

    fun evaluateTrackedApp(
        context: Context,
        item: GitHubTrackedApp,
        strategy: GitHubReleaseLookupStrategy? = null
    ): GitHubTrackedReleaseCheck {
        val lookupConfig = GitHubReleaseStrategyRegistry.loadLookupConfig()
        val localVersionInfo = runCatching {
            GitHubVersionUtils.localVersionInfoOrNull(context, item.packageName)
        }.getOrNull()
        val localVersion = localVersionInfo?.versionName.orEmpty()
        val localVersionCode = localVersionInfo?.versionCode ?: -1L
        val effectiveStrategy = strategy ?: GitHubReleaseStrategyRegistry.resolveConfiguredStrategy().getOrElse { error ->
            return GitHubTrackedReleaseCheck(
                strategyId = lookupConfig.selectedStrategy.storageId,
                localVersion = localVersion,
                localVersionCode = localVersionCode,
                status = GitHubTrackedReleaseStatus.Failed,
                message = "检查失败: ${error.message ?: "unknown"}"
            )
        }

        val snapshot = loadSnapshotWithFallback(
            owner = item.owner,
            repo = item.repo,
            strategy = effectiveStrategy,
            lookupConfig = lookupConfig,
            allowFallback = strategy == null
        ).getOrElse { error ->
            return GitHubTrackedReleaseCheck(
                strategyId = effectiveStrategy.id,
                localVersion = localVersion,
                localVersionCode = localVersionCode,
                status = GitHubTrackedReleaseStatus.Failed,
                message = "检查失败: ${error.message ?: "unknown"}"
            )
        }

        return evaluateSnapshot(
            item = item,
            localVersion = localVersion,
            localVersionCode = localVersionCode,
            snapshot = snapshot,
            checkAllTrackedPreReleases = lookupConfig.checkAllTrackedPreReleases
        )
    }

    internal fun evaluateSnapshot(
        item: GitHubTrackedApp,
        localVersion: String,
        localVersionCode: Long,
        snapshot: GitHubRepositoryReleaseSnapshot,
        checkAllTrackedPreReleases: Boolean = false
    ): GitHubTrackedReleaseCheck {
        val matchedEntry = snapshot.feed.entries.firstOrNull { entry ->
            GitHubVersionUtils.compareVersionToStructuredCandidates(localVersion, entry.versionCandidates) == 0
        }
        val matchedCurrentStable = snapshot.hasStableRelease &&
            matchedEntry != null &&
            GitHubVersionUtils.compareCandidateSetsWithSources(
                matchedEntry.versionCandidates.map { it.value },
                snapshot.latestStable.versionCandidates
            ) == 0
        val latestStable = snapshot.latestStable.takeIf { snapshot.hasStableRelease }
        val latestPre = snapshot.latestPreRelease
        val hasOnlyPreReleases = !snapshot.hasStableRelease && latestPre != null
        val localChannel = when {
            matchedCurrentStable -> GitHubReleaseChannel.STABLE
            else -> matchedEntry?.channel
        }
            ?: GitHubVersionUtils.classifyVersionChannel(localVersion)
            ?: GitHubReleaseChannel.UNKNOWN
        val isLocalPreReleaseInstalled =
            (matchedEntry?.isLikelyPreRelease == true && !matchedCurrentStable) || localChannel.isPreRelease
        val inspectPreRelease = checkAllTrackedPreReleases || item.preferPreRelease || isLocalPreReleaseInstalled

        val stableCmp = latestStable?.let {
            GitHubVersionUtils.compareVersionToStructuredCandidates(localVersion, it.versionCandidates)
        }
        val latestPreIsRelevant = when {
            latestPre == null -> false
            latestStable == null -> true
            else -> GitHubVersionUtils.isRelevantPreRelease(
                preReleaseCandidates = latestPre.versionCandidates,
                stableCandidates = latestStable.versionCandidates,
                preReleaseUpdatedAtMillis = latestPre.updatedAtMillis,
                stableUpdatedAtMillis = latestStable.updatedAtMillis
            )
        }
        val latestPreCmp = if (latestPre != null) {
            GitHubVersionUtils.compareVersionToStructuredCandidates(localVersion, latestPre.versionCandidates)
        } else {
            null
        }

        val hasPreReleaseUpdate = inspectPreRelease &&
            latestPreIsRelevant &&
            (latestPreCmp?.let { it < 0 } == true)
        val stableHasUpdate = stableCmp?.let { it < 0 } == true
        val recommendsPreRelease = hasPreReleaseUpdate &&
            (item.preferPreRelease || (isLocalPreReleaseInstalled && !stableHasUpdate))
        val hasUpdate = stableHasUpdate || recommendsPreRelease

        val preReleaseInfo = when {
            inspectPreRelease && latestPre != null -> latestPre.displayVersion
            inspectPreRelease && isLocalPreReleaseInstalled && matchedEntry != null -> matchedEntry.displayVersion
            else -> ""
        }
        val showPreReleaseInfo = inspectPreRelease && preReleaseInfo.isNotBlank()
        val releaseHint = when {
            hasOnlyPreReleases && !inspectPreRelease -> "该项目暂时可能只有预发行版"
            else -> ""
        }

        val status = when {
            recommendsPreRelease -> GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable
            stableHasUpdate -> GitHubTrackedReleaseStatus.UpdateAvailable
            hasPreReleaseUpdate -> GitHubTrackedReleaseStatus.PreReleaseOptional
            inspectPreRelease && isLocalPreReleaseInstalled -> GitHubTrackedReleaseStatus.PreReleaseTracked
            stableCmp != null && hasUpdate == false -> GitHubTrackedReleaseStatus.UpToDate
            matchedEntry != null -> GitHubTrackedReleaseStatus.MatchedRelease
            else -> GitHubTrackedReleaseStatus.ComparisonUncertain
        }

        return GitHubTrackedReleaseCheck(
            strategyId = snapshot.strategyId,
            localVersion = localVersion,
            localVersionCode = localVersionCode,
            matchedRelease = matchedEntry,
            stableRelease = latestStable,
            preRelease = latestPre,
            hasStableRelease = snapshot.hasStableRelease,
            hasUpdate = hasUpdate,
            hasPreReleaseUpdate = hasPreReleaseUpdate,
            recommendsPreRelease = recommendsPreRelease,
            isPreReleaseInstalled = isLocalPreReleaseInstalled,
            preReleaseInfo = preReleaseInfo,
            showPreReleaseInfo = showPreReleaseInfo,
            releaseHint = releaseHint,
            status = status,
            message = status.defaultMessage
        )
    }

    private fun loadSnapshotWithFallback(
        owner: String,
        repo: String,
        strategy: GitHubReleaseLookupStrategy,
        lookupConfig: GitHubLookupConfig,
        allowFallback: Boolean
    ): Result<GitHubRepositoryReleaseSnapshot> {
        val primaryResult = loadSnapshotWithTransientRetry(
            strategy = strategy,
            owner = owner,
            repo = repo
        )
        if (primaryResult.isSuccess) return primaryResult

        val primaryError = primaryResult.exceptionOrNull() ?: IllegalStateException("unknown")
        val fallbackStrategy = if (allowFallback && primaryError.shouldTryStrategyFallback()) {
            resolveFallbackStrategy(
                primaryStrategyId = strategy.id,
                lookupConfig = lookupConfig
            )
        } else {
            null
        } ?: return primaryResult

        val fallbackResult = loadSnapshotWithTransientRetry(
            strategy = fallbackStrategy,
            owner = owner,
            repo = repo
        )
        if (fallbackResult.isSuccess) return fallbackResult

        val fallbackError = fallbackResult.exceptionOrNull()
        val message = buildString {
            append("Primary strategy failed(")
            append(strategy.id)
            append("): ")
            append(primaryError.message ?: "unknown")
            append("; fallback strategy failed(")
            append(fallbackStrategy.id)
            append("): ")
            append(fallbackError?.message ?: "unknown")
        }
        return Result.failure(
            IllegalStateException(message, fallbackError ?: primaryError)
        )
    }

    private fun loadSnapshotWithTransientRetry(
        strategy: GitHubReleaseLookupStrategy,
        owner: String,
        repo: String
    ): Result<GitHubRepositoryReleaseSnapshot> {
        var latestResult = strategy.loadSnapshot(owner, repo)
        if (latestResult.isSuccess) return latestResult

        repeat(transientRetryCount) {
            val error = latestResult.exceptionOrNull() ?: return latestResult
            if (!error.shouldTryStrategyFallback()) {
                return latestResult
            }
            strategy.clearCaches()
            latestResult = strategy.loadSnapshot(owner, repo)
            if (latestResult.isSuccess) return latestResult
        }
        return latestResult
    }

    private fun resolveFallbackStrategy(
        primaryStrategyId: String,
        lookupConfig: GitHubLookupConfig
    ): GitHubReleaseLookupStrategy? {
        return when (primaryStrategyId) {
            GitHubLookupStrategyOption.AtomFeed.storageId -> {
                val token = lookupConfig.apiToken.trim()
                if (token.isBlank()) {
                    null
                } else {
                    GitHubApiTokenReleaseStrategy(apiToken = token)
                }
            }

            GitHubLookupStrategyOption.GitHubApiToken.storageId -> {
                GitHubAtomReleaseStrategy
            }

            else -> null
        }
    }

    private fun Throwable.shouldTryStrategyFallback(): Boolean {
        var current: Throwable? = this
        var depth = 0
        while (current != null && depth < 8) {
            val message = current.message.orEmpty().lowercase()
            if (
                message.contains("http 5") ||
                message.contains("http 429") ||
                message.contains("timeout") ||
                message.contains("timed out") ||
                message.contains("connection reset") ||
                message.contains("connection closed") ||
                message.contains("failed to connect") ||
                message.contains("unable to resolve host") ||
                message.contains("network")
            ) {
                return true
            }
            if (current is IOException) return true
            current = current.cause
            depth += 1
        }
        return false
    }

    fun GitHubTrackedReleaseCheck.toCacheEntry(): GitHubCheckCacheEntry {
        return GitHubCheckCacheEntry(
            loading = false,
            localVersion = localVersion,
            localVersionCode = localVersionCode,
            latestTag = stableRelease?.displayVersion.orEmpty(),
            latestStableName = stableRelease?.rawName.orEmpty(),
            latestStableRawTag = stableRelease?.rawTag.orEmpty(),
            latestStableUrl = stableRelease?.link.orEmpty(),
            latestStableUpdatedAtMillis = stableRelease?.updatedAtMillis ?: -1L,
            latestPreName = preRelease?.rawName.orEmpty(),
            latestPreRawTag = preRelease?.rawTag.orEmpty(),
            latestPreUrl = preRelease?.link.orEmpty(),
            latestPreUpdatedAtMillis = preRelease?.updatedAtMillis ?: -1L,
            hasStableRelease = hasStableRelease,
            hasUpdate = hasUpdate,
            message = message,
            isPreRelease = isPreReleaseInstalled,
            preReleaseInfo = preReleaseInfo,
            showPreReleaseInfo = showPreReleaseInfo,
            hasPreReleaseUpdate = hasPreReleaseUpdate,
            recommendsPreRelease = recommendsPreRelease,
            releaseHint = releaseHint,
            sourceStrategyId = strategyId
        )
    }

    fun fromCacheEntry(entry: GitHubCheckCacheEntry): GitHubTrackedReleaseCheck {
        return GitHubTrackedReleaseCheck(
            strategyId = entry.sourceStrategyId.ifBlank { GitHubAtomReleaseStrategy.id },
            localVersion = entry.localVersion,
            localVersionCode = entry.localVersionCode,
            stableRelease = entry
                .takeIf {
                    it.hasStableRelease &&
                        (it.latestStableRawTag.isNotBlank() || it.latestStableName.isNotBlank() || it.latestTag.isNotBlank())
                }
                ?.let {
                os.kei.feature.github.model.GitHubReleaseVersionSignals(
                    displayVersion = it.latestStableName.ifBlank { it.latestTag.ifBlank { it.latestStableRawTag } },
                    rawTag = it.latestStableRawTag.ifBlank { it.latestTag },
                    rawName = it.latestStableName.ifBlank { it.latestTag.ifBlank { it.latestStableRawTag } },
                    link = entry.latestStableUrl,
                    updatedAtMillis = entry.latestStableUpdatedAtMillis.takeIf { ts -> ts > 0L }
                )
            },
            preRelease = entry
                .takeIf { it.latestPreRawTag.isNotBlank() || it.latestPreName.isNotBlank() || it.preReleaseInfo.isNotBlank() }
                ?.let {
                os.kei.feature.github.model.GitHubReleaseVersionSignals(
                    displayVersion = it.latestPreName.ifBlank { it.preReleaseInfo.ifBlank { it.latestPreRawTag } },
                    rawTag = it.latestPreRawTag.ifBlank { it.preReleaseInfo },
                    rawName = it.latestPreName.ifBlank { it.preReleaseInfo.ifBlank { it.latestPreRawTag } },
                    link = entry.latestPreUrl,
                    updatedAtMillis = entry.latestPreUpdatedAtMillis.takeIf { ts -> ts > 0L }
                )
            },
            hasStableRelease = entry.hasStableRelease,
            hasUpdate = entry.hasUpdate,
            hasPreReleaseUpdate = entry.hasPreReleaseUpdate,
            recommendsPreRelease = entry.recommendsPreRelease ||
                entry.message == GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable.defaultMessage,
            isPreReleaseInstalled = entry.isPreRelease,
            preReleaseInfo = entry.preReleaseInfo,
            showPreReleaseInfo = entry.showPreReleaseInfo,
            releaseHint = entry.releaseHint,
            status = GitHubTrackedReleaseStatus.entries.firstOrNull { it.defaultMessage == entry.message }
                ?: if (entry.message.startsWith("检查失败")) {
                    GitHubTrackedReleaseStatus.Failed
                } else {
                    GitHubTrackedReleaseStatus.ComparisonUncertain
                },
            message = entry.message
        )
    }
}
