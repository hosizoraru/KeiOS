package com.example.keios.feature.github.data.remote

import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.GitHubAtomReleaseEntry

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

internal object GitHubShareImportResolver {
    fun resolve(
        sharedText: String,
        lookupConfig: GitHubLookupConfig
    ): Result<GitHubShareImportAssetPlan> = runCatching {
        val parsedLink = GitHubShareIntentParser.parseSharedReleaseLink(sharedText)
            ?: error("未识别到有效的 GitHub 链接")

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

        val apkAssets = fetchedBundle.assets
            .filter { asset ->
                asset.name.endsWith(".apk", ignoreCase = true) &&
                    !asset.name.contains("metadata", ignoreCase = true)
            }

        val mergedAssets = mergeDirectSharedAssetIfNeeded(
            strategy = strategy,
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
                    error("目标 release 中未找到可用 APK")
                }
            }
        }

        GitHubShareImportAssetPlan(
            parsedLink = parsedLink,
            resolvedReleaseTag = target.tag,
            resolvedReleaseUrl = target.releaseUrl,
            assets = finalAssets,
            preferredAssetName = target.preferredAssetName
        )
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
                check(tag.isNotBlank()) { "该仓库暂无可用 release" }
                ResolvedReleaseTarget(
                    tag = tag,
                    releaseUrl = releaseUrl
                )
            }

            GitHubSharedUrlType.ReleaseTag,
            GitHubSharedUrlType.ReleaseDownloadAsset -> {
                val tag = parsedLink.releaseTag.trim()
                check(tag.isNotBlank()) { "分享链接缺少 release tag" }
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
        ?: error("该仓库暂无可用 release")
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
}
