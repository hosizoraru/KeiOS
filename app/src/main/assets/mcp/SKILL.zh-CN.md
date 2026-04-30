# KeiOS MCP Skill

## 服务身份
- App：{{APP_LABEL}}（{{APP_PACKAGE}}）
- 版本：{{APP_VERSION}}
- MCP 服务：{{SERVER_NAME}}
- 本机 endpoint：{{LOCAL_ENDPOINT}}
- 局域网 endpoint：{{LAN_ENDPOINTS}}

## 接入入口
1. 调用 `keios.health.ping` 确认通道连通。
2. 调用 `keios.mcp.runtime.status` 读取运行态、endpoint、在线客户端与 token 状态。
3. 调用 `keios.mcp.runtime.config(mode=auto)` 生成客户端导入 JSON。
4. 读取 `{{RESOURCE_OVERVIEW_URI}}` 获取工具分组摘要。
5. 针对当前任务读取 `{{RESOURCE_SKILL_URI}}` 或 `keios://skill/tool/{tool}`。

## 配置导入
- 默认资源：`{{RESOURCE_CONFIG_URI}}`
- mode 模板：`{{RESOURCE_CONFIG_TEMPLATE_URI}}`
- 初始化 Prompt：`{{PROMPT_BOOTSTRAP}}`
- Claw 接入引导：`keios.mcp.claw.skill.guide(mode=auto)`
- `local` 用于同设备客户端，`lan` 用于跨设备联调，`auto` 会生成当前可用入口。

## 工具能力
{{TOOL_LIST}}

## Home 总览
- `keios.home.overview.snapshot`：读取 Home 上 MCP、GitHub、Blue Archive 总览卡片的数据源快照。

## 运行与环境
- `keios.health.ping`：连接探针，固定返回 `pong`。
- `keios.app.info`：应用标签、包名、版本与 Shizuku API。
- `keios.app.version`：versionName 与 versionCode。
- `keios.shizuku.status`：当前 Shizuku 状态。
- `keios.mcp.runtime.status`：MCP 运行态、端口、路径、token 与客户端数量。
- `keios.mcp.runtime.logs`：MCP 日志，`limit` 范围 1 到 200。
- `keios.mcp.runtime.config`：生成 streamable HTTP 客户端配置。
- `keios.mcp.claw.skill.guide`：输出 Claw 导入 JSON、资源 URI 与完整 Skill 文本。

## OS 与系统巡检
- `keios.os.cards.snapshot`：OS 页面可见卡片、展开态、缓存体积与估算值。
- `keios.os.activity.cards`：Activity card 列表，支持 `query`、`onlyVisible`、`limit`。
- `keios.os.shell.cards`：shell card 列表，支持 `query`、`onlyVisible`、`includeOutput`、`limit`。
- `keios.os.cards.export`：导出 Activity 或 shell card JSON，支持 `target=activity|shell|all`。
- `keios.os.cards.import`：预览或合并导入 Activity 或 shell card JSON，`apply=false` 默认预览。
- `keios.system.topinfo.query`：系统 TopInfo 缓存查询，支持 `query` 与 `limit`。

## GitHub 跟踪
- `keios.github.tracked.snapshot`：跟踪数量、缓存命中、策略、token 与刷新间隔。
- `keios.github.tracked.list`：跟踪仓库列表，支持 `repoFilter` 与 `limit`。
- `keios.github.tracked.export`：导出跟踪仓库 JSON，支持 `repoFilter`。
- `keios.github.tracked.import`：预览或合并导入跟踪仓库，`apply=false` 默认预览。
- `keios.github.tracked.summary`：汇总缓存或在线检查结果，`mode=cache|network`。
- `keios.github.tracked.check`：在线检查更新，支持 `repoFilter`、`onlyUpdates`、`limit`。
- `keios.github.tracked.cache.clear`：清理检查缓存与 release asset 缓存。

## GitHub 分享导入
- `keios.github.share.parse`：解析分享文本中的 GitHub repo、release、tag 与 APK 链接。
- `keios.github.share.resolve`：按当前策略解析分享链接，并列出 APK asset 候选。
- `keios.github.share.pending`：查看或清除分享安装前跟踪状态，支持 `clear=true`。

## Blue Archive 缓存
- `keios.ba.snapshot`：AP、咖啡厅、通知阈值、刷新间隔与玩家标识。
- `keios.ba.calendar.cache`：活动日历缓存，支持 `serverIndex`、`includeEntries`、`limit`。
- `keios.ba.pool.cache`：招募卡池缓存，支持 `serverIndex`、`includeEntries`、`limit`。
- `keios.ba.guide.catalog.cache`：学生图鉴目录缓存，支持 `tab=all|student|npc`。
- `keios.ba.guide.cache.overview`：学生图鉴详情缓存总体体积与最近同步时间。
- `keios.ba.guide.cache.inspect`：按 URL 检查学生详情缓存完整度与分区统计。
- `keios.ba.guide.media.list`：列出学生图鉴缓存中的影画鉴赏与语音媒体，支持 `kind=all|gallery|voice|image|video|audio`。
- `keios.ba.guide.bgm.favorites`：读取、导出或导入回忆大厅 BGM 收藏，支持 `action=list|export|import`。
- `keios.ba.cache.clear`：清理 Blue Archive 与 GitHub 缓存，支持 `scope` 与 `url`。

## 推荐任务流
1. 运行排障：`keios.health.ping` -> `keios.mcp.runtime.status` -> `keios.mcp.runtime.logs` -> `keios.shizuku.status`
2. 配置导入：`keios.mcp.runtime.config(mode=auto)` -> 读取 `{{RESOURCE_CONFIG_URI}}` -> `keios.mcp.claw.skill.guide`
3. Home 巡检：`keios.home.overview.snapshot` -> 按 MCP、GitHub、Blue Archive 状态下钻
4. OS 巡检：`keios.os.cards.snapshot` -> `keios.os.activity.cards` -> `keios.os.shell.cards` -> `keios.os.cards.export`
5. GitHub 巡检：`keios.github.tracked.snapshot` -> `keios.github.tracked.summary(mode=cache)` -> `keios.github.tracked.check(onlyUpdates=true)`
6. GitHub 分享导入：`keios.github.share.parse` -> `keios.github.share.resolve` -> `keios.github.share.pending`
7. Blue Archive 巡检：`keios.ba.snapshot` -> `keios.ba.guide.cache.inspect` -> `keios.ba.guide.media.list` -> `keios.ba.guide.bgm.favorites`
8. 导入写入：先让 import 工具保持 `apply=false` 预览，确认数量后再用 `apply=true`
9. 缓存清理：使用精确 `scope` 清理，再调用对应 snapshot 或 cache 工具确认

## 参数与输出
- `limit` 先用 20 到 80；大范围审计时再提高到工具上限。
- `repoFilter` 支持 owner/repo、包名、应用名。
- `serverIndex` 范围 0 到 2；留空时使用当前 Blue Archive 设置。
- `includeOutput=true` 会输出 shell 运行摘要，适合排障。
- `includeEntries=true` 会展开缓存条目，适合抽样审计。
- 导入类工具默认预览；写入需要显式传 `apply=true`。
- 输出以 `key=value` 和固定列表行为主，调用端可保留原始文本用于报告与日志归档。
