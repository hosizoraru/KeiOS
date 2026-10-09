# KeiOS 功能完整介绍

<!-- markdownlint-disable MD013 -->

[English Version](FEATURES.md)

KeiOS 是一个面向日常使用的 Android 工具台。它把系统参数查看、MCP 服务管理、GitHub Releases / Actions
追踪、订阅项目追踪、GitHub Star 导入、仓库发现、Blue Archive 办公室提醒、JSON 数据迁移、本地 Issue
反馈和学生图鉴媒体浏览放在同一个移动端界面里。

## Home

Home 是状态入口，顶部 pill 展示 Shizuku、MCP、GitHub 和 BA 轻量状态，下方保留 MCP、GitHub、BA
三张聚焦摘要卡。顶部操作区可以调整底栏板块显示和 Home 摘要卡片。

## OS

OS 页面聚焦设备与系统信息：

- TopInfo，以及 System、Secure、Global、Android Properties、Java Properties、Linux environment 等键值表。
- TopInfo 会先展示设备、系统构建、CPU、运行时、内存、存储、语言时区、开发调试状态与 verified boot
  的可读摘要，再进入原始属性表。
- 支持搜索 OS 参数和活动入口。
- 可配置活动快捷卡片，内置 AOSP / Google 系统隐藏入口。
- 活动卡片和 Shell 卡片 sheet 支持搜索、分组和独立导入导出入口，导入前会预览并合并变化。
- 后台快捷方式动作支持刷新与页面入口类流程。
- 基于 Shizuku 的 Shell 运行器，支持命令历史、格式化输出、超时控制、高风险命令确认和保存到卡片。
- 系统快照缓存用于提升回访速度。
- v2 液态浮动 dock 承载添加、刷新、搜索和页面动作。

## MCP

MCP 页面用于管理本地 KeiOS MCP Server：

- 支持启动/停止、本机或局域网连接配置、端口/路径/Token 展示和配置复制。
- 入口工具、工作流工具和高级工具分区展示，并支持搜索与分组卡片，降低初次接入成本。
- 提供 Claw Skill 快速接入提示词、本地化 SKILL.md、工作流蓝图和单工具帮助资源。
- 51 个 MCP 工具覆盖 Home 总览、OS 卡片、系统 TopInfo、GitHub 跟踪 / 分享导入 / 仓库发现、Star List
  导入、包名扫描、仓库反扫、订阅项目检查，以及 Blue Archive 账号、日常模板、制造室与缓存操作。
- 补齐 typed catalog、JSON schema、工具 annotations、结构化输出和资源 / prompt 注册表，适配支持新协议面的
  MCP 客户端。
- 自适应运行日志与会话监控用于降低长期运行 MCP 服务时的前台开销。
- 支持前台保活服务、测试通知与通知构建器的语义化图标（Semantic Icon）。
- 通过通知兼容设置支持 HyperOS 超级岛模板与 AOSP Live Update 实时通知。

## GitHub

GitHub 页面用于追踪 GitHub 项目与订阅项目的更新：

- 支持 GitHub 仓库的稳定版 / 预发版检查，也支持直链 APK、JSON feed、伴生 JSON、带版本目录和 APK
  目录索引的远端版本检查。
- 支持 GitHub API 抓取方案配置，Token 可同时服务 Releases 与 Actions。
- 支持读取 Release 资源、APK 下载路由、本 App 托管安装路由和最新发布下载。
- 独立发行版历史页提供折叠 / 展开卡片、发行说明、筛选后的 APK、页码 / 标签跳转、纯标签过滤和旧版本直接安装。
- 独立 F-Droid 版本历史页先加载紧凑包元数据，再逐步补充文件详情，并展示反特性说明、校验值与签名信息以完成信任检查。
- 两种历史页中的旧版本都可查看 APK 信息并通过本 App 托管安装，所有分享与下载遵循“分享到安装器”和下载方式设置。
- 发行版选择能识别重新编号的项目（仅在列表显示出这种迹象时才用 `releases/latest` 确认），会让已被正式版取代或 14 天无人更新的预发布退出比较，忽略没有可下载文件的发行版，如实显示无法确定的比较，并在卡片上说明出乎意料的选择。
- 刷新诊断会为慢项目标出耗时所在的网络阶段（排队、DNS、建立连接、服务器等待、下载），附带请求数、流量与连接复用，并出现在历史、导出和 MCP 中。
- 支持 GitHub Actions 的分支、workflow、run、artifact 浏览，可走 nightly.link 公开链路或 GitHub API Token 链路。
- Actions 推荐 run 更新检查、应用图标通知、调试通知测试，并支持从通知深链进入对应追踪项目的 Actions sheet。
- 分支推荐会综合默认分支、近期活跃度、成功 run 与 artifact 可用性。
- Artifact 排序会突出 Android 包、构建类型、universal 包、更新时间和历史下载记录。
- 跟踪项可关联包名，并与本机已安装应用匹配；支持从最新稳定 release APK
  扫描包名、按包名和应用名反扫仓库、切换来源模式、订阅导入导出兼容和未保存变更确认。
- 深度仓库画像、健康评分、归档 / fork 信号、发行日志解析、发行日志翻译、精确 APK 版本模式和运行时缓存新鲜度检查。
- 订阅项目卡支持远端健康度、远端稳定 / 预发发布、Scene 风格 index 发行日志、已安装 App 名称、发行日志动作和未安装时的安装动作。
- 支持从仓库、Release、Tag、直链 APK 分享导入，提供透明窗口处理、通知优先 / Sheet 优先路由、外部安装器联动和本 App Shizuku 交付。
- 托管安装界面提供远端 / 本机 APK 对比、Manifest 检查、ABI / SDK / 包名提示、versionName / versionCode 展示、安装确认通知和 Shizuku PackageInstaller Session。
- 支持从自己的 stars、他人的公开 stars、公开 Star List 链接导入项目，导入页提供分类发现、搜索、多选筛选、Android/APK
  质量分类、release APK 验证和确认弹窗。
- 抓取方案诊断可对比 Atom 与 API 在 release 检查、包名扫描、仓库反扫上的表现。
- 支持排序 / 顺序 / 过滤记忆、悬浮 dock
  按过滤结果刷新、桌面快捷入口全量刷新、刷新通知、本地缓存摘要、追踪卡聚焦 / 自动滚动和 KeiOS 自追踪快捷入口。
- Actions 更新检查支持跟随全局或独立配置 1h / 2h / 3h / 更长间隔。

## 导入、反馈与迁移

- JSON 导入 Activity 支持 KeiOS 数据迁移，提供预览、指标卡片、结果跳转，并为 OS 卡片迁移数据提供多 schema 路由。
- 本地 GitHub Issue 助手支持结构化日志等级、Issue Markdown 生成和无遥测诊断。
- OS 卡片、Shell 卡片、GitHub 追踪和学生图鉴数据共享导入导出服务。

## BA 办公室

BA 页面是 Blue Archive 办公室仪表盘：

- 追踪 AP 与咖啡厅 AP，并按不同服务器时区计算。
- 支持进度与倒计时模板的 BA 专用超级岛显示。
- 支持从快捷方式触发 AP 与咖啡厅 AP 超级岛通知。
- 每个账号提供三个生成槽和三个物质合成槽，支持等级、数量、总时长编辑、精确完成提醒与紧凑运行概览。
- 可配置日常模板可为单账号或全部账号更新 AP 余量、摸头 / 邀请次数和制造计划，并可从快速设置磁贴、启动器快捷方式与 MCP 触发。
- 支持按服务器独立的昵称 / 好友码 ID 卡、好友码复制和办公室总览卡。
- 支持服务器、咖啡厅等级、AP 阈值、媒体旋转、自定义媒体保存位置等配置。
- 活动日历与卡池合并到同一页面，保留服务器分类、紧凑时间布局、通知设置和学生图鉴入口；宽窗口同时展示日历与卡池双栏，手机在同一路由内切换分类。

## 学生图鉴

学生图鉴扩展 BA 相关资料与媒体浏览：

- 图鉴目录支持学生和相关分区、搜索、排序、紧凑本地化标签、同步状态、本地缓存、实装学生筛选和 NPC /
  卫星学园筛选。
- 学生与相关条目详情支持资料、攻略/模拟、NPC / 卫星角色标签分类、鉴赏媒体、本地化语音语言标签、音频/视频内容和来源分享。
- 礼物偏好解析保留礼物图片和态度标记。
- MP4 回忆大厅保留；Spine 动态回忆大厅支持动作切换、缩放、移动、重置视角、关联 BGM 和静音，隐藏控件不会中断动画。
- 有对应资源的学生支持 3D 模型和动画选择、视角操作、进度、速度、循环、描边及可保存的背景调色盘。
  Spine 素材与模型文件采用有容量限制的缓存，复用已下载资源。
- 宽横屏 Pad 的模型工具使用叠在场景上的 Liquid Glass 侧栏，含内联调色盘与输入法避让；打开工具保持模型画布稳定，沉浸时仍可操作视角并继续播放。
- 动态大厅和 3D 默认使用系统 WebView。设置 → 界面提供独立的兼容呈现选项，供显示异常的设备选择，下次进入鉴赏页生效。
- 回忆大厅 BGM 收藏库支持播放队列、液态底栏、mini player、批量缓存、失败重试、删除撤销、导入导出、原生媒体通知和跳回学生详情。
- 媒体缓存和导出流程支持表达包/媒体包打包保存。
- NPC 与卫星条目适配较早期的 GameKee 页面，补齐更广的影画鉴赏解析、相关角色分类、更轻的信息行和缺少实装学生字段时的兼容。

## 设置与兼容

设置页集中管理运行策略：

- 主题模式、过渡动画、预测返回、搜索默认聚焦、预加载、应用语言入口和 Home HDR 高光。
- v2 液态玻璃 ActionBar、标题卡、搜索栏、浮动 dock、底栏、滑动时底栏完整特效策略和局部卡片按压反馈。
- 长 Liquid Sheet 使用懒加载，离屏玻璃跳过绘制，拖拽状态在布局阶段读取；现有材质、动画、虚化和交互效果保持完整。
- 主导航会在手机使用底栏，在常规平板宽度使用顶部导航，并在超宽窗口使用可记忆、可拖动收起的侧边栏。
- 设置、关于、MCP、OS 卡片与 Shell、BA 办公室、学生图鉴、合并后的活动日历与卡池、GitHub 追踪与历史、Play 会在宽度允许时使用可独立滚动的双栏布局。
- 图标设计以 Android Designs 作为默认图标，并提供 Apple Designs 焕新版图标切换。
- 二级页面自定义背景图和透明度覆盖卡片、chrome 与弹层场景。
- 通知权限、电池优化、OEM 自启动、应用列表访问和 Shizuku 状态。
- 超级岛通知样式、自动关闭时间、HyperOS 兼容绕过和恢复延迟调节；GitHub 刷新进度仍按任务结束收尾。
- 复制/文本选择模式、缓存诊断、调试日志、日志 ZIP 导出和一键清理缓存。
- 本地 GitHub Issue 反馈与结构化日志等级控制。
- 调试组件实验室与 Liquid 组件样张用于检查共享 chrome、按钮、下拉菜单、滑杆、进度条和 dock 行为。
- 简体中文、English、日本語 资源已覆盖主要显示面，并针对 BA 术语、Android 设置、GitHub、Shell 与 MCP Skill 做本地化。

## 平台基线

- 包名：`os.kei`。
- ABI：`arm64-v8a`。
- Android 基线：Android 15+（`minSdk 35`），`targetSdk=37`。
- UI 技术栈：Jetpack Compose `1.12.1`、Miuix KMP、Lifecycle ViewModel Compose、自研 v2 液态玻璃
  Chrome、MMKV 偏好存储。
- 构建基线：Java 21、Gradle Wrapper `9.8.0`、Kotlin `2.4.21-RC`、Android Gradle Plugin
  `9.4.1`、Ktor `3.6.0`、已生成的 Baseline Profiles 与共享 build-logic 约定。
- Baseline Profile 采集覆盖六段有回放上限的旅程，采集结果与发布新鲜度分别核对；v1.17.0 已在 HyperOS Phone 完成采集，并在 Pad 鉴赏适配后使用原生 HyperOS Pad 重新完整采集。
  详见 [构建指南](BUILD_CN.md) 和 [采集计划](../docs/planning/baseline-profile-coverage.md)。
