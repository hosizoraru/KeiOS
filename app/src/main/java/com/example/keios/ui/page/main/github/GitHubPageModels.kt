package com.example.keios.ui.page.main

import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import kotlin.math.max

internal data class VersionCheckUi(
    val loading: Boolean = false,
    val localVersion: String = "",
    val localVersionCode: Long = -1L,
    val latestTag: String = "",
    val latestStableRawTag: String = "",
    val latestStableUrl: String = "",
    val latestPreRawTag: String = "",
    val latestPreUrl: String = "",
    val hasUpdate: Boolean? = null,
    val message: String = "",
    val isPreRelease: Boolean = false,
    val preReleaseInfo: String = "",
    val showPreReleaseInfo: Boolean = false,
    val hasPreReleaseUpdate: Boolean = false,
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
    if (minutes < 60L) return "$minutes 分钟前"
    val hours = minutes / 60L
    val mins = minutes % 60L
    return if (mins == 0L) "$hours 小时前" else "$hours 小时 $mins 分钟前"
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
    summary = "推荐为 KeiOS 单独创建一个 Fine-grained PAT；读取 Releases API 的最小权限可收敛到只读仓库内容，公开仓库追踪不受选仓限制。",
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
        "`Only select repositories` 仅限制当前 owner 下额外授权的仓库；公开仓库仍可正常追踪，不会挡住别人的 public repo。",
        "`MAX 50 repositories` 指 `Selected repositories` 里手动勾选的仓库上限，只算当前 owner 下所选仓库，不是 GitHub 全站上限，也不是全部可访问仓库总数。",
        "若资源 owner 选组织且组织要求审批，token 可能先处于 pending；批准前通常只能读取 public 资源。",
        "若组织启用 SSO，创建或首次使用时可能需要先完成组织 SSO。",
        "Classic token 只建议作为兼容兜底；它通常覆盖你可访问的更多仓库，权限面更大。"
    )
)

internal val githubStrategyGuides: List<GitHubStrategyGuide> = listOf(
    GitHubStrategyGuide(
        option = GitHubLookupStrategyOption.AtomFeed,
        summary = "通过仓库 releases.atom 和 latest 跳转推断稳定版/预发行。",
        pros = listOf(
            "无需 Token，开箱即用，适合公开仓库。",
            "配置最少，切换成本低。"
        ),
        cons = listOf(
            "依赖 Atom feed 和页面跳转，结构变化时更容易受影响。",
            "对私有仓库和更精细的 release 元数据支持较弱。"
        ),
        requirement = "无需额外凭证。"
    ),
    GitHubStrategyGuide(
        option = GitHubLookupStrategyOption.GitHubApiToken,
        summary = "通过 GitHub Releases API 直接读取 release 列表和 prerelease 标记。",
        pros = listOf(
            "数据结构更稳定，能直接识别 GitHub 的 prerelease 标记。",
            "空 token 时也可先走游客 API，适合少量追踪快速体验。"
        ),
        cons = listOf(
            "游客 API 额度很低，追踪项目较多时更容易遇到限流。",
            "token 失效、权限不足或被撤销时会退回错误或访问受限。"
        ),
        requirement = "token 选填；未填写时自动使用游客 API。"
    )
)
