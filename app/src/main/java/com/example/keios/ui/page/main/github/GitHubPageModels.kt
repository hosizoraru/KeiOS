package com.example.keios.ui.page.main

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

internal enum class GitHubSortMode(val label: String) {
    UpdateFirst("更新优先"),
    NameAsc("名称 A-Z"),
    PreReleaseFirst("预发行优先")
}

internal enum class OverviewRefreshState {
    Idle,
    Cached,
    Refreshing,
    Completed
}

internal enum class RefreshIntervalOption(val hours: Int, val label: String) {
    Hour1(1, "1 小时"),
    Hour3(3, "3 小时"),
    Hour6(6, "6 小时"),
    Hour12(12, "12 小时");

    companion object {
        fun fromHours(hours: Int): RefreshIntervalOption {
            return entries.firstOrNull { it.hours == hours } ?: Hour3
        }
    }
}

internal fun formatRefreshAgo(lastRefreshMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (lastRefreshMs <= 0L) return "未刷新"
    val deltaMs = max(0L, nowMs - lastRefreshMs)
    val minutes = deltaMs / 60_000L
    if (minutes <= 0L) return "刚刚"
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

internal fun formatFutureEta(targetMs: Long?, nowMs: Long = System.currentTimeMillis()): String {
    if (targetMs == null || targetMs <= 0L) return "未知"
    val deltaMs = (targetMs - nowMs).coerceAtLeast(0L)
    val minutes = deltaMs / 60_000L
    if (minutes <= 0L) return "即将恢复"
    if (minutes < 60L) return "$minutes 分钟后"
    val hours = minutes / 60L
    val mins = minutes % 60L
    return if (mins == 0L) "$hours 小时后" else "$hours 小时 $mins 分钟后"
}

internal fun strategyLabelForId(id: String): String {
    return GitHubLookupStrategyOption.fromStorageId(id).label
}

internal fun GitHubLookupStrategyOption.overviewLabel(): String {
    return when (this) {
        GitHubLookupStrategyOption.AtomFeed -> "Atom"
        GitHubLookupStrategyOption.GitHubApiToken -> "API"
    }
}

internal fun GitHubLookupConfig.overviewApiLabel(): String {
    return when {
        selectedStrategy != GitHubLookupStrategyOption.GitHubApiToken -> "未使用"
        apiToken.isBlank() -> "游客"
        else -> apiToken.maskedApiPreview()
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

private fun String.maskedApiPreview(): String {
    val token = trim()
    if (token.isBlank()) return "游客"

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

internal val githubRecommendedTokenGuide = GitHubRecommendedTokenGuide(
    collapsedSummary = "Fine-grained PAT · Contents: Read",
    summary = "建议为 KeiOS 单独建一个 Fine-grained PAT。当前这套 Releases API 与资源下载只需 `Contents: Read`；公开仓库也能正常追踪。",
    fields = listOf(
        GitHubTokenGuideField(
            label = "类型",
            value = "Fine-grained PAT",
            emphasized = true
        ),
        GitHubTokenGuideField(
            label = "Owner",
            value = "单用户 / 单组织"
        ),
        GitHubTokenGuideField(
            label = "仓库",
            value = "Only select repositories"
        ),
        GitHubTokenGuideField(
            label = "上限",
            value = "Selected repos 最多 50"
        ),
        GitHubTokenGuideField(
            label = "权限",
            value = "Contents: Read",
            emphasized = true
        ),
        GitHubTokenGuideField(
            label = "过期",
            value = "90 天"
        )
    ),
    notes = listOf(
        "`Only select repositories` 只限制当前 owner 下额外授权的私有仓库，公开仓库不受影响。",
        "`Selected repositories` 最多 50 个，只统计当前 owner 下手动勾选的仓库。",
        "若组织要求审批或启用 SSO，token 可能要先完成批准或 SSO 后才可用。",
        "Classic token 仅建议兜底使用，权限面通常更大。"
    )
)

internal val githubStrategyGuides: List<GitHubStrategyGuide> = listOf(
    GitHubStrategyGuide(
        option = GitHubLookupStrategyOption.AtomFeed,
        summary = "走 Atom feed 与 release 页面，轻量但更依赖网页结构。",
        pros = listOf(
            "无需 Token，适合公开仓库",
            "配置最少，上手最快"
        ),
        cons = listOf(
            "更依赖页面结构，变动时更脆弱",
            "私有仓库与细粒度元数据支持较弱"
        ),
        requirement = "无需额外凭证"
    ),
    GitHubStrategyGuide(
        option = GitHubLookupStrategyOption.GitHubApiToken,
        summary = "走 Releases API，结构更稳，也更适合资源读取与私有仓库。",
        pros = listOf(
            "元数据更稳定，可直接识别 prerelease",
            "可解析更多 release 细节，资源下载更稳"
        ),
        cons = listOf(
            "游客额度很低，项目多时更易限流",
            "token 失效或权限不足时会访问受限"
        ),
        requirement = "token 选填；留空时自动走游客 API"
    )
)
