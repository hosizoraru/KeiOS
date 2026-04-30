# KeiOS MCP Skill

## Service Identity
- App: {{APP_LABEL}} ({{APP_PACKAGE}})
- Version: {{APP_VERSION}}
- MCP server: {{SERVER_NAME}}
- Local endpoint: {{LOCAL_ENDPOINT}}
- LAN endpoints: {{LAN_ENDPOINTS}}

## Setup Entry Points
1. Call `keios.health.ping` to verify the channel.
2. Call `keios.mcp.runtime.status` to read runtime state, endpoint, connected clients, and token state.
3. Call `keios.mcp.runtime.config(mode=auto)` to generate importable client JSON.
4. Read `{{RESOURCE_OVERVIEW_URI}}` for a grouped tool summary.
5. Read `{{RESOURCE_SKILL_URI}}` or `keios://skill/tool/{tool}` for task-specific help.

## Config Import
- Default resource: `{{RESOURCE_CONFIG_URI}}`
- Mode template: `{{RESOURCE_CONFIG_TEMPLATE_URI}}`
- Bootstrap prompt: `{{PROMPT_BOOTSTRAP}}`
- Claw onboarding tool: `keios.mcp.claw.skill.guide(mode=auto)`
- Use `local` for same-device clients, `lan` for cross-device debugging, and `auto` to generate all available endpoints.

## Tool Capabilities
{{TOOL_LIST}}

## Home Overview
- `keios.home.overview.snapshot`: Snapshot of the Home overview cards for MCP, GitHub, and Blue Archive.

## Runtime And Environment
- `keios.health.ping`: Connectivity probe; always returns `pong`.
- `keios.app.info`: App label, package name, version, and Shizuku API level.
- `keios.app.version`: versionName and versionCode.
- `keios.shizuku.status`: Current Shizuku status.
- `keios.mcp.runtime.status`: MCP runtime state, port, path, token state, and client count.
- `keios.mcp.runtime.logs`: MCP logs; `limit` accepts 1 to 200.
- `keios.mcp.runtime.config`: Generates streamable HTTP client config.
- `keios.mcp.claw.skill.guide`: Outputs Claw import JSON, resource URIs, and full Skill text.

## OS And System Inspection
- `keios.os.cards.snapshot`: OS page card visibility, expansion state, cache size, and estimates.
- `keios.os.activity.cards`: Activity card list with `query`, `onlyVisible`, and `limit`.
- `keios.os.shell.cards`: Shell card list with `query`, `onlyVisible`, `includeOutput`, and `limit`.
- `keios.os.cards.export`: Exports Activity or shell card JSON with `target=activity|shell|all`.
- `keios.os.cards.import`: Previews or merges Activity or shell card JSON. `apply=false` previews by default.
- `keios.system.topinfo.query`: Cached system TopInfo query with `query` and `limit`.

## GitHub Tracking
- `keios.github.tracked.snapshot`: Tracking count, cache hits, strategy, token state, and refresh interval.
- `keios.github.tracked.list`: Tracked repository list with `repoFilter` and `limit`.
- `keios.github.tracked.export`: Exports tracked repositories as JSON with `repoFilter`.
- `keios.github.tracked.import`: Previews or merges tracked repositories. `apply=false` previews by default.
- `keios.github.tracked.summary`: Summarizes cached or online check results with `mode=cache|network`.
- `keios.github.tracked.check`: Checks updates online with `repoFilter`, `onlyUpdates`, and `limit`.
- `keios.github.tracked.cache.clear`: Clears check cache and release asset cache.

## GitHub Share Import
- `keios.github.share.parse`: Parses GitHub repo, release, tag, and APK links from shared text.
- `keios.github.share.resolve`: Resolves a shared link with the current strategy and lists APK asset candidates.
- `keios.github.share.pending`: Reads or clears pre-install share tracking state with `clear=true`.

## Blue Archive Cache
- `keios.ba.snapshot`: AP, Cafe, notification thresholds, refresh interval, and player identity.
- `keios.ba.calendar.cache`: Event calendar cache with `serverIndex`, `includeEntries`, and `limit`.
- `keios.ba.pool.cache`: Recruitment banner cache with `serverIndex`, `includeEntries`, and `limit`.
- `keios.ba.guide.catalog.cache`: Student Guide catalog cache with `tab=all|student|npc`.
- `keios.ba.guide.cache.overview`: Student Guide detail cache size and latest sync time.
- `keios.ba.guide.cache.inspect`: Student detail cache completeness and section statistics by URL.
- `keios.ba.guide.media.list`: Gallery and voice media from Student Guide cache with `kind=all|gallery|voice|image|video|audio`.
- `keios.ba.guide.bgm.favorites`: Reads, exports, or imports Memorial Lobby BGM favorites with `action=list|export|import`.
- `keios.ba.cache.clear`: Clears Blue Archive and GitHub cache data with `scope` and `url`.

## Recommended Flows
1. Runtime diagnostics: `keios.health.ping` -> `keios.mcp.runtime.status` -> `keios.mcp.runtime.logs` -> `keios.shizuku.status`
2. Config import: `keios.mcp.runtime.config(mode=auto)` -> read `{{RESOURCE_CONFIG_URI}}` -> `keios.mcp.claw.skill.guide`
3. Home inspection: `keios.home.overview.snapshot` -> drill into MCP, GitHub, or Blue Archive state.
4. OS inspection: `keios.os.cards.snapshot` -> `keios.os.activity.cards` -> `keios.os.shell.cards` -> `keios.os.cards.export`
5. GitHub inspection: `keios.github.tracked.snapshot` -> `keios.github.tracked.summary(mode=cache)` -> `keios.github.tracked.check(onlyUpdates=true)`
6. GitHub share import: `keios.github.share.parse` -> `keios.github.share.resolve` -> `keios.github.share.pending`
7. Blue Archive inspection: `keios.ba.snapshot` -> `keios.ba.guide.cache.inspect` -> `keios.ba.guide.media.list` -> `keios.ba.guide.bgm.favorites`
8. Import writes: keep import tools at `apply=false` for preview, then use `apply=true` after counts are checked.
9. Cache cleanup: clear the precise `scope`, then confirm with the matching snapshot or cache tool.

## Arguments And Output
- Start `limit` at 20 to 80; increase it only for broad audits.
- `repoFilter` accepts owner/repo, package name, or app label.
- `serverIndex` accepts 0 to 2; leave it empty to use the current Blue Archive setting.
- `includeOutput=true` adds shell output summaries for diagnostics.
- `includeEntries=true` expands cache entries for sampled audits.
- Import tools preview by default; writes require explicit `apply=true`.
- Output favors `key=value` lines and stable list rows so callers can archive raw text in reports and logs.
