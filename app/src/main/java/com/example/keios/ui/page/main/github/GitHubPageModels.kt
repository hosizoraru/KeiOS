package com.example.keios.ui.page.main

import android.content.Context
import androidx.annotation.StringRes
import com.example.keios.R
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import kotlin.math.max

internal data class VersionCheckUi(
    val loading: Boolean = false,
    val localVersion: String = "",
    val localVersionCode: Long = -1L,
    val latestTag: String = "",
    val latestStableName: String = "",
    val latestStableRawTag: String = "",
    val latestStableUrl: String = "",
    val latestPreName: String = "",
    val latestPreRawTag: String = "",
    val latestPreUrl: String = "",
    val hasStableRelease: Boolean = true,
    val hasUpdate: Boolean? = null,
    val message: String = "",
    val isPreRelease: Boolean = false,
    val preReleaseInfo: String = "",
    val showPreReleaseInfo: Boolean = false,
    val hasPreReleaseUpdate: Boolean = false,
    val recommendsPreRelease: Boolean = false,
    val releaseHint: String = "",
    val failed: Boolean = false,
    val sourceStrategyId: String = ""
)

internal data class GitHubStrategyGuide(
    val option: GitHubLookupStrategyOption,
    val summary: String,
    val pros: List<String>,
    val cons: List<String>,
    val requirement: String
)

internal data class GitHubTokenGuideField(
    val label: String,
    val value: String,
    val emphasized: Boolean = false
)

internal data class GitHubRecommendedTokenGuide(
    val collapsedSummary: String,
    val summary: String,
    val fields: List<GitHubTokenGuideField>,
    val notes: List<String>
)

internal enum class GitHubSortMode(
    @StringRes val labelRes: Int
) {
    UpdateFirst(R.string.github_sort_update_first),
    NameAsc(R.string.github_sort_name_asc),
    PreReleaseFirst(R.string.github_sort_prerelease_first)
}

internal enum class OverviewRefreshState {
    Idle,
    Cached,
    Refreshing,
    Completed
}

internal enum class RefreshIntervalOption(
    val hours: Int,
    @StringRes val labelRes: Int
) {
    Hour1(1, R.string.github_refresh_interval_1h),
    Hour3(3, R.string.github_refresh_interval_3h),
    Hour6(6, R.string.github_refresh_interval_6h),
    Hour12(12, R.string.github_refresh_interval_12h);

    companion object {
        fun fromHours(hours: Int): RefreshIntervalOption {
            return entries.firstOrNull { it.hours == hours } ?: Hour3
        }
    }
}

internal fun formatRefreshAgo(
    context: Context,
    lastRefreshMs: Long,
    nowMs: Long = System.currentTimeMillis()
): String {
    if (lastRefreshMs <= 0L) return context.getString(R.string.github_refresh_ago_not_refreshed)
    val deltaMs = max(0L, nowMs - lastRefreshMs)
    val minutes = deltaMs / 60_000L
    if (minutes <= 0L) return context.getString(R.string.common_just_now)
    if (minutes < 60L) return "${minutes}m"
    val hours = minutes / 60L
    val mins = minutes % 60L
    val days = hours / 24L
    val remainHours = hours % 24L
    return when {
        days > 0L && mins == 0L -> "${days}d ${remainHours}h"
        days > 0L -> "${days}d ${remainHours}h ${mins}m"
        mins == 0L -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}

internal fun formatFutureEta(
    context: Context,
    targetMs: Long?,
    nowMs: Long = System.currentTimeMillis()
): String {
    if (targetMs == null || targetMs <= 0L) return context.getString(R.string.common_unknown)
    val deltaMs = (targetMs - nowMs).coerceAtLeast(0L)
    val minutes = deltaMs / 60_000L
    if (minutes <= 0L) return context.getString(R.string.github_eta_soon_recover)
    if (minutes < 60L) return context.getString(R.string.github_eta_after_minutes, minutes)
    val hours = minutes / 60L
    val mins = minutes % 60L
    return if (mins == 0L) {
        context.getString(R.string.github_eta_after_hours, hours)
    } else {
        context.getString(R.string.github_eta_after_hours_minutes, hours, mins)
    }
}

internal fun strategyLabelForId(id: String): String {
    return GitHubLookupStrategyOption.fromStorageId(id).label
}

internal fun GitHubLookupStrategyOption.overviewLabel(context: Context): String {
    return when (this) {
        GitHubLookupStrategyOption.AtomFeed -> context.getString(R.string.github_overview_strategy_atom)
        GitHubLookupStrategyOption.GitHubApiToken -> context.getString(R.string.github_overview_strategy_api)
    }
}

internal fun GitHubLookupConfig.overviewApiLabel(context: Context): String {
    return when {
        selectedStrategy != GitHubLookupStrategyOption.GitHubApiToken ->
            context.getString(R.string.common_not_used)
        apiToken.isBlank() -> context.getString(R.string.common_guest)
        else -> apiToken.maskedApiPreview(context)
    }
}

private fun String.normalizeVersionPrefix(): String {
    return trim().removePrefix("v").removePrefix("V")
}

internal fun formatReleaseValue(
    releaseName: String,
    rawTag: String
): String {
    val name = releaseName.trim()
    val tag = rawTag.trim()
    val normalizedName = name.normalizeVersionPrefix()
    val normalizedTag = tag.normalizeVersionPrefix()
    return when {
        name.isBlank() -> normalizedTag.ifBlank { tag }
        tag.isBlank() -> normalizedName.ifBlank { name }
        normalizedName.equals(normalizedTag, ignoreCase = true) -> normalizedName.ifBlank { normalizedTag }
        name.equals(tag, ignoreCase = true) -> normalizedName.ifBlank { name }
        else -> "$name · $tag"
    }
}

internal fun VersionCheckUi.statusActionUrl(
    owner: String,
    repo: String
): String {
    return when {
        recommendsPreRelease && latestPreUrl.isNotBlank() -> latestPreUrl
        recommendsPreRelease && latestPreRawTag.isNotBlank() ->
            GitHubVersionUtils.buildReleaseTagUrl(owner, repo, latestPreRawTag)
        hasUpdate == true && latestStableUrl.isNotBlank() -> latestStableUrl
        hasUpdate == true && latestStableRawTag.isNotBlank() ->
            GitHubVersionUtils.buildReleaseTagUrl(owner, repo, latestStableRawTag)
        hasPreReleaseUpdate && latestPreUrl.isNotBlank() -> latestPreUrl
        hasPreReleaseUpdate && latestPreRawTag.isNotBlank() ->
            GitHubVersionUtils.buildReleaseTagUrl(owner, repo, latestPreRawTag)
        else -> ""
    }
}

private fun String.maskedApiPreview(context: Context): String {
    val token = trim()
    if (token.isBlank()) return context.getString(R.string.common_guest)

    return when {
        token.startsWith("github_pat_") -> "FG ${token.fineGrainedMarker()}"
        token.startsWith("ghp_") -> "CL ${token.compactMarker(prefix = "ghp_")}"
        token.startsWith("gho_") -> "OA ${token.compactMarker(prefix = "gho_")}"
        token.startsWith("ghu_") -> "US ${token.compactMarker(prefix = "ghu_")}"
        token.startsWith("ghs_") -> "SV ${token.compactMarker(prefix = "ghs_")}"
        token.startsWith("ghr_") -> "RF ${token.compactMarker(prefix = "ghr_")}"
        else -> "KEY ${token.compactMarker()}"
    }
}

private fun String.fineGrainedMarker(): String {
    val payload = removePrefix("github_pat_")
    val segmentA = payload.substringBefore('_', "").tokenFingerprintSource()
    val segmentB = payload.substringAfterLast('_', "").tokenFingerprintSource()
    return buildCompactMarker(
        headSource = segmentA.ifBlank { payload.tokenFingerprintSource() },
        tailSource = segmentB.ifBlank { payload.tokenFingerprintSource() }
    )
}

private fun String.compactMarker(prefix: String = ""): String {
    return buildCompactMarker(
        headSource = removePrefix(prefix).tokenFingerprintSource(),
        tailSource = removePrefix(prefix).tokenFingerprintSource()
    )
}

private fun buildCompactMarker(
    headSource: String,
    tailSource: String
): String {
    val head = headSource.take(2)
    val tail = tailSource.takeLast(2)
    return when {
        head.isBlank() && tail.isBlank() -> "--"
        head.isBlank() -> tail
        tail.isBlank() -> head
        head == tail -> head
        else -> "$head…$tail"
    }
}

private fun String.tokenFingerprintSource(): String {
    return filter { it.isLetterOrDigit() }
}

internal const val githubFineGrainedPatDocsUrl =
    "https://docs.github.com/en/enterprise-cloud@latest/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens?apiVersion=2022-11-28"

internal fun buildGitHubFineGrainedTokenTemplateUrl(): String {
    return "https://github.com/settings/personal-access-tokens/new" +
        "?name=KeiOS%20Release%20Read" +
        "&description=Read-only%20release%20check%20token%20for%20KeiOS" +
        "&expires_in=90" +
        "&contents=read"
}

internal fun githubRecommendedTokenGuide(context: Context): GitHubRecommendedTokenGuide {
    return GitHubRecommendedTokenGuide(
        collapsedSummary = context.getString(R.string.github_token_guide_collapsed),
        summary = context.getString(R.string.github_token_guide_summary),
        fields = listOf(
            GitHubTokenGuideField(
                label = context.getString(R.string.github_token_guide_field_type),
                value = context.getString(R.string.github_token_guide_field_type_value),
                emphasized = true
            ),
            GitHubTokenGuideField(
                label = context.getString(R.string.github_token_guide_field_owner),
                value = context.getString(R.string.github_token_guide_field_owner_value)
            ),
            GitHubTokenGuideField(
                label = context.getString(R.string.github_token_guide_field_repo),
                value = context.getString(R.string.github_token_guide_field_repo_value)
            ),
            GitHubTokenGuideField(
                label = context.getString(R.string.github_token_guide_field_limit),
                value = context.getString(R.string.github_token_guide_field_limit_value)
            ),
            GitHubTokenGuideField(
                label = context.getString(R.string.github_token_guide_field_permission),
                value = context.getString(R.string.github_token_guide_field_permission_value),
                emphasized = true
            ),
            GitHubTokenGuideField(
                label = context.getString(R.string.github_token_guide_field_expire),
                value = context.getString(R.string.github_token_guide_field_expire_value)
            )
        ),
        notes = listOf(
            context.getString(R.string.github_token_guide_note_1),
            context.getString(R.string.github_token_guide_note_2),
            context.getString(R.string.github_token_guide_note_3),
            context.getString(R.string.github_token_guide_note_4)
        )
    )
}

internal fun githubStrategyGuides(context: Context): List<GitHubStrategyGuide> = listOf(
    GitHubStrategyGuide(
        option = GitHubLookupStrategyOption.AtomFeed,
        summary = context.getString(R.string.github_strategy_guide_atom_summary),
        pros = listOf(
            context.getString(R.string.github_strategy_guide_atom_pro_1),
            context.getString(R.string.github_strategy_guide_atom_pro_2)
        ),
        cons = listOf(
            context.getString(R.string.github_strategy_guide_atom_con_1),
            context.getString(R.string.github_strategy_guide_atom_con_2)
        ),
        requirement = context.getString(R.string.github_strategy_guide_atom_requirement)
    ),
    GitHubStrategyGuide(
        option = GitHubLookupStrategyOption.GitHubApiToken,
        summary = context.getString(R.string.github_strategy_guide_api_summary),
        pros = listOf(
            context.getString(R.string.github_strategy_guide_api_pro_1),
            context.getString(R.string.github_strategy_guide_api_pro_2)
        ),
        cons = listOf(
            context.getString(R.string.github_strategy_guide_api_con_1),
            context.getString(R.string.github_strategy_guide_api_con_2)
        ),
        requirement = context.getString(R.string.github_strategy_guide_api_requirement)
    )
)
