package os.kei.ui.page.main.github.install

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File
import os.kei.feature.github.data.local.GitHubInstalledAppRepository
import os.kei.feature.github.model.GitHubInstalledPackageInfo

/** What is installed under [packageName] right now, or null when nothing is. */
internal fun loadInstalledPackageInfo(
    context: Context,
    packageName: String,
): GitHubInstalledPackageInfo? {
    val normalizedPackageName = packageName.trim()
    if (normalizedPackageName.isBlank()) return null
    val packageInfo =
        runCatching {
            GitHubInstalledAppRepository.installedPackageInfoOrNull(context, normalizedPackageName)
        }.getOrNull() ?: return null
    val applicationInfo = packageInfo.applicationInfo
    return GitHubInstalledPackageInfo(
        packageName = normalizedPackageName,
        appLabel = applicationInfo?.loadLabel(context.packageManager)?.toString().orEmpty(),
        versionName = packageInfo.versionName?.trim().orEmpty(),
        versionCode = packageInfo.longVersionCode,
        minSdk = applicationInfo?.minSdkVersion ?: -1,
        targetSdk = applicationInfo?.targetSdkVersion ?: -1,
        apkSizeBytes = applicationInfo.installedApkSizeBytes(),
    )
}

internal fun ApplicationInfo?.installedApkSizeBytes(): Long {
    this ?: return -1L
    val paths = buildList {
        sourceDir?.takeIf { it.isNotBlank() }?.let(::add)
        splitSourceDirs?.forEach { path ->
            path.takeIf { it.isNotBlank() }?.let(::add)
        }
    }.distinct()
    val total = paths.sumOf { path ->
        File(path).takeIf { it.isFile }?.length() ?: 0L
    }
    return total.takeIf { it > 0L } ?: -1L
}
