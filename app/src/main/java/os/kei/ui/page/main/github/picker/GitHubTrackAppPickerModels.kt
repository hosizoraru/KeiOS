package os.kei.ui.page.main.github.picker

import os.kei.R
import os.kei.feature.github.model.InstalledAppItem
import java.util.Locale

internal class GitHubTrackAppPickerInput(
    val appList: List<InstalledAppItem>,
    val query: String,
    val includeUserApps: Boolean,
    val includeSystemApps: Boolean,
    val includeTrackedApps: Boolean,
    val trackedPackageNames: Set<String>,
    val pinnedPackageNames: Set<String>,
    val sortMode: GitHubTrackAppPickerSortMode,
    val sortDirection: GitHubTrackAppPickerSortDirection,
    val includeDisabledOrFrozenApps: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is GitHubTrackAppPickerInput &&
            appList === other.appList &&
            query == other.query &&
            includeUserApps == other.includeUserApps &&
            includeSystemApps == other.includeSystemApps &&
            includeTrackedApps == other.includeTrackedApps &&
            includeDisabledOrFrozenApps == other.includeDisabledOrFrozenApps &&
            trackedPackageNames == other.trackedPackageNames &&
            pinnedPackageNames == other.pinnedPackageNames &&
            sortMode == other.sortMode &&
            sortDirection == other.sortDirection
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(appList)
        result = 31 * result + query.hashCode()
        result = 31 * result + includeUserApps.hashCode()
        result = 31 * result + includeSystemApps.hashCode()
        result = 31 * result + includeTrackedApps.hashCode()
        result = 31 * result + includeDisabledOrFrozenApps.hashCode()
        result = 31 * result + trackedPackageNames.hashCode()
        result = 31 * result + pinnedPackageNames.hashCode()
        result = 31 * result + sortMode.hashCode()
        result = 31 * result + sortDirection.hashCode()
        return result
    }
}

internal data class GitHubTrackAppPickerDerivedState(
    val filteredApps: List<InstalledAppItem> = emptyList(),
    val filteredIconPreloadPackages: List<String> = emptyList(),
    val deriving: Boolean = false,
    val input: GitHubTrackAppPickerInput? = null,
) {
    companion object {
        val Empty = GitHubTrackAppPickerDerivedState()
    }
}

internal enum class GitHubTrackAppPickerSortMode(
    val labelRes: Int,
    val storageId: String,
) {
    Name(R.string.github_track_sheet_app_sort_name, "name"),
    LastUpdated(R.string.github_track_sheet_app_sort_last_updated, "last_updated"),
    RecentlyInstalled(
        R.string.github_track_sheet_app_sort_recently_installed,
        "recently_installed",
    ),
    InstallSource(R.string.github_track_sheet_app_sort_install_source, "install_source"),
    Package(R.string.github_track_sheet_app_sort_package, "package"),
    ;

    companion object {
        fun fromStorageId(storageId: String): GitHubTrackAppPickerSortMode = entries.firstOrNull { it.storageId == storageId } ?: Name
    }
}

internal enum class GitHubTrackAppPickerSortDirection(
    val labelRes: Int,
    val storageId: String,
) {
    Ascending(R.string.github_track_sheet_app_sort_direction_ascending, "ascending"),
    Descending(R.string.github_track_sheet_app_sort_direction_descending, "descending"),
    ;

    companion object {
        fun fromStorageId(storageId: String): GitHubTrackAppPickerSortDirection =
            entries.firstOrNull { it.storageId == storageId } ?: Ascending
    }
}

internal fun GitHubTrackAppPickerSortMode.isTimeSort(): Boolean =
    this == GitHubTrackAppPickerSortMode.LastUpdated ||
        this == GitHubTrackAppPickerSortMode.RecentlyInstalled

internal fun GitHubTrackAppPickerSortMode.showsInstallSourcePill(): Boolean = this == GitHubTrackAppPickerSortMode.InstallSource

internal fun gitHubTrackAppCandidateInitialScrollIndex(
    candidates: List<InstalledAppItem>,
    selectedPackageName: String?,
): Int {
    val normalizedSelectedPackage =
        selectedPackageName
            ?.normalizedGitHubTrackAppPackageNameOrNull()
            ?: return 0
    val selectedIndex =
        candidates.indexOfFirst { candidate ->
            candidate.packageName.normalizedGitHubTrackAppPackageNameOrNull() == normalizedSelectedPackage
        }
    return when {
        selectedIndex <= 0 -> 0
        else -> selectedIndex - 1
    }
}

internal fun filterAndSortGitHubTrackAppCandidates(
    apps: List<InstalledAppItem>,
    query: String,
    includeUserApps: Boolean,
    includeSystemApps: Boolean,
    includeTrackedApps: Boolean,
    trackedPackageNames: Set<String>,
    pinnedPackageNames: Set<String>,
    sortMode: GitHubTrackAppPickerSortMode,
    sortDirection: GitHubTrackAppPickerSortDirection,
    includeDisabledOrFrozenApps: Boolean = false,
): List<InstalledAppItem> {
    val normalizedTrackedPackages =
        trackedPackageNames
            .mapNotNullTo(LinkedHashSet()) { it.normalizedGitHubTrackAppPackageNameOrNull() }
    val normalizedPinnedPackages =
        pinnedPackageNames
            .mapNotNullTo(LinkedHashSet()) { it.normalizedGitHubTrackAppPackageNameOrNull() }
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    return apps
        .asSequence()
        .map(::GitHubTrackAppCandidateProjection)
        .filter { projection -> projection.matchesAppPickerQuery(normalizedQuery) }
        .filter { projection ->
            projection.matchesAppPickerScope(
                includeUserApps = includeUserApps,
                includeSystemApps = includeSystemApps,
                includeTrackedApps = includeTrackedApps,
                includeDisabledOrFrozenApps = includeDisabledOrFrozenApps,
                trackedPackageNames = normalizedTrackedPackages,
                pinnedPackageNames = normalizedPinnedPackages,
            )
        }.sortedWith(sortMode.comparator(sortDirection))
        .map { projection -> projection.app }
        .toList()
}

internal fun gitHubTrackAppPickerIconPreloadPackages(
    apps: List<InstalledAppItem>,
): List<String> =
    apps
        .asSequence()
        .map { app -> app.packageName.trim() }
        .filter { packageName -> packageName.isNotBlank() }
        .distinct()
        .toList()

private const val GITHUB_TRACK_APP_PICKER_ICON_PREFETCH_DISTANCE = 8

internal fun gitHubTrackAppPickerViewportIconPackages(
    apps: List<InstalledAppItem>,
    visibleItemIndices: List<Int>,
): List<String> {
    if (apps.isEmpty()) return emptyList()
    val validVisibleIndices = visibleItemIndices.filter { index -> index in apps.indices }
    val firstVisibleIndex = validVisibleIndices.minOrNull() ?: return emptyList()
    val lastVisibleIndex = validVisibleIndices.maxOrNull() ?: return emptyList()
    val preloadStart =
        (firstVisibleIndex - GITHUB_TRACK_APP_PICKER_ICON_PREFETCH_DISTANCE)
            .coerceAtLeast(0)
    val preloadEndExclusive =
        (lastVisibleIndex + GITHUB_TRACK_APP_PICKER_ICON_PREFETCH_DISTANCE + 1)
            .coerceAtMost(apps.size)
    return gitHubTrackAppPickerIconPreloadPackages(
        apps = apps.subList(preloadStart, preloadEndExclusive),
    )
}

internal fun String.normalizedGitHubTrackAppPackageNameOrNull(): String? {
    val normalized = trim().lowercase(Locale.ROOT)
    return normalized.ifBlank { null }
}

private data class GitHubTrackAppCandidateProjection(
    val app: InstalledAppItem,
) {
    val labelKey: String = app.label.lowercase(Locale.ROOT)
    val packageNameKey: String = app.packageName.lowercase(Locale.ROOT)
    val installSourcePackageNameKey: String = app.installSourcePackageName.lowercase(Locale.ROOT)
    val installSourceKey: String =
        app.installSourceLabel
            .ifBlank { app.installSourcePackageName }
            .lowercase(Locale.ROOT)
    val normalizedPackageName: String? = app.packageName.normalizedGitHubTrackAppPackageNameOrNull()
}

private fun GitHubTrackAppCandidateProjection.matchesAppPickerQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return labelKey.contains(query) ||
        packageNameKey.contains(query) ||
        installSourceKey.contains(query) ||
        installSourcePackageNameKey.contains(query)
}

private fun GitHubTrackAppCandidateProjection.matchesAppPickerScope(
    includeUserApps: Boolean,
    includeSystemApps: Boolean,
    includeTrackedApps: Boolean,
    includeDisabledOrFrozenApps: Boolean,
    trackedPackageNames: Set<String>,
    pinnedPackageNames: Set<String>,
): Boolean {
    if (normalizedPackageName != null && normalizedPackageName in pinnedPackageNames) {
        return true
    }
    if (app.isDisabledOrFrozen && !includeDisabledOrFrozenApps) return false
    val matchesType =
        when {
            app.isSystemApp -> includeSystemApps
            else -> includeUserApps
        }
    if (!matchesType) return false
    val alreadyTracked =
        normalizedPackageName != null &&
            normalizedPackageName in trackedPackageNames
    return includeTrackedApps || !alreadyTracked
}

private fun GitHubTrackAppPickerSortMode.comparator(
    direction: GitHubTrackAppPickerSortDirection,
): Comparator<GitHubTrackAppCandidateProjection> {
    val baseComparator =
        when (this) {
            GitHubTrackAppPickerSortMode.Name -> {
                compareBy<GitHubTrackAppCandidateProjection> { it.labelKey }
                    .thenBy { it.packageNameKey }
            }

            GitHubTrackAppPickerSortMode.LastUpdated -> {
                compareBy<GitHubTrackAppCandidateProjection> { it.app.lastUpdateTimeMs }
                    .thenBy { it.labelKey }
            }

            GitHubTrackAppPickerSortMode.RecentlyInstalled -> {
                compareBy<GitHubTrackAppCandidateProjection> { it.app.firstInstallTimeMs }
                    .thenBy { it.labelKey }
            }

            GitHubTrackAppPickerSortMode.InstallSource -> {
                compareBy<GitHubTrackAppCandidateProjection> { it.installSourceKey.isBlank() }
                    .thenBy { it.installSourceKey }
                    .thenBy { it.labelKey }
                    .thenBy { it.packageNameKey }
            }

            GitHubTrackAppPickerSortMode.Package -> {
                compareBy<GitHubTrackAppCandidateProjection> { it.packageNameKey }
                    .thenBy { it.labelKey }
            }
        }
    return when (direction) {
        GitHubTrackAppPickerSortDirection.Ascending -> baseComparator
        GitHubTrackAppPickerSortDirection.Descending -> baseComparator.reversed()
    }
}
