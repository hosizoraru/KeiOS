package os.kei.mcp.server

import java.util.Locale

data class McpToolMeta(
    val name: String,
    val description: String
)

internal object McpToolCatalog {
    val runtimeToolNames = listOf(
        "keios.health.ping",
        "keios.mcp.runtime.status",
        "keios.mcp.runtime.logs",
        "keios.mcp.runtime.config",
        "keios.mcp.claw.skill.guide"
    )

    val homeToolNames = listOf(
        "keios.home.overview.snapshot"
    )

    val osToolNames = listOf(
        "keios.os.cards.snapshot",
        "keios.os.activity.cards",
        "keios.os.shell.cards",
        "keios.os.cards.export",
        "keios.os.cards.import"
    )

    val systemToolNames = listOf(
        "keios.system.topinfo.query"
    )

    val githubToolNames = listOf(
        "keios.github.tracked.snapshot",
        "keios.github.tracked.list",
        "keios.github.tracked.export",
        "keios.github.tracked.import",
        "keios.github.tracked.check",
        "keios.github.tracked.summary",
        "keios.github.tracked.cache.clear",
        "keios.github.share.parse",
        "keios.github.share.resolve",
        "keios.github.share.pending"
    )

    val baToolNames = listOf(
        "keios.ba.snapshot",
        "keios.ba.calendar.cache",
        "keios.ba.pool.cache",
        "keios.ba.guide.catalog.cache",
        "keios.ba.guide.cache.overview",
        "keios.ba.guide.cache.inspect",
        "keios.ba.guide.media.list",
        "keios.ba.guide.bgm.favorites",
        "keios.ba.cache.clear"
    )

    val all: List<McpToolMeta>
        get() = englishTools

    fun forLocale(locale: Locale): List<McpToolMeta> {
        return when {
            locale.language.equals("zh", ignoreCase = true) -> simplifiedChineseTools
            locale.language.equals("ja", ignoreCase = true) -> japaneseTools
            else -> englishTools
        }
    }

    fun descriptionFor(name: String, locale: Locale): String {
        return forLocale(locale).firstOrNull { it.name == name }?.description
            ?: englishTools.firstOrNull { it.name == name }?.description
            ?: ""
    }

    private val englishTools: List<McpToolMeta> = listOf(
        McpToolMeta("keios.health.ping", "Health probe; returns pong."),
        McpToolMeta("keios.app.info", "Read app metadata: label, package, version, and Shizuku API level."),
        McpToolMeta("keios.app.version", "Read versionName and versionCode."),
        McpToolMeta("keios.shizuku.status", "Read the current Shizuku status string."),
        McpToolMeta("keios.mcp.runtime.status", "Read MCP runtime state: endpoint, client count, and errors."),
        McpToolMeta("keios.mcp.runtime.logs", "Read MCP runtime logs. Args: limit=1..200."),
        McpToolMeta("keios.mcp.runtime.config", "Generate importable MCP JSON. Args: mode=auto|local|lan, endpoint/serverName overrides."),
        McpToolMeta("keios.mcp.claw.skill.guide", "Build Claw onboarding: import JSON, SKILL.md, and registration steps."),
        McpToolMeta("keios.home.overview.snapshot", "Read the Home overview card snapshot for MCP, GitHub, and Blue Archive."),
        McpToolMeta("keios.system.topinfo.query", "Query cached system TopInfo values. Args: query, limit."),
        McpToolMeta("keios.os.cards.snapshot", "Read the OS page card overview: visibility, expansion state, cache size, and stats."),
        McpToolMeta("keios.os.activity.cards", "Read Activity card entries. Args: query, onlyVisible, limit."),
        McpToolMeta("keios.os.shell.cards", "Read shell card entries. Args: query, onlyVisible, includeOutput, limit."),
        McpToolMeta("keios.os.cards.export", "Export OS Activity or shell card JSON. Args: target=activity|shell|all."),
        McpToolMeta("keios.os.cards.import", "Preview or import OS Activity or shell card JSON. apply=false previews by default."),
        McpToolMeta("keios.github.tracked.snapshot", "Read GitHub tracking settings and cache snapshot."),
        McpToolMeta("keios.github.tracked.list", "List tracked repositories. Args: repoFilter, limit."),
        McpToolMeta("keios.github.tracked.export", "Export tracked GitHub repositories as JSON. Args: repoFilter."),
        McpToolMeta("keios.github.tracked.import", "Preview or import tracked GitHub repositories. apply=false previews by default."),
        McpToolMeta("keios.github.tracked.check", "Check tracked repositories for updates online. Args: repoFilter, onlyUpdates, limit."),
        McpToolMeta("keios.github.tracked.summary", "Read tracking summary. Args: mode=cache|network, repoFilter."),
        McpToolMeta("keios.github.tracked.cache.clear", "Clear GitHub check cache and release asset cache."),
        McpToolMeta("keios.github.share.parse", "Parse repo, release, tag, and APK links from shared GitHub text."),
        McpToolMeta("keios.github.share.resolve", "Resolve a shared GitHub link and list installable APK asset candidates."),
        McpToolMeta("keios.github.share.pending", "Read or clear pending pre-install GitHub share tracking state."),
        McpToolMeta("keios.ba.snapshot", "Read the Blue Archive snapshot: AP, Cafe, notification thresholds, and refresh interval."),
        McpToolMeta("keios.ba.calendar.cache", "Read Blue Archive event calendar cache. Args: serverIndex, includeEntries, limit."),
        McpToolMeta("keios.ba.pool.cache", "Read Blue Archive recruitment banner cache. Args: serverIndex, includeEntries, limit."),
        McpToolMeta("keios.ba.guide.catalog.cache", "Read Student Guide catalog cache. Args: tab, includeEntries, limit."),
        McpToolMeta("keios.ba.guide.cache.overview", "Read Student Guide detail cache overview."),
        McpToolMeta("keios.ba.guide.cache.inspect", "Inspect Student Guide detail cache by URL. Args: url, includeSections, refreshIntervalHours."),
        McpToolMeta("keios.ba.guide.media.list", "List gallery and voice media from Student Guide detail cache."),
        McpToolMeta("keios.ba.guide.bgm.favorites", "Read, export, or import Memorial Lobby BGM favorites."),
        McpToolMeta("keios.ba.cache.clear", "Clear Blue Archive and GitHub cache data. Args: scope, url.")
    )

    private val simplifiedChineseTools: List<McpToolMeta> = listOf(
        McpToolMeta("keios.health.ping", "健康探针，返回 pong。"),
        McpToolMeta("keios.app.info", "读取应用元信息：label、package、version 与 Shizuku API。"),
        McpToolMeta("keios.app.version", "读取 versionName 与 versionCode。"),
        McpToolMeta("keios.shizuku.status", "读取当前 Shizuku 状态。"),
        McpToolMeta("keios.mcp.runtime.status", "读取 MCP 运行态：endpoint、客户端数量与错误。"),
        McpToolMeta("keios.mcp.runtime.logs", "读取 MCP 运行日志。参数：limit=1..200。"),
        McpToolMeta("keios.mcp.runtime.config", "生成可导入 MCP JSON。参数：mode=auto|local|lan，支持 endpoint/serverName 覆盖。"),
        McpToolMeta("keios.mcp.claw.skill.guide", "生成 Claw 接入引导：导入 JSON、SKILL.md 与注册步骤。"),
        McpToolMeta("keios.home.overview.snapshot", "读取 Home 总览卡片快照，覆盖 MCP、GitHub 与 Blue Archive。"),
        McpToolMeta("keios.system.topinfo.query", "查询系统 TopInfo 缓存值。参数：query、limit。"),
        McpToolMeta("keios.os.cards.snapshot", "读取 OS 页面卡片总览：可见性、展开态、缓存体积与统计。"),
        McpToolMeta("keios.os.activity.cards", "读取 Activity card 列表。参数：query、onlyVisible、limit。"),
        McpToolMeta("keios.os.shell.cards", "读取 shell card 列表。参数：query、onlyVisible、includeOutput、limit。"),
        McpToolMeta("keios.os.cards.export", "导出 OS Activity 或 shell card JSON。参数：target=activity|shell|all。"),
        McpToolMeta("keios.os.cards.import", "预览或导入 OS Activity 或 shell card JSON。apply=false 默认预览。"),
        McpToolMeta("keios.github.tracked.snapshot", "读取 GitHub 跟踪设置与缓存快照。"),
        McpToolMeta("keios.github.tracked.list", "列出已跟踪仓库。参数：repoFilter、limit。"),
        McpToolMeta("keios.github.tracked.export", "导出已跟踪 GitHub 仓库 JSON。参数：repoFilter。"),
        McpToolMeta("keios.github.tracked.import", "预览或导入 GitHub 跟踪仓库。apply=false 默认预览。"),
        McpToolMeta("keios.github.tracked.check", "在线检查已跟踪仓库更新。参数：repoFilter、onlyUpdates、limit。"),
        McpToolMeta("keios.github.tracked.summary", "读取跟踪汇总。参数：mode=cache|network、repoFilter。"),
        McpToolMeta("keios.github.tracked.cache.clear", "清理 GitHub 检查缓存与 release asset 缓存。"),
        McpToolMeta("keios.github.share.parse", "解析 GitHub 分享文本中的 repo、release、tag 与 APK 链接。"),
        McpToolMeta("keios.github.share.resolve", "解析 GitHub 分享链接，并列出可安装 APK asset 候选。"),
        McpToolMeta("keios.github.share.pending", "读取或清除分享安装前跟踪状态。"),
        McpToolMeta("keios.ba.snapshot", "读取 Blue Archive 快照：AP、咖啡厅、通知阈值与刷新间隔。"),
        McpToolMeta("keios.ba.calendar.cache", "读取 Blue Archive 活动日历缓存。参数：serverIndex、includeEntries、limit。"),
        McpToolMeta("keios.ba.pool.cache", "读取 Blue Archive 招募卡池缓存。参数：serverIndex、includeEntries、limit。"),
        McpToolMeta("keios.ba.guide.catalog.cache", "读取学生图鉴目录缓存。参数：tab、includeEntries、limit。"),
        McpToolMeta("keios.ba.guide.cache.overview", "读取学生图鉴详情缓存总览。"),
        McpToolMeta("keios.ba.guide.cache.inspect", "按 URL 检查学生图鉴详情缓存。参数：url、includeSections、refreshIntervalHours。"),
        McpToolMeta("keios.ba.guide.media.list", "从学生图鉴详情缓存列出影画鉴赏与语音媒体。"),
        McpToolMeta("keios.ba.guide.bgm.favorites", "读取、导出或导入回忆大厅 BGM 收藏。"),
        McpToolMeta("keios.ba.cache.clear", "清理 Blue Archive 与 GitHub 缓存数据。参数：scope、url。")
    )

    private val japaneseTools: List<McpToolMeta> = listOf(
        McpToolMeta("keios.health.ping", "ヘルスチェック。pong を返します。"),
        McpToolMeta("keios.app.info", "アプリ情報を読み取ります: ラベル、パッケージ、バージョン、Shizuku API レベル。"),
        McpToolMeta("keios.app.version", "versionName と versionCode を読み取ります。"),
        McpToolMeta("keios.shizuku.status", "現在の Shizuku 状態を読み取ります。"),
        McpToolMeta("keios.mcp.runtime.status", "MCP 実行状態を読み取ります: endpoint、クライアント数、エラー。"),
        McpToolMeta("keios.mcp.runtime.logs", "MCP 実行ログを読み取ります。引数: limit=1..200。"),
        McpToolMeta("keios.mcp.runtime.config", "インポート可能な MCP JSON を生成します。引数: mode=auto|local|lan、endpoint/serverName の上書き。"),
        McpToolMeta("keios.mcp.claw.skill.guide", "Claw 接続ガイドを生成します: インポート JSON、SKILL.md、登録手順。"),
        McpToolMeta("keios.home.overview.snapshot", "Home の概要カードスナップショットを読み取ります。MCP、GitHub、ブルーアーカイブを含みます。"),
        McpToolMeta("keios.system.topinfo.query", "キャッシュ済みのシステム TopInfo を検索します。引数: query、limit。"),
        McpToolMeta("keios.os.cards.snapshot", "OS ページのカード概要を読み取ります: 表示状態、展開状態、キャッシュサイズ、統計。"),
        McpToolMeta("keios.os.activity.cards", "Activity カードを読み取ります。引数: query、onlyVisible、limit。"),
        McpToolMeta("keios.os.shell.cards", "Shell カードを読み取ります。引数: query、onlyVisible、includeOutput、limit。"),
        McpToolMeta("keios.os.cards.export", "OS Activity または Shell カード JSON をエクスポートします。引数: target=activity|shell|all。"),
        McpToolMeta("keios.os.cards.import", "OS Activity または Shell カード JSON をプレビューまたはインポートします。既定は apply=false のプレビューです。"),
        McpToolMeta("keios.github.tracked.snapshot", "GitHub 追跡設定とキャッシュのスナップショットを読み取ります。"),
        McpToolMeta("keios.github.tracked.list", "追跡中リポジトリを一覧します。引数: repoFilter、limit。"),
        McpToolMeta("keios.github.tracked.export", "追跡中 GitHub リポジトリを JSON でエクスポートします。引数: repoFilter。"),
        McpToolMeta("keios.github.tracked.import", "GitHub 追跡リポジトリをプレビューまたはインポートします。既定は apply=false のプレビューです。"),
        McpToolMeta("keios.github.tracked.check", "追跡中リポジトリの更新をオンライン確認します。引数: repoFilter、onlyUpdates、limit。"),
        McpToolMeta("keios.github.tracked.summary", "追跡サマリーを読み取ります。引数: mode=cache|network、repoFilter。"),
        McpToolMeta("keios.github.tracked.cache.clear", "GitHub の確認キャッシュとリリースアセットキャッシュを削除します。"),
        McpToolMeta("keios.github.share.parse", "共有された GitHub テキストから repo、release、tag、APK リンクを解析します。"),
        McpToolMeta("keios.github.share.resolve", "共有 GitHub リンクを解決し、インストール可能な APK アセット候補を一覧します。"),
        McpToolMeta("keios.github.share.pending", "インストール前 GitHub 共有追跡の保留状態を読み取り、または削除します。"),
        McpToolMeta("keios.ba.snapshot", "ブルーアーカイブのスナップショットを読み取ります: AP、カフェ、通知しきい値、更新間隔。"),
        McpToolMeta("keios.ba.calendar.cache", "ブルーアーカイブのイベントカレンダーキャッシュを読み取ります。引数: serverIndex、includeEntries、limit。"),
        McpToolMeta("keios.ba.pool.cache", "ブルーアーカイブの募集キャッシュを読み取ります。引数: serverIndex、includeEntries、limit。"),
        McpToolMeta("keios.ba.guide.catalog.cache", "生徒名簿カタログキャッシュを読み取ります。引数: tab、includeEntries、limit。"),
        McpToolMeta("keios.ba.guide.cache.overview", "生徒情報詳細キャッシュの概要を読み取ります。"),
        McpToolMeta("keios.ba.guide.cache.inspect", "URL で生徒情報詳細キャッシュを検査します。引数: url、includeSections、refreshIntervalHours。"),
        McpToolMeta("keios.ba.guide.media.list", "生徒情報詳細キャッシュからギャラリーとボイスメディアを一覧します。"),
        McpToolMeta("keios.ba.guide.bgm.favorites", "メモリアルロビー BGM のお気に入りを読み取り、エクスポート、またはインポートします。"),
        McpToolMeta("keios.ba.cache.clear", "ブルーアーカイブと GitHub のキャッシュデータを削除します。引数: scope、url。")
    )
}
