package os.kei.ui.page.main.github.page.action

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.net.toUri
import os.kei.R
import os.kei.core.intent.SafeExternalIntents
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.asset.apkAssetTarget
import os.kei.ui.page.main.github.statusActionUrl
import kotlinx.coroutines.launch

internal class GitHubAssetActions(
    private val env: GitHubPageActionEnvironment
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state
    private val repository get() = env.repository
    private val systemDmOption get() = env.systemDmOption

    fun openExternalUrl(
        url: String,
        failureMessage: String = env.openLinkFailureMessage
    ) {
        if (!SafeExternalIntents.startBrowsableUrl(context, url)) {
            env.toast(failureMessage)
        }
    }

    fun shareApkLink(asset: GitHubReleaseAssetFile) {
        scope.launch {
            shareApkLinkInternal(asset)
        }
    }

    fun openApkInDownloader(asset: GitHubReleaseAssetFile) {
        scope.launch {
            openApkInDownloaderInternal(asset)
        }
    }

    suspend fun sendAssetToConfiguredChannel(asset: GitHubReleaseAssetFile): Boolean {
        return if (state.lookupConfig.onlineShareTargetPackage.isNotBlank()) {
            shareApkLinkInternal(asset)
        } else {
            openApkInDownloaderInternal(asset)
        }
    }

    fun clearApkAssetUiState(itemId: String) {
        state.clearAssetUiState(itemId)
    }

    fun clearApkAssetRuntimeState(itemId: String) {
        state.clearAssetRuntimeState(itemId)
    }

    fun clearApkAssetCache(item: GitHubTrackedApp, itemState: VersionCheckUi) {
        val target = itemState.apkAssetTarget(
            owner = item.owner,
            repo = item.repo,
            context = context,
            alwaysLatestRelease = item.alwaysShowLatestReleaseDownloadButton
        ) ?: return
        val preferHtml = state.lookupConfig.selectedStrategy == GitHubLookupStrategyOption.AtomFeed
        val hasApiToken = state.lookupConfig.apiToken.isNotBlank()
        val releaseUrl = target.releaseUrl
        val normalizedRawTag = target.rawTag
        val cacheKeyDefault = repository.buildAssetCacheKey(
            owner = item.owner,
            repo = item.repo,
            rawTag = normalizedRawTag,
            releaseUrl = releaseUrl,
            preferHtml = preferHtml,
            aggressiveFiltering = state.lookupConfig.aggressiveApkFiltering,
            includeAllAssets = false,
            hasApiToken = hasApiToken
        )
        val cacheKeyAllAssets = repository.buildAssetCacheKey(
            owner = item.owner,
            repo = item.repo,
            rawTag = normalizedRawTag,
            releaseUrl = releaseUrl,
            preferHtml = preferHtml,
            aggressiveFiltering = state.lookupConfig.aggressiveApkFiltering,
            includeAllAssets = true,
            hasApiToken = hasApiToken
        )
        scope.launch {
            repository.clearAssetCaches(
                listOf(cacheKeyDefault, cacheKeyAllAssets)
            )
        }
    }

    fun loadApkAssets(
        item: GitHubTrackedApp,
        itemState: VersionCheckUi,
        toggleOnlyWhenCached: Boolean = true,
        includeAllAssets: Boolean = false
    ) {
        val alwaysLatestRelease = item.alwaysShowLatestReleaseDownloadButton
        val target = itemState.apkAssetTarget(
            owner = item.owner,
            repo = item.repo,
            context = context,
            alwaysLatestRelease = alwaysLatestRelease
        )
        if (target == null) {
            val fallbackUrl = if (alwaysLatestRelease) {
                repository.buildReleaseUrl(item.owner, item.repo)
            } else {
                itemState.statusActionUrl(item.owner, item.repo)
            }
            if (fallbackUrl.isNotBlank()) {
                openExternalUrl(fallbackUrl)
            } else {
                env.toast(R.string.github_toast_no_apk_to_load)
            }
            return
        }

        state.apkAssetIncludeAll[item.id] = includeAllAssets

        val cachedBundle = state.apkAssetBundles[item.id]
        if (
            toggleOnlyWhenCached &&
            cachedBundle != null &&
            state.matchesAssetSourceSignature(cachedBundle) &&
            cachedBundle.tagName.equals(target.rawTag, ignoreCase = true) &&
            cachedBundle.showingAllAssets == includeAllAssets
        ) {
            state.apkAssetExpanded[item.id] = !(state.apkAssetExpanded[item.id] ?: false)
            state.apkAssetErrors.remove(item.id)
            return
        }

        state.apkAssetExpanded[item.id] = true
        state.apkAssetLoading[item.id] = true
        state.apkAssetErrors.remove(item.id)
        scope.launch {
            val preferHtml = state.lookupConfig.selectedStrategy == GitHubLookupStrategyOption.AtomFeed
            val refreshIntervalHours = repository.loadRefreshIntervalHours()
            val assetCacheKey = repository.buildAssetCacheKey(
                owner = item.owner,
                repo = item.repo,
                rawTag = target.rawTag,
                releaseUrl = target.releaseUrl,
                preferHtml = preferHtml,
                aggressiveFiltering = state.lookupConfig.aggressiveApkFiltering,
                includeAllAssets = includeAllAssets,
                hasApiToken = state.lookupConfig.apiToken.isNotBlank()
            )
            val persistedBundle = repository.loadAssetBundle(
                cacheKey = assetCacheKey,
                refreshIntervalHours = refreshIntervalHours
            )
            if (persistedBundle != null && state.matchesAssetSourceSignature(persistedBundle)) {
                state.apkAssetLoading[item.id] = false
                state.apkAssetBundles[item.id] = persistedBundle
                state.apkAssetErrors[item.id] = buildEmptyAssetMessage(
                    label = target.label,
                    includeAllAssets = includeAllAssets,
                    assetCount = persistedBundle.assets.size
                )
                return@launch
            } else if (persistedBundle != null) {
                repository.clearAssetCache(assetCacheKey)
            }
            val result = repository.fetchApkAssets(
                owner = item.owner,
                repo = item.repo,
                rawTag = target.rawTag,
                releaseUrl = target.releaseUrl,
                preferHtml = preferHtml,
                aggressiveFiltering = state.lookupConfig.aggressiveApkFiltering,
                includeAllAssets = includeAllAssets,
                apiToken = state.lookupConfig.apiToken
            )
            state.apkAssetLoading[item.id] = false
            result.onSuccess { bundle ->
                val persistedBundle = bundle.copy(
                    sourceConfigSignature = state.buildAssetSourceSignature()
                )
                state.apkAssetBundles[item.id] = persistedBundle
                scope.launch {
                    repository.saveAssetBundle(
                        cacheKey = assetCacheKey,
                        bundle = persistedBundle
                    )
                }
                state.apkAssetErrors[item.id] = buildEmptyAssetMessage(
                    label = target.label,
                    includeAllAssets = includeAllAssets,
                    assetCount = persistedBundle.assets.size
                )
            }.onFailure { error ->
                state.apkAssetErrors[item.id] = error.message
                    ?: context.getString(R.string.github_error_load_apk_assets_failed)
            }
        }
    }

    private suspend fun resolvePreferredAssetUrl(asset: GitHubReleaseAssetFile): String {
        val token = state.lookupConfig.apiToken.trim()
        val preferApiAsset =
            state.lookupConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken
        return repository.resolvePreferredDownloadUrl(
            asset = asset,
            useApiAssetUrl = preferApiAsset,
            apiToken = token
        )
    }

    private suspend fun shareApkLinkInternal(asset: GitHubReleaseAssetFile): Boolean {
        val resolvedUrl = SafeExternalIntents.httpsExternalUrlOrNull(resolvePreferredAssetUrl(asset))
            ?: run {
                env.toast(R.string.github_toast_share_link_failed)
                return false
            }
        val onlineSharePackage = state.lookupConfig.onlineShareTargetPackage.trim()
        val intent = SafeExternalIntents.textShareIntent(
            text = resolvedUrl,
            subject = asset.name,
            targetPackage = onlineSharePackage,
            extras = if (onlineSharePackage.isNotBlank()) {
                mapOf(
                    "channel" to "Online",
                    "extra_channel" to "Online",
                    "online_channel" to true
                )
            } else {
                emptyMap()
            }
        )
        return runCatching {
            if (onlineSharePackage.isNotBlank()) {
                context.startActivity(intent)
            } else {
                context.startActivity(
                    Intent.createChooser(intent, context.getString(R.string.github_share_apk_link_title))
                )
            }
            true
        }.getOrElse {
            env.toast(R.string.github_toast_share_link_failed)
            false
        }
    }

    private suspend fun openApkInDownloaderInternal(asset: GitHubReleaseAssetFile): Boolean {
        val resolvedUrl = SafeExternalIntents.httpsExternalUrlOrNull(resolvePreferredAssetUrl(asset))
            ?: run {
                env.toast(R.string.github_toast_open_downloader_failed)
                return false
            }
        val preferredPackage = state.lookupConfig.preferredDownloaderPackage.trim()
        return runCatching {
            when (preferredPackage) {
                systemDmOption.packageName -> {
                    enqueueWithSystemDownloadManager(resolvedUrl, asset.name)
                    env.toast(R.string.github_toast_downloader_system_builtin)
                }
                "" -> {
                    require(SafeExternalIntents.startBrowsableUrl(context, resolvedUrl))
                    env.toast(R.string.github_toast_downloader_system_default)
                }
                else -> {
                    require(SafeExternalIntents.startBrowsableUrl(context, resolvedUrl, preferredPackage))
                    env.toast(R.string.github_toast_downloader_selected)
                }
            }
            true
        }.recoverCatching {
            if (preferredPackage.isNotBlank() && preferredPackage != systemDmOption.packageName) {
                require(SafeExternalIntents.startBrowsableUrl(context, resolvedUrl))
                env.toast(R.string.github_toast_downloader_fallback_system)
                true
            } else {
                throw it
            }
        }.getOrElse {
            env.toast(R.string.github_toast_open_downloader_failed)
            false
        }
    }

    private fun enqueueWithSystemDownloadManager(url: String, fileName: String) {
        val safeUrl = SafeExternalIntents.httpsExternalUrlOrNull(url)
            ?: throw IllegalArgumentException("download url must be https")
        val request = DownloadManager.Request(safeUrl.toUri()).apply {
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
            setTitle(fileName)
            setDescription(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: throw IllegalStateException("download manager unavailable")
        manager.enqueue(request)
    }

    private fun buildEmptyAssetMessage(
        label: String,
        includeAllAssets: Boolean,
        assetCount: Int
    ): String {
        if (assetCount > 0) return ""
        return if (includeAllAssets) {
            context.getString(
                R.string.github_msg_assets_no_downloadable_except_source,
                label
            )
        } else {
            context.getString(
                R.string.github_msg_assets_no_downloadable,
                label
            )
        }
    }
}
