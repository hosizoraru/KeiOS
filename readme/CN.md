# KeiOS

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
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-1.10.6-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white">
</p>

KeiOS 是一个 Android 工具台，聚合系统参数查看、本地 MCP 服务、GitHub Releases / Actions artifact、Blue Archive 辅助与学生图鉴功能。应用使用 Compose + Miuix 构建，并提供液态玻璃风格界面、高密度状态卡、导入导出、本地化 MCP Skill、通知提醒和缓存诊断。

## 项目信息

| 项目 | 内容 |
| --- | --- |
| 正式包名 | `os.kei` |
| 支持 ABI | `arm64-v8a` |
| Android 基线 | Android 15+（`minSdk 35`） |
| Target SDK | Android 17 / API 37 |
| UI 技术栈 | Jetpack Compose、Miuix、液态玻璃风格 chrome |
| 运行技术栈 | Kotlin、Java 21、Shizuku、Media3、MMKV、Ktor、OkHttp |
| 语言资源 | 简体中文、English、日本語 |

## 常用入口

- [最新稳定版](https://github.com/hosizoraru/KeiOS/releases/latest)
- [全部 Releases](https://github.com/hosizoraru/KeiOS/releases)
- [Debug APK CI artifact](https://nightly.link/hosizoraru/KeiOS/workflows/ci-debug-apk/master)
- [Benchmark APK CI artifact](https://nightly.link/hosizoraru/KeiOS/workflows/ci-benchmark-apk/master)
- [功能完整介绍](FEATURES_CN.md)
- [构建指南](BUILD_CN.md)

## 主要功能

- Home 仪表盘集中显示 MCP、GitHub、BA、Shizuku 与分享导入状态。
- OS 工具支持系统表、Android/Java/Linux 属性、活动快捷入口、Shizuku Shell、Shell 卡片和卡片导入导出。
- MCP 页面支持本地服务开关、配置复制、运行日志、前台保活、Claw 接入引导、本地化 SKILL.md，以及 Home、OS、GitHub、BA 快照工具。
- GitHub 页面支持 Releases 与 Actions artifact 追踪、预发版策略、nightly.link 或 Token Actions 读取、分支 / workflow / run / artifact 选择、分享链接导入和本机应用联动。
- BA 办公室支持 AP、咖啡厅来访、竞技场刷新提醒、分服务器时区、媒体设置、超级岛通知和学生图鉴入口。
- 学生图鉴支持目录搜索排序、媒体缓存、语音语言标签、BGM 收藏、鉴赏媒体、媒体导出和收藏导入导出。
- 设置页提供主题、动效、液态玻璃组件、底栏特效策略、背景图、应用语言、权限、缓存诊断、日志与通知兼容配置。

完整功能介绍：
- [功能完整介绍 (CN)](FEATURES_CN.md)
- [Feature Overview (EN)](FEATURES.md)

## 当前分发方式

- 稳定版安装包通过 [GitHub Releases](https://github.com/hosizoraru/KeiOS/releases) 发布。
- 当前稳定标签：[v1.3.2](https://github.com/hosizoraru/KeiOS/releases/tag/v1.3.2)。
- 正式版基线：`os.kei`、`arm64-v8a`、Android 15+（`minSdk 35`）。
- 运行与构建基线：`targetSdk=37`、Java 21、Kotlin/Gradle 项目工具链。
- 当前应用语言资源覆盖简体中文、English、日本語。

## 文档

- [文档索引](INDEX.md)
- [Build Guide (EN)](BUILD.md)
- [构建指南 (CN)](BUILD_CN.md)
- [Todo List (EN)](TODO.md)
- [待办清单 (CN)](TODO_CN.md)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=hosizoraru/KeiOS&type=Date)](https://www.star-history.com/#hosizoraru/KeiOS&Date)
