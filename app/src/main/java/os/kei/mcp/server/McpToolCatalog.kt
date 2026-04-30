package os.kei.mcp.server

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

    val all: List<McpToolMeta> = listOf(
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
}
