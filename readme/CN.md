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
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white">
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
| 当前稳定版 | [v1.19.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.19.0) |
| 更新说明 | [v1.19.0 双语更新说明](RELEASE_V1.19.0.md) |

## 常用入口

- [最新稳定版](https://github.com/hosizoraru/KeiOS/releases/latest)
- [全部 Releases](https://github.com/hosizoraru/KeiOS/releases)
- [Debug APK CI artifact](https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master)
- [Benchmark APK CI artifact](https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master)
- [功能完整介绍](FEATURES_CN.md)
- [构建指南](BUILD_CN.md)
- [CI 工作流说明（EN）](../docs/build/github-actions.md)
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
- [GitHub 项目追踪桌面小组件](../docs/planning/github-project-widget.md)支持缓存更新数、响应式布局、
  可选的项目名称与版本，并在 GitHub 页顶栏提供添加入口。
- GitHub Star 导入 Activity 支持自己的 stars、他人的公开 stars 与公开 Star List
  链接，提供分类发现、质量筛选、多选导入、APK 验证和退出确认。
- GitHub 托管安装与分享导入联动支持 Shizuku APK 交付，提供通知 / 超级岛进度、Manifest 检查、versionCode 展示和安装确认界面。
- GitHub Actions 更新通知支持追踪应用图标、跳转到对应 Actions sheet、推荐 run 定位和调试通知测试。
- 历史中心支持 Actions、刷新诊断、追踪变更和已追踪 App 安装/更新记录，慢刷新会按网络阶段给出耗时，并提供未读角标、搜索、导出和 MCP 查询。
- JSON 导入与 WebDAV 同步支持多种 KeiOS 数据结构迁移，包括 GitHub/F-Droid 追踪、OS 卡片、BA 多账号、预览摘要与导入结果跳转。
  当前源码中的同步合并、上传校验和下载覆盖恢复规则见 [WebDAV 与坚果云说明](../feature-webdav/README.md#中文说明)。
- BA 办公室支持 AP、咖啡厅来访、竞技场刷新提醒、六槽制造室计时、可配置一键日常、分账号快速设置磁贴与启动器快捷方式、分服务器活动/卡池数据、超级岛通知和学生图鉴入口。
- 学生图鉴支持全页搜索、排序、实装学生详情长期缓存、媒体缓存、MP4 与 Spine 动态回忆大厅、学生 3D 模型工具、PiP 视频播放、带删除撤销的 BGM 收藏、默认原生媒体通知、鉴赏媒体、媒体导出、液态底栏和收藏导入导出。
- 设置页提供主题、动效、v2 液态玻璃组件、底栏特效策略、搜索默认聚焦、握姿感知浮动
  dock、背景图、应用语言、权限、缓存诊断、结构化日志、本地 GitHub Issue 反馈、无遥测诊断与通知兼容配置。

## v1.19.0 重点变化

v1.19.0 已在 [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases/tag/v1.19.0) 发布，
以下为自 v1.18.0 起的变化。

- 修复 BGM 封面和当前曲目显示；从播放通知返回时，即使曲目未收藏，也能保留播放队列和暂停位置。
- 修正桌面图标形状与角标；离开设置后保留所选设计，更新后修复图标消失或重复的问题。
- 卡片、追踪与 BGM 收藏导入每次选择一个文件，取消后可重新选择；背景裁剪不再长期保留原图访问权限。
- 应用被系统关闭后，重新打开可继续最近未完成的 OS 卡片与 GitHub 追踪编辑；主动取消则丢弃草稿。
- AP 与咖啡厅 AP 提醒可以划除，后续 AP 更新保持静默。
- 调整窗口大小后，方向行为随手机和大窗口切换。

详细变化与升级说明见 [v1.19.0 双语更新说明](RELEASE_V1.19.0.md)；
[v1.18.0 记录](RELEASE_V1.18.0.md) 保留已发布版本的说明。

完整功能介绍：

- [功能完整介绍 (CN)](FEATURES_CN.md)
- [Feature Overview (EN)](FEATURES.md)

## 当前分发方式

| 渠道 | 包名 | 签名 | 下载 |
| --- | --- | --- | --- |
| 稳定版 | `os.kei` | KeiOS Release | [最新稳定版](https://github.com/hosizoraru/KeiOS/releases/latest) |
| Debug 预览版 | `os.kei.debug` | 公开 CI debug key | [Debug 制品](https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master) |
| Benchmark 预览版 | `os.kei` | 公开 CI debug key | [Benchmark 制品](https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master) |

- 稳定版安装包通过 [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases) 发布。
- [v1.19.0](https://github.com/hosizoraru/KeiOS/releases/tag/v1.19.0) 已提供 KeiOS Release 签名 APK 与 SHA-256 校验文件，后续公开更新以稳定版入口为准。
- Debug 可与稳定版共存。CI Benchmark 与稳定版使用相同包名、不同签名，不能直接覆盖已安装的稳定版。
- CI APK 是开发预览包，保留 14 天；版本名包含 commit 数和短 SHA。job summary 分别提供完整源码 commit、APK SHA-256 与制品归档 SHA-256。
  下载、手动构建和校验方法见 [CI 工作流说明（EN）](../docs/build/github-actions.md)。
- 正式版基线：`os.kei`、`arm64-v8a`、Android 15+（`minSdk 35`）。
- 运行与构建基线：`targetSdk=37`、Java 21、Gradle Wrapper `9.8.2`、Kotlin `2.4.21`、
  Compose `1.12.1`、Android Gradle Plugin `9.5.0-rc01`、Ktor `3.6.0`。
- 当前应用语言资源覆盖简体中文、English、日本語。

## 文档

- [文档索引](INDEX.md)
- [Release Notes v1.19.0](RELEASE_V1.19.0.md)
- [Release Notes v1.18.0](RELEASE_V1.18.0.md)
- [Release Notes v1.17.0](RELEASE_V1.17.0.md)
- [Release Notes v1.16.0](RELEASE_V1.16.0.md)
- [Build Guide (EN)](BUILD.md)
- [构建指南 (CN)](BUILD_CN.md)
- [CI 工作流说明（EN）](../docs/build/github-actions.md)
- [工程文档](../docs/README.md)
- [Todo List (EN)](TODO.md)
- [待办清单 (CN)](TODO_CN.md)
- [行为准则](../CODE_OF_CONDUCT.md)
- [贡献指南](../CONTRIBUTING.md)
- [安全策略](../SECURITY.md)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=hosizoraru/KeiOS&type=Date)](https://www.star-history.com/#hosizoraru/KeiOS&Date)
