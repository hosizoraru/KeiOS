package os.kei.ui.page.main.github.query

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import os.kei.R
import os.kei.feature.github.model.InstalledAppItem

internal const val installerXRevivedPackageName = "com.rosan.installer.x.revived"
internal const val packageInstallerOnlinePackageName = "io.github.vvb2060.packageinstaller"

internal data class DownloaderOption(
    val packageName: String,
    val label: String,
    val isRecommended: Boolean = false
)

internal data class OnlineShareTargetOption(
    val packageName: String,
    val label: String
)

internal fun systemDefaultDownloaderOption(context: Context): DownloaderOption = DownloaderOption(
    packageName = "",
    label = context.getString(R.string.github_downloader_system_default)
)

internal fun systemDownloadManagerOption(context: Context): DownloaderOption = DownloaderOption(
    packageName = "__system_download_manager__",
    label = context.getString(R.string.github_downloader_system_builtin)
)

internal fun noOnlineShareTargetOption(context: Context): OnlineShareTargetOption = OnlineShareTargetOption(
    packageName = "",
    label = context.getString(R.string.github_online_share_none)
)

internal fun queryOnlineShareTargetOptions(
    context: Context,
    appList: List<InstalledAppItem>
): List<OnlineShareTargetOption> {
    val knownTargets = listOf(
        OnlineShareTargetOption(
            installerXRevivedPackageName,
            context.getString(R.string.github_online_share_target_installerx)
        ),
        OnlineShareTargetOption(
            packageInstallerOnlinePackageName,
            context.getString(R.string.github_online_share_target_package_installer)
        )
    )
    return knownTargets.filter { target ->
        appList.any { it.packageName == target.packageName } || runCatching {
            context.packageManager.getPackageInfo(
                target.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
            true
        }.recoverCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(target.packageName, 0)
            true
        }.getOrDefault(false)
    }
}

internal fun queryDownloaderOptions(context: Context): List<DownloaderOption> {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/")).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    val resolved = runCatching {
        context.packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0)
        )
    }.recoverCatching {
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(intent, 0)
    }.getOrDefault(emptyList<ResolveInfo>())

    return resolved.mapNotNull { it.toDownloaderOptionOrNull(context) }
        .filterNot { it.packageName == context.packageName }
        .distinctBy { it.packageName }
        .sortedWith(
            compareByDescending<DownloaderOption> { it.isRecommended }
                .thenBy { it.label.lowercase() }
        )
}

internal fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return runCatching {
        context.packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
        true
    }.recoverCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}

private fun ResolveInfo.toDownloaderOptionOrNull(context: Context): DownloaderOption? {
    val packageName = activityInfo?.packageName?.trim().orEmpty()
    val label = loadLabel(context.packageManager).toString().trim().ifBlank { packageName }
    if (packageName.isBlank()) return null

    val normalizedPackage = packageName.lowercase()
    val normalizedLabel = label.lowercase()
    val combined = "$normalizedPackage $normalizedLabel"
    val excludedKeywords = listOf("android", "system ui", "package installer")
    val positiveKeywords = listOf(
        "download", "downloader", "downloadmanager", "manager", "loader",
        "adm", "idm", "1dm", "aria", "fetch", "torrent", "\u4e0b\u8f7d"
    )
    val knownDownloaderPackages = listOf(
        "com.dv.adm",
        "idm.internet.download.manager.plus",
        "idm.internet.download.manager",
        "idm.internet.download.manager.adm.lite",
        "com.apps2sd.adm"
    )
    if (excludedKeywords.any { combined == it || combined.contains(" $it") }) return null

    val isRecommended = positiveKeywords.any { combined.contains(it) } ||
        knownDownloaderPackages.any {
            normalizedPackage == it || normalizedPackage.startsWith("$it.")
        }
    val decoratedLabel = when {
        isRecommended -> context.getString(R.string.github_downloader_recommended_format, label)
        else -> label
    }
    return DownloaderOption(
        packageName = packageName,
        label = decoratedLabel,
        isRecommended = isRecommended
    )
}
