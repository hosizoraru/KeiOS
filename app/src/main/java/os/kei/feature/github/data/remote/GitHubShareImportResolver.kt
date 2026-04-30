package os.kei.feature.github.data.remote

import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubAtomReleaseEntry
import java.io.IOException

internal data class GitHubShareImportAssetPlan(
    val parsedLink: GitHubSharedReleaseLink,
    val resolvedReleaseTag: String,
    val resolvedReleaseUrl: String,
    val assets: List<GitHubReleaseAssetFile>,
    val preferredAssetName: String = ""
)

private data class ResolvedReleaseTarget(
    val tag: String,
    val releaseUrl: String,
    val preferredAssetName: String = ""
)

private data class ShareImportResolveAttempt(
    val target: ResolvedReleaseTarget,
    val fetchedBundle: GitHubReleaseAssetBundle
)

internal object GitHubShareImportResolver {
    fun resolve(
        sharedText: String,
        lookupConfig: GitHubLookupConfig
    ): Result<GitHubShareImportAssetPlan> = runCatching {
        val parsedLink = GitHubShareIntentParser.parseSharedReleaseLink(sharedText)
            ?: error("No valid GitHub link was detected")

        val attempt = resolveWithFallbackStrategies(
            parsedLink = parsedLink,
            lookupConfig = lookupConfig
        )

        val apkAssets = attempt.fetchedBundle.assets
            .filter { asset ->
                asset.name.endsWith(".apk", ignoreCase = true) &&
                    !asset.name.contains("metadata", ignoreCase = true)
            }

        val mergedAssets = mergeDirectSharedAssetIfNeeded(
            strategy = lookupConfig.selectedStrategy,
            parsedLink = parsedLink,
            fetchedAssets = apkAssets
        )

        val finalAssets = when {
            mergedAssets.isNotEmpty() -> mergedAssets
            else -> {
                val fallbackAsset = buildDirectSharedAsset(parsedLink)
                if (fallbackAsset != null) {
                    listOf(fallbackAsset)
                } else {
                    error("The target release contains no usable APK")
                }
            }
        }

        GitHubShareImportAssetPlan(
            parsedLink = parsedLink,
            resolvedReleaseTag = attempt.target.tag,
            resolvedReleaseUrl = attempt.target.releaseUrl,
            assets = finalAssets,
            preferredAssetName = attempt.target.preferredAssetName
        )
    }

    private fun resolveWithFallbackStrategies(
        parsedLink: GitHubSharedReleaseLink,
        lookupConfig: GitHubLookupConfig
    ): ShareImportResolveAttempt {
        val primaryStrategy = lookupConfig.selectedStrategy
        val fallbackStrategy = primaryStrategy.fallbackStrategy()
        val attempts = listOfNotNull(primaryStrategy, fallbackStrategy)
        var lastError: Throwable? = null

        attempts.forEachIndexed { index, strategy ->
            val strategyConfig = if (strategy == primaryStrategy) {
                lookupConfig
            } else {
                lookupConfig.copy(selectedStrategy = strategy)
            }
            val attemptResult = runCatching {
                resolveOnce(
                    parsedLink = parsedLink,
                    lookupConfig = strategyConfig
                )
            }
            attemptResult.getOrNull()?.let { return it }

            val error = attemptResult.exceptionOrNull()
            lastError = error
            val canTryFallback = index < attempts.lastIndex &&
                shouldTryStrategyFallback(parsedLink = parsedLink, error = error)
            if (!canTryFallback) {
                throw error ?: IllegalStateException("Share import resolution failed")
            }
        }

        throw lastError ?: IllegalStateException("Share import resolution failed")
    }

    private fun resolveOnce(
        parsedLink: GitHubSharedReleaseLink,
        lookupConfig: GitHubLookupConfig
    ): ShareImportResolveAttempt {
        val strategy = lookupConfig.selectedStrategy
        val target = resolveReleaseTarget(parsedLink, lookupConfig)
        val preferHtml = strategy == GitHubLookupStrategyOption.AtomFeed
        val fetchedBundle = GitHubReleaseAssetRepository.fetchApkAssets(
            owner = parsedLink.owner,
            repo = parsedLink.repo,
            rawTag = target.tag,
            releaseUrl = target.releaseUrl,
            preferHtml = preferHtml,
            aggressiveFiltering = lookupConfig.aggressiveApkFiltering,
            includeAllAssets = false,
            apiToken = lookupConfig.apiToken
        ).getOrElse { error ->
            if (
                parsedLink.type == GitHubSharedUrlType.ReleaseDownloadAsset &&
                strategy == GitHubLookupStrategyOption.AtomFeed
            ) {
                val fallbackAsset = buildDirectSharedAsset(parsedLink)
                if (fallbackAsset != null) {
                    return@getOrElse GitHubReleaseAssetBundle(
                        releaseName = target.tag,
                        tagName = target.tag,
                        htmlUrl = target.releaseUrl,
                        assets = listOf(fallbackAsset)
                    )
                }
            }
            throw error
        }
        return ShareImportResolveAttempt(
            target = target,
            fetchedBundle = fetchedBundle
        )
    }

    private fun shouldTryStrategyFallback(
        parsedLink: GitHubSharedReleaseLink,
        error: Throwable?
    ): Boolean {
        if (error == null) return true
        return when (parsedLink.type) {
            GitHubSharedUrlType.Repo,
            GitHubSharedUrlType.Releases,
            GitHubSharedUrlType.ReleasesLatest -> true
            GitHubSharedUrlType.ReleaseTag,
            GitHubSharedUrlType.ReleaseDownloadAsset -> error.isTransientGitHubResolveError()
        }
    }

    private fun resolveReleaseTarget(
        parsedLink: GitHubSharedReleaseLink,
        lookupConfig: GitHubLookupConfig
    ): ResolvedReleaseTarget {
        return when (parsedLink.type) {
            GitHubSharedUrlType.Repo,
            GitHubSharedUrlType.Releases -> {
                val latest = resolveLatestReleaseEntry(
                    owner = parsedLink.owner,
                    repo = parsedLink.repo,
                    lookupConfig = lookupConfig
                )
                val tag = latest.tag.trim().ifBlank {
                    GitHubReleaseAssetRepository.parseReleaseTagFromUrl(latest.link)
                }
                val releaseUrl = latest.link.trim().ifBlank {
                    GitHubVersionUtils.buildReleaseTagUrl(parsedLink.owner, parsedLink.repo, tag)
                }
                check(tag.isNotBlank()) { "This repository has no usable release" }
                ResolvedReleaseTarget(
                    tag = tag,
                    releaseUrl = releaseUrl
                )
            }

            GitHubSharedUrlType.ReleasesLatest -> {
                resolveLatestStableReleaseTarget(
                    owner = parsedLink.owner,
                    repo = parsedLink.repo,
                    lookupConfig = lookupConfig
                )
            }

            GitHubSharedUrlType.ReleaseTag,
            GitHubSharedUrlType.ReleaseDownloadAsset -> {
                val tag = parsedLink.releaseTag.trim()
                check(tag.isNotBlank()) { "The shared link is missing a release tag" }
                ResolvedReleaseTarget(
                    tag = tag,
                    releaseUrl = GitHubVersionUtils.buildReleaseTagUrl(
                        parsedLink.owner,
                        parsedLink.repo,
                        tag
                    ),
                    preferredAssetName = parsedLink.assetName
                )
            }
        }
    }

    private fun resolveLatestStableReleaseTarget(
        owner: String,
        repo: String,
        lookupConfig: GitHubLookupConfig
    ): ResolvedReleaseTarget {
        val snapshot = when (lookupConfig.selectedStrategy) {
            GitHubLookupStrategyOption.AtomFeed -> {
                GitHubAtomReleaseStrategy.loadSnapshot(owner, repo).getOrThrow()
            }

            GitHubLookupStrategyOption.GitHubApiToken -> {
                GitHubApiTokenReleaseStrategy(
                    apiToken = lookupConfig.apiToken
                ).loadSnapshot(owner, repo).getOrThrow()
            }
        }
        check(snapshot.hasStableRelease) { "This repository has no stable release" }
        val latestStable = snapshot.latestStable
        val tag = latestStable.rawTag.trim().ifBlank {
            GitHubReleaseAssetRepository.parseReleaseTagFromUrl(latestStable.link)
        }
        check(tag.isNotBlank()) { "This repository has no stable release" }
        val releaseUrl = latestStable.link.trim().ifBlank {
            GitHubVersionUtils.buildReleaseTagUrl(owner, repo, tag)
        }
        return ResolvedReleaseTarget(
            tag = tag,
            releaseUrl = releaseUrl
        )
    }

    private fun resolveLatestReleaseEntry(
        owner: String,
        repo: String,
        lookupConfig: GitHubLookupConfig
    ): GitHubAtomReleaseEntry {
        val entries = when (lookupConfig.selectedStrategy) {
            GitHubLookupStrategyOption.AtomFeed -> {
                GitHubAtomReleaseStrategy.fetchReleaseEntries(owner, repo, limit = 30).getOrThrow()
            }

            GitHubLookupStrategyOption.GitHubApiToken -> {
                GitHubApiTokenReleaseStrategy(
                    apiToken = lookupConfig.apiToken
                ).fetchReleaseEntries(owner, repo, limit = 30).getOrThrow()
            }
        }
        return entries.maxWithOrNull(
            compareBy<GitHubAtomReleaseEntry> {
                it.updatedAtMillis ?: Long.MIN_VALUE
            }.thenByDescending { it.entryId }
        ) ?: entries.firstOrNull()
        ?: error("This repository has no usable release")
    }

    private fun mergeDirectSharedAssetIfNeeded(
        strategy: GitHubLookupStrategyOption,
        parsedLink: GitHubSharedReleaseLink,
        fetchedAssets: List<GitHubReleaseAssetFile>
    ): List<GitHubReleaseAssetFile> {
        if (parsedLink.type != GitHubSharedUrlType.ReleaseDownloadAsset) {
            return fetchedAssets
        }
        val sharedAssetName = parsedLink.assetName.trim()
        val directAsset = buildDirectSharedAsset(parsedLink) ?: return fetchedAssets

        val matchedIndex = fetchedAssets.indexOfFirst { asset ->
            asset.name.equals(sharedAssetName, ignoreCase = true)
        }

        return when {
            strategy == GitHubLookupStrategyOption.AtomFeed && matchedIndex >= 0 -> {
                fetchedAssets.toMutableList().apply {
                    val matched = this[matchedIndex]
                    this[matchedIndex] = matched.copy(downloadUrl = parsedLink.sourceUrl)
                }
            }

            strategy == GitHubLookupStrategyOption.AtomFeed -> {
                listOf(directAsset) + fetchedAssets
            }

            matchedIndex >= 0 -> {
                val selected = fetchedAssets[matchedIndex]
                listOf(selected) + fetchedAssets.filterIndexed { index, _ -> index != matchedIndex }
            }

            else -> fetchedAssets
        }
    }

    private fun buildDirectSharedAsset(parsedLink: GitHubSharedReleaseLink): GitHubReleaseAssetFile? {
        if (parsedLink.type != GitHubSharedUrlType.ReleaseDownloadAsset) return null
        val name = parsedLink.assetName.trim()
        if (!name.endsWith(".apk", ignoreCase = true)) return null
        return GitHubReleaseAssetFile(
            name = name,
            downloadUrl = parsedLink.sourceUrl,
            sizeBytes = 0L,
            downloadCount = 0,
            contentType = "application/vnd.android.package-archive"
        )
    }

    private fun GitHubLookupStrategyOption.fallbackStrategy(): GitHubLookupStrategyOption? {
        return when (this) {
            GitHubLookupStrategyOption.AtomFeed -> GitHubLookupStrategyOption.GitHubApiToken
            GitHubLookupStrategyOption.GitHubApiToken -> GitHubLookupStrategyOption.AtomFeed
        }
    }

    private fun Throwable.isTransientGitHubResolveError(): Boolean {
        if (this is IOException) return true
        var current: Throwable? = this
        var depth = 0
        while (current != null && depth < 8) {
            val message = current.message.orEmpty().lowercase()
            if (
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
}
