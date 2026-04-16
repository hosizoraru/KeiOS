# KeiOS MCP Skill

## 1. 服务身份
- App: {{APP_LABEL}} ({{APP_PACKAGE}})
- 版本: {{APP_VERSION}}
- MCP 服务名: {{SERVER_NAME}}
- 本机 endpoint: {{LOCAL_ENDPOINT}}
- 局域网 endpoint: {{LAN_ENDPOINTS}}

## 2. 快速接入
1. 读取资源 `{{RESOURCE_CONFIG_URI}}` 获取默认接入 JSON（auto）。
2. 如需指定模式，读取模板 `{{RESOURCE_CONFIG_TEMPLATE_URI}}`（`mode=auto|local|lan`）。
3. 如果客户端不读取资源，调用工具 `keios.mcp.runtime.config` 获取同等配置。
4. 完成接入后执行 `keios.health.ping` 与 `keios.mcp.runtime.status` 验证连通。

## 3. 启动顺序
1. 读取资源 `{{RESOURCE_SKILL_URI}}` 获取完整技能说明。
2. 读取资源 `{{RESOURCE_OVERVIEW_URI}}` 获取摘要与入口。
3. 调用 Prompt `{{PROMPT_BOOTSTRAP}}` 生成当前任务计划。
4. 按任务调用对应工具。

## 4. 工具能力总览
{{TOOL_LIST}}

## 5. 常用任务流
- MCP 运行排障：`keios.mcp.runtime.status` -> `keios.mcp.runtime.logs` -> `keios.shizuku.status`
- 系统参数检索：`keios.system.topinfo.query`
- GitHub 跟踪检查：`keios.github.tracked.snapshot` -> `keios.github.tracked.check` -> `keios.github.tracked.summary`
- BA 缓存巡检：`keios.ba.snapshot` -> `keios.ba.calendar.cache` -> `keios.ba.guide.cache.inspect`
- 缓存清理：`keios.ba.cache.clear`（按 `scope` 精准清理）

## 6. 输出约定
- 绝大多数工具输出为 `key=value` 文本，方便快速检索。
- 列表工具使用多行结构，保留关键字段（id、名称、状态、时间戳）。
- 有网络依赖的工具会返回失败信息，不会静默吞错。
