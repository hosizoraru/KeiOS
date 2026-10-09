# KeiOS

<!-- markdownlint-disable MD013 MD033 -->

[English Version](../README.md)

<p align="center">
  <a href="https://github.com/hosizoraru/KeiOS/releases"><img alt="Latest release" src="https://img.shields.io/github/v/release/hosizoraru/KeiOS?include_prereleases&sort=semver&display_name=tag&style=flat-square"></a>
  <a href="../LICENSE"><img alt="License" src="https://img.shields.io/github/license/hosizoraru/KeiOS?style=flat-square"></a>
  <a href="https://github.com/hosizoraru/KeiOS/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/hosizoraru/KeiOS?style=flat-square"></a>
  <a href="https://github.com/hosizoraru/KeiOS/network/members"><img alt="GitHub forks" src="https://img.shields.io/github/forks/hosizoraru/KeiOS?style=flat-square"></a>
  <a href="https://github.com/hosizoraru/KeiOS/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/hosizoraru/KeiOS?style=flat-square"></a>
  <a href="https://github.com/hosizoraru/KeiOS/commits/master"><img alt="Last commit" src="https://img.shields.io/github/last-commit/hosizoraru/KeiOS/master?style=flat-square"></a>
  <img alt="Release downloads" src="https://img.shields.io/github/downloads/hosizoraru/KeiOS/total?style=flat-square">
</p>

<p align="center">
  <a href="https://github.com/hosizoraru/KeiOS/actions/workflows/ci-debug-apk.yml"><img alt="Debug APK CI" src="https://github.com/hosizoraru/KeiOS/actions/workflows/ci-debug-apk.yml/badge.svg?branch=master"></a>
  <a href="https://github.com/hosizoraru/KeiOS/actions/workflows/ci-benchmark-apk.yml"><img alt="Benchmark APK CI" src="https://github.com/hosizoraru/KeiOS/actions/workflows/ci-benchmark-apk.yml/badge.svg?branch=master"></a>
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-35-3DDC84?style=flat-square&logo=android&logoColor=white">
  <img alt="targetSdk" src="https://img.shields.io/badge/targetSdk-37-3DDC84?style=flat-square&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4.21--RC-7F52FF?style=flat-square&logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-1.12.1-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white">
</p>

KeiOS 是一个 Android 工具台，聚合系统参数查看、本地 MCP 服务、GitHub Releases / Actions、GitHub Star
导入、订阅项目追踪、Blue Archive 辅助与学生图鉴功能。应用使用 Compose + Miuix 构建，并提供 v2
液态玻璃界面、高密度状态卡、导入导出、本地化
MCP Skill、支持语义化图标的通知提醒、仓库发现、反馈 Issue 草稿、缓存诊断和已生成的 Baseline Profiles。

## 项目信息

| 项目 | 内容 |
| --- | --- |
| 正式包名 | `os.kei` |
| 支持 ABI | `arm64-v8a` |
| Android 基线 | Android 15+（`minSdk 35`） |
| Target SDK | Android 17 / API 37 |
| UI 技术栈 | Jetpack Compose、Miuix、液态玻璃风格 chrome |
| 运行技术栈 | Kotlin、Java 21、Shizuku/Root、Media3、MMKV、Ktor、OkHttp |
| 语言资源 | 简体中文、English、日本語 |
| 发布版本 | [`v1.17.0`](https://github.com/hosizoraru/KeiOS/releases/tag/v1.17.0) |

## 常用入口

- [最新稳定版](https://github.com/hosizoraru/KeiOS/releases/latest)
- [全部 Releases](https://github.com/hosizoraru/KeiOS/releases)
- [Debug APK CI artifact](https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master)
- [Benchmark APK CI artifact](https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master)
- [功能完整介绍](FEATURES_CN.md)
- [构建指南](BUILD_CN.md)
- [贡献指南](../CONTRIBUTING.md)
- [安全策略](../SECURITY.md)

## 主要功能

- Home 仪表盘使用状态 pill 与 MCP、GitHub、BA 摘要卡呈现关键状态。
- OS 工具支持系统表、Android/Java/Linux 属性、内置活动快捷入口、活动 / Shell 卡片搜索分组 sheet、Shizuku Shell
  和卡片导入导出。
- MCP 页面支持本地服务开关、配置复制、运行日志、前台保活、Claw 接入引导、本地化
  SKILL.md、工作流蓝图、结构化工具元数据，以及覆盖运行态、Home、OS、GitHub 发现/追踪、BA 账号、日常与缓存巡检的 51 个工具。
- GitHub 页面支持 Releases、Actions artifact、通用 Git、Direct APK 和 F-Droid 仓库追踪，提供
  Atom/API 策略对比、包名扫描、已安装应用反扫、订阅项目、分享链接导入、本机应用联动和 Star List 导入。
- GitHub Star 导入 Activity 支持自己的 stars、他人的公开 stars 与公开 Star List
  链接，提供分类发现、质量筛选、多选导入、APK 验证和退出确认。
- GitHub 托管安装与分享导入联动支持 Shizuku APK 交付，提供通知 / 超级岛进度、Manifest 检查、versionCode 展示和安装确认界面。
- GitHub Actions 更新通知支持追踪应用图标、跳转到对应 Actions sheet、推荐 run 定位和调试通知测试。
- 历史中心支持 Actions、刷新诊断、追踪变更和已追踪 App 安装/更新记录，慢刷新会按网络阶段给出耗时，并提供未读角标、搜索、导出和 MCP 查询。
- JSON 导入与 WebDAV 同步支持多种 KeiOS 数据结构迁移，包括 GitHub/F-Droid 追踪、OS 卡片、BA 多账号、预览摘要与导入结果跳转。
- BA 办公室支持 AP、咖啡厅来访、竞技场刷新提醒、六槽制造室计时、可配置一键日常、分账号快速设置磁贴与启动器快捷方式、分服务器活动/卡池数据、超级岛通知和学生图鉴入口。
- 学生图鉴支持全页搜索、排序、实装学生详情长期缓存、媒体缓存、MP4 与 Spine 动态回忆大厅、学生 3D 模型工具、PiP 视频播放、带删除撤销的 BGM 收藏、默认原生媒体通知、鉴赏媒体、媒体导出、液态底栏和收藏导入导出。
- 设置页提供主题、动效、v2 液态玻璃组件、底栏特效策略、搜索默认聚焦、握姿感知浮动
  dock、背景图、应用语言、权限、缓存诊断、结构化日志、本地 GitHub Issue 反馈、无遥测诊断与通知兼容配置。

## v1.17.0 重点变化

本次发布包含自公开 v1.15.0 起的变化，已在 HyperOS Phone 完成六段 Baseline Profile 采集，并在 Pad 鉴赏适配后使用 HyperOS Pad 重新完整采集。正式签名 APK 可从 [v1.17.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.17.0) 下载。

- Spine 动态回忆大厅支持动作切换、缩放、移动、重置视角与关联 BGM；隐藏控件后动画继续播放，老学生的 MP4 观看方式也保留。
- 已匹配 3D 资源的学生可切换模型和动画、调整视角、进度、速度、循环和描边；背景支持深浅色默认值及可保存的调色盘。
- Pad 鉴赏增加叠在场景上的液态玻璃工具侧栏及内联调色盘，打开工具时模型保持稳定，选色后调色盘继续保留；颜色编辑区为键盘让出空间，整个场景保持原位，并修复视角操作时动画偶发停住的问题。
- 两个鉴赏界面都默认使用系统 WebView，可在设置 → 界面 → 性能预取中分别选择「兼容画面呈现」，下次进入生效；已有选择继续保留。这些选项调整鉴赏画面的呈现方式，不会切换设备的 Vulkan/OpenGL 驱动，也不保证更高帧率。
- 搜索栏保留光标、选择范围和中文输入状态；键盘弹出或收起时，弹层玻璃模糊保持连续。
- 开启预发行选项后，仅预发行仓库也可扫描包名；同仓库的主应用与插件按各自 APK 包名追踪，旧 Atom 判断会自动刷新。
- 冷启动增加主题协调的过渡，减少初始状态闪现；改善学生详情及多处页面滚动体验，保留玻璃材质与动画。
- 可选择超级岛的关闭时间；Shell 命令长时间无输出也能触发超时，避免一直等待。
- 一并包含本地 v1.16 阶段的改进：更可靠的版本判断、刷新诊断、发行版/F-Droid 旧版本应用内安装，以及更低的玻璃渲染开销。

从公开 v1.15.0 升级的完整变化、CI 用户安装说明及兼容性说明见 [v1.17.0 更新说明](RELEASE_V1.17.0.md)；v1.16.0 的历史说明记录本地开发里程碑。

完整功能介绍：

- [功能完整介绍 (CN)](FEATURES_CN.md)
- [Feature Overview (EN)](FEATURES.md)

## 当前分发方式

- 稳定版安装包通过 [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases) 发布。
- 公开稳定版始终通过 [最新稳定版](https://github.com/hosizoraru/KeiOS/releases/latest) 获取。
- 当前发布版本为 [v1.17.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.17.0)，提供 KeiOS Release 签名 APK 和 SHA-256 校验文件。CI 包使用独立的签名证书。
- 正式版基线：`os.kei`、`arm64-v8a`、Android 15+（`minSdk 35`）。
- 运行与构建基线：`targetSdk=37`、Java 21、Gradle Wrapper `9.8.0`、Kotlin `2.4.21-RC`、
  Compose `1.12.1`、Android Gradle Plugin `9.4.1`、Ktor `3.6.0`。
- 当前应用语言资源覆盖简体中文、English、日本語。

## 文档

- [文档索引](INDEX.md)
- [Release Notes v1.17.0](RELEASE_V1.17.0.md)
- [Release Notes v1.16.0](RELEASE_V1.16.0.md)
- [Build Guide (EN)](BUILD.md)
- [构建指南 (CN)](BUILD_CN.md)
- [Todo List (EN)](TODO.md)
- [待办清单 (CN)](TODO_CN.md)
- [行为准则](../CODE_OF_CONDUCT.md)
- [贡献指南](../CONTRIBUTING.md)
- [安全策略](../SECURITY.md)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=hosizoraru/KeiOS&type=Date)](https://www.star-history.com/#hosizoraru/KeiOS&Date)
