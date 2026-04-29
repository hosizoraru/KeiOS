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

    val osToolNames = listOf(
        "keios.os.cards.snapshot",
        "keios.os.activity.cards",
        "keios.os.shell.cards"
    )

    val systemToolNames = listOf(
        "keios.system.topinfo.query"
    )

    val githubToolNames = listOf(
        "keios.github.tracked.snapshot",
        "keios.github.tracked.list",
        "keios.github.tracked.check",
        "keios.github.tracked.summary",
        "keios.github.tracked.cache.clear"
    )

    val baToolNames = listOf(
        "keios.ba.snapshot",
        "keios.ba.calendar.cache",
        "keios.ba.pool.cache",
        "keios.ba.guide.catalog.cache",
        "keios.ba.guide.cache.overview",
        "keios.ba.guide.cache.inspect",
        "keios.ba.cache.clear"
    )

    val all: List<McpToolMeta> = listOf(
        McpToolMeta("keios.health.ping", "健康探针，返回 pong。"),
        McpToolMeta("keios.app.info", "读取应用元信息（label/package/version/shizukuApi）。"),
        McpToolMeta("keios.app.version", "读取版本信息（versionName/versionCode）。"),
        McpToolMeta("keios.shizuku.status", "读取 Shizuku 当前状态字符串。"),
        McpToolMeta("keios.mcp.runtime.status", "读取 MCP 运行态（endpoint/clientCount/error）。"),
        McpToolMeta("keios.mcp.runtime.logs", "读取 MCP 日志（limit=1..200）。"),
        McpToolMeta("keios.mcp.runtime.config", "生成可导入 MCP JSON（mode=auto|local|lan，支持 endpoint/serverName 覆盖）。"),
        McpToolMeta("keios.mcp.claw.skill.guide", "生成 Claw 接入引导（导入 JSON + SKILL.md + 注册步骤）。"),
        McpToolMeta("keios.system.topinfo.query", "查询系统 TopInfo 缓存（query/limit）。"),
        McpToolMeta("keios.os.cards.snapshot", "读取 OS 页面卡片总览（可见性/展开态/缓存体积/统计）。"),
        McpToolMeta("keios.os.activity.cards", "读取活动 card 列表（query/onlyVisible/limit）。"),
        McpToolMeta("keios.os.shell.cards", "读取 shell card 列表（query/onlyVisible/includeOutput/limit）。"),
        McpToolMeta("keios.github.tracked.snapshot", "读取 GitHub 跟踪配置与缓存快照。"),
        McpToolMeta("keios.github.tracked.list", "读取跟踪仓库列表（repoFilter/limit）。"),
        McpToolMeta("keios.github.tracked.check", "在线检查跟踪仓库更新（repoFilter/onlyUpdates/limit）。"),
        McpToolMeta("keios.github.tracked.summary", "读取跟踪汇总（mode=cache|network，支持 repoFilter）。"),
        McpToolMeta("keios.github.tracked.cache.clear", "清空 GitHub 检查缓存与 Release 资源缓存。"),
        McpToolMeta("keios.ba.snapshot", "读取 BA 核心快照（AP/咖啡厅/通知阈值/刷新间隔）。"),
        McpToolMeta("keios.ba.calendar.cache", "读取 BA 活动日历缓存（serverIndex/includeEntries/limit）。"),
        McpToolMeta("keios.ba.pool.cache", "读取 BA 卡池缓存（serverIndex/includeEntries/limit）。"),
        McpToolMeta("keios.ba.guide.catalog.cache", "读取 BA 图鉴总览缓存（tab/includeEntries/limit）。"),
        McpToolMeta("keios.ba.guide.cache.overview", "读取学生图鉴详情缓存总览。"),
        McpToolMeta("keios.ba.guide.cache.inspect", "按 URL 检查学生图鉴详情缓存（url/includeSections/refreshIntervalHours）。"),
        McpToolMeta("keios.ba.cache.clear", "清理 BA/GitHub 缓存（scope=all|ba_calendar_pool|ba_guide_catalog|ba_guide_all|ba_guide_url|github_check）。")
    )
}
