package os.kei.feature.github.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import os.kei.core.privilege.PrivilegeCapability
import os.kei.core.privilege.PrivilegeMode
import os.kei.core.privilege.PrivilegedShell
import os.kei.core.system.AppCommandResult
import os.kei.core.system.HyperOsSettingsIntents
import os.kei.core.system.getInstalledPackageInfosSafely
import os.kei.feature.github.model.GitHubLocalVersionInfo
import os.kei.feature.github.model.InstalledAppItem

object GitHubInstalledAppRepository {
    private const val INSTALLED_APPS_CACHE_TTL_MS = 5L * 60L * 1000L
    private const val PRIVILEGED_PACKAGE_QUERY_TIMEOUT_MS = 5_000L
    private const val PRIVILEGED_USER_PACKAGES_MARKER = "__keios_user_packages__"
    private const val PRIVILEGED_SYSTEM_PACKAGES_MARKER = "__keios_system_packages__"

    @Volatile
    private var installedAppsCache: CachedInstalledApps? = null

    @Volatile
    private var privilegedPackageCache: CachedPrivilegedPackages? = null

    fun buildAppListPermissionIntent(context: Context): Intent? {
        return HyperOsSettingsIntents.buildAppListPermissionIntent(context)
    }

    fun queryInstalledLaunchableApps(
        context: Context,
        forceRefresh: Boolean = false,
        ttlMs: Long = INSTALLED_APPS_CACHE_TTL_MS,
        includeSystemApps: Boolean = true,
        pinnedSystemPackageNames: Set<String> = emptySet(),
        requiredPackageNames: Set<String> = emptySet(),
    ): List<InstalledAppItem> {
        val now = System.currentTimeMillis()
        val normalizedPinnedSystemPackages = pinnedSystemPackageNames.normalizedPackageNameSet()
        val normalizedRequiredPackages = requiredPackageNames.normalizedPackageNameSet()
        if (!forceRefresh) {
            installedAppsCache?.takeIf { cache ->
                (now - cache.updatedAtMs).coerceAtLeast(0L) < ttlMs.coerceAtLeast(0L) &&
                    cache.canServeScope(
                        includeSystemApps = includeSystemApps,
                        pinnedSystemPackageNames = normalizedPinnedSystemPackages,
                    ) &&
                    cache.containsPackages(normalizedRequiredPackages)
            }?.let { cache ->
                return filterByScanScope(
                    apps = cache.apps,
                    includeSystemApps = includeSystemApps,
                    pinnedSystemPackageNames = normalizedPinnedSystemPackages,
                )
            }
        }

        val packageManager = context.packageManager
        val overlayFlagMask = installedAppResourceOverlayFlagMask()
        val installSourceLabelCache = mutableMapOf<String, String>()
        val labelSortLocale = Locale.getDefault()
        val availablePackageNames = runCatching {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()),
            ).mapTo(HashSet()) { it.packageName.lowercase(Locale.ROOT) }
        }.getOrNull()
        val scannedApps = packageManager.getInstalledPackageInfosSafely(
            flags = PackageManager.PackageInfoFlags.of(gitHubInstalledAppQueryFlags),
        )
            .asSequence()
            .mapNotNull { packageInfo ->
                packageManager.toInstalledAppItem(
                    packageInfo = packageInfo,
                    overlayFlagMask = overlayFlagMask,
                    installSourceLabelCache = installSourceLabelCache,
                    availablePackageNames = availablePackageNames,
                )
            }
            .toMutableList()
        val scannedPackages =
            scannedApps.mapTo(HashSet()) { app -> app.packageName.lowercase(Locale.ROOT) }
        normalizedRequiredPackages.forEach { packageName ->
            if (packageName in scannedPackages) return@forEach
            packageManager.installedAppItemOrNull(
                packageName = packageName,
                overlayFlagMask = overlayFlagMask,
                installSourceLabelCache = installSourceLabelCache,
                availablePackageNames = availablePackageNames,
            )?.let { app ->
                scannedApps += app
                scannedPackages += packageName
            }
        }
        val apps = scannedApps
            .asSequence()
            .filter { app ->
                includeSystemApps ||
                    !app.isSystemApp ||
                    app.packageName.lowercase(Locale.ROOT) in normalizedPinnedSystemPackages
            }
            .map { item ->
                InstalledAppSortEntry(
                    item = item,
                    labelSortKey = item.label.lowercase(labelSortLocale),
                    packageSortKey = item.packageName.lowercase(Locale.ROOT),
                )
            }
            .distinctBy { entry -> entry.item.packageName.lowercase(Locale.ROOT) }
            .sortedWith(
                compareBy<InstalledAppSortEntry> { entry -> entry.labelSortKey }
                    .thenBy { entry -> entry.packageSortKey },
            )
            .map { entry -> entry.item }
            .toList()
        installedAppsCache = CachedInstalledApps(
            updatedAtMs = now,
            apps = apps,
            includesSystemApps = includeSystemApps,
            pinnedSystemPackageNames = normalizedPinnedSystemPackages,
            reconciledPackageNames = normalizedRequiredPackages,
        )
        return apps
    }

    suspend fun queryInstalledLaunchableAppsWithPrivilege(
        context: Context,
        privilegedShell: PrivilegedShell,
        forceRefresh: Boolean = false,
        ttlMs: Long = INSTALLED_APPS_CACHE_TTL_MS,
        includeSystemApps: Boolean = true,
        pinnedSystemPackageNames: Set<String> = emptySet(),
        requiredPackageNames: Set<String> = emptySet(),
    ): List<InstalledAppItem> {
        val directApps =
            queryInstalledLaunchableApps(
                context = context,
                forceRefresh = forceRefresh,
                ttlMs = ttlMs,
                includeSystemApps = includeSystemApps,
                pinnedSystemPackageNames = pinnedSystemPackageNames,
                requiredPackageNames = requiredPackageNames,
            )
        val activeMode = privilegedShell.activeMode
        if (
            !shouldQueryGitHubPrivilegedPackageInventory(
                mode = activeMode,
                supportsShellCommand = privilegedShell.supports(PrivilegeCapability.ShellCommand),
            )
        ) {
            return directApps
        }
        val inventory =
            queryPrivilegedPackageInventory(
                privilegedShell = privilegedShell,
                mode = activeMode,
                forceRefresh = forceRefresh,
                ttlMs = ttlMs,
            ) ?: return directApps
        currentCoroutineContext().ensureActive()
        if (privilegedShell.activeMode != activeMode) return directApps
        return mergePrivilegedPackageInventory(
            context = context,
            directApps = directApps,
            inventory = inventory,
            includeSystemApps = includeSystemApps,
            pinnedSystemPackageNames = pinnedSystemPackageNames,
            requiredPackageNames = requiredPackageNames,
        )
    }

    internal fun filterByScanScope(
        apps: List<InstalledAppItem>,
        includeSystemApps: Boolean,
        pinnedSystemPackageNames: Set<String>,
    ): List<InstalledAppItem> {
        if (includeSystemApps) return apps
        val normalizedPinnedSystemPackages = pinnedSystemPackageNames.normalizedPackageNameSet()
        return apps.filter { app ->
            !app.isSystemApp ||
                app.packageName.lowercase(Locale.ROOT) in normalizedPinnedSystemPackages
        }
    }

    fun invalidateCache() {
        installedAppsCache = null
        privilegedPackageCache = null
    }

    fun localVersionName(context: Context, packageName: String): String {
        return localVersionInfoOrNull(context, packageName)?.versionName.orEmpty()
            .ifBlank { "unknown" }
    }

    fun localVersionCode(context: Context, packageName: String): Long {
        return localVersionInfoOrNull(context, packageName)?.versionCode ?: -1L
    }

    fun localVersionInfoOrNull(
        context: Context,
        packageName: String,
    ): GitHubLocalVersionInfo? {
        val packageInfo = installedPackageInfoOrNull(context, packageName) ?: return null
        val appInfo = packageInfo.applicationInfo
            ?: context.packageManager.getApplicationInfoCompat(packageName) ?: return null
        return GitHubLocalVersionInfo(
            versionName = packageInfo.versionName?.trim().orEmpty().ifBlank { "unknown" },
            versionCode = packageInfo.longVersionCode,
            isSystemApp = appInfo.isSystemAppForPicker(),
        )
    }

    fun installedPackageInfoOrNull(
        context: Context,
        packageName: String,
    ): PackageInfo? {
        val packageManager = context.packageManager
        val packageInfo = runCatching {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(gitHubInstalledAppQueryFlags),
            )
        }.getOrElse { error ->
            if (error is PackageManager.NameNotFoundException) return null
            throw error
        }
        val appInfo = packageInfo.applicationInfo
            ?: packageManager.getApplicationInfoCompat(packageName) ?: return null
        val overlayFlagMask = installedAppResourceOverlayFlagMask()
        if (shouldIgnoreInstalledApp(appInfo, overlayFlagMask)) return null
        return packageInfo
    }

    private data class CachedInstalledApps(
        val updatedAtMs: Long,
        val apps: List<InstalledAppItem>,
        val includesSystemApps: Boolean,
        val pinnedSystemPackageNames: Set<String>,
        val reconciledPackageNames: Set<String>,
    ) {
        fun canServeScope(
            includeSystemApps: Boolean,
            pinnedSystemPackageNames: Set<String>,
        ): Boolean {
            if (includesSystemApps) return true
            if (includeSystemApps) return false
            return pinnedSystemPackageNames.all { packageName ->
                packageName in this.pinnedSystemPackageNames
            }
        }

        fun containsPackages(packageNames: Set<String>): Boolean {
            if (packageNames.isEmpty()) return true
            val cachedPackages = apps.mapTo(HashSet()) { app ->
                app.packageName.lowercase(Locale.ROOT)
            }
            return packageNames.all { packageName ->
                packageName in cachedPackages || packageName in reconciledPackageNames
            }
        }
    }

    private data class CachedPrivilegedPackages(
        val updatedAtMs: Long,
        val mode: PrivilegeMode,
        val inventory: GitHubPrivilegedPackageInventory,
    )

    private data class InstalledAppSortEntry(
        val item: InstalledAppItem,
        val labelSortKey: String,
        val packageSortKey: String,
    )

    private suspend fun queryPrivilegedPackageInventory(
        privilegedShell: PrivilegedShell,
        mode: PrivilegeMode,
        forceRefresh: Boolean,
        ttlMs: Long,
    ): GitHubPrivilegedPackageInventory? {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            privilegedPackageCache
                ?.takeIf { cache ->
                    cache.mode == mode &&
                        (now - cache.updatedAtMs).coerceAtLeast(0L) < ttlMs.coerceAtLeast(0L)
                }?.let { cache -> return cache.inventory }
        }
        val result =
            privilegedShell.execCommandCancellableResult(
                command = PRIVILEGED_PACKAGE_QUERY_COMMAND,
                timeoutMs = PRIVILEGED_PACKAGE_QUERY_TIMEOUT_MS,
            )
        currentCoroutineContext().ensureActive()
        if (privilegedShell.activeMode != mode) return null
        val inventory = resolveGitHubPrivilegedPackageInventory(result) ?: return null
        privilegedPackageCache =
            CachedPrivilegedPackages(
                updatedAtMs = now,
                mode = mode,
                inventory = inventory,
            )
        return inventory
    }

    private suspend fun mergePrivilegedPackageInventory(
        context: Context,
        directApps: List<InstalledAppItem>,
        inventory: GitHubPrivilegedPackageInventory,
        includeSystemApps: Boolean,
        pinnedSystemPackageNames: Set<String>,
        requiredPackageNames: Set<String>,
    ): List<InstalledAppItem> {
        val packageManager = context.packageManager
        val overlayFlagMask = installedAppResourceOverlayFlagMask()
        val installSourceLabelCache = mutableMapOf<String, String>()
        val normalizedPinnedSystemPackages = pinnedSystemPackageNames.normalizedPackageNameSet()
        val normalizedRequiredPackages = requiredPackageNames.normalizedPackageNameSet()
        val merged = directApps.toMutableList()
        val knownPackages =
            directApps.mapTo(HashSet()) { app -> app.packageName.lowercase(Locale.ROOT) }
        inventory.allPackages.forEachIndexed { index, packageName ->
            if (index % 32 == 0) currentCoroutineContext().ensureActive()
            val normalizedPackageName = packageName.lowercase(Locale.ROOT)
            if (normalizedPackageName in knownPackages) return@forEachIndexed
            val isSystemApp = inventory.isSystemPackage(normalizedPackageName)
            if (
                isSystemApp &&
                !includeSystemApps &&
                normalizedPackageName !in normalizedPinnedSystemPackages &&
                normalizedPackageName !in normalizedRequiredPackages
            ) {
                return@forEachIndexed
            }
            val item =
                packageManager.installedAppItemOrNull(
                    packageName = packageName,
                    overlayFlagMask = overlayFlagMask,
                    installSourceLabelCache = installSourceLabelCache,
                ) ?: InstalledAppItem(
                    label = packageName,
                    packageName = packageName,
                    isSystemApp = isSystemApp,
                )
            merged += item
            knownPackages += normalizedPackageName
        }
        val labelSortLocale = Locale.getDefault()
        return merged
            .asSequence()
            .filter { app ->
                includeSystemApps ||
                    !app.isSystemApp ||
                    app.packageName.lowercase(Locale.ROOT) in normalizedPinnedSystemPackages
            }.map { item ->
                InstalledAppSortEntry(
                    item = item,
                    labelSortKey = item.label.lowercase(labelSortLocale),
                    packageSortKey = item.packageName.lowercase(Locale.ROOT),
                )
            }.distinctBy { entry -> entry.packageSortKey }
            .sortedWith(
                compareBy<InstalledAppSortEntry> { entry -> entry.labelSortKey }
                    .thenBy { entry -> entry.packageSortKey },
            ).map { entry -> entry.item }
            .toList()
    }

    private fun PackageManager.installedAppItemOrNull(
        packageName: String,
        overlayFlagMask: Int,
        installSourceLabelCache: MutableMap<String, String>,
        availablePackageNames: Set<String>? = null,
    ): InstalledAppItem? {
        val packageInfo = runCatching {
            getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(gitHubInstalledAppQueryFlags),
            )
        }.getOrNull() ?: return null
        return toInstalledAppItem(
            packageInfo = packageInfo,
            overlayFlagMask = overlayFlagMask,
            installSourceLabelCache = installSourceLabelCache,
            availablePackageNames = availablePackageNames,
        )
    }

    private fun PackageManager.toInstalledAppItem(
        packageInfo: PackageInfo,
        overlayFlagMask: Int,
        installSourceLabelCache: MutableMap<String, String>,
        availablePackageNames: Set<String>? = null,
    ): InstalledAppItem? {
        val packageName = packageInfo.packageName.trim()
        if (packageName.isBlank()) return null
        val appInfo =
            packageInfo.applicationInfo ?: getApplicationInfoCompat(packageName) ?: return null
        if (shouldIgnoreInstalledApp(appInfo, overlayFlagMask)) return null
        val label = runCatching {
            getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName).trim().ifBlank { packageName }
        val installSource = resolveInstallSource(
            packageName = packageName,
            labelCache = installSourceLabelCache,
        )
        return InstalledAppItem(
            label = label,
            packageName = packageName,
            firstInstallTimeMs = packageInfo.firstInstallTime,
            lastUpdateTimeMs = packageInfo.lastUpdateTime,
            isSystemApp = appInfo.isSystemAppForPicker(),
            installSourcePackageName = installSource.packageName,
            installSourceLabel = installSource.label,
            isDisabledOrFrozen = !appInfo.enabled ||
                (appInfo.flags and ApplicationInfo.FLAG_SUSPENDED) != 0 ||
                isHiddenInstalledApp(packageName, availablePackageNames),
        )
    }

    private fun PackageManager.isHiddenInstalledApp(
        packageName: String,
        availablePackageNames: Set<String>?,
    ): Boolean {
        if (availablePackageNames != null) {
            return packageName.lowercase(Locale.ROOT) !in availablePackageNames
        }
        // A failed bulk query must not classify ordinary headless apps as hidden.
        return runCatching {
            getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()),
            )
        }.exceptionOrNull() is PackageManager.NameNotFoundException
    }

    private val PRIVILEGED_PACKAGE_QUERY_COMMAND =
        listOf(
            "set -e",
            "printf '$PRIVILEGED_USER_PACKAGES_MARKER\\n'",
            "pm list packages -3",
            "printf '$PRIVILEGED_SYSTEM_PACKAGES_MARKER\\n'",
            "pm list packages -s",
        ).joinToString(separator = "; ")
}

internal data class GitHubPrivilegedPackageInventory(
    val userPackages: Set<String>,
    val systemPackages: Set<String>,
) {
    private val normalizedSystemPackages =
        systemPackages.mapTo(HashSet(systemPackages.size)) { packageName ->
            packageName.lowercase(Locale.ROOT)
        }

    val allPackages: Set<String>
        get() = LinkedHashSet<String>(userPackages.size + systemPackages.size).apply {
            addAll(userPackages)
            addAll(systemPackages)
        }

    val isEmpty: Boolean
        get() = userPackages.isEmpty() && systemPackages.isEmpty()

    fun isSystemPackage(packageName: String): Boolean {
        val normalizedPackageName = packageName.trim().lowercase(Locale.ROOT)
        return normalizedPackageName in normalizedSystemPackages
    }
}

internal fun shouldQueryGitHubPrivilegedPackageInventory(
    mode: PrivilegeMode,
    supportsShellCommand: Boolean,
): Boolean = mode != PrivilegeMode.Disabled && supportsShellCommand

internal fun resolveGitHubPrivilegedPackageInventory(
    result: AppCommandResult,
): GitHubPrivilegedPackageInventory? {
    if (!result.succeeded || result.stdoutTruncated) return null
    return parseGitHubPrivilegedPackageInventory(result.stdout).takeUnless { inventory ->
        inventory.isEmpty
    }
}

internal fun parseGitHubPrivilegedPackageInventory(output: String): GitHubPrivilegedPackageInventory {
    val userPackages = LinkedHashSet<String>()
    val systemPackages = LinkedHashSet<String>()
    var target: MutableSet<String>? = null
    output.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        when (line) {
            "__keios_user_packages__" -> target = userPackages
            "__keios_system_packages__" -> target = systemPackages
            else -> {
                val packageName =
                    line
                        .takeIf { value -> value.startsWith("package:") }
                        ?.removePrefix("package:")
                        ?.substringBefore(' ')
                        ?.trim()
                        ?.takeIf(::isValidGitHubPackageName)
                if (packageName != null) target?.add(packageName)
            }
        }
    }
    val normalizedSystemPackages =
        systemPackages.mapTo(HashSet(systemPackages.size)) { packageName ->
            packageName.lowercase(Locale.ROOT)
        }
    userPackages.removeAll { packageName ->
        packageName.lowercase(Locale.ROOT) in normalizedSystemPackages
    }
    return GitHubPrivilegedPackageInventory(
        userPackages = userPackages,
        systemPackages = systemPackages,
    )
}

private fun isValidGitHubPackageName(value: String): Boolean =
    value.isNotBlank() && PACKAGE_NAME_PATTERN.matches(value)

private val PACKAGE_NAME_PATTERN = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*")

private fun PackageManager.getApplicationInfoCompat(packageName: String): ApplicationInfo? {
    return runCatching {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(gitHubInstalledAppQueryFlags))
    }.getOrNull()
}

private fun Set<String>.normalizedPackageNameSet(): Set<String> {
    return mapNotNullTo(LinkedHashSet()) { packageName ->
        packageName.trim().lowercase(Locale.ROOT).takeIf { it.isNotBlank() }
    }
}

private data class InstalledAppInstallSource(
    val packageName: String,
    val label: String,
)

private fun PackageManager.resolveInstallSource(
    packageName: String,
    labelCache: MutableMap<String, String>,
): InstalledAppInstallSource {
    val sourcePackageName = runCatching {
        val sourceInfo = getInstallSourceInfo(packageName)
        sourceInfo.installingPackageName
            ?: sourceInfo.initiatingPackageName
            ?: sourceInfo.originatingPackageName
    }.getOrNull()?.trim().orEmpty()
    if (sourcePackageName.isBlank()) return InstalledAppInstallSource("", "")
    val sourceLabel = labelCache.getOrPut(sourcePackageName) {
        runCatching {
            val sourceInfo = getApplicationInfoCompat(sourcePackageName)
            if (sourceInfo == null) sourcePackageName else getApplicationLabel(sourceInfo).toString()
        }.getOrDefault(sourcePackageName).trim().ifBlank { sourcePackageName }
    }
    return InstalledAppInstallSource(
        packageName = sourcePackageName,
        label = sourceLabel,
    )
}

private fun installedAppResourceOverlayFlagMask(): Int {
    return runCatching {
        ApplicationInfo::class.java.getField("FLAG_IS_RESOURCE_OVERLAY").getInt(null)
    }.getOrDefault(0)
}

private fun shouldIgnoreInstalledApp(
    appInfo: ApplicationInfo,
    overlayFlagMask: Int,
): Boolean {
    // Hidden packages require MATCH_UNINSTALLED_PACKAGES, which also returns removed/archived apps.
    if ((appInfo.flags and ApplicationInfo.FLAG_INSTALLED) == 0) return true
    if ((appInfo.flags and ApplicationInfo.FLAG_HAS_CODE) == 0) return true
    if ((appInfo.flags and ApplicationInfo.FLAG_TEST_ONLY) != 0) return true
    if (overlayFlagMask != 0 && (appInfo.flags and overlayFlagMask) != 0) return true
    return false
}

internal val gitHubInstalledAppQueryFlags: Long =
    PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong() or
        PackageManager.MATCH_DISABLED_COMPONENTS.toLong()

private fun ApplicationInfo.isSystemAppForPicker(): Boolean {
    return (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
        (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}
